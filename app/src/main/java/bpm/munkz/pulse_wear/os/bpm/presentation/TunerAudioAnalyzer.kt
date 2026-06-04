package bpm.munkz.pulse_wear.os.bpm.presentation

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val AUDIO_SAMPLE_RATE = 44_100
private const val AUDIO_FRAME_SIZE = 2_048
private const val AUDIO_TEMPO_LOG_TAG = "BPM_TEMPO"
private const val SMALL_WATCH_MIN_BUFFER_SIZE = 2_000
private const val SMALL_WATCH_PITCH_FRAME_STRIDE = 6
private const val AUDIO_UI_UPDATE_INTERVAL_MS = 140L
private const val TUNER_AVERAGE_SAMPLE_COUNT = 11
private const val TUNER_NOTE_HOLD_MS = 650L
private const val TUNER_NOTE_SWITCH_CONFIRMATION_COUNT = 3
private const val TUNER_NOTE_LOST_HOLD_MS = 1_200L
private const val TUNER_KEY_SWITCH_CONFIRMATION_COUNT = 5
private const val MIN_TEMPO_ONSET_INTERVAL_MS = 300L
private const val MAX_TEMPO_ONSET_INTERVAL_MS = 2_000L
private const val TEMPO_ONSET_HISTORY_COUNT = 24
private const val TEMPO_INTERVAL_WINDOW_COUNT = 11
private const val TEMPO_CLUSTER_TOLERANCE_BPM = 4
private const val TEMPO_CHANGE_CONFIRMATION_COUNT = 3
private const val TEMPO_DETECTION_TIMEOUT_MS = 3_000L
private const val TEMPO_DEBUG_LOG_INTERVAL_MS = 1_000L
private const val TEMPO_PROCESSING_LOG_INTERVAL_MS = 2_000L
private const val TUNER_KEY_SAMPLE_COUNT = 28

private data class PitchAnalysisState(
    val frequencyHz: Float? = null,
    val noteName: String = "--",
    val cents: Int = 0,
    val recentNotes: List<String> = emptyList(),
    val guessedKey: String? = null,
    val likelyChords: List<String> = emptyList(),
)

private fun AudioAnalysisState.toPitchAnalysisState(): PitchAnalysisState {
    return PitchAnalysisState(
        frequencyHz = frequencyHz,
        noteName = noteName,
        cents = cents,
        recentNotes = recentNotes,
        guessedKey = guessedKey,
        likelyChords = likelyChords,
    )
}

@Composable
fun rememberAudioAnalysisState(
    enabled: Boolean,
    listenProfile: TunerListenProfile,
    a4ReferenceHz: Int,
    includeSpectrum: Boolean,
): AudioAnalysisState {
    var analysisState by remember { mutableStateOf(AudioAnalysisState()) }

    LaunchedEffect(enabled, listenProfile, a4ReferenceHz, includeSpectrum) {
        if (!enabled) {
            analysisState = AudioAnalysisState()
            return@LaunchedEffect
        }

        runAudioAnalyzer(
            listenProfile = listenProfile,
            a4ReferenceHz = a4ReferenceHz,
            includeSpectrum = includeSpectrum,
        ) { nextState ->
            analysisState = nextState
        }
    }

    return analysisState
}

