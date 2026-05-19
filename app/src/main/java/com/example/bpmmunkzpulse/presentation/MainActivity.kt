package com.example.bpmmunkzpulse.presentation

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MIN_BPM = 30
private const val MAX_BPM = 240
private const val BEAT_FLASH_DURATION_MS = 80L
private const val BEEP_LEAD_MS = 55L
private const val BEEP_DURATION_MS = 70
private const val TAP_TEMPO_RESET_TIMEOUT_MS = 2_000L
private const val TAP_TEMPO_SAMPLE_COUNT = 5
private const val PULSE_PAGE_COUNT = 4
@Suppress("SpellCheckingInspection")
private const val WATCH_FACE_PACKAGE = "com.example.bpmmunkzface"
private const val PLAYLIST_PREFS = "bpm_munkz_playlists"
private const val PLAYLIST_LIBRARY_KEY = "playlist_library"
private const val DEFAULT_MAIN_COLOR = -47872
private const val DEFAULT_BACKGROUND_COLOR = -16777216
private const val DEFAULT_CLOCK_COLOR = -47872
private const val DEFAULT_BIG_PULSE_RING_COLOR = -47872

private val PulseColorOptions = listOf(
    -47872,
    -16715777,
    -32512,
    -7667457,
    -1,
    -65281,
)

private val ThemeMainColorOptions = listOf(
    -47872,
    -16715777,
    -32512,
    -7667457,
    -1,
    -65281,
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
    val musicalKey: String,
    val note: String,
)

private data class SavedPlaylist(
    val name: String,
    val songs: List<PlaylistSong>,
)

private data class ClockImageChoice(
    val label: String,
)

private val ClockImageChoices = listOf(
    ClockImageChoice("Rainb"),
    ClockImageChoice("Blue"),
    ClockImageChoice("Green"),
    ClockImageChoice("Orange"),
    ClockImageChoice("Purple"),
    ClockImageChoice("White"),
    ClockImageChoice("Munk"),
    ClockImageChoice("Sax"),
    ClockImageChoice("Piano"),
    ClockImageChoice("Gtr"),
    ClockImageChoice("Trum"),
    ClockImageChoice("Rock"),
)

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
        AppScaffold {
            BeatPulseScreen()
        }
    }
}

