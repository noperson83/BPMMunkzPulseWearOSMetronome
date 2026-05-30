package com.example.bpmmunkzpulse.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.edit
import com.example.bpmmunkzpulse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

enum class BeatAccentType(val persistedValue: Int) {
    Big(0),
    Medium(1),
    Small(2),
    Silent(3);

    fun next(): BeatAccentType {
        return when (this) {
            Big -> Medium
            Medium -> Small
            Small -> Silent
            Silent -> Big
        }
    }

    val hasBeep: Boolean
        get() = this != Silent

    companion object {
        fun fromPersistedValue(value: Int): BeatAccentType {
            return entries.firstOrNull { it.persistedValue == value } ?: Silent
        }
    }
}

enum class BeatSoundMode(val persistedValue: Int) {
    Clicks(0),
    Wood(1);

    companion object {
        fun fromPersistedValue(value: Int): BeatSoundMode {
            return entries.firstOrNull { it.persistedValue == value } ?: Clicks
        }
    }
}

enum class AccentIntensityMode(val persistedValue: Int) {
    Big(0),
    Medium(1),
    Little(2),
    Silent(3);

    fun volumePercentFor(
        accentType: BeatAccentType,
        ranges: List<AccentIntensityRange>,
    ): Int {
        if (accentType == BeatAccentType.Silent) return 0

        return when (accentType) {
            BeatAccentType.Big -> ranges.rangeFor(Big).valuePercent
            BeatAccentType.Medium -> ranges.rangeFor(Medium).valuePercent
            BeatAccentType.Small -> ranges.rangeFor(Little).valuePercent
            BeatAccentType.Silent -> 0
        }
    }

    fun hapticDurationMsFor(
        accentType: BeatAccentType,
        ranges: List<AccentIntensityRange>,
    ): Long {
        val volumePercent = volumePercentFor(accentType, ranges)
        if (volumePercent <= 0) return 0L

        return (volumePercent * 0.8f).toLong().coerceAtLeast(10L)
    }

    companion object {
        fun fromPersistedValue(value: Int): AccentIntensityMode {
            return entries.firstOrNull { it.persistedValue == value } ?: Big
        }
    }
}

data class AccentIntensityRange(
    val maxPercent: Int,
    val minPercent: Int,
    val valuePercent: Int,
) {
    val midPercent: Int
        get() = ((maxPercent + minPercent) / 2).coerceIn(0, 100)
}

data class MetronomeState(
    val bpm: Int = 64,
    val beatsPerMeasure: Int = 4,
    val accentBeat: Int = 1,
    val subdivisionCount: Int = 1,
    val beatAccentTypes: List<BeatAccentType> = defaultBeatAccentTypes(beatsPerMeasure, accentBeat),
    val accentIntensityMode: AccentIntensityMode = AccentIntensityMode.Big,
    val accentIntensityRanges: List<AccentIntensityRange> = defaultAccentIntensityRanges(),
    val hapticsEnabled: Boolean = false,
    val beepEnabled: Boolean = false,
    val beatSoundMode: BeatSoundMode = BeatSoundMode.Clicks,
    val keyDroneEnabled: Boolean = false,
    val keyDroneVolumePercent: Int = 18,
    val musicalKey: String = "C",
    val currentBeatIndex: Int = 1,
    val currentSubdivisionIndex: Int = 1,
    val playlistIndex: Int = 0,
    val songIndex: Int = 0,
    val isRunning: Boolean = false,
    val beatFlash: Boolean = false,
    val flashingBeat: Int = 0,
    val beatClockStartedAtMs: Long = SystemClock.elapsedRealtime(),
    val playbackStartedAtMs: Long = 0L,
)

class MetronomeService : Service() {
    private val binder = LocalBinder()
    private val timingDispatcher = Executors
        .newSingleThreadExecutor { runnable ->
            priorityThread(
                name = "BPM-MetronomeTiming",
                priority = Process.THREAD_PRIORITY_URGENT_AUDIO,
                runnable = runnable,
            )
        }
        .asCoroutineDispatcher()
    private val toneDispatcher = Executors
        .newSingleThreadExecutor { runnable ->
            priorityThread(
                name = "BPM-MetronomeTone",
                priority = Process.THREAD_PRIORITY_AUDIO,
                runnable = runnable,
            )
        }
        .asCoroutineDispatcher()
    private val hapticDispatcher = Executors
        .newSingleThreadExecutor { runnable ->
            priorityThread(
                name = "BPM-MetronomeHaptic",
                priority = Process.THREAD_PRIORITY_AUDIO,
                runnable = runnable,
            )
        }
        .asCoroutineDispatcher()
    private val serviceScope = CoroutineScope(SupervisorJob() + timingDispatcher)
    private val toneScope = CoroutineScope(SupervisorJob() + toneDispatcher)
    private val hapticScope = CoroutineScope(SupervisorJob() + hapticDispatcher)
    private val mutableState = MutableStateFlow(MetronomeState())
    private var metronomeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var tonePlayer: BeatTonePlayer? = null
    private var keyDronePlayer: KeyDronePlayer? = null

    val state: StateFlow<MetronomeState> = mutableState.asStateFlow()

    inner class LocalBinder : Binder() {
        val service: MetronomeService
            get() = this@MetronomeService
    }

    override fun onCreate() {
        super.onCreate()
        mutableState.value = applicationContext.loadSavedRhythmState()
        vibrator = beatPulseVibrator()
        tonePlayer = BeatTonePlayer(applicationContext)
        keyDronePlayer = KeyDronePlayer()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!mutableState.value.isRunning) {
            stopSelf()
        }
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startPlayback(intent.readMetronomeState(mutableState.value))
            ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    fun startPlayback(config: MetronomeState = mutableState.value) {
        val now = SystemClock.elapsedRealtime()
        mutableState.value = config
            .normalized()
            .copy(
                isRunning = true,
                beatFlash = false,
                flashingBeat = 0,
                currentBeatIndex = 1,
                currentSubdivisionIndex = 1,
                beatClockStartedAtMs = now,
                playbackStartedAtMs = now,
            )

        applicationContext.saveRhythmState(mutableState.value)
        startInForeground()
        wakeLock().acquireIfNeeded()
        syncKeyDrone()
        restartTicker(resetClock = false)
    }

