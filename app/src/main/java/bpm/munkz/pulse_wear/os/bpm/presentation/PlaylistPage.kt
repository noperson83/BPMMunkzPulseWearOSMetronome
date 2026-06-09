package bpm.munkz.pulse_wear.os.bpm.presentation

import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import kotlin.math.abs
@Composable
internal fun PlaylistClockPage(
    appText: AppText,
    playlist: SavedPlaylist,
    songIndex: Int,
    isRunning: Boolean,
    beatClockStartedAtMs: Long,
    playbackStartedAtMs: Long,
    clockImageResId: Int,
    clockColorArgb: Int,
    forceSimpleRhythm: Boolean = false,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onEditPlaylist: () -> Unit,
    onToggleRunning: () -> Unit,
) {
    val song = playlist.songs[songIndex]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val pageHorizontalPadding = if (watchSClass) 6.dp else 8.dp
        val pageVerticalPadding = if (watchSClass) 8.dp else 4.dp
        val headerTopPadding = if (watchSClass) 10.dp else 14.dp
        val headerTitleFontSize = if (watchSClass) 11.sp else 12.sp
        val songLineWidth = if (watchSClass) 116.dp else 132.dp
        val songLineStartPadding = if (watchSClass) 8.dp else 14.dp
        val songLineFontSize = if (watchSClass) 10.sp else 12.sp
        val songIndexFontSize = if (watchSClass) 8.sp else 9.sp
        val editBpmTopPadding = if (watchSClass) 40.dp else 46.dp
        val editBpmWidth = if (watchSClass) 156.dp else 180.dp
        val editBpmEndPadding = if (watchSClass) 8.dp else 12.dp
        val editButtonWidth = if (watchSClass) 38.dp else 42.dp
        val editButtonFontSize = if (watchSClass) 8.sp else 9.sp
        val bpmFontSize = if (watchSClass) 19.sp else 22.sp
        val dialTopPadding = if (watchSClass) 8.dp else 10.dp
        val dialSize = if (watchSClass) 108.dp else 124.dp
        val navButtonWidth = if (watchSClass) 32.dp else 38.dp
        val navButtonHeight = if (watchSClass) 54.dp else 64.dp
        val navButtonFontSize = if (watchSClass) 42.sp else 50.sp
        val rhythmBottomPadding = if (watchSClass) 38.dp else 44.dp
        val rhythmStartPadding = if (watchSClass) 8.dp else 14.dp
        val rhythmFontSize = if (watchSClass) 12.sp else 14.sp
        val keyBottomPadding = if (watchSClass) 35.dp else 40.dp
        val keyEndPadding = if (watchSClass) 8.dp else 12.dp
        val keyWidth = if (watchSClass) 50.dp else 58.dp
        val keyFontSize = if (watchSClass) 15.sp else 18.sp
        val timerBottomPadding = if (watchSClass) 38.dp else 43.dp
        val startButtonBottomPadding = if (watchSClass) 6.dp else 8.dp
        val startButtonWidth = if (watchSClass) 94.dp else 104.dp
        val startButtonHeight = if (watchSClass) 27.dp else 30.dp
        val startButtonFontSize = if (watchSClass) 13.sp else 15.sp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pageHorizontalPadding, vertical = pageVerticalPadding),
        ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerTopPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = playlist.name,
                fontSize = headerTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = songIndexFontSize)) {
                        append("${songIndex + 1} of ${playlist.songs.size}")
                    }
                    append(" - ")
                    append(song.name)
                },
                modifier = Modifier
                    .width(songLineWidth)
                    .padding(start = songLineStartPadding),
                fontSize = songLineFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = editBpmTopPadding, end = editBpmEndPadding)
                .width(editBpmWidth)
                .height(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(58.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassCommandButton(
                    text = appText.edit,
                    modifier = Modifier
                        .width(editButtonWidth)
                        .height(24.dp),
                    fontSize = editButtonFontSize,
                    onClick = onEditPlaylist,
                )
            }

            Text(
                text = "${song.bpm}",
                modifier = Modifier.align(Alignment.CenterEnd),
                fontSize = bpmFontSize,
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
                .padding(top = dialTopPadding)
                .size(dialSize),
        )

        PlaylistNavButton(
            isNext = false,
            width = navButtonWidth,
            height = navButtonHeight,
            fontSize = navButtonFontSize,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp),
            onClick = onPreviousSong,
        )

        PlaylistNavButton(
            isNext = true,
            width = navButtonWidth,
            height = navButtonHeight,
            fontSize = navButtonFontSize,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 0.dp),
            onClick = onNextSong,
        )

        Text(
            text = if (forceSimpleRhythm) "4/4 x1" else "${song.beatsPerMeasure}/4 x${song.subdivisionCount}",
            modifier = Modifier.align(Alignment.BottomStart)
                .padding(start = rhythmStartPadding, bottom = rhythmBottomPadding),
            fontSize = rhythmFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = song.musicalKey,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = keyEndPadding, bottom = keyBottomPadding)
                .width(keyWidth),
            fontSize = keyFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        RhythmElapsedTimer(
            isRunning = isRunning,
            playbackStartedAtMs = playbackStartedAtMs,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = timerBottomPadding),
        )

        GlassCommandButton(
            text = if (isRunning) appText.stopUpper else appText.startUpper,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = startButtonBottomPadding)
                .width(startButtonWidth)
                .height(startButtonHeight),
            fontSize = startButtonFontSize,
            selected = isRunning,
            prominent = true,
            onClick = onToggleRunning,
        )
        }
    }
}