@SuppressLint("MissingPermission")
private suspend fun runAudioAnalyzer(
    listenProfile: TunerListenProfile,
    a4ReferenceHz: Int,
    includeSpectrum: Boolean,
    onAnalysis: (AudioAnalysisState) -> Unit,
) {
    val minBufferSize = AudioRecord.getMinBufferSize(
        AUDIO_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
    )
    if (minBufferSize <= 0) {
        Log.w(AUDIO_TEMPO_LOG_TAG, "audio init failed minBuffer=$minBufferSize requestedRate=$AUDIO_SAMPLE_RATE")
        return
    }

    val bufferSize = max(minBufferSize, AUDIO_FRAME_SIZE * 4)
    val recorder = withContext(Dispatchers.IO) {
        AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AUDIO_SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
    }
    val pitchFrameStride = if (minBufferSize >= SMALL_WATCH_MIN_BUFFER_SIZE) {
        SMALL_WATCH_PITCH_FRAME_STRIDE
    } else {
        1
    }
    Log.i(
        AUDIO_TEMPO_LOG_TAG,
        "audio init requestedRate=$AUDIO_SAMPLE_RATE actualRate=${recorder.sampleRate} " +
            "state=${recorder.state} minBuffer=$minBufferSize bufferSize=$bufferSize " +
            "frameSize=$AUDIO_FRAME_SIZE pitchStride=$pitchFrameStride listen=${listenProfile.label} " +
            "range=${listenProfile.minHz.roundToInt()}-${listenProfile.maxHz.roundToInt()}Hz " +
            "spectrum=$includeSpectrum",
    )
    val buffer = ShortArray(AUDIO_FRAME_SIZE)
    val pitchAverager = TunerPitchAverager(listenProfile)
    val tempoEstimator = MicTempoEstimator()
    var frameIndex = 0L
    var audioSamplePosition = 0L
    var lastUiUpdateElapsedMs = 0L
    var lastPublishedTempoBpm: Int? = null
    var lastPitchAnalysis = PitchAnalysisState()

    try {
        withContext(Dispatchers.IO) {
            recorder.startRecording()
        }
        val analyzerStartElapsedMs = SystemClock.elapsedRealtime()
        var lastProcessingLogElapsedMs = analyzerStartElapsedMs

        while (true) {
            val read = withContext(Dispatchers.IO) {
                recorder.read(buffer, 0, buffer.size)
            }
            if (read > 0) {
                val frameStartAudioMs = (audioSamplePosition * 1_000L) / AUDIO_SAMPLE_RATE
                audioSamplePosition += read
                val shouldAnalyzePitch = frameIndex % pitchFrameStride == 0L
                frameIndex += 1
                val analysis = analyzeAudioFrame(
                    buffer = buffer,
                    read = read,
                    frameStartAudioMs = frameStartAudioMs,
                    listenProfile = listenProfile,
                    pitchAverager = pitchAverager,
                    tempoEstimator = tempoEstimator,
                    a4ReferenceHz = a4ReferenceHz,
                    includeSpectrum = includeSpectrum,
                    analyzePitch = shouldAnalyzePitch,
                    previousPitchAnalysis = lastPitchAnalysis,
                )
                lastPitchAnalysis = analysis.toPitchAnalysisState()
                val nowElapsedMs = SystemClock.elapsedRealtime()
                if (nowElapsedMs - lastProcessingLogElapsedMs >= TEMPO_PROCESSING_LOG_INTERVAL_MS) {
                    val wallAudioMs = nowElapsedMs - analyzerStartElapsedMs
                    Log.i(
                        AUDIO_TEMPO_LOG_TAG,
                        "processing read=$read audio=${frameStartAudioMs}ms wall=${wallAudioMs}ms " +
                            "drift=${wallAudioMs - frameStartAudioMs}ms",
                    )
                    lastProcessingLogElapsedMs = nowElapsedMs
                }
                val tempoChanged = analysis.detectedTempoBpm != lastPublishedTempoBpm
                if (
                    nowElapsedMs - lastUiUpdateElapsedMs >= AUDIO_UI_UPDATE_INTERVAL_MS ||
                    tempoChanged
                ) {
                    onAnalysis(analysis)
                    lastUiUpdateElapsedMs = nowElapsedMs
                    lastPublishedTempoBpm = analysis.detectedTempoBpm
                }
            }
        }
    } finally {
        withContext(Dispatchers.IO) {
            runCatching { recorder.stop() }
            recorder.release()
        }
    }
}

