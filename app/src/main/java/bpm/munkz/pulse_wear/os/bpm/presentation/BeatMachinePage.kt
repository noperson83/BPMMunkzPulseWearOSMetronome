package bpm.munkz.pulse_wear.os.bpm.presentation

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import bpm.munkz.pulse_wear.os.bpm.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BEAT_MACHINE_STEP_COUNT = 16
private const val BEAT_MACHINE_PAD_COUNT = 8
private const val BEAT_MACHINE_MIN_BPM = 60
private const val BEAT_MACHINE_MAX_BPM = 180

private val BeatMachinePads = listOf(
    BeatMachinePad("Kick", R.raw.wood_big, Color(0xFFFFC857)),
    BeatMachinePad("Snare", R.raw.bell_mid, Color(0xFFEF476F)),
    BeatMachinePad("Hat", R.raw.wood_lil, Color(0xFF56F1C8)),
    BeatMachinePad("Clap", R.raw.bell_lil, Color(0xFF8D6BFF)),
    BeatMachinePad("Tom", R.raw.wood_mid, Color(0xFFFF8A3D)),
    BeatMachinePad("Bell", R.raw.bell_big, Color(0xFF7BE6FF)),
    BeatMachinePad("Perc", R.raw.wood_lil, Color(0xFFB9F45D)),
    BeatMachinePad("Hit", R.raw.bell_mid, Color(0xFFFF7BD5)),
)

private fun defaultBeatMachinePatternMasks(): List<Int> {
    return listOf(
        0b1000_1000_1000_1000,
        0b0000_1000_0000_1000,
        0b1010_1010_1010_1010,
        0,
        0b0000_0010_0000_0000,
        0,
        0,
        0,
    )
}

private data class BeatMachinePad(
    val label: String,
    val sampleResId: Int,
    val color: Color,
)

private data class BeatMachineLayoutMetrics(
    val isPhone: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val headerControlSize: Dp,
    val headerPrimaryTextSize: androidx.compose.ui.unit.TextUnit,
    val headerSecondaryTextSize: androidx.compose.ui.unit.TextUnit,
    val padWidth: Dp,
    val padHeight: Dp,
    val padSpacing: Dp,
    val padTextSize: androidx.compose.ui.unit.TextUnit,
    val stepSize: Dp,
    val stepSpacing: Dp,
    val stepTextSize: androidx.compose.ui.unit.TextUnit,
    val labelTextSize: androidx.compose.ui.unit.TextUnit,
)

