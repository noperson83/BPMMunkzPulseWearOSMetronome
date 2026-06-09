package bpm.munkz.pulse_wear.os.bpm.presentation

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import bpm.munkz.pulse_wear.os.bpm.R
import bpm.munkz.pulse_wear.os.bpm.presentation.theme.BPMMunkzPulseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val MIN_BPM = 30
internal const val MAX_BPM = 240
internal const val BEAT_FLASH_DURATION_MS = 80L
internal const val BEEP_DURATION_MS = 70
private const val APP_LAUNCH_SPLASH_DURATION_MS = 740L
private const val TAP_TEMPO_RESET_TIMEOUT_MS = 2_000L
private const val TAP_TEMPO_SAMPLE_COUNT = 5
private const val MAIN_PAGE_INDEX = 0
private const val RHYTHM_PAGE_INDEX = 1
private const val TAP_TEMPO_FREE_PAGE_INDEX = 2
private const val SETTINGS_PAGE_INDEX = 3
private const val FREE_TAP_PAGE_INDEX = 0
private const val FREE_SETTINGS_PAGE_INDEX = 1
private const val RHYTHM_APP_RHYTHM_PAGE_INDEX = 0
private const val RHYTHM_APP_SETTINGS_PAGE_INDEX = 1
private const val TUNE_TUNER_PAGE_INDEX = 0
private const val TUNE_SPECTRUM_PAGE_INDEX = 1
private const val TUNE_KEY_PAGE_INDEX = 2
private const val TUNE_SETTINGS_PAGE_INDEX = 3
internal const val ACTION_OPEN_SPECTRUM = "bpm.munkz.pulse_wear.os.bpm.action.OPEN_SPECTRUM"
internal const val EXTRA_OPEN_SPECTRUM = "bpm.munkz.pulse_wear.os.bpm.extra.OPEN_SPECTRUM"
private const val PAGER_TOUCH_VISUAL_QUIET_MS = 900L
private const val SETTINGS_PREFS = "bpm_munkz_settings"
private const val KEEP_SCREEN_AWAKE_WHILE_PLAYING_KEY = "keep_screen_awake_while_playing"
private const val KEEP_SCREEN_MODE_KEY = "keep_screen_mode"
private const val SETTINGS_A4_REFERENCE_HZ_KEY = "a4_reference_hz"
private const val SETTINGS_MAIN_COLOR_KEY = "main_color"
private const val SETTINGS_BACKGROUND_COLOR_KEY = "background_color"
private const val SETTINGS_CLOCK_COLOR_KEY = "clock_color"
private const val SETTINGS_CLOCK_IMAGE_INDEX_KEY = "clock_image_index"
private const val SETTINGS_RING_COLOR_KEY = "ring_color"
private const val SETTINGS_RING_MODE_KEY = "ring_mode"
private const val SETTINGS_LANGUAGE_INDEX_KEY = "language_index"
private const val FREE_SETTINGS_TRIAL_STARTED_DAY_KEY = "free_settings_trial_started_day"
private const val FREE_SETTINGS_TRIAL_DURATION_DAYS = 30
private const val FREE_SETTINGS_TRIAL_COOLDOWN_DAYS = 30
private const val FREE_SETTINGS_TRIAL_RESET_DAYS =
    FREE_SETTINGS_TRIAL_DURATION_DAYS + FREE_SETTINGS_TRIAL_COOLDOWN_DAYS
internal const val DEFAULT_TEMPO_NUDGE_MS = 200
internal const val MIN_TEMPO_NUDGE_MS = 50
internal const val MAX_TEMPO_NUDGE_MS = 250
internal const val TEMPO_NUDGE_STEP_MS = 50

internal object BeatTimingTrace {
    private const val TAG = "BPM_TIMING"

    private data class TimingEvent(
        val label: String,
        val deltaMs: Long,
    )

    private var beatId = 0L
    private var currentBeat = 0
    private var beatStartedAtMs = 0L
    private var events = mutableListOf<TimingEvent>()

    @Synchronized
    fun startBeat(
        beat: Int,
        bpm: Int,
        accentType: BeatAccentType,
    ) {
        beatId += 1L
        currentBeat = beat
        beatStartedAtMs = SystemClock.elapsedRealtime()
        events = mutableListOf(
            TimingEvent("beat tick b$beat ${accentType.name} @${bpm}bpm", 0L),
        )
        Log.d(TAG, "beat#$beatId high->low: ${formatEvents()}")
    }

    @Synchronized
    fun markForBeat(label: String, beat: Int) {
        if (currentBeat != beat) return
        mark(label)
    }

    @Synchronized
    fun mark(label: String) {
        if (beatStartedAtMs <= 0L) return
        if (events.any { it.label == label }) return

        events += TimingEvent(
            label = label,
            deltaMs = SystemClock.elapsedRealtime() - beatStartedAtMs,
        )
        Log.d(TAG, "beat#$beatId high->low: ${formatEvents()}")
    }

    private fun formatEvents(): String {
        return events
            .sortedByDescending { it.deltaMs }
            .joinToString(separator = " | ") { event ->
                "${event.label}=+${event.deltaMs}ms"
            }
    }
}
internal const val NEON_GREEN_COLOR = -6422784
private const val DEFAULT_MAIN_COLOR = NEON_GREEN_COLOR
private const val DEFAULT_BACKGROUND_COLOR = -16769244
private const val DEFAULT_CLOCK_COLOR = NEON_GREEN_COLOR
private const val DEFAULT_BIG_PULSE_RING_COLOR = NEON_GREEN_COLOR
private val DEFAULT_BIG_RING_FLASH_MODE = BigRingFlashMode.Big
private const val DEFAULT_CLOCK_IMAGE_INDEX = 6
private const val DEFAULT_LANGUAGE_INDEX = 0
internal val MusicalKeyRoots = listOf(
    "C",
    "Db",
    "D",
    "Eb",
    "E",
    "F",
    "Gb",
    "G",
    "Ab",
    "A",
    "Bb",
    "B",
)

val MusicalKeyModeSuffixes = listOf(
    "",
    "maj",
    "m",
    "min",
    " Dor",
    " Phr",
    " Lyd",
    " Mix",
    " Aeol",
    " Loc",
    "aug",
    "dim",
    "sus2",
    "sus4",
    "7",
    "maj7",
    "m7",
)

private val MusicalKeyOptions = MusicalKeyRoots.flatMap { root ->
    MusicalKeyModeSuffixes.map { suffix -> "$root$suffix" }
}

enum class AppLanguage {
    English,
    Spanish,
}

data class AppText(
    val tap: String,
    val start: String,
    val stop: String,
    val startUpper: String,
    val stopUpper: String,
    val settings: String,
    val tuner: String,
    val spectrum: String,
    val micAccess: String,
    val listen: String,
    val keyGuess: String,
    val saveKey: String,
    val saveBpm: String,
    val toClock: String,
    val rhythm: String,
    val timeSignature: String,
    val subdivision: String,
    val saveSong: String,
    val newSong: String,
    val beat: String,
    val beatCount: String,
    val bigPulse: String,
    val haptics: String,
    val beep: String,
    val wood: String,
    val bell: String,
    val drone: String,
    val droneVolume: String,
    val visualNudge: String,
    val intensity: String,
    val intensityTitle: String,
    val a4Reference: String,
    val diagnostics: String,
    val appCpu: String,
    val keepScreenOn: String,
    val keepScreenAppOpen: String,
    val keepScreenPlaying: String,
    val keepScreenWatchTimeout: String,
    val on: String,
    val off: String,
    val theme: String,
    val mainColor: String,
    val backgroundColor: String,
    val clock: String,
    val clockImage: String,
    val handColor: String,
    val bigRing: String,
    val language: String,
    val big: String,
    val edit: String,
    val editRhythm: String,
    val deleteSong: String,
    val editPlaylist: String,
    val newList: String,
    val song: String,
    val addSong: String,
    val key: String,
    val note: String,
    val done: String,
    val decreaseBpmBy5: String,
    val increaseBpmBy5: String,
)

internal val AppLanguages = listOf(
    AppLanguage.English,
    AppLanguage.Spanish,
)

internal fun BigRingFlashMode.shouldFlashRing(
    beatFlash: Boolean,
    flashingAccentType: BeatAccentType,
): Boolean {
    if (!beatFlash) return false

    return when (this) {
        BigRingFlashMode.All -> true
        BigRingFlashMode.Big -> flashingAccentType == BeatAccentType.Big
        BigRingFlashMode.Off -> false
    }
}

private fun appTextFor(language: AppLanguage): AppText {
    return when (language) {
        AppLanguage.English -> AppText(
            tap = "Tap",
            start = "Start",
            stop = "Stop",
            startUpper = "START",
            stopUpper = "STOP",
            settings = "Settings",
            tuner = "Tuner",
            spectrum = "Spectrum",
            micAccess = "Mic access",
            listen = "Listen",
            keyGuess = "Key",
            saveKey = "Save Key",
            saveBpm = "Save BPM",
            toClock = "Clock",
            rhythm = "Pulse",
            timeSignature = "Time Signature",
            subdivision = "Sub Divisions",
            saveSong = "Save Song",
            newSong = "New Song",
            beat = "Beat",
            beatCount = "Beat count",
            bigPulse = "Big pulse",
            haptics = "Vibe",
            beep = "Beep",
            wood = "DWood",
            bell = "Bell",
            drone = "Drone",
            droneVolume = "Drone Vol",
            visualNudge = "Tempo Push",
            intensity = "Beep & Vibe",
            intensityTitle = "Beep & Vibe\nIntensity",
            a4Reference = "A4 Ref",
            diagnostics = "Diagnostics",
            appCpu = "App CPU",
            keepScreenOn = "Keep screen on",
            keepScreenAppOpen = "App Open",
            keepScreenPlaying = "Playing",
            keepScreenWatchTimeout = "Timeout",
            on = "On",
            off = "Off",
            theme = "Theme",
            mainColor = "Main color",
            backgroundColor = "BG color",
            clock = "Clock",
            clockImage = "Clock image",
            handColor = "Clock Hand",
            bigRing = "Tap ring",
            language = "Language",
            big = "BIG",
            edit = "Edit",
            editRhythm = "Edit Rhythm",
            deleteSong = "Del",
            editPlaylist = "Edit Playlist",
            newList = "New List",
            song = "Song",
            addSong = "Add Song",
            key = "Key",
            note = "Note",
            done = "Done",
            decreaseBpmBy5 = "Decrease BPM by 5",
            increaseBpmBy5 = "Increase BPM by 5",
        )

        AppLanguage.Spanish -> AppText(
            tap = "Pulsar",
            start = "Iniciar",
            stop = "Parar",
            startUpper = "INICIAR",
            stopUpper = "PARAR",
            settings = "Ajustes",
            tuner = "Afinador",
            spectrum = "Espectro",
            micAccess = "Microfono",
            listen = "Escuchar",
            keyGuess = "Tono",
            saveKey = "Guardar tono",
            saveBpm = "Guardar BPM",
            toClock = "Reloj",
            rhythm = "Pulso",
            timeSignature = "Compas",
            subdivision = "Sub Divisiones",
            saveSong = "Guardar",
            newSong = "Nueva",
            beat = "Compas",
            beatCount = "Beats",
            bigPulse = "Pulso grande",
            haptics = "Vibra",
            beep = "Pitido",
            wood = "DWood",
            bell = "Bell",
            drone = "Drone",
            droneVolume = "Vol Drone",
            visualNudge = "Empuje tempo",
            intensity = "Pitido y Vibra",
            intensityTitle = "Pitido/Vibra\nIntensidad",
            a4Reference = "Ref A4",
            diagnostics = "Diagnostico",
            appCpu = "CPU app",
            keepScreenOn = "Pantalla activa",
            keepScreenAppOpen = "App abierta",
            keepScreenPlaying = "Tocando",
            keepScreenWatchTimeout = "Normal",
            on = "Si",
            off = "No",
            theme = "Tema",
            mainColor = "Color base",
            backgroundColor = "Color fondo",
            clock = "Reloj",
            clockImage = "Imagen reloj",
            handColor = "Mano de reloj",
            bigRing = "Aro tempo",
            language = "Idioma",
            big = "GRAN",
            edit = "Editar",
            editRhythm = "Editar ritmo",
            deleteSong = " Borrar",
            editPlaylist = "Editar lista",
            newList = "Nueva",
            song = "Cancion",
            addSong = "Agregar",
            key = "Tono",
            note = "Nota",
            done = "Listo",
            decreaseBpmBy5 = "Bajar BPM por 5",
            increaseBpmBy5 = "Subir BPM por 5",
        )
    }
}

class MainActivity : ComponentActivity() {
    private var openSpectrumRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openSpectrumRequest = intent.shouldOpenSpectrum()
        setContent {
            WearApp(
                openSpectrumRequest = openSpectrumRequest,
                onOpenSpectrumRequestConsumed = { openSpectrumRequest = false },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.shouldOpenSpectrum()) {
            openSpectrumRequest = true
        }
    }
}

@Composable
fun WearApp(
    openSpectrumRequest: Boolean = false,
    onOpenSpectrumRequestConsumed: () -> Unit = {},
) {
    BPMMunkzPulseTheme {
        var showLaunchSplash by rememberSaveable { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            delay(APP_LAUNCH_SPLASH_DURATION_MS)
            showLaunchSplash = false
        }

        if (showLaunchSplash) {
            AppLaunchSplashScreen()
        } else {
            AppScaffold {
                BeatPulseScreen(
                    openSpectrumRequest = openSpectrumRequest,
                    onOpenSpectrumRequestConsumed = onOpenSpectrumRequestConsumed,
                )
            }
        }
    }
}

private fun Intent?.shouldOpenSpectrum(): Boolean {
    return this?.action == ACTION_OPEN_SPECTRUM || this?.getBooleanExtra(EXTRA_OPEN_SPECTRUM, false) == true
}

@Composable
private fun AppLaunchSplashScreen() {
    val logoScale = remember { Animatable(1.38f) }

    LaunchedEffect(Unit) {
        logoScale.snapTo(1.38f)
        logoScale.animateTo(
            targetValue = 0.72f,
            animationSpec = tween(
                durationMillis = 620,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.bpm_munkz_app_logo_metronome),
            contentDescription = "BPM Munkz Pulse",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.82f)
                .scale(logoScale.value),
        )
    }
}