private fun analyzeAudioFrame(
    buffer: ShortArray,
    read: Int,
    frameStartAudioMs: Long,
    listenProfile: TunerListenProfile,
    pitchAverager: TunerPitchAverager,
    tempoEstimator: MicTempoEstimator,
    a4ReferenceHz: Int,
    includeSpectrum: Boolean,
    analyzePitch: Boolean,
    previousPitchAnalysis: PitchAnalysisState,
): AudioAnalysisState {
    val sampleCount = read.coerceIn(1, buffer.size)
    var sumSquares = 0.0
    var transientSum = 0.0
    var peak = 0f
    var transientPeak = 0f
    var peakIndex = 0
    var previousSample = buffer[0] / Short.MAX_VALUE.toFloat()
    for (index in 0 until sampleCount) {
        val sample = buffer[index] / Short.MAX_VALUE.toFloat()
        val sampleAbs = abs(sample)
        if (sampleAbs > peak) {
            peak = sampleAbs
            peakIndex = index
        }
        if (index > 0) {
            val transient = abs(sample - previousSample)
            transientSum += transient
            if (transient > transientPeak) {
                transientPeak = transient
                peakIndex = index
            }
        }
        previousSample = sample
        sumSquares += sample * sample
    }
    val level = sqrt(sumSquares / sampleCount).toFloat().coerceIn(0f, 1f)
    val transientAverage = (transientSum / sampleCount).toFloat()
    val transientLevel = max(transientAverage * 5.0f, transientPeak * 0.35f).coerceIn(0f, 1f)
    val transientAudioTimeMs = frameStartAudioMs + ((peakIndex * 1_000L) / AUDIO_SAMPLE_RATE)
    val pitchAnalysis = if (analyzePitch) {
        val frequency = pitchAverager.average(detectPitchHz(buffer, sampleCount, listenProfile))
        val note = pitchAverager.stableNoteReading(
            noteReading = frequency?.toNoteReading(a4ReferenceHz),
            audioTimeMs = frameStartAudioMs,
        )
        val keyAnalysis = pitchAverager.noteSummary(note?.first)
        PitchAnalysisState(
            frequencyHz = frequency,
            noteName = note?.first ?: "--",
            cents = note?.second ?: 0,
            recentNotes = keyAnalysis.recentNotes,
            guessedKey = keyAnalysis.guessedKey,
            likelyChords = keyAnalysis.likelyChords,
        )
    } else {
        previousPitchAnalysis
    }
    val detectedTempoBpm = tempoEstimator.estimate(level = transientLevel, audioTimeMs = transientAudioTimeMs)

    return AudioAnalysisState(
        frequencyHz = pitchAnalysis.frequencyHz,
        noteName = pitchAnalysis.noteName,
        cents = pitchAnalysis.cents,
        level = level,
        detectedTempoBpm = detectedTempoBpm,
        recentNotes = pitchAnalysis.recentNotes,
        guessedKey = pitchAnalysis.guessedKey,
        likelyChords = pitchAnalysis.likelyChords,
        spectrum = if (includeSpectrum) buildSpectrum(buffer, sampleCount) else emptyList(),
    )
}

private fun detectPitchHz(
    buffer: ShortArray,
    sampleCount: Int,
    listenProfile: TunerListenProfile,
): Float? {
    val minLag = (AUDIO_SAMPLE_RATE / listenProfile.maxHz).roundToInt().coerceAtLeast(1)
    val maxLag = (AUDIO_SAMPLE_RATE / listenProfile.minHz).roundToInt().coerceAtMost(sampleCount - 2)
    if (maxLag <= minLag) return null

    var bestLag = 0
    var bestCorrelation = 0.0

    for (lag in minLag..maxLag) {
        var dot = 0.0
        var energyA = 0.0
        var energyB = 0.0
        val limit = sampleCount - lag

        for (index in 0 until limit) {
            val a = buffer[index].toDouble()
            val b = buffer[index + lag].toDouble()
            dot += a * b
            energyA += a * a
            energyB += b * b
        }

        val denominator = sqrt(energyA * energyB)
        val correlation = if (denominator > 0.0) dot / denominator else 0.0
        if (correlation > bestCorrelation) {
            bestCorrelation = correlation
            bestLag = lag
        }
    }

    if (bestLag <= 0 || bestCorrelation < 0.34) return null
    return AUDIO_SAMPLE_RATE.toFloat() / bestLag
}

