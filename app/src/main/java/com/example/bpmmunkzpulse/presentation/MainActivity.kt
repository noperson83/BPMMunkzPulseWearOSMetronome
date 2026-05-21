package com.example.bpmmunkzpulse.presentation

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val MIN_BPM = 30
internal const val MAX_BPM = 240
internal const val BEAT_FLASH_DURATION_MS = 80L
internal const val BEEP_LEAD_MS = 55L
internal const val BEEP_DURATION_MS = 70
private const val APP_LAUNCH_SPLASH_DURATION_MS = 740L
private const val TAP_TEMPO_RESET_TIMEOUT_MS = 2_000L
private const val TAP_TEMPO_SAMPLE_COUNT = 5
private const val MAIN_PAGE_INDEX = 0
private const val RHYTHM_PAGE_INDEX = 1
private const val SETTINGS_PAGE_INDEX = 2
private const val UPGRADE_PAGE_INDEX = 3
private const val TAP_TEMPO_FREE_PAGE_INDEX = 4
private const val PULSE_PAGE_COUNT = 5
@Suppress("SpellCheckingInspection")
private const val WATCH_FACE_PACKAGE = "com.example.bpmmunkzface"
private const val SETTINGS_PREFS = "bpm_munkz_settings"
private const val KEEP_SCREEN_AWAKE_WHILE_PLAYING_KEY = "keep_screen_awake_while_playing"
private const val PLAYLIST_PREFS = "bpm_munkz_playlists"
private const val PLAYLIST_LIBRARY_KEY = "playlist_library"
private const val DEFAULT_MAIN_COLOR = -47872
private const val DEFAULT_BACKGROUND_COLOR = -16777216
private const val DEFAULT_CLOCK_COLOR = -47872
private const val DEFAULT_BIG_PULSE_RING_COLOR = -47872
private const val NEON_GREEN_COLOR = 0xFF9DFF00.toInt()
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