@Composable
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
fun BeatPulseScreen() {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val vibrator = remember(context, isPreview) {
        if (isPreview) {
            null
        } else {
            context.beatPulseVibrator()
        }
    }
    val wakeLock = remember(context, isPreview) {
        if (isPreview) {
            null
        } else {
            context.beatPulseWakeLock()
        }
    }
    val tonePlayer = remember(isPreview) {
        if (isPreview) {
            null
        } else {
            BeatTonePlayer.create()
        }
    }

    var bpm by rememberSaveable { mutableIntStateOf(64) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var beatFlash by remember { mutableStateOf(false) }
    var flashingBeat by remember { mutableIntStateOf(0) }
    var tempoTapSync by remember { mutableIntStateOf(0) }
    var beatClockStartedAtMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var beatsPerMeasure by rememberSaveable { mutableIntStateOf(4) }
    var accentBeat by rememberSaveable { mutableIntStateOf(1) }
    var beepEnabled by rememberSaveable { mutableStateOf(false) }
    var mainColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_MAIN_COLOR) }
    var backgroundColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_BACKGROUND_COLOR) }
    var clockColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_CLOCK_COLOR) }
    val clockImageIndexState = rememberSaveable { mutableIntStateOf(0) }
    var bigPulseRingColorArgb by rememberSaveable { mutableIntStateOf(DEFAULT_BIG_PULSE_RING_COLOR) }
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
    var playlistEditorOpen by rememberSaveable { mutableStateOf(false) }
    val tapTempoTimes = remember { mutableListOf<Long>() }
    val pagerState = rememberPagerState(pageCount = { PULSE_PAGE_COUNT })
    val playlistIndex = selectedPlaylistIndexState.intValue.coerceIn(0, playlists.lastIndex)
    val currentPlaylist = playlists[playlistIndex]
    val songIndex = selectedSongIndexState.intValue.coerceIn(0, currentPlaylist.songs.lastIndex)
    val currentSong = currentPlaylist.songs[songIndex]
    val selectedClockImageIndex = clockImageIndexState.intValue.coerceIn(0, ClockImageChoices.lastIndex)
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

    val clearTapTempo = {
        tapTempoTimes.clear()
    }

    fun selectPlaylist(index: Int) {
        val nextPlaylistIndex = index.wrap(playlists.size)
        val nextPlaylist = playlists[nextPlaylistIndex]
        selectedPlaylistIndexState.intValue = nextPlaylistIndex
        selectedSongIndexState.intValue = 0
        bpm = nextPlaylist.songs.first().bpm
        beatClockStartedAtMs = SystemClock.elapsedRealtime()
    }

    fun selectSong(index: Int) {
        val nextSongIndex = index.wrap(currentPlaylist.songs.size)
        selectedSongIndexState.intValue = nextSongIndex
        bpm = currentPlaylist.songs[nextSongIndex].bpm
        beatClockStartedAtMs = SystemClock.elapsedRealtime()
    }

    fun updateCurrentSong(update: (PlaylistSong) -> PlaylistSong) {
        playlists = playlists.updateSong(playlistIndex, songIndex, update)
    }

    val toggleRunning = {
        clearTapTempo()
        if (!isRunning) {
            beatClockStartedAtMs = SystemClock.elapsedRealtime()
        }
        isRunning = !isRunning
    }

    val recordTapTempo = {
        val now = SystemClock.elapsedRealtime()

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
                bpm = (60_000.0 / averageIntervalMs).roundToInt().coerceIn(MIN_BPM, MAX_BPM)
            }
        }

        if (isRunning) {
            beatClockStartedAtMs = now
            tempoTapSync += 1
        }
    }

    LaunchedEffect(beatsPerMeasure) {
        if (accentBeat > beatsPerMeasure) {
            accentBeat = beatsPerMeasure
        }
    }

    LaunchedEffect(playlists, isPreview) {
        if (!isPreview) {
            context.saveSavedPlaylists(playlists)
        }
    }

    LaunchedEffect(pagerState.currentPage, playlistEditorOpen, currentSong.bpm) {
        if (pagerState.currentPage == 2 && !playlistEditorOpen && bpm != currentSong.bpm) {
            bpm = currentSong.bpm
            beatClockStartedAtMs = SystemClock.elapsedRealtime()
        }
    }

    LaunchedEffect(isRunning, wakeLock) {
        if (isRunning) {
            wakeLock?.acquireIfNeeded()
        } else {
            wakeLock?.releaseIfHeld()
        }
    }

    LaunchedEffect(isRunning, bpm, tempoTapSync, beatsPerMeasure, accentBeat, beepEnabled) {
        beatFlash = false
        flashingBeat = 0
        var beat = 1
        var shouldLeadCurrentBeat = beepEnabled && beat == accentBeat

        while (isRunning) {
            val intervalMs = 60_000L / bpm
            val isAccentBeat = beat == accentBeat

            if (shouldLeadCurrentBeat) {
                tonePlayer?.beep()
                delay(BEEP_LEAD_MS)
                shouldLeadCurrentBeat = false
            }

            beatFlash = true
            flashingBeat = beat
            vibrator?.pulse(isAccentBeat)

            delay(BEAT_FLASH_DURATION_MS)
            beatFlash = false
            flashingBeat = 0

            val nextBeat = if (beat == beatsPerMeasure) 1 else beat + 1
            val waitUntilNextBeatMs = (intervalMs - BEAT_FLASH_DURATION_MS).coerceAtLeast(0L)
            val shouldLeadBeep = beepEnabled && nextBeat == accentBeat

            if (shouldLeadBeep && waitUntilNextBeatMs > BEEP_LEAD_MS) {
                delay(waitUntilNextBeatMs - BEEP_LEAD_MS)
                tonePlayer?.beep()
                delay(BEEP_LEAD_MS)
            } else {
                delay(waitUntilNextBeatMs)
            }

            beat = nextBeat
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            wakeLock?.releaseIfHeld()
            vibrator?.cancel()
            tonePlayer?.release()
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> TapTempoPage(
                    bpm = bpm,
                    beatFlash = beatFlash,
                    isAccentFlash = flashingBeat == accentBeat,
                    isRunning = isRunning,
                    onTapTempo = recordTapTempo,
                    onDecrease = {
                        clearTapTempo()
                        bpm = (bpm - 1).coerceAtLeast(MIN_BPM)
                    },
                    onDecreaseLarge = {
                        clearTapTempo()
                        bpm = (bpm - 5).coerceAtLeast(MIN_BPM)
                    },
                    onIncrease = {
                        clearTapTempo()
                        bpm = (bpm + 1).coerceAtMost(MAX_BPM)
                    },
                    onIncreaseLarge = {
                        clearTapTempo()
                        bpm = (bpm + 5).coerceAtMost(MAX_BPM)
                    },
                    onToggleRunning = toggleRunning,
                )

                    1 -> SettingsPage(
                    beatsPerMeasure = beatsPerMeasure,
                    accentBeat = accentBeat,
                    beepEnabled = beepEnabled,
                    mainColorArgb = mainColorArgb,
                    backgroundColorArgb = backgroundColorArgb,
                    clockColorArgb = clockColorArgb,
                    clockImageIndex = selectedClockImageIndex,
                    ringColorArgb = bigPulseRingColorArgb,
                    onBeatChoice = { beatChoice ->
                        beatsPerMeasure = beatChoice
                        accentBeat = accentBeat.coerceAtMost(beatChoice)
                    },
                    onAccentBeatChoice = { beatChoice ->
                        accentBeat = beatChoice
                    },
                    onBeepToggle = {
                        beepEnabled = !beepEnabled
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
                )

                    2 -> if (playlistEditorOpen) {
                    PlaylistEditorPage(
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
                            bpm = nextPlaylists.last().songs.first().bpm
                            beatClockStartedAtMs = SystemClock.elapsedRealtime()
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
                            bpm = nextPlaylists[playlistIndex].songs[nextSongIndex].bpm
                            beatClockStartedAtMs = SystemClock.elapsedRealtime()
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
                            bpm = nextBpm
                            beatClockStartedAtMs = SystemClock.elapsedRealtime()
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
                        onDone = {
                            playlistEditorOpen = false
                        },
                    )
                } else {
                    PlaylistClockPage(
                        playlist = currentPlaylist,
                        songIndex = songIndex,
                        isRunning = isRunning,
                        beatClockStartedAtMs = beatClockStartedAtMs,
                        clockImageResId = clockImageResId,
                        clockColor = Color(clockColorArgb),
                        onPreviousSong = { selectSong(songIndex - 1) },
                        onNextSong = { selectSong(songIndex + 1) },
                        onEditPlaylist = {
                            playlistEditorOpen = true
                        },
                        onToggleRunning = toggleRunning,
                    )
                }

                    3 -> UpgradePage(
                    isPurchased = pulseProPurchased,
                    onBuyNow = {
                        pulseProPurchased = true
                    },
                    onUseWatchFace = {
                        context.openWatchFacePicker()
                    },
                )
                }
            }

            if (beatFlash && flashingBeat == accentBeat) {
                BigPulseRing(
                    color = Color(bigPulseRingColorArgb),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (pagerState.currentPage != 2) {
                PulsePagerIndicator(
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TapTempoPage(
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
                onLongClickLabel = "Decrease BPM by 5",
            )

            Button(
                onClick = onTapTempo,
                modifier = Modifier.size(72.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                CenteredButtonLabel(
                    text = "Tap",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            TempoAdjustButton(
                text = "+",
                onClick = onIncrease,
                onLongClick = onIncreaseLarge,
                onLongClickLabel = "Increase BPM by 5",
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        StartStopButton(
            isRunning = isRunning,
            onClick = onToggleRunning,
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
    playlist: SavedPlaylist,
    songIndex: Int,
    isRunning: Boolean,
    beatClockStartedAtMs: Long,
    clockImageResId: Int,
    clockColor: Color,
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
                .height(18.dp),
        ) {
            Text(
                text = "${song.bpm}",
                modifier = Modifier.align(Alignment.CenterStart),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = song.musicalKey,
                modifier = Modifier.align(Alignment.CenterEnd),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        BeatClockDial(
            bpm = song.bpm,
            isRunning = isRunning,
            beatClockStartedAtMs = beatClockStartedAtMs,
            clockImageResId = clockImageResId,
            clockColor = clockColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 14.dp)
                .size(124.dp),
        )

        GlassCommandButton(
            text = "<",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 14.dp)
                .padding(start = 8.dp)
                .size(38.dp),
            fontSize = 20.sp,
            circular = true,
            onClick = onPreviousSong,
        )

        GlassCommandButton(
            text = ">",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 14.dp)
                .padding(end = 8.dp)
                .size(38.dp),
            fontSize = 20.sp,
            circular = true,
            onClick = onNextSong,
        )

        GlassCommandButton(
            text = "Edit",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 46.dp)
                .width(42.dp)
                .height(24.dp),
            fontSize = 9.sp,
            onClick = onEditPlaylist,
        )

        GlassCommandButton(
            text = if (isRunning) "STOP" else "START",
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
private fun BeatClockDial(
    bpm: Int,
    isRunning: Boolean,
    beatClockStartedAtMs: Long,
    clockImageResId: Int,
    clockColor: Color,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    var frameTimeMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(isRunning, isPreview) {
        if (isPreview) return@LaunchedEffect

        while (isRunning) {
            frameTimeMs = SystemClock.elapsedRealtime()
            delay(16L)
        }
    }

    val elapsedMs = if (isRunning) {
        (frameTimeMs - beatClockStartedAtMs).coerceAtLeast(0L)
    } else {
        0L
    }
    val secondAngle = ((elapsedMs % 60_000L).toFloat() / 60_000f) * 360f
    val beatAngle = ((elapsedMs.toDouble() * bpm * 6.0 / 60_000.0) % 360.0).toFloat()

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
            drawLine(
                color = clockColor,
                start = center,
                end = beatEnd,
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color.Black,
                radius = 7.dp.toPx(),
                center = center,
            )
            drawCircle(
                color = clockColor,
                radius = 4.dp.toPx(),
                center = center,
            )
        }
    }
}

@Composable
private fun PlaylistEditorPage(
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
                text = "Edit Playlist",
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
                firstText = "List Name",
                firstClick = { onPlaylistNameChange(1) },
                secondText = "New List",
                secondClick = onAddPlaylist,
            )

            Spacer(modifier = Modifier.height(7.dp))

            ChoiceRow(
                label = "Song ${songIndex + 1}/${playlist.songs.size}",
                value = song.name,
                onPrevious = onPreviousSong,
                onNext = onNextSong,
            )

            TinyActionRow(
                firstText = "Song Name",
                firstClick = { onSongNameChange(1) },
                secondText = "Add Song",
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
                label = "Key",
                value = song.musicalKey,
                onDecrease = { onSongKeyChange(-1) },
                onIncrease = { onSongKeyChange(1) },
            )

            EditValueRow(
                label = "Note",
                value = song.note,
                onDecrease = { onSongNoteChange(-1) },
                onIncrease = { onSongNoteChange(1) },
            )

            Spacer(modifier = Modifier.height(7.dp))

            SmallCommandButton(
                text = "Done",
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
    isPurchased: Boolean,
    onBuyNow: () -> Unit,
    onUseWatchFace: () -> Unit,
) {
    BeatPulsePage {
        Text(
            text = "Pulse Pro",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Full app includes",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(4.dp))

        UpgradeDetail("Custom Settings")
        UpgradeDetail("Saved Tempo Playlists")
        UpgradeDetail("Seconds to Beats Clock")
        UpgradeDetail("More beat sounds")
        UpgradeDetail("BPM Munkz watch face")

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = if (isPurchased) onUseWatchFace else onBuyNow,
            modifier = Modifier
                .width(96.dp)
                .height(34.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            CenteredButtonLabel(
                text = if (isPurchased) "Use Face" else "Buy Now",
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colorOptions.forEach { colorArgb ->
            ColorSwatchButton(
                colorArgb = colorArgb,
                selected = selectedColorArgb == colorArgb,
                onClick = { onColorChoice(colorArgb) },
            )
        }
    }
}

@Composable
private fun ColorSwatchButton(
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(if (selected) 32.dp else 26.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 20.dp else 16.dp)
                .background(
                    color = Color(colorArgb),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Text(
                    text = "*",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (colorArgb == -1) Color.Black else Color.White,
                )
            }
        }
    }
}

@Composable
private fun ClockImagePicker(
    selectedIndex: Int,
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
                        text = choice.label,
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
    beatsPerMeasure: Int,
    accentBeat: Int,
    beepEnabled: Boolean,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    clockColorArgb: Int,
    clockImageIndex: Int,
    ringColorArgb: Int,
    onBeatChoice: (Int) -> Unit,
    onAccentBeatChoice: (Int) -> Unit,
    onBeepToggle: () -> Unit,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onClockColorChoice: (Int) -> Unit,
    onClockImageChoice: (Int) -> Unit,
    onRingColorChoice: (Int) -> Unit,
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
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Beat",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(4, 5, 3).forEach { beatChoice ->
                    SettingButton(
                        text = "$beatChoice/4",
                        selected = beatsPerMeasure == beatChoice,
                        modifier = Modifier
                            .width(42.dp)
                            .height(28.dp),
                        onClick = { onBeatChoice(beatChoice) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Big pulse",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (1..beatsPerMeasure).forEach { beatChoice ->
                    AccentBeatButton(
                        beat = beatChoice,
                        selected = accentBeat == beatChoice,
                        onClick = { onAccentBeatChoice(beatChoice) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Beep",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                SettingButton(
                    text = if (beepEnabled) "On" else "Off",
                    selected = beepEnabled,
                    modifier = Modifier
                        .width(50.dp)
                        .height(28.dp),
                    onClick = onBeepToggle,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Theme",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Main color",
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
                text = "BG color",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorPickerRow(
                selectedColorArgb = backgroundColorArgb,
                onColorChoice = onBackgroundColorChoice,
                colorOptions = ThemeBackgroundColorOptions,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Clock",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Clock image",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ClockImagePicker(
                selectedIndex = clockImageIndex,
                onClockImageChoice = onClockImageChoice,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hand color",
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
                text = "Big ring",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ColorPickerRow(
                selectedColorArgb = ringColorArgb,
                onColorChoice = onRingColorChoice,
            )
        }
    }
}

@Composable
private fun AccentBeatButton(
    beat: Int,
    selected: Boolean,
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
                    text = "BIG",
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
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        drawCircle(
            color = color.copy(alpha = 0.95f),
            radius = (size.minDimension - strokeWidth) / 2f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
private fun PulsePagerIndicator(
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "${currentPage + 1}/$PULSE_PAGE_COUNT",
        modifier = modifier,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
    )
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
            text = if (isRunning) "Stop" else "Start",
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

private fun Context.beatPulseVibrator(): Vibrator {
    val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    return manager.defaultVibrator
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
                    musicalKey = "C",
                    note = "Count in",
                ),
                PlaylistSong(
                    name = "Verse",
                    bpm = 92,
                    musicalKey = "G",
                    note = "Keep pocket",
                ),
                PlaylistSong(
                    name = "Chorus",
                    bpm = 116,
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
                    musicalKey = "Am",
                    note = "Hold tempo",
                ),
                PlaylistSong(
                    name = "Break",
                    bpm = 108,
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
                        add(
                            PlaylistSong(
                                name = songObject.optString("name", "Song ${songIndex + 1}"),
                                bpm = songObject.optInt("bpm", 64).coerceIn(MIN_BPM, MAX_BPM),
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

private fun Context.beatPulseWakeLock(): PowerManager.WakeLock {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:BeatPulse")
        .apply { setReferenceCounted(false) }
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

private fun Vibrator.pulse(isAccentBeat: Boolean) {
    if (!hasVibrator()) return

    vibrate(
        VibrationEffect.createOneShot(
            if (isAccentBeat) 80 else 15,
            VibrationEffect.DEFAULT_AMPLITUDE,
        ),
        VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_ALARM)
            .build(),
    )
}

private class BeatTonePlayer private constructor(
    private val toneGenerator: ToneGenerator,
) {
    fun beep() {
        toneGenerator.startTone(
            ToneGenerator.TONE_PROP_BEEP,
            BEEP_DURATION_MS,
        )
    }

    fun release() {
        toneGenerator.release()
    }

    companion object {
        fun create(): BeatTonePlayer? {
            return runCatching {
                BeatTonePlayer(ToneGenerator(AudioManager.STREAM_MUSIC, 60))
            }.getOrNull()
        }
    }
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
                beatsPerMeasure = 4,
                accentBeat = 1,
                beepEnabled = false,
                mainColorArgb = DEFAULT_MAIN_COLOR,
                backgroundColorArgb = DEFAULT_BACKGROUND_COLOR,
                clockColorArgb = DEFAULT_CLOCK_COLOR,
                clockImageIndex = 0,
                ringColorArgb = DEFAULT_BIG_PULSE_RING_COLOR,
                onBeatChoice = {},
                onAccentBeatChoice = {},
                onBeepToggle = {},
                onMainColorChoice = {},
                onBackgroundColorChoice = {},
                onClockColorChoice = {},
                onClockImageChoice = {},
                onRingColorChoice = {},
            )
        }
    }
}
