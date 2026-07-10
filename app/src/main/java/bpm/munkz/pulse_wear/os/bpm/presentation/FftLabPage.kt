package bpm.munkz.pulse_wear.os.bpm.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlin.math.roundToInt

@Composable
fun FftLabPage(
    appText: AppText,
    title: String = "FFT Lab",
    audioAnalysisState: AudioAnalysisState,
    selectedProfile: TunerListenProfile,
    selectedReaderMode: SpectrumReaderMode,
    a4ReferenceHz: Int,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val titleFont = if (watchSClass) 12.sp else 14.sp
        val labelFont = if (watchSClass) 6.sp else 7.sp
        val valueFont = if (watchSClass) 9.sp else 10.sp
        val graphHeight = if (watchSClass) 48.dp else 56.dp
        val range = selectedProfile.audioListenRangeFor(selectedReaderMode)
        val liveChord = audioAnalysisState.likelyChords.firstOrNull()
        val progressionChords = audioAnalysisState.chordProgression.take(16)
        val harmonyProgression = audioAnalysisState.harmonyProgression.take(progressionChords.size)
        val phraseState = audioAnalysisState.songPhraseState()
        val meterText = audioAnalysisState.tempoMeterLabel.ifBlank { "${audioAnalysisState.tempoMeter}/4" }
        val musicalTempoText = audioAnalysisState.musicalTempoBpm?.toString() ?: "--"
        val feelText = audioAnalysisState.tempoFeelLabel.ifBlank { "Raw" }
        val keyText = audioAnalysisState.guessedKey?.let { key ->
            if (audioAnalysisState.keyConfidence > 0f) {
                "$key ${(audioAnalysisState.keyConfidence * 100f).roundToInt()}%"
            } else {
                key
            }
        } ?: "--"
        val phraseText = phraseState.compactLabel()
        val sensedA4Text = audioAnalysisState.sensedA4Hz?.let { sensedA4Hz ->
            val direction = when {
                audioAnalysisState.sensedA4OffsetCents > 0 -> "+"
                else -> ""
            }
            "A4 set $a4ReferenceHz  song ${oneDecimal(sensedA4Hz)}  $direction${audioAnalysisState.sensedA4OffsetCents}c"
        } ?: "A4 set $a4ReferenceHz  song --"

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (watchSClass) 12.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                fontSize = titleFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${selectedReaderMode.label()} ${selectedProfile.label} ${range.minHz.toInt()}-${range.maxHz.toInt()} Hz",
                fontSize = labelFont,
                color = Color.White.copy(alpha = 0.62f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = sensedA4Text,
                fontSize = labelFont,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (audioAnalysisState.sensedA4Hz != null) 0.86f else 0.48f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(3.dp))

            if (!micPermissionGranted) {
                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(graphHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    FftBarsGraph(
                        spectrum = audioAnalysisState.spectrum,
                        modifier = Modifier.fillMaxSize(),
                    )
                    PhraseChordOverlay(
                        chords = progressionChords,
                        harmony = harmonyProgression,
                        keyName = audioAnalysisState.guessedKey,
                        phraseBarIndex = phraseState.currentBarIndex,
                        phraseLocked = phraseState.locked,
                        modifier = Modifier.fillMaxSize(),
                        labelFont = labelFont,
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TempoEngineChip("Strict", audioAnalysisState.strictTempoBpm, labelFont, valueFont)
                TempoEngineChip("Bass", audioAnalysisState.bassTempoBpm, labelFont, valueFont)
                TempoEngineChip("Snare", audioAnalysisState.snareTempoBpm, labelFont, valueFont)
                TempoEngineChip("Flux", audioAnalysisState.fluxTempoBpm, labelFont, valueFont)
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FftReadoutChip(
                    label = "Song",
                    value = musicalTempoText,
                    labelFont = labelFont,
                    valueFont = valueFont,
                    selected = audioAnalysisState.tempoConfident,
                )
                FftReadoutChip(
                    label = "Meter",
                    value = meterText,
                    labelFont = labelFont,
                    valueFont = valueFont,
                    selected = audioAnalysisState.tempoConfident,
                )
                FftReadoutChip(
                    label = "Smart",
                    value = "${(audioAnalysisState.smartTempoConfidence * 100f).roundToInt()}%",
                    labelFont = labelFont,
                    valueFont = valueFont,
                    selected = audioAnalysisState.tempoConfident,
                )
                FftReadoutChip(
                    label = "Key",
                    value = keyText,
                    labelFont = labelFont,
                    valueFont = valueFont,
                    selected = audioAnalysisState.keyConfidence >= 0.5f,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildAnnotatedString {
                    append(feelText)
                    append("  ")
                    append("Chord ")
                    if (liveChord == null) {
                        append("--")
                    } else {
                        pushStyle(SpanStyle(color = liveChord.chordEmotionColor()))
                        append(liveChord)
                        pop()
                        if (audioAnalysisState.chordConfidence > 0f) {
                            append(" ")
                            append("${(audioAnalysisState.chordConfidence * 100f).roundToInt()}%")
                        }
                        audioAnalysisState.alternateChord?.let { alternate ->
                            append("/")
                            pushStyle(SpanStyle(color = alternate.chordEmotionColor().copy(alpha = 0.68f)))
                            append(alternate)
                            pop()
                        }
                    }
                    if (progressionChords.isNotEmpty()) {
                        append("  Prog ")
                        append(phraseText)
                        append(" ")
                        progressionChords.forEachIndexed { index, chord ->
                            if (index > 0) append(" ")
                            pushStyle(SpanStyle(color = chord.chordEmotionColor()))
                            append(chord)
                            pop()
                        }
                    }
                },
                fontSize = labelFont,
                color = Color.White.copy(alpha = 0.74f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PhraseChordOverlay(
    chords: List<String>,
    harmony: List<String>,
    keyName: String?,
    phraseBarIndex: Int,
    phraseLocked: Boolean,
    modifier: Modifier = Modifier,
    labelFont: TextUnit,
) {
    if (chords.isEmpty()) return

    val displayedChords = chords.take(16)
    val rowChunks = displayedChords.chunked(4)
    val rows = rowChunks.size.coerceAtLeast(1)
    val rowHeight = when {
        rows >= 4 -> 13.dp
        rows == 3 -> 16.dp
        rows == 2 -> 20.dp
        else -> 26.dp
    }
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
    ) {
        rowChunks.forEachIndexed { rowIndex, rowChords ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                rowChords.forEachIndexed { index, chord ->
                    val absoluteIndex = rowIndex * 4 + index + 1
                    val isActive = phraseBarIndex == absoluteIndex
                    val isFirst = rowIndex == 0 && index == 0
                    PhraseChordCell(
                        chord = chord,
                        harmony = harmony.getOrNull(absoluteIndex - 1).orEmpty(),
                        keyName = keyName,
                        isFirst = isFirst,
                        isActive = isActive,
                        phraseLocked = phraseLocked,
                        labelFont = labelFont,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhraseChordCell(
    chord: String,
    harmony: String,
    keyName: String?,
    isFirst: Boolean,
    isActive: Boolean,
    phraseLocked: Boolean,
    labelFont: TextUnit,
    modifier: Modifier = Modifier,
) {
    val uncertain = chord.endsWith("?")
    val color = if (uncertain) Color.White.copy(alpha = 0.54f) else chord.chordEmotionColor()
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(color.copy(alpha = if (uncertain) 0.12f else 0.22f), shape)
            .border(
                width = when {
                    isActive -> 1.7.dp
                    isFirst -> 1.3.dp
                    else -> 0.8.dp
                },
                color = when {
                    isActive -> MaterialTheme.colorScheme.primary.copy(alpha = if (phraseLocked) 0.95f else 0.72f)
                    isFirst -> MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                    else -> color.copy(alpha = 0.42f)
                },
                shape = shape,
            )
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chord,
            fontSize = labelFont,
            lineHeight = labelFont,
            fontWeight = if (isFirst) FontWeight.Black else FontWeight.Bold,
            color = if (uncertain) Color.White.copy(alpha = 0.74f) else Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val function = harmony.ifBlank { functionalHarmonyRoman(keyName, chord) }
        if (function.isNotBlank() && !uncertain) {
            Text(
                text = function,
                modifier = Modifier.align(Alignment.BottomCenter),
                fontSize = 5.sp,
                lineHeight = 5.sp,
                color = Color.White.copy(alpha = 0.48f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TempoEngineChip(
    label: String,
    bpm: Int?,
    labelFont: TextUnit,
    valueFont: TextUnit,
) {
    FftReadoutChip(
        label = label,
        value = bpm?.toString() ?: "--",
        labelFont = labelFont,
        valueFont = valueFont,
        selected = bpm != null,
        wide = false,
    )
}

@Composable
private fun FftReadoutChip(
    label: String,
    value: String,
    labelFont: TextUnit,
    valueFont: TextUnit,
    selected: Boolean,
    wide: Boolean = true,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .width(if (wide) 38.dp else 34.dp)
            .height(24.dp)
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.05f),
                shape,
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.76f)
                else Color.White.copy(alpha = 0.16f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = label,
                fontSize = labelFont,
                lineHeight = labelFont,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                fontSize = valueFont,
                lineHeight = valueFont,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FftBarsGraph(
    spectrum: List<Float>,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val bars = spectrum.ifEmpty { List(SPECTRUM_BAR_COUNT) { 0f } }
        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (bars.size - 1)) / bars.size).coerceAtLeast(1f)
        val baseline = size.height - 4.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1.dp.toPx(),
        )
        bars.forEachIndexed { index, rawLevel ->
            val level = rawLevel.coerceIn(0f, 1f)
            val x = index * (barWidth + gap)
            val barHeight = (level * (size.height - 10.dp.toPx())).coerceAtLeast(2.dp.toPx())
            val top = baseline - barHeight
            val color = primary.copy(alpha = 0.24f + 0.7f * level)
            drawLine(
                color = color,
                start = Offset(x + barWidth / 2f, baseline),
                end = Offset(x + barWidth / 2f, top),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun SpectrumReaderMode.label(): String {
    return when (this) {
        SpectrumReaderMode.Instrument -> "Inst."
        SpectrumReaderMode.Song -> "Song"
    }
}

private fun oneDecimal(value: Float): String {
    val roundedTenths = (value * 10f).roundToInt()
    return "${roundedTenths / 10}.${kotlin.math.abs(roundedTenths % 10)}"
}

private fun String?.chordEmotionColor(): Color {
    val chord = this ?: return Color.White.copy(alpha = 0.74f)
    return when {
        chord.containsAlteredColor() -> Color(0xFFFF6B4A)
        chord.contains("aug") || chord.contains("b13") -> Color(0xFFFF5FD2)
        chord.contains("dim") || chord.contains("m7b5") -> Color(0xFFB79CFF)
        chord.contains("sus") -> Color(0xFF62E6FF)
        chord.containsDominantColor() -> Color(0xFFFFB84D)
        chord.containsMinorChord() -> Color(0xFF6EA8FF)
        else -> Color(0xFF9BE06D)
    }
}

private fun String.containsMinorChord(): Boolean {
    val rootLength = if (length >= 2 && (this[1] == '#' || this[1] == 'b')) 2 else 1
    return drop(rootLength).startsWith("m")
}

private fun String.containsDominantColor(): Boolean {
    return contains("7") || contains("9") || contains("13")
}

private fun String.containsAlteredColor(): Boolean {
    val rootLength = if (length >= 2 && (this[1] == '#' || this[1] == 'b')) 2 else 1
    val suffix = drop(rootLength)
    return suffix.contains("#") || suffix.contains("b9") || suffix.contains("b13") || suffix.contains("b5")
}