@Composable
private fun PlaylistNavButton(
    isNext: Boolean,
    width: Dp = 38.dp,
    height: Dp = 64.dp,
    fontSize: TextUnit = 50.sp,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = primary.copy(alpha = 0.86f)
    val chevronColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = modifier
            .width(width)
            .height(height)
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
            fontSize = fontSize,
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
internal fun PlaylistEditorPopup(
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
    onSongBpmClick: () -> Unit,
    onRhythmPresetChoice: (Int, List<BeatAccentType>, Int) -> Unit,
    onSongKeyChange: (Int) -> Unit,
    onSongKeySet: (String) -> Unit,
    onSongNoteChange: (Int) -> Unit,
    onSongNoteEdit: (String) -> Unit,
    showRhythmEditor: Boolean = true,
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
            onSongBpmClick = onSongBpmClick,
            onRhythmPresetChoice = onRhythmPresetChoice,
            onSongKeyChange = onSongKeyChange,
            onSongKeySet = onSongKeySet,
            onSongNoteChange = onSongNoteChange,
            onSongNoteEdit = onSongNoteEdit,
            showRhythmEditor = showRhythmEditor,
            onEditRhythm = onEditRhythm,
            onDone = onDone,
        )
    }
}

@Composable
internal fun DismissibleEditorPopup(
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
    onSongBpmClick: () -> Unit,
    onRhythmPresetChoice: (Int, List<BeatAccentType>, Int) -> Unit,
    onSongKeyChange: (Int) -> Unit,
    onSongKeySet: (String) -> Unit,
    onSongNoteChange: (Int) -> Unit,
    onSongNoteEdit: (String) -> Unit,
    showRhythmEditor: Boolean = true,
    onEditRhythm: () -> Unit,
    onDone: () -> Unit,
) {
    val playlist = playlists[playlistIndex]
    val song = playlist.songs[songIndex]
    var textEditTarget by remember { mutableStateOf<PlaylistTextEditTarget?>(null) }
    var keyPickerOpen by remember { mutableStateOf(false) }
    val playlistEditorScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(playlistEditorScrollState)
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

            RhythmPresetButtons(
                beatsPerMeasure = song.beatsPerMeasure,
                beatAccentTypes = song.beatAccentTypes,
                buttonWidth = 42.dp,
                buttonHeight = 25.dp,
                onPresetChoice = onRhythmPresetChoice,
            )

            Spacer(modifier = Modifier.height(7.dp))

            EditValueRow(
                label = "BPM",
                value = "${song.bpm}",
                onDecrease = { onSongBpmChange(-1) },
                onIncrease = { onSongBpmChange(1) },
                onValueClick = onSongBpmClick,
            )

            EditValueRow(
                label = appText.key,
                value = song.musicalKey,
                onDecrease = { onSongKeyChange(-1) },
                onIncrease = { onSongKeyChange(1) },
                onValueClick = { keyPickerOpen = true },
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

                if (showRhythmEditor) {
                    SmallCommandButton(
                        text = appText.editRhythm,
                        modifier = Modifier
                            .width(92.dp)
                            .height(28.dp),
                        fontSize = 10.sp,
                        onClick = onEditRhythm,
                    )
                }
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

        SettingsScrollIndicator(
            scrollState = playlistEditorScrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .width(4.dp)
                .height(112.dp),
        )

        if (keyPickerOpen) {
            MusicalKeyPickerPopup(
                value = song.musicalKey,
                doneText = appText.done,
                onCancel = { keyPickerOpen = false },
                onCommit = { nextKey ->
                    onSongKeySet(nextKey)
                    keyPickerOpen = false
                },
            )
        }

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
internal fun MusicalKeyPickerPopup(
    value: String,
    doneText: String,
    onCancel: () -> Unit,
    onCommit: (String) -> Unit,
) {
    var root by remember(value) { mutableStateOf(value.toMusicalKeyRoot()) }
    var suffix by remember(value) { mutableStateOf(value.toMusicalKeyModeSuffix()) }
    val selectedKey = "$root$suffix"

    BackHandler(onBack = onCancel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-4).dp)
                .width(176.dp)
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Key",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            KeyPickerRow(
                label = "Root",
                value = root,
                onPrevious = { root = cycleOption(MusicalKeyRoots, root, -1) },
                onNext = { root = cycleOption(MusicalKeyRoots, root, 1) },
            )

            KeyPickerRow(
                label = "Mode",
                value = suffix.ifBlank { "maj" }.trim(),
                onPrevious = { suffix = cycleOption(MusicalKeyModeSuffixes, suffix, -1) },
                onNext = { suffix = cycleOption(MusicalKeyModeSuffixes, suffix, 1) },
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = selectedKey,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        PlaylistBottomActionButton(
            text = "Back",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = (-12).dp),
            mirrored = true,
            onClick = onCancel,
        )

        PlaylistBottomActionButton(
            text = doneText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = (-12).dp),
            onClick = { onCommit(selectedKey) },
        )
    }
}

@Composable
private fun KeyPickerRow(
    label: String,
    value: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Text(
        text = label,
        fontSize = 9.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
    )

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
            modifier = Modifier.width(88.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
private fun PlaylistBottomActionButton(
    text: String,
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    onClick: () -> Unit,
) {
    SmallCommandButton(
        text = text,
        modifier = modifier
            .padding(
                start = if (mirrored) 2.dp else 0.dp,
                end = if (mirrored) 0.dp else 2.dp,
                bottom = 16.dp,
            )
            .rotate(if (mirrored) 38f else -38f)
            .width(64.dp)
            .height(24.dp),
        fontSize = 7.sp,
        onClick = onClick,
    )
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
