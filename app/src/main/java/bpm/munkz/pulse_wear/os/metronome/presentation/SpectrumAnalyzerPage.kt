package bpm.munkz.pulse_wear.os.metronome.presentation

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun SpectrumAnalyzerPage(
    appText: AppText,
    appLanguage: AppLanguage,
    audioAnalysisState: AudioAnalysisState,
    a4ReferenceHz: Int,
    selectedProfile: TunerListenProfile,
    micPermissionGranted: Boolean,
    onSaveToClock: (Int, String) -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    val peakReading = remember(audioAnalysisState.spectrum) {
        audioAnalysisState.spectrum.peakSpectrumReading()
    }
    val detectedBpm = audioAnalysisState.detectedTempoBpm
    val guessedKey = audioAnalysisState.guessedKey

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val titleFontSize = if (watchSClass) 14.sp else 16.sp
        val keyFontSize = if (watchSClass) 9.sp else 10.sp
        val rangeFontSize = if (watchSClass) 7.sp else 8.sp
        val graphWidth = if (watchSClass) 160.dp else 176.dp
        val graphHeight = if (watchSClass) 96.dp else 110.dp
        val peakFontSize = if (watchSClass) 10.sp else 12.sp
        val graphBottomSpacing = if (watchSClass) 3.dp else 4.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = appText.spectrum,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(2.dp))

            AudioTempoReadout(
                detectedBpm = detectedBpm,
                modifier = Modifier.width(60.dp),
            )

            Text(
                text = guessedKey?.let { "Key of $it" } ?: "Key of --",
                fontSize = keyFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Text(
                text = "${selectedProfile.constraintLabelFor(appLanguage)} ${selectedProfile.frequencyRangeLabel()}",
                fontSize = rangeFontSize,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            if (!micPermissionGranted) {
                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            } else {
                SpectrumAnalyzerGraph(
                    spectrum = audioAnalysisState.spectrum,
                    peakReading = peakReading,
                    restrictionProfile = selectedProfile,
                    modifier = Modifier
                        .width(graphWidth)
                        .height(graphHeight),
                )

                Spacer(modifier = Modifier.height(graphBottomSpacing))

                Text(
                    text = peakReading?.let { peak ->
                        val noteName = peak.frequencyHz.toNoteReading(a4ReferenceHz).first
                        "${peak.frequencyHz.roundToInt()} Hz  $noteName  ${peak.bandLabel}"
                    } ?: "-- Hz",
                    fontSize = peakFontSize,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (detectedBpm != null && guessedKey != null) {
            ArchedTopStartButton(
                text = appText.toClock,
                modifier = Modifier.align(Alignment.TopStart),
                onClick = { onSaveToClock(detectedBpm, guessedKey) },
            )
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
fun MicPermissionButton(
    appText: AppText,
    onClick: () -> Unit,
) {
    GlassCommandButton(
        text = appText.micAccess,
        modifier = Modifier
            .width(104.dp)
            .height(34.dp),
        fontSize = 11.sp,
        prominent = true,
        onClick = onClick,
    )
}

@Composable
private fun ArchedTopStartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(top = 10.dp, start = 10.dp)
            .rotate(-38f)
            .width(58.dp)
            .height(24.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpectrumAnalyzerGraph(
    spectrum: List<Float>,
    peakReading: SpectrumPeak?,
    restrictionProfile: TunerListenProfile,
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

        val restrictionStartX = frequencyToX(restrictionProfile.minHz)
        val restrictionEndX = frequencyToX(restrictionProfile.maxHz)
        drawRect(
            color = secondaryColor.copy(alpha = 0.07f),
            topLeft = Offset(restrictionStartX, chartTop),
            size = androidx.compose.ui.geometry.Size(
                (restrictionEndX - restrictionStartX).coerceAtLeast(1f),
                chartHeight,
            ),
        )

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
        val restrictionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = secondaryColor.copy(alpha = 0.88f).toArgb()
            textSize = 6.sp.toPx()
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

        listOf(
            restrictionProfile.minHz to restrictionStartX,
            restrictionProfile.maxHz to restrictionEndX,
        ).forEach { (frequency, x) ->
            drawLine(
                color = secondaryColor.copy(alpha = 0.86f),
                start = Offset(x, chartTop),
                end = Offset(x, chartBottom),
                strokeWidth = 1.7.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawContext.canvas.nativeCanvas.drawText(
                frequency.roundToInt().toString(),
                x,
                chartTop + 7.dp.toPx(),
                restrictionPaint,
            )
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