@Composable
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
fun BeatPulseScreen(
    openSpectrumRequest: Boolean = false,
    onOpenSpectrumRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val activity = remember(context) { context.findActivity() }
    var metronomeService by remember { mutableStateOf<MetronomeService?>(null) }
    var pointerIsDown by remember { mutableStateOf(false) }
    var quietPulseVisualsUntilMs by remember { mutableLongStateOf(0L) }
    var metronomeState by remember(context, isPreview) {
        mutableStateOf(
            if (isPreview) {
                MetronomeState()
            } else {
                context.loadSavedRhythmState()
            },
        )
    }
    val bpm = metronomeState.bpm
    val isRunning = metronomeState.isRunning
    val beatFlash = metronomeState.beatFlash
    val flashingBeat = metronomeState.flashingBeat
    val beatClockStartedAtMs = metronomeState.beatClockStartedAtMs
    val playbackStartedAtMs = metronomeState.playbackStartedAtMs
    val beatsPerMeasure = metronomeState.beatsPerMeasure
    val accentBeat = metronomeState.accentBeat
    val subdivisionCount = metronomeState.subdivisionCount
    val beatAccentTypes = metronomeState.beatAccentTypes
    val currentBeatIndex = metronomeState.currentBeatIndex
    val currentSubdivisionIndex = metronomeState.currentSubdivisionIndex
    val accentIntensityMode = metronomeState.accentIntensityMode
    val hapticsEnabled = metronomeState.hapticsEnabled
    val beepEnabled = metronomeState.beepEnabled
    val beatSoundMode = metronomeState.beatSoundMode
    val keyDroneEnabled = metronomeState.keyDroneEnabled
    val keyDroneVolumePercent = metronomeState.keyDroneVolumePercent
    val tempoNudgeMs = metronomeState.tempoNudgeMs
    var keepScreenMode by rememberSaveable {
        mutableStateOf(if (isPreview) KeepScreenMode.WatchTimeout else context.loadKeepScreenMode())
    }
    var a4ReferenceHz by rememberSaveable {
        mutableIntStateOf(
            if (isPreview) {
                DEFAULT_A4_REFERENCE_HZ
            } else {
                context.loadSettingsInt(
                    SETTINGS_A4_REFERENCE_HZ_KEY,
                    DEFAULT_A4_REFERENCE_HZ,
                )
            }.coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ),
        )
    }
    var mainColorArgb by rememberSaveable {
        mutableIntStateOf(
            if (isPreview) DEFAULT_MAIN_COLOR else context.loadSettingsInt(
                SETTINGS_MAIN_COLOR_KEY,
                DEFAULT_MAIN_COLOR,
            ),
        )
    }
    var backgroundColorArgb by rememberSaveable {
        mutableIntStateOf(
            if (isPreview) DEFAULT_BACKGROUND_COLOR else context.loadSettingsInt(
                SETTINGS_BACKGROUND_COLOR_KEY,
                DEFAULT_BACKGROUND_COLOR,
            ),
        )
    }
    var clockColorArgb by rememberSaveable {
        mutableIntStateOf(
            if (isPreview) DEFAULT_CLOCK_COLOR else context.loadSettingsInt(
                SETTINGS_CLOCK_COLOR_KEY,
                DEFAULT_CLOCK_COLOR,
            ),
        )
    }
    val clockImageIndexState = rememberSaveable {
        mutableIntStateOf(
            if (isPreview) {
                DEFAULT_CLOCK_IMAGE_INDEX
            } else {
                context.loadSettingsInt(
                    SETTINGS_CLOCK_IMAGE_INDEX_KEY,
                    DEFAULT_CLOCK_IMAGE_INDEX,
                )
            },
        )
    }
    var bigPulseRingColorArgb by rememberSaveable {
        mutableIntStateOf(
            if (isPreview) DEFAULT_BIG_PULSE_RING_COLOR else context.loadSettingsInt(
                SETTINGS_RING_COLOR_KEY,
                DEFAULT_BIG_PULSE_RING_COLOR,
            ),
        )
    }
    val bigRingModeState = rememberSaveable {
        mutableIntStateOf(
            if (isPreview) {
                DEFAULT_BIG_RING_FLASH_MODE.persistedValue
            } else {
                context.loadSettingsInt(
                    SETTINGS_RING_MODE_KEY,
                    DEFAULT_BIG_RING_FLASH_MODE.persistedValue,
                )
            },
        )
    }
    val appLanguageIndexState = rememberSaveable {
        mutableIntStateOf(
            if (isPreview) {
                DEFAULT_LANGUAGE_INDEX
            } else {
                context.loadSettingsInt(
                    SETTINGS_LANGUAGE_INDEX_KEY,
                    DEFAULT_LANGUAGE_INDEX,
                )
            },
        )
    }
    var playlists by remember(context, isPreview) {
        mutableStateOf(
            if (isPreview) {
                defaultSavedPlaylists()
            } else {
                context.loadSavedPlaylists()
            },
        )
    }
    val initialPlaylistIndex = metronomeState.playlistIndex.coerceIn(0, playlists.lastIndex)
    val initialSongIndex = metronomeState.songIndex.coerceIn(
        0,
        playlists[initialPlaylistIndex].songs.lastIndex,
    )
    val selectedPlaylistIndexState = rememberSaveable { mutableIntStateOf(initialPlaylistIndex) }
    val selectedSongIndexState = rememberSaveable { mutableIntStateOf(initialSongIndex) }
    var playlistEditorPopupOpen by rememberSaveable { mutableStateOf(false) }
    var playlistRhythmEditorPopupOpen by rememberSaveable { mutableStateOf(false) }
    var rhythmEditorPopupOpen by rememberSaveable { mutableStateOf(false) }
    var rhythmEditorDraft by remember { mutableStateOf<MetronomeState?>(null) }
    var tapTempoPopupOpen by rememberSaveable { mutableStateOf(false) }
    var freeTapKeyPickerOpen by rememberSaveable { mutableStateOf(false) }
    var freeTapTimeSignaturePickerOpen by rememberSaveable { mutableStateOf(false) }
    var tunerOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var spectrumOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var keyOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var bpmReaderOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var fftOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var tunerListenProfile by rememberSaveable { mutableStateOf(TunerListenProfile.Full) }
    var spectrumReaderMode by rememberSaveable { mutableStateOf(SpectrumReaderMode.Instrument) }
    var spectrumTuningChoice by rememberSaveable { mutableStateOf<SpectrumTuningChoice?>(null) }
    val tapTempoTimes = remember { mutableListOf<Long>() }
    val appFeatures = AppEditionConfig.features
    val pagerState = rememberPagerState(pageCount = { appFeatures.pageCount })
    val pagerScope = rememberCoroutineScope()
    var tunePageIndex by rememberSaveable { mutableIntStateOf(TUNE_TUNER_PAGE_INDEX) }
    val playlistIndex = selectedPlaylistIndexState.intValue.coerceIn(0, playlists.lastIndex)
    val currentPlaylist = playlists[playlistIndex]
    val songIndex = selectedSongIndexState.intValue.coerceIn(0, currentPlaylist.songs.lastIndex)
    val currentSong = currentPlaylist.songs[songIndex]
    val selectedClockImageIndex = clockImageIndexState.intValue.coerceIn(0, ClockImageChoices.lastIndex)
    val bigRingFlashMode = BigRingFlashMode.fromPersistedValue(bigRingModeState.intValue)
    val appLanguage = AppLanguages[appLanguageIndexState.intValue.coerceIn(0, AppLanguages.lastIndex)]
    val appText = appTextFor(appLanguage)
    val todayEpochDay = currentEpochDay()
    var freeSettingsTrialStartedDay by rememberSaveable {
        mutableLongStateOf(
            if (isPreview) 0L else context.loadSettingsLong(FREE_SETTINGS_TRIAL_STARTED_DAY_KEY, 0L),
        )
    }
    val freeSettingsTrialState = freeSettingsTrialState(
        trialStartedDay = freeSettingsTrialStartedDay,
        todayEpochDay = todayEpochDay,
    )
    val appCpuUsagePercent = rememberAppCpuUsagePercent(enabled = !isPreview)
    var micPermissionGranted by remember(context, isPreview) {
        mutableStateOf(
            isPreview ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        micPermissionGranted = granted
    }
    val audioOverlayOpen = tunerOverlayOpen || spectrumOverlayOpen || keyOverlayOpen || bpmReaderOverlayOpen || fftOverlayOpen
    val tuneAudioOpen = appFeatures.isTuneOnly && tunePageIndex != TUNE_SETTINGS_PAGE_INDEX

    LaunchedEffect(openSpectrumRequest) {
        if (openSpectrumRequest) {
            tapTempoPopupOpen = false
            tunerOverlayOpen = false
            bpmReaderOverlayOpen = false
            fftOverlayOpen = false
            keyOverlayOpen = false
            playlistEditorPopupOpen = false
            playlistRhythmEditorPopupOpen = false
            rhythmEditorPopupOpen = false
            if (appFeatures.isTuneOnly) {
                tunePageIndex = TUNE_SPECTRUM_PAGE_INDEX
            } else {
                spectrumOverlayOpen = appFeatures.showSpectrumEntry
            }
            onOpenSpectrumRequestConsumed()
        }
    }

    val audioAnalysisState = rememberAudioAnalysisState(
        enabled = micPermissionGranted && (audioOverlayOpen || tuneAudioOpen) && !isPreview && !appFeatures.isFreeOnly,
        listenProfile = tunerListenProfile,
        readerMode = spectrumReaderMode,
        tuningChoice = spectrumTuningChoice,
        a4ReferenceHz = a4ReferenceHz,
        includeSpectrum = spectrumOverlayOpen || fftOverlayOpen ||
            appFeatures.isTuneOnly &&
            (tunePageIndex == TUNE_TUNER_PAGE_INDEX || tunePageIndex == TUNE_SPECTRUM_PAGE_INDEX),
    )
    val clockImageResId = if (isPreview) {
        R.drawable.clock_dial_all_colors
    } else {
        clockImageResIdForIndex(selectedClockImageIndex)
    }
    val mainColor = Color(mainColorArgb)
    val backgroundColor = Color(backgroundColorArgb)
    val onBackgroundColor = readableTextColorFor(backgroundColorArgb)
    val onMainColor = readableTextColorFor(mainColorArgb)
    val colorScheme = MaterialTheme.colorScheme.copy(
        primary = mainColor,
        primaryDim = mainColor.copy(alpha = 0.72f),
        primaryContainer = mainColor.copy(alpha = 0.82f),
        onPrimary = onMainColor,
        onPrimaryContainer = onMainColor,
        secondary = mainColor.copy(alpha = 0.7f),
        secondaryContainer = mainColor.copy(alpha = 0.28f),
        onSecondary = onMainColor,
        onSecondaryContainer = onBackgroundColor,
        surfaceContainerLow = backgroundColor.copy(alpha = 0.82f),
        surfaceContainer = mainColor.copy(alpha = 0.22f),
        surfaceContainerHigh = mainColor.copy(alpha = 0.3f),
        onSurface = onBackgroundColor,
        onSurfaceVariant = onBackgroundColor.copy(alpha = 0.78f),
        outline = mainColor,
        outlineVariant = mainColor.copy(alpha = 0.54f),
        background = backgroundColor,
        onBackground = onBackgroundColor,
    )

    DisposableEffect(context, isPreview) {
        if (isPreview) {
            onDispose { }
        } else {
            val appContext = context.applicationContext
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val boundService = (service as? MetronomeService.LocalBinder)?.service
                    metronomeService = boundService
                    boundService?.let {
                        val serviceState = it.state.value
                        metronomeState = serviceState
                        val servicePlaylistIndex = serviceState.playlistIndex.coerceIn(0, playlists.lastIndex)
                        selectedPlaylistIndexState.intValue = servicePlaylistIndex
                        selectedSongIndexState.intValue = serviceState.songIndex.coerceIn(
                            0,
                            playlists[servicePlaylistIndex].songs.lastIndex,
                        )
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    metronomeService = null
                }
            }

            appContext.bindService(
                Intent(appContext, MetronomeService::class.java),
                connection,
                Context.BIND_AUTO_CREATE,
            )

            onDispose {
                appContext.unbindService(connection)
                metronomeService = null
            }
        }
    }

    fun pagerIsMovingOrBetweenPages(): Boolean {
        return pagerState.isScrollInProgress ||
            abs(pagerState.currentPageOffsetFraction) > 0.01f
    }

    fun pointerIsPreparingGesture(): Boolean {
        return pointerIsDown ||
            SystemClock.elapsedRealtime() < quietPulseVisualsUntilMs
    }

    fun pageShowsRhythmPulse(page: Int): Boolean {
        return if (appFeatures.isRhythmOnly) {
            page == RHYTHM_APP_RHYTHM_PAGE_INDEX
        } else {
            page == RHYTHM_PAGE_INDEX
        }
    }

    fun shouldShowPagerBigRing(): Boolean {
        return bigRingFlashMode != BigRingFlashMode.Off &&
            !appFeatures.isTuneOnly &&
            pagerState.currentPage in MAIN_PAGE_INDEX until appFeatures.pageCount &&
            !pagerIsMovingOrBetweenPages() &&
            !pointerIsPreparingGesture() &&
            !tapTempoPopupOpen &&
            !freeTapKeyPickerOpen &&
            !freeTapTimeSignaturePickerOpen &&
            !audioOverlayOpen &&
            !rhythmEditorPopupOpen &&
            !tunerOverlayOpen &&
            !spectrumOverlayOpen &&
            !playlistEditorPopupOpen
    }

    fun filterMetronomeStateForVisibleUi(state: MetronomeState): MetronomeState {
        if (!state.isRunning) return state

        val currentPage = pagerState.currentPage
        val pagerIsMovingOrBetweenPages = pagerIsMovingOrBetweenPages()
        val pointerIsPreparingGesture = pointerIsPreparingGesture()

        if (pagerIsMovingOrBetweenPages || pointerIsPreparingGesture) {
            return state.withPulseVisualsFrom(metronomeState)
        }

        val shouldShowFullPulse =
            shouldShowPagerBigRing() ||
            (!playlistEditorPopupOpen || playlistRhythmEditorPopupOpen || rhythmEditorPopupOpen) &&
            (
                tapTempoPopupOpen ||
                    playlistRhythmEditorPopupOpen ||
                    rhythmEditorPopupOpen ||
                    pageShowsRhythmPulse(currentPage) ||
                    currentPage == TAP_TEMPO_FREE_PAGE_INDEX
                )
        val shouldShowLightClockPulse = !playlistEditorPopupOpen &&
            currentPage == MAIN_PAGE_INDEX

        val filteredState = when {
            shouldShowFullPulse -> state
            shouldShowLightClockPulse -> state.withLightClockVisualsFrom(metronomeState)
            else -> state.withPulseVisualsFrom(metronomeState)
        }

        return filteredState
    }

    LaunchedEffect(
        metronomeService,
        tapTempoPopupOpen,
        playlistEditorPopupOpen,
        playlistRhythmEditorPopupOpen,
        rhythmEditorPopupOpen,
        freeTapKeyPickerOpen,
        freeTapTimeSignaturePickerOpen,
        audioOverlayOpen,
        tunerOverlayOpen,
        spectrumOverlayOpen,
        bpmReaderOverlayOpen,
        bigRingFlashMode,
    ) {
        metronomeService?.state?.collect { state ->
            val nextState = filterMetronomeStateForVisibleUi(state)
            val shouldUpdateRingVisuals = shouldShowPagerBigRing() &&
                nextState.hasDifferentPulseVisualsFrom(metronomeState)
            val shouldUpdateNow = !state.isRunning ||
                !nextState.hasSameNonVisualStateAs(metronomeState) ||
                shouldUpdateRingVisuals

            if (shouldUpdateNow && nextState != metronomeState) {
                metronomeState = nextState
            }
        }
    }

    LaunchedEffect(
        metronomeService,
        pagerState,
        tapTempoPopupOpen,
        playlistEditorPopupOpen,
        playlistRhythmEditorPopupOpen,
        rhythmEditorPopupOpen,
        freeTapKeyPickerOpen,
        freeTapTimeSignaturePickerOpen,
        audioOverlayOpen,
        tunerOverlayOpen,
        spectrumOverlayOpen,
        bpmReaderOverlayOpen,
        bigRingFlashMode,
    ) {
        snapshotFlow {
            pagerState.isScrollInProgress || abs(pagerState.currentPageOffsetFraction) > 0.01f
        }.collect { pagerIsMovingOrBetweenPages ->
            if (!pagerIsMovingOrBetweenPages) {
                metronomeService?.state?.value?.let { state ->
                    val nextState = filterMetronomeStateForVisibleUi(state)
                    val shouldUpdateRingVisuals = shouldShowPagerBigRing() &&
                        nextState.hasDifferentPulseVisualsFrom(metronomeState)
                    if (
                        (
                            !state.isRunning ||
                                !nextState.hasSameNonVisualStateAs(metronomeState) ||
                                shouldUpdateRingVisuals
                            ) &&
                        nextState != metronomeState
                    ) {
                        metronomeState = nextState
                    }
                }
            }
        }
    }

    LaunchedEffect(
        metronomeState.isRunning,
        metronomeState.playlistIndex,
        metronomeState.songIndex,
        playlists,
    ) {
        if (metronomeState.isRunning) {
            val servicePlaylistIndex = metronomeState.playlistIndex.coerceIn(0, playlists.lastIndex)
            selectedPlaylistIndexState.intValue = servicePlaylistIndex
            selectedSongIndexState.intValue = metronomeState.songIndex.coerceIn(
                0,
                playlists[servicePlaylistIndex].songs.lastIndex,
            )
        }
    }

    val clearTapTempo = {
        tapTempoTimes.clear()
    }

    fun applySongToMetronome(
        nextPlaylistIndex: Int,
        nextSongIndex: Int,
        song: PlaylistSong,
        restartBeat: Boolean,
    ) {
        metronomeService?.setPlaylistItem(
            playlistIndex = nextPlaylistIndex,
            songIndex = nextSongIndex,
            bpm = song.bpm,
            beatsPerMeasure = song.beatsPerMeasure,
            accentBeat = song.accentBeat,
            subdivisionCount = song.subdivisionCount,
            beatAccentTypes = song.beatAccentTypes,
            accentIntensityMode = song.accentIntensityMode,
            musicalKey = song.musicalKey,
            tempoNudgeMs = tempoNudgeMs,
            restartBeat = restartBeat,
        )
    }

    fun selectPlaylist(index: Int) {
        val nextPlaylistIndex = index.wrap(playlists.size)
        val nextPlaylist = playlists[nextPlaylistIndex]
        selectedPlaylistIndexState.intValue = nextPlaylistIndex
        selectedSongIndexState.intValue = 0
        applySongToMetronome(nextPlaylistIndex, 0, nextPlaylist.songs.first(), restartBeat = isRunning)
    }

    fun selectSong(index: Int) {
        val nextSongIndex = index.wrap(currentPlaylist.songs.size)
        selectedSongIndexState.intValue = nextSongIndex
        applySongToMetronome(
            nextPlaylistIndex = playlistIndex,
            nextSongIndex = nextSongIndex,
            song = currentPlaylist.songs[nextSongIndex],
            restartBeat = isRunning,
        )
    }

    fun updateCurrentSong(update: (PlaylistSong) -> PlaylistSong) {
        playlists = playlists.updateSong(playlistIndex, songIndex, update)
    }

    fun setCurrentSongTimeSignature(beatChoice: Int) {
        val nextBeatChoice = beatChoice.coerceIn(2, 16)
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(beatsPerMeasure = nextBeatChoice)
        }
        playlists = nextPlaylists
        if (!isPreview) {
            context.saveSavedPlaylists(nextPlaylists)
        }
        metronomeService?.setPlaylistItem(
            playlistIndex = playlistIndex,
            songIndex = songIndex,
            bpm = currentSong.bpm,
            beatsPerMeasure = nextBeatChoice,
            accentBeat = currentSong.accentBeat.coerceIn(1, nextBeatChoice),
            subdivisionCount = currentSong.subdivisionCount,
            beatAccentTypes = currentSong.beatAccentTypes,
            accentIntensityMode = currentSong.accentIntensityMode,
            musicalKey = currentSong.musicalKey,
            restartBeat = isRunning,
        )
    }

    fun setCurrentSongSubdivision(subdivision: Int) {
        val nextSubdivision = subdivision.toSupportedPulseSubdivisionCount()
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(subdivisionCount = nextSubdivision)
        }
        playlists = nextPlaylists
        if (!isPreview) {
            context.saveSavedPlaylists(nextPlaylists)
        }
        metronomeService?.setPlaylistItem(
            playlistIndex = playlistIndex,
            songIndex = songIndex,
            bpm = currentSong.bpm,
            beatsPerMeasure = currentSong.beatsPerMeasure,
            accentBeat = currentSong.accentBeat,
            subdivisionCount = nextSubdivision,
            beatAccentTypes = currentSong.beatAccentTypes,
            accentIntensityMode = currentSong.accentIntensityMode,
            musicalKey = currentSong.musicalKey,
            restartBeat = isRunning,
        )
    }

    fun setCurrentSongRhythmPreset(
        beatsPerMeasureChoice: Int,
        beatAccentTypeChoices: List<BeatAccentType>,
        subdivisionChoice: Int,
    ) {
        val nextBeatsPerMeasure = beatsPerMeasureChoice.coerceIn(2, 16)
        val nextBeatAccentTypes = beatAccentTypeChoices.take(nextBeatsPerMeasure)
        val nextSubdivisionCount = subdivisionChoice.toSupportedPulseSubdivisionCount()
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(
                beatsPerMeasure = nextBeatsPerMeasure,
                accentBeat = 1,
                subdivisionCount = nextSubdivisionCount,
                beatAccentTypes = nextBeatAccentTypes,
            )
        }
        playlists = nextPlaylists
        if (!isPreview) {
            context.saveSavedPlaylists(nextPlaylists)
        }
        metronomeService?.setPlaylistItem(
            playlistIndex = playlistIndex,
            songIndex = songIndex,
            bpm = currentSong.bpm,
            beatsPerMeasure = nextBeatsPerMeasure,
            accentBeat = 1,
            subdivisionCount = nextSubdivisionCount,
            beatAccentTypes = nextBeatAccentTypes,
            accentIntensityMode = currentSong.accentIntensityMode,
            musicalKey = currentSong.musicalKey,
            restartBeat = isRunning,
        )
    }

    fun applyRhythmOnlyState(
        nextState: MetronomeState,
        restartBeat: Boolean,
    ) {
        metronomeState = nextState
        if (!isPreview) {
            context.saveRhythmState(nextState)
        }
        metronomeService?.setPlaylistItem(
            playlistIndex = nextState.playlistIndex,
            songIndex = nextState.songIndex,
            bpm = nextState.bpm,
            beatsPerMeasure = nextState.beatsPerMeasure,
            accentBeat = nextState.accentBeat,
            subdivisionCount = nextState.subdivisionCount,
            beatAccentTypes = nextState.beatAccentTypes,
            accentIntensityMode = nextState.accentIntensityMode,
            accentIntensityRanges = nextState.accentIntensityRanges,
            musicalKey = nextState.musicalKey,
            tempoNudgeMs = nextState.tempoNudgeMs,
            restartBeat = restartBeat,
        )
    }

    fun setRhythmOnlyPreset(
        beatsPerMeasureChoice: Int,
        beatAccentTypeChoices: List<BeatAccentType>,
        subdivisionChoice: Int,
    ) {
        val nextBeatsPerMeasure = beatsPerMeasureChoice.coerceIn(2, 16)
        val nextBeatAccentTypes = beatAccentTypeChoices.take(nextBeatsPerMeasure)
        val nextSubdivisionCount = subdivisionChoice.toSupportedPulseSubdivisionCount()
        applyRhythmOnlyState(
            metronomeState.copy(
                beatsPerMeasure = nextBeatsPerMeasure,
                accentBeat = nextBeatAccentTypes.primaryRhythmAccentBeat(),
                subdivisionCount = nextSubdivisionCount,
                beatAccentTypes = nextBeatAccentTypes,
            ),
            restartBeat = isRunning,
        )
    }

    fun updateRhythmEditorDraft(update: (MetronomeState) -> MetronomeState) {
        rhythmEditorDraft = update(rhythmEditorDraft ?: metronomeState)
    }

    fun startRhythmEditor() {
        if (appFeatures.isRhythmOnly) {
            rhythmEditorDraft = metronomeState
        }
        rhythmEditorPopupOpen = true
    }

    fun cancelRhythmEditorAndClose() {
        rhythmEditorDraft = null
        rhythmEditorPopupOpen = false
    }

    fun commitRhythmEditorAndClose() {
        if (appFeatures.isRhythmOnly) {
            rhythmEditorDraft?.let { draft ->
                applyRhythmOnlyState(draft, restartBeat = isRunning)
            }
            rhythmEditorDraft = null
            rhythmEditorPopupOpen = false
        } else {
            val rhythmState = metronomeService?.state?.value ?: metronomeState
            val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
                song.copy(
                    bpm = rhythmState.bpm,
                    beatsPerMeasure = rhythmState.beatsPerMeasure,
                    accentBeat = rhythmState.accentBeat,
                    subdivisionCount = rhythmState.subdivisionCount,
                    beatAccentTypes = rhythmState.beatAccentTypes,
                    accentIntensityMode = rhythmState.accentIntensityMode,
                )
            }
            playlists = nextPlaylists
            if (!isPreview) {
                context.saveSavedPlaylists(nextPlaylists)
            }
            rhythmEditorPopupOpen = false
        }
    }

    fun setCurrentSongKey(key: String) {
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(musicalKey = key)
        }
        playlists = nextPlaylists
        if (!isPreview) {
            context.saveSavedPlaylists(nextPlaylists)
        }
        metronomeService?.setPlaylistItem(
            playlistIndex = playlistIndex,
            songIndex = songIndex,
            bpm = currentSong.bpm,
            beatsPerMeasure = currentSong.beatsPerMeasure,
            accentBeat = currentSong.accentBeat,
            subdivisionCount = currentSong.subdivisionCount,
            beatAccentTypes = currentSong.beatAccentTypes,
            accentIntensityMode = currentSong.accentIntensityMode,
            musicalKey = key,
            restartBeat = false,
        )
    }

    fun saveCurrentRhythmToSong() {
        if (appFeatures.isRhythmOnly) {
            return
        }
        val rhythmState = metronomeService?.state?.value ?: metronomeState
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(
                bpm = rhythmState.bpm,
                beatsPerMeasure = rhythmState.beatsPerMeasure,
                accentBeat = rhythmState.accentBeat,
                subdivisionCount = rhythmState.subdivisionCount,
                beatAccentTypes = rhythmState.beatAccentTypes,
                accentIntensityMode = rhythmState.accentIntensityMode,
            )
        }
        playlists = nextPlaylists
        if (!isPreview) {
            context.saveSavedPlaylists(nextPlaylists)
        }
    }

    fun saveCurrentBpmToSong() {
        val currentBpm = (metronomeService?.state?.value ?: metronomeState).bpm
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(bpm = currentBpm)
        }
        playlists = nextPlaylists
        if (!isPreview) {
            context.saveSavedPlaylists(nextPlaylists)
        }
    }

    fun saveDetectedBpmToSong(detectedBpm: Int) {
        val nextBpm = detectedBpm.coerceIn(MIN_BPM, MAX_BPM)
        metronomeService?.setBpm(nextBpm, restartBeat = isRunning)
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(bpm = nextBpm)
        }
        playlists = nextPlaylists
        if (!isPreview) {
            context.saveSavedPlaylists(nextPlaylists)
        }
    }

    fun savePlaylistEditorAndClose() {
        if (!isPreview) {
            context.saveSavedPlaylists(playlists)
        }
        playlistRhythmEditorPopupOpen = false
        playlistEditorPopupOpen = false
    }

    fun savePlaylistRhythmEditorAndClose() {
        saveCurrentRhythmToSong()
        playlistRhythmEditorPopupOpen = false
    }

    fun saveRhythmEditorAndClose() {
        saveCurrentRhythmToSong()
        rhythmEditorPopupOpen = false
    }

    val toggleRunning = {
        clearTapTempo()
        if (isRunning) {
            metronomeState = metronomeState.copy(
                isRunning = false,
                beatFlash = false,
                flashingBeat = 0,
                currentBeatIndex = 1,
                currentSubdivisionIndex = 1,
                playbackStartedAtMs = 0L,
            )
            metronomeService?.stopPlayback() ?: MetronomeService.stop(context)
        } else {
            val now = SystemClock.elapsedRealtime()
            val nextState = metronomeState.copy(
                isRunning = true,
                beatFlash = false,
                flashingBeat = 0,
                currentBeatIndex = 1,
                currentSubdivisionIndex = 1,
                beatClockStartedAtMs = now,
                playbackStartedAtMs = now,
                playlistIndex = playlistIndex,
                songIndex = songIndex,
                musicalKey = currentSong.musicalKey,
                tempoNudgeMs = tempoNudgeMs,
            )
            metronomeState = nextState
            MetronomeService.start(
                context,
                nextState,
            )
        }
    }

    val recordTapTempo = {
        val now = SystemClock.elapsedRealtime()
        var updatedBpm = false

        if (tapTempoTimes.isNotEmpty() && now - tapTempoTimes.last() > TAP_TEMPO_RESET_TIMEOUT_MS) {
            tapTempoTimes.clear()
        }

        tapTempoTimes.add(now)
        while (tapTempoTimes.size > TAP_TEMPO_SAMPLE_COUNT) {
            tapTempoTimes.removeAt(0)
        }

        if (tapTempoTimes.size >= 2) {
            val averageIntervalMs = tapTempoTimes
                .zipWithNext { previous, current -> current - previous }
                .average()

            if (averageIntervalMs > 0.0) {
                metronomeService?.setBpm(
                    bpm = (60_000.0 / averageIntervalMs).roundToInt().coerceIn(MIN_BPM, MAX_BPM),
                    restartBeat = isRunning,
                )
                updatedBpm = true
            }
        }

        if (isRunning && !updatedBpm) {
            metronomeService?.syncBeatClock()
        }
    }

    val decreaseBpm: () -> Unit = {
        clearTapTempo()
        metronomeService?.setBpm((bpm - 1).coerceAtLeast(MIN_BPM), restartBeat = isRunning)
    }

    val decreaseBpmLarge: () -> Unit = {
        clearTapTempo()
        metronomeService?.setBpm((bpm - 5).coerceAtLeast(MIN_BPM), restartBeat = isRunning)
    }

    val increaseBpm: () -> Unit = {
        clearTapTempo()
        metronomeService?.setBpm((bpm + 1).coerceAtMost(MAX_BPM), restartBeat = isRunning)
    }

    val increaseBpmLarge: () -> Unit = {
        clearTapTempo()
        metronomeService?.setBpm((bpm + 5).coerceAtMost(MAX_BPM), restartBeat = isRunning)
    }

    LaunchedEffect(playlists, isPreview) {
        if (!isPreview) {
            context.saveSavedPlaylists(playlists)
        }
    }

    LaunchedEffect(keepScreenMode, isPreview) {
        if (!isPreview) {
            context.saveKeepScreenMode(keepScreenMode)
        }
    }

    LaunchedEffect(
        pagerState.currentPage,
        currentSong.bpm,
        currentSong.beatsPerMeasure,
        currentSong.accentBeat,
        currentSong.subdivisionCount,
        currentSong.beatAccentTypes,
        currentSong.accentIntensityMode,
        playlistIndex,
        songIndex,
    ) {
        if (!appFeatures.isRhythmOnly && pagerState.currentPage == MAIN_PAGE_INDEX && !isRunning) {
            applySongToMetronome(playlistIndex, songIndex, currentSong, restartBeat = false)
        }
    }

    LaunchedEffect(isRunning, keepScreenMode, activity, isPreview) {
        if (isPreview) return@LaunchedEffect

        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val shouldKeepScreenOn = when (keepScreenMode) {
            KeepScreenMode.AppOpen -> true
            KeepScreenMode.Playing -> isRunning
            KeepScreenMode.WatchTimeout -> false
        }
        if (shouldKeepScreenOn) {
            activity?.window?.addFlags(keepScreenOnFlag)
        } else {
            activity?.window?.clearFlags(keepScreenOnFlag)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        @Composable
        fun SimpleSettingsSurface(
            showBuyNowButton: Boolean,
            settingsEnabled: Boolean,
        ) {
            val effectiveSettingsEnabled = settingsEnabled || freeSettingsTrialState.settingsEnabled
            SimpleSettingsPage(
                appText = appText,
                hapticsEnabled = hapticsEnabled,
                beepEnabled = beepEnabled,
                beatSoundMode = beatSoundMode,
                beatsPerMeasure = if (appFeatures.isRhythmOnly) null else currentSong.beatsPerMeasure,
                beatAccentTypes = if (appFeatures.isRhythmOnly) null else currentSong.beatAccentTypes,
                accentIntensityMode = if (appFeatures.isRhythmOnly) accentIntensityMode else null,
                accentIntensityRanges = if (appFeatures.isRhythmOnly) metronomeState.accentIntensityRanges else null,
                keepScreenMode = keepScreenMode,
                mainColorArgb = mainColorArgb,
                backgroundColorArgb = backgroundColorArgb,
                ringColorArgb = bigPulseRingColorArgb,
                bigRingFlashMode = bigRingFlashMode,
                appLanguage = appLanguage,
                appCpuUsagePercent = appCpuUsagePercent,
                showBuyNowButton = showBuyNowButton,
                settingsEnabled = effectiveSettingsEnabled,
                trialStatusText = freeSettingsTrialState.statusText,
                trialButtonText = freeSettingsTrialState.buttonText,
                trialButtonEnabled = freeSettingsTrialState.buttonEnabled,
                onStartTrial = {
                    val startDay = currentEpochDay()
                    freeSettingsTrialStartedDay = startDay
                    if (!isPreview) {
                        context.saveSettingsLong(FREE_SETTINGS_TRIAL_STARTED_DAY_KEY, startDay)
                    }
                },
                onHapticsToggle = {
                    val nextHapticsEnabled = !hapticsEnabled
                    val nextState = metronomeState.copy(hapticsEnabled = nextHapticsEnabled)
                    metronomeState = nextState
                    if (!isPreview) {
                        context.saveRhythmState(nextState)
                    }
                    metronomeService?.setHapticsEnabled(nextHapticsEnabled)
                },
                onBeepToggle = {
                    val nextBeepEnabled = !beepEnabled
                    val nextState = metronomeState.copy(beepEnabled = nextBeepEnabled)
                    metronomeState = nextState
                    if (!isPreview) {
                        context.saveRhythmState(nextState)
                    }
                    metronomeService?.setBeepEnabled(nextBeepEnabled)
                },
                onBeatSoundModeChoice = { mode ->
                    val nextBeatSoundMode = if (beatSoundMode == mode) {
                        BeatSoundMode.Clicks
                    } else {
                        mode
                    }
                    val nextState = metronomeState.copy(
                        beatSoundMode = nextBeatSoundMode,
                        beepEnabled = if (nextBeatSoundMode != BeatSoundMode.Clicks) true else beepEnabled,
                    )
                    metronomeState = nextState
                    if (!isPreview) {
                        context.saveRhythmState(nextState)
                    }
                    if (nextState.beepEnabled != beepEnabled) {
                        metronomeService?.setBeepEnabled(nextState.beepEnabled)
                    }
                    metronomeService?.setBeatSoundMode(nextBeatSoundMode)
                },
                onRhythmPresetChoice = { nextBeatsPerMeasure, nextBeatAccentTypes, nextSubdivisionCount ->
                    if (appFeatures.isRhythmOnly) {
                        setRhythmOnlyPreset(
                            nextBeatsPerMeasure,
                            nextBeatAccentTypes,
                            nextSubdivisionCount,
                        )
                    } else {
                        setCurrentSongRhythmPreset(
                            nextBeatsPerMeasure,
                            nextBeatAccentTypes,
                            nextSubdivisionCount,
                        )
                    }
                },
                onAccentIntensityModeChoice = { mode ->
                    applyRhythmOnlyState(
                        metronomeState.copy(accentIntensityMode = mode),
                        restartBeat = false,
                    )
                },
                onAccentIntensityRangesChange = { ranges ->
                    applyRhythmOnlyState(
                        metronomeState.copy(accentIntensityRanges = ranges),
                        restartBeat = false,
                    )
                },
                onKeepScreenModeChoice = { mode ->
                    keepScreenMode = mode
                },
                onMainColorChoice = { colorArgb ->
                    val nextColorArgb = safeMainColorArgb(
                        requestedMainColorArgb = colorArgb,
                        backgroundColorArgb = backgroundColorArgb,
                    )
                    mainColorArgb = nextColorArgb
                    if (!isPreview) {
                        context.saveSettingsInt(SETTINGS_MAIN_COLOR_KEY, nextColorArgb)
                    }
                },
                onBackgroundColorChoice = { colorArgb ->
                    val nextColorArgb = safeBackgroundColorArgb(
                        requestedBackgroundColorArgb = colorArgb,
                        mainColorArgb = mainColorArgb,
                    )
                    backgroundColorArgb = nextColorArgb
                    if (!isPreview) {
                        context.saveSettingsInt(SETTINGS_BACKGROUND_COLOR_KEY, nextColorArgb)
                    }
                },
                onRingColorChoice = { colorArgb ->
                    bigPulseRingColorArgb = colorArgb
                    if (!isPreview) {
                        context.saveSettingsInt(SETTINGS_RING_COLOR_KEY, colorArgb)
                    }
                },
                onBigRingModeChoice = { mode ->
                    bigRingModeState.intValue = mode.persistedValue
                    if (!isPreview) {
                        context.saveSettingsInt(SETTINGS_RING_MODE_KEY, mode.persistedValue)
                    }
                },
                onLanguageChoice = { language ->
                    val nextLanguageIndex = AppLanguages.indexOf(language).coerceAtLeast(0)
                    appLanguageIndexState.intValue = nextLanguageIndex
                    if (!isPreview) {
                        context.saveSettingsInt(SETTINGS_LANGUAGE_INDEX_KEY, nextLanguageIndex)
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val hasPressedPointer = event.changes.any { it.pressed }
                            if (hasPressedPointer != pointerIsDown) {
                                pointerIsDown = hasPressedPointer
                                if (hasPressedPointer) {
                                    quietPulseVisualsUntilMs = SystemClock.elapsedRealtime() +
                                        PAGER_TOUCH_VISUAL_QUIET_MS
                                }
                            }
                        }
                    }
                }
                .background(backgroundColor),
        ) {
            val pagerBigRingVisible = shouldShowPagerBigRing()
            BigPulseRingOverlay(
                beatFlash = beatFlash && pagerBigRingVisible,
                flashingAccentType = beatAccentTypes.typeForBeat(flashingBeat),
                bigRingFlashMode = bigRingFlashMode,
                colorArgb = bigPulseRingColorArgb,
                modifier = Modifier.fillMaxSize(),
            )

            if (appFeatures.isTuneOnly) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (tunePageIndex) {
                        TUNE_TUNER_PAGE_INDEX -> TunerPage(
                            appText = appText,
                            appLanguage = appLanguage,
                            audioAnalysisState = audioAnalysisState,
                            a4ReferenceHz = a4ReferenceHz,
                            selectedProfile = tunerListenProfile,
                            micPermissionGranted = micPermissionGranted,
                            showSaveActions = false,
                            onOpenSpectrum = {
                                tunePageIndex = TUNE_SPECTRUM_PAGE_INDEX
                            },
                            onOpenBpmReader = {
                                bpmReaderOverlayOpen = true
                            },
                            onOpenKey = {
                                tunePageIndex = TUNE_KEY_PAGE_INDEX
                            },
                            onOpenSettings = {
                                tunePageIndex = TUNE_SETTINGS_PAGE_INDEX
                            },
                            onProfileChoice = { tunerListenProfile = it },
                            onSaveKey = {},
                            onSaveBpm = {},
                            onRequestMicPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        )

                        TUNE_SPECTRUM_PAGE_INDEX -> SpectrumAnalyzerPage(
                            appText = appText,
                            appLanguage = appLanguage,
                            audioAnalysisState = audioAnalysisState,
                            a4ReferenceHz = a4ReferenceHz,
                            selectedProfile = tunerListenProfile,
                            selectedReaderMode = spectrumReaderMode,
                            selectedTuningChoice = spectrumTuningChoice,
                            micPermissionGranted = micPermissionGranted,
                            showSaveToClock = false,
                            onSpectrumSettingsSaved = { readerMode, profile, tuningChoice ->
                                spectrumReaderMode = readerMode
                                tunerListenProfile = profile
                                spectrumTuningChoice = tuningChoice?.takeIf { it.profile == profile }
                            },
                            onSaveToClock = { _, _ -> },
                            onOpenFft = {
                                fftOverlayOpen = true
                            },
                            onRequestMicPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        )

                        TUNE_KEY_PAGE_INDEX -> TuneKeyPage(
                            appText = appText,
                            audioAnalysisState = audioAnalysisState,
                            micPermissionGranted = micPermissionGranted,
                            showSaveKey = false,
                            onSaveKey = {},
                            onRequestMicPermission = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        )

                        TUNE_SETTINGS_PAGE_INDEX -> TuneSettingsPage(
                            appText = appText,
                            a4ReferenceHz = a4ReferenceHz,
                            mainColorArgb = mainColorArgb,
                            backgroundColorArgb = backgroundColorArgb,
                            appLanguage = appLanguage,
                            appCpuUsagePercent = appCpuUsagePercent,
                            keepScreenMode = keepScreenMode,
                            showBuyNowButton = true,
                            settingsEnabled = freeSettingsTrialState.settingsEnabled,
                            trialStatusText = freeSettingsTrialState.statusText,
                            trialButtonText = freeSettingsTrialState.buttonText,
                            trialButtonEnabled = freeSettingsTrialState.buttonEnabled,
                            onStartTrial = {
                                val startDay = currentEpochDay()
                                freeSettingsTrialStartedDay = startDay
                                if (!isPreview) {
                                    context.saveSettingsLong(FREE_SETTINGS_TRIAL_STARTED_DAY_KEY, startDay)
                                }
                            },
                            onA4ReferenceHzChange = { referenceHz ->
                                val nextReferenceHz = referenceHz.coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ)
                                a4ReferenceHz = nextReferenceHz
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_A4_REFERENCE_HZ_KEY, nextReferenceHz)
                                }
                            },
                            onKeepScreenModeChoice = { mode ->
                                keepScreenMode = mode
                            },
                            onMainColorChoice = { colorArgb ->
                                val nextColorArgb = safeMainColorArgb(
                                    requestedMainColorArgb = colorArgb,
                                    backgroundColorArgb = backgroundColorArgb,
                                )
                                mainColorArgb = nextColorArgb
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_MAIN_COLOR_KEY, nextColorArgb)
                                }
                            },
                            onBackgroundColorChoice = { colorArgb ->
                                val nextColorArgb = safeBackgroundColorArgb(
                                    requestedBackgroundColorArgb = colorArgb,
                                    mainColorArgb = mainColorArgb,
                                )
                                backgroundColorArgb = nextColorArgb
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_BACKGROUND_COLOR_KEY, nextColorArgb)
                                }
                            },
                            onLanguageChoice = { language ->
                                val nextLanguageIndex = AppLanguages.indexOf(language).coerceAtLeast(0)
                                appLanguageIndexState.intValue = nextLanguageIndex
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_LANGUAGE_INDEX_KEY, nextLanguageIndex)
                                }
                            },
                        )
                    }

                    if (tunePageIndex != TUNE_TUNER_PAGE_INDEX) {
                        TunerProfileButton(
                            text = appText.done,
                            selected = false,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 24.dp, end = 12.dp)
                                .rotate(38f)
                                .width(58.dp)
                                .height(24.dp),
                            fontSize = 9.sp,
                            onClick = {
                                tunePageIndex = TUNE_TUNER_PAGE_INDEX
                            },
                        )
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->

                if (appFeatures.isFreeOnly) {
                    when (page) {
                        FREE_TAP_PAGE_INDEX -> TapTempoFree(
                            appText = appText,
                            bpm = bpm,
                            musicalKey = currentSong.musicalKey,
                            beatsPerMeasure = currentSong.beatsPerMeasure,
                            subdivisionCount = currentSong.subdivisionCount,
                            beatFlash = beatFlash,
                            isAccentFlash = flashingBeat == accentBeat,
                            isRunning = isRunning,
                            showAudioTools = false,
                            showRhythmChoices = false,
                            showTimeSignatureReadout = true,
                            onOpenTuner = {},
                            onOpenSpectrum = {},
                            onTapTempo = recordTapTempo,
                            onDecrease = decreaseBpm,
                            onDecreaseLarge = decreaseBpmLarge,
                            onIncrease = increaseBpm,
                            onIncreaseLarge = increaseBpmLarge,
                            onToggleRunning = toggleRunning,
                            onTimeSignatureClick = {},
                            onKeyClick = {},
                        )

                        FREE_SETTINGS_PAGE_INDEX -> SimpleSettingsSurface(
                            showBuyNowButton = true,
                            settingsEnabled = false,
                        )
                    }
                    return@HorizontalPager
                }

                if (appFeatures.isRhythmOnly) {
                    when (page) {
                        RHYTHM_APP_RHYTHM_PAGE_INDEX -> RhythmSetupPage(
                            appText = appText,
                            bpm = bpm,
                            beatsPerMeasure = beatsPerMeasure,
                            subdivisionCount = subdivisionCount,
                            beatAccentTypes = beatAccentTypes,
                            currentBeatIndex = currentBeatIndex,
                            currentSubdivisionIndex = currentSubdivisionIndex,
                            beatFlash = beatFlash,
                            isRunning = isRunning,
                            beatClockStartedAtMs = beatClockStartedAtMs,
                            tempoNudgeMs = tempoNudgeMs,
                            playbackStartedAtMs = playbackStartedAtMs,
                            beatVisualsEnabled = pageShowsRhythmPulse(pagerState.currentPage) &&
                                !pagerState.isScrollInProgress &&
                                abs(pagerState.currentPageOffsetFraction) <= 0.01f,
                            beatRingVisible = pageShowsRhythmPulse(pagerState.currentPage) &&
                                !pagerState.isScrollInProgress &&
                                abs(pagerState.currentPageOffsetFraction) <= 0.01f,
                            showAudioTools = false,
                            onEditRhythm = { startRhythmEditor() },
                            onToggleRunning = toggleRunning,
                            onBpmClick = {
                                tapTempoPopupOpen = true
                            },
                            onTempoNudge = { step ->
                                metronomeService?.pushTempoPhase(step)
                            },
                            onOpenTuner = {},
                            onOpenSpectrum = {},
                        )

                        RHYTHM_APP_SETTINGS_PAGE_INDEX -> SimpleSettingsSurface(
                            showBuyNowButton = true,
                            settingsEnabled = freeSettingsTrialState.settingsEnabled,
                        )
                    }
                    return@HorizontalPager
                }

                if (appFeatures.isPlaylistOnly) {
                    when (page) {
                        MAIN_PAGE_INDEX -> PlaylistClockPage(
                            appText = appText,
                            playlist = currentPlaylist,
                            songIndex = songIndex,
                            isRunning = isRunning,
                            beatClockStartedAtMs = beatClockStartedAtMs,
                            playbackStartedAtMs = playbackStartedAtMs,
                            clockImageResId = clockImageResId,
                            clockColorArgb = clockColorArgb,
                            forceSimpleRhythm = false,
                            onPreviousSong = { selectSong(songIndex - 1) },
                            onNextSong = { selectSong(songIndex + 1) },
                            onEditPlaylist = {
                                playlistEditorPopupOpen = true
                            },
                            onToggleRunning = toggleRunning,
                        )

                        FREE_SETTINGS_PAGE_INDEX -> PlaylistSettingsPage(
                            appText = appText,
                            hapticsEnabled = hapticsEnabled,
                            beepEnabled = beepEnabled,
                            beatSoundMode = beatSoundMode,
                            keyDroneEnabled = keyDroneEnabled,
                            keyDroneVolumePercent = keyDroneVolumePercent,
                            a4ReferenceHz = a4ReferenceHz,
                            keepScreenMode = keepScreenMode,
                            mainColorArgb = mainColorArgb,
                            backgroundColorArgb = backgroundColorArgb,
                            clockColorArgb = clockColorArgb,
                            clockImageIndex = selectedClockImageIndex,
                            appLanguage = appLanguage,
                            appCpuUsagePercent = appCpuUsagePercent,
                            showBuyNowButton = true,
                            settingsEnabled = freeSettingsTrialState.settingsEnabled,
                            trialStatusText = freeSettingsTrialState.statusText,
                            trialButtonText = freeSettingsTrialState.buttonText,
                            trialButtonEnabled = freeSettingsTrialState.buttonEnabled,
                            onStartTrial = {
                                val startDay = currentEpochDay()
                                freeSettingsTrialStartedDay = startDay
                                if (!isPreview) {
                                    context.saveSettingsLong(FREE_SETTINGS_TRIAL_STARTED_DAY_KEY, startDay)
                                }
                            },
                            onHapticsToggle = {
                                val nextHapticsEnabled = !hapticsEnabled
                                val nextState = metronomeState.copy(hapticsEnabled = nextHapticsEnabled)
                                metronomeState = nextState
                                if (!isPreview) {
                                    context.saveRhythmState(nextState)
                                }
                                metronomeService?.setHapticsEnabled(nextHapticsEnabled)
                            },
                            onBeepToggle = {
                                val nextBeepEnabled = !beepEnabled
                                val nextState = metronomeState.copy(beepEnabled = nextBeepEnabled)
                                metronomeState = nextState
                                if (!isPreview) {
                                    context.saveRhythmState(nextState)
                                }
                                metronomeService?.setBeepEnabled(nextBeepEnabled)
                            },
                            onBeatSoundModeChoice = { mode ->
                                val nextBeatSoundMode = if (beatSoundMode == mode) {
                                    BeatSoundMode.Clicks
                                } else {
                                    mode
                                }
                                val nextState = metronomeState.copy(
                                    beatSoundMode = nextBeatSoundMode,
                                    beepEnabled = if (nextBeatSoundMode != BeatSoundMode.Clicks) true else beepEnabled,
                                )
                                metronomeState = nextState
                                if (!isPreview) {
                                    context.saveRhythmState(nextState)
                                }
                                if (nextState.beepEnabled != beepEnabled) {
                                    metronomeService?.setBeepEnabled(nextState.beepEnabled)
                                }
                                metronomeService?.setBeatSoundMode(nextBeatSoundMode)
                            },
                            onKeyDroneToggle = {
                                val nextKeyDroneEnabled = !keyDroneEnabled
                                val nextState = metronomeState.copy(
                                    keyDroneEnabled = nextKeyDroneEnabled,
                                    musicalKey = currentSong.musicalKey,
                                )
                                metronomeState = nextState
                                if (!isPreview) {
                                    context.saveRhythmState(nextState)
                                }
                                metronomeService?.setKeyDroneEnabled(nextKeyDroneEnabled)
                            },
                            onKeyDroneVolumeChange = { step ->
                                val nextVolume = (keyDroneVolumePercent + step).coerceIn(0, 100)
                                val nextState = metronomeState.copy(keyDroneVolumePercent = nextVolume)
                                metronomeState = nextState
                                if (!isPreview) {
                                    context.saveRhythmState(nextState)
                                }
                                metronomeService?.setKeyDroneVolumePercent(nextVolume)
                            },
                            onA4ReferenceHzChange = { referenceHz ->
                                val nextReferenceHz = referenceHz
                                    .coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ)
                                a4ReferenceHz = nextReferenceHz
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_A4_REFERENCE_HZ_KEY, nextReferenceHz)
                                }
                            },
                            onKeepScreenModeChoice = { mode ->
                                keepScreenMode = mode
                            },
                            onMainColorChoice = { colorArgb ->
                                val nextColorArgb = safeMainColorArgb(
                                    requestedMainColorArgb = colorArgb,
                                    backgroundColorArgb = backgroundColorArgb,
                                )
                                mainColorArgb = nextColorArgb
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_MAIN_COLOR_KEY, nextColorArgb)
                                }
                            },
                            onBackgroundColorChoice = { colorArgb ->
                                val nextColorArgb = safeBackgroundColorArgb(
                                    requestedBackgroundColorArgb = colorArgb,
                                    mainColorArgb = mainColorArgb,
                                )
                                backgroundColorArgb = nextColorArgb
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_BACKGROUND_COLOR_KEY, nextColorArgb)
                                }
                            },
                            onClockColorChoice = { colorArgb ->
                                clockColorArgb = colorArgb
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_CLOCK_COLOR_KEY, colorArgb)
                                }
                            },
                            onClockImageChoice = { choiceIndex ->
                                val nextChoiceIndex = choiceIndex.coerceIn(0, ClockImageChoices.lastIndex)
                                clockImageIndexState.intValue = nextChoiceIndex
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_CLOCK_IMAGE_INDEX_KEY, nextChoiceIndex)
                                }
                            },
                            onLanguageChoice = { language ->
                                val nextLanguageIndex = AppLanguages.indexOf(language).coerceAtLeast(0)
                                appLanguageIndexState.intValue = nextLanguageIndex
                                if (!isPreview) {
                                    context.saveSettingsInt(SETTINGS_LANGUAGE_INDEX_KEY, nextLanguageIndex)
                                }
                            },
                        )
                    }
                    return@HorizontalPager
                }

                when (page) {
                    MAIN_PAGE_INDEX -> PlaylistClockPage(
                        appText = appText,
                        playlist = currentPlaylist,
                        songIndex = songIndex,
                        isRunning = isRunning,
                        beatClockStartedAtMs = beatClockStartedAtMs,
                        playbackStartedAtMs = playbackStartedAtMs,
                        clockImageResId = clockImageResId,
                        clockColorArgb = clockColorArgb,
                        onPreviousSong = { selectSong(songIndex - 1) },
                        onNextSong = { selectSong(songIndex + 1) },
                        onEditPlaylist = {
                            playlistEditorPopupOpen = true
                        },
                        onToggleRunning = toggleRunning,
                    )

                    RHYTHM_PAGE_INDEX -> RhythmSetupPage(
                        appText = appText,
                        bpm = bpm,
                        beatsPerMeasure = beatsPerMeasure,
                        subdivisionCount = subdivisionCount,
                        beatAccentTypes = beatAccentTypes,
                        currentBeatIndex = currentBeatIndex,
                        currentSubdivisionIndex = currentSubdivisionIndex,
                        beatFlash = beatFlash,
                        isRunning = isRunning,
                        beatClockStartedAtMs = beatClockStartedAtMs,
                        tempoNudgeMs = tempoNudgeMs,
                        playbackStartedAtMs = playbackStartedAtMs,
                        beatVisualsEnabled = pageShowsRhythmPulse(pagerState.currentPage) &&
                            !pagerState.isScrollInProgress &&
                            abs(pagerState.currentPageOffsetFraction) <= 0.01f,
                        beatRingVisible = pageShowsRhythmPulse(pagerState.currentPage) &&
                            !pagerState.isScrollInProgress &&
                            abs(pagerState.currentPageOffsetFraction) <= 0.01f,
                        onEditRhythm = {
                            rhythmEditorPopupOpen = true
                        },
                        onToggleRunning = toggleRunning,
                        onBpmClick = {
                            tapTempoPopupOpen = true
                        },
                        onTempoNudge = { step ->
                            metronomeService?.pushTempoPhase(step)
                        },
                        onOpenTuner = {
                            tunerOverlayOpen = true
                        },
                        onOpenSpectrum = {
                            spectrumOverlayOpen = true
                        },
                    )

                    SETTINGS_PAGE_INDEX -> SettingsPage(
                    appText = appText,
                    hapticsEnabled = hapticsEnabled,
                    beepEnabled = beepEnabled,
                    beatSoundMode = beatSoundMode,
                    keyDroneEnabled = keyDroneEnabled,
                    keyDroneVolumePercent = keyDroneVolumePercent,
                    tempoNudgeMs = tempoNudgeMs,
                    accentIntensityMode = accentIntensityMode,
                    accentIntensityRanges = metronomeState.accentIntensityRanges,
                    beatsPerMeasure = currentSong.beatsPerMeasure,
                    beatAccentTypes = currentSong.beatAccentTypes,
                    a4ReferenceHz = a4ReferenceHz,
                    keepScreenMode = keepScreenMode,
                    mainColorArgb = mainColorArgb,
                    backgroundColorArgb = backgroundColorArgb,
                    clockColorArgb = clockColorArgb,
                    clockImageIndex = selectedClockImageIndex,
                    ringColorArgb = bigPulseRingColorArgb,
                    bigRingFlashMode = bigRingFlashMode,
                    appLanguage = appLanguage,
                    appCpuUsagePercent = appCpuUsagePercent,
                    onHapticsToggle = {
                        val nextHapticsEnabled = !hapticsEnabled
                        val nextState = metronomeState.copy(hapticsEnabled = nextHapticsEnabled)
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        metronomeService?.setHapticsEnabled(nextHapticsEnabled)
                    },
                    onBeepToggle = {
                        val nextBeepEnabled = !beepEnabled
                        val nextState = metronomeState.copy(beepEnabled = nextBeepEnabled)
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        metronomeService?.setBeepEnabled(nextBeepEnabled)
                    },
                    onBeatSoundModeChoice = { mode ->
                        val nextBeatSoundMode = if (beatSoundMode == mode) {
                            BeatSoundMode.Clicks
                        } else {
                            mode
                        }
                        val nextState = metronomeState.copy(
                            beatSoundMode = nextBeatSoundMode,
                            beepEnabled = if (nextBeatSoundMode != BeatSoundMode.Clicks) true else beepEnabled,
                        )
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        if (nextState.beepEnabled != beepEnabled) {
                            metronomeService?.setBeepEnabled(nextState.beepEnabled)
                        }
                        metronomeService?.setBeatSoundMode(nextBeatSoundMode)
                    },
                    onKeyDroneToggle = {
                        val nextKeyDroneEnabled = !keyDroneEnabled
                        val nextState = metronomeState.copy(
                            keyDroneEnabled = nextKeyDroneEnabled,
                            musicalKey = currentSong.musicalKey,
                        )
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        metronomeService?.setKeyDroneEnabled(nextKeyDroneEnabled)
                    },
                    onKeyDroneVolumeChange = { step ->
                        val nextVolume = (keyDroneVolumePercent + step).coerceIn(0, 100)
                        val nextState = metronomeState.copy(keyDroneVolumePercent = nextVolume)
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        metronomeService?.setKeyDroneVolumePercent(nextVolume)
                    },
                    onTempoNudgeChange = { step ->
                        val nextNudgeMs = (tempoNudgeMs + step)
                            .coerceIn(MIN_TEMPO_NUDGE_MS, MAX_TEMPO_NUDGE_MS)
                        val nextState = metronomeState.copy(tempoNudgeMs = nextNudgeMs)
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        metronomeService?.setTempoNudgeMs(nextNudgeMs)
                    },
                    onAccentIntensityModeChoice = { mode ->
                        val nextState = metronomeState.copy(accentIntensityMode = mode)
                        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
                            song.copy(accentIntensityMode = mode)
                        }
                        metronomeState = nextState
                        playlists = nextPlaylists
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                            context.saveSavedPlaylists(nextPlaylists)
                        }
                        metronomeService?.setAccentIntensityMode(mode)
                    },
                    onAccentIntensityRangesChange = { ranges ->
                        val nextState = metronomeState.copy(accentIntensityRanges = ranges)
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        metronomeService?.setAccentIntensityRanges(ranges)
                    },
                    onRhythmPresetChoice = { nextBeatsPerMeasure, nextBeatAccentTypes, nextSubdivisionCount ->
                        setCurrentSongRhythmPreset(
                            nextBeatsPerMeasure,
                            nextBeatAccentTypes,
                            nextSubdivisionCount,
                        )
                    },
                    onA4ReferenceHzChange = { referenceHz ->
                        val nextReferenceHz = referenceHz.coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ)
                        a4ReferenceHz = nextReferenceHz
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_A4_REFERENCE_HZ_KEY, nextReferenceHz)
                        }
                    },
                    onKeepScreenModeChoice = { mode ->
                        keepScreenMode = mode
                    },
                    onMainColorChoice = { colorArgb ->
                        val nextColorArgb = safeMainColorArgb(
                            requestedMainColorArgb = colorArgb,
                            backgroundColorArgb = backgroundColorArgb,
                        )
                        mainColorArgb = nextColorArgb
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_MAIN_COLOR_KEY, nextColorArgb)
                        }
                    },
                    onBackgroundColorChoice = { colorArgb ->
                        val nextColorArgb = safeBackgroundColorArgb(
                            requestedBackgroundColorArgb = colorArgb,
                            mainColorArgb = mainColorArgb,
                        )
                        backgroundColorArgb = nextColorArgb
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_BACKGROUND_COLOR_KEY, nextColorArgb)
                        }
                    },
                    onClockColorChoice = { colorArgb ->
                        clockColorArgb = colorArgb
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_CLOCK_COLOR_KEY, colorArgb)
                        }
                    },
                    onClockImageChoice = { choiceIndex ->
                        val nextChoiceIndex = choiceIndex.coerceIn(0, ClockImageChoices.lastIndex)
                        clockImageIndexState.intValue = nextChoiceIndex
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_CLOCK_IMAGE_INDEX_KEY, nextChoiceIndex)
                        }
                    },
                    onRingColorChoice = { colorArgb ->
                        bigPulseRingColorArgb = colorArgb
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_RING_COLOR_KEY, colorArgb)
                        }
                    },
                    onBigRingModeChoice = { mode ->
                        bigRingModeState.intValue = mode.persistedValue
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_RING_MODE_KEY, mode.persistedValue)
                        }
                    },
                    onLanguageChoice = { language ->
                        val nextLanguageIndex = AppLanguages.indexOf(language).coerceAtLeast(0)
                        appLanguageIndexState.intValue = nextLanguageIndex
                        if (!isPreview) {
                            context.saveSettingsInt(SETTINGS_LANGUAGE_INDEX_KEY, nextLanguageIndex)
                        }
                    },
                )

                    TAP_TEMPO_FREE_PAGE_INDEX -> TapTempoFree(
                        appText = appText,
                        bpm = bpm,
                        musicalKey = currentSong.musicalKey,
                        beatsPerMeasure = currentSong.beatsPerMeasure,
                        subdivisionCount = currentSong.subdivisionCount,
                        beatFlash = beatFlash,
                        isAccentFlash = flashingBeat == accentBeat,
                        isRunning = isRunning,
                        showAudioTools = appFeatures.showTunerEntry || appFeatures.showSpectrumEntry,
                        showRhythmChoices = appFeatures.showTapRhythmChoices,
                        onOpenTuner = { tunerOverlayOpen = true },
                        onOpenSpectrum = { spectrumOverlayOpen = true },
                        onTapTempo = recordTapTempo,
                        onDecrease = decreaseBpm,
                        onDecreaseLarge = decreaseBpmLarge,
                        onIncrease = increaseBpm,
                        onIncreaseLarge = increaseBpmLarge,
                        onToggleRunning = toggleRunning,
                        onTimeSignatureClick = {
                            freeTapTimeSignaturePickerOpen = true
                        },
                        onKeyClick = {
                            freeTapKeyPickerOpen = true
                        },
                    )
                }
                }
            }

            if (
                    appFeatures.pageCount > 1 &&
                    !audioOverlayOpen &&
                    (
                    appFeatures.isFreeOnly ||
                        pagerState.currentPage != MAIN_PAGE_INDEX &&
                        !pageShowsRhythmPulse(pagerState.currentPage)
                    )
            ) {
                PulsePagerIndicator(
                    currentPage = pagerState.currentPage,
                    pageCount = appFeatures.pageCount,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }

            CpuPercentOverlay(
                cpuUsagePercent = appCpuUsagePercent,
                modifier = Modifier.align(Alignment.TopStart),
            )

            if (playlistEditorPopupOpen) {
                PlaylistEditorPopup(
                    appText = appText,
                    playlists = playlists,
                    playlistIndex = playlistIndex,
                    songIndex = songIndex,
                    onPreviousPlaylist = { selectPlaylist(playlistIndex - 1) },
                    onNextPlaylist = { selectPlaylist(playlistIndex + 1) },
                    onAddPlaylist = {
                        val nextPlaylists = playlists + defaultSavedPlaylist(playlists.size + 1)
                        playlists = nextPlaylists
                        selectedPlaylistIndexState.intValue = nextPlaylists.lastIndex
                        selectedSongIndexState.intValue = 0
                        applySongToMetronome(
                            nextPlaylistIndex = nextPlaylists.lastIndex,
                            nextSongIndex = 0,
                            song = nextPlaylists.last().songs.first(),
                            restartBeat = isRunning,
                        )
                    },
                    onPreviousSong = { selectSong(songIndex - 1) },
                    onNextSong = { selectSong(songIndex + 1) },
                    onAddSong = {
                        val nextSongIndex = currentPlaylist.songs.size
                        val nextPlaylists = playlists.updatePlaylist(playlistIndex) { playlist ->
                            playlist.copy(
                                songs = playlist.songs + defaultPlaylistSong(nextSongIndex + 1),
                            )
                        }
                        playlists = nextPlaylists
                        selectedSongIndexState.intValue = nextSongIndex
                        applySongToMetronome(
                            nextPlaylistIndex = playlistIndex,
                            nextSongIndex = nextSongIndex,
                            song = nextPlaylists[playlistIndex].songs[nextSongIndex],
                            restartBeat = isRunning,
                        )
                    },
                    onDeleteSong = {
                        val activePlaylist = playlists[playlistIndex]
                        if (activePlaylist.songs.size > 1) {
                            val nextSongs = activePlaylist.songs.filterIndexed { index, _ -> index != songIndex }
                            val nextSongIndex = songIndex.coerceAtMost(nextSongs.lastIndex)
                            val nextPlaylists = playlists.updatePlaylist(playlistIndex) { playlist ->
                                playlist.copy(songs = nextSongs)
                            }
                            playlists = nextPlaylists
                            selectedSongIndexState.intValue = nextSongIndex
                            applySongToMetronome(
                                nextPlaylistIndex = playlistIndex,
                                nextSongIndex = nextSongIndex,
                                song = nextSongs[nextSongIndex],
                                restartBeat = isRunning,
                            )
                        } else {
                            val remainingPlaylists = playlists.filterIndexed { index, _ -> index != playlistIndex }
                            val nextPlaylists = remainingPlaylists.ifEmpty { listOf(defaultSavedPlaylist(1)) }
                            val nextPlaylistIndex = playlistIndex.coerceAtMost(nextPlaylists.lastIndex)
                            playlists = nextPlaylists
                            selectedPlaylistIndexState.intValue = nextPlaylistIndex
                            selectedSongIndexState.intValue = 0
                            applySongToMetronome(
                                nextPlaylistIndex = nextPlaylistIndex,
                                nextSongIndex = 0,
                                song = nextPlaylists[nextPlaylistIndex].songs.first(),
                                restartBeat = isRunning,
                            )
                        }
                    },
                    onPlaylistNameEdit = { name ->
                        playlists = playlists.updatePlaylist(playlistIndex) { playlist ->
                            playlist.copy(name = name)
                        }
                    },
                    onSongNameEdit = { name ->
                        updateCurrentSong { song ->
                            song.copy(name = name)
                        }
                    },
                    onSongBpmChange = { step ->
                        val nextBpm = (currentSong.bpm + step).coerceIn(MIN_BPM, MAX_BPM)
                        updateCurrentSong { song -> song.copy(bpm = nextBpm) }
                        metronomeService?.setPlaylistItem(
                            playlistIndex = playlistIndex,
                            songIndex = songIndex,
                            bpm = nextBpm,
                            beatsPerMeasure = currentSong.beatsPerMeasure,
                            accentBeat = currentSong.accentBeat,
                            subdivisionCount = currentSong.subdivisionCount,
                            beatAccentTypes = currentSong.beatAccentTypes,
                            accentIntensityMode = currentSong.accentIntensityMode,
                            restartBeat = isRunning,
                        )
                    },
                    onSongBpmClick = {
                        clearTapTempo()
                        tapTempoPopupOpen = true
                    },
                    onRhythmPresetChoice = { nextBeatsPerMeasure, nextBeatAccentTypes, nextSubdivisionCount ->
                        setCurrentSongRhythmPreset(
                            nextBeatsPerMeasure,
                            nextBeatAccentTypes,
                            nextSubdivisionCount,
                        )
                    },
                    onSongKeyChange = { step ->
                        updateCurrentSong { song ->
                            song.copy(
                                musicalKey = cycleOption(
                                    options = MusicalKeyOptions,
                                    current = song.musicalKey,
                                    step = step,
                                ),
                            )
                        }
                    },
                    onSongKeySet = { key ->
                        updateCurrentSong { song ->
                            song.copy(musicalKey = key)
                        }
                    },
                    onSongNoteChange = { step ->
                        updateCurrentSong { song ->
                            song.copy(
                                note = cycleOption(
                                    options = SongNoteOptions,
                                    current = song.note,
                                    step = step,
                                ),
                            )
                        }
                    },
                    onSongNoteEdit = { note ->
                        updateCurrentSong { song ->
                            song.copy(note = note)
                        }
                    },
                    showRhythmEditor = !appFeatures.isPlaylistOnly,
                    onEditRhythm = {
                        if (!appFeatures.isPlaylistOnly) {
                            playlistRhythmEditorPopupOpen = true
                        }
                    },
                    onDone = { savePlaylistEditorAndClose() },
                )
            }

            if (playlistRhythmEditorPopupOpen) {
                RhythmEditorPopup(
                    appText = appText,
                    bpm = bpm,
                    beatsPerMeasure = beatsPerMeasure,
                    subdivisionCount = subdivisionCount,
                    beatAccentTypes = beatAccentTypes,
                    currentBeatIndex = currentBeatIndex,
                    currentSubdivisionIndex = currentSubdivisionIndex,
                    beatFlash = beatFlash,
                    accentIntensityMode = accentIntensityMode,
                    accentIntensityRanges = metronomeState.accentIntensityRanges,
                    appLanguage = appLanguage,
                    onTimeSignatureChoice = { beatChoice ->
                        metronomeService?.setBeatsPerMeasure(beatChoice)
                    },
                    onBeatAccentTypeCycle = { beatChoice ->
                        metronomeService?.cycleBeatAccentType(beatChoice)
                    },
                    onSubdivisionChoice = { subdivision ->
                        metronomeService?.setSubdivisionCount(subdivision)
                    },
                    onAccentIntensityModeChoice = { mode ->
                        val nextState = metronomeState.copy(accentIntensityMode = mode)
                        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
                            song.copy(accentIntensityMode = mode)
                        }
                        metronomeState = nextState
                        playlists = nextPlaylists
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                            context.saveSavedPlaylists(nextPlaylists)
                        }
                        metronomeService?.setAccentIntensityMode(mode)
                    },
                    onAccentIntensityRangesChange = { ranges ->
                        val nextState = metronomeState.copy(accentIntensityRanges = ranges)
                        metronomeState = nextState
                        if (!isPreview) {
                            context.saveRhythmState(nextState)
                        }
                        metronomeService?.setAccentIntensityRanges(ranges)
                    },
                    onBpmClick = {
                        tapTempoPopupOpen = true
                    },
                    onDone = { savePlaylistRhythmEditorAndClose() },
                )
            }

            if (rhythmEditorPopupOpen) {
                val displayedRhythmState = if (appFeatures.isRhythmOnly) {
                    rhythmEditorDraft ?: metronomeState
                } else {
                    metronomeState
                }
                RhythmEditorPopup(
                    appText = appText,
                    bpm = displayedRhythmState.bpm,
                    beatsPerMeasure = displayedRhythmState.beatsPerMeasure,
                    subdivisionCount = displayedRhythmState.subdivisionCount,
                    beatAccentTypes = displayedRhythmState.beatAccentTypes,
                    currentBeatIndex = displayedRhythmState.currentBeatIndex,
                    currentSubdivisionIndex = displayedRhythmState.currentSubdivisionIndex,
                    beatFlash = displayedRhythmState.beatFlash,
                    accentIntensityMode = displayedRhythmState.accentIntensityMode,
                    accentIntensityRanges = displayedRhythmState.accentIntensityRanges,
                    appLanguage = appLanguage,
                    onTimeSignatureChoice = { beatChoice ->
                        if (appFeatures.isRhythmOnly) {
                            updateRhythmEditorDraft { draft ->
                                val nextBeatsPerMeasure = beatChoice.coerceIn(2, 16)
                                val nextBeatAccentTypes = draft.beatAccentTypes.normalizedRhythmAccentTypes(
                                    beatsPerMeasure = nextBeatsPerMeasure,
                                    accentBeat = draft.accentBeat,
                                )
                                draft.copy(
                                    beatsPerMeasure = nextBeatsPerMeasure,
                                    accentBeat = nextBeatAccentTypes.primaryRhythmAccentBeat(),
                                    beatAccentTypes = nextBeatAccentTypes,
                                    currentBeatIndex = draft.currentBeatIndex.coerceIn(1, nextBeatsPerMeasure),
                                )
                            }
                        } else {
                            metronomeService?.setBeatsPerMeasure(beatChoice)
                        }
                    },
                    onBeatAccentTypeCycle = { beatChoice ->
                        if (appFeatures.isRhythmOnly) {
                            updateRhythmEditorDraft { draft ->
                                val safeBeat = beatChoice.coerceIn(1, draft.beatsPerMeasure)
                                val nextBeatAccentTypes = draft.beatAccentTypes
                                    .normalizedRhythmAccentTypes(
                                        beatsPerMeasure = draft.beatsPerMeasure,
                                        accentBeat = draft.accentBeat,
                                    )
                                    .mapIndexed { index, accentType ->
                                        if (index + 1 == safeBeat) accentType.next() else accentType
                                    }
                                draft.copy(
                                    accentBeat = nextBeatAccentTypes.primaryRhythmAccentBeat(),
                                    beatAccentTypes = nextBeatAccentTypes,
                                )
                            }
                        } else {
                            metronomeService?.cycleBeatAccentType(beatChoice)
                        }
                    },
                    onSubdivisionChoice = { subdivision ->
                        if (appFeatures.isRhythmOnly) {
                            updateRhythmEditorDraft { draft ->
                                draft.copy(
                                    subdivisionCount = subdivision.toSupportedPulseSubdivisionCount(),
                                    currentSubdivisionIndex = 1,
                                )
                            }
                        } else {
                            metronomeService?.setSubdivisionCount(subdivision)
                        }
                    },
                    onAccentIntensityModeChoice = { mode ->
                        if (appFeatures.isRhythmOnly) {
                            updateRhythmEditorDraft { draft ->
                                draft.copy(accentIntensityMode = mode)
                            }
                        } else {
                            val nextState = metronomeState.copy(accentIntensityMode = mode)
                            val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
                                song.copy(accentIntensityMode = mode)
                            }
                            metronomeState = nextState
                            playlists = nextPlaylists
                            if (!isPreview) {
                                context.saveRhythmState(nextState)
                                context.saveSavedPlaylists(nextPlaylists)
                            }
                            metronomeService?.setAccentIntensityMode(mode)
                        }
                    },
                    onAccentIntensityRangesChange = { ranges ->
                        if (appFeatures.isRhythmOnly) {
                            updateRhythmEditorDraft { draft ->
                                draft.copy(accentIntensityRanges = ranges)
                            }
                        } else {
                            val nextState = metronomeState.copy(accentIntensityRanges = ranges)
                            metronomeState = nextState
                            if (!isPreview) {
                                context.saveRhythmState(nextState)
                            }
                            metronomeService?.setAccentIntensityRanges(ranges)
                        }
                    },
                    onBpmClick = {
                        tapTempoPopupOpen = true
                    },
                    onCancel = { cancelRhythmEditorAndClose() },
                    onDone = { commitRhythmEditorAndClose() },
                )
            }

            if (tapTempoPopupOpen) {
                TapTempoPopup(
                    appText = appText,
                    bpm = bpm,
                    beatsPerMeasure = currentSong.beatsPerMeasure,
                    subdivisionCount = currentSong.subdivisionCount,
                    beatFlash = beatFlash,
                    isAccentFlash = flashingBeat == accentBeat,
                    isRunning = isRunning,
                    showAudioTools = appFeatures.showTunerEntry || appFeatures.showSpectrumEntry,
                    onOpenTuner = {
                        tapTempoPopupOpen = false
                        tunerOverlayOpen = true
                    },
                    onOpenSpectrum = {
                        tapTempoPopupOpen = false
                        spectrumOverlayOpen = true
                    },
                    onTapTempo = recordTapTempo,
                    onDecrease = decreaseBpm,
                    onDecreaseLarge = decreaseBpmLarge,
                    onIncrease = increaseBpm,
                    onIncreaseLarge = increaseBpmLarge,
                    onToggleRunning = toggleRunning,
                    onDismiss = {
                        saveCurrentBpmToSong()
                        tapTempoPopupOpen = false
                    },
                )
            }

            if (freeTapTimeSignaturePickerOpen) {
                RhythmTimingChoicePopup(
                    appText = appText,
                    initialPicker = RhythmChoicePicker.TimeSignature,
                    beatsPerMeasure = currentSong.beatsPerMeasure,
                    subdivisionCount = currentSong.subdivisionCount,
                    onTimeSignatureChoice = { option ->
                        setCurrentSongTimeSignature(option)
                    },
                    onSubdivisionChoice = { option ->
                        setCurrentSongSubdivision(option)
                    },
                    onDismiss = {
                        freeTapTimeSignaturePickerOpen = false
                    },
                )
            }

            if (freeTapKeyPickerOpen) {
                MusicalKeyPickerPopup(
                    value = currentSong.musicalKey,
                    doneText = appText.done,
                    onCancel = {
                        freeTapKeyPickerOpen = false
                    },
                    onCommit = { nextKey ->
                        setCurrentSongKey(nextKey)
                        freeTapKeyPickerOpen = false
                    },
                )
            }

            if (tunerOverlayOpen) {
                AnalyzerOverlay(
                    closeText = appText.done,
                    onClose = { tunerOverlayOpen = false },
                ) {
                    TunerPage(
                        appText = appText,
                        appLanguage = appLanguage,
                        audioAnalysisState = audioAnalysisState,
                        a4ReferenceHz = a4ReferenceHz,
                        selectedProfile = tunerListenProfile,
                        micPermissionGranted = micPermissionGranted,
                        showSettingsButton = false,
                        onOpenSpectrum = {
                            tunerOverlayOpen = false
                            spectrumOverlayOpen = true
                        },
                        onOpenBpmReader = {
                            tunerOverlayOpen = false
                            bpmReaderOverlayOpen = true
                        },
                        onOpenKey = {
                            tunerOverlayOpen = false
                            keyOverlayOpen = true
                        },
                        onOpenSettings = {
                            tunerOverlayOpen = false
                            pagerScope.launch {
                                pagerState.scrollToPage(SETTINGS_PAGE_INDEX)
                            }
                        },
                        onProfileChoice = { tunerListenProfile = it },
                        onSaveKey = { guessedKey ->
                            updateCurrentSong { song -> song.copy(musicalKey = guessedKey) }
                        },
                        onSaveBpm = { detectedBpm ->
                            saveDetectedBpmToSong(detectedBpm)
                        },
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
            }

            if (spectrumOverlayOpen) {
                AnalyzerOverlay(
                    closeText = appText.done,
                    onClose = { spectrumOverlayOpen = false },
                ) {
                    SpectrumAnalyzerPage(
                        appText = appText,
                        appLanguage = appLanguage,
                        audioAnalysisState = audioAnalysisState,
                        a4ReferenceHz = a4ReferenceHz,
                        selectedProfile = tunerListenProfile,
                        selectedReaderMode = spectrumReaderMode,
                        selectedTuningChoice = spectrumTuningChoice,
                        micPermissionGranted = micPermissionGranted,
                        onSpectrumSettingsSaved = { readerMode, profile, tuningChoice ->
                            spectrumReaderMode = readerMode
                            tunerListenProfile = profile
                            spectrumTuningChoice = tuningChoice?.takeIf { it.profile == profile }
                        },
                        onSaveToClock = { detectedBpm, guessedKey ->
                            if (!isPreview) {
                                context.saveLatestMusicReading(detectedBpm, guessedKey)
                                spectrumOverlayOpen = false
                                context.showWatchFace()
                            } else {
                                spectrumOverlayOpen = false
                            }
                        },
                        onOpenFft = {
                            spectrumOverlayOpen = false
                            fftOverlayOpen = true
                        },
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
            }

            if (keyOverlayOpen) {
                AnalyzerOverlay(
                    closeText = appText.done,
                    onClose = { keyOverlayOpen = false },
                    glassDoneButton = true,
                ) {
                    TuneKeyPage(
                        appText = appText,
                        audioAnalysisState = audioAnalysisState,
                        micPermissionGranted = micPermissionGranted,
                        showSaveKey = true,
                        onSaveKey = { guessedKey ->
                            updateCurrentSong { song -> song.copy(musicalKey = guessedKey) }
                            keyOverlayOpen = false
                        },
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
            }

            if (fftOverlayOpen) {
                AnalyzerOverlay(
                    closeText = appText.done,
                    onClose = { fftOverlayOpen = false },
                    glassDoneButton = true,
                ) {
                    FftLabPage(
                        appText = appText,
                        audioAnalysisState = audioAnalysisState,
                        selectedProfile = tunerListenProfile,
                        selectedReaderMode = spectrumReaderMode,
                        a4ReferenceHz = a4ReferenceHz,
                        micPermissionGranted = micPermissionGranted,
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
            }

            if (bpmReaderOverlayOpen) {
                AnalyzerOverlay(
                    closeText = appText.done,
                    onClose = { bpmReaderOverlayOpen = false },
                    glassDoneButton = true,
                ) {
                    BpmReaderPage(
                        appText = appText,
                        audioAnalysisState = audioAnalysisState,
                        micPermissionGranted = micPermissionGranted,
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyzerOverlay(
    closeText: String,
    onClose: () -> Unit,
    glassDoneButton: Boolean = false,
    content: @Composable () -> Unit,
) {
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .clickable(onClick = {}),
    ) {
        content()

        if (glassDoneButton) {
            ArchedGlassDoneButton(
                text = closeText,
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        } else {
            ArchedDoneButton(
                text = closeText,
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
internal fun ArchedGlassDoneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCommandButton(
        text = text,
        modifier = modifier
            .padding(top = 24.dp, end = 12.dp)
            .rotate(38f)
            .width(58.dp)
            .height(24.dp),
        fontSize = 9.sp,
        onClick = onClick,
    )
}

@Composable
private fun ArchedDoneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallCommandButton(
        text = text,
        modifier = modifier
            .padding(top = 24.dp, end = 12.dp)
            .rotate(38f)
            .width(58.dp)
            .height(24.dp),
        fontSize = 9.sp,
        onClick = onClick,
    )
}

@Composable
internal fun SmallCommandButton(
    text: String,
    modifier: Modifier,
    fontSize: TextUnit,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
    ) {
        CenteredButtonLabel(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun GlassCommandButton(
    text: String,
    modifier: Modifier,
    fontSize: TextUnit,
    circular: Boolean = false,
    selected: Boolean = false,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = if (circular) CircleShape else RoundedCornerShape(50)
    val buttonColor = if (prominent) {
        MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.92f else 0.78f)
    } else if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    } else {
        Color.White.copy(alpha = 0.13f)
    }
    val borderColor = if (prominent || selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(buttonColor, shape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = if (prominent) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun CenteredButtonLabel(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

internal fun clockHandEnd(
    center: Offset,
    length: Float,
    angleDegrees: Float,
): Offset {
    val radians = (angleDegrees - 90f) * PI / 180.0
    return Offset(
        x = center.x + (cos(radians) * length).toFloat(),
        y = center.y + (sin(radians) * length).toFloat(),
    )
}

private fun safeMainColorArgb(
    requestedMainColorArgb: Int,
    backgroundColorArgb: Int,
): Int {
    if (hasSafeThemeContrast(requestedMainColorArgb, backgroundColorArgb)) {
        return requestedMainColorArgb
    }

    return ThemeMainColorOptions.firstOrNull { colorArgb ->
        hasSafeThemeContrast(colorArgb, backgroundColorArgb)
    } ?: DEFAULT_MAIN_COLOR
}

private fun safeBackgroundColorArgb(
    requestedBackgroundColorArgb: Int,
    mainColorArgb: Int,
): Int {
    if (hasSafeThemeContrast(mainColorArgb, requestedBackgroundColorArgb)) {
        return requestedBackgroundColorArgb
    }

    return ThemeBackgroundColorOptions.firstOrNull { colorArgb ->
        hasSafeThemeContrast(mainColorArgb, colorArgb)
    } ?: DEFAULT_BACKGROUND_COLOR
}

private fun clockImageResIdForIndex(clockImageIndex: Int): Int {
    return when (clockImageIndex.coerceIn(0, ClockImageChoices.lastIndex)) {
        0 -> R.drawable.clock_dial_all_colors
        1 -> R.drawable.clock_dial_blue
        2 -> R.drawable.clock_dial_green
        3 -> R.drawable.clock_dial_orange
        4 -> R.drawable.clock_dial_purple
        5 -> R.drawable.clock_dial_white
        6 -> R.drawable.clock_dial_logo2
        7 -> R.drawable.clock_dial_sax
        8 -> R.drawable.clock_dial_piano
        9 -> R.drawable.clock_dial_guitar_gold
        10 -> R.drawable.clock_dial_trumpet
        11 -> R.drawable.clock_dial_rock
        else -> R.drawable.clock_dial_all_colors
    }
}

internal fun cycleOption(
    options: List<String>,
    current: String,
    step: Int,
): String {
    val currentIndex = options.indexOf(current).takeIf { it >= 0 } ?: 0
    return options[(currentIndex + step).wrap(options.size)]
}

fun String.toMusicalKeyRoot(): String {
    val key = trim()
    val root = MusicalKeyRoots
        .sortedByDescending { it.length }
        .firstOrNull { key.startsWith(it) }
        ?: "C"
    return when (root) {
        "C#" -> "Db"
        "D#" -> "Eb"
        "F#" -> "Gb"
        "G#" -> "Ab"
        "A#" -> "Bb"
        else -> root.takeIf { it in MusicalKeyRoots } ?: "C"
    }
}

fun String.toMusicalKeyModeSuffix(): String {
    val root = toMusicalKeyRoot()
    val suffix = trim().removePrefix(root)
    return MusicalKeyModeSuffixes.firstOrNull { it == suffix } ?: ""
}

internal fun Int.wrap(size: Int): Int {
    return ((this % size) + size) % size
}

private data class FreeSettingsTrialState(
    val settingsEnabled: Boolean,
    val buttonEnabled: Boolean,
    val buttonText: String,
    val statusText: String,
)

private fun currentEpochDay(): Long {
    return LocalDate.now().toEpochDay()
}

private fun freeSettingsTrialState(
    trialStartedDay: Long,
    todayEpochDay: Long,
): FreeSettingsTrialState {
    if (trialStartedDay <= 0L) {
        return FreeSettingsTrialState(
            settingsEnabled = false,
            buttonEnabled = true,
            buttonText = "30 Day Trial",
            statusText = "Settings locked",
        )
    }

    val daysSinceStart = (todayEpochDay - trialStartedDay).coerceAtLeast(0L)
    if (daysSinceStart < FREE_SETTINGS_TRIAL_DURATION_DAYS) {
        val daysLeft = FREE_SETTINGS_TRIAL_DURATION_DAYS - daysSinceStart
        return FreeSettingsTrialState(
            settingsEnabled = true,
            buttonEnabled = false,
            buttonText = "$daysLeft days left",
            statusText = "Trial active: $daysLeft days left",
        )
    }

    if (daysSinceStart < FREE_SETTINGS_TRIAL_RESET_DAYS) {
        val cooldownDay = (daysSinceStart - FREE_SETTINGS_TRIAL_DURATION_DAYS + 1)
            .coerceIn(1L, FREE_SETTINGS_TRIAL_COOLDOWN_DAYS.toLong())
        val daysUntilAvailable = FREE_SETTINGS_TRIAL_RESET_DAYS - daysSinceStart
        return FreeSettingsTrialState(
            settingsEnabled = false,
            buttonEnabled = false,
            buttonText = "Again in ${daysUntilAvailable}d",
            statusText = "Cooldown day $cooldownDay of $FREE_SETTINGS_TRIAL_COOLDOWN_DAYS",
        )
    }

    return FreeSettingsTrialState(
        settingsEnabled = false,
        buttonEnabled = true,
        buttonText = "30 Day Trial",
        statusText = "Trial ready again",
    )
}

private fun Context.loadKeepScreenMode(): KeepScreenMode {
    val preferences = getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    if (preferences.contains(KEEP_SCREEN_MODE_KEY)) {
        return KeepScreenMode.fromPersistedValue(
            preferences.getInt(KEEP_SCREEN_MODE_KEY, KeepScreenMode.Playing.persistedValue),
        )
    }
    return if (preferences.getBoolean(KEEP_SCREEN_AWAKE_WHILE_PLAYING_KEY, false)) {
        KeepScreenMode.Playing
    } else {
        KeepScreenMode.WatchTimeout
    }
}

private fun Context.saveKeepScreenMode(keepScreenMode: KeepScreenMode) {
    getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit {
            putInt(KEEP_SCREEN_MODE_KEY, keepScreenMode.persistedValue)
        }
}

private fun Context.loadSettingsInt(
    key: String,
    defaultValue: Int,
): Int {
    return getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getInt(key, defaultValue)
}

private fun Context.saveSettingsInt(
    key: String,
    value: Int,
) {
    getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit {
            putInt(key, value)
        }
}

private fun Context.loadSettingsLong(
    key: String,
    defaultValue: Long,
): Long {
    return getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getLong(key, defaultValue)
}

private fun Context.saveSettingsLong(
    key: String,
    value: Long,
) {
    getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit {
            putLong(key, value)
        }
}

private fun Context.showWatchFace() {
    findActivity()?.moveTaskToBack(true)
}

@Composable
private fun rememberAppCpuUsagePercent(enabled: Boolean): Float? {
    val sampler = remember { ProcessCpuSampler() }
    var cpuUsagePercent by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            cpuUsagePercent = null
            return@LaunchedEffect
        }

        sampler.reset()
        while (true) {
            delay(1_000L)
            cpuUsagePercent = sampler.sample()
        }
    }

    return cpuUsagePercent
}

private class ProcessCpuSampler {
    private val clockTicksPerSecond = runCatching {
        Os.sysconf(OsConstants._SC_CLK_TCK)
    }.getOrDefault(100L).coerceAtLeast(1L)
    private val processorCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    private var previousCpuTicks: Long? = null
    private var previousElapsedMs: Long? = null

    fun reset() {
        previousCpuTicks = readProcessCpuTicks()
        previousElapsedMs = SystemClock.elapsedRealtime()
    }

    fun sample(): Float? {
        val cpuTicks = readProcessCpuTicks() ?: return null
        val elapsedMs = SystemClock.elapsedRealtime()
        val lastCpuTicks = previousCpuTicks
        val lastElapsedMs = previousElapsedMs

        previousCpuTicks = cpuTicks
        previousElapsedMs = elapsedMs

        if (lastCpuTicks == null || lastElapsedMs == null) {
            return null
        }

        val cpuMs = (cpuTicks - lastCpuTicks).coerceAtLeast(0L) * 1_000f / clockTicksPerSecond
        val wallMs = (elapsedMs - lastElapsedMs).coerceAtLeast(1L).toFloat()
        return (cpuMs / (wallMs * processorCount) * 100f).coerceIn(0f, 100f)
    }

    private fun readProcessCpuTicks(): Long? {
        val stat = runCatching { File("/proc/self/stat").readText() }.getOrNull() ?: return null
        val commandEnd = stat.lastIndexOf(')')
        if (commandEnd == -1 || commandEnd + 2 >= stat.length) return null

        val fields = stat.substring(commandEnd + 2).trim().split(' ')
        val userTicks = fields.getOrNull(11)?.toLongOrNull() ?: return null
        val systemTicks = fields.getOrNull(12)?.toLongOrNull() ?: return null
        return userTicks + systemTicks
    }
}

internal fun Float?.formatCpuUsagePercent(): String {
    if (this == null) return "--"

    val tenths = (this * 10f).roundToInt().coerceIn(0, 1_000)
    return "${tenths / 10}.${tenths % 10}%"
}

@Composable
private fun CpuPercentOverlay(
    cpuUsagePercent: Float?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(start = 46.dp, top = 44.dp)
            .width(34.dp)
            .height(18.dp)
            .background(
                color = Color.Black.copy(alpha = 0.64f),
                shape = RoundedCornerShape(7.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cpuUsagePercent.formatCpuUsageCompactPercent(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Float?.formatCpuUsageCompactPercent(): String {
    if (this == null) return "--%"

    return "${roundToInt().coerceIn(0, 100)}%"
}

private fun MetronomeState.hasSameNonVisualStateAs(other: MetronomeState): Boolean {
    return bpm == other.bpm &&
        beatsPerMeasure == other.beatsPerMeasure &&
        accentBeat == other.accentBeat &&
        subdivisionCount == other.subdivisionCount &&
        beatAccentTypes == other.beatAccentTypes &&
        accentIntensityMode == other.accentIntensityMode &&
        accentIntensityRanges == other.accentIntensityRanges &&
        hapticsEnabled == other.hapticsEnabled &&
        beepEnabled == other.beepEnabled &&
        beatSoundMode == other.beatSoundMode &&
        keyDroneEnabled == other.keyDroneEnabled &&
        keyDroneVolumePercent == other.keyDroneVolumePercent &&
        musicalKey == other.musicalKey &&
        tempoNudgeMs == other.tempoNudgeMs &&
        playlistIndex == other.playlistIndex &&
        songIndex == other.songIndex &&
        isRunning == other.isRunning &&
        playbackStartedAtMs == other.playbackStartedAtMs
}

private fun MetronomeState.hasDifferentPulseVisualsFrom(other: MetronomeState): Boolean {
    return beatFlash != other.beatFlash ||
        flashingBeat != other.flashingBeat
}

private fun MetronomeState.withPulseVisualsFrom(previousState: MetronomeState): MetronomeState {
    return copy(
        beatFlash = previousState.beatFlash,
        flashingBeat = previousState.flashingBeat,
        currentBeatIndex = previousState.currentBeatIndex,
        currentSubdivisionIndex = previousState.currentSubdivisionIndex,
        beatClockStartedAtMs = previousState.beatClockStartedAtMs,
    )
}

private fun MetronomeState.withLightClockVisualsFrom(previousState: MetronomeState): MetronomeState {
    return copy(
        beatFlash = previousState.beatFlash,
        flashingBeat = previousState.flashingBeat,
        currentSubdivisionIndex = previousState.currentSubdivisionIndex,
    )
}

private fun List<BeatAccentType>.normalizedRhythmAccentTypes(
    beatsPerMeasure: Int,
    accentBeat: Int,
): List<BeatAccentType> {
    val safeBeatsPerMeasure = beatsPerMeasure.coerceIn(2, 16)
    val safeAccentBeat = accentBeat.coerceIn(1, safeBeatsPerMeasure)
    return List(safeBeatsPerMeasure) { index ->
        getOrNull(index) ?: if (index + 1 == safeAccentBeat) BeatAccentType.Big else BeatAccentType.Silent
    }
}

private fun List<BeatAccentType>.primaryRhythmAccentBeat(): Int {
    val bigAccentIndex = indexOfFirst { it == BeatAccentType.Big }
    if (bigAccentIndex >= 0) return bigAccentIndex + 1

    val audibleAccentIndex = indexOfFirst { it.hasBeep }
    if (audibleAccentIndex >= 0) return audibleAccentIndex + 1

    return 1
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun DefaultPreview() {
    WearApp()
}

@WearPreviewDevices
@WearPreviewFontScales
@Composable
fun SettingsPreview() {
    BPMMunkzPulseTheme {
        AppScaffold {
            SettingsPage(
                appText = appTextFor(AppLanguage.English),
                hapticsEnabled = false,
                beepEnabled = false,
                beatSoundMode = BeatSoundMode.Clicks,
                keyDroneEnabled = false,
                keyDroneVolumePercent = 18,
                tempoNudgeMs = DEFAULT_TEMPO_NUDGE_MS,
                accentIntensityMode = AccentIntensityMode.Big,
                accentIntensityRanges = defaultAccentIntensityRanges(),
                beatsPerMeasure = 4,
                beatAccentTypes = defaultBeatAccentTypes(4, 1),
                a4ReferenceHz = DEFAULT_A4_REFERENCE_HZ,
                keepScreenMode = KeepScreenMode.Playing,
                mainColorArgb = DEFAULT_MAIN_COLOR,
                backgroundColorArgb = DEFAULT_BACKGROUND_COLOR,
                clockColorArgb = DEFAULT_CLOCK_COLOR,
                clockImageIndex = DEFAULT_CLOCK_IMAGE_INDEX,
                ringColorArgb = DEFAULT_BIG_PULSE_RING_COLOR,
                bigRingFlashMode = DEFAULT_BIG_RING_FLASH_MODE,
                appLanguage = AppLanguage.English,
                appCpuUsagePercent = 2.4f,
                onHapticsToggle = {},
                onBeepToggle = {},
                onBeatSoundModeChoice = {},
                onKeyDroneToggle = {},
                onKeyDroneVolumeChange = {},
                onTempoNudgeChange = {},
                onAccentIntensityModeChoice = {},
                onAccentIntensityRangesChange = {},
                onRhythmPresetChoice = { _, _, _ -> },
                onA4ReferenceHzChange = {},
                onKeepScreenModeChoice = {},
                onMainColorChoice = {},
                onBackgroundColorChoice = {},
                onClockColorChoice = {},
                onClockImageChoice = {},
                onRingColorChoice = {},
                onBigRingModeChoice = {},
                onLanguageChoice = {},
            )
        }
    }
}