@Composable
internal fun BeatMachinePage() {
    val context = LocalContext.current
    var bpm by rememberSaveable { mutableIntStateOf(96) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var selectedPadIndex by rememberSaveable { mutableIntStateOf(0) }
    var playheadStep by rememberSaveable { mutableIntStateOf(0) }
    var patternMasks by rememberSaveable { mutableStateOf(defaultBeatMachinePatternMasks()) }
    var liveRecordedPadIndex by rememberSaveable { mutableIntStateOf(-1) }
    var playlists by remember(context) { mutableStateOf(context.loadSavedPlaylists()) }
    var playlistIndex by rememberSaveable { mutableIntStateOf(0) }
    var songIndex by rememberSaveable { mutableIntStateOf(0) }
    var saveStatus by rememberSaveable { mutableStateOf("") }
    var metronomePanelOpen by rememberSaveable { mutableStateOf(false) }
    var setManagerOpen by rememberSaveable { mutableStateOf(false) }
    var setNameDraft by rememberSaveable { mutableStateOf("") }
    var songNameDraft by rememberSaveable { mutableStateOf("") }
    var songNoteDraft by rememberSaveable { mutableStateOf("") }
    var tapTempoTimes by remember { mutableStateOf(emptyList<Long>()) }
    val sampler = remember {
        BeatMachineSampler(context.applicationContext)
    }
    val playbackScope = rememberCoroutineScope()
    val safePlaylists = playlists.ifEmpty { defaultSavedPlaylists() }
    playlistIndex = playlistIndex.coerceIn(0, safePlaylists.lastIndex)
    val selectedPlaylist = safePlaylists[playlistIndex]
    songIndex = songIndex.coerceIn(0, selectedPlaylist.songs.lastIndex)
    val selectedSong = selectedPlaylist.songs[songIndex]

    fun loadBeatMachineSongFrom(nextPlaylists: List<SavedPlaylist>, nextPlaylistIndex: Int, nextSongIndex: Int) {
        val usablePlaylists = nextPlaylists.ifEmpty { defaultSavedPlaylists() }
        val clampedPlaylistIndex = nextPlaylistIndex.coerceIn(0, usablePlaylists.lastIndex)
        val nextPlaylist = usablePlaylists[clampedPlaylistIndex]
        val clampedSongIndex = nextSongIndex.coerceIn(0, nextPlaylist.songs.lastIndex)
        val nextSong = nextPlaylist.songs[clampedSongIndex]
        playlistIndex = clampedPlaylistIndex
        songIndex = clampedSongIndex
        bpm = nextSong.bpm.coerceIn(BEAT_MACHINE_MIN_BPM, BEAT_MACHINE_MAX_BPM)
        patternMasks = nextSong.beatMachineSequenceMasks?.take(8) ?: defaultBeatMachinePatternMasks()
        if (setManagerOpen) {
            setNameDraft = nextPlaylist.name
            songNameDraft = nextSong.name
            songNoteDraft = nextSong.note
        }
        saveStatus = ""
    }

    fun loadBeatMachineSong(nextPlaylistIndex: Int, nextSongIndex: Int) {
        loadBeatMachineSongFrom(safePlaylists, nextPlaylistIndex, nextSongIndex)
    }

    fun saveSequenceToSong() {
        val nextPlaylists = safePlaylists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(
                bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
                beatMachineSequenceMasks = patternMasks.take(8).map { it.coerceIn(0, 0xFFFF) },
            )
        }
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
        saveStatus = "Saved to ${nextPlaylists[playlistIndex].songs[songIndex].name}"
    }

    fun openSetManager() {
        setNameDraft = selectedPlaylist.name
        songNameDraft = selectedSong.name
        songNoteDraft = selectedSong.note
        setManagerOpen = true
    }

    fun saveSetName() {
        val cleanName = setNameDraft.trim().take(32).ifBlank { "Set ${playlistIndex + 1}" }
        val nextPlaylists = safePlaylists.mapIndexed { index, playlist ->
            if (index == playlistIndex) playlist.copy(name = cleanName) else playlist
        }
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
        setNameDraft = cleanName
        saveStatus = "Renamed set"
    }

    fun saveSongDetails() {
        val cleanName = songNameDraft.trim().take(32).ifBlank { "Song ${songIndex + 1}" }
        val cleanNote = songNoteDraft.trim().take(72)
        val nextPlaylists = safePlaylists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(name = cleanName, note = cleanNote)
        }
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
        songNameDraft = cleanName
        songNoteDraft = cleanNote
        saveStatus = "Saved song details"
    }

    fun createSet() {
        val newSet = defaultSavedPlaylist(safePlaylists.size + 1)
        val nextPlaylists = safePlaylists + newSet
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
        setNameDraft = newSet.name
        songNameDraft = newSet.songs.first().name
        songNoteDraft = newSet.songs.first().note
        loadBeatMachineSongFrom(nextPlaylists, nextPlaylists.lastIndex, 0)
    }

    fun createSong() {
        val nextSongIndex = selectedPlaylist.songs.size
        val nextPlaylists = safePlaylists.updatePlaylist(playlistIndex) { playlist ->
            playlist.copy(songs = playlist.songs + defaultPlaylistSong(nextSongIndex + 1))
        }
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
        loadBeatMachineSongFrom(nextPlaylists, playlistIndex, nextSongIndex)
    }

    fun deleteSong() {
        val nextPlaylists = if (selectedPlaylist.songs.size > 1) {
            safePlaylists.updatePlaylist(playlistIndex) { playlist ->
                playlist.copy(songs = playlist.songs.filterIndexed { index, _ -> index != songIndex })
            }
        } else {
            safePlaylists
                .filterIndexed { index, _ -> index != playlistIndex }
                .ifEmpty { listOf(defaultSavedPlaylist(1)) }
        }
        val nextPlaylistIndex = if (selectedPlaylist.songs.size > 1) playlistIndex else playlistIndex.coerceAtMost(nextPlaylists.lastIndex)
        val nextSongIndex = if (selectedPlaylist.songs.size > 1) songIndex.coerceAtMost((selectedPlaylist.songs.size - 2).coerceAtLeast(0)) else 0
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
        loadBeatMachineSongFrom(nextPlaylists, nextPlaylistIndex, nextSongIndex)
    }

    fun deleteSet() {
        val nextPlaylists = safePlaylists
            .filterIndexed { index, _ -> index != playlistIndex }
            .ifEmpty { defaultSavedPlaylists() }
        val nextPlaylistIndex = playlistIndex.coerceAtMost(nextPlaylists.lastIndex)
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
        setNameDraft = nextPlaylists[nextPlaylistIndex].name
        songNameDraft = nextPlaylists[nextPlaylistIndex].songs.first().name
        songNoteDraft = nextPlaylists[nextPlaylistIndex].songs.first().note
        loadBeatMachineSongFrom(nextPlaylists, nextPlaylistIndex, 0)
    }

    fun updateSongMeter(beats: Int) {
        val safeBeats = beats.coerceIn(2, 16)
        val nextPlaylists = safePlaylists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(
                beatsPerMeasure = safeBeats,
                accentBeat = song.accentBeat.coerceIn(1, safeBeats),
                beatAccentTypes = defaultBeatAccentTypes(safeBeats, song.accentBeat.coerceIn(1, safeBeats)),
            )
        }
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
    }

    fun updateSongSubdivision(subdivision: Int) {
        val nextPlaylists = safePlaylists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(subdivisionCount = subdivision.coerceIn(1, 8))
        }
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
    }

    fun updateSongKey(key: String) {
        val nextPlaylists = safePlaylists.updateSong(playlistIndex, songIndex) { song ->
            song.copy(musicalKey = key)
        }
        playlists = nextPlaylists
        context.saveSavedPlaylists(nextPlaylists)
    }

    fun recordTapTempo() {
        val now = SystemClock.elapsedRealtime()
        val recentTaps = (tapTempoTimes + now)
            .filter { now - it <= 2_000L }
            .takeLast(5)
        tapTempoTimes = recentTaps
        if (recentTaps.size >= 2) {
            val averageInterval = recentTaps
                .zipWithNext { first, second -> second - first }
                .average()
            if (averageInterval > 0.0) {
                bpm = (60_000.0 / averageInterval)
                    .toInt()
                    .coerceIn(BEAT_MACHINE_MIN_BPM, BEAT_MACHINE_MAX_BPM)
            }
        }
    }

    DisposableEffect(sampler) {
        onDispose { sampler.release() }
    }

    LaunchedEffect(Unit) {
        loadBeatMachineSong(playlistIndex, songIndex)
    }

    LaunchedEffect(saveStatus) {
        if (saveStatus.isNotBlank()) {
            delay(1_600L)
            saveStatus = ""
        }
    }

    LaunchedEffect(liveRecordedPadIndex) {
        if (liveRecordedPadIndex >= 0) {
            delay(140L)
            liveRecordedPadIndex = -1
        }
    }

    LaunchedEffect(isPlaying, bpm, patternMasks) {
        if (!isPlaying) return@LaunchedEffect

        val stepDelayMs = BeatMachineGrooveAgent.stepDurationMs(bpm)
        while (isPlaying) {
            val currentStep = playheadStep
            BeatMachinePads.forEachIndexed { padIndex, _ ->
                if (patternMasks[padIndex].hasStep(currentStep)) {
                    val timingOffsetMs = BeatMachineGrooveAgent.timingOffsetMs(
                        padIndex = padIndex,
                        step = currentStep,
                        stepDurationMs = stepDelayMs,
                    )
                    if (timingOffsetMs <= 0L) {
                        sampler.play(padIndex)
                    } else {
                        playbackScope.launch {
                            delay(timingOffsetMs)
                            sampler.play(padIndex)
                        }
                    }
                }
            }
            delay(stepDelayMs)
            playheadStep = (playheadStep + 1) % BEAT_MACHINE_STEP_COUNT
        }
    }

    val selectedPad = BeatMachinePads[selectedPadIndex]
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val isPhoneLayout = maxWidth >= 300.dp || maxHeight >= 360.dp
        val metrics = if (isPhoneLayout) {
            val phoneHorizontalPadding = 22.dp
            val phonePadSpacing = 8.dp
            val phonePadWidth = ((maxWidth - (phoneHorizontalPadding * 2) - (phonePadSpacing * 3)) / 4)
                .coerceIn(62.dp, 78.dp)
            BeatMachineLayoutMetrics(
                isPhone = true,
                horizontalPadding = phoneHorizontalPadding,
                verticalPadding = 18.dp,
                headerControlSize = 54.dp,
                headerPrimaryTextSize = 20.sp,
                headerSecondaryTextSize = 12.sp,
                padWidth = phonePadWidth,
                padHeight = 58.dp,
                padSpacing = phonePadSpacing,
                padTextSize = 13.sp,
                stepSize = 42.dp,
                stepSpacing = 8.dp,
                stepTextSize = 12.sp,
                labelTextSize = 22.sp,
            )
        } else {
            BeatMachineLayoutMetrics(
                isPhone = false,
                horizontalPadding = 10.dp,
                verticalPadding = 8.dp,
                headerControlSize = 34.dp,
                headerPrimaryTextSize = 14.sp,
                headerSecondaryTextSize = 9.sp,
                padWidth = 38.dp,
                padHeight = 31.dp,
                padSpacing = 4.dp,
                padTextSize = 8.sp,
                stepSize = 24.dp,
                stepSpacing = 4.dp,
                stepTextSize = 7.sp,
                labelTextSize = 11.sp,
            )
        }
        val phoneInsetsModifier = if (metrics.isPhone) {
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1A1510), Color(0xFF050607)),
                        center = Offset(110f, 92f),
                        radius = if (metrics.isPhone) 640f else 220f,
                    ),
                )
                .then(phoneInsetsModifier)
                .padding(horizontal = metrics.horizontalPadding, vertical = metrics.verticalPadding),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = selectedPad.color.copy(alpha = if (metrics.isPhone) 0.10f else 0.15f),
                    radius = size.minDimension * if (metrics.isPhone) 0.42f else 0.44f,
                    center = center,
                    style = Stroke(width = if (metrics.isPhone) 18f else 12f),
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BeatMachineHeader(
                    bpm = bpm,
                    isPlaying = isPlaying,
                    metrics = metrics,
                    onBpmChange = { bpm = (bpm + it).coerceIn(BEAT_MACHINE_MIN_BPM, BEAT_MACHINE_MAX_BPM) },
                    onBpmClick = { metronomePanelOpen = true },
                    onTogglePlaying = { isPlaying = !isPlaying },
                )

                Spacer(modifier = Modifier.height(if (metrics.isPhone) 10.dp else 4.dp))

                BeatMachineSongStrip(
                    playlist = selectedPlaylist,
                    songIndex = songIndex,
                    saveStatus = saveStatus,
                    metrics = metrics,
                    onPreviousSong = {
                        val nextSongIndex = songIndex - 1
                        if (nextSongIndex >= 0) {
                            loadBeatMachineSong(playlistIndex, nextSongIndex)
                        } else {
                            val nextPlaylistIndex = (playlistIndex - 1).wrap(safePlaylists.size)
                            loadBeatMachineSong(nextPlaylistIndex, safePlaylists[nextPlaylistIndex].songs.lastIndex)
                        }
                    },
                    onNextSong = {
                        val nextSongIndex = songIndex + 1
                        if (nextSongIndex <= selectedPlaylist.songs.lastIndex) {
                            loadBeatMachineSong(playlistIndex, nextSongIndex)
                        } else {
                            val nextPlaylistIndex = (playlistIndex + 1).wrap(safePlaylists.size)
                            loadBeatMachineSong(nextPlaylistIndex, 0)
                        }
                    },
                    onSaveSequence = ::saveSequenceToSong,
                    onOpenSetManager = ::openSetManager,
                )

                Spacer(modifier = Modifier.height(if (metrics.isPhone) 10.dp else 4.dp))

                BeatMachinePadGrid(
                    selectedPadIndex = selectedPadIndex,
                    playheadStep = playheadStep,
                    patternMasks = patternMasks,
                    liveRecordedPadIndex = liveRecordedPadIndex,
                    metrics = metrics,
                    onPadSelected = { padIndex ->
                        selectedPadIndex = padIndex
                        sampler.play(padIndex)
                        if (isPlaying) {
                            val recordStep = playheadStep
                            patternMasks = patternMasks.mapIndexed { index, mask ->
                                if (index == padIndex) mask or (1 shl recordStep) else mask
                            }
                            liveRecordedPadIndex = padIndex
                            saveStatus = "${BeatMachinePads[padIndex].label} -> step ${recordStep + 1}"
                        }
                    },
                )

                Spacer(modifier = Modifier.height(if (metrics.isPhone) 18.dp else 6.dp))

                StepRail(
                    selectedPad = selectedPad,
                    selectedMask = patternMasks[selectedPadIndex],
                    playheadStep = playheadStep,
                    metrics = metrics,
                    onStepToggle = { step ->
                        patternMasks = patternMasks.mapIndexed { index, mask ->
                            if (index == selectedPadIndex) mask xor (1 shl step) else mask
                        }
                    },
                )

                Spacer(modifier = Modifier.height(if (metrics.isPhone) 10.dp else 4.dp))

                Text(
                    text = selectedPad.label.uppercase(),
                    color = selectedPad.color,
                    fontSize = metrics.labelTextSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            if (metronomePanelOpen) {
                BeatMachineMetronomePanel(
                    bpm = bpm,
                    isPlaying = isPlaying,
                    metrics = metrics,
                    onTempoDelta = { delta ->
                        bpm = (bpm + delta).coerceIn(BEAT_MACHINE_MIN_BPM, BEAT_MACHINE_MAX_BPM)
                    },
                    onTapTempo = ::recordTapTempo,
                    onTogglePlaying = { isPlaying = !isPlaying },
                    onClose = { metronomePanelOpen = false },
                )
            }

            if (setManagerOpen) {
                BeatMachineSetManagerPanel(
                    playlist = selectedPlaylist,
                    playlistIndex = playlistIndex,
                    playlistCount = safePlaylists.size,
                    song = selectedSong,
                    songIndex = songIndex,
                    setNameDraft = setNameDraft,
                    songNameDraft = songNameDraft,
                    songNoteDraft = songNoteDraft,
                    metrics = metrics,
                    onSetNameChange = { setNameDraft = it.take(32) },
                    onSongNameChange = { songNameDraft = it.take(32) },
                    onSongNoteChange = { songNoteDraft = it.take(72) },
                    onPreviousSet = {
                        val nextPlaylistIndex = (playlistIndex - 1).wrap(safePlaylists.size)
                        loadBeatMachineSong(nextPlaylistIndex, 0)
                    },
                    onNextSet = {
                        val nextPlaylistIndex = (playlistIndex + 1).wrap(safePlaylists.size)
                        loadBeatMachineSong(nextPlaylistIndex, 0)
                    },
                    onSaveName = ::saveSetName,
                    onSaveSong = ::saveSongDetails,
                    onCreateSet = ::createSet,
                    onDeleteSet = ::deleteSet,
                    onCreateSong = ::createSong,
                    onDeleteSong = ::deleteSong,
                    onMeterDelta = { updateSongMeter(selectedSong.beatsPerMeasure + it) },
                    onSubdivisionDelta = { updateSongSubdivision(selectedSong.subdivisionCount + it) },
                    onKeyChange = ::updateSongKey,
                    onClose = { setManagerOpen = false },
                )
            }
        }
    }
}