private class TunerPitchAverager(
    private val listenProfile: TunerListenProfile,
) {
    private val recentFrequencies = mutableListOf<Float>()
    private val recentNoteClasses = mutableListOf<String>()
    private var stableNoteReading: Pair<String, Int>? = null
    private var stableNoteUpdatedAtMs = 0L
    private var pendingNoteName: String? = null
    private var pendingNoteCount = 0
    private var stableKeyAnalysis: KeyAnalysis? = null
    private var pendingKeyName: String? = null
    private var pendingKeyCount = 0

    fun average(frequency: Float?): Float? {
        if (frequency == null || frequency !in listenProfile.minHz..listenProfile.maxHz) {
            return recentFrequencies.smartAverageOrNull()
        }

        val currentAverage = recentFrequencies.smartAverageOrNull()
        if (currentAverage != null && abs(centsBetween(frequency, currentAverage)) > 85) {
            recentFrequencies.clear()
        }

        recentFrequencies += frequency
        while (recentFrequencies.size > TUNER_AVERAGE_SAMPLE_COUNT) {
            recentFrequencies.removeAt(0)
        }

        return recentFrequencies.smartAverageOrNull()
    }

    fun stableNoteReading(
        noteReading: Pair<String, Int>?,
        audioTimeMs: Long,
    ): Pair<String, Int>? {
        if (noteReading == null) {
            return stableNoteReading.takeIf {
                audioTimeMs - stableNoteUpdatedAtMs <= TUNER_NOTE_LOST_HOLD_MS
            }
        }

        val noteName = noteReading.first
        val currentStableNote = stableNoteReading?.first
        if (currentStableNote == null) {
            stableNoteReading = noteReading
            stableNoteUpdatedAtMs = audioTimeMs
            pendingNoteName = null
            pendingNoteCount = 0
            return noteReading
        }

        if (noteName == currentStableNote) {
            stableNoteReading = noteReading
            stableNoteUpdatedAtMs = audioTimeMs
            pendingNoteName = null
            pendingNoteCount = 0
            return noteReading
        }

        if (audioTimeMs - stableNoteUpdatedAtMs < TUNER_NOTE_HOLD_MS) {
            return stableNoteReading
        }

        if (pendingNoteName == noteName) {
            pendingNoteCount += 1
        } else {
            pendingNoteName = noteName
            pendingNoteCount = 1
        }

        return if (pendingNoteCount >= TUNER_NOTE_SWITCH_CONFIRMATION_COUNT) {
            stableNoteReading = noteReading
            stableNoteUpdatedAtMs = audioTimeMs
            pendingNoteName = null
            pendingNoteCount = 0
            noteReading
        } else {
            stableNoteReading
        }
    }

    fun noteSummary(noteName: String?): KeyAnalysis {
        val noteClass = noteName?.toNoteClass()
        if (noteClass != null) {
            recentNoteClasses += noteClass
            while (recentNoteClasses.size > TUNER_KEY_SAMPLE_COUNT) {
                recentNoteClasses.removeAt(0)
            }
        }

        val nextAnalysis = analyzeMusicalKey(recentNoteClasses)
        val nextKey = nextAnalysis.guessedKey
        val stableKey = stableKeyAnalysis?.guessedKey

        if (nextKey == null) {
            return stableKeyAnalysis?.copy(recentNotes = nextAnalysis.recentNotes) ?: nextAnalysis
        }

        if (stableKey == null || nextKey == stableKey) {
            stableKeyAnalysis = nextAnalysis
            pendingKeyName = null
            pendingKeyCount = 0
            return nextAnalysis
        }

        if (pendingKeyName == nextKey) {
            pendingKeyCount += 1
        } else {
            pendingKeyName = nextKey
            pendingKeyCount = 1
        }

        return if (pendingKeyCount >= TUNER_KEY_SWITCH_CONFIRMATION_COUNT) {
            stableKeyAnalysis = nextAnalysis
            pendingKeyName = null
            pendingKeyCount = 0
            nextAnalysis
        } else {
            stableKeyAnalysis?.copy(recentNotes = nextAnalysis.recentNotes) ?: nextAnalysis
        }
    }
}

private class MicTempoEstimator {
    private val onsetTimesMs = mutableListOf<Long>()
    private var smoothedLevel = 0f
    private var previousLevel = 0f
    private var lastOnsetMs = 0L
    private var stableTempoBpm: Int? = null
    private var pendingTempoBpm: Int? = null
    private var pendingTempoCount = 0
    private var lastDebugLogMs = 0L
    private var lastLoggedStableTempoBpm: Int? = null

