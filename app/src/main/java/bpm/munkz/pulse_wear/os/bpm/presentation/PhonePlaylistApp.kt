package bpm.munkz.pulse_wear.os.bpm.presentation

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.wear.compose.material3.Text
import bpm.munkz.pulse_wear.os.bpm.presentation.theme.BPMMunkzPulseTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val PHONE_PLAYLIST_PREFS = "bpm_munkz_phone_playlist"
private const val PHONE_PLAYLIST_INDEX_KEY = "playlist_index"
private const val PHONE_PLAYLIST_SONG_INDEX_KEY = "song_index"

@Composable
fun PhonePlaylistApp() {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(context.loadSavedPlaylists()) }
    var playlistIndex by rememberSaveable {
        mutableIntStateOf(context.phonePlaylistLoadInt(PHONE_PLAYLIST_INDEX_KEY, 0))
    }
    var songIndex by rememberSaveable {
        mutableIntStateOf(context.phonePlaylistLoadInt(PHONE_PLAYLIST_SONG_INDEX_KEY, 0))
    }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var pulseOn by remember { mutableStateOf(false) }
    var tapTimes by remember { mutableStateOf(emptyList<Long>()) }

    val safePlaylists = playlists.ifEmpty { defaultSavedPlaylists() }
    playlistIndex = playlistIndex.coerceIn(0, safePlaylists.lastIndex)
    val playlist = safePlaylists[playlistIndex]
    songIndex = songIndex.coerceIn(0, playlist.songs.lastIndex)
    val song = playlist.songs[songIndex]

    val metronomeState = remember(song, playlistIndex, songIndex, isRunning) {
        song.toPhonePlaylistMetronomeState(
            playlistIndex = playlistIndex,
            songIndex = songIndex,
            isRunning = isRunning,
        )
    }

    LaunchedEffect(isRunning, metronomeState) {
        if (isRunning) {
            MetronomeService.start(context, metronomeState)
        } else {
            MetronomeService.stop(context)
        }
    }

    LaunchedEffect(playlists) {
        context.saveSavedPlaylists(safePlaylists)
    }

    LaunchedEffect(playlistIndex, songIndex) {
        context.phonePlaylistSaveInt(PHONE_PLAYLIST_INDEX_KEY, playlistIndex)
        context.phonePlaylistSaveInt(PHONE_PLAYLIST_SONG_INDEX_KEY, songIndex)
    }

    LaunchedEffect(isRunning, song.bpm) {
        pulseOn = false
        while (isRunning) {
            pulseOn = true
            delay(90L)
            pulseOn = false
            delay((60_000L / song.bpm.coerceAtLeast(1) - 90L).coerceAtLeast(80L))
        }
    }

    fun updatePlaylists(next: List<SavedPlaylist>) {
        playlists = next.ifEmpty { listOf(defaultSavedPlaylist(1)) }
    }

    fun updateCurrentSong(update: (PlaylistSong) -> PlaylistSong) {
        updatePlaylists(
            safePlaylists.updateSong(playlistIndex, songIndex) { current ->
                update(current).normalizedForPhonePlaylist()
            },
        )
    }

    BPMMunkzPulseTheme {
        PhonePlaylistScreen(
            playlists = safePlaylists,
            playlistIndex = playlistIndex,
            songIndex = songIndex,
            isRunning = isRunning,
            pulseOn = pulseOn,
            onPlaylistIndexChange = { playlistIndex = it.phonePlaylistWrap(safePlaylists.size) },
            onSongIndexChange = { songIndex = it.phonePlaylistWrap(playlist.songs.size) },
            onRunningChange = { isRunning = it },
            onTapTempo = {
                val now = SystemClock.elapsedRealtime()
                val recentTaps = (tapTimes + now)
                    .filter { now - it <= 2_000L }
                    .takeLast(5)
                tapTimes = recentTaps
                if (recentTaps.size >= 2) {
                    val intervals = recentTaps.zipWithNext { first, second -> second - first }
                    val averageInterval = intervals.average()
                    if (averageInterval > 0.0) {
                        updateCurrentSong { current ->
                            current.copy(bpm = (60_000.0 / averageInterval).roundToInt().coerceIn(MIN_BPM, MAX_BPM))
                        }
                    }
                }
            },
            onAddPlaylist = {
                val next = safePlaylists + defaultSavedPlaylist(safePlaylists.size + 1)
                updatePlaylists(next)
                playlistIndex = next.lastIndex
                songIndex = 0
            },
            onAddSong = {
                val nextSongIndex = playlist.songs.size
                updatePlaylists(
                    safePlaylists.updatePlaylist(playlistIndex) { current ->
                        current.copy(songs = current.songs + defaultPlaylistSong(nextSongIndex + 1))
                    },
                )
                songIndex = nextSongIndex
            },
            onDeleteSong = {
                if (playlist.songs.size > 1) {
                    updatePlaylists(
                        safePlaylists.updatePlaylist(playlistIndex) { current ->
                            current.copy(songs = current.songs.filterIndexed { index, _ -> index != songIndex })
                        },
                    )
                    songIndex = songIndex.coerceAtMost((playlist.songs.size - 2).coerceAtLeast(0))
                } else {
                    val next = safePlaylists.filterIndexed { index, _ -> index != playlistIndex }
                        .ifEmpty { listOf(defaultSavedPlaylist(1)) }
                    updatePlaylists(next)
                    playlistIndex = playlistIndex.coerceAtMost(next.lastIndex)
                    songIndex = 0
                }
            },
            onPlaylistNameChange = { name ->
                updatePlaylists(
                    safePlaylists.updatePlaylist(playlistIndex) { current ->
                        current.copy(name = name.take(32).ifBlank { "Set ${playlistIndex + 1}" })
                    },
                )
            },
            onSongNameChange = { name ->
                updateCurrentSong { it.copy(name = name.take(32).ifBlank { "Song ${songIndex + 1}" }) }
            },
            onSongNoteChange = { note ->
                updateCurrentSong { it.copy(note = note.take(72)) }
            },
            onBpmChange = { bpm ->
                updateCurrentSong { it.copy(bpm = bpm.coerceIn(MIN_BPM, MAX_BPM)) }
            },
            onBeatsChange = { beats ->
                updateCurrentSong { current ->
                    val safeBeats = beats.coerceIn(2, 16)
                    current.copy(
                        beatsPerMeasure = safeBeats,
                        accentBeat = current.accentBeat.coerceIn(1, safeBeats),
                        beatAccentTypes = current.beatAccentTypes.phonePlaylistNormalizeAccents(safeBeats, current.accentBeat),
                    )
                }
            },
            onSubdivisionChange = { subdivision ->
                updateCurrentSong { it.copy(subdivisionCount = subdivision.coerceIn(1, 8)) }
            },
            onKeyChange = { key ->
                updateCurrentSong { it.copy(musicalKey = key) }
            },
            onAccentClick = { beat ->
                updateCurrentSong { current ->
                    val accents = current.beatAccentTypes
                        .phonePlaylistNormalizeAccents(current.beatsPerMeasure, current.accentBeat)
                        .mapIndexed { index, accentType ->
                            if (index + 1 == beat) accentType.next() else accentType
                        }
                    current.copy(
                        accentBeat = accents.indexOfFirst { it == BeatAccentType.Big }
                            .takeIf { it >= 0 }
                            ?.plus(1)
                            ?: current.accentBeat,
                        beatAccentTypes = accents,
                    )
                }
            },
        )
    }
}