    fun stopPlayback() {
        metronomeJob?.cancel()
        metronomeJob = null
        mutableState.update {
            it.copy(
                isRunning = false,
                beatFlash = false,
                flashingBeat = 0,
                currentBeatIndex = 1,
                currentSubdivisionIndex = 1,
                playbackStartedAtMs = 0L,
            )
        }
        wakeLock?.releaseIfHeld()
        cancelHaptics()
        keyDronePlayer?.stop()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopPlayback()
        super.onTaskRemoved(rootIntent)
    }

    fun setBpm(bpm: Int, restartBeat: Boolean = true) {
        updateConfig(restartBeat = restartBeat) {
            it.copy(bpm = bpm.coerceIn(MIN_BPM, MAX_BPM))
        }
    }

    fun setBeatsPerMeasure(beatsPerMeasure: Int) {
        updateConfig(restartBeat = true) {
            val safeBeatsPerMeasure = beatsPerMeasure.coerceIn(MIN_BEATS_PER_MEASURE, MAX_BEATS_PER_MEASURE)
            val beatAccentTypes = it.beatAccentTypes.normalizedBeatAccentTypes(
                beatsPerMeasure = safeBeatsPerMeasure,
                accentBeat = it.accentBeat,
            )
            it.copy(
                beatsPerMeasure = safeBeatsPerMeasure,
                accentBeat = beatAccentTypes.primaryAccentBeat(),
                beatAccentTypes = beatAccentTypes,
                currentBeatIndex = it.currentBeatIndex.coerceIn(1, safeBeatsPerMeasure),
            )
        }
    }

    fun setAccentBeat(accentBeat: Int) {
        updateConfig(restartBeat = false) {
            val safeAccentBeat = accentBeat.coerceIn(1, it.beatsPerMeasure)
            val beatAccentTypes = List(it.beatsPerMeasure) { index ->
                if (index + 1 == safeAccentBeat) BeatAccentType.Big else BeatAccentType.Silent
            }
            it.copy(
                accentBeat = safeAccentBeat,
                beatAccentTypes = beatAccentTypes,
            )
        }
    }

    fun cycleBeatAccentType(beat: Int) {
        updateConfig(restartBeat = false) {
            val safeBeat = beat.coerceIn(1, it.beatsPerMeasure)
            val beatAccentTypes = it.beatAccentTypes
                .normalizedBeatAccentTypes(
                    beatsPerMeasure = it.beatsPerMeasure,
                    accentBeat = it.accentBeat,
                )
                .mapIndexed { index, accentType ->
                    if (index + 1 == safeBeat) accentType.next() else accentType
                }
            it.copy(
                accentBeat = beatAccentTypes.primaryAccentBeat(),
                beatAccentTypes = beatAccentTypes,
            )
        }
    }

    fun setHapticsEnabled(hapticsEnabled: Boolean) {
        updateConfig(restartBeat = false) {
            it.copy(hapticsEnabled = hapticsEnabled)
        }

        if (!hapticsEnabled) {
            cancelHaptics()
        }
    }

    fun setSubdivisionCount(subdivisionCount: Int) {
        updateConfig(restartBeat = true) {
            it.copy(
                subdivisionCount = subdivisionCount.toSupportedSubdivisionCount(),
                currentSubdivisionIndex = 1,
            )
        }
    }

    fun setBeepEnabled(beepEnabled: Boolean) {
        updateConfig(restartBeat = false) {
            it.copy(beepEnabled = beepEnabled)
        }
    }

    fun setBeatSoundMode(beatSoundMode: BeatSoundMode) {
        updateConfig(restartBeat = false) {
            it.copy(beatSoundMode = beatSoundMode)
        }
    }

    fun setKeyDroneEnabled(keyDroneEnabled: Boolean) {
        updateConfig(restartBeat = false) {
            it.copy(keyDroneEnabled = keyDroneEnabled)
        }
    }

    fun setKeyDroneVolumePercent(keyDroneVolumePercent: Int) {
        updateConfig(restartBeat = false) {
            it.copy(keyDroneVolumePercent = keyDroneVolumePercent.coerceIn(0, 100))
        }
    }

    fun setAccentIntensityMode(accentIntensityMode: AccentIntensityMode) {
        updateConfig(restartBeat = false) {
            it.copy(accentIntensityMode = accentIntensityMode)
        }
    }

    fun setAccentIntensityRanges(accentIntensityRanges: List<AccentIntensityRange>) {
        updateConfig(restartBeat = false) {
            it.copy(accentIntensityRanges = accentIntensityRanges)
        }
    }

    fun setPlaylistItem(
        playlistIndex: Int,
        songIndex: Int,
        bpm: Int,
        beatsPerMeasure: Int = mutableState.value.beatsPerMeasure,
        accentBeat: Int = mutableState.value.accentBeat,
        subdivisionCount: Int = mutableState.value.subdivisionCount,
        beatAccentTypes: List<BeatAccentType> = mutableState.value.beatAccentTypes,
        accentIntensityMode: AccentIntensityMode = mutableState.value.accentIntensityMode,
        accentIntensityRanges: List<AccentIntensityRange> = mutableState.value.accentIntensityRanges,
        musicalKey: String = mutableState.value.musicalKey,
        restartBeat: Boolean,
    ) {
        updateConfig(restartBeat = restartBeat) {
            val safeBeatsPerMeasure = beatsPerMeasure.coerceIn(MIN_BEATS_PER_MEASURE, MAX_BEATS_PER_MEASURE)
            val safeBeatAccentTypes = beatAccentTypes.normalizedBeatAccentTypes(
                beatsPerMeasure = safeBeatsPerMeasure,
                accentBeat = accentBeat,
            )
            it.copy(
                playlistIndex = playlistIndex.coerceAtLeast(0),
                songIndex = songIndex.coerceAtLeast(0),
                bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
                beatsPerMeasure = safeBeatsPerMeasure,
                accentBeat = safeBeatAccentTypes.primaryAccentBeat(),
                subdivisionCount = subdivisionCount.toSupportedSubdivisionCount(),
                beatAccentTypes = safeBeatAccentTypes,
                accentIntensityMode = accentIntensityMode,
                accentIntensityRanges = accentIntensityRanges,
                musicalKey = musicalKey,
            )
        }
    }