    fun estimate(level: Float, audioTimeMs: Long): Int? {
        val safeLevel = level.coerceIn(0f, 1f)
        val intervalSinceLastOnsetMs = if (lastOnsetMs > 0L) audioTimeMs - lastOnsetMs else null
        if (lastOnsetMs > 0L && audioTimeMs - lastOnsetMs > TEMPO_DETECTION_TIMEOUT_MS) {
            onsetTimesMs.clear()
            stableTempoBpm = null
            pendingTempoBpm = null
            pendingTempoCount = 0
        }
        smoothedLevel = if (smoothedLevel <= 0f) {
            safeLevel
        } else {
            (smoothedLevel * 0.96f) + (safeLevel * 0.04f)
        }

        val levelJump = safeLevel - previousLevel
        val adaptiveLevelThreshold = max(0.006f, smoothedLevel * 1.45f)
        val adaptiveJumpThreshold = max(0.0025f, smoothedLevel * 0.28f)
        val isOnset = safeLevel > adaptiveLevelThreshold &&
            levelJump > adaptiveJumpThreshold &&
            audioTimeMs - lastOnsetMs >= MIN_TEMPO_ONSET_INTERVAL_MS

        previousLevel = safeLevel
        if (!isOnset) {
            logTempoDebug(
                audioTimeMs = audioTimeMs,
                safeLevel = safeLevel,
                levelJump = levelJump,
                isOnset = false,
                intervalSinceLastOnsetMs = intervalSinceLastOnsetMs,
                candidateBpm = null,
            )
            return stableTempoBpm
        }

        lastOnsetMs = audioTimeMs
        onsetTimesMs += audioTimeMs
        while (onsetTimesMs.size > TEMPO_ONSET_HISTORY_COUNT) {
            onsetTimesMs.removeAt(0)
        }

        val candidateBpm = estimateTempoFromOnsets(hasStableTempo = stableTempoBpm != null)
        candidateBpm?.let { candidate ->
            updateStableTempo(candidate)
        }
        logTempoDebug(
            audioTimeMs = audioTimeMs,
            safeLevel = safeLevel,
            levelJump = levelJump,
            isOnset = true,
            intervalSinceLastOnsetMs = intervalSinceLastOnsetMs,
            candidateBpm = candidateBpm,
        )
        return stableTempoBpm
    }

    private fun logTempoDebug(
        audioTimeMs: Long,
        safeLevel: Float,
        levelJump: Float,
        isOnset: Boolean,
        intervalSinceLastOnsetMs: Long?,
        candidateBpm: Int?,
    ) {
        val stableChanged = stableTempoBpm != lastLoggedStableTempoBpm
        val shouldLog = isOnset ||
            candidateBpm != null ||
            stableChanged ||
            audioTimeMs - lastDebugLogMs >= TEMPO_DEBUG_LOG_INTERVAL_MS
        if (!shouldLog) return

        lastDebugLogMs = audioTimeMs
        lastLoggedStableTempoBpm = stableTempoBpm
        Log.i(
            AUDIO_TEMPO_LOG_TAG,
            "tempo t=${audioTimeMs}ms level=${safeLevel.debugLevel()} " +
                "smooth=${smoothedLevel.debugLevel()} jump=${levelJump.debugLevel()} " +
                "onset=$isOnset interval=${intervalSinceLastOnsetMs ?: "--"}ms " +
                "onsets=${onsetTimesMs.size} candidate=${candidateBpm ?: "--"} " +
                "pending=${pendingTempoBpm ?: "--"}/$pendingTempoCount " +
                "stable=${stableTempoBpm ?: "--"} " +
                "window=${tempoDebugWindow()}",
        )
    }

    private fun tempoDebugWindow(): String {
        val intervals = onsetTimesMs
            .zipWithNext { previous, current -> current - previous }
            .filter { it in MIN_TEMPO_ONSET_INTERVAL_MS..MAX_TEMPO_ONSET_INTERVAL_MS }
            .takeLast(TEMPO_INTERVAL_WINDOW_COUNT)
        if (intervals.isEmpty()) return "[]"

        val bpmCandidates = intervals.map { intervalMs ->
            (60_000f / intervalMs).roundToInt()
                .normalizedDetectedTempo()
                .coerceIn(MIN_BPM, MAX_BPM)
        }
        return intervals.zip(bpmCandidates).joinToString(
            separator = ",",
            prefix = "[",
            postfix = "]",
        ) { (intervalMs, bpm) ->
            "${intervalMs}ms:${bpm}bpm"
        }
    }