@Composable
private fun PhonePlaylistScreen(
    playlists: List<SavedPlaylist>,
    playlistIndex: Int,
    songIndex: Int,
    isRunning: Boolean,
    pulseOn: Boolean,
    onPlaylistIndexChange: (Int) -> Unit,
    onSongIndexChange: (Int) -> Unit,
    onRunningChange: (Boolean) -> Unit,
    onTapTempo: () -> Unit,
    onAddPlaylist: () -> Unit,
    onAddSong: () -> Unit,
    onDeleteSong: () -> Unit,
    onPlaylistNameChange: (String) -> Unit,
    onSongNameChange: (String) -> Unit,
    onSongNoteChange: (String) -> Unit,
    onBpmChange: (Int) -> Unit,
    onBeatsChange: (Int) -> Unit,
    onSubdivisionChange: (Int) -> Unit,
    onKeyChange: (String) -> Unit,
    onAccentClick: (Int) -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val background = Color(0xFF050604)
    val panel = Color(0xFF151812)
    val playlist = playlists[playlistIndex]
    val song = playlist.songs[songIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Munkz Setlist",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${playlistIndex + 1}/${playlists.size} sets  :  ${songIndex + 1}/${playlist.songs.size} songs",
                    color = if (isRunning) green else Color.White.copy(alpha = 0.64f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            PhonePlaylistStatus(active = isRunning)
        }

        PhonePlaylistTextField(
            value = playlist.name,
            label = "Set",
            onValueChange = onPlaylistNameChange,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(panel)
                .border(1.dp, green.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
                .clickable(onClick = onTapTempo),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(if (pulseOn) 222.dp else 198.dp)) {
                drawCircle(color = green.copy(alpha = if (pulseOn) 0.24f else 0.12f))
                drawCircle(
                    color = green.copy(alpha = if (pulseOn) 0.88f else 0.55f),
                    radius = size.minDimension * 0.36f,
                )
                drawCircle(color = background, radius = size.minDimension * 0.25f)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = song.bpm.toString(),
                    color = Color.White,
                    fontSize = 78.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "${song.name}  :  ${song.musicalKey}",
                    color = green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhonePlaylistButton("-5", Modifier.weight(1f)) { onBpmChange(song.bpm - 5) }
            PhonePlaylistButton("-1", Modifier.weight(1f)) { onBpmChange(song.bpm - 1) }
            PhonePlaylistButton("+1", Modifier.weight(1f)) { onBpmChange(song.bpm + 1) }
            PhonePlaylistButton("+5", Modifier.weight(1f)) { onBpmChange(song.bpm + 5) }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhonePlaylistButton(
                label = if (isRunning) "Stop" else "Start",
                modifier = Modifier.weight(1.3f),
                prominent = true,
            ) {
                onRunningChange(!isRunning)
            }
            PhonePlaylistButton("Tap", Modifier.weight(1f), onClick = onTapTempo)
        }

        PhonePlaylistPanel {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhonePlaylistButton("< Set", Modifier.weight(1f), compact = true) {
                    onPlaylistIndexChange(playlistIndex - 1)
                }
                PhonePlaylistButton("New Set", Modifier.weight(1f), compact = true, onClick = onAddPlaylist)
                PhonePlaylistButton("Set >", Modifier.weight(1f), compact = true) {
                    onPlaylistIndexChange(playlistIndex + 1)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhonePlaylistButton("< Song", Modifier.weight(1f), compact = true) {
                    onSongIndexChange(songIndex - 1)
                }
                PhonePlaylistButton("Add Song", Modifier.weight(1f), compact = true, onClick = onAddSong)
                PhonePlaylistButton("Song >", Modifier.weight(1f), compact = true) {
                    onSongIndexChange(songIndex + 1)
                }
            }
        }

        PhonePlaylistPanel {
            PhonePlaylistTextField(
                value = song.name,
                label = "Song",
                onValueChange = onSongNameChange,
            )
            PhonePlaylistTextField(
                value = song.note,
                label = "Note",
                onValueChange = onSongNoteChange,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhonePlaylistStepper(
                    label = "Meter",
                    value = "${song.beatsPerMeasure}/4",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onBeatsChange(song.beatsPerMeasure - 1) },
                    onIncrease = { onBeatsChange(song.beatsPerMeasure + 1) },
                )
                PhonePlaylistStepper(
                    label = "Sub",
                    value = "x${song.subdivisionCount}",
                    modifier = Modifier.weight(1f),
                    onDecrease = { onSubdivisionChange(song.subdivisionCount - 1) },
                    onIncrease = { onSubdivisionChange(song.subdivisionCount + 1) },
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("C", "D", "E", "F", "G", "A", "B").forEach { key ->
                    PhonePlaylistMode(
                        label = key,
                        selected = song.musicalKey == key,
                        modifier = Modifier.weight(1f),
                    ) {
                        onKeyChange(key)
                    }
                }
            }
            PhonePlaylistAccentRow(
                beatAccentTypes = song.beatAccentTypes,
                beatsPerMeasure = song.beatsPerMeasure,
                accentBeat = song.accentBeat,
                onAccentClick = onAccentClick,
            )
            PhonePlaylistButton(
                label = "Delete Song",
                modifier = Modifier.fillMaxWidth(),
                compact = true,
                onClick = onDeleteSong,
            )
        }
    }
}

@Composable
private fun PhonePlaylistPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151812))
            .border(1.dp, Color(0xFF9BFF00).copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = content,
    )
}