    fun syncBeatClock() {
        if (mutableState.value.isRunning) {
            restartTicker(resetClock = true)
        }
    }

    override fun onDestroy() {
        metronomeJob?.cancel()
        wakeLock?.releaseIfHeld()
        vibrator?.cancel()
        tonePlayer?.release()
        keyDronePlayer?.release()
        toneScope.cancel()
        hapticScope.cancel()
        serviceScope.cancel()
        toneDispatcher.close()
        hapticDispatcher.close()
        timingDispatcher.close()
        super.onDestroy()
    }

    private fun updateConfig(
        restartBeat: Boolean,
        update: (MetronomeState) -> MetronomeState,
    ) {
        val previousState = mutableState.value
        val nextState = update(previousState).normalized()
        if (nextState == previousState) return

        mutableState.value = nextState
        applicationContext.saveRhythmState(nextState)

        if (nextState.isRunning) {
            updateForegroundNotification()
            syncKeyDrone()
            if (restartBeat) {
                restartTicker(resetClock = true)
            }
        } else {
            syncKeyDrone()
        }
    }

    private fun restartTicker(resetClock: Boolean) {
        metronomeJob?.cancel()
        mutableState.update {
            it.copy(
                beatFlash = false,
                flashingBeat = 0,
                currentSubdivisionIndex = 1,
                beatClockStartedAtMs = if (resetClock) SystemClock.elapsedRealtime() else it.beatClockStartedAtMs,
            )
        }

        metronomeJob = serviceScope.launch {
            var beat = mutableState.value.currentBeatIndex.coerceIn(1, mutableState.value.beatsPerMeasure)
            var nextBeatStartedAtMs = SystemClock.elapsedRealtime()
            mutableState.update {
                it.copy(beatClockStartedAtMs = nextBeatStartedAtMs)
            }

            while (isActive && mutableState.value.isRunning) {
                delayUntilElapsedRealtime(nextBeatStartedAtMs)

                val beatState = mutableState.value
                val intervalMs = 60_000L / beatState.bpm
                val subdivisionCount = beatState.subdivisionCount.toSupportedSubdivisionCount()
                val subdivisionIntervalMs = intervalMs / subdivisionCount
                val beatAccentType = beatState.beatAccentTypes.typeForBeat(beat)
                val beatStartedAtMs = nextBeatStartedAtMs

                BeatTimingTrace.startBeat(
                    beat = beat,
                    bpm = beatState.bpm,
                    accentType = beatAccentType,
                )

                mutableState.update {
                    it.copy(
                        beatFlash = true,
                        flashingBeat = beat,
                        currentBeatIndex = beat,
                        currentSubdivisionIndex = 1,
                    )
                }
                BeatTimingTrace.mark("service beatFlash set")
                if (beatState.hapticsEnabled) {
                    pulseHaptic(
                        accentType = beatAccentType,
                        accentIntensityMode = beatState.accentIntensityMode,
                        accentIntensityRanges = beatState.accentIntensityRanges,
                    )
                }
                if (beatState.beepEnabled) {
                    playBeep(
                        accentType = beatAccentType,
                        accentIntensityMode = beatState.accentIntensityMode,
                        accentIntensityRanges = beatState.accentIntensityRanges,
                        beatSoundMode = beatState.beatSoundMode,
                    )
                }

                delayUntilElapsedRealtime(beatStartedAtMs + BEAT_FLASH_DURATION_MS)
                mutableState.update {
                    it.copy(
                        beatFlash = false,
                        flashingBeat = 0,
                    )
                }

                for (subdivisionIndex in 2..subdivisionCount) {
                    val targetElapsedMs = (subdivisionIndex - 1) * subdivisionIntervalMs
                    delayUntilElapsedRealtime(beatStartedAtMs + targetElapsedMs)
                    mutableState.update {
                        it.copy(currentSubdivisionIndex = subdivisionIndex)
                    }
                }

                val latestState = mutableState.value
                val nextBeat = if (beat == latestState.beatsPerMeasure) 1 else beat + 1
                nextBeatStartedAtMs = beatStartedAtMs + intervalMs

                beat = nextBeat
            }
        }
    }

    private fun playBeep(
        accentType: BeatAccentType,
        accentIntensityMode: AccentIntensityMode,
        accentIntensityRanges: List<AccentIntensityRange>,
        beatSoundMode: BeatSoundMode,
    ) {
        if (!accentType.hasBeep) return

        toneScope.launch {
            tonePlayer?.beep(accentType, accentIntensityMode, accentIntensityRanges, beatSoundMode)
        }
    }

    private fun syncKeyDrone() {
        val state = mutableState.value
        if (state.isRunning && state.keyDroneEnabled) {
            keyDronePlayer?.start(state.musicalKey, state.keyDroneVolumePercent)
        } else {
            keyDronePlayer?.stop()
        }
    }

    private fun pulseHaptic(
        accentType: BeatAccentType,
        accentIntensityMode: AccentIntensityMode,
        accentIntensityRanges: List<AccentIntensityRange>,
    ) {
        if (accentType == BeatAccentType.Silent) return

        BeatTimingTrace.mark("haptic dispatch")
        hapticScope.launch {
            vibrator?.pulse(accentType, accentIntensityMode, accentIntensityRanges)
        }
    }

    private fun cancelHaptics() {
        hapticScope.launch {
            vibrator?.cancel()
        }
    }

