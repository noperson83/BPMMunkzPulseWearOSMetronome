package bpm.munkz.pulse_wear.os.bpm.presentation

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Popup
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
    selectedReaderMode: SpectrumReaderMode,
    selectedTuningChoice: SpectrumTuningChoice?,
    micPermissionGranted: Boolean,
    showSaveToClock: Boolean = true,
    onSpectrumSettingsSaved: (SpectrumReaderMode, TunerListenProfile, SpectrumTuningChoice?) -> Unit,
    onSaveToClock: (Int, String) -> Unit,
    onOpenFft: () -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    val peakReading = remember(audioAnalysisState.spectrum) {
        audioAnalysisState.spectrum.peakSpectrumReading()
    }
    val detectedBpm = audioAnalysisState.detectedTempoBpm
    val guessedKey = audioAnalysisState.guessedKey
    var settingsPopupOpen by rememberSaveable { mutableStateOf(false) }
    var draftReaderMode by rememberSaveable { mutableStateOf(selectedReaderMode) }
    var draftProfile by rememberSaveable { mutableStateOf(selectedProfile) }
    var draftTuningChoice by rememberSaveable { mutableStateOf(selectedTuningChoice) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = (-10).dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val titleFontSize = if (watchSClass) 14.sp else 16.sp
        val keyFontSize = if (watchSClass) 9.sp else 10.sp
        val graphWidth = if (watchSClass) 160.dp else 176.dp
        val graphHeight = if (watchSClass) 96.dp else 110.dp
        val editButtonWidth = if (watchSClass) 46.dp else 54.dp
        val editButtonHeight = if (watchSClass) 21.dp else 23.dp
        val editButtonFontSize = if (watchSClass) 8.sp else 9.sp

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

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AudioTempoReadout(
                    detectedBpm = detectedBpm,
                    modifier = Modifier.width(if (watchSClass) 54.dp else 62.dp),
                    fontSize = keyFontSize,
                    numberFontSize = keyFontSize,
                )

                Text(
                    text = ":",
                    modifier = Modifier.width(8.dp),
                    fontSize = keyFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                Text(
                    text = guessedKey?.let { "Key of $it" } ?: "Key of --",
                    fontSize = keyFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }

            if (!micPermissionGranted) {
                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            } else {
                SpectrumAnalyzerGraph(
                    spectrum = audioAnalysisState.spectrum,
                    peakReading = peakReading,
                    readerMode = selectedReaderMode,
                    restrictionProfile = selectedProfile,
                    tuningChoice = selectedTuningChoice,
                    modifier = Modifier
                        .width(graphWidth)
                        .height(graphHeight),
                )
            }
        }

        TunerProfileButton(
            text = appText.edit,
            selected = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .width(editButtonWidth)
                .height(editButtonHeight),
            fontSize = editButtonFontSize,
            onClick = {
                draftReaderMode = selectedReaderMode
                draftProfile = selectedProfile
                draftTuningChoice = selectedTuningChoice
                settingsPopupOpen = true
            },
        )

        TunerProfileButton(
            text = "FFT",
            selected = false,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 4.dp)
                .width(editButtonWidth)
                .height(editButtonHeight),
            fontSize = editButtonFontSize,
            onClick = onOpenFft,
        )

        if (showSaveToClock && detectedBpm != null && guessedKey != null) {
            ArchedTopStartButton(
                text = appText.toClock,
                modifier = Modifier.align(Alignment.TopStart),
                onClick = { onSaveToClock(detectedBpm, guessedKey) },
            )
        }

        if (settingsPopupOpen) {
            SpectrumSettingsPopup(
                appText = appText,
                appLanguage = appLanguage,
                selectedReaderMode = draftReaderMode,
                selectedProfile = draftProfile,
                selectedTuningChoice = draftTuningChoice,
                onReaderModeChoice = { draftReaderMode = it },
                onProfileChoice = { profile ->
                    draftProfile = profile
                    draftTuningChoice = draftTuningChoice
                        ?.takeIf { it.profile == profile }
                        ?: profile.defaultSpectrumTuningChoice()
                },
                onTuningChoice = { draftTuningChoice = it },
                onCancel = { settingsPopupOpen = false },
                onDone = {
                    onSpectrumSettingsSaved(draftReaderMode, draftProfile, draftTuningChoice)
                    settingsPopupOpen = false
                },
            )
        }
    }
}

