package bpm.munkz.pulse_wear.os.bpm.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun BpmReaderPage(
    appText: AppText,
    audioAnalysisState: AudioAnalysisState,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
) {
    val bpmHistory = remember { mutableStateListOf<Int>() }
    LaunchedEffect(audioAnalysisState.detectedTempoBpm) {
        val bpm = audioAnalysisState.detectedTempoBpm ?: return@LaunchedEffect
        if (bpmHistory.firstOrNull() == bpm) return@LaunchedEffect
        bpmHistory.add(0, bpm)
        while (bpmHistory.size > 3) {
            bpmHistory.removeAt(bpmHistory.lastIndex)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val detectedBpm = audioAnalysisState.detectedTempoBpm
        val titleFontSize = if (watchSClass) 15.sp else 17.sp
        val bpmFontSize = if (watchSClass) 36.sp else 42.sp
        val labelFontSize = if (watchSClass) 10.sp else 11.sp
        val historyFontSize = if (watchSClass) 13.sp else 15.sp

        BpmHistoryStack(
            bpmHistory = bpmHistory,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = if (watchSClass) 0.dp else 2.dp)
                .width(if (watchSClass) 36.dp else 42.dp),
            fontSize = historyFontSize,
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "BPM Reader",
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 6.dp else 8.dp))

            Text(
                text = detectedBpm?.toString() ?: "--",
                fontSize = bpmFontSize,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Text(
                text = "BPM",
                fontSize = labelFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(if (watchSClass) 5.dp else 7.dp))

            if (!micPermissionGranted) {
                MicPermissionButton(appText = appText, onClick = onRequestMicPermission)
            } else {
                Text(
                    text = if (audioAnalysisState.tempoConfident) {
                        "Reading ${audioAnalysisState.tempoMeter}/4"
                    } else {
                        "Learning ${audioAnalysisState.tempoLearningBeats}/${audioAnalysisState.tempoMeter}"
                    },
                    fontSize = labelFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                Text(
                    text = "Smart ${(audioAnalysisState.smartTempoConfidence * 100).toInt()}%",
                    fontSize = if (watchSClass) 8.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.46f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                Spacer(modifier = Modifier.height(4.dp))

                BpmBeatDots(
                    learnedBeats = audioAnalysisState.tempoLearningBeats,
                    activeBeatIndex = audioAnalysisState.tempoActiveBeatIndex,
                    confident = audioAnalysisState.tempoConfident,
                    meter = audioAnalysisState.tempoMeter,
                    dotSize = if (watchSClass) 10.dp else 12.dp,
                )
            }
        }
    }
}

@Composable
private fun BpmHistoryStack(
    bpmHistory: List<Int>,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    val colors = listOf(
        Color.White.copy(alpha = 0.46f),
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.18f),
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        bpmHistory.forEachIndexed { index, bpm ->
            Text(
                text = bpm.toString(),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = colors.getOrElse(index) { colors.last() },
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BpmBeatDots(
    learnedBeats: Int,
    activeBeatIndex: Int,
    confident: Boolean,
    meter: Int,
    dotSize: androidx.compose.ui.unit.Dp,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(meter.coerceIn(3, 4)) { index ->
            val learned = index < learnedBeats
            val active = confident && index == activeBeatIndex
            Canvas(modifier = Modifier.size(dotSize)) {
                val radius = size.minDimension / 2f
                drawCircle(
                    color = when {
                        active -> primaryColor
                        learned -> primaryColor.copy(alpha = 0.56f)
                        else -> Color.White.copy(alpha = 0.18f)
                    },
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
                if (active) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.28f),
                        radius = radius * 1.55f,
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx()),
                    )
                }
            }
        }
    }
}