private val PlaylistNameOptions = listOf(
    "Set 1",
    "Set 2",
    "Practice",
    "Live",
    "Studio",
    "Warmups",
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

private enum class AppLanguage {
    English,
    Spanish,
}

private enum class RhythmChoicePicker {
    TimeSignature,
    Subdivision,
}

private data class AppText(
    val tap: String,
    val start: String,
    val stop: String,
    val startUpper: String,
    val stopUpper: String,
    val settings: String,
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
    val keepScreenAwakeWhilePlaying: String,
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
    val editPlaylist: String,
    val listName: String,
    val newList: String,
    val song: String,
    val songName: String,
    val addSong: String,
    val key: String,
    val note: String,
    val done: String,
    val pulsePro: String,
    val fullAppIncludes: String,
    val customSettings: String,
    val savedTempoPlaylists: String,
    val secondsToBeatsClock: String,
    val moreBeatSounds: String,
    val watchFace: String,
    val buyNow: String,
    val useFace: String,
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

private fun appTextFor(language: AppLanguage): AppText {
    return when (language) {
        AppLanguage.English -> AppText(
            tap = "Tap",
            start = "Start",
            stop = "Stop",
            startUpper = "START",
            stopUpper = "STOP",
            settings = "Settings",
            rhythm = "Pulse",
            timeSignature = "Time Signature",
            subdivision = "Sub Divisions",
            saveSong = "Save Song",
            newSong = "New Song",
            beat = "Beat",
            beatCount = "Beat count",
            bigPulse = "Big pulse",
            haptics = "Haptics",
            beep = "Beep",
            keepScreenAwakeWhilePlaying = "Keep screen awake while playing",
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
            editPlaylist = "Edit Playlist",
            listName = "List Name",
            newList = "New List",
            song = "Song",
            songName = "Song Name",
            addSong = "Add Song",
            key = "Key",
            note = "Note",
            done = "Done",
            pulsePro = "Pulse Pro",
            fullAppIncludes = "Full app includes",
            customSettings = "Custom Settings",
            savedTempoPlaylists = "Saved Tempo Playlists",
            secondsToBeatsClock = "Seconds to Beats Clock",
            moreBeatSounds = "More beat sounds",
            watchFace = "BPM Munkz watch face",
            buyNow = "Buy Now",
            useFace = "Use Face",
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
            keepScreenAwakeWhilePlaying = "Pantalla activa al tocar",
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
            editPlaylist = "Editar lista",
            listName = "Lista",
            newList = "Nueva",
            song = "Cancion",
            songName = "Cancion",
            addSong = "Agregar",
            key = "Tono",
            note = "Nota",
            done = "Listo",
            pulsePro = "Pulse Pro",
            fullAppIncludes = "App completa",
            customSettings = "Ajustes propios",
            savedTempoPlaylists = "Listas guardadas",
            secondsToBeatsClock = "Reloj beats/seg",
            moreBeatSounds = "Mas sonidos",
            watchFace = "Face BPM Munkz",
            buyNow = "Comprar",
            useFace = "Usar Face",
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
    val beatsPerMeasure = metronomeState.beatsPerMeasure
    val accentBeat = metronomeState.accentBeat
    val subdivisionCount = metronomeState.subdivisionCount
    val beatAccentTypes = metronomeState.beatAccentTypes
    val currentBeatIndex = metronomeState.currentBeatIndex
    val currentSubdivisionIndex = metronomeState.currentSubdivisionIndex
    val hapticsEnabled = metronomeState.hapticsEnabled
    val beepEnabled = metronomeState.beepEnabled
    var keepScreenAwakeWhilePlaying by rememberSaveable {
        mutableStateOf(!isPreview && context.loadKeepScreenAwakeWhilePlaying())
    }
    var mainColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_MAIN_COLOR) }
    var backgroundColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_BACKGROUND_COLOR) }
    var clockColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_CLOCK_COLOR) }
    val clockImageIndexState = rememberSaveable { mutableIntStateOf(0) }
    var bigPulseRingColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_BIG_PULSE_RING_COLOR) }
    val appLanguageIndexState = rememberSaveable { mutableIntStateOf(0) }
    var pulseProPurchased by rememberSaveable { mutableStateOf(false) }
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
    var tapTempoPopupOpen by rememberSaveable { mutableStateOf(false) }
    val tapTempoTimes = remember { mutableListOf<Long>() }
    val pagerState = rememberPagerState(pageCount = { PULSE_PAGE_COUNT })
    val playlistIndex = selectedPlaylistIndexState.intValue.coerceIn(0, playlists.lastIndex)
    val currentPlaylist = playlists[playlistIndex]
    val songIndex = selectedSongIndexState.intValue.coerceIn(0, currentPlaylist.songs.lastIndex)
    val currentSong = currentPlaylist.songs[songIndex]
    val selectedClockImageIndex = clockImageIndexState.intValue.coerceIn(0, ClockImageChoices.lastIndex)
    val appLanguage = AppLanguages[appLanguageIndexState.intValue.coerceIn(0, AppLanguages.lastIndex)]
    val appText = appTextFor(appLanguage)
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
        val shouldShowFullPulse = !pagerState.isScrollInProgress &&
            (!playlistEditorPopupOpen || playlistRhythmEditorPopupOpen) &&
            (
                tapTempoPopupOpen ||
                    playlistRhythmEditorPopupOpen ||
                    currentPage == RHYTHM_PAGE_INDEX ||
                    currentPage == TAP_TEMPO_FREE_PAGE_INDEX
                )
        val shouldShowLightClockPulse = !pagerState.isScrollInProgress &&
            !playlistEditorPopupOpen &&
            currentPage == MAIN_PAGE_INDEX

        return when {
            shouldShowFullPulse -> state
            shouldShowLightClockPulse -> state.withLightClockVisualsFrom(metronomeState)
            else -> state.withPulseVisualsFrom(metronomeState)
        }
    }

    LaunchedEffect(metronomeService, tapTempoPopupOpen, playlistEditorPopupOpen, playlistRhythmEditorPopupOpen) {
        metronomeService?.state?.collect { state ->
            val nextState = filterMetronomeStateForVisibleUi(state)
            if (nextState != metronomeState) {
                metronomeState = nextState
            }
        }
    }

    LaunchedEffect(metronomeService, pagerState, tapTempoPopupOpen, playlistEditorPopupOpen, playlistRhythmEditorPopupOpen) {
        snapshotFlow { pagerState.isScrollInProgress }.collect { isScrollInProgress ->
            if (!isScrollInProgress) {
                metronomeService?.state?.value?.let { state ->
                    val nextState = filterMetronomeStateForVisibleUi(state)
                    if (nextState != metronomeState) {
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
        val nextPlaylists = playlists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(
                bpm = bpm,
                beatsPerMeasure = beatsPerMeasure,
                accentBeat = accentBeat,
                subdivisionCount = subdivisionCount,
                beatAccentTypes = beatAccentTypes,
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

    fun saveRhythmEditorAndClose() {
        saveCurrentRhythmToSong()
        playlistRhythmEditorPopupOpen = false
    }

    fun addSongWithCurrentRhythm() {
        val nextSongIndex = currentPlaylist.songs.size
        val nextSong = defaultPlaylistSong(nextSongIndex + 1).copy(
            bpm = bpm,
            beatsPerMeasure = beatsPerMeasure,
            accentBeat = accentBeat,
            subdivisionCount = subdivisionCount,
            beatAccentTypes = beatAccentTypes,
        )
        val nextPlaylists = playlists.updatePlaylist(playlistIndex) { playlist ->
            playlist.copy(songs = playlist.songs + nextSong)
        }
        playlists = nextPlaylists
        selectedSongIndexState.intValue = nextSongIndex
        applySongToMetronome(
            nextPlaylistIndex = playlistIndex,
            nextSongIndex = nextSongIndex,
            song = nextSong,
            restartBeat = isRunning,
        )
    }

    val toggleRunning = {
        clearTapTempo()
        if (isRunning) {
            metronomeState = metronomeState.copy(
                isRunning = false,
                beatFlash = false,
                flashingBeat = 0,
                currentSubdivisionIndex = 1,
            )
            metronomeService?.stopPlayback() ?: MetronomeService.stop(context)
        } else {
            MetronomeService.start(
                context,
                metronomeState.copy(
                    playlistIndex = playlistIndex,
                    songIndex = songIndex,
                ),
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

    LaunchedEffect(keepScreenAwakeWhilePlaying, isPreview) {
        if (!isPreview) {
            context.saveKeepScreenAwakeWhilePlaying(keepScreenAwakeWhilePlaying)
        }
    }

    LaunchedEffect(
        pagerState.currentPage,
        currentSong.bpm,
        currentSong.beatsPerMeasure,
        currentSong.accentBeat,
        currentSong.subdivisionCount,
        currentSong.beatAccentTypes,
        playlistIndex,
        songIndex,
    ) {
        if (pagerState.currentPage == MAIN_PAGE_INDEX && !isRunning) {
            applySongToMetronome(playlistIndex, songIndex, currentSong, restartBeat = false)
        }
    }

    LaunchedEffect(isRunning, keepScreenAwakeWhilePlaying, activity, isPreview) {
        if (isPreview) return@LaunchedEffect

        val keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (isRunning && keepScreenAwakeWhilePlaying) {
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
                .background(backgroundColor),
        ) {
            if (beatFlash && beatAccentTypes.typeForBeat(flashingBeat) == BeatAccentType.Big) {
                BigPulseRing(
                    colorArgb = bigPulseRingColorArgb,
                    modifier = Modifier.fillMaxSize(),
                )
            }

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
                    accentBeat = accentBeat,
                    subdivisionCount = subdivisionCount,
                    beatAccentTypes = beatAccentTypes,
                    currentBeatIndex = currentBeatIndex,
                    currentSubdivisionIndex = currentSubdivisionIndex,
                    beatFlash = beatFlash,
                    isRunning = isRunning,
                    onTimeSignatureChoice = { beatChoice ->
                        metronomeService?.setBeatsPerMeasure(beatChoice)
                    },
                    onBeatAccentTypeCycle = { beatChoice ->
                        metronomeService?.cycleBeatAccentType(beatChoice)
                    },
                    onSubdivisionChoice = { subdivision ->
                        metronomeService?.setSubdivisionCount(subdivision)
                    },
                    onToggleRunning = toggleRunning,
                    onBpmClick = {
                        tapTempoPopupOpen = true
                    },
                )

                    SETTINGS_PAGE_INDEX -> SettingsPage(
                    appText = appText,
                    hapticsEnabled = hapticsEnabled,
                    beepEnabled = beepEnabled,
                    keepScreenAwakeWhilePlaying = keepScreenAwakeWhilePlaying,
                    mainColorArgb = mainColorArgb,
                    backgroundColorArgb = backgroundColorArgb,
                    clockColorArgb = clockColorArgb,
                    clockImageIndex = selectedClockImageIndex,
                    ringColorArgb = bigPulseRingColorArgb,
                    appLanguage = appLanguage,
                    onHapticsToggle = {
                        metronomeService?.setHapticsEnabled(!hapticsEnabled)
                    },
                    onBeepToggle = {
                        metronomeService?.setBeepEnabled(!beepEnabled)
                    },
                    onKeepScreenAwakeWhilePlayingToggle = {
                        keepScreenAwakeWhilePlaying = !keepScreenAwakeWhilePlaying
                    },
                    onMainColorChoice = { colorArgb ->
                        mainColorArgb = safeMainColorArgb(
                            requestedMainColorArgb = colorArgb,
                            backgroundColorArgb = backgroundColorArgb,
                        )
                    },
                    onBackgroundColorChoice = { colorArgb ->
                        backgroundColorArgb = safeBackgroundColorArgb(
                            requestedBackgroundColorArgb = colorArgb,
                            mainColorArgb = mainColorArgb,
                        )
                    },
                    onClockColorChoice = { clockColorArgb = it },
                    onClockImageChoice = { choiceIndex ->
                        clockImageIndexState.intValue = choiceIndex.coerceIn(0, ClockImageChoices.lastIndex)
                    },
                    onRingColorChoice = { bigPulseRingColorArgb = it },
                    onLanguageChoice = { language ->
                        appLanguageIndexState.intValue = AppLanguages.indexOf(language).coerceAtLeast(0)
                    },
                )

                    UPGRADE_PAGE_INDEX -> UpgradePage(
                    appText = appText,
                    isPurchased = pulseProPurchased,
                    onBuyNow = {
                        pulseProPurchased = true
                    },
                    onUseWatchFace = {
                        context.openWatchFacePicker()
                    },
                )

                    TAP_TEMPO_FREE_PAGE_INDEX -> TapTempoFree(
                        appText = appText,
                        bpm = bpm,
                        beatFlash = beatFlash,
                        isAccentFlash = flashingBeat == accentBeat,
                        isRunning = isRunning,
                        onTapTempo = recordTapTempo,
                        onDecrease = decreaseBpm,
                        onDecreaseLarge = decreaseBpmLarge,
                        onIncrease = increaseBpm,
                        onIncreaseLarge = increaseBpmLarge,
                        onToggleRunning = toggleRunning,
                    )
                }
            }

            if (pagerState.currentPage != MAIN_PAGE_INDEX && pagerState.currentPage != RHYTHM_PAGE_INDEX) {
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
                    onPlaylistNameChange = { step ->
                        playlists = playlists.updatePlaylist(playlistIndex) { playlist ->
                            playlist.copy(
                                name = cycleOption(
                                    options = PlaylistNameOptions,
                                    current = playlist.name,
                                    step = step,
                                ),
                            )
                        }
                    },
                    onSongNameChange = { step ->
                        updateCurrentSong { song ->
                            song.copy(
                                name = cycleOption(
                                    options = SongNameOptions,
                                    current = song.name,
                                    step = step,
                                ),
                            )
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
    onTapTempo: () -> Unit,
    onDecrease: () -> Unit,
    onDecreaseLarge: () -> Unit,
    onIncrease: () -> Unit,
    onIncreaseLarge: () -> Unit,
    onToggleRunning: () -> Unit,
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
    footer: @Composable () -> Unit,
) {
    BeatPulsePage {
        BeatTempoReadout(
            bpm = bpm,
            beatFlash = beatFlash,
            isAccentFlash = isAccentFlash,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TempoAdjustButton(
                text = "-",
                onClick = onDecrease,
                onLongClick = onDecreaseLarge,
                onLongClickLabel = appText.decreaseBpmBy5,
            )

            Button(
                onClick = onTapTempo,
                modifier = Modifier.size(82.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                CenteredButtonLabel(
                    text = appText.tap,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            TempoAdjustButton(
                text = "+",
                onClick = onIncrease,
                onLongClick = onIncreaseLarge,
                onLongClickLabel = appText.increaseBpmBy5,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

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
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
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
    accentBeat: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    isRunning: Boolean,
    onTimeSignatureChoice: (Int) -> Unit,
    onBeatAccentTypeCycle: (Int) -> Unit,
    onSubdivisionChoice: (Int) -> Unit,
    onToggleRunning: () -> Unit,
    onBpmClick: () -> Unit,
) {
    var rhythmEditorOpen by rememberSaveable { mutableStateOf(false) }
    var activeChoicePicker by rememberSaveable { mutableStateOf<RhythmChoicePicker?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (rhythmEditorOpen) {
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
                onDone = { rhythmEditorOpen = false },
            )
        } else {
            RhythmLiveCanvas(
                appText = appText,
                bpm = bpm,
                beatsPerMeasure = beatsPerMeasure,
                accentBeat = accentBeat,
                subdivisionCount = subdivisionCount,
                beatAccentTypes = beatAccentTypes,
                currentBeatIndex = currentBeatIndex,
                currentSubdivisionIndex = currentSubdivisionIndex,
                beatFlash = beatFlash,
                isRunning = isRunning,
                modifier = Modifier.fillMaxSize(),
                onBpmClick = onBpmClick,
                onEdit = { rhythmEditorOpen = true },
                onToggleRunning = onToggleRunning,
            )
        }

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

@Composable
private fun RhythmLiveCanvas(
    appText: AppText,
    bpm: Int,
    beatsPerMeasure: Int,
    accentBeat: Int,
    subdivisionCount: Int,
    beatAccentTypes: List<BeatAccentType>,
    currentBeatIndex: Int,
    currentSubdivisionIndex: Int,
    beatFlash: Boolean,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    onBpmClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleRunning: () -> Unit,
) {
    BigPulseCircleSelector(
        beatsPerMeasure = beatsPerMeasure,
        beatAccentTypes = beatAccentTypes,
        currentBeatIndex = currentBeatIndex,
        beatFlash = beatFlash,
        modifier = modifier,
        onBeatAccentTypeCycle = null,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$bpm BPM",
                modifier = Modifier.clickable(onClick = onBpmClick),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulseBeatDot(
                    beatFlash = beatFlash,
                    accentType = beatAccentTypes.typeForBeat(currentBeatIndex),
                )

                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "$currentBeatIndex/$beatsPerMeasure",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Text(
                        text = "$accentBeat  x$subdivisionCount",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SubdivisionDots(
                subdivisionCount = subdivisionCount,
                currentSubdivisionIndex = currentSubdivisionIndex,
            )

            Spacer(modifier = Modifier.height(9.dp))

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

            Spacer(modifier = Modifier.height(5.dp))

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
    ) {
        Text(
            text = "$bpm BPM",
            modifier = Modifier.clickable(onClick = onBpmClick),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
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
            selected = true,
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
            selected = true,
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

        Spacer(modifier = Modifier.height(6.dp))

        SmallCommandButton(
            text = appText.done,
            modifier = Modifier
                .width(64.dp)
                .height(24.dp),
            fontSize = 9.sp,
            onClick = onDone,
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
    selected: Boolean,
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

        SettingButton(
            text = valueText,
            selected = selected,
            modifier = Modifier
                .width(48.dp)
                .height(24.dp),
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
    onBeatAccentTypeCycle: ((Int) -> Unit)?,
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
            modifier = Modifier.width(128.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }

        (1..rightCount).forEach { index ->
            val beat = index
            val angle = splitCircleAngle(index, rightCount, rightSide = true)
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
        BeatAccentType.Big,
        BeatAccentType.Silent -> 20.dp
        BeatAccentType.Medium -> 15.dp
        BeatAccentType.Small -> 10.dp
    }
    val dotSize = if (beatFlash) baseDotSize + 6.dp else baseDotSize
    val dotColor = when (accentType) {
        BeatAccentType.Big -> MaterialTheme.colorScheme.primary
        BeatAccentType.Medium -> MaterialTheme.colorScheme.primary.copy(alpha = if (beatFlash) 1f else 0.9f)
        BeatAccentType.Small -> MaterialTheme.colorScheme.primary.copy(alpha = if (beatFlash) 0.95f else 0.72f)
        BeatAccentType.Silent -> Color.Transparent
    }
    val borderModifier = if (accentType == BeatAccentType.Silent) {
        Modifier.border(
            width = if (beatFlash) 3.dp else 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
            shape = CircleShape,
        )
    } else {
        Modifier
    }

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
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(dotColor, CircleShape)
                .then(borderModifier),
        )
    }
}

private fun steppedOption(
    options: List<Int>,
    current: Int,
    step: Int,
): Int {
    val currentIndex = options.indexOf(current).takeIf { it >= 0 }
        ?: options.indexOf(options.minBy { kotlin.math.abs(it - current) })
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
private fun PulseBeatDot(
    beatFlash: Boolean,
    accentType: BeatAccentType,
) {
    val dotSize = when (accentType) {
        BeatAccentType.Big -> if (beatFlash) 20.dp else 10.dp
        BeatAccentType.Medium -> if (beatFlash) 15.dp else 8.dp
        BeatAccentType.Small -> if (beatFlash) 10.dp else 6.dp
        BeatAccentType.Silent -> if (beatFlash) 20.dp else 10.dp
    }
    val borderModifier = if (accentType == BeatAccentType.Silent) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = if (beatFlash) 0.9f else 0.55f),
            shape = CircleShape,
        )
    } else {
        Modifier
    }
    val dotColor = if (accentType == BeatAccentType.Silent) {
        Color.Transparent
    } else if (beatFlash) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = Modifier
            .size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .background(dotColor, CircleShape)
                .then(borderModifier),
        )
    }
}

@Composable
private fun SubdivisionDots(
    subdivisionCount: Int,
    currentSubdivisionIndex: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..subdivisionCount).forEach { subdivisionIndex ->
            Box(
                modifier = Modifier
                    .size(if (subdivisionIndex == currentSubdivisionIndex) 5.dp else 3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (subdivisionIndex == currentSubdivisionIndex) 0.72f else 0.3f,
                        ),
                        shape = CircleShape,
                    ),
            )
        }
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
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = playlist.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = "${songIndex + 1}/${playlist.songs.size} ${song.name}",
                modifier = Modifier.width(132.dp),
                fontSize = 12.sp,
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
                .padding(top = 48.dp)
                .width(152.dp)
                .height(24.dp),
        ) {
            Text(
                text = "${song.bpm}",
                modifier = Modifier.align(Alignment.CenterStart),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = song.musicalKey,
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
                .padding(top = 14.dp)
                .size(124.dp),
        )

        PlaylistNavButton(
            isNext = false,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 14.dp)
                .padding(start = 8.dp),
            onClick = onPreviousSong,
        )

        PlaylistNavButton(
            isNext = true,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 14.dp)
                .padding(end = 8.dp),
            onClick = onNextSong,
        )

        PlaylistTimingBadge(
            text = "${song.beatsPerMeasure}/4 x${song.subdivisionCount}",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 46.dp)
                .width(54.dp)
                .height(24.dp),
        )

        GlassCommandButton(
            text = appText.edit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 46.dp)
                .width(42.dp)
                .height(24.dp),
            fontSize = 9.sp,
            onClick = onEditPlaylist,
        )

        GlassCommandButton(
            text = if (isRunning) appText.stopUpper else appText.startUpper,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
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
            .width(34.dp)
            .height(56.dp)
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
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = chevronColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlaylistTimingBadge(
    text: String,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
    onPlaylistNameChange: (Int) -> Unit,
    onSongNameChange: (Int) -> Unit,
    onSongBpmChange: (Int) -> Unit,
    onSongKeyChange: (Int) -> Unit,
    onSongNoteChange: (Int) -> Unit,
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
            onPlaylistNameChange = onPlaylistNameChange,
            onSongNameChange = onSongNameChange,
            onSongBpmChange = onSongBpmChange,
            onSongKeyChange = onSongKeyChange,
            onSongNoteChange = onSongNoteChange,
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
    var horizontalDrag by remember { mutableStateOf(0f) }

    BackHandler(onBack = onDone)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
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
    onPlaylistNameChange: (Int) -> Unit,
    onSongNameChange: (Int) -> Unit,
    onSongBpmChange: (Int) -> Unit,
    onSongKeyChange: (Int) -> Unit,
    onSongNoteChange: (Int) -> Unit,
    onEditRhythm: () -> Unit,
    onDone: () -> Unit,
) {
    val playlist = playlists[playlistIndex]
    val song = playlist.songs[songIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = appText.editPlaylist,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            ChoiceRow(
                label = "${playlistIndex + 1}/${playlists.size}",
                value = playlist.name,
                onPrevious = onPreviousPlaylist,
                onNext = onNextPlaylist,
            )

            TinyActionRow(
                firstText = appText.listName,
                firstClick = { onPlaylistNameChange(1) },
                secondText = appText.newList,
                secondClick = onAddPlaylist,
            )

            Spacer(modifier = Modifier.height(7.dp))

            ChoiceRow(
                label = "${appText.song} ${songIndex + 1}/${playlist.songs.size}",
                value = song.name,
                onPrevious = onPreviousSong,
                onNext = onNextSong,
            )

            TinyActionRow(
                firstText = appText.songName,
                firstClick = { onSongNameChange(1) },
                secondText = appText.addSong,
                secondClick = onAddSong,
            )

            Spacer(modifier = Modifier.height(7.dp))

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
            )

            Spacer(modifier = Modifier.height(7.dp))

            SmallCommandButton(
                text = appText.editRhythm,
                modifier = Modifier
                    .width(92.dp)
                    .height(28.dp),
                fontSize = 10.sp,
                onClick = onEditRhythm,
            )

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
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    value: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
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

        Text(
            text = value,
            modifier = Modifier.width(92.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
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
private fun TinyActionRow(
    firstText: String,
    firstClick: () -> Unit,
    secondText: String,
    secondClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(4.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallCommandButton(
            text = firstText,
            modifier = Modifier
                .width(72.dp)
                .height(26.dp),
            fontSize = 8.sp,
            onClick = firstClick,
        )

        SmallCommandButton(
            text = secondText,
            modifier = Modifier
                .width(72.dp)
                .height(26.dp),
            fontSize = 8.sp,
            onClick = secondClick,
        )
    }
}

@Composable
private fun EditValueRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
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

        Text(
            text = value,
            modifier = Modifier.width(92.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

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
private fun UpgradePage(
    appText: AppText,
    isPurchased: Boolean,
    onBuyNow: () -> Unit,
    onUseWatchFace: () -> Unit,
) {
    BeatPulsePage {
        Text(
            text = appText.pulsePro,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = appText.fullAppIncludes,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(4.dp))

        UpgradeDetail(appText.customSettings)
        UpgradeDetail(appText.savedTempoPlaylists)
        UpgradeDetail(appText.secondsToBeatsClock)
        UpgradeDetail(appText.moreBeatSounds)
        UpgradeDetail(appText.watchFace)

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = if (isPurchased) onUseWatchFace else onBuyNow,
            modifier = Modifier
                .width(96.dp)
                .height(34.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            CenteredButtonLabel(
                text = if (isPurchased) appText.useFace else appText.buyNow,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
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
                        onClick = { onClockImageChoice(choiceIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockImageChoiceButton(
    text: String,
    selected: Boolean,
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

    Box(
        modifier = Modifier
            .width(42.dp)
            .height(26.dp)
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
            fontSize = 9.sp,
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
                onClick = { onLanguageChoice(language) },
            )
        }
    }
}

@Composable
private fun UpgradeDetail(
    text: String,
) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun SettingsPage(
    appText: AppText,
    hapticsEnabled: Boolean,
    beepEnabled: Boolean,
    keepScreenAwakeWhilePlaying: Boolean,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    clockColorArgb: Int,
    clockImageIndex: Int,
    ringColorArgb: Int,
    appLanguage: AppLanguage,
    onHapticsToggle: () -> Unit,
    onBeepToggle: () -> Unit,
    onKeepScreenAwakeWhilePlayingToggle: () -> Unit,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onClockColorChoice: (Int) -> Unit,
    onClockImageChoice: (Int) -> Unit,
    onRingColorChoice: (Int) -> Unit,
    onLanguageChoice: (AppLanguage) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appText.haptics,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                SettingButton(
                    text = if (hapticsEnabled) appText.on else appText.off,
                    selected = hapticsEnabled,
                    modifier = Modifier
                        .width(50.dp)
                        .height(28.dp),
                    onClick = onHapticsToggle,
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = appText.beep,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                SettingButton(
                    text = if (beepEnabled) appText.on else appText.off,
                    selected = beepEnabled,
                    modifier = Modifier
                        .width(50.dp)
                        .height(28.dp),
                    onClick = onBeepToggle,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText.keepScreenAwakeWhilePlaying,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(3.dp))

            SettingButton(
                text = if (keepScreenAwakeWhilePlaying) appText.on else appText.off,
                selected = keepScreenAwakeWhilePlaying,
                modifier = Modifier
                    .width(50.dp)
                    .height(28.dp),
                onClick = onKeepScreenAwakeWhilePlayingToggle,
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = appText.clock,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

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
    }
}

@Composable
private fun AccentBeatButton(
    beat: Int,
    selected: Boolean,
    bigLabel: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(if (selected) 40.dp else 28.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "$beat",
                fontSize = if (selected) 15.sp else 11.sp,
                fontWeight = FontWeight.Bold,
            )

            if (selected) {
                Text(
                    text = bigLabel,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SettingButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
    ) {
        CenteredButtonLabel(
            text = text,
            fontSize = 11.sp,
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
private fun BigPulseRing(
    colorArgb: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        if (isRainbowColor(colorArgb)) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = RainbowColors,
                    center = center,
                ),
                radius = (size.minDimension - strokeWidth) / 2f,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        } else {
            drawCircle(
                color = colorFromChoice(colorArgb).copy(alpha = 0.95f),
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
) {
    val pulseSize = when {
        beatFlash && isAccentFlash -> 28.dp
        beatFlash -> 22.dp
        else -> 11.dp
    }

    Row(
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
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(86.dp)
            .height(height = 34.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        CenteredButtonLabel(
            text = if (isRunning) appText.stop else appText.start,
            fontSize = 12.sp,
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

private fun Context.loadKeepScreenAwakeWhilePlaying(): Boolean {
    return getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEEP_SCREEN_AWAKE_WHILE_PLAYING_KEY, false)
}

private fun Context.saveKeepScreenAwakeWhilePlaying(keepScreenAwakeWhilePlaying: Boolean) {
    getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit {
            putBoolean(KEEP_SCREEN_AWAKE_WHILE_PLAYING_KEY, keepScreenAwakeWhilePlaying)
        }
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

private fun Context.openWatchFacePicker() {
    if (!isBpmMunkzWatchFaceInstalled()) {
        Toast.makeText(
            this,
            "Install BPM Munkz Pulse watch face first",
            Toast.LENGTH_LONG,
        ).show()
        return
    }

    val pickerIntents = listOf(
        Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
        Intent(Intent.ACTION_SET_WALLPAPER),
    )

    val didOpenPicker = pickerIntents.any { pickerIntent ->
        runCatching {
            startActivity(pickerIntent)
        }.isSuccess
    }

    if (!didOpenPicker) {
        Toast.makeText(
            this,
            "Choose BPM Munkz Pulse from watch faces",
            Toast.LENGTH_SHORT,
        ).show()
    }
}

private fun Context.isBpmMunkzWatchFaceInstalled(): Boolean {
    return runCatching {
        packageManager.getPackageInfo(
            WATCH_FACE_PACKAGE,
            PackageManager.PackageInfoFlags.of(0),
        )
    }.isSuccess
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
                hapticsEnabled = true,
                beepEnabled = false,
                keepScreenAwakeWhilePlaying = false,
                mainColorArgb = DEFAULT_MAIN_COLOR,
                backgroundColorArgb = DEFAULT_BACKGROUND_COLOR,
                clockColorArgb = DEFAULT_CLOCK_COLOR,
                clockImageIndex = 0,
                ringColorArgb = DEFAULT_BIG_PULSE_RING_COLOR,
                appLanguage = AppLanguage.English,
                onHapticsToggle = {},
                onBeepToggle = {},
                onKeepScreenAwakeWhilePlayingToggle = {},
                onMainColorChoice = {},
                onBackgroundColorChoice = {},
                onClockColorChoice = {},
                onClockImageChoice = {},
                onRingColorChoice = {},
                onLanguageChoice = {},
            )
        }
    }
}