@Composable
private fun SpectrumSettingsPopup(
    appText: AppText,
    appLanguage: AppLanguage,
    selectedReaderMode: SpectrumReaderMode,
    selectedProfile: TunerListenProfile,
    selectedTuningChoice: SpectrumTuningChoice?,
    onReaderModeChoice: (SpectrumReaderMode) -> Unit,
    onProfileChoice: (TunerListenProfile) -> Unit,
    onTuningChoice: (SpectrumTuningChoice?) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Popup {
        BackHandler(onBack = onCancel)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = {}),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = appText.spectrum,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TunerProfileButton(
                            text = "Instrument",
                            selected = selectedReaderMode == SpectrumReaderMode.Instrument,
                            modifier = Modifier
                                .width(82.dp)
                                .height(24.dp),
                            fontSize = 8.sp,
                            onClick = { onReaderModeChoice(SpectrumReaderMode.Instrument) },
                        )

                        TunerProfileButton(
                            text = "Song",
                            selected = selectedReaderMode == SpectrumReaderMode.Song,
                            modifier = Modifier
                                .width(54.dp)
                                .height(24.dp),
                            fontSize = 9.sp,
                            onClick = { onReaderModeChoice(SpectrumReaderMode.Song) },
                        )
                    }

                    SpectrumTunerListenProfiles.chunked(3).forEach { rowProfiles ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowProfiles.forEach { profile ->
                                TunerProfileButton(
                                    text = profile.spectrumPopupLabel(),
                                    selected = selectedProfile == profile,
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(23.dp),
                                    fontSize = 7.sp,
                                    onClick = { onProfileChoice(profile) },
                                )
                            }
                        }
                    }

                    if (selectedReaderMode == SpectrumReaderMode.Instrument) {
                        val tuningChoices = spectrumTuningChoicesFor(selectedProfile)
                        if (tuningChoices.isNotEmpty()) {
                            Text(
                                text = "Tuning",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )

                            tuningChoices.chunked(3).forEach { rowChoices ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    rowChoices.forEach { tuningChoice ->
                                        TunerProfileButton(
                                            text = tuningChoice.label,
                                            selected = selectedTuningChoice == tuningChoice,
                                            modifier = Modifier
                                                .width(48.dp)
                                                .height(22.dp),
                                            fontSize = 7.sp,
                                            onClick = { onTuningChoice(tuningChoice) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ArchedGlassDoneButton(
                    text = "Save",
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = onDone,
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
    readerMode: SpectrumReaderMode,
    restrictionProfile: TunerListenProfile,
    tuningChoice: SpectrumTuningChoice?,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val axisColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.52f)
    val spectrumBands = remember { spectrumBands() }
    val listenRange = remember(readerMode, restrictionProfile, tuningChoice) {
        restrictionProfile.audioListenRangeFor(readerMode, tuningChoice)
    }
    val targetFrequencies = remember(readerMode, restrictionProfile, tuningChoice) {
        restrictionProfile.spectrumTargetsFor(readerMode, tuningChoice)
    }
    Canvas(modifier = modifier) {
        val chartLeft = 24.dp.toPx()
        val chartTop = 6.dp.toPx()
        val chartRight = size.width - 2.dp.toPx()
        val chartBottom = size.height - 28.dp.toPx()
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

        val restrictionStartX = frequencyToX(listenRange.minHz)
        val restrictionEndX = frequencyToX(listenRange.maxHz)
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
        val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor.copy(alpha = 0.9f).toArgb()
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
            listenRange.minHz to restrictionStartX,
            listenRange.maxHz to restrictionEndX,
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

        targetFrequencies.forEach { target ->
            val x = frequencyToX(target.frequencyHz)
            drawLine(
                color = primaryColor.copy(alpha = 0.7f),
                start = Offset(x, chartTop),
                end = Offset(x, chartBottom),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawContext.canvas.nativeCanvas.drawText(
                target.label,
                x,
                chartBottom + 21.dp.toPx(),
                targetPaint,
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

private fun TunerListenProfile.spectrumPopupLabel(): String {
    return when (this) {
        TunerListenProfile.Full -> "Full"
        TunerListenProfile.Bass -> "Bass"
        TunerListenProfile.Guitar -> "Guitar"
        TunerListenProfile.Voice -> "Voice"
        TunerListenProfile.Violin -> "Violin"
        TunerListenProfile.Trumpet -> "Trumpet"
        TunerListenProfile.High -> "High"
    }
}
