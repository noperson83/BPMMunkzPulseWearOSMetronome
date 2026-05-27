package com.example.bpmmunkzpulse.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Paint
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.example.bpmmunkzpulse.R
import com.example.bpmmunkzpulse.presentation.theme.BPMMunkzPulseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal const val MIN_BPM = 30
internal const val MAX_BPM = 240
internal const val BEAT_FLASH_DURATION_MS = 80L
internal const val BEEP_DURATION_MS = 70
private const val APP_LAUNCH_SPLASH_DURATION_MS = 740L
private const val TAP_TEMPO_RESET_TIMEOUT_MS = 2_000L
private const val TAP_TEMPO_SAMPLE_COUNT = 5
private const val MAIN_PAGE_INDEX = 0
private const val RHYTHM_PAGE_INDEX = 1
private const val SETTINGS_PAGE_INDEX = 2
private const val TAP_TEMPO_FREE_PAGE_INDEX = 3
private const val PULSE_PAGE_COUNT = 4
private const val AUDIO_SAMPLE_RATE = 44_100
private const val AUDIO_FRAME_SIZE = 2_048
private const val SPECTRUM_BAR_COUNT = 28
private const val AUDIO_UI_UPDATE_INTERVAL_MS = 140L
private const val RHYTHM_VISUAL_MAX_DELAY_MS = 24L
private const val RHYTHM_VISUAL_WAKE_AHEAD_MS = 6L
private const val PAGER_TOUCH_VISUAL_QUIET_MS = 220L
private const val DEFAULT_A4_REFERENCE_HZ = 440
private const val MIN_A4_REFERENCE_HZ = 400
private const val MAX_A4_REFERENCE_HZ = 480
private const val TUNER_AVERAGE_SAMPLE_COUNT = 7
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
private const val PLAYLIST_PREFS = "bpm_munkz_playlists"
private const val PLAYLIST_LIBRARY_KEY = "playlist_library"

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
private const val NEON_GREEN_COLOR = -6422784
private const val DEFAULT_MAIN_COLOR = NEON_GREEN_COLOR
private const val DEFAULT_BACKGROUND_COLOR = -16769244
private const val DEFAULT_CLOCK_COLOR = NEON_GREEN_COLOR
private const val DEFAULT_BIG_PULSE_RING_COLOR = NEON_GREEN_COLOR
private val DEFAULT_BIG_RING_FLASH_MODE = BigRingFlashMode.Big
private const val DEFAULT_CLOCK_IMAGE_INDEX = 6
private const val DEFAULT_LANGUAGE_INDEX = 0
private const val RAINBOW_COLOR = 0x00ABCDEF

private val TimeSignatureBeatOptions = (2..16).toList()
private val SubdivisionOptions = listOf(1, 2, 3, 4, 6)

private val PulseColorOptions = listOf(
    -47872,
    -16715777,
    -32512,
    -7667457,
    NEON_GREEN_COLOR,
    -1,
    -65281,
    RAINBOW_COLOR,
)

private val ThemeMainColorOptions = listOf(
    -47872,
    -16715777,
    -32512,
    -7667457,
    NEON_GREEN_COLOR,
    -1,
    -65281,
)

private val RainbowColors = listOf(
    Color(0xFFFF3B30),
    Color(0xFFFFD60A),
    Color(0xFF32D74B),
    Color(0xFF64D2FF),
    Color(0xFFBF5AF2),
    Color(0xFFFF2D55),
)

private val ThemeBackgroundColorOptions = listOf(
    0xFF000000.toInt(),
    0xFF111827.toInt(),
    0xFF001F24.toInt(),
    0xFF1A1028.toInt(),
    0xFF24120A.toInt(),
    0xFF102016.toInt(),
)

private val MusicalKeyOptions = listOf(
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
    "Am",
    "Em",
    "Dm",
    "Gm",
)

private val SongNameOptions = listOf(
    "Intro",
    "Verse",
    "Chorus",
    "Bridge",
    "Solo",
    "Break",
    "Outro",
)

private val SongNoteOptions = listOf(
    "Count in",
    "Hold tempo",
    "Big accents",
    "Keep pocket",
    "Breakdown",
    "Loop twice",
    "End tight",
)

private data class PlaylistSong(
    val name: String,
    val bpm: Int,
    val beatsPerMeasure: Int,
    val accentBeat: Int,
    val subdivisionCount: Int,
    val beatAccentTypes: List<BeatAccentType>,
    val accentIntensityMode: AccentIntensityMode,
    val musicalKey: String,
    val note: String,
)

private data class SavedPlaylist(
    val name: String,
    val songs: List<PlaylistSong>,
)

private data class ClockImageChoice(
    val label: String,
    val spanishLabel: String = label,
)

private data class BigRingModeChoice(
    val mode: BigRingFlashMode,
    val label: String,
    val spanishLabel: String = label,
)

private data class AccentIntensityChoice(
    val mode: AccentIntensityMode,
    val label: String,
    val spanishLabel: String = label,
)

private data class BeatVisualState(
    val currentBeatIndex: Int,
    val currentSubdivisionIndex: Int,
    val beatFlash: Boolean,
)

private data class AudioAnalysisState(
    val frequencyHz: Float? = null,
    val noteName: String = "--",
    val cents: Int = 0,
    val level: Float = 0f,
    val detectedTempoBpm: Int? = null,
    val recentNotes: List<String> = emptyList(),
    val guessedKey: String? = null,
    val likelyChords: List<String> = emptyList(),
    val spectrum: List<Float> = List(SPECTRUM_BAR_COUNT) { 0f },
)

private data class KeyAnalysis(
    val recentNotes: List<String>,
    val guessedKey: String?,
    val likelyChords: List<String>,
)

private data class KeyGuess(
    val rootIndex: Int,
    val displayName: String,
    val scale: ScaleProfile,
    val score: Float,
)

private data class ScaleProfile(
    val displaySuffix: String,
    val offsets: List<Int>,
    val chordQualities: List<String>,
)

private data class SpectrumPeak(
    val frequencyHz: Float,
    val level: Float,
    val bandLabel: String,
)

private data class SpectrumBand(
    val startHz: Float,
    val endHz: Float,
    val label: String,
    val color: Color,
)

private enum class TunerListenProfile(
    val label: String,
    val spanishLabel: String,
    val minHz: Float,
    val maxHz: Float,
) {
    Full("Full", "Todo", 55f, 1_600f),
    Guitar("Gtr", "Guit", 75f, 1_200f),
    Voice("Vox", "Voz", 110f, 1_100f),
    Bass("Bass", "Bajo", 38f, 420f),
}

private enum class AppLanguage {
    English,
    Spanish,
}

private enum class BigRingFlashMode(val persistedValue: Int) {
    All(0),
    Big(1),
    Off(2);

    companion object {
        fun fromPersistedValue(value: Int): BigRingFlashMode {
            return entries.firstOrNull { it.persistedValue == value } ?: Big
        }
    }
}

private enum class RhythmChoicePicker {
    TimeSignature,
    Subdivision,
}

private enum class KeepScreenMode(val persistedValue: Int) {
    AppOpen(0),
    Playing(1),
    WatchTimeout(2);

    companion object {
        fun fromPersistedValue(value: Int): KeepScreenMode {
            return entries.firstOrNull { it.persistedValue == value } ?: Playing
        }
    }
}