@Composable
private fun PhonePlaylistStatus(active: Boolean) {
    val green = Color(0xFF9BFF00)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) green.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f))
            .border(1.dp, if (active) green else Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (active) green else Color.White.copy(alpha = 0.38f)),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = if (active) "Live" else "Ready",
            color = if (active) green else Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PhonePlaylistTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    val green = Color(0xFF9BFF00)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF10130E))
                .border(1.dp, green.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun PhonePlaylistButton(
    label: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(if (compact) 42.dp else 54.dp)
            .clip(shape)
            .background(if (prominent) green else Color(0xFF202619))
            .border(1.dp, if (prominent) Color.Transparent else green.copy(alpha = 0.34f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (prominent) Color.Black else Color.White,
            fontSize = if (compact) 13.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhonePlaylistStepper(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10130E))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PhonePlaylistMiniButton("-", onDecrease)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.58f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = value,
                color = Color(0xFF9BFF00),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        PhonePlaylistMiniButton("+", onIncrease)
    }
}

@Composable
private fun PhonePlaylistMiniButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhonePlaylistMode(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val green = Color(0xFF9BFF00)
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) green.copy(alpha = 0.2f) else Color(0xFF10130E))
            .border(1.dp, if (selected) green else Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) green else Color.White.copy(alpha = 0.72f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhonePlaylistAccentRow(
    beatAccentTypes: List<BeatAccentType>,
    beatsPerMeasure: Int,
    accentBeat: Int,
    onAccentClick: (Int) -> Unit,
) {
    val normalized = beatAccentTypes.phonePlaylistNormalizeAccents(beatsPerMeasure, accentBeat)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        normalized.forEachIndexed { index, accentType ->
            PhonePlaylistAccentButton(
                beat = index + 1,
                accentType = accentType,
                modifier = Modifier.weight(1f),
                onClick = { onAccentClick(index + 1) },
            )
        }
    }
}