    private fun startInForeground() {
        createNotificationChannel()
        startForeground(
            METRONOME_NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    private fun updateForegroundNotification() {
        if (mutableState.value.isRunning && canPostNotifications()) {
            notificationManager().notify(METRONOME_NOTIFICATION_ID, buildNotification())
        }
    }

    private fun canPostNotifications(): Boolean {
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun buildNotification(): Notification {
        val currentState = mutableState.value
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MetronomeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return Notification.Builder(this, METRONOME_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_metronome_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("${currentState.bpm} BPM")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_metronome_notification),
                    "Stop",
                    stopIntent,
                ).build(),
            )
            .build()
    }

    private fun createNotificationChannel() {
        notificationManager().createNotificationChannel(
            NotificationChannel(
                METRONOME_CHANNEL_ID,
                "Metronome",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun notificationManager(): NotificationManager {
        return getSystemService(NotificationManager::class.java)
    }

    private fun wakeLock(): PowerManager.WakeLock {
        return wakeLock ?: beatPulseWakeLock().also { wakeLock = it }
    }

    private fun Context.beatPulseWakeLock(): PowerManager.WakeLock {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:BeatPulse")
            .apply { setReferenceCounted(false) }
    }

    private fun Context.beatPulseVibrator(): Vibrator {
        val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        return manager.defaultVibrator
    }

    companion object {
        private const val ACTION_START = "com.example.bpmmunkzpulse.presentation.action.START_METRONOME"
        private const val ACTION_STOP = "com.example.bpmmunkzpulse.presentation.action.STOP_METRONOME"
        private const val EXTRA_BPM = "bpm"
        private const val EXTRA_BEATS_PER_MEASURE = "beats_per_measure"
        private const val EXTRA_ACCENT_BEAT = "accent_beat"
        private const val EXTRA_SUBDIVISION_COUNT = "subdivision_count"
        private const val EXTRA_ACCENT_INTENSITY_MODE = "accent_intensity_mode"
        private const val EXTRA_ACCENT_INTENSITY_MAXES = "accent_intensity_maxes"
        private const val EXTRA_ACCENT_INTENSITY_MINS = "accent_intensity_mins"
        private const val EXTRA_ACCENT_INTENSITY_VALUES = "accent_intensity_values"
        private const val EXTRA_HAPTICS_ENABLED = "haptics_enabled"
        private const val EXTRA_BEEP_ENABLED = "beep_enabled"
        private const val EXTRA_CURRENT_BEAT_INDEX = "current_beat_index"
        private const val EXTRA_CURRENT_SUBDIVISION_INDEX = "current_subdivision_index"
        private const val EXTRA_PLAYLIST_INDEX = "playlist_index"
        private const val EXTRA_SONG_INDEX = "song_index"
        private const val MIN_BEATS_PER_MEASURE = 2
        private const val MAX_BEATS_PER_MEASURE = 16
        private const val METRONOME_CHANNEL_ID = "metronome_playback"
        private const val METRONOME_NOTIFICATION_ID = 101

        fun start(context: Context, state: MetronomeState) {
            context.startForegroundService(
                Intent(context, MetronomeService::class.java)
                    .setAction(ACTION_START)
                    .putMetronomeState(state),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, MetronomeService::class.java)
                    .setAction(ACTION_STOP),
            )
        }
    }
}

private fun MetronomeState.normalized(): MetronomeState {
    val safeBeatsPerMeasure = beatsPerMeasure.coerceIn(2, 16)
    val safeSubdivisionCount = subdivisionCount.toSupportedSubdivisionCount()
    val safeBeatAccentTypes = beatAccentTypes.normalizedBeatAccentTypes(
        beatsPerMeasure = safeBeatsPerMeasure,
        accentBeat = accentBeat,
    )
    return copy(
        bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
        beatsPerMeasure = safeBeatsPerMeasure,
        accentBeat = safeBeatAccentTypes.primaryAccentBeat(),
        subdivisionCount = safeSubdivisionCount,
        beatAccentTypes = safeBeatAccentTypes,
        accentIntensityMode = accentIntensityMode,
        accentIntensityRanges = accentIntensityRanges.normalizedAccentIntensityRanges(),
        keyDroneVolumePercent = keyDroneVolumePercent.coerceIn(0, 100),
        currentBeatIndex = currentBeatIndex.coerceIn(1, safeBeatsPerMeasure),
        currentSubdivisionIndex = currentSubdivisionIndex.coerceIn(1, safeSubdivisionCount),
        playlistIndex = playlistIndex.coerceAtLeast(0),
        songIndex = songIndex.coerceAtLeast(0),
    )
}

private fun Int.toSupportedSubdivisionCount(): Int {
    return when {
        this <= 1 -> 1
        this == 2 -> 2
        this == 3 -> 3
        this == 4 -> 4
        else -> 6
    }
}

private fun Intent.putMetronomeState(state: MetronomeState): Intent {
    return putExtra("bpm", state.bpm)
        .putExtra("beats_per_measure", state.beatsPerMeasure)
        .putExtra("accent_beat", state.accentBeat)
        .putExtra("subdivision_count", state.subdivisionCount)
        .putExtra("beat_accent_types", state.beatAccentTypes.toPersistedIntArray())
        .putExtra("accent_intensity_mode", state.accentIntensityMode.persistedValue)
        .putExtra("accent_intensity_maxes", state.accentIntensityRanges.toMaxPercentIntArray())
        .putExtra("accent_intensity_mins", state.accentIntensityRanges.toMinPercentIntArray())
        .putExtra("accent_intensity_values", state.accentIntensityRanges.toValuePercentIntArray())
        .putExtra("haptics_enabled", state.hapticsEnabled)
        .putExtra("beep_enabled", state.beepEnabled)
        .putExtra("beat_sound_mode", state.beatSoundMode.persistedValue)
        .putExtra("key_drone_enabled", state.keyDroneEnabled)
        .putExtra("key_drone_volume_percent", state.keyDroneVolumePercent)
        .putExtra("musical_key", state.musicalKey)
        .putExtra("current_beat_index", state.currentBeatIndex)
        .putExtra("current_subdivision_index", state.currentSubdivisionIndex)
        .putExtra("playlist_index", state.playlistIndex)
        .putExtra("song_index", state.songIndex)
        .putExtra("playback_started_at_ms", state.playbackStartedAtMs)
}

private fun Intent.readMetronomeState(fallback: MetronomeState): MetronomeState {
    return fallback.copy(
        bpm = getIntExtra("bpm", fallback.bpm),
        beatsPerMeasure = getIntExtra("beats_per_measure", fallback.beatsPerMeasure),
        accentBeat = getIntExtra("accent_beat", fallback.accentBeat),
        subdivisionCount = getIntExtra("subdivision_count", fallback.subdivisionCount),
        beatAccentTypes = getIntArrayExtra("beat_accent_types")
            ?.map { BeatAccentType.fromPersistedValue(it) }
            ?: fallback.beatAccentTypes,
        accentIntensityMode = AccentIntensityMode.fromPersistedValue(
            getIntExtra("accent_intensity_mode", fallback.accentIntensityMode.persistedValue),
        ),
        accentIntensityRanges = readAccentIntensityRanges(fallback.accentIntensityRanges),
        hapticsEnabled = getBooleanExtra("haptics_enabled", fallback.hapticsEnabled),
        beepEnabled = getBooleanExtra("beep_enabled", fallback.beepEnabled),
        beatSoundMode = BeatSoundMode.fromPersistedValue(
            getIntExtra("beat_sound_mode", fallback.beatSoundMode.persistedValue),
        ),
        keyDroneEnabled = getBooleanExtra("key_drone_enabled", fallback.keyDroneEnabled),
        keyDroneVolumePercent = getIntExtra(
            "key_drone_volume_percent",
            fallback.keyDroneVolumePercent,
        ),
        musicalKey = getStringExtra("musical_key") ?: fallback.musicalKey,
        currentBeatIndex = getIntExtra("current_beat_index", fallback.currentBeatIndex),
        currentSubdivisionIndex = getIntExtra(
            "current_subdivision_index",
            fallback.currentSubdivisionIndex,
        ),
        playlistIndex = getIntExtra("playlist_index", fallback.playlistIndex),
        songIndex = getIntExtra("song_index", fallback.songIndex),
        playbackStartedAtMs = getLongExtra("playback_started_at_ms", fallback.playbackStartedAtMs),
    )
}

private fun Intent.readAccentIntensityRanges(
    fallback: List<AccentIntensityRange>,
): List<AccentIntensityRange> {
    val maxes = getIntArrayExtra("accent_intensity_maxes")
    val mins = getIntArrayExtra("accent_intensity_mins")
    val values = getIntArrayExtra("accent_intensity_values")
    if (maxes == null || mins == null) return fallback

    return AccentIntensityMode.entries.mapIndexed { index, mode ->
        val defaultRange = defaultAccentIntensityRange(mode)
        AccentIntensityRange(
            maxPercent = maxes.getOrNull(index) ?: defaultRange.maxPercent,
            minPercent = mins.getOrNull(index) ?: defaultRange.minPercent,
            valuePercent = values?.getOrNull(index) ?: defaultRange.valuePercent,
        )
    }.normalizedAccentIntensityRanges()
}

private fun priorityThread(
    name: String,
    priority: Int,
    runnable: Runnable,
): Thread {
    return Thread {
        Process.setThreadPriority(priority)
        runnable.run()
    }.apply {
        this.name = name
    }
}

private suspend fun delayUntilElapsedRealtime(targetMs: Long) {
    val waitMs = targetMs - SystemClock.elapsedRealtime()
    if (waitMs > 0L) {
        delay(waitMs)
    }
}

private const val RHYTHM_PREFS = "bpm_munkz_rhythm"
private const val RHYTHM_BPM_KEY = "bpm"
private const val RHYTHM_BEATS_PER_MEASURE_KEY = "beats_per_measure"
private const val RHYTHM_ACCENT_BEAT_KEY = "accent_beat"
private const val RHYTHM_SUBDIVISION_COUNT_KEY = "subdivision_count"
private const val RHYTHM_BEAT_ACCENT_TYPES_KEY = "beat_accent_types"
private const val RHYTHM_ACCENT_INTENSITY_MODE_KEY = "accent_intensity_mode"
private const val RHYTHM_ACCENT_INTENSITY_RANGES_KEY = "accent_intensity_ranges"
private const val RHYTHM_HAPTICS_ENABLED_KEY = "haptics_enabled"
private const val RHYTHM_BEEP_ENABLED_KEY = "beep_enabled"
private const val RHYTHM_BEAT_SOUND_MODE_KEY = "beat_sound_mode"
private const val RHYTHM_KEY_DRONE_ENABLED_KEY = "key_drone_enabled"
private const val RHYTHM_KEY_DRONE_VOLUME_PERCENT_KEY = "key_drone_volume_percent"
private const val RHYTHM_MUSICAL_KEY_KEY = "musical_key"
private const val RHYTHM_PLAYLIST_INDEX_KEY = "playlist_index"
private const val RHYTHM_SONG_INDEX_KEY = "song_index"

internal fun Context.loadSavedRhythmState(): MetronomeState {
    val prefs = getSharedPreferences(RHYTHM_PREFS, Context.MODE_PRIVATE)
    return MetronomeState(
        bpm = prefs.getInt(RHYTHM_BPM_KEY, 64),
        beatsPerMeasure = prefs.getInt(RHYTHM_BEATS_PER_MEASURE_KEY, 4),
        accentBeat = prefs.getInt(RHYTHM_ACCENT_BEAT_KEY, 1),
        subdivisionCount = prefs.getInt(RHYTHM_SUBDIVISION_COUNT_KEY, 1),
        beatAccentTypes = prefs.getString(RHYTHM_BEAT_ACCENT_TYPES_KEY, null)
            .toBeatAccentTypes(),
        accentIntensityMode = AccentIntensityMode.fromPersistedValue(
            prefs.getInt(RHYTHM_ACCENT_INTENSITY_MODE_KEY, AccentIntensityMode.Big.persistedValue),
        ),
        accentIntensityRanges = prefs.getString(RHYTHM_ACCENT_INTENSITY_RANGES_KEY, null)
            .toAccentIntensityRanges(),
        hapticsEnabled = prefs.getBoolean(RHYTHM_HAPTICS_ENABLED_KEY, false),
        beepEnabled = prefs.getBoolean(RHYTHM_BEEP_ENABLED_KEY, false),
        beatSoundMode = BeatSoundMode.fromPersistedValue(
            prefs.getInt(RHYTHM_BEAT_SOUND_MODE_KEY, BeatSoundMode.Clicks.persistedValue),
        ),
        keyDroneEnabled = prefs.getBoolean(RHYTHM_KEY_DRONE_ENABLED_KEY, false),
        keyDroneVolumePercent = prefs.getInt(RHYTHM_KEY_DRONE_VOLUME_PERCENT_KEY, 18),
        musicalKey = prefs.getString(RHYTHM_MUSICAL_KEY_KEY, "C") ?: "C",
        playlistIndex = prefs.getInt(RHYTHM_PLAYLIST_INDEX_KEY, 0),
        songIndex = prefs.getInt(RHYTHM_SONG_INDEX_KEY, 0),
        beatClockStartedAtMs = SystemClock.elapsedRealtime(),
    ).normalized()
}

internal fun Context.saveRhythmState(state: MetronomeState) {
    val normalizedState = state.normalized()
    getSharedPreferences(RHYTHM_PREFS, Context.MODE_PRIVATE).edit {
        putInt(RHYTHM_BPM_KEY, normalizedState.bpm)
        putInt(RHYTHM_BEATS_PER_MEASURE_KEY, normalizedState.beatsPerMeasure)
        putInt(RHYTHM_ACCENT_BEAT_KEY, normalizedState.accentBeat)
        putInt(RHYTHM_SUBDIVISION_COUNT_KEY, normalizedState.subdivisionCount)
        putString(RHYTHM_BEAT_ACCENT_TYPES_KEY, normalizedState.beatAccentTypes.toPersistedString())
        putInt(RHYTHM_ACCENT_INTENSITY_MODE_KEY, normalizedState.accentIntensityMode.persistedValue)
        putString(RHYTHM_ACCENT_INTENSITY_RANGES_KEY, normalizedState.accentIntensityRanges.toPersistedRangeString())
        putBoolean(RHYTHM_HAPTICS_ENABLED_KEY, normalizedState.hapticsEnabled)
        putBoolean(RHYTHM_BEEP_ENABLED_KEY, normalizedState.beepEnabled)
        putInt(RHYTHM_BEAT_SOUND_MODE_KEY, normalizedState.beatSoundMode.persistedValue)
        putBoolean(RHYTHM_KEY_DRONE_ENABLED_KEY, normalizedState.keyDroneEnabled)
        putInt(RHYTHM_KEY_DRONE_VOLUME_PERCENT_KEY, normalizedState.keyDroneVolumePercent)
        putString(RHYTHM_MUSICAL_KEY_KEY, normalizedState.musicalKey)
        putInt(RHYTHM_PLAYLIST_INDEX_KEY, normalizedState.playlistIndex)
        putInt(RHYTHM_SONG_INDEX_KEY, normalizedState.songIndex)
    }
}

@SuppressLint("WakelockTimeout")
private fun PowerManager.WakeLock.acquireIfNeeded() {
    if (!isHeld) {
        acquire()
    }
}

private fun PowerManager.WakeLock.releaseIfHeld() {
    if (isHeld) {
        release()
    }
}

private fun Vibrator.pulse(
    accentType: BeatAccentType,
    accentIntensityMode: AccentIntensityMode,
    accentIntensityRanges: List<AccentIntensityRange>,
) {
    if (!hasVibrator()) return

    val durationMs = accentIntensityMode.hapticDurationMsFor(accentType, accentIntensityRanges)
    if (durationMs <= 0L) return

    BeatTimingTrace.mark("vibrate call")
    vibrate(
        createBeatVibrationEffect(
            accentType = accentType,
            durationMs = durationMs,
            hasAmplitudeControl = hasAmplitudeControl(),
        ),
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_ALARM)
            .build(),
    )
}

private fun createBeatVibrationEffect(
    accentType: BeatAccentType,
    durationMs: Long,
    hasAmplitudeControl: Boolean,
): VibrationEffect {
    val safeDurationMs = durationMs.coerceAtLeast(1L)
    if (!hasAmplitudeControl) {
        return VibrationEffect.createOneShot(
            safeDurationMs,
            VibrationEffect.DEFAULT_AMPLITUDE,
        )
    }

    val peakAmplitude = when (accentType) {
        BeatAccentType.Big -> 220
        BeatAccentType.Medium -> 172
        BeatAccentType.Small -> 124
        BeatAccentType.Silent -> 0
    }
    if (peakAmplitude <= 0) {
        return VibrationEffect.createOneShot(1L, 1)
    }

    val attackMs = 4L.coerceAtMost(safeDurationMs)
    val releaseMs = 6L.coerceAtMost((safeDurationMs - attackMs).coerceAtLeast(0L))
    val sustainMs = (safeDurationMs - attackMs - releaseMs).coerceAtLeast(1L)
    val startAmplitude = (peakAmplitude * 0.38f).toInt().coerceIn(1, 255)
    val releaseAmplitude = (peakAmplitude * 0.28f).toInt().coerceIn(1, 255)

    return VibrationEffect.createWaveform(
        longArrayOf(0L, attackMs, sustainMs, releaseMs),
        intArrayOf(0, startAmplitude, peakAmplitude, releaseAmplitude),
        -1,
    )
}

private class BeatTonePlayer(context: Context) {
    private var fallbackToneGenerator: ToneGenerator? = null
    private val toneGenerators = mutableMapOf<Int, ToneGenerator>()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loadedWoodSounds = mutableSetOf<Int>()
    private val woodBig: Int
    private val woodMid: Int
    private val woodLil: Int

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedWoodSounds += sampleId
            }
        }
        woodBig = soundPool.load(context, R.raw.wood_big, 1)
        woodMid = soundPool.load(context, R.raw.wood_mid, 1)
        woodLil = soundPool.load(context, R.raw.wood_lil, 1)
    }

    fun beep(
        accentType: BeatAccentType,
        accentIntensityMode: AccentIntensityMode,
        accentIntensityRanges: List<AccentIntensityRange>,
        beatSoundMode: BeatSoundMode,
    ) {
        if (!accentType.hasBeep) return

        val volumePercent = accentIntensityMode.volumePercentFor(accentType, accentIntensityRanges)
        if (volumePercent <= 0) return

        if (beatSoundMode == BeatSoundMode.Wood) {
            playWood(accentType, volumePercent)
            return
        }

        playClick(accentType, volumePercent)
    }

    fun release() {
        soundPool.release()
        fallbackToneGenerator?.release()
        fallbackToneGenerator = null
        toneGenerators.values.forEach { toneGenerator ->
            toneGenerator.release()
        }
        toneGenerators.clear()
    }

    private fun toneGenerator(volumePercent: Int): ToneGenerator? {
        val safeVolumePercent = volumePercent.coerceIn(0, 100)
        return toneGenerators[safeVolumePercent]
            ?: runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, safeVolumePercent)
            }.getOrNull()?.also { toneGenerator ->
                toneGenerators[safeVolumePercent] = toneGenerator
            }
    }

    private fun fallbackToneGenerator(): ToneGenerator? {
        return fallbackToneGenerator
            ?: runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            }.getOrNull()?.also { toneGenerator ->
                fallbackToneGenerator = toneGenerator
            }
    }

    private fun playWood(accentType: BeatAccentType, volumePercent: Int) {
        val soundId = when (accentType) {
            BeatAccentType.Big -> woodBig
            BeatAccentType.Medium -> woodMid
            BeatAccentType.Small -> woodLil
            BeatAccentType.Silent -> 0
        }
        if (soundId == 0 || soundId !in loadedWoodSounds) {
            playClick(accentType, volumePercent)
            return
        }

        val volume = (volumePercent / 100f).coerceIn(0f, 1f)
        BeatTimingTrace.mark("wood soundPool play")
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }

    private fun playClick(accentType: BeatAccentType, volumePercent: Int) {
        val toneGenerator = toneGenerator(volumePercent)
            ?: fallbackToneGenerator()
            ?: return

        BeatTimingTrace.mark("beep startTone call")
        toneGenerator.startTone(
            when (accentType) {
                BeatAccentType.Big -> ToneGenerator.TONE_PROP_BEEP
                BeatAccentType.Medium -> ToneGenerator.TONE_PROP_ACK
                BeatAccentType.Small -> ToneGenerator.TONE_PROP_NACK
                BeatAccentType.Silent -> ToneGenerator.TONE_PROP_BEEP
            },
            when (accentType) {
                BeatAccentType.Big -> BEEP_DURATION_MS
                BeatAccentType.Medium -> 54
                BeatAccentType.Small -> 46
                BeatAccentType.Silent -> BEEP_DURATION_MS
            },
        )
    }
}

