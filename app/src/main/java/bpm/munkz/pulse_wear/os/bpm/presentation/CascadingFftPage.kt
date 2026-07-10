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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun CascadingFftPage(
    appText: AppText,
    title: String = "FFT Waterfall",
    audioAnalysisState: AudioAnalysisState,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
) {
    var durationIndex by rememberSaveable { mutableIntStateOf(1) }
    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    val duration = FftCascadeDurations[durationIndex.coerceIn(FftCascadeDurations.indices)]
    val range = FftCascadeRanges[rangeIndex.coerceIn(FftCascadeRanges.indices)]
    val frames = remember { mutableStateListOf<List<Float>>() }
    val maxFrames = FftCascadeDurations.maxOf { it.frameRows }
    val liveChord = audioAnalysisState.likelyChords.firstOrNull()
    val evidence = remember(audioAnalysisState.spectrum, liveChord, range) {
        audioAnalysisState.spectrum.fftChordEvidence(liveChord, range)
    }

    LaunchedEffect(audioAnalysisState.spectrum) {
        if (audioAnalysisState.spectrum.isNotEmpty()) {
            frames += audioAnalysisState.spectrum
            while (frames.size > maxFrames) {
                frames.removeAt(0)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val titleFont = if (watchSClass) 11.sp else 13.sp
        val labelFont = if (watchSClass) 6.sp else 7.sp
        val buttonFont = if (watchSClass) 7.sp else 8.sp
        val graphHeight = if (watchSClass) 112.dp else 132.dp

        Column(
            modifier = Modifier.fillMaxWidth(),
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

            Text(
                text = "${range.label} ${range.minHz.toInt()}-${range.maxHz.toInt()} Hz  ${duration.label}",
                fontSize = labelFont,
                color = Color.White.copy(alpha = 0.66f),
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
                        .fillMaxWidth(0.92f)
                        .height(graphHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f), RoundedCornerShape(6.dp)),
                ) {
                    CascadingFftGraph(
                        frames = frames.takeLast(duration.frameRows),
                        range = range,
                        evidence = evidence,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TunerProfileButton(
                    text = "Dur ${duration.label}",
                    selected = true,
                    onClick = { durationIndex = (durationIndex + 1).mod(FftCascadeDurations.size) },
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    fontSize = buttonFont,
                )
                TunerProfileButton(
                    text = range.label,
                    selected = true,
                    onClick = { rangeIndex = (rangeIndex + 1).mod(FftCascadeRanges.size) },
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    fontSize = buttonFont,
                )
                TunerProfileButton(
                    text = "Clear",
                    selected = false,
                    onClick = { frames.clear() },
                    modifier = Modifier
                        .weight(0.72f)
                        .height(24.dp),
                    fontSize = buttonFont,
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            val peak = audioAnalysisState.spectrum.peakSpectrumReading()
            Text(
                text = "Peak ${peak?.frequencyHz?.roundToInt() ?: "--"} Hz  Key ${audioAnalysisState.guessedKey ?: "--"}  Chord ${liveChord ?: "--"}",
                fontSize = labelFont,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = evidence?.chromaText ?: "Chroma --",
                fontSize = labelFont,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = evidence?.thirdText ?: "Root --  Maj3 --  Min3 --",
                fontSize = labelFont,
                color = evidence?.leanColor ?: Color.White.copy(alpha = 0.64f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CascadingFftGraph(
    frames: List<List<Float>>,
    range: FftCascadeRange,
    evidence: FftChordEvidence?,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        drawRect(Color.Black)
        if (frames.isEmpty()) return@Canvas

        val columns = 44
        val rowHeight = size.height / frames.size.coerceAtLeast(1)
        val columnWidth = size.width / columns
        frames.forEachIndexed { rowIndex, frame ->
            val y = size.height - (rowIndex + 1) * rowHeight
            for (column in 0 until columns) {
                val hz = range.frequencyAt(column, columns)
                val level = frame.levelAtFrequency(hz)
                val color = fftHeatColor(level)
                drawRect(
                    color = color,
                    topLeft = Offset(column * columnWidth, y),
                    size = Size(columnWidth + 0.7f, rowHeight + 0.7f),
                )
            }
        }

        FftCascadeMarkers.forEach { markerHz ->
            if (markerHz in range.minHz..range.maxHz) {
                val x = range.progressFor(markerHz) * size.width
                drawLine(
                    color = primary.copy(alpha = if (markerHz == 440f) 0.86f else 0.44f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (markerHz == 440f) 1.8f else 1.1f,
                )
            }
        }

        evidence?.markers.orEmpty().forEach { marker ->
            marker.frequencies.forEach { frequency ->
                if (frequency in range.minHz..range.maxHz) {
                    val x = range.progressFor(frequency) * size.width
                    drawLine(
                        color = marker.color.copy(alpha = (0.24f + marker.level * 0.64f).coerceIn(0.24f, 0.9f)),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = if (marker.isRoot) 2.4f else 1.35f,
                    )
                }
            }
        }

        repeat(4) { index ->
            val y = size.height * (index + 1) / 5f
            drawLine(
                color = Color.White.copy(alpha = 0.13f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.8f,
            )
        }

        drawRect(
            color = primary.copy(alpha = 0.34f),
            style = Stroke(width = 1.2f),
        )
    }
}

private data class FftCascadeDuration(
    val label: String,
    val frameRows: Int,
)

private data class FftCascadeRange(
    val label: String,
    val minHz: Float,
    val maxHz: Float,
) {
    fun frequencyAt(index: Int, count: Int): Float {
        val progress = if (count <= 1) 0f else index.toFloat() / (count - 1)
        val minLog = log10(minHz)
        val maxLog = log10(maxHz)
        return 10f.pow(minLog + (maxLog - minLog) * progress)
    }

    fun progressFor(frequencyHz: Float): Float {
        val minLog = log10(minHz)
        val maxLog = log10(maxHz)
        return ((log10(frequencyHz) - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
    }
}

private val FftCascadeDurations = listOf(
    FftCascadeDuration("4s", 24),
    FftCascadeDuration("8s", 48),
    FftCascadeDuration("16s", 84),
    FftCascadeDuration("32s", 128),
)

private val FftCascadeRanges = listOf(
    FftCascadeRange("Full", 30f, 10_000f),
    FftCascadeRange("Bass root", 30f, 420f),
    FftCascadeRange("Gtr chord", 70f, 2_500f),
    FftCascadeRange("Thirds", 90f, 1_600f),
    FftCascadeRange("Full song", 45f, 6_000f),
    FftCascadeRange("High", 800f, 10_000f),
)

private val FftCascadeMarkers = listOf(110f, 220f, 440f, 880f, 1_760f, 3_520f)

private fun List<Float>.levelAtFrequency(frequencyHz: Float): Float {
    if (isEmpty()) return 0f
    val minLog = log10(30f)
    val maxLog = log10(10_000f)
    val progress = ((log10(frequencyHz.coerceIn(30f, 10_000f)) - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
    val position = progress * (size - 1)
    val low = position.toInt().coerceIn(indices)
    val high = (low + 1).coerceIn(indices)
    val blend = position - low
    return (this[low] * (1f - blend) + this[high] * blend)
        .coerceIn(0f, 1f)
}

private fun fftHeatColor(level: Float): Color {
    val x = level.coerceIn(0f, 1f)
    return when {
        x < 0.18f -> Color(0xFF090014).copy(alpha = 0.78f + x)
        x < 0.38f -> Color(
            red = 0.10f + x * 0.5f,
            green = 0.02f,
            blue = 0.34f + x * 0.9f,
            alpha = 1f,
        )
        x < 0.68f -> Color(
            red = 0.62f + x * 0.42f,
            green = 0.04f + x * 0.18f,
            blue = 0.82f,
            alpha = 1f,
        )
        else -> Color(
            red = 0.68f + x * 0.28f,
            green = 0.86f + x * 0.12f,
            blue = 0.08f,
            alpha = 1f,
        )
    }
}

private data class FftChordEvidence(
    val chromaText: String,
    val thirdText: String,
    val leanColor: Color,
    val markers: List<FftEvidenceMarker>,
)

private data class FftEvidenceMarker(
    val label: String,
    val frequencies: List<Float>,
    val level: Float,
    val color: Color,
    val isRoot: Boolean = false,
)

private fun List<Float>.fftChordEvidence(
    chordLabel: String?,
    range: FftCascadeRange,
): FftChordEvidence? {
    if (isEmpty()) return null
    val rootIndex = chordLabel?.rootNoteClass()?.noteClassIndex()
        ?: strongestNoteClass(range)?.first
        ?: return null
    val root = FftNoteClasses[rootIndex]
    val rootLevel = noteClassLevel(rootIndex, range)
    val majorThirdIndex = (rootIndex + 4).mod(FftNoteClasses.size)
    val minorThirdIndex = (rootIndex + 3).mod(FftNoteClasses.size)
    val fifthIndex = (rootIndex + 7).mod(FftNoteClasses.size)
    val flatSevenIndex = (rootIndex + 10).mod(FftNoteClasses.size)
    val majorSevenIndex = (rootIndex + 11).mod(FftNoteClasses.size)
    val majorThird = noteClassLevel(majorThirdIndex, range)
    val minorThird = noteClassLevel(minorThirdIndex, range)
    val fifth = noteClassLevel(fifthIndex, range)
    val flatSeven = noteClassLevel(flatSevenIndex, range)
    val majorSeven = noteClassLevel(majorSevenIndex, range)
    val chroma = noteClassLevels(range)
        .sortedByDescending { it.second }
        .take(5)
    val maxChroma = chroma.maxOfOrNull { it.second }?.coerceAtLeast(0.0001f) ?: 1f
    val chromaText = "Chroma " + chroma.joinToString(" ") { (index, level) ->
        "${FftNoteClasses[index]} ${(level / maxChroma * 100f).roundToInt()}%"
    }
    val thirdState = when {
        majorThird < 0.18f && minorThird < 0.18f -> "Third unclear"
        minorThird > majorThird * 1.12f -> "Minor leaning"
        majorThird > minorThird * 1.12f -> "Major leaning"
        else -> "Third split"
    }
    val leanColor = when (thirdState) {
        "Minor leaning" -> Color(0xFF7DD3FC)
        "Major leaning" -> Color(0xFFFDE047)
        "Third unclear" -> Color.White.copy(alpha = 0.62f)
        else -> Color(0xFFC4B5FD)
    }
    val thirdText = "Root $root ${(rootLevel * 100f).roundToInt()}%  " +
        "Maj3 ${(majorThird * 100f).roundToInt()}% / Min3 ${(minorThird * 100f).roundToInt()}%  $thirdState"
    val markers = listOf(
        FftEvidenceMarker("R", noteClassFrequencies(rootIndex, range), rootLevel, Color(0xFFB6FF3B), isRoot = true),
        FftEvidenceMarker("m3", noteClassFrequencies(minorThirdIndex, range), minorThird, Color(0xFF7DD3FC)),
        FftEvidenceMarker("M3", noteClassFrequencies(majorThirdIndex, range), majorThird, Color(0xFFFDE047)),
        FftEvidenceMarker("5", noteClassFrequencies(fifthIndex, range), fifth, Color(0xFFFFFFFF)),
        FftEvidenceMarker("b7", noteClassFrequencies(flatSevenIndex, range), flatSeven, Color(0xFFF97316)),
        FftEvidenceMarker("7", noteClassFrequencies(majorSevenIndex, range), majorSeven, Color(0xFFE879F9)),
    )
    return FftChordEvidence(
        chromaText = chromaText,
        thirdText = thirdText,
        leanColor = leanColor,
        markers = markers,
    )
}

private fun List<Float>.strongestNoteClass(range: FftCascadeRange): Pair<Int, Float>? {
    return noteClassLevels(range)
        .maxByOrNull { it.second }
        ?.takeIf { it.second > 0.01f }
}

private fun List<Float>.noteClassLevels(range: FftCascadeRange): List<Pair<Int, Float>> {
    return FftNoteClasses.indices.map { index -> index to noteClassLevel(index, range) }
}

private fun List<Float>.noteClassLevel(
    noteClassIndex: Int,
    range: FftCascadeRange,
): Float {
    return noteClassFrequencies(noteClassIndex, range)
        .maxOfOrNull { frequency -> levelAtFrequency(frequency) }
        ?: 0f
}

private fun noteClassFrequencies(
    noteClassIndex: Int,
    range: FftCascadeRange,
): List<Float> {
    return (12..120)
        .filter { midi -> midi.mod(12) == noteClassIndex }
        .map { midi -> 440f * 2f.pow((midi - 69) / 12f) }
        .filter { frequency -> frequency in range.minHz..range.maxHz }
}

private fun String.rootNoteClass(): String? {
    val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
    return root.takeIf { it in FftNoteClasses }
}

private fun String.noteClassIndex(): Int? {
    return FftNoteClasses.indexOf(this).takeIf { it >= 0 }
}

private val FftNoteClasses = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