@Composable
private fun PhonePlaylistAccentButton(
    beat: Int,
    accentType: BeatAccentType,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = when (accentType) {
        BeatAccentType.Big -> Color(0xFF9BFF00)
        BeatAccentType.Medium -> Color(0xFFFFB020)
        BeatAccentType.Small -> Color(0xFF62D9FF)
        BeatAccentType.Silent -> Color.White.copy(alpha = 0.28f)
    }
    val label = when (accentType) {
        BeatAccentType.Big -> "Big"
        BeatAccentType.Medium -> "Med"
        BeatAccentType.Small -> "Low"
        BeatAccentType.Silent -> "Mute"
    }
    Column(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = beat.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun PlaylistSong.toPhonePlaylistMetronomeState(
    playlistIndex: Int,
    songIndex: Int,
    isRunning: Boolean,
): MetronomeState {
    return MetronomeState(
        bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
        beatsPerMeasure = beatsPerMeasure.coerceIn(2, 16),
        accentBeat = accentBeat.coerceIn(1, beatsPerMeasure.coerceIn(2, 16)),
        subdivisionCount = subdivisionCount.coerceIn(1, 8),
        beatAccentTypes = beatAccentTypes.phonePlaylistNormalizeAccents(beatsPerMeasure, accentBeat),
        accentIntensityMode = accentIntensityMode,
        musicalKey = musicalKey,
        playlistIndex = playlistIndex,
        songIndex = songIndex,
        beepEnabled = true,
        hapticsEnabled = true,
        isRunning = isRunning,
    )
}

private fun PlaylistSong.normalizedForPhonePlaylist(): PlaylistSong {
    val safeBeats = beatsPerMeasure.coerceIn(2, 16)
    val safeAccentBeat = accentBeat.coerceIn(1, safeBeats)
    return copy(
        bpm = bpm.coerceIn(MIN_BPM, MAX_BPM),
        beatsPerMeasure = safeBeats,
        accentBeat = safeAccentBeat,
        subdivisionCount = subdivisionCount.coerceIn(1, 8),
        beatAccentTypes = beatAccentTypes.phonePlaylistNormalizeAccents(safeBeats, safeAccentBeat),
    )
}

private fun List<BeatAccentType>.phonePlaylistNormalizeAccents(
    beatsPerMeasure: Int,
    accentBeat: Int,
): List<BeatAccentType> {
    val safeBeats = beatsPerMeasure.coerceIn(2, 16)
    val defaults = defaultBeatAccentTypes(safeBeats, accentBeat.coerceIn(1, safeBeats))
    if (isEmpty()) return defaults
    return take(safeBeats) + List((safeBeats - size).coerceAtLeast(0)) { BeatAccentType.Medium }
}

private fun Int.phonePlaylistWrap(size: Int): Int {
    if (size <= 0) return 0
    return ((this % size) + size) % size
}

private fun Context.phonePlaylistPrefs() = getSharedPreferences(PHONE_PLAYLIST_PREFS, Context.MODE_PRIVATE)

private fun Context.phonePlaylistLoadInt(key: String, fallback: Int): Int {
    return phonePlaylistPrefs().getInt(key, fallback)
}

private fun Context.phonePlaylistSaveInt(key: String, value: Int) {
    phonePlaylistPrefs().edit { putInt(key, value) }
}