private class KeyDronePlayer {
    private var audioTrack: AudioTrack? = null
    private var activeKey: String? = null
    private var activeVolumePercent: Int = -1

    fun start(musicalKey: String, volumePercent: Int) {
        val rootFrequency = musicalKey.rootFrequencyHz() ?: return
        val safeVolumePercent = volumePercent.coerceIn(0, 100)
        if (safeVolumePercent <= 0) {
            stop()
            return
        }
        if (
            audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING &&
            activeKey == musicalKey &&
            activeVolumePercent == safeVolumePercent
        ) {
            return
        }

        stop()
        val sampleRate = 22_050
        val samples = sampleRate
        val data = ByteArray(samples * 2)
        for (index in 0 until samples) {
            val phase = index.toDouble() / sampleRate.toDouble()
            val root = sin(2.0 * PI * rootFrequency * phase)
            val fifth = sin(2.0 * PI * rootFrequency * 1.5 * phase)
            val value = ((root * 0.62 + fifth * 0.38) * (safeVolumePercent / 100.0)).coerceIn(-1.0, 1.0)
            val sample = (value * Short.MAX_VALUE).toInt().toShort()
            data[index * 2] = (sample.toInt() and 0xff).toByte()
            data[index * 2 + 1] = ((sample.toInt() shr 8) and 0xff).toByte()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(data.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(data, 0, data.size)
        track.setLoopPoints(0, samples, -1)
        track.play()
        audioTrack = track
        activeKey = musicalKey
        activeVolumePercent = safeVolumePercent
    }

    fun stop() {
        audioTrack?.let { track ->
            runCatching { track.stop() }
            track.release()
        }
        audioTrack = null
        activeKey = null
        activeVolumePercent = -1
    }

    fun release() {
        stop()
    }
}

private fun String.rootFrequencyHz(): Double? {
    val cleaned = trim().replace("\u266f", "#").replace("\u266d", "b")
    val root = Regex("^[A-Ga-g](#|b)?").find(cleaned)?.value ?: return null
    val semitone = when (root.replaceFirstChar { it.uppercase() }) {
        "C" -> 0
        "C#", "Db" -> 1
        "D" -> 2
        "D#", "Eb" -> 3
        "E" -> 4
        "F" -> 5
        "F#", "Gb" -> 6
        "G" -> 7
        "G#", "Ab" -> 8
        "A" -> 9
        "A#", "Bb" -> 10
        "B" -> 11
        else -> return null
    }
    val midiNote = 48 + semitone
    return 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)
}

internal fun defaultBeatAccentTypes(
    beatsPerMeasure: Int,
    accentBeat: Int = 1,
): List<BeatAccentType> {
    val safeBeatsPerMeasure = beatsPerMeasure.coerceIn(2, 16)
    val safeAccentBeat = accentBeat.coerceIn(1, safeBeatsPerMeasure)
    return List(safeBeatsPerMeasure) { index ->
        if (index + 1 == safeAccentBeat) BeatAccentType.Big else BeatAccentType.Silent
    }
}

internal fun List<BeatAccentType>.typeForBeat(beat: Int): BeatAccentType {
    return getOrNull(beat - 1) ?: BeatAccentType.Silent
}

private fun List<BeatAccentType>.normalizedBeatAccentTypes(
    beatsPerMeasure: Int,
    accentBeat: Int,
): List<BeatAccentType> {
    val defaults = defaultBeatAccentTypes(beatsPerMeasure, accentBeat)
    return List(beatsPerMeasure.coerceIn(2, 16)) { index ->
        getOrNull(index) ?: defaults[index]
    }
}

private fun List<BeatAccentType>.primaryAccentBeat(): Int {
    val bigAccentIndex = indexOfFirst { it == BeatAccentType.Big }
    if (bigAccentIndex >= 0) return bigAccentIndex + 1

    val audibleAccentIndex = indexOfFirst { it.hasBeep }
    if (audibleAccentIndex >= 0) return audibleAccentIndex + 1

    return 1
}

private fun List<BeatAccentType>.toPersistedIntArray(): IntArray {
    return map { it.persistedValue }.toIntArray()
}

private fun List<BeatAccentType>.toPersistedString(): String {
    return joinToString(separator = ",") { it.persistedValue.toString() }
}

private fun String?.toBeatAccentTypes(): List<BeatAccentType> {
    if (isNullOrBlank()) return emptyList()

    return split(",")
        .mapNotNull { rawValue -> rawValue.toIntOrNull() }
        .map { BeatAccentType.fromPersistedValue(it) }
}

internal fun defaultAccentIntensityRanges(): List<AccentIntensityRange> {
    return AccentIntensityMode.entries.map { mode ->
        defaultAccentIntensityRange(mode)
    }
}

private fun defaultAccentIntensityRange(mode: AccentIntensityMode): AccentIntensityRange {
    return when (mode) {
        AccentIntensityMode.Big -> AccentIntensityRange(maxPercent = 100, minPercent = 80, valuePercent = 92)
        AccentIntensityMode.Medium -> AccentIntensityRange(maxPercent = 80, minPercent = 60, valuePercent = 72)
        AccentIntensityMode.Little -> AccentIntensityRange(maxPercent = 60, minPercent = 40, valuePercent = 52)
        AccentIntensityMode.Silent -> AccentIntensityRange(maxPercent = 40, minPercent = 0, valuePercent = 20)
    }
}

internal fun List<AccentIntensityRange>.rangeFor(mode: AccentIntensityMode): AccentIntensityRange {
    return normalizedAccentIntensityRanges().getOrNull(mode.ordinal)
        ?: defaultAccentIntensityRange(mode)
}

internal fun List<AccentIntensityRange>.withRangeFor(
    mode: AccentIntensityMode,
    range: AccentIntensityRange,
): List<AccentIntensityRange> {
    val normalizedRanges = normalizedAccentIntensityRanges()
    return AccentIntensityMode.entries.mapIndexed { index, entry ->
        if (entry == mode) range.normalized(entry) else normalizedRanges[index]
    }
}

private fun List<AccentIntensityRange>.normalizedAccentIntensityRanges(): List<AccentIntensityRange> {
    return AccentIntensityMode.entries.mapIndexed { index, mode ->
        (getOrNull(index) ?: defaultAccentIntensityRange(mode)).normalized(mode)
    }
}

private fun AccentIntensityRange.normalized(mode: AccentIntensityMode? = null): AccentIntensityRange {
    if (mode == AccentIntensityMode.Silent) {
        return AccentIntensityRange(
            maxPercent = 40,
            minPercent = 0,
            valuePercent = valuePercent.coerceIn(0, 40),
        )
    }

    val safeMax = maxPercent.coerceIn(0, 100)
    val safeMin = minPercent.coerceIn(0, safeMax)
    return AccentIntensityRange(
        maxPercent = safeMax,
        minPercent = safeMin,
        valuePercent = valuePercent.coerceIn(safeMin, safeMax),
    )
}

private fun List<AccentIntensityRange>.toMaxPercentIntArray(): IntArray {
    return normalizedAccentIntensityRanges()
        .map { it.maxPercent }
        .toIntArray()
}

private fun List<AccentIntensityRange>.toMinPercentIntArray(): IntArray {
    return normalizedAccentIntensityRanges()
        .map { it.minPercent }
        .toIntArray()
}

private fun List<AccentIntensityRange>.toValuePercentIntArray(): IntArray {
    return normalizedAccentIntensityRanges()
        .map { it.valuePercent }
        .toIntArray()
}

private fun List<AccentIntensityRange>.toPersistedRangeString(): String {
    return normalizedAccentIntensityRanges()
        .joinToString(separator = ",") { range -> "${range.maxPercent}:${range.minPercent}:${range.valuePercent}" }
}

private fun String?.toAccentIntensityRanges(): List<AccentIntensityRange> {
    if (isNullOrBlank()) return defaultAccentIntensityRanges()

    val ranges = split(",")
        .mapNotNull { rawRange ->
            val parts = rawRange.split(":")
            val maxPercent = parts.getOrNull(0)?.toIntOrNull()
            val minPercent = parts.getOrNull(1)?.toIntOrNull()
            val valuePercent = parts.getOrNull(2)?.toIntOrNull()
            if (maxPercent == null || minPercent == null) {
                null
            } else {
                AccentIntensityRange(
                    maxPercent = maxPercent,
                    minPercent = minPercent,
                    valuePercent = valuePercent ?: ((maxPercent + minPercent) / 2),
                )
            }
        }

    return ranges.normalizedAccentIntensityRanges()
}