    private fun estimateTempoFromOnsets(hasStableTempo: Boolean): Int? {
        if (onsetTimesMs.size < 4) return null

        val intervals = onsetTimesMs
            .zipWithNext { previous, current -> current - previous }
            .filter { it in MIN_TEMPO_ONSET_INTERVAL_MS..MAX_TEMPO_ONSET_INTERVAL_MS }
            .takeLast(TEMPO_INTERVAL_WINDOW_COUNT)
        val minimumIntervalCount = if (hasStableTempo) 4 else 3
        if (intervals.size < minimumIntervalCount) return null

        val bpmCandidates = intervals
            .map { intervalMs ->
                (60_000f / intervalMs).roundToInt()
                    .normalizedDetectedTempo()
                    .coerceIn(MIN_BPM, MAX_BPM)
            }
            .sorted()

        val medianBpm = bpmCandidates[bpmCandidates.size / 2]
        val clustered = bpmCandidates.filter { bpm ->
            abs(bpm - medianBpm) <= TEMPO_CLUSTER_TOLERANCE_BPM
        }
        val requiredClusterSize = if (hasStableTempo) {
            ((intervals.size / 2) + 1).coerceAtLeast(4)
        } else {
            ((intervals.size / 2) + 1).coerceAtLeast(3)
        }
        if (clustered.size < requiredClusterSize) return null

        val adjacentTempo = clustered.average().roundToInt()
        val multiGapTempo = estimateTempoFromMultiGapOnsets(hasStableTempo)
        return multiGapTempo ?: adjacentTempo
    }

    private fun estimateTempoFromMultiGapOnsets(hasStableTempo: Boolean): Int? {
        val times = onsetTimesMs.takeLast(TEMPO_ONSET_HISTORY_COUNT)
        if (times.size < 5) return null

        val scoredTempos = mutableMapOf<Int, Float>()
        for (startIndex in times.indices) {
            for (endIndex in startIndex + 1 until times.size) {
                val gapMs = times[endIndex] - times[startIndex]
                if (gapMs !in MIN_TEMPO_ONSET_INTERVAL_MS..MAX_TEMPO_ONSET_INTERVAL_MS) continue

                val beatSpan = endIndex - startIndex
                val rawBpm = (60_000f * beatSpan / gapMs).roundToInt()
                    .normalizedDetectedTempo()
                    .coerceIn(MIN_BPM, MAX_BPM)
                val tempoBin = (rawBpm / 2) * 2
                val continuityWeight = if (beatSpan == 1) 1.0f else 1.35f
                val stableWeight = stableTempoBpm?.let { stable ->
                    if (abs(rawBpm - stable) <= TEMPO_CLUSTER_TOLERANCE_BPM) 1.35f else 1.0f
                } ?: 1.0f
                scoredTempos[tempoBin] = (scoredTempos[tempoBin] ?: 0f) + continuityWeight * stableWeight
            }
        }

        val best = scoredTempos.maxByOrNull { it.value } ?: return null
        val requiredScore = if (hasStableTempo) 5.0f else 4.0f
        if (best.value < requiredScore) return null

        val matchingTempos = scoredTempos
            .filterKeys { tempo -> abs(tempo - best.key) <= TEMPO_CLUSTER_TOLERANCE_BPM }
        val weightedAverage = matchingTempos.entries.sumOf { (tempo, score) -> tempo.toDouble() * score } /
            matchingTempos.values.sumOf { it.toDouble() }
        return weightedAverage.roundToInt()
    }

    private fun updateStableTempo(candidateBpm: Int) {
        val currentStable = stableTempoBpm
        if (currentStable != null && abs(candidateBpm - currentStable) <= TEMPO_CLUSTER_TOLERANCE_BPM) {
            stableTempoBpm = ((currentStable * 0.82f) + (candidateBpm * 0.18f)).roundToInt()
            pendingTempoBpm = null
            pendingTempoCount = 0
            return
        }

        if (pendingTempoBpm?.let { abs(candidateBpm - it) <= TEMPO_CLUSTER_TOLERANCE_BPM } == true) {
            pendingTempoBpm = ((pendingTempoBpm ?: candidateBpm) + candidateBpm) / 2
            pendingTempoCount += 1
        } else {
            pendingTempoBpm = candidateBpm
            pendingTempoCount = 1
        }

        if (currentStable == null || pendingTempoCount >= TEMPO_CHANGE_CONFIRMATION_COUNT) {
            stableTempoBpm = pendingTempoBpm
            pendingTempoBpm = null
            pendingTempoCount = 0
        }
    }
}

private fun List<Float>.smartAverageOrNull(): Float? {
    if (isEmpty()) return null

    val sorted = sorted()
    val median = sorted[sorted.lastIndex / 2]
    val stable = filter { abs(centsBetween(it, median)) <= 45 }
        .ifEmpty { this }

    return stable.average().toFloat()
}

