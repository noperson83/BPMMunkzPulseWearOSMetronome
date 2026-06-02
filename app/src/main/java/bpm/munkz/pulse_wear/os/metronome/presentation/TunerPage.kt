package bpm.munkz.pulse_wear.os.metronome.presentation

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TunerPage(
    appText: AppText,
    appLanguage: AppLanguage,
    audioAnalysisState: AudioAnalysisState,
    selectedProfile: TunerListenProfile,
    micPermissionGranted: Boolean,
    onProfileChoice: (TunerListenProfile) -> Unit,
    onSaveKey: (String) -> Unit,
    onSaveBpm: (Int) -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    val guessedKey = audioAnalysisState.guessedKey
    val likelyChords = audioAnalysisState.likelyChords
    val detectedBpm = audioAnalysisState.detectedTempoBpm
    var keySaveRoot by rememberSaveable { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val titleFontSize = if (watchSClass) 13.sp else 14.sp
        val noteFontSize = if (watchSClass) 30.sp else 34.sp
        val frequencyFontSize = if (watchSClass) 12.sp else 13.sp
        val centsFontSize = if (watchSClass) 10.sp else 11.sp
        val recentNotesFontSize = if (watchSClass) 9.sp else 10.sp
        val centerWidth = if (watchSClass) 112.dp else 126.dp
        val profileButtonWidth = if (watchSClass) 32.dp else 36.dp
        val profileButtonHeight = if (watchSClass) 21.dp else 23.dp
        val profileButtonFontSize = if (watchSClass) 7.sp else 8.sp
        val profileSpacing = if (watchSClass) 3.dp else 4.dp
        val tempoTopPadding = if (watchSClass) 9.dp else 12.dp
        val tempoStartPadding = if (watchSClass) 12.dp else 18.dp
        val keyColumnWidth = if (watchSClass) 42.dp else 48.dp
        val keyGuessLabelFontSize = if (watchSClass) 8.sp else 9.sp
        val keyGuessFontSize = if (watchSClass) 13.sp else 15.sp
        val chordFontSize = if (watchSClass) 8.sp else 9.sp

        if (!micPermissionGranted) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = appText.tuner,
                    fontSize = if (watchSClass) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            }
            return@BoxWithConstraints
        }

        TunerTempoReadout(
            detectedBpm = detectedBpm,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = tempoStartPadding, top = tempoTopPadding)
                .width(52.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 0.dp),
            verticalArrangement = Arrangement.spacedBy(profileSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VisibleTunerListenProfiles.forEach { profile ->
                val isSelectedProfile = selectedProfile == profile
                TunerProfileButton(
                    text = profile.labelFor(appLanguage),
                    selected = isSelectedProfile,
                    modifier = Modifier
                        .width(profileButtonWidth)
                        .height(profileButtonHeight),
                    fontSize = profileButtonFontSize,
                    onClick = {
                        onProfileChoice(
                            if (isSelectedProfile) TunerListenProfile.Full else profile,
                        )
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(centerWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = appText.tuner,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = audioAnalysisState.noteName,
                fontSize = noteFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = audioAnalysisState.frequencyHz?.let { "${it.roundToInt()} Hz" } ?: "-- Hz",
                fontSize = frequencyFontSize,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(2.dp))
            TunerNeedle(cents = audioAnalysisState.cents)
            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${audioAnalysisState.cents.coerceIn(-99, 99)} cents",
                fontSize = centsFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
            )

            val recentNotesText = audioAnalysisState.recentNotes.takeLast(8).joinToString(" ")
            Text(
                text = recentNotesText.ifBlank { "--" },
                fontSize = recentNotesFontSize,
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
                .width(keyColumnWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${appText.keyGuess}:",
                fontSize = keyGuessLabelFontSize,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Text(
                text = guessedKey ?: "--",
                fontSize = keyGuessFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(2.dp))

            likelyChords.take(3).forEach { chord ->
                Text(
                    text = chord,
                    fontSize = chordFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        if (detectedBpm != null) {
            TunerBottomActionButton(
                text = appText.saveBpm,
                modifier = Modifier.align(Alignment.BottomStart),
                mirrored = true,
                onClick = { onSaveBpm(detectedBpm) },
            )
        }

        if (guessedKey != null) {
            TunerBottomActionButton(
                text = appText.saveKey,
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = { keySaveRoot = guessedKey.toMusicalKeyRoot() },
            )
        }

        keySaveRoot?.let { root ->
            TunerKeySavePopup(
                root = root,
                doneText = appText.done,
                onCancel = { keySaveRoot = null },
                onSaveKey = { key ->
                    onSaveKey(key)
                    keySaveRoot = null
                },
            )
        }
    }
}

@Composable
private fun TunerKeySavePopup(
    root: String,
    doneText: String,
    onCancel: () -> Unit,
    onSaveKey: (String) -> Unit,
) {
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
                .width(176.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Save as",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            MusicalKeyModeSuffixes.chunked(2).forEach { rowModes ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowModes.forEach { suffix ->
                        TunerCommandButton(
                            text = "$root$suffix",
                            modifier = Modifier
                                .width(78.dp)
                                .height(28.dp),
                            fontSize = 10.sp,
                            onClick = { onSaveKey("$root$suffix") },
                        )
                    }
                }
            }

            TunerCommandButton(
                text = doneText,
                modifier = Modifier
                    .width(74.dp)
                    .height(28.dp),
                fontSize = 10.sp,
                onClick = onCancel,
            )
        }
    }
}

@Composable
private fun TunerTempoReadout(
    detectedBpm: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = detectedBpm?.toString() ?: "--",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )

        Text(
            text = "BPM",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun TunerBottomActionButton(
    text: String,
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    onClick: () -> Unit,
) {
    TunerCommandButton(
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
private fun TunerProfileButton(
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
private fun TunerCommandButton(
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
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