@Composable
private fun BeatMachineSongStrip(
    playlist: SavedPlaylist,
    songIndex: Int,
    saveStatus: String,
    metrics: BeatMachineLayoutMetrics,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onSaveSequence: () -> Unit,
    onOpenSetManager: () -> Unit,
) {
    val song = playlist.songs[songIndex]
    val height = if (metrics.isPhone) 56.dp else 30.dp
    val fontSize = if (metrics.isPhone) 13.sp else 8.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (metrics.isPhone) 8.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BeatMachineMiniButton("<", metrics, onPreviousSong)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(7.dp))
                .clickable(onClick = onOpenSetManager)
                .padding(vertical = if (metrics.isPhone) 4.dp else 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${playlist.name}  ${songIndex + 1}/${playlist.songs.size}",
                color = Color(0xFFFFC857),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = saveStatus.ifBlank { "${song.name} : ${song.bpm} BPM" },
                color = Color.White.copy(alpha = 0.82f),
                fontSize = if (metrics.isPhone) 12.sp else 7.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        BeatMachineMiniButton("Save", metrics, onSaveSequence)
        BeatMachineMiniButton(">", metrics, onNextSong)
    }
}

@Composable
private fun BeatMachineSetManagerPanel(
    playlist: SavedPlaylist,
    playlistIndex: Int,
    playlistCount: Int,
    song: PlaylistSong,
    songIndex: Int,
    setNameDraft: String,
    songNameDraft: String,
    songNoteDraft: String,
    metrics: BeatMachineLayoutMetrics,
    onSetNameChange: (String) -> Unit,
    onSongNameChange: (String) -> Unit,
    onSongNoteChange: (String) -> Unit,
    onPreviousSet: () -> Unit,
    onNextSet: () -> Unit,
    onSaveName: () -> Unit,
    onSaveSong: () -> Unit,
    onCreateSet: () -> Unit,
    onDeleteSet: () -> Unit,
    onCreateSong: () -> Unit,
    onDeleteSong: () -> Unit,
    onMeterDelta: (Int) -> Unit,
    onSubdivisionDelta: (Int) -> Unit,
    onKeyChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val panelShape = RoundedCornerShape(if (metrics.isPhone) 16.dp else 12.dp)
    val rowGap = if (metrics.isPhone) 10.dp else 5.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .padding(if (metrics.isPhone) 22.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(panelShape)
                .background(Color(0xFF11140F))
                .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.42f), panelShape)
                .verticalScroll(rememberScrollState())
                .padding(if (metrics.isPhone) 18.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 12.dp else 6.dp),
        ) {
            Text(
                text = "Set Manager",
                color = Color(0xFFFFC857),
                fontSize = if (metrics.isPhone) 18.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${playlistIndex + 1}/$playlistCount sets  :  ${songIndex + 1}/${playlist.songs.size} songs",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = if (metrics.isPhone) 12.sp else 7.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            BeatMachineTextEditField(
                label = "Set",
                value = setNameDraft,
                onValueChange = onSetNameChange,
                metrics = metrics,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowGap),
            ) {
                BeatMachineTempoButton("< Set", metrics, Modifier.weight(1f), onClick = onPreviousSet)
                BeatMachineTempoButton("Save Set", metrics, Modifier.weight(1f), prominent = true, onClick = onSaveName)
                BeatMachineTempoButton("Set >", metrics, Modifier.weight(1f), onClick = onNextSet)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowGap),
            ) {
                BeatMachineTempoButton("New Set", metrics, Modifier.weight(1f), onClick = onCreateSet)
                BeatMachineTempoButton("Delete Set", metrics, Modifier.weight(1f), onClick = onDeleteSet)
            }
            BeatMachineTextEditField(
                label = "Song",
                value = songNameDraft,
                onValueChange = onSongNameChange,
                metrics = metrics,
            )
            BeatMachineTextEditField(
                label = "Note",
                value = songNoteDraft,
                onValueChange = onSongNoteChange,
                metrics = metrics,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowGap),
            ) {
                BeatMachineStepper(
                    label = "Meter",
                    value = "${song.beatsPerMeasure}/4",
                    metrics = metrics,
                    modifier = Modifier.weight(1f),
                    onDecrease = { onMeterDelta(-1) },
                    onIncrease = { onMeterDelta(1) },
                )
                BeatMachineStepper(
                    label = "Sub",
                    value = "x${song.subdivisionCount}",
                    metrics = metrics,
                    modifier = Modifier.weight(1f),
                    onDecrease = { onSubdivisionDelta(-1) },
                    onIncrease = { onSubdivisionDelta(1) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 7.dp else 3.dp),
            ) {
                listOf("C", "D", "E", "F", "G", "A", "B").forEach { key ->
                    BeatMachineKeyButton(
                        label = key,
                        selected = song.musicalKey == key,
                        metrics = metrics,
                        modifier = Modifier.weight(1f),
                        onClick = { onKeyChange(key) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowGap),
            ) {
                BeatMachineTempoButton("Save Song", metrics, Modifier.weight(1f), prominent = true, onClick = onSaveSong)
                BeatMachineTempoButton("Add Song", metrics, Modifier.weight(1f), onClick = onCreateSong)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowGap),
            ) {
                BeatMachineTempoButton("Delete Song", metrics, Modifier.weight(1f), onClick = onDeleteSong)
                BeatMachineTempoButton("Done", metrics, Modifier.weight(1f), onClick = onClose)
            }
        }
    }
}