private fun centsBetween(
    frequency: Float,
    reference: Float,
): Float {
    return 1200f * log2(frequency / reference)
}

fun Float.toNoteReading(a4ReferenceHz: Int): Pair<String, Int> {
    val referenceHz = a4ReferenceHz.coerceIn(MIN_A4_REFERENCE_HZ, MAX_A4_REFERENCE_HZ).toFloat()
    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val midi = (69f + 12f * log2(this / referenceHz)).roundToInt()
    val noteFrequency = referenceHz * 2f.pow((midi - 69) / 12f)
    val cents = (1200f * log2(this / noteFrequency)).roundToInt()
    val octave = (midi / 12) - 1
    val name = "${noteNames[midi.floorMod(noteNames.size)]}$octave"
    return name to cents
}

private fun Int.floorMod(divisor: Int): Int {
    return ((this % divisor) + divisor) % divisor
}

private fun Int.normalizedDetectedTempo(): Int {
    var tempo = this
    while (tempo < 70) tempo *= 2
    while (tempo > 190) tempo /= 2
    return tempo
}

private fun buildSpectrum(
    buffer: ShortArray,
    sampleCount: Int,
): List<Float> {
    return List(SPECTRUM_BAR_COUNT) { index ->
        val frequency = spectrumFrequencyForIndex(index, SPECTRUM_BAR_COUNT)
        goertzelLevel(buffer, sampleCount, frequency).coerceIn(0f, 1f)
    }
}

fun List<Float>.peakSpectrumReading(): SpectrumPeak? {
    if (isEmpty()) return null

    var peakIndex = 0
    var peakLevel = 0f
    forEachIndexed { index, level ->
        if (level > peakLevel) {
            peakIndex = index
            peakLevel = level
        }
    }

    if (peakLevel < 0.015f) return null

    val frequencyHz = spectrumFrequencyForIndex(peakIndex, size)
    return SpectrumPeak(
        frequencyHz = frequencyHz,
        level = peakLevel,
        bandLabel = spectrumBandForFrequency(frequencyHz).label,
    )
}

fun spectrumBands(): List<SpectrumBand> {
    return listOf(
        SpectrumBand(30f, 60f, "Sub bass", Color(0xFF7E8799)),
        SpectrumBand(60f, 250f, "Bass/Kick", Color(0xFF2DD4BF)),
        SpectrumBand(250f, 500f, "Low mids", Color(0xFF84CC16)),
        SpectrumBand(500f, 2_000f, "Mids/Vox", Color(0xFFFACC15)),
        SpectrumBand(2_000f, 6_000f, "Presence", Color(0xFFFB7185)),
        SpectrumBand(6_000f, 10_000f, "Air/Hats", Color(0xFF60A5FA)),
    )
}

fun spectrumBandForFrequency(
    frequencyHz: Float,
    bands: List<SpectrumBand> = spectrumBands(),
): SpectrumBand {
    return bands.firstOrNull { frequencyHz >= it.startHz && frequencyHz < it.endHz }
        ?: bands.last()
}

private fun spectrumFrequencyForIndex(
    index: Int,
    count: Int,
): Float {
    val progress = if (count <= 1) 0f else index.toFloat() / (count - 1)
    return 30f * (10_000f / 30f).pow(progress)
}

private fun goertzelLevel(
    buffer: ShortArray,
    sampleCount: Int,
    frequency: Float,
): Float {
    val omega = 2.0 * PI * frequency / AUDIO_SAMPLE_RATE
    val coefficient = 2.0 * cos(omega)
    var q0: Double
    var q1 = 0.0
    var q2 = 0.0

    for (index in 0 until sampleCount) {
        q0 = coefficient * q1 - q2 + (buffer[index] / Short.MAX_VALUE.toDouble())
        q2 = q1
        q1 = q0
    }

    val power = q1 * q1 + q2 * q2 - coefficient * q1 * q2
    val magnitude = (sqrt(power).toFloat() * 2f / sampleCount).coerceAtLeast(0f)
    return (magnitude * 10f).coerceIn(0f, 1f)
}

private fun Float.debugLevel(): String {
    return ((this * 10_000f).roundToInt() / 10_000f).toString()
}
