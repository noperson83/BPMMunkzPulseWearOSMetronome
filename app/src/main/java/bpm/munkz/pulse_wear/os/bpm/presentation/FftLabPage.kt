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
        val titleFont = if (watchSClass) 14.sp else 16.sp
        val labelFont = if (watchSClass) 6.sp else 7.sp
        val valueFont = if (watchSClass) 9.sp else 10.sp
        val graphHeight = if (watchSClass) 68.dp else 80.dp
        val range = selectedProfile.audioListenRangeFor(selectedReaderMode)
        val likelyChords = audioAnalysisState.likelyChords.take(3)
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
                .padding(top = if (watchSClass) 6.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "FFT Lab",
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
                FftBarsGraph(
                    spectrum = audioAnalysisState.spectrum,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .height(graphHeight),
                )
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
                    label = "Smart",
                    value = "${(audioAnalysisState.smartTempoConfidence * 100f).roundToInt()}%",
                    labelFont = labelFont,
                    valueFont = valueFont,
                    selected = audioAnalysisState.tempoConfident,
                )
                FftReadoutChip(
                    label = "Meter",
                    value = "${audioAnalysisState.tempoMeter}/4",
                    labelFont = labelFont,
                    valueFont = valueFont,
                    selected = audioAnalysisState.tempoConfident,
                )
                FftReadoutChip(
                    label = "Key",
                    value = audioAnalysisState.guessedKey ?: "--",
                    labelFont = labelFont,
                    valueFont = valueFont,
                    selected = audioAnalysisState.guessedKey != null,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (likelyChords.isEmpty()) "Chords --" else "Chords ${likelyChords.joinToString("  ")}",
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
            .height(27.dp)
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