@Composable
private fun BeatMachineTextEditField(
    label: String,
    value: String,
    metrics: BeatMachineLayoutMetrics,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 5.dp else 2.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = if (metrics.isPhone) 11.sp else 6.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = if (metrics.isPhone) 18.sp else 9.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (metrics.isPhone) 48.dp else 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                .padding(horizontal = if (metrics.isPhone) 12.dp else 6.dp, vertical = if (metrics.isPhone) 12.dp else 6.dp),
        )
    }
}

@Composable
private fun BeatMachineStepper(
    label: String,
    value: String,
    metrics: BeatMachineLayoutMetrics,
    modifier: Modifier = Modifier,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(if (metrics.isPhone) 50.dp else 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (metrics.isPhone) 7.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 6.dp else 3.dp),
    ) {
        BeatMachineSquareButton("-", metrics, onDecrease)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = if (metrics.isPhone) 11.sp else 6.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = value,
                color = Color(0xFFFFC857),
                fontSize = if (metrics.isPhone) 13.sp else 7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        BeatMachineSquareButton("+", metrics, onIncrease)
    }
}

@Composable
private fun BeatMachineSquareButton(
    text: String,
    metrics: BeatMachineLayoutMetrics,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(if (metrics.isPhone) 30.dp else 18.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFFFC857).copy(alpha = 0.86f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = if (metrics.isPhone) 16.sp else 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BeatMachineKeyButton(
    label: String,
    selected: Boolean,
    metrics: BeatMachineLayoutMetrics,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(if (metrics.isPhone) 38.dp else 22.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFFFC857) else Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.40f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = if (metrics.isPhone) 13.sp else 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BeatMachineMiniButton(
    text: String,
    metrics: BeatMachineLayoutMetrics,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(if (metrics.isPhone) 54.dp else 30.dp)
            .height(if (metrics.isPhone) 40.dp else 22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFFFC857).copy(alpha = 0.86f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = if (metrics.isPhone) 12.sp else 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BeatMachineHeader(
    bpm: Int,
    isPlaying: Boolean,
    metrics: BeatMachineLayoutMetrics,
    onBpmChange: (Int) -> Unit,
    onBpmClick: () -> Unit,
    onTogglePlaying: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundBeatControl(text = "-5", color = Color(0xFF56F1C8), metrics = metrics, onClick = { onBpmChange(-5) })
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBpmClick)
                .padding(horizontal = if (metrics.isPhone) 10.dp else 2.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "BPM ${bpm}",
                color = Color.White,
                fontSize = metrics.headerPrimaryTextSize,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "MUNKZ MPC",
                color = Color(0xFFFFC857),
                fontSize = metrics.headerSecondaryTextSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        RoundBeatControl(
            text = if (isPlaying) "Stop" else "Play",
            color = if (isPlaying) Color(0xFFEF476F) else Color(0xFFB9F45D),
            metrics = metrics,
            onClick = onTogglePlaying,
        )
        RoundBeatControl(text = "+5", color = Color(0xFF56F1C8), metrics = metrics, onClick = { onBpmChange(5) })
    }
}

@Composable
private fun BeatMachineMetronomePanel(
    bpm: Int,
    isPlaying: Boolean,
    metrics: BeatMachineLayoutMetrics,
    onTempoDelta: (Int) -> Unit,
    onTapTempo: () -> Unit,
    onTogglePlaying: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val panelShape = RoundedCornerShape(if (metrics.isPhone) 16.dp else 12.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .padding(if (metrics.isPhone) 22.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(panelShape)
                .background(Color(0xFF11140F))
                .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.42f), panelShape)
                .padding(if (metrics.isPhone) 18.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 14.dp else 7.dp),
        ) {
            Text(
                text = "Beat Machine BPM",
                color = Color(0xFFFFC857),
                fontSize = if (metrics.isPhone) 18.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = bpm.toString(),
                color = Color.White,
                fontSize = if (metrics.isPhone) 78.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 10.dp else 5.dp),
            ) {
                BeatMachineTempoButton("-5", metrics, Modifier.weight(1f)) { onTempoDelta(-5) }
                BeatMachineTempoButton("-1", metrics, Modifier.weight(1f)) { onTempoDelta(-1) }
                BeatMachineTempoButton("+1", metrics, Modifier.weight(1f)) { onTempoDelta(1) }
                BeatMachineTempoButton("+5", metrics, Modifier.weight(1f)) { onTempoDelta(5) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (metrics.isPhone) 10.dp else 5.dp),
            ) {
                BeatMachineTempoButton(
                    text = if (isPlaying) "Stop" else "Start",
                    metrics = metrics,
                    modifier = Modifier.weight(1f),
                    prominent = true,
                    onClick = onTogglePlaying,
                )
                BeatMachineTempoButton(
                    text = "Tap",
                    metrics = metrics,
                    modifier = Modifier.weight(1f),
                    onClick = onTapTempo,
                )
                BeatMachineTempoButton(
                    text = "Done",
                    metrics = metrics,
                    modifier = Modifier.weight(1f),
                    onClick = onClose,
                )
            }
        }
    }
}

@Composable
private fun BeatMachineTempoButton(
    text: String,
    metrics: BeatMachineLayoutMetrics,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (prominent) Color(0xFF9BDB59) else Color(0xFFFFC857)
    Box(
        modifier = modifier
            .height(if (metrics.isPhone) 54.dp else 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.9f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = if (metrics.isPhone) 16.sp else 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BeatMachinePadGrid(
    selectedPadIndex: Int,
    playheadStep: Int,
    patternMasks: List<Int>,
    liveRecordedPadIndex: Int,
    metrics: BeatMachineLayoutMetrics,
    onPadSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(metrics.padSpacing)) {
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.padSpacing)) {
                repeat(4) { column ->
                    val padIndex = row * 4 + column
                    val pad = BeatMachinePads[padIndex]
                    val selected = padIndex == selectedPadIndex
                    val activeOnPlayhead = patternMasks.getOrNull(padIndex)?.hasStep(playheadStep) == true
                    val justRecorded = padIndex == liveRecordedPadIndex
                    Box(
                        modifier = Modifier
                            .size(width = metrics.padWidth, height = metrics.padHeight)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (justRecorded || activeOnPlayhead) {
                                    pad.color
                                } else if (selected) {
                                    pad.color.copy(alpha = 0.88f)
                                } else {
                                    Color.White.copy(alpha = 0.10f)
                                },
                            )
                            .border(
                                width = 1.dp,
                                color = pad.color.copy(alpha = if (selected || activeOnPlayhead || justRecorded) 1f else 0.55f),
                                shape = RoundedCornerShape(7.dp),
                            )
                            .clickable { onPadSelected(padIndex) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = pad.label,
                            color = if (selected || activeOnPlayhead || justRecorded) Color.Black else Color.White,
                            fontSize = metrics.padTextSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRail(
    selectedPad: BeatMachinePad,
    selectedMask: Int,
    playheadStep: Int,
    metrics: BeatMachineLayoutMetrics,
    onStepToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(metrics.stepSpacing)) {
        repeat(4) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.stepSpacing)) {
                repeat(4) { column ->
                    val step = row * 4 + column
                    val active = selectedMask.hasStep(step)
                    val playhead = step == playheadStep
                    Box(
                        modifier = Modifier
                            .size(metrics.stepSize)
                            .clip(CircleShape)
                            .background(
                                when {
                                    playhead && active -> selectedPad.color
                                    playhead -> Color.White.copy(alpha = 0.86f)
                                    active -> selectedPad.color.copy(alpha = 0.70f)
                                    else -> Color.White.copy(alpha = 0.10f)
                                },
                            )
                            .border(
                                width = if (playhead) 2.dp else 1.dp,
                                color = if (playhead) Color.White else selectedPad.color.copy(alpha = 0.52f),
                                shape = CircleShape,
                            )
                            .clickable { onStepToggle(step) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (step + 1).toString(),
                            color = if (active || playhead) Color.Black else MaterialTheme.colorScheme.onBackground,
                            fontSize = metrics.stepTextSize,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundBeatControl(
    text: String,
    color: Color,
    metrics: BeatMachineLayoutMetrics,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(metrics.headerControlSize)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.84f))
            .border(1.dp, Color.White.copy(alpha = 0.62f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = if (metrics.isPhone) 13.sp else 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Int.hasStep(step: Int): Boolean = this and (1 shl step) != 0

private class BeatMachineSampler(context: android.content.Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(BEAT_MACHINE_PAD_COUNT)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .build()
    private val sampleIds = BeatMachinePads.map { pad ->
        soundPool.load(context, pad.sampleResId, 1)
    }

    fun play(padIndex: Int) {
        sampleIds.getOrNull(padIndex)?.let { sampleId ->
            soundPool.play(sampleId, 0.85f, 0.85f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