private data class AppText(
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
    val intensity: String,
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

private val ClockImageChoices = listOf(
    ClockImageChoice("Rainb", "Arco"),
    ClockImageChoice("Blue", "Azul"),
    ClockImageChoice("Green", "Verde"),
    ClockImageChoice("Orange", "Naran"),
    ClockImageChoice("Purple", "Morad"),
    ClockImageChoice("White", "Blanc"),
    ClockImageChoice("Munk"),
    ClockImageChoice("Sax"),
    ClockImageChoice("Piano"),
    ClockImageChoice("Gtr", "Guit"),
    ClockImageChoice("Trum", "Trom"),
    ClockImageChoice("Rock"),
)

private val BigRingModeChoices = listOf(
    BigRingModeChoice(BigRingFlashMode.All, "All", "Todo"),
    BigRingModeChoice(BigRingFlashMode.Big, "Big", "Big"),
    BigRingModeChoice(BigRingFlashMode.Off, "Off", "Off"),
)

private val AccentIntensityChoices = listOf(
    AccentIntensityChoice(AccentIntensityMode.Big, "Big", "Gran"),
    AccentIntensityChoice(AccentIntensityMode.Medium, "Mid", "Med"),
    AccentIntensityChoice(AccentIntensityMode.Little, "Lil", "Peq"),
    AccentIntensityChoice(AccentIntensityMode.Silent, "Sil", "Sil"),
)

private val AppLanguages = listOf(
    AppLanguage.English,
    AppLanguage.Spanish,
)

private fun ClockImageChoice.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

private fun BigRingModeChoice.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

private fun AccentIntensityChoice.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

private fun TunerListenProfile.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

private fun BigRingFlashMode.shouldFlashRing(
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
            rhythm = "Pulse",
            timeSignature = "Time Signature",
            subdivision = "Sub Divisions",
            saveSong = "Save Song",
            newSong = "New Song",
            beat = "Beat",
            beatCount = "Beat count",
            bigPulse = "Big pulse",
            haptics = "Vibration",
            beep = "Beep",
            intensity = "Intensity",
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
            bigRing = "Big ring",
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
            rhythm = "Pulso",
            timeSignature = "Compas",
            subdivision = "Sub Divisiones",
            saveSong = "Guardar",
            newSong = "Nueva",
            beat = "Compas",
            beatCount = "Beats",
            bigPulse = "Pulso grande",
            haptics = "Vibracion",
            beep = "Pitido",
            intensity = "Intensidad",
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
            bigRing = "Aro grande",
            language = "Idioma",
            big = "GRAN",
            edit = "Editar",
            editRhythm = "Editar ritmo",
            deleteSong = "Borrar",
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
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
                BeatPulseScreen()
            }
        }
    }
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
            painter = painterResource(id = R.drawable.bpm_munkz_app_logo),
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
fun BeatPulseScreen() {
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
    val selectedPlaylistIndexState = rememberSaveable { mutableIntStateOf(0) }
    val selectedSongIndexState = rememberSaveable { mutableIntStateOf(0) }
    var playlistEditorPopupOpen by rememberSaveable { mutableStateOf(false) }
    var playlistRhythmEditorPopupOpen by rememberSaveable { mutableStateOf(false) }
    var rhythmEditorPopupOpen by rememberSaveable { mutableStateOf(false) }
    var tapTempoPopupOpen by rememberSaveable { mutableStateOf(false) }
    var tunerOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var spectrumOverlayOpen by rememberSaveable { mutableStateOf(false) }
    var tunerListenProfile by rememberSaveable { mutableStateOf(TunerListenProfile.Full) }
    val tapTempoTimes = remember { mutableListOf<Long>() }
    val pagerState = rememberPagerState(pageCount = { PULSE_PAGE_COUNT })
    val playlistIndex = selectedPlaylistIndexState.intValue.coerceIn(0, playlists.lastIndex)
    val currentPlaylist = playlists[playlistIndex]
    val songIndex = selectedSongIndexState.intValue.coerceIn(0, currentPlaylist.songs.lastIndex)
    val currentSong = currentPlaylist.songs[songIndex]
    val selectedClockImageIndex = clockImageIndexState.intValue.coerceIn(0, ClockImageChoices.lastIndex)
    val bigRingFlashMode = BigRingFlashMode.fromPersistedValue(bigRingModeState.intValue)
    val appLanguage = AppLanguages[appLanguageIndexState.intValue.coerceIn(0, AppLanguages.lastIndex)]
    val appText = appTextFor(appLanguage)
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
    val audioOverlayOpen = tunerOverlayOpen || spectrumOverlayOpen
    val audioAnalysisState = rememberAudioAnalysisState(
        enabled = micPermissionGranted && audioOverlayOpen && !isPreview,
        listenProfile = tunerListenProfile,
        a4ReferenceHz = a4ReferenceHz,
        includeSpectrum = spectrumOverlayOpen,
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
                        if (serviceState.isRunning) {
                            val servicePlaylistIndex = serviceState.playlistIndex.coerceIn(0, playlists.lastIndex)
                            selectedPlaylistIndexState.intValue = servicePlaylistIndex
                            selectedSongIndexState.intValue = serviceState.songIndex.coerceIn(
                                0,
                                playlists[servicePlaylistIndex].songs.lastIndex,
                            )
                        }
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

    fun filterMetronomeStateForVisibleUi(state: MetronomeState): MetronomeState {
        if (!state.isRunning) return state

        val currentPage = pagerState.currentPage
        val pagerIsMovingOrBetweenPages = pagerState.isScrollInProgress ||
            abs(pagerState.currentPageOffsetFraction) > 0.01f
        val pointerIsPreparingGesture = pointerIsDown ||
            SystemClock.elapsedRealtime() < quietPulseVisualsUntilMs

        if (pagerIsMovingOrBetweenPages || pointerIsPreparingGesture) {
            return state.withPulseVisualsFrom(metronomeState)
        }

        val shouldShowFullPulse =
            (!playlistEditorPopupOpen || playlistRhythmEditorPopupOpen || rhythmEditorPopupOpen) &&
            (
                tapTempoPopupOpen ||
                    playlistRhythmEditorPopupOpen ||
                    rhythmEditorPopupOpen ||
                    currentPage == RHYTHM_PAGE_INDEX ||
                    currentPage == TAP_TEMPO_FREE_PAGE_INDEX
                )
        val shouldShowLightClockPulse = !playlistEditorPopupOpen &&
            currentPage == MAIN_PAGE_INDEX

        val filteredState = when {
            shouldShowFullPulse -> state
            shouldShowLightClockPulse -> state.withLightClockVisualsFrom(metronomeState)
            else -> state.withPulseVisualsFrom(metronomeState)
        }

        return if (bigRingFlashMode == BigRingFlashMode.Off) {
            filteredState
        } else {
            filteredState.copy(
                beatFlash = state.beatFlash,
                flashingBeat = state.flashingBeat,
            )
        }
    }

    LaunchedEffect(
        metronomeService,
        tapTempoPopupOpen,
        playlistEditorPopupOpen,
        playlistRhythmEditorPopupOpen,
        rhythmEditorPopupOpen,
        bigRingFlashMode,
    ) {
        metronomeService?.state?.collect { state ->
            val nextState = filterMetronomeStateForVisibleUi(state)
            val shouldUpdateRingVisuals = bigRingFlashMode != BigRingFlashMode.Off &&
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
        bigRingFlashMode,
    ) {
        snapshotFlow {
            pagerState.isScrollInProgress || abs(pagerState.currentPageOffsetFraction) > 0.01f
        }.collect { pagerIsMovingOrBetweenPages ->
            if (!pagerIsMovingOrBetweenPages) {
                metronomeService?.state?.value?.let { state ->
                    val nextState = filterMetronomeStateForVisibleUi(state)
                    val shouldUpdateRingVisuals = bigRingFlashMode != BigRingFlashMode.Off &&
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

    fun saveCurrentRhythmToSong() {
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
        if (pagerState.currentPage == MAIN_PAGE_INDEX && !isRunning) {
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
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
                        playbackStartedAtMs = playbackStartedAtMs,
                        beatVisualsEnabled = !pagerState.isScrollInProgress &&
                            abs(pagerState.currentPageOffsetFraction) <= 0.01f,
                        onEditRhythm = {
                            rhythmEditorPopupOpen = true
                        },
                        onToggleRunning = toggleRunning,
                        onBpmClick = {
                            tapTempoPopupOpen = true
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
                    accentIntensityMode = accentIntensityMode,
                    accentIntensityRanges = metronomeState.accentIntensityRanges,
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
                        beatFlash = beatFlash,
                        isAccentFlash = flashingBeat == accentBeat,
                        isRunning = isRunning,
                        onOpenTuner = { tunerOverlayOpen = true },
                        onOpenSpectrum = { spectrumOverlayOpen = true },
                        onTapTempo = recordTapTempo,
                        onDecrease = decreaseBpm,
                        onDecreaseLarge = decreaseBpmLarge,
                        onIncrease = increaseBpm,
                        onIncreaseLarge = increaseBpmLarge,
                        onToggleRunning = toggleRunning,
                    )
                }
            }

            BigPulseRingOverlay(
                beatFlash = beatFlash,
                flashingAccentType = beatAccentTypes.typeForBeat(flashingBeat),
                bigRingFlashMode = bigRingFlashMode,
                colorArgb = bigPulseRingColorArgb,
                modifier = Modifier.fillMaxSize(),
            )

            CpuPercentOverlay(
                cpuUsagePercent = appCpuUsagePercent,
                modifier = Modifier.align(Alignment.TopStart),
            )

            if (isRunning && !audioOverlayOpen) {
                QuickStopOverlay(
                    text = appText.stop,
                    onStop = toggleRunning,
                    modifier = Modifier.align(Alignment.TopEnd),
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
                        selectedProfile = tunerListenProfile,
                        micPermissionGranted = micPermissionGranted,
                        onProfileChoice = { tunerListenProfile = it },
                        onSaveKey = { guessedKey ->
                            updateCurrentSong { song -> song.copy(musicalKey = guessedKey) }
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
                        audioAnalysisState = audioAnalysisState,
                        a4ReferenceHz = a4ReferenceHz,
                        micPermissionGranted = micPermissionGranted,
                        onRequestMicPermission = {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                    )
                }
            }

            if (!audioOverlayOpen && pagerState.currentPage != MAIN_PAGE_INDEX && pagerState.currentPage != RHYTHM_PAGE_INDEX) {
                PulsePagerIndicator(
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }

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
                    onEditRhythm = {
                        playlistRhythmEditorPopupOpen = true
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
                    onTimeSignatureChoice = { beatChoice ->
                        metronomeService?.setBeatsPerMeasure(beatChoice)
                    },
                    onBeatAccentTypeCycle = { beatChoice ->
                        metronomeService?.cycleBeatAccentType(beatChoice)
                    },
                    onSubdivisionChoice = { subdivision ->
                        metronomeService?.setSubdivisionCount(subdivision)
                    },
                    onBpmClick = {
                        tapTempoPopupOpen = true
                    },
                    onDone = { savePlaylistRhythmEditorAndClose() },
                )
            }

            if (rhythmEditorPopupOpen) {
                RhythmEditorPopup(
                    appText = appText,
                    bpm = bpm,
                    beatsPerMeasure = beatsPerMeasure,
                    subdivisionCount = subdivisionCount,
                    beatAccentTypes = beatAccentTypes,
                    currentBeatIndex = currentBeatIndex,
                    currentSubdivisionIndex = currentSubdivisionIndex,
                    beatFlash = beatFlash,
                    onTimeSignatureChoice = { beatChoice ->
                        metronomeService?.setBeatsPerMeasure(beatChoice)
                    },
                    onBeatAccentTypeCycle = { beatChoice ->
                        metronomeService?.cycleBeatAccentType(beatChoice)
                    },
                    onSubdivisionChoice = { subdivision ->
                        metronomeService?.setSubdivisionCount(subdivision)
                    },
                    onBpmClick = {
                        tapTempoPopupOpen = true
                    },
                    onDone = { saveRhythmEditorAndClose() },
                )
            }

            if (tapTempoPopupOpen) {
                TapTempoPopup(
                    appText = appText,
                    bpm = bpm,
                    beatFlash = beatFlash,
                    isAccentFlash = flashingBeat == accentBeat,
                    isRunning = isRunning,
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
                    onDismiss = { tapTempoPopupOpen = false },
                )
            }
        }
    }
}

@Composable
private fun TapTempoFree(
    appText: AppText,
    bpm: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    isRunning: Boolean,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
    onTapTempo: () -> Unit,
    onDecrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncrease: () -> Unit,
    onIncreaseLarge: () -> Unit,
    onToggleRunning: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        TapTempoControls(
            appText = appText,
            bpm = bpm,
            beatFlash = beatFlash,
            isAccentFlash = isAccentFlash,
            onTapTempo = onTapTempo,
            onDecrease = onDecrease,
            onDecreaseLarge = onDecreaseLarge,
            onIncrease = onIncrease,
            onIncreaseLarge = onIncreaseLarge,
        ) {
            StartStopButton(
                appText = appText,
                isRunning = isRunning,
                onClick = onToggleRunning,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-38).dp),
            contentAlignment = Alignment.Center,
        ) {
            AudioToolButtons(
                appText = appText,
                onOpenTuner = onOpenTuner,
                onOpenSpectrum = onOpenSpectrum,
            )
        }
    }
}

@Composable
private fun TapTempoControls(
    appText: AppText,
    bpm: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    onTapTempo: () -> Unit,
    onDecrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncrease: () -> Unit,
    onIncreaseLarge: () -> Unit,
    readoutOffsetY: Dp = 0.dp,
    readoutBottomSpacing: Dp = 8.dp,
    adjustRowOffsetY: Dp = 0.dp,
    footerTopSpacing: Dp = 6.dp,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit,
) {
    BeatPulsePage {
        BeatTempoReadout(
            bpm = bpm,
            beatFlash = beatFlash,
            isAccentFlash = isAccentFlash,
            modifier = Modifier.offset(y = readoutOffsetY),
        )

        Spacer(modifier = Modifier.height(readoutBottomSpacing))

        header()

        Row(
            modifier = Modifier.offset(y = adjustRowOffsetY),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TempoAdjustButton(
                text = "-",
                onClick = onDecrease,
                onLongClick = onDecreaseLarge,
                onLongClickLabel = appText.decreaseBpmBy5,
            )

            FastTapTempoButton(
                text = appText.tap,
                onTap = onTapTempo,
            )

            TempoAdjustButton(
                text = "+",
                onClick = onIncrease,
                onLongClick = onIncreaseLarge,
                onLongClickLabel = appText.increaseBpmBy5,
            )
        }

        Spacer(modifier = Modifier.height(footerTopSpacing))

        footer()
    }
}

@Composable
private fun TapTempoPopup(
    appText: AppText,
    bpm: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    isRunning: Boolean,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
    onTapTempo: () -> Unit,
    onDecrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncrease: () -> Unit,
    onIncreaseLarge: () -> Unit,
    onToggleRunning: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        TapTempoControls(
            appText = appText,
            bpm = bpm,
            beatFlash = beatFlash,
            isAccentFlash = isAccentFlash,
            onTapTempo = onTapTempo,
            onDecrease = onDecrease,
            onDecreaseLarge = onDecreaseLarge,
            onIncrease = onIncrease,
            onIncreaseLarge = onIncreaseLarge,
            readoutOffsetY = 0.dp,
            readoutBottomSpacing = 0.dp,
            adjustRowOffsetY = (-6).dp,
            footerTopSpacing = 0.dp,
            header = {
                AudioToolButtons(
                    appText = appText,
                    onOpenTuner = onOpenTuner,
                    onOpenSpectrum = onOpenSpectrum,
                    modifier = Modifier
                        .width(164.dp)
                        .height(22.dp),
                )

                Spacer(modifier = Modifier.height(0.dp))

            },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StartStopButton(
                    appText = appText,
                    isRunning = isRunning,
                    onClick = onToggleRunning,
                )

                SmallCommandButton(
                    text = appText.done,
                    modifier = Modifier
                        .width(66.dp)
                        .height(34.dp),
                    fontSize = 10.sp,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun RhythmSetupPage(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    isRunning: Boolean,
    beatClockStartedAtMs: Long,
    playbackStartedAtMs: Long,
    beatVisualsEnabled: Boolean,
    onEditRhythm: () -> Unit,
    onToggleRunning: () -> Unit,
    onBpmClick: () -> Unit,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
) {
    val beatVisualState = rememberRhythmBeatVisualState(
        isRunning = isRunning,
        animationEnabled = beatVisualsEnabled,
        bpm = bpm,
        beatsPerMeasure = beatsPerMeasure,
        subdivisionCount = subdivisionCount,
        beatClockStartedAtMs = beatClockStartedAtMs,
        fallbackBeatIndex = currentBeatIndex,
        fallbackSubdivisionIndex = currentSubdivisionIndex,
        fallbackBeatFlash = beatFlash,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        RhythmLiveCanvas(
            appText = appText,
            bpm = bpm,
            beatsPerMeasure = beatsPerMeasure,
            subdivisionCount = subdivisionCount,
            beatAccentTypes = beatAccentTypes,
            currentBeatIndex = beatVisualState.currentBeatIndex,
            currentSubdivisionIndex = beatVisualState.currentSubdivisionIndex,
            beatFlash = beatVisualState.beatFlash,
            isRunning = isRunning,
            playbackStartedAtMs = playbackStartedAtMs,
            modifier = Modifier.fillMaxSize(),
            onBpmClick = onBpmClick,
            onEdit = onEditRhythm,
            onToggleRunning = onToggleRunning,
            onOpenTuner = onOpenTuner,
            onOpenSpectrum = onOpenSpectrum,
        )
    }
}

@Composable
private fun RhythmLiveCanvas(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    isRunning: Boolean,
    playbackStartedAtMs: Long,
    modifier: Modifier = Modifier,
    onBpmClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleRunning: () -> Unit,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
) {
    BigPulseCircleSelector(
        beatsPerMeasure = beatsPerMeasure,
        beatAccentTypes = beatAccentTypes,
        currentBeatIndex = currentBeatIndex,
        beatFlash = beatFlash,
        modifier = modifier,
        centerContentWidth = 192.dp,
        onBeatAccentTypeCycle = null,
        bottomContent = {
            RhythmElapsedTimer(
                isRunning = isRunning,
                playbackStartedAtMs = playbackStartedAtMs,
            )
        },
    ) {
        Box(
            modifier = Modifier.width(192.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$bpm",
                    modifier = Modifier.clickable(onClick = onBpmClick),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.width(150.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassCommandButton(
                        text = appText.tuner,
                        modifier = Modifier
                            .width(40.dp)
                            .height(21.dp),
                        fontSize = 8.sp,
                        selected = false,
                        prominent = false,
                        onClick = onOpenTuner,
                    )

                    Text(
                        text = "$currentBeatIndex/$beatsPerMeasure",
                        modifier = Modifier.width(68.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )

                    GlassCommandButton(
                        text = "Spect",
                        modifier = Modifier
                            .width(40.dp)
                            .height(21.dp),
                        fontSize = 8.sp,
                        selected = false,
                        prominent = false,
                        onClick = onOpenSpectrum,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SubdivisionDots(
                    subdivisionCount = subdivisionCount,
                    currentSubdivisionIndex = currentSubdivisionIndex,
                    activeSize = 18.dp,
                    inactiveSize = 10.dp,
                    spacing = 3.dp,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    GlassCommandButton(
                        text = if (isRunning) appText.stopUpper else appText.startUpper,
                        modifier = Modifier
                            .width(104.dp)
                            .height(30.dp),
                        fontSize = 15.sp,
                        selected = isRunning,
                        prominent = true,
                        onClick = onToggleRunning,
                    )

                    GlassCommandButton(
                        text = appText.edit,
                        modifier = Modifier
                            .width(42.dp)
                            .height(24.dp),
                        fontSize = 9.sp,
                        onClick = onEdit,
                    )
                }
            }
        }
    }
}

@Composable
private fun RhythmElapsedTimer(
    isRunning: Boolean,
    playbackStartedAtMs: Long,
    modifier: Modifier = Modifier,
) {
    var elapsedMs by remember(isRunning, playbackStartedAtMs) {
        mutableLongStateOf(elapsedTimerMs(isRunning, playbackStartedAtMs))
    }

    LaunchedEffect(isRunning, playbackStartedAtMs) {
        if (!isRunning || playbackStartedAtMs <= 0L) {
            elapsedMs = 0L
            return@LaunchedEffect
        }

        while (true) {
            elapsedMs = elapsedTimerMs(isRunning = true, playbackStartedAtMs = playbackStartedAtMs)
            delay(250)
        }
    }

    if (!isRunning && playbackStartedAtMs <= 0L) {
        return
    }

    Box(
        modifier = modifier
            .width(58.dp)
            .height(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatElapsedTimer(elapsedMs),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isRunning) 0.94f else 0.48f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun RhythmEditDetails(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    modifier: Modifier = Modifier,
    onTimeSignatureChoice: (Int) -> Unit,
    onBeatAccentTypeCycle: (Int) -> Unit,
    onSubdivisionChoice: (Int) -> Unit,
    onTimeSignaturePickerClick: () -> Unit,
    onSubdivisionPickerClick: () -> Unit,
    onBpmClick: () -> Unit,
    onDone: () -> Unit,
) {
    BigPulseCircleSelector(
        beatsPerMeasure = beatsPerMeasure,
        beatAccentTypes = beatAccentTypes,
        currentBeatIndex = currentBeatIndex,
        beatFlash = beatFlash,
        modifier = modifier,
        onBeatAccentTypeCycle = onBeatAccentTypeCycle,
        bottomContent = {
            SmallCommandButton(
                text = appText.done,
                modifier = Modifier
                    .width(64.dp)
                    .height(24.dp),
                fontSize = 9.sp,
                onClick = onDone,
            )
        },
    ) {
        SmallCommandButton(
            text = "$bpm BPM",
            modifier = Modifier
                .width(78.dp)
                .height(28.dp),
            fontSize = 13.sp,
            onClick = onBpmClick,
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = appText.timeSignature,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(2.dp))

        RhythmValueStepper(
            valueText = "$beatsPerMeasure/4",
            onDecrease = {
                onTimeSignatureChoice(steppedOption(TimeSignatureBeatOptions, beatsPerMeasure, -1))
            },
            onValueClick = onTimeSignaturePickerClick,
            onIncrease = {
                onTimeSignatureChoice(steppedOption(TimeSignatureBeatOptions, beatsPerMeasure, 1))
            },
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = appText.subdivision,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(2.dp))

        RhythmValueStepper(
            valueText = "$subdivisionCount",
            onDecrease = {
                onSubdivisionChoice(steppedOption(SubdivisionOptions, subdivisionCount, -1))
            },
            onValueClick = onSubdivisionPickerClick,
            onIncrease = {
                onSubdivisionChoice(steppedOption(SubdivisionOptions, subdivisionCount, 1))
            },
        )

        Spacer(modifier = Modifier.height(3.dp))

        SubdivisionDots(
            subdivisionCount = subdivisionCount,
            currentSubdivisionIndex = currentSubdivisionIndex,
        )
    }
}

@Composable
private fun RhythmEditorPopup(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    onTimeSignatureChoice: (Int) -> Unit,
    onBeatAccentTypeCycle: (Int) -> Unit,
    onSubdivisionChoice: (Int) -> Unit,
    onBpmClick: () -> Unit,
    onDone: () -> Unit,
) {
    var activeChoicePicker by rememberSaveable { mutableStateOf<RhythmChoicePicker?>(null) }

    DismissibleEditorPopup(onDone = onDone) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RhythmEditDetails(
                appText = appText,
                bpm = bpm,
                beatsPerMeasure = beatsPerMeasure,
                subdivisionCount = subdivisionCount,
                beatAccentTypes = beatAccentTypes,
                currentBeatIndex = currentBeatIndex,
                currentSubdivisionIndex = currentSubdivisionIndex,
                beatFlash = beatFlash,
                modifier = Modifier.fillMaxSize(),
                onTimeSignatureChoice = onTimeSignatureChoice,
                onBeatAccentTypeCycle = onBeatAccentTypeCycle,
                onSubdivisionChoice = onSubdivisionChoice,
                onTimeSignaturePickerClick = {
                    activeChoicePicker = RhythmChoicePicker.TimeSignature
                },
                onSubdivisionPickerClick = {
                    activeChoicePicker = RhythmChoicePicker.Subdivision
                },
                onBpmClick = onBpmClick,
                onDone = onDone,
            )

            activeChoicePicker?.let { picker ->
                RhythmChoicePopup(
                    title = if (picker == RhythmChoicePicker.TimeSignature) {
                        appText.timeSignature
                    } else {
                        appText.subdivision
                    },
                    options = if (picker == RhythmChoicePicker.TimeSignature) {
                        TimeSignatureBeatOptions
                    } else {
                        SubdivisionOptions
                    },
                    selectedOption = if (picker == RhythmChoicePicker.TimeSignature) {
                        beatsPerMeasure
                    } else {
                        subdivisionCount
                    },
                    dismissText = appText.done,
                    optionLabel = { option ->
                        if (picker == RhythmChoicePicker.TimeSignature) "$option/4" else "$option"
                    },
                    onOptionChoice = { option ->
                        if (picker == RhythmChoicePicker.TimeSignature) {
                            onTimeSignatureChoice(option)
                        } else {
                            onSubdivisionChoice(option)
                        }
                        activeChoicePicker = null
                    },
                    onDismiss = {
                        activeChoicePicker = null
                    },
                )
            }
        }
    }
}

@Composable
private fun RhythmValueStepper(
    valueText: String,
    onDecrease: () -> Unit,
    onValueClick: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallCommandButton(
            text = "-",
            modifier = Modifier
                .width(26.dp)
                .height(24.dp),
            fontSize = 12.sp,
            onClick = onDecrease,
        )

        SmallCommandButton(
            text = valueText,
            modifier = Modifier
                .width(48.dp)
                .height(24.dp),
            fontSize = 11.sp,
            onClick = onValueClick,
        )

        SmallCommandButton(
            text = "+",
            modifier = Modifier
                .width(26.dp)
                .height(24.dp),
            fontSize = 12.sp,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun RhythmChoicePopup(
    title: String,
    options: List<Int>,
    selectedOption: Int,
    dismissText: String,
    optionLabel: (Int) -> String,
    onOptionChoice: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(7.dp))

            options.chunked(4).forEach { optionRow ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    optionRow.forEach { option ->
                        SettingButton(
                            text = optionLabel(option),
                            selected = selectedOption == option,
                            modifier = Modifier
                                .width(40.dp)
                                .height(28.dp),
                            onClick = { onOptionChoice(option) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            SmallCommandButton(
                text = dismissText,
                modifier = Modifier
                    .width(62.dp)
                    .height(26.dp),
                fontSize = 10.sp,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun BigPulseCircleSelector(
    beatsPerMeasure: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    beatFlash: Boolean,
    modifier: Modifier = Modifier,
    centerContentWidth: Dp = 128.dp,
    onBeatAccentTypeCycle: ((Int) -> Unit)?,
    bottomContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val beatCount = beatsPerMeasure.coerceIn(2, 16)
    val rightCount = (beatCount + 1) / 2
    val leftCount = beatCount - rightCount
    val hitSize = when {
        beatCount > 12 -> 26.dp
        beatCount > 8 -> 30.dp
        else -> 34.dp
    }
    val circleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val ringSize = if (maxWidth < maxHeight) maxWidth else maxHeight
        val radius = (ringSize / 2) - (hitSize / 2) - 2.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val ringRadius = ((size.minDimension / 2f) - (hitSize.toPx() / 2f) - 2.dp.toPx())
                .coerceAtLeast(0f)
            drawCircle(
                color = circleColor,
                radius = ringRadius,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = strokeWidth),
            )
        }

        Column(
            modifier = Modifier.width(centerContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }

        (1..rightCount).forEach { index ->
            val angle = splitCircleAngle(index, rightCount, rightSide = true)
            BeatAccentDotButton(
                accentType = beatAccentTypes.typeForBeat(index),
                beatFlash = beatFlash && currentBeatIndex == index,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (maxWidth / 2) + (radius * cos(angle).toFloat()) - (hitSize / 2),
                        y = (maxHeight / 2) + (radius * sin(angle).toFloat()) - (hitSize / 2),
                    )
                    .size(hitSize),
                onClick = onBeatAccentTypeCycle?.let { onBeatChoice ->
                    { onBeatChoice(index) }
                },
            )
        }

        (1..leftCount).forEach { index ->
            val beat = rightCount + index
            val angle = splitCircleAngle(index, leftCount, rightSide = false)
            BeatAccentDotButton(
                accentType = beatAccentTypes.typeForBeat(beat),
                beatFlash = beatFlash && currentBeatIndex == beat,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (maxWidth / 2) + (radius * cos(angle).toFloat()) - (hitSize / 2),
                        y = (maxHeight / 2) + (radius * sin(angle).toFloat()) - (hitSize / 2),
                    )
                    .size(hitSize),
                onClick = onBeatAccentTypeCycle?.let { onBeatChoice ->
                    { onBeatChoice(beat) }
                },
            )
        }

        bottomContent?.let { footer ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                footer()
            }
        }
    }
}

private fun splitCircleAngle(
    index: Int,
    count: Int,
    rightSide: Boolean,
): Double {
    val startDegrees = if (rightSide) -58.0 else 122.0
    val endDegrees = if (rightSide) 58.0 else 238.0
    val progress = if (count <= 1) 0.0 else (index - 1).toDouble() / (count - 1).toDouble()
    val angleDegrees = startDegrees + ((endDegrees - startDegrees) * progress)

    return angleDegrees * PI / 180.0
}

@Composable
private fun BeatAccentDotButton(
    accentType: BeatAccentType,
    beatFlash: Boolean,
    modifier: Modifier,
    onClick: (() -> Unit)?,
) {
    val baseDotSize = when (accentType) {
        BeatAccentType.Big -> 20.dp
        BeatAccentType.Medium -> 16.dp
        BeatAccentType.Small -> 13.dp
        BeatAccentType.Silent -> 20.dp
    }
    val dotRadius = (if (beatFlash) baseDotSize + 6.dp else baseDotSize) / 2
    val primaryColor = MaterialTheme.colorScheme.primary
    val dotAlpha = when (accentType) {
        BeatAccentType.Big -> if (beatFlash) 1f else 0.9f
        BeatAccentType.Medium -> if (beatFlash) 1f else 0.72f
        BeatAccentType.Small -> if (beatFlash) 0.95f else 0.56f
        BeatAccentType.Silent -> 0f
    }
    val borderAlpha = if (beatFlash) 0.95f else 0.58f
    val borderWidth = if (beatFlash) 3.dp else 2.dp

    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            if (accentType == BeatAccentType.Silent) {
                drawCircle(
                    color = primaryColor.copy(alpha = borderAlpha),
                    radius = dotRadius.toPx(),
                    center = center,
                    style = Stroke(width = borderWidth.toPx()),
                )
            } else {
                drawCircle(
                    color = primaryColor.copy(alpha = dotAlpha),
                    radius = dotRadius.toPx(),
                    center = center,
                )
            }
        }
    }
}

private fun steppedOption(
    options: List<Int>,
    current: Int,
    step: Int,
): Int {
    val currentIndex = options.indexOf(current).takeIf { it >= 0 }
        ?: options.indexOf(options.minBy { abs(it - current) })
    return options[(currentIndex + step).coerceIn(0, options.lastIndex)]
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

@Composable
private fun SubdivisionDots(
    subdivisionCount: Int,
    currentSubdivisionIndex: Int,
    activeSize: Dp = 7.dp,
    inactiveSize: Dp = 5.dp,
    spacing: Dp = 5.dp,
) {
    val slotSize = if (activeSize > inactiveSize) activeSize else inactiveSize
    val dotColor = MaterialTheme.colorScheme.onBackground
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..subdivisionCount).forEach { subdivisionIndex ->
            val active = subdivisionIndex == currentSubdivisionIndex
            Canvas(
                modifier = Modifier
                    .size(slotSize),
            ) {
                drawCircle(
                    color = dotColor.copy(
                        alpha = if (active) 0.72f else 0.3f,
                    ),
                    radius = (if (active) activeSize else inactiveSize).toPx() / 2f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
        }
    }
}

private fun elapsedTimerMs(
    isRunning: Boolean,
    playbackStartedAtMs: Long,
): Long {
    return if (isRunning && playbackStartedAtMs > 0L) {
        (SystemClock.elapsedRealtime() - playbackStartedAtMs).coerceAtLeast(0L)
    } else {
        0L
    }
}

private fun formatElapsedTimer(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds / 60L) % 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0L) {
        "$hours:${minutes.twoDigitTimerPart()}:${seconds.twoDigitTimerPart()}"
    } else {
        "$minutes:${seconds.twoDigitTimerPart()}"
    }
}

private fun Long.twoDigitTimerPart(): String {
    return toString().padStart(2, '0')
}

@Composable
private fun rememberRhythmBeatVisualState(
    isRunning: Boolean,
    animationEnabled: Boolean,
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatClockStartedAtMs: Long,
    fallbackBeatIndex: Int,
    fallbackSubdivisionIndex: Int,
    fallbackBeatFlash: Boolean,
): BeatVisualState {
    var beatVisualState by remember {
        mutableStateOf(
            BeatVisualState(
                currentBeatIndex = fallbackBeatIndex,
                currentSubdivisionIndex = fallbackSubdivisionIndex,
                beatFlash = fallbackBeatFlash,
            ),
        )
    }
    val latestFallbackBeatIndex by rememberUpdatedState(fallbackBeatIndex)
    val latestFallbackSubdivisionIndex by rememberUpdatedState(fallbackSubdivisionIndex)
    val latestFallbackBeatFlash by rememberUpdatedState(fallbackBeatFlash)

    LaunchedEffect(
        isRunning,
        animationEnabled,
        bpm,
        beatsPerMeasure,
        subdivisionCount,
        beatClockStartedAtMs,
    ) {
        if (!isRunning || beatClockStartedAtMs <= 0L) {
            beatVisualState = BeatVisualState(
                currentBeatIndex = latestFallbackBeatIndex.coerceIn(1, beatsPerMeasure.coerceAtLeast(1)),
                currentSubdivisionIndex = latestFallbackSubdivisionIndex.coerceAtLeast(1),
                beatFlash = latestFallbackBeatFlash,
            )
            return@LaunchedEffect
        }

        if (!animationEnabled) {
            return@LaunchedEffect
        }

        while (true) {
            val nextBeatVisualState = currentRhythmBeatVisualState(
                bpm = bpm,
                beatsPerMeasure = beatsPerMeasure,
                subdivisionCount = subdivisionCount,
                beatClockStartedAtMs = beatClockStartedAtMs,
            )
            if (nextBeatVisualState.beatFlash && isRhythmVisualTraceWindow(
                    bpm = bpm,
                    beatClockStartedAtMs = beatClockStartedAtMs,
                )
            ) {
                BeatTimingTrace.markForBeat(
                    label = "rhythm visual loop",
                    beat = nextBeatVisualState.currentBeatIndex,
                )
            }
            beatVisualState = nextBeatVisualState
            delay(
                rhythmVisualDelayMs(
                    bpm = bpm,
                    subdivisionCount = subdivisionCount,
                    beatClockStartedAtMs = beatClockStartedAtMs,
                ),
            )
        }
    }

    return beatVisualState
}

private fun currentRhythmBeatVisualState(
    bpm: Int,
    beatsPerMeasure: Int,
    subdivisionCount: Int,
    beatClockStartedAtMs: Long,
): BeatVisualState {
    val normalizedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    val normalizedBeatsPerMeasure = beatsPerMeasure.coerceIn(2, 16)
    val normalizedSubdivisionCount = subdivisionCount.toSupportedSubdivisionCount()
    val intervalMs = (60_000L / normalizedBpm).coerceAtLeast(1L)
    val subdivisionIntervalMs = (intervalMs / normalizedSubdivisionCount).coerceAtLeast(1L)
    val elapsedMs = (SystemClock.elapsedRealtime() - beatClockStartedAtMs).coerceAtLeast(0L)
    val beatElapsedMs = elapsedMs % intervalMs

    return BeatVisualState(
        currentBeatIndex = (((elapsedMs / intervalMs) % normalizedBeatsPerMeasure) + 1L).toInt(),
        currentSubdivisionIndex = ((beatElapsedMs / subdivisionIntervalMs) + 1L)
            .toInt()
            .coerceIn(1, normalizedSubdivisionCount),
        beatFlash = beatElapsedMs < BEAT_FLASH_DURATION_MS,
    )
}

private fun isRhythmVisualTraceWindow(
    bpm: Int,
    beatClockStartedAtMs: Long,
): Boolean {
    val normalizedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    val intervalMs = (60_000L / normalizedBpm).coerceAtLeast(1L)
    val elapsedMs = (SystemClock.elapsedRealtime() - beatClockStartedAtMs).coerceAtLeast(0L)
    val beatElapsedMs = elapsedMs % intervalMs
    return beatElapsedMs < BEAT_FLASH_DURATION_MS
}

private fun rhythmVisualDelayMs(
    bpm: Int,
    subdivisionCount: Int,
    beatClockStartedAtMs: Long,
): Long {
    val normalizedBpm = bpm.coerceIn(MIN_BPM, MAX_BPM)
    val normalizedSubdivisionCount = subdivisionCount.toSupportedSubdivisionCount()
    val intervalMs = (60_000L / normalizedBpm).coerceAtLeast(1L)
    val subdivisionIntervalMs = (intervalMs / normalizedSubdivisionCount).coerceAtLeast(1L)
    val elapsedMs = (SystemClock.elapsedRealtime() - beatClockStartedAtMs).coerceAtLeast(0L)
    val beatElapsedMs = elapsedMs % intervalMs
    val subdivisionElapsedMs = beatElapsedMs % subdivisionIntervalMs
    val untilNextBeatMs = intervalMs - beatElapsedMs
    val untilNextSubdivisionMs = subdivisionIntervalMs - subdivisionElapsedMs
    val untilFlashEndsMs = if (beatElapsedMs < BEAT_FLASH_DURATION_MS) {
        BEAT_FLASH_DURATION_MS - beatElapsedMs
    } else {
        Long.MAX_VALUE
    }
    val untilNextVisualBoundaryMs = minOf(
        untilNextBeatMs,
        untilNextSubdivisionMs,
        untilFlashEndsMs,
    )

    return (untilNextVisualBoundaryMs - RHYTHM_VISUAL_WAKE_AHEAD_MS)
        .coerceAtLeast(1L)
        .coerceAtMost(RHYTHM_VISUAL_MAX_DELAY_MS)
}

@Composable
private fun FastTapTempoButton(
    text: String,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(onTap) {
                detectTapGestures(
                    onPress = {
                        onTap()
                        tryAwaitRelease()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TempoAdjustButton(
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String,
) {
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        modifier = Modifier.size(36.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        CenteredButtonLabel(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PlaylistClockPage(
    appText: AppText,
    playlist: SavedPlaylist,
    songIndex: Int,
    isRunning: Boolean,
    beatClockStartedAtMs: Long,
    playbackStartedAtMs: Long,
    clockImageResId: Int,
    clockColorArgb: Int,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onEditPlaylist: () -> Unit,
    onToggleRunning: () -> Unit,
) {
    val song = playlist.songs[songIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = playlist.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "${songIndex + 1}/${playlist.songs.size} ${song.name}",
                modifier = Modifier.width(132.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 46.dp)
                .width(184.dp)
                .height(26.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = song.musicalKey,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = "${song.bpm}",
                modifier = Modifier.align(Alignment.CenterEnd),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        BeatClockDial(
            bpm = song.bpm,
            isRunning = isRunning,
            beatClockStartedAtMs = beatClockStartedAtMs,
            clockImageResId = clockImageResId,
            clockColorArgb = clockColorArgb,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 10.dp)
                .size(124.dp),
        )

        PlaylistNavButton(
            isNext = false,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp),
            onClick = onPreviousSong,
        )

        PlaylistNavButton(
            isNext = true,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 0.dp),
            onClick = onNextSong,
        )

        Text(
            text = "${song.beatsPerMeasure}/4 x${song.subdivisionCount}",
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 44.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        GlassCommandButton(
            text = appText.edit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 40.dp)
                .width(42.dp)
                .height(24.dp),
            fontSize = 9.sp,
            onClick = onEditPlaylist,
        )

        RhythmElapsedTimer(
            isRunning = isRunning,
            playbackStartedAtMs = playbackStartedAtMs,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 43.dp),
        )

        GlassCommandButton(
            text = if (isRunning) appText.stopUpper else appText.startUpper,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .width(104.dp)
                .height(30.dp),
            fontSize = 15.sp,
            selected = isRunning,
            prominent = true,
            onClick = onToggleRunning,
        )
    }
}

@Composable
private fun PlaylistNavButton(
    isNext: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = primary.copy(alpha = 0.86f)
    val chevronColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .width(38.dp)
            .height(64.dp)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.44f),
                        primary.copy(alpha = 0.16f),
                        Color.Black.copy(alpha = 0.34f),
                    ),
                ),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isNext) ">" else "<",
            fontSize = 50.sp,
            fontWeight = FontWeight.Bold,
            color = chevronColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BeatClockDial(
    bpm: Int,
    isRunning: Boolean,
    beatClockStartedAtMs: Long,
    clockImageResId: Int,
    clockColorArgb: Int,
    modifier: Modifier = Modifier,
) {
    val elapsedMs = if (isRunning) {
        (SystemClock.elapsedRealtime() - beatClockStartedAtMs).coerceAtLeast(0L)
    } else {
        0L
    }
    val secondAngle = ((elapsedMs % 60_000L).toFloat() / 60_000f) * 360f
    val beatAngle = ((elapsedMs.toDouble() * bpm * 6.0 / 60_000.0) % 360.0).toFloat()
    val clockColor = colorFromChoice(clockColorArgb)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = clockImageResId),
            contentDescription = "BPM Munkz beat clock dial",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val secondEnd = clockHandEnd(center, size.minDimension * 0.43f, secondAngle)
            val beatEnd = clockHandEnd(center, size.minDimension * 0.36f, beatAngle)

            drawLine(
                color = Color.Black.copy(alpha = 0.65f),
                start = center,
                end = secondEnd,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = center,
                end = secondEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.7f),
                start = center,
                end = beatEnd,
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round,
            )
            if (isRainbowColor(clockColorArgb)) {
                drawLine(
                    brush = Brush.linearGradient(
                        colors = RainbowColors,
                        start = center,
                        end = beatEnd,
                    ),
                    start = center,
                    end = beatEnd,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else {
                drawLine(
                    color = clockColor,
                    start = center,
                    end = beatEnd,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                color = Color.Black,
                radius = 7.dp.toPx(),
                center = center,
            )
            if (isRainbowColor(clockColorArgb)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = RainbowColors,
                        center = center,
                    ),
                    radius = 4.dp.toPx(),
                    center = center,
                )
            } else {
                drawCircle(
                    color = clockColor,
                    radius = 4.dp.toPx(),
                    center = center,
                )
            }
        }
    }
}

@Composable
private fun PlaylistEditorPopup(
    appText: AppText,
    playlists: List<SavedPlaylist>,
    playlistIndex: Int,
    songIndex: Int,
    onPreviousPlaylist: () -> Unit,
    onNextPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onAddSong: () -> Unit,
    onDeleteSong: () -> Unit,
    onPlaylistNameEdit: (String) -> Unit,
    onSongNameEdit: (String) -> Unit,
    onSongBpmChange: (Int) -> Unit,
    onSongKeyChange: (Int) -> Unit,
    onSongNoteChange: (Int) -> Unit,
    onSongNoteEdit: (String) -> Unit,
    onEditRhythm: () -> Unit,
    onDone: () -> Unit,
) {
    DismissibleEditorPopup(onDone = onDone) {
        PlaylistEditorPage(
            appText = appText,
            playlists = playlists,
            playlistIndex = playlistIndex,
            songIndex = songIndex,
            onPreviousPlaylist = onPreviousPlaylist,
            onNextPlaylist = onNextPlaylist,
            onAddPlaylist = onAddPlaylist,
            onPreviousSong = onPreviousSong,
            onNextSong = onNextSong,
            onAddSong = onAddSong,
            onDeleteSong = onDeleteSong,
            onPlaylistNameEdit = onPlaylistNameEdit,
            onSongNameEdit = onSongNameEdit,
            onSongBpmChange = onSongBpmChange,
            onSongKeyChange = onSongKeyChange,
            onSongNoteChange = onSongNoteChange,
            onSongNoteEdit = onSongNoteEdit,
            onEditRhythm = onEditRhythm,
            onDone = onDone,
        )
    }
}

@Composable
private fun DismissibleEditorPopup(
    onDone: () -> Unit,
    content: @Composable () -> Unit,
) {
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val focusRequester = remember { FocusRequester() }

    BackHandler(onBack = onDone)

    LaunchedEffect(focusRequester) {
        runCatching {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp && event.nativeKeyEvent.isEditorDismissKey()) {
                    onDone()
                    true
                } else {
                    false
                }
            }
            .focusable()
            .pointerInput(onDone) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        horizontalDrag = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        horizontalDrag += dragAmount
                    },
                    onDragEnd = {
                        if (abs(horizontalDrag) > 60f) {
                            onDone()
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = {
                        horizontalDrag = 0f
                    },
                )
            }
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun android.view.KeyEvent.isEditorDismissKey(): Boolean {
    return when (keyCode) {
        android.view.KeyEvent.KEYCODE_BACK,
        android.view.KeyEvent.KEYCODE_STEM_PRIMARY,
        android.view.KeyEvent.KEYCODE_STEM_1,
        android.view.KeyEvent.KEYCODE_STEM_2,
        android.view.KeyEvent.KEYCODE_STEM_3 -> true
        else -> false
    }
}

private enum class PlaylistTextEditTarget {
    PlaylistName,
    SongName,
    SongNote,
}

@Composable
private fun PlaylistEditorPage(
    appText: AppText,
    playlists: List<SavedPlaylist>,
    playlistIndex: Int,
    songIndex: Int,
    onPreviousPlaylist: () -> Unit,
    onNextPlaylist: () -> Unit,
    onAddPlaylist: () -> Unit,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onAddSong: () -> Unit,
    onDeleteSong: () -> Unit,
    onPlaylistNameEdit: (String) -> Unit,
    onSongNameEdit: (String) -> Unit,
    onSongBpmChange: (Int) -> Unit,
    onSongKeyChange: (Int) -> Unit,
    onSongNoteChange: (Int) -> Unit,
    onSongNoteEdit: (String) -> Unit,
    onEditRhythm: () -> Unit,
    onDone: () -> Unit,
) {
    val playlist = playlists[playlistIndex]
    val song = playlist.songs[songIndex]
    var textEditTarget by remember { mutableStateOf<PlaylistTextEditTarget?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = appText.editPlaylist,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            ChoiceRow(
                label = "${playlistIndex + 1}/${playlists.size}",
                value = playlist.name,
                onPrevious = onPreviousPlaylist,
                onNext = onNextPlaylist,
                onValueClick = { textEditTarget = PlaylistTextEditTarget.PlaylistName },
            )

            Spacer(modifier = Modifier.height(9.dp))

            ChoiceRow(
                label = "${appText.song} ${songIndex + 1}/${playlist.songs.size}",
                value = song.name,
                onPrevious = onPreviousSong,
                onNext = onNextSong,
                onValueClick = { textEditTarget = PlaylistTextEditTarget.SongName },
            )

            Spacer(modifier = Modifier.height(9.dp))

            EditValueRow(
                label = "BPM",
                value = "${song.bpm}",
                onDecrease = { onSongBpmChange(-1) },
                onIncrease = { onSongBpmChange(1) },
            )

            EditValueRow(
                label = appText.key,
                value = song.musicalKey,
                onDecrease = { onSongKeyChange(-1) },
                onIncrease = { onSongKeyChange(1) },
            )

            EditValueRow(
                label = appText.note,
                value = song.note,
                onDecrease = { onSongNoteChange(-1) },
                onIncrease = { onSongNoteChange(1) },
                onValueClick = { textEditTarget = PlaylistTextEditTarget.SongNote },
            )

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallCommandButton(
                    text = appText.deleteSong,
                    modifier = Modifier
                        .width(54.dp)
                        .height(28.dp),
                    fontSize = 9.sp,
                    onClick = onDeleteSong,
                )

                SmallCommandButton(
                    text = appText.editRhythm,
                    modifier = Modifier
                        .width(92.dp)
                        .height(28.dp),
                    fontSize = 10.sp,
                    onClick = onEditRhythm,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            SmallCommandButton(
                text = appText.done,
                modifier = Modifier
                    .width(74.dp)
                    .height(30.dp),
                fontSize = 11.sp,
                onClick = onDone,
            )
        }

        CornerActionButton(
            text = appText.newList,
            modifier = Modifier.align(Alignment.TopStart),
            mirrored = true,
            onClick = onAddPlaylist,
        )

        CornerActionButton(
            text = appText.addSong,
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = onAddSong,
        )

        textEditTarget?.let { target ->
            val title = when (target) {
                PlaylistTextEditTarget.PlaylistName -> "Edit List"
                PlaylistTextEditTarget.SongName -> "Edit Song"
                PlaylistTextEditTarget.SongNote -> "Edit Note"
            }
            val editValue = when (target) {
                PlaylistTextEditTarget.PlaylistName -> playlist.name
                PlaylistTextEditTarget.SongName -> song.name
                PlaylistTextEditTarget.SongNote -> song.note
            }
            PlaylistTextEditPopup(
                title = title,
                value = editValue,
                doneText = appText.done,
                onCancel = { textEditTarget = null },
                onCommit = { nextValue ->
                    when (target) {
                        PlaylistTextEditTarget.PlaylistName -> onPlaylistNameEdit(nextValue)
                        PlaylistTextEditTarget.SongName -> onSongNameEdit(nextValue)
                        PlaylistTextEditTarget.SongNote -> onSongNoteEdit(nextValue)
                    }
                    textEditTarget = null
                },
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    value: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onValueClick: () -> Unit,
) {
    Text(
        text = label,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallCommandButton(
            text = "<",
            modifier = Modifier.size(28.dp),
            fontSize = 13.sp,
            onClick = onPrevious,
        )

        PillValueButton(
            value = value,
            modifier = Modifier.width(118.dp),
            fontSize = 12.sp,
            onClick = onValueClick,
        )

        SmallCommandButton(
            text = ">",
            modifier = Modifier.size(28.dp),
            fontSize = 13.sp,
            onClick = onNext,
        )
    }
}

@Composable
private fun CornerActionButton(
    text: String,
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    onClick: () -> Unit,
) {
    SmallCommandButton(
        text = text,
        modifier = modifier
            .padding(
                top = 20.dp,
                start = if (mirrored) 6.dp else 0.dp,
                end = if (mirrored) 0.dp else 6.dp,
            )
            .rotate(if (mirrored) -38f else 38f)
            .width(62.dp)
            .height(24.dp),
        fontSize = 8.sp,
        onClick = onClick,
    )
}

@Composable
private fun EditValueRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onValueClick: (() -> Unit)? = null,
) {
    Text(
        text = label,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )

    Spacer(modifier = Modifier.height(2.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallCommandButton(
            text = "-",
            modifier = Modifier.size(28.dp),
            fontSize = 14.sp,
            onClick = onDecrease,
        )

        if (onValueClick == null) {
            Text(
                text = value,
                modifier = Modifier.width(118.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        } else {
            PillValueButton(
                value = value,
                modifier = Modifier.width(118.dp),
                fontSize = 12.sp,
                onClick = onValueClick,
            )
        }

        SmallCommandButton(
            text = "+",
            modifier = Modifier.size(28.dp),
            fontSize = 14.sp,
            onClick = onIncrease,
        )
    }

    Spacer(modifier = Modifier.height(5.dp))
}

@Composable
private fun PillValueButton(
    value: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    onClick: () -> Unit,
) {
    SmallCommandButton(
        text = value.ifBlank { "--" },
        modifier = modifier.height(28.dp),
        fontSize = fontSize,
        onClick = onClick,
    )
}

@Composable
private fun PlaylistTextEditPopup(
    title: String,
    value: String,
    doneText: String,
    onCancel: () -> Unit,
    onCommit: (String) -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(title, value) { mutableStateOf(value) }
    var editText by remember { mutableStateOf<EditText?>(null) }
    val shape = RoundedCornerShape(50)
    fun commitDraft() {
        onCommit((editText?.text?.toString() ?: draft).trim())
    }

    BackHandler(onBack = onCancel)

    LaunchedEffect(editText) {
        val input = editText ?: return@LaunchedEffect
        delay(120)
        input.requestFocus()
        input.setSelection(input.text?.length ?: 0)
        val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
        inputMethodManager?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(170.dp)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primary, shape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    factory = { viewContext ->
                        EditText(viewContext).apply {
                            setSingleLine(true)
                            setText(value)
                            setSelectAllOnFocus(false)
                            setSelection(text?.length ?: 0)
                            setTextColor(android.graphics.Color.BLACK)
                            setHintTextColor(android.graphics.Color.argb(160, 0, 0, 0))
                            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f)
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            gravity = Gravity.CENTER
                            background = null
                            includeFontPadding = false
                            inputType = InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            imeOptions = EditorInfo.IME_ACTION_DONE
                            setOnEditorActionListener { _, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    commitDraft()
                                    true
                                } else {
                                    false
                                }
                            }
                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(
                                    text: CharSequence?,
                                    start: Int,
                                    count: Int,
                                    after: Int,
                                ) = Unit

                                override fun onTextChanged(
                                    text: CharSequence?,
                                    start: Int,
                                    before: Int,
                                    count: Int,
                                ) {
                                    val next = text?.toString().orEmpty()
                                    if (next.length > 48) {
                                        val clipped = next.take(48)
                                        setText(clipped)
                                        setSelection(clipped.length)
                                    } else {
                                        draft = next
                                    }
                                }

                                override fun afterTextChanged(text: Editable?) = Unit
                            })
                            editText = this
                        }
                    },
                    update = { input ->
                        if (input.text?.toString() != draft && !input.isFocused) {
                            input.setText(draft)
                            input.setSelection(input.text?.length ?: 0)
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SmallCommandButton(
                text = doneText,
                modifier = Modifier
                    .width(74.dp)
                    .height(30.dp),
                fontSize = 11.sp,
                onClick = { commitDraft() },
            )
        }
    }
}

@Composable
private fun ColorPickerRow(
    selectedColorArgb: Int,
    onColorChoice: (Int) -> Unit,
    colorOptions: List<Int> = PulseColorOptions,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        colorOptions.chunked(4).forEach { colorRow ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                colorRow.forEach { colorArgb ->
                    ColorSwatchButton(
                        colorArgb = colorArgb,
                        selected = selectedColorArgb == colorArgb,
                        onClick = { onColorChoice(colorArgb) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f)
    }
    val baseModifier = Modifier
        .width(36.dp)
        .height(22.dp)
        .clip(shape)
        .then(
            if (isRainbowColor(colorArgb)) {
                Modifier.background(Brush.horizontalGradient(RainbowColors), shape)
            } else {
                Modifier.background(colorFromChoice(colorArgb), shape)
            },
        )
        .border(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor,
            shape = shape,
        )
        .clickable(onClick = onClick)

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(
                text = "*",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = selectedSwatchMarkColor(colorArgb),
            )
        }
    }
}

@Composable
private fun ClockImagePicker(
    selectedIndex: Int,
    appLanguage: AppLanguage,
    onClockImageChoice: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ClockImageChoices.chunked(4).forEachIndexed { rowIndex, choices ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                choices.forEachIndexed { columnIndex, choice ->
                    val choiceIndex = rowIndex * 4 + columnIndex
                    ClockImageChoiceButton(
                        text = choice.labelFor(appLanguage),
                        selected = selectedIndex == choiceIndex,
                        modifier = Modifier
                            .width(42.dp)
                            .height(26.dp),
                        onClick = { onClockImageChoice(choiceIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BigRingModePicker(
    selectedMode: BigRingFlashMode,
    appLanguage: AppLanguage,
    onModeChoice: (BigRingFlashMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BigRingModeChoices.forEach { choice ->
            ClockImageChoiceButton(
                text = choice.labelFor(appLanguage),
                selected = selectedMode == choice.mode,
                modifier = Modifier
                    .width(42.dp)
                    .height(26.dp),
                onClick = { onModeChoice(choice.mode) },
            )
        }
    }
}

@Composable
private fun ClockImageChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 9.sp,
) {
    val shape = RoundedCornerShape(50)
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    } else {
        Color.Black.copy(alpha = 0.34f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LanguagePicker(
    selectedLanguage: AppLanguage,
    onLanguageChoice: (AppLanguage) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLanguages.forEach { language ->
            ClockImageChoiceButton(
                text = when (language) {
                    AppLanguage.English -> "EN"
                    AppLanguage.Spanish -> "ES"
                },
                selected = selectedLanguage == language,
                modifier = Modifier
                    .width(42.dp)
                    .height(26.dp),
                onClick = { onLanguageChoice(language) },
            )
        }
    }
}

@Composable
private fun SettingsPage(
    appText: AppText,
    hapticsEnabled: Boolean,
    beepEnabled: Boolean,
    accentIntensityMode: AccentIntensityMode,
    accentIntensityRanges: List<AccentIntensityRange>,
    a4ReferenceHz: Int,
    keepScreenMode: KeepScreenMode,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    clockColorArgb: Int,
    clockImageIndex: Int,
    ringColorArgb: Int,
    bigRingFlashMode: BigRingFlashMode,
    appLanguage: AppLanguage,
    appCpuUsagePercent: Float?,
    onHapticsToggle: () -> Unit,
    onBeepToggle: () -> Unit,
    onAccentIntensityModeChoice: (AccentIntensityMode) -> Unit,
    onAccentIntensityRangesChange: (List<AccentIntensityRange>) -> Unit,
    onA4ReferenceHzChange: (Int) -> Unit,
    onKeepScreenModeChoice: (KeepScreenMode) -> Unit,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onClockColorChoice: (Int) -> Unit,
    onClockImageChoice: (Int) -> Unit,
    onRingColorChoice: (Int) -> Unit,
    onBigRingModeChoice: (BigRingFlashMode) -> Unit,
    onLanguageChoice: (AppLanguage) -> Unit,
) {
    var intensityPickerOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = appText.settings,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SettingsToggleColumn(
                    label = appText.beep,
                    enabled = beepEnabled,
                    onText = appText.on,
                    offText = appText.off,
                    onClick = onBeepToggle,
                )

                SettingsToggleColumn(
                    label = appText.haptics,
                    enabled = hapticsEnabled,
                    onText = appText.on,
                    offText = appText.off,
                    onClick = onHapticsToggle,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText.intensity,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(3.dp))

            AccentIntensityPicker(
                accentIntensityRanges = accentIntensityRanges,
                appLanguage = appLanguage,
                onClick = {
                    intensityPickerOpen = true
                },
            )

            Spacer(modifier = Modifier.height(10.dp))

            A4ReferenceControl(
                label = appText.a4Reference,
                referenceHz = a4ReferenceHz,
                onReferenceHzChange = onA4ReferenceHzChange,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText.diagnostics,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            CpuUsageReadout(
                label = appText.appCpu,
                cpuUsagePercent = appCpuUsagePercent,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText.keepScreenOn,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(3.dp))

            KeepScreenModeButtons(
                selectedMode = keepScreenMode,
                appText = appText,
                onModeChoice = onKeepScreenModeChoice,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText.theme,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = appText.mainColor,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorPickerRow(
                selectedColorArgb = mainColorArgb,
                onColorChoice = onMainColorChoice,
                colorOptions = ThemeMainColorOptions,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appText.backgroundColor,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorPickerRow(
                selectedColorArgb = backgroundColorArgb,
                onColorChoice = onBackgroundColorChoice,
                colorOptions = ThemeBackgroundColorOptions,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appText.bigRing,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorPickerRow(
                selectedColorArgb = ringColorArgb,
                onColorChoice = onRingColorChoice,
            )

            Spacer(modifier = Modifier.height(5.dp))

            BigRingModePicker(
                selectedMode = bigRingFlashMode,
                appLanguage = appLanguage,
                onModeChoice = onBigRingModeChoice,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText.clock,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = appText.handColor,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorPickerRow(
                selectedColorArgb = clockColorArgb,
                onColorChoice = onClockColorChoice,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appText.clockImage,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ClockImagePicker(
                selectedIndex = clockImageIndex,
                appLanguage = appLanguage,
                onClockImageChoice = onClockImageChoice,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = appText.language,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            LanguagePicker(
                selectedLanguage = appLanguage,
                onLanguageChoice = onLanguageChoice,
            )
        }

        if (intensityPickerOpen) {
            AccentIntensityChoicePopup(
                title = appText.intensity,
                selectedMode = accentIntensityMode,
                accentIntensityRanges = accentIntensityRanges,
                appLanguage = appLanguage,
                dismissText = appText.done,
                onModeChoice = { mode ->
                    onAccentIntensityModeChoice(mode)
                },
                onValueChange = { mode, value ->
                    onAccentIntensityRangesChange(
                        accentIntensityRanges.withRangeFor(
                            mode,
                            accentIntensityRanges.rangeFor(mode).copy(valuePercent = value),
                        ),
                    )
                },
                onDismiss = {
                    intensityPickerOpen = false
                },
            )
        }
    }
}

@Composable
private fun CpuUsageReadout(
    label: String,
    cpuUsagePercent: Float?,
) {
    Row(
        modifier = Modifier
            .width(118.dp)
            .height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = cpuUsagePercent.formatCpuUsagePercent(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun A4ReferenceControl(
    label: String,
    referenceHz: Int,
    onReferenceHzChange: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallCommandButton(
                text = "-",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                onClick = {
                    onReferenceHzChange((referenceHz - 1).coerceAtLeast(MIN_A4_REFERENCE_HZ))
                },
            )

            Text(
                text = "A $referenceHz Hz",
                modifier = Modifier.width(72.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            SmallCommandButton(
                text = "+",
                modifier = Modifier
                    .width(24.dp)
                    .height(22.dp),
                fontSize = 11.sp,
                onClick = {
                    onReferenceHzChange((referenceHz + 1).coerceAtMost(MAX_A4_REFERENCE_HZ))
                },
            )
        }
    }
}

@Composable
private fun AccentIntensityPicker(
    accentIntensityRanges: List<AccentIntensityRange>,
    appLanguage: AppLanguage,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(166.dp)
            .height(42.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccentIntensityChoices.forEach { choice ->
                val value = accentIntensityRanges.rangeFor(choice.mode).valuePercent
                Column(
                    modifier = Modifier.width(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = choice.labelFor(appLanguage),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        text = value.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentIntensityChoicePopup(
    title: String,
    selectedMode: AccentIntensityMode,
    accentIntensityRanges: List<AccentIntensityRange>,
    appLanguage: AppLanguage,
    dismissText: String,
    onModeChoice: (AccentIntensityMode) -> Unit,
    onValueChange: (AccentIntensityMode, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        ArchedDoneButton(
            text = dismissText,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 0.dp, end = 0.dp),
            onClick = onDismiss,
        )

        val selectedRange = accentIntensityRanges.rangeFor(selectedMode)

        Column(
            modifier = Modifier
                .width(160.dp)
                .padding(top = 13.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(7.dp))

            AccentIntensityChoices.forEach { choice ->
                AccentIntensityTypePill(
                    choice = choice,
                    range = accentIntensityRanges.rangeFor(choice.mode),
                    selected = selectedMode == choice.mode,
                    appLanguage = appLanguage,
                    onClick = { onModeChoice(choice.mode) },
                )

                Spacer(modifier = Modifier.height(3.dp))
            }

            Spacer(modifier = Modifier.height(1.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallCommandButton(
                    text = "-",
                    modifier = Modifier
                        .width(34.dp)
                        .height(26.dp),
                    fontSize = 14.sp,
                    onClick = {
                        onValueChange(
                            selectedMode,
                            (selectedRange.valuePercent - 1).coerceAtLeast(selectedRange.minPercent),
                        )
                    },
                )

                SmallCommandButton(
                    text = "+",
                    modifier = Modifier
                        .width(34.dp)
                        .height(26.dp),
                    fontSize = 14.sp,
                    onClick = {
                        onValueChange(
                            selectedMode,
                            (selectedRange.valuePercent + 1).coerceAtMost(selectedRange.maxPercent),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AccentIntensityTypePill(
    choice: AccentIntensityChoice,
    range: AccentIntensityRange,
    selected: Boolean,
    appLanguage: AppLanguage,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
    } else {
        Color.Black.copy(alpha = 0.34f)
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.24f)
    }

    Column(
        modifier = Modifier
            .width(142.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .width(138.dp)
                .height(26.dp)
                .clip(shape)
                .background(backgroundColor, shape)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = shape,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = choice.labelFor(appLanguage),
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.84f)
                },
                maxLines = 1,
            )

            Text(
                text = "${range.valuePercent}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )

            Text(
                text = "${range.maxPercent}-${range.minPercent}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun KeepScreenModeButtons(
    selectedMode: KeepScreenMode,
    appText: AppText,
    onModeChoice: (KeepScreenMode) -> Unit,
) {
    val choices = listOf(
        KeepScreenMode.AppOpen to appText.keepScreenAppOpen,
        KeepScreenMode.Playing to appText.keepScreenPlaying,
        KeepScreenMode.WatchTimeout to appText.keepScreenWatchTimeout,
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        choices.forEach { (mode, label) ->
            ClockImageChoiceButton(
                text = label,
                selected = selectedMode == mode,
                modifier = Modifier
                    .width(42.dp)
                    .height(26.dp),
                onClick = { onModeChoice(mode) },
            )
        }
    }
}

@Composable
private fun SettingsToggleColumn(
    label: String,
    enabled: Boolean,
    onText: String,
    offText: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(3.dp))

        SettingButton(
            text = if (enabled) onText else offText,
            selected = enabled,
            modifier = Modifier
                .width(54.dp)
                .height(28.dp),
            onClick = onClick,
        )
    }
}

@Composable
private fun AudioToolButtons(
    appText: AppText,
    onOpenTuner: () -> Unit,
    onOpenSpectrum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassCommandButton(
            text = appText.tuner,
            modifier = Modifier
                .width(50.dp)
                .height(22.dp),
            fontSize = 9.sp,
            selected = false,
            prominent = false,
            onClick = onOpenTuner,
        )

        GlassCommandButton(
            text = "Spect",
            modifier = Modifier
                .width(50.dp)
                .height(22.dp),
            fontSize = 8.sp,
            selected = false,
            prominent = false,
            onClick = onOpenSpectrum,
        )
    }
}

@Composable
private fun AnalyzerOverlay(
    closeText: String,
    onClose: () -> Unit,
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

        ArchedDoneButton(
            text = closeText,
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
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
private fun TunerPage(
    appText: AppText,
    appLanguage: AppLanguage,
    audioAnalysisState: AudioAnalysisState,
    selectedProfile: TunerListenProfile,
    micPermissionGranted: Boolean,
    onProfileChoice: (TunerListenProfile) -> Unit,
    onSaveKey: (String) -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    val guessedKey = audioAnalysisState.guessedKey
    val likelyChords = audioAnalysisState.likelyChords

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!micPermissionGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = appText.tuner,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            }
            return@Box
        }

        AudioTempoReadout(
            detectedBpm = audioAnalysisState.detectedTempoBpm,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 34.dp)
                .width(52.dp),
            fontSize = 8.sp,
            numberFontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f),
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TunerListenProfile.entries.forEach { profile ->
                ClockImageChoiceButton(
                    text = profile.labelFor(appLanguage),
                    selected = selectedProfile == profile,
                    modifier = Modifier
                        .width(36.dp)
                        .height(23.dp),
                    fontSize = 8.sp,
                    onClick = { onProfileChoice(profile) },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(126.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = appText.tuner,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = audioAnalysisState.noteName,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = audioAnalysisState.frequencyHz?.let { "${it.roundToInt()} Hz" } ?: "-- Hz",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(2.dp))
            TunerNeedle(cents = audioAnalysisState.cents)
            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${audioAnalysisState.cents.coerceIn(-99, 99)} cents",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )

            val recentNotesText = audioAnalysisState.recentNotes.takeLast(8).joinToString(" ")
            Text(
                text = recentNotesText.ifBlank { "--" },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 3.dp)
                .width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${appText.keyGuess}:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Text(
                text = guessedKey ?: "--",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(2.dp))

            likelyChords.take(3).forEach { chord ->
                Text(
                    text = chord,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        if (guessedKey != null) {
            SmallCommandButton(
                text = appText.saveKey,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .width(62.dp)
                    .height(22.dp),
                fontSize = 8.sp,
                onClick = { onSaveKey(guessedKey) },
            )
        }
    }
}

@Composable
private fun SpectrumAnalyzerPage(
    appText: AppText,
    audioAnalysisState: AudioAnalysisState,
    a4ReferenceHz: Int,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
) {
    val peakReading = remember(audioAnalysisState.spectrum) {
        audioAnalysisState.spectrum.peakSpectrumReading()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = appText.spectrum,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(2.dp))

            AudioTempoReadout(
                detectedBpm = audioAnalysisState.detectedTempoBpm,
                modifier = Modifier.width(60.dp),
            )

            Text(
                text = audioAnalysisState.guessedKey?.let { "Key of $it" } ?: "Key of --",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            if (!micPermissionGranted) {
                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            } else {
                SpectrumAnalyzerGraph(
                    spectrum = audioAnalysisState.spectrum,
                    peakReading = peakReading,
                    modifier = Modifier
                        .width(176.dp)
                        .height(110.dp),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = peakReading?.let { peak ->
                        val noteName = peak.frequencyHz.toNoteReading(a4ReferenceHz).first
                        "${peak.frequencyHz.roundToInt()} Hz  $noteName  ${peak.bandLabel}"
                    } ?: "-- Hz",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AudioTempoReadout(
    detectedBpm: Int?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp,
    numberFontSize: TextUnit = fontSize,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = detectedBpm?.let { bpm ->
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = numberFontSize)) {
                    append(bpm.toString())
                }
                append(" BPM")
            }
        } ?: buildAnnotatedString { append("-- BPM") },
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun MicPermissionButton(
    appText: AppText,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(104.dp)
            .height(34.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        CenteredButtonLabel(
            text = appText.micAccess,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TunerNeedle(cents: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = Modifier
            .width(132.dp)
            .height(38.dp),
    ) {
        val centerY = size.height * 0.58f
        val startX = 8.dp.toPx()
        val endX = size.width - 8.dp.toPx()
        val centerX = size.width / 2f
        val leftOuterEndX = startX + (endX - startX) * 0.18f
        val rightOuterStartX = endX - (endX - startX) * 0.18f
        val clampedCents = cents.coerceIn(-100, 100)
        val needleX = when {
            clampedCents < -50 -> {
                val progress = ((clampedCents + 100) / 50f).coerceIn(0f, 1f)
                startX + (leftOuterEndX - startX) * progress
            }
            clampedCents > 50 -> {
                val progress = ((clampedCents - 50) / 50f).coerceIn(0f, 1f)
                rightOuterStartX + (endX - rightOuterStartX) * progress
            }
            else -> {
                val progress = ((clampedCents + 50) / 100f).coerceIn(0f, 1f)
                leftOuterEndX + (rightOuterStartX - leftOuterEndX) * progress
            }
        }
        val tuned = abs(cents) <= 5
        val dotRadius = when {
            abs(cents) <= 2 -> 8.dp.toPx()
            tuned -> 7.dp.toPx()
            else -> 5.dp.toPx()
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.copy(alpha = 0.54f).toArgb()
            textSize = 6.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(startX, centerY),
            end = Offset(leftOuterEndX, centerY),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.36f),
            start = Offset(leftOuterEndX, centerY),
            end = Offset(rightOuterStartX, centerY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(rightOuterStartX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = primaryColor,
            start = Offset(centerX, centerY - 11.dp.toPx()),
            end = Offset(centerX, centerY + 11.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        listOf(
            startX to "-100",
            leftOuterEndX to "-50",
            centerX to "0",
            rightOuterStartX to "+50",
            endX to "+100",
        ).forEach { (x, label) ->
            drawLine(
                color = Color.White.copy(alpha = if (label == "0") 0.44f else 0.26f),
                start = Offset(x, centerY - 4.dp.toPx()),
                end = Offset(x, centerY + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                centerY + 14.dp.toPx(),
                labelPaint,
            )
        }
        if (tuned) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.22f),
                radius = dotRadius + 4.dp.toPx(),
                center = Offset(needleX, centerY),
            )
        }
        drawCircle(
            color = if (tuned) primaryColor else secondaryColor,
            radius = dotRadius,
            center = Offset(needleX, centerY),
        )
    }
}

@Composable
private fun SpectrumAnalyzerGraph(
    spectrum: List<Float>,
    peakReading: SpectrumPeak?,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val axisColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
    val spectrumBands = remember { spectrumBands() }
    Canvas(modifier = modifier) {
        val chartLeft = 24.dp.toPx()
        val chartTop = 6.dp.toPx()
        val chartRight = size.width - 2.dp.toPx()
        val chartBottom = size.height - 18.dp.toPx()
        val chartWidth = (chartRight - chartLeft).coerceAtLeast(1f)
        val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
        val points = spectrum.ifEmpty { List(SPECTRUM_BAR_COUNT) { 0f } }
        val minFrequency = 30f
        val maxFrequency = 10_000f
        val logMin = log2(minFrequency)
        val logMax = log2(maxFrequency)

        fun frequencyToX(frequency: Float): Float {
            val normalized = ((log2(frequency.coerceIn(minFrequency, maxFrequency)) - logMin) / (logMax - logMin))
                .coerceIn(0f, 1f)
            return chartLeft + (chartWidth * normalized)
        }

        fun levelToY(level: Float): Float {
            val shapedLevel = sqrt(level.coerceIn(0f, 1f))
            return chartBottom - (chartHeight * shapedLevel)
        }

        drawRect(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(chartLeft, chartTop),
            size = androidx.compose.ui.geometry.Size(chartWidth, chartHeight),
        )

        spectrumBands.forEach { band ->
            val left = frequencyToX(band.startHz)
            val right = frequencyToX(band.endHz)
            drawRect(
                color = band.color.copy(alpha = 0.16f),
                topLeft = Offset(left, chartTop),
                size = androidx.compose.ui.geometry.Size((right - left).coerceAtLeast(1f), chartHeight),
            )
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor.toArgb()
            textSize = 7.sp.toPx()
            textAlign = Paint.Align.RIGHT
        }
        val axisTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor.copy(alpha = 0.72f).toArgb()
            textSize = 6.sp.toPx()
            textAlign = Paint.Align.RIGHT
        }
        val bottomTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = axisColor.toArgb()
            textSize = 7.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        drawContext.canvas.nativeCanvas.drawText(
            "Energy",
            chartLeft - 5.dp.toPx(),
            chartTop - 6.dp.toPx(),
            axisTitlePaint,
        )

        listOf(
            1f to "Hi",
            0.56f to "Med",
            0.2f to "Low",
            0f to "0",
        ).forEach { (level, label) ->
            val y = levelToY(level)
            drawLine(
                color = axisColor.copy(alpha = if (level == 1f) 0.24f else 0.16f),
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx(),
            )
            drawContext.canvas.nativeCanvas.drawText(
                label,
                chartLeft - 5.dp.toPx(),
                y + 2.5.dp.toPx(),
                textPaint,
            )
        }

        val frequencyTicks = listOf(30f, 50f, 100f, 200f, 500f, 1_000f, 2_000f, 5_000f, 10_000f)
        frequencyTicks.forEach { frequency ->
            val x = frequencyToX(frequency)
            drawLine(
                color = secondaryColor.copy(alpha = 0.12f),
                start = Offset(x, chartTop),
                end = Offset(x, chartBottom),
                strokeWidth = 1.dp.toPx(),
            )
            val label = when {
                frequency >= 1_000f -> "${(frequency / 1_000f).roundToInt()}k"
                else -> frequency.roundToInt().toString()
            }
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                chartBottom + 10.dp.toPx(),
                bottomTextPaint,
            )
        }

        val barCount = points.size.coerceAtLeast(1)
        points.forEachIndexed { index, level ->
            val startProgress = index.toFloat() / barCount
            val endProgress = (index + 1).toFloat() / barCount
            val startFrequency = minFrequency * (maxFrequency / minFrequency).pow(startProgress)
            val endFrequency = minFrequency * (maxFrequency / minFrequency).pow(endProgress)
            val left = frequencyToX(startFrequency)
            val right = frequencyToX(endFrequency)
            val top = levelToY(level)
            val barWidth = (right - left - 1.dp.toPx()).coerceAtLeast(1f)
            val bandColor = spectrumBandForFrequency(startFrequency, spectrumBands).color
            drawRoundRect(
                color = bandColor.copy(alpha = 0.24f + level.coerceIn(0f, 1f) * 0.5f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, chartBottom - top),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
            )
        }

        val fillPath = Path().apply {
            moveTo(chartLeft, chartBottom)
            points.forEachIndexed { index, level ->
                val frequency = if (points.size == 1) {
                    30f
                } else {
                    minFrequency * (maxFrequency / minFrequency).pow(index.toFloat() / points.lastIndex)
                }
                lineTo(frequencyToX(frequency), levelToY(level))
            }
            lineTo(chartRight, chartBottom)
            close()
        }

        drawPath(
            path = fillPath,
            color = primaryColor.copy(alpha = 0.12f),
        )

        var previousPoint: Offset? = null
        points.forEachIndexed { index, level ->
            val frequency = if (points.size == 1) {
                30f
            } else {
                    minFrequency * (maxFrequency / minFrequency).pow(index.toFloat() / points.lastIndex)
                }
            val point = Offset(frequencyToX(frequency), levelToY(level))
            previousPoint?.let { previous ->
                drawLine(
                    color = primaryColor.copy(alpha = 0.48f),
                    start = previous,
                    end = point,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            previousPoint = point
        }

        peakReading?.let { peak ->
            val peakX = frequencyToX(peak.frequencyHz)
            val peakY = levelToY(peak.level)
            drawLine(
                color = secondaryColor.copy(alpha = 0.82f),
                start = Offset(peakX, chartTop),
                end = Offset(peakX, chartBottom),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(
                color = secondaryColor,
                radius = 3.2.dp.toPx(),
                center = Offset(peakX, peakY),
            )
        }

        drawLine(
            color = axisColor.copy(alpha = 0.34f),
            start = Offset(chartLeft, chartTop),
            end = Offset(chartLeft, chartBottom),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = axisColor.copy(alpha = 0.34f),
            start = Offset(chartLeft, chartBottom),
            end = Offset(chartRight, chartBottom),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

@Composable
private fun SettingButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    fontSize: TextUnit = 11.sp,
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
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SmallCommandButton(
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
private fun GlassCommandButton(
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
private fun BigPulseRingOverlay(
    beatFlash: Boolean,
    flashingAccentType: BeatAccentType,
    bigRingFlashMode: BigRingFlashMode,
    colorArgb: Int,
    modifier: Modifier = Modifier,
) {
    val shouldFlash = bigRingFlashMode.shouldFlashRing(
        beatFlash = beatFlash,
        flashingAccentType = flashingAccentType,
    )

    if (shouldFlash) {
        BeatTimingTrace.mark("big ring draw")
    }

    BigPulseRing(
        colorArgb = colorArgb,
        alpha = if (shouldFlash) {
            0.95f
        } else {
            0f
        },
        modifier = modifier,
    )
}

@Composable
private fun BigPulseRing(
    colorArgb: Int,
    modifier: Modifier = Modifier,
    alpha: Float = 0.95f,
) {
    Canvas(modifier = modifier) {
        if (alpha <= 0f) return@Canvas

        val strokeWidth = 6.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        if (isRainbowColor(colorArgb)) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = RainbowColors.map { color -> color.copy(alpha = alpha.coerceIn(0f, 1f)) },
                    center = center,
                ),
                radius = (size.minDimension - strokeWidth) / 2f,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        } else {
            drawCircle(
                color = colorFromChoice(colorArgb).copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = (size.minDimension - strokeWidth) / 2f,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

@Composable
private fun PulsePagerIndicator(
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PULSE_PAGE_COUNT) { page ->
            Box(
                modifier = Modifier
                    .size(if (page == currentPage) 5.dp else 3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (page == currentPage) 0.8f else 0.34f,
                        ),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun BeatPulsePage(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun BeatTempoReadout(
    bpm: Int,
    beatFlash: Boolean,
    isAccentFlash: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulseSize = when {
        beatFlash && isAccentFlash -> 28.dp
        beatFlash -> 22.dp
        else -> 11.dp
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(pulseSize)
                    .background(
                        color = if (beatFlash) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        shape = CircleShape,
                    ),
            )
        }

        Text(
            text = "$bpm",
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "BPM",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun StartStopButton(
    appText: AppText,
    isRunning: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(86.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        onClick()
                        tryAwaitRelease()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isRunning) appText.stop else appText.start,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun CenteredButtonLabel(
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

private fun clockHandEnd(
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

private fun isRainbowColor(colorArgb: Int): Boolean {
    return colorArgb == RAINBOW_COLOR
}

private fun colorFromChoice(colorArgb: Int): Color {
    return if (isRainbowColor(colorArgb)) {
        Color(NEON_GREEN_COLOR)
    } else {
        Color(colorArgb)
    }
}

private fun selectedSwatchMarkColor(colorArgb: Int): Color {
    return if (isRainbowColor(colorArgb) || relativeLuminance(colorArgb) > 0.46) {
        Color.Black
    } else {
        Color.White
    }
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

private fun hasSafeThemeContrast(
    mainColorArgb: Int,
    backgroundColorArgb: Int,
): Boolean {
    return colorDistanceSquared(mainColorArgb, backgroundColorArgb) >= 0.16
}

private fun readableTextColorFor(colorArgb: Int): Color {
    return if (relativeLuminance(colorArgb) > 0.46) {
        Color.Black
    } else {
        Color.White
    }
}

private fun colorDistanceSquared(
    firstColorArgb: Int,
    secondColorArgb: Int,
): Double {
    val redDifference = colorChannel(firstColorArgb, 16) - colorChannel(secondColorArgb, 16)
    val greenDifference = colorChannel(firstColorArgb, 8) - colorChannel(secondColorArgb, 8)
    val blueDifference = colorChannel(firstColorArgb, 0) - colorChannel(secondColorArgb, 0)
    return redDifference * redDifference +
        greenDifference * greenDifference +
        blueDifference * blueDifference
}

private fun relativeLuminance(colorArgb: Int): Double {
    val red = linearColorChannel(colorArgb, 16)
    val green = linearColorChannel(colorArgb, 8)
    val blue = linearColorChannel(colorArgb, 0)
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

private fun linearColorChannel(
    colorArgb: Int,
    shift: Int,
): Double {
    val channel = colorChannel(colorArgb, shift)
    return if (channel <= 0.03928) {
        channel / 12.92
    } else {
        ((channel + 0.055) / 1.055).pow(2.4)
    }
}

private fun colorChannel(
    colorArgb: Int,
    shift: Int,
): Double {
    return ((colorArgb shr shift) and 0xFF) / 255.0
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

private fun defaultSavedPlaylists(): List<SavedPlaylist> {
    return listOf(
        SavedPlaylist(
            name = "Set 1",
            songs = listOf(
                PlaylistSong(
                    name = "Intro",
                    bpm = 64,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "C",
                    note = "Count in",
                ),
                PlaylistSong(
                    name = "Verse",
                    bpm = 92,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "G",
                    note = "Keep pocket",
                ),
                PlaylistSong(
                    name = "Chorus",
                    bpm = 116,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "D",
                    note = "Big accents",
                ),
            ),
        ),
        SavedPlaylist(
            name = "Practice",
            songs = listOf(
                PlaylistSong(
                    name = "Warmup",
                    bpm = 72,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "Am",
                    note = "Hold tempo",
                ),
                PlaylistSong(
                    name = "Break",
                    bpm = 108,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "Em",
                    note = "Loop twice",
                ),
            ),
        ),
    )
}

private fun defaultSavedPlaylist(number: Int): SavedPlaylist {
    return SavedPlaylist(
        name = "Set $number",
        songs = listOf(defaultPlaylistSong(1)),
    )
}

private fun defaultPlaylistSong(number: Int): PlaylistSong {
    val name = SongNameOptions[(number - 1).wrap(SongNameOptions.size)]
    return PlaylistSong(
        name = name,
        bpm = 64,
        beatsPerMeasure = 4,
        accentBeat = 1,
        subdivisionCount = 1,
        beatAccentTypes = defaultBeatAccentTypes(4, 1),
        accentIntensityMode = AccentIntensityMode.Big,
        musicalKey = "C",
        note = "Count in",
    )
}

private fun List<SavedPlaylist>.updatePlaylist(
    playlistIndex: Int,
    update: (SavedPlaylist) -> SavedPlaylist,
): List<SavedPlaylist> {
    return mapIndexed { index, playlist ->
        if (index == playlistIndex) update(playlist) else playlist
    }
}

private fun List<SavedPlaylist>.updateSong(
    playlistIndex: Int,
    songIndex: Int,
    update: (PlaylistSong) -> PlaylistSong,
): List<SavedPlaylist> {
    return updatePlaylist(playlistIndex) { playlist ->
        playlist.copy(
            songs = playlist.songs.mapIndexed { index, song ->
                if (index == songIndex) update(song) else song
            },
        )
    }
}

private fun cycleOption(
    options: List<String>,
    current: String,
    step: Int,
): String {
    val currentIndex = options.indexOf(current).takeIf { it >= 0 } ?: 0
    return options[(currentIndex + step).wrap(options.size)]
}

private fun Int.wrap(size: Int): Int {
    return ((this % size) + size) % size
}

private fun Context.loadSavedPlaylists(): List<SavedPlaylist> {
    val rawLibrary = getSharedPreferences(PLAYLIST_PREFS, Context.MODE_PRIVATE)
        .getString(PLAYLIST_LIBRARY_KEY, null)
        ?: return defaultSavedPlaylists()

    return runCatching {
        val array = JSONArray(rawLibrary)
        buildList {
            for (playlistIndex in 0 until array.length()) {
                val playlistObject = array.optJSONObject(playlistIndex) ?: continue
                val songArray = playlistObject.optJSONArray("songs") ?: JSONArray()
                val songs = buildList {
                    for (songIndex in 0 until songArray.length()) {
                        val songObject = songArray.optJSONObject(songIndex) ?: continue
                        val loadedBeatsPerMeasure = songObject.optInt("beatsPerMeasure", 4).coerceIn(2, 16)
                        val loadedAccentBeat = songObject.optInt("accentBeat", 1)
                            .coerceIn(1, loadedBeatsPerMeasure)
                        add(
                            PlaylistSong(
                                name = songObject.optString("name", "Song ${songIndex + 1}"),
                                bpm = songObject.optInt("bpm", 64).coerceIn(MIN_BPM, MAX_BPM),
                                beatsPerMeasure = loadedBeatsPerMeasure,
                                accentBeat = loadedAccentBeat,
                                subdivisionCount = songObject.optInt("subdivisionCount", 1)
                                    .toSupportedSubdivisionCount(),
                                beatAccentTypes = songObject.optJSONArray("beatAccentTypes")
                                    .toBeatAccentTypes(loadedBeatsPerMeasure, loadedAccentBeat),
                                accentIntensityMode = AccentIntensityMode.fromPersistedValue(
                                    songObject.optInt(
                                        "accentIntensityMode",
                                        AccentIntensityMode.Big.persistedValue,
                                    ),
                                ),
                                musicalKey = songObject.optString("musicalKey", "C"),
                                note = songObject.optString("note", "Count in"),
                            ),
                        )
                    }
                }

                add(
                    SavedPlaylist(
                        name = playlistObject.optString("name", "Set ${playlistIndex + 1}"),
                        songs = songs.ifEmpty { listOf(defaultPlaylistSong(1)) },
                    ),
                )
            }
        }.ifEmpty { defaultSavedPlaylists() }
    }.getOrElse {
        defaultSavedPlaylists()
    }
}

private fun Context.saveSavedPlaylists(playlists: List<SavedPlaylist>) {
    val playlistArray = JSONArray()

    playlists.forEach { playlist ->
        val songArray = JSONArray()
        playlist.songs.forEach { song ->
            songArray.put(
                JSONObject()
                    .put("name", song.name)
                    .put("bpm", song.bpm)
                    .put("beatsPerMeasure", song.beatsPerMeasure)
                    .put("accentBeat", song.accentBeat)
                    .put("subdivisionCount", song.subdivisionCount)
                    .put("beatAccentTypes", song.beatAccentTypes.toJsonArray())
                    .put("accentIntensityMode", song.accentIntensityMode.persistedValue)
                    .put("musicalKey", song.musicalKey)
                    .put("note", song.note),
            )
        }

        playlistArray.put(
            JSONObject()
                .put("name", playlist.name)
                .put("songs", songArray),
        )
    }

    getSharedPreferences(PLAYLIST_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(PLAYLIST_LIBRARY_KEY, playlistArray.toString())
        }
}

private fun JSONArray?.toBeatAccentTypes(
    beatsPerMeasure: Int,
    accentBeat: Int,
): List<BeatAccentType> {
    val defaults = defaultBeatAccentTypes(beatsPerMeasure, accentBeat)
    if (this == null || length() == 0) return defaults

    return List(beatsPerMeasure.coerceIn(2, 16)) { index ->
        if (index < length()) {
            BeatAccentType.fromPersistedValue(optInt(index, BeatAccentType.Silent.persistedValue))
        } else {
            defaults[index]
        }
    }
}

private fun List<BeatAccentType>.toJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { accentType ->
            array.put(accentType.persistedValue)
        }
    }
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

private fun Float?.formatCpuUsagePercent(): String {
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
            .padding(start = 5.dp, top = 3.dp)
            .width(36.dp)
            .height(18.dp)
            .background(
                color = Color.Black.copy(alpha = 0.48f),
                shape = RoundedCornerShape(7.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = cpuUsagePercent.formatCpuUsageCompactPercent(),
            fontSize = 10.sp,
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

@Composable
private fun rememberAudioAnalysisState(
    enabled: Boolean,
    listenProfile: TunerListenProfile,
    a4ReferenceHz: Int,
    includeSpectrum: Boolean,
): AudioAnalysisState {
    var analysisState by remember { mutableStateOf(AudioAnalysisState()) }

    LaunchedEffect(enabled, listenProfile, a4ReferenceHz, includeSpectrum) {
        if (!enabled) {
            analysisState = AudioAnalysisState()
            return@LaunchedEffect
        }

        runAudioAnalyzer(
            listenProfile = listenProfile,
            a4ReferenceHz = a4ReferenceHz,
            includeSpectrum = includeSpectrum,
        ) { nextState ->
            analysisState = nextState
        }
    }

    return analysisState
}

@SuppressLint("MissingPermission")
private suspend fun runAudioAnalyzer(
    listenProfile: TunerListenProfile,
    a4ReferenceHz: Int,
    includeSpectrum: Boolean,
    onAnalysis: (AudioAnalysisState) -> Unit,
) {
    val minBufferSize = AudioRecord.getMinBufferSize(
        AUDIO_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )
    if (minBufferSize <= 0) return

    val bufferSize = maxOf(minBufferSize, AUDIO_FRAME_SIZE * 4)
    val recorder = withContext(Dispatchers.IO) {
        AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AUDIO_SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
    }
    val buffer = ShortArray(AUDIO_FRAME_SIZE)
    val pitchAverager = TunerPitchAverager(listenProfile)
    val tempoEstimator = MicTempoEstimator()
    var audioSamplePosition = 0L
    var lastUiUpdateElapsedMs = 0L
    var lastPublishedTempoBpm: Int? = null

    try {
        withContext(Dispatchers.IO) {
            recorder.startRecording()
        }

        while (true) {
            val read = withContext(Dispatchers.IO) {
                recorder.read(buffer, 0, buffer.size)
            }
            if (read > 0) {
                val frameStartAudioMs = (audioSamplePosition * 1_000L) / AUDIO_SAMPLE_RATE
                audioSamplePosition += read
                val analysis = analyzeAudioFrame(
                    buffer = buffer,
                    read = read,
                    frameStartAudioMs = frameStartAudioMs,
                    listenProfile = listenProfile,
                    pitchAverager = pitchAverager,
                    tempoEstimator = tempoEstimator,
                    a4ReferenceHz = a4ReferenceHz,
                    includeSpectrum = includeSpectrum,
                )
                val nowElapsedMs = SystemClock.elapsedRealtime()
                val tempoChanged = analysis.detectedTempoBpm != lastPublishedTempoBpm
                if (
                    nowElapsedMs - lastUiUpdateElapsedMs >= AUDIO_UI_UPDATE_INTERVAL_MS ||
                    tempoChanged
                ) {
                    onAnalysis(analysis)
                    lastUiUpdateElapsedMs = nowElapsedMs
                    lastPublishedTempoBpm = analysis.detectedTempoBpm
                }
            }
        }
    } finally {
        withContext(Dispatchers.IO) {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }
}

private fun analyzeAudioFrame(
    buffer: ShortArray,
    read: Int,
    frameStartAudioMs: Long,
    listenProfile: TunerListenProfile,
    pitchAverager: TunerPitchAverager,
    tempoEstimator: MicTempoEstimator,
    a4ReferenceHz: Int,
    includeSpectrum: Boolean,
): AudioAnalysisState {
    val sampleCount = read.coerceIn(1, buffer.size)
    var sumSquares = 0.0
    var peak = 0f
    var peakIndex = 0
    for (index in 0 until sampleCount) {
        val sample = buffer[index] / Short.MAX_VALUE.toFloat()
        val sampleAbs = abs(sample)
        if (sampleAbs > peak) {
            peak = sampleAbs
            peakIndex = index
        }
        sumSquares += sample * sample
    }
    val level = sqrt(sumSquares / sampleCount).toFloat().coerceIn(0f, 1f)
    val transientLevel = maxOf(level, peak * 0.45f).coerceIn(0f, 1f)
    val transientAudioTimeMs = frameStartAudioMs + ((peakIndex * 1_000L) / AUDIO_SAMPLE_RATE)
    val frequency = pitchAverager.average(detectPitchHz(buffer, sampleCount, listenProfile))
    val note = frequency?.toNoteReading(a4ReferenceHz)
    val keyAnalysis = pitchAverager.noteSummary(note?.first)
    val detectedTempoBpm = tempoEstimator.estimate(level = transientLevel, audioTimeMs = transientAudioTimeMs)

    return AudioAnalysisState(
        frequencyHz = frequency,
        noteName = note?.first ?: "--",
        cents = note?.second ?: 0,
        level = level,
        detectedTempoBpm = detectedTempoBpm,
        recentNotes = keyAnalysis.recentNotes,
        guessedKey = keyAnalysis.guessedKey,
        likelyChords = keyAnalysis.likelyChords,
        spectrum = if (includeSpectrum) buildSpectrum(buffer, sampleCount) else emptyList(),
    )
}

private fun detectPitchHz(
    buffer: ShortArray,
    sampleCount: Int,
    listenProfile: TunerListenProfile,
): Float? {
    val minLag = (AUDIO_SAMPLE_RATE / listenProfile.maxHz).roundToInt().coerceAtLeast(1)
    val maxLag = (AUDIO_SAMPLE_RATE / listenProfile.minHz).roundToInt().coerceAtMost(sampleCount - 2)
    if (maxLag <= minLag) return null

    var bestLag = 0
    var bestCorrelation = 0.0

    for (lag in minLag..maxLag) {
        var dot = 0.0
        var energyA = 0.0
        var energyB = 0.0
        val limit = sampleCount - lag

        for (index in 0 until limit) {
            val a = buffer[index].toDouble()
            val b = buffer[index + lag].toDouble()
            dot += a * b
            energyA += a * a
            energyB += b * b
        }

        val denominator = sqrt(energyA * energyB)
        val correlation = if (denominator > 0.0) dot / denominator else 0.0
        if (correlation > bestCorrelation) {
            bestCorrelation = correlation
            bestLag = lag
        }
    }

    if (bestLag <= 0 || bestCorrelation < 0.34) return null
    return AUDIO_SAMPLE_RATE.toFloat() / bestLag
}

private class TunerPitchAverager(
    private val listenProfile: TunerListenProfile,
) {
    private val recentFrequencies = mutableListOf<Float>()
    private val recentNoteClasses = mutableListOf<String>()

    fun average(frequency: Float?): Float? {
        if (frequency == null || frequency !in listenProfile.minHz..listenProfile.maxHz) {
            return recentFrequencies.smartAverageOrNull()
        }

        val currentAverage = recentFrequencies.smartAverageOrNull()
        if (currentAverage != null && abs(centsBetween(frequency, currentAverage)) > 85) {
            recentFrequencies.clear()
        }

        recentFrequencies += frequency
        while (recentFrequencies.size > TUNER_AVERAGE_SAMPLE_COUNT) {
            recentFrequencies.removeAt(0)
        }

        return recentFrequencies.smartAverageOrNull()
    }

    fun noteSummary(noteName: String?): KeyAnalysis {
        val noteClass = noteName?.toNoteClass()
        if (noteClass != null) {
            recentNoteClasses += noteClass
            while (recentNoteClasses.size > TUNER_KEY_SAMPLE_COUNT) {
                recentNoteClasses.removeAt(0)
            }
        }

        return analyzeMusicalKey(recentNoteClasses)
    }
}

private class MicTempoEstimator {
    private val onsetTimesMs = mutableListOf<Long>()
    private var smoothedLevel = 0f
    private var previousLevel = 0f
    private var lastOnsetMs = 0L
    private var stableTempoBpm: Int? = null
    private var pendingTempoBpm: Int? = null
    private var pendingTempoCount = 0

    fun estimate(level: Float, audioTimeMs: Long): Int? {
        val safeLevel = level.coerceIn(0f, 1f)
        if (lastOnsetMs > 0L && audioTimeMs - lastOnsetMs > TEMPO_DETECTION_TIMEOUT_MS) {
            onsetTimesMs.clear()
            stableTempoBpm = null
            pendingTempoBpm = null
            pendingTempoCount = 0
        }
        smoothedLevel = if (smoothedLevel <= 0f) {
            safeLevel
        } else {
            (smoothedLevel * 0.88f) + (safeLevel * 0.12f)
        }

        val levelJump = safeLevel - previousLevel
        val isOnset = safeLevel > 0.012f &&
            levelJump > 0.006f &&
            safeLevel > smoothedLevel * 1.18f &&
            audioTimeMs - lastOnsetMs >= MIN_TEMPO_ONSET_INTERVAL_MS

        previousLevel = safeLevel
        if (!isOnset) return stableTempoBpm

        lastOnsetMs = audioTimeMs
        onsetTimesMs += audioTimeMs
        while (onsetTimesMs.size > TEMPO_ONSET_HISTORY_COUNT) {
            onsetTimesMs.removeAt(0)
        }

        estimateTempoFromOnsets(hasStableTempo = stableTempoBpm != null)?.let { candidateBpm ->
            updateStableTempo(candidateBpm)
        }
        return stableTempoBpm
    }

    private fun estimateTempoFromOnsets(hasStableTempo: Boolean): Int? {
        if (onsetTimesMs.size < 4) return null

        val intervals = onsetTimesMs
            .zipWithNext { previous, current -> current - previous }
            .filter { it in MIN_TEMPO_ONSET_INTERVAL_MS..MAX_TEMPO_ONSET_INTERVAL_MS }
            .takeLast(TEMPO_INTERVAL_WINDOW_COUNT)
        val minimumIntervalCount = if (hasStableTempo) 4 else 3
        if (intervals.size < minimumIntervalCount) return null

        val bpmCandidates = intervals
            .map { intervalMs ->
                (60_000f / intervalMs).roundToInt()
                    .normalizedDetectedTempo()
                    .coerceIn(MIN_BPM, MAX_BPM)
            }
            .sorted()

        val medianBpm = bpmCandidates[bpmCandidates.size / 2]
        val clustered = bpmCandidates.filter { bpm ->
            abs(bpm - medianBpm) <= TEMPO_CLUSTER_TOLERANCE_BPM
        }
        val requiredClusterSize = if (hasStableTempo) {
            ((intervals.size / 2) + 1).coerceAtLeast(4)
        } else {
            ((intervals.size / 2) + 1).coerceAtLeast(3)
        }
        if (clustered.size < requiredClusterSize) return null

        return clustered.average().roundToInt()
    }

    private fun updateStableTempo(candidateBpm: Int) {
        val currentStable = stableTempoBpm
        if (currentStable != null && abs(candidateBpm - currentStable) <= TEMPO_CLUSTER_TOLERANCE_BPM) {
            stableTempoBpm = ((currentStable * 0.82f) + (candidateBpm * 0.18f)).roundToInt()
            pendingTempoBpm = null
            pendingTempoCount = 0
            return
        }

        if (pendingTempoBpm?.let { abs(candidateBpm - it) <= TEMPO_CLUSTER_TOLERANCE_BPM } == true) {
            pendingTempoBpm = ((pendingTempoBpm ?: candidateBpm) + candidateBpm) / 2
            pendingTempoCount += 1
        } else {
            pendingTempoBpm = candidateBpm
            pendingTempoCount = 1
        }

        if (currentStable == null || pendingTempoCount >= TEMPO_CHANGE_CONFIRMATION_COUNT) {
            stableTempoBpm = pendingTempoBpm
            pendingTempoBpm = null
            pendingTempoCount = 0
        }
    }
}

private const val MIN_TEMPO_ONSET_INTERVAL_MS = 250L
private const val MAX_TEMPO_ONSET_INTERVAL_MS = 2_000L
private const val TEMPO_ONSET_HISTORY_COUNT = 12
private const val TEMPO_INTERVAL_WINDOW_COUNT = 7
private const val TEMPO_CLUSTER_TOLERANCE_BPM = 4
private const val TEMPO_CHANGE_CONFIRMATION_COUNT = 3
private const val TEMPO_DETECTION_TIMEOUT_MS = 3_000L
private const val TUNER_KEY_SAMPLE_COUNT = 28

private val KeyNoteClasses = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val MajorScaleOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
private val MinorScaleOffsets = listOf(0, 2, 3, 5, 7, 8, 10)
private val TunerScaleProfiles = listOf(
    ScaleProfile(
        displaySuffix = "",
        offsets = MajorScaleOffsets,
        chordQualities = listOf("", "m", "m", "", "", "m", "dim"),
    ),
    ScaleProfile(
        displaySuffix = "m",
        offsets = MinorScaleOffsets,
        chordQualities = listOf("m", "dim", "", "m", "m", "", ""),
    ),
    ScaleProfile(
        displaySuffix = " Dor",
        offsets = listOf(0, 2, 3, 5, 7, 9, 10),
        chordQualities = listOf("m", "m", "", "", "m", "dim", ""),
    ),
    ScaleProfile(
        displaySuffix = " Mix",
        offsets = listOf(0, 2, 4, 5, 7, 9, 10),
        chordQualities = listOf("", "m", "dim", "", "m", "m", ""),
    ),
)

private fun String.toNoteClass(): String? {
    val noteClass = takeWhile { it.isLetter() || it == '#' }
    return noteClass.takeIf { it in KeyNoteClasses }
}

private fun analyzeMusicalKey(noteClasses: List<String>): KeyAnalysis {
    if (noteClasses.size < 6) {
        return KeyAnalysis(
            recentNotes = noteClasses.takeLast(8),
            guessedKey = null,
            likelyChords = emptyList(),
        )
    }

    val weightedCounts = FloatArray(KeyNoteClasses.size)
    noteClasses.forEachIndexed { position, noteClass ->
        val noteIndex = KeyNoteClasses.indexOf(noteClass)
        if (noteIndex >= 0) {
            val recencyWeight = 0.7f + (position.toFloat() / noteClasses.lastIndex.coerceAtLeast(1)) * 1.3f
            weightedCounts[noteIndex] += recencyWeight
        }
    }

    var bestGuess: KeyGuess? = null
    KeyNoteClasses.forEachIndexed { rootIndex, rootName ->
        TunerScaleProfiles.forEach { scale ->
            val score = scaleScore(
                counts = weightedCounts,
                rootIndex = rootIndex,
                scaleOffsets = scale.offsets,
                tonalCenterIndex = noteClasses.lastOrNull()?.let { KeyNoteClasses.indexOf(it) } ?: -1,
            )
            if (bestGuess == null || score > bestGuess!!.score) {
                bestGuess = KeyGuess(
                    rootIndex = rootIndex,
                    displayName = "$rootName${scale.displaySuffix}",
                    scale = scale,
                    score = score,
                )
            }
        }
    }

    val guess = bestGuess
    return KeyAnalysis(
        recentNotes = noteClasses.takeLast(8),
        guessedKey = guess?.displayName,
        likelyChords = guess?.likelyChords(weightedCounts).orEmpty(),
    )
}

private fun scaleScore(
    counts: FloatArray,
    rootIndex: Int,
    scaleOffsets: List<Int>,
    tonalCenterIndex: Int,
): Float {
    var score = 0f
    val scaleNoteIndices = scaleOffsets.map { offset ->
        (rootIndex + offset).floorMod(KeyNoteClasses.size)
    }
    scaleOffsets.forEachIndexed { scaleDegree, offset ->
        val noteIndex = (rootIndex + offset).floorMod(KeyNoteClasses.size)
        val weight = when (scaleDegree) {
            0 -> 5f
            4 -> 3.5f
            2 -> 2.4f
            6 -> 1.8f
            else -> 1.2f
        }
        score += counts[noteIndex] * weight
    }
    counts.forEachIndexed { noteIndex, count ->
        if (noteIndex !in scaleNoteIndices) {
            score -= count * 1.4f
        }
    }
    if (tonalCenterIndex == rootIndex) {
        score += 4f
    }
    return score
}

private fun KeyGuess.likelyChords(counts: FloatArray): List<String> {
    return scale.offsets.asSequence()
        .mapIndexed { degreeIndex, offset ->
            val chordRootIndex = (rootIndex + offset).floorMod(KeyNoteClasses.size)
            val thirdIndex = (chordRootIndex + if (scale.chordQualities[degreeIndex] == "") 4 else 3)
                .floorMod(KeyNoteClasses.size)
            val fifthIndex = (chordRootIndex + if (scale.chordQualities[degreeIndex] == "dim") 6 else 7)
                .floorMod(KeyNoteClasses.size)
            val rootWeight = if (degreeIndex == 0) 1.5f else 1f
            val score = (counts[chordRootIndex] * 3.2f * rootWeight) +
                (counts[thirdIndex] * 2f) +
                (counts[fifthIndex] * 2f)
            "${KeyNoteClasses[chordRootIndex]}${scale.chordQualities[degreeIndex]}" to score
        }
        .sortedByDescending { it.second }
        .map { it.first }
        .distinct()
        .take(3)
        .toList()
}

private fun List<Float>.smartAverageOrNull(): Float? {
    if (isEmpty()) return null

    val sorted = sorted()
    val median = sorted[sorted.lastIndex / 2]
    val stable = filter { abs(centsBetween(it, median)) <= 45 }
        .ifEmpty { this }

    return stable.average().toFloat()
}

private fun centsBetween(
    frequency: Float,
    reference: Float,
): Float {
    return 1200f * log2(frequency / reference)
}

private fun Float.toNoteReading(a4ReferenceHz: Int): Pair<String, Int> {
    val referenceHz = a4ReferenceHz.coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ).toFloat()
    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val midi = (69f + 12f * log2(this / referenceHz)).roundToInt()
    val noteFrequency = referenceHz * 2f.pow((midi - 69) / 12f)
    val cents = (1200f * log2(this / noteFrequency)).roundToInt()
    val octave = (midi / 12) - 1
    val name = "${noteNames[midi.floorMod(noteNames.size)]}$octave"
    return name to cents
}

private fun Int.floorMod(divisor: Int): Int {
    return ((this % divisor) + divisor) % divisor
}

private fun Int.normalizedDetectedTempo(): Int {
    var tempo = this
    while (tempo < 70) tempo *= 2
    while (tempo > 190) tempo /= 2
    return tempo
}

private fun buildSpectrum(
    buffer: ShortArray,
    sampleCount: Int,
): List<Float> {
    return List(SPECTRUM_BAR_COUNT) { index ->
        val frequency = spectrumFrequencyForIndex(index, SPECTRUM_BAR_COUNT)
        goertzelLevel(buffer, sampleCount, frequency).coerceIn(0f, 1f)
    }
}

private fun List<Float>.peakSpectrumReading(): SpectrumPeak? {
    if (isEmpty()) return null

    var peakIndex = 0
    var peakLevel = 0f
    forEachIndexed { index, level ->
        if (level > peakLevel) {
            peakIndex = index
            peakLevel = level
        }
    }

    if (peakLevel < 0.015f) return null

    val frequencyHz = spectrumFrequencyForIndex(peakIndex, size)
    return SpectrumPeak(
        frequencyHz = frequencyHz,
        level = peakLevel,
        bandLabel = spectrumBandForFrequency(frequencyHz).label,
    )
}

private fun spectrumBands(): List<SpectrumBand> {
    return listOf(
        SpectrumBand(30f, 60f, "Sub bass", Color(0xFF7E8799)),
        SpectrumBand(60f, 250f, "Bass/Kick", Color(0xFF2DD4BF)),
        SpectrumBand(250f, 500f, "Low mids", Color(0xFF84CC16)),
        SpectrumBand(500f, 2_000f, "Mids/Vox", Color(0xFFFACC15)),
        SpectrumBand(2_000f, 6_000f, "Presence", Color(0xFFFB7185)),
        SpectrumBand(6_000f, 10_000f, "Air/Hats", Color(0xFF60A5FA)),
    )
}

private fun spectrumBandForFrequency(
    frequencyHz: Float,
    bands: List<SpectrumBand> = spectrumBands(),
): SpectrumBand {
    return bands.firstOrNull { frequencyHz >= it.startHz && frequencyHz < it.endHz }
        ?: bands.last()
}

private fun spectrumFrequencyForIndex(
    index: Int,
    count: Int,
): Float {
    val progress = if (count <= 1) 0f else index.toFloat() / (count - 1)
    return 30f * (10_000f / 30f).pow(progress)
}

private fun goertzelLevel(
    buffer: ShortArray,
    sampleCount: Int,
    frequency: Float,
): Float {
    val omega = 2.0 * PI * frequency / AUDIO_SAMPLE_RATE
    val coefficient = 2.0 * cos(omega)
    var q0: Double
    var q1 = 0.0
    var q2 = 0.0

    for (index in 0 until sampleCount) {
        q0 = coefficient * q1 - q2 + (buffer[index] / Short.MAX_VALUE.toDouble())
        q2 = q1
        q1 = q0
    }

    val power = q1 * q1 + q2 * q2 - coefficient * q1 * q2
    val magnitude = (sqrt(power).toFloat() * 2f / sampleCount).coerceAtLeast(0f)
    return (magnitude * 10f).coerceIn(0f, 1f)
}

@Composable
private fun QuickStopOverlay(
    text: String,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = 3.dp, end = 5.dp)
            .width(44.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(onStop) {
                detectTapGestures(
                    onPress = {
                        onStop()
                        tryAwaitRelease()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun MetronomeState.hasSameNonVisualStateAs(other: MetronomeState): Boolean {
    return bpm == other.bpm &&
        beatsPerMeasure == other.beatsPerMeasure &&
        accentBeat == other.accentBeat &&
        subdivisionCount == other.subdivisionCount &&
        beatAccentTypes == other.beatAccentTypes &&
        accentIntensityMode == other.accentIntensityMode &&
        hapticsEnabled == other.hapticsEnabled &&
        beepEnabled == other.beepEnabled &&
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
                accentIntensityMode = AccentIntensityMode.Big,
                accentIntensityRanges = defaultAccentIntensityRanges(),
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
                onAccentIntensityModeChoice = {},
                onAccentIntensityRangesChange = {},
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
