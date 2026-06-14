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
import kotlin.math.sin
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
private const val TUNER_KEY_SWITCH_CONFIRMATION_COUNT = 20
private const val TUNER_MIN_PITCH_LEVEL = 0.0065f
private const val TUNER_MIN_KEY_LEVEL = 0.012f
private const val TUNER_A4_REFERENCE_SAMPLE_COUNT = 18
private const val MIN_TEMPO_ONSET_INTERVAL_MS = 300L
private const val MAX_TEMPO_ONSET_INTERVAL_MS = 2_000L
private const val TEMPO_ONSET_HISTORY_COUNT = 24
private const val TEMPO_INTERVAL_WINDOW_COUNT = 11
private const val TEMPO_CLUSTER_TOLERANCE_BPM = 4
private const val TEMPO_CHANGE_CONFIRMATION_COUNT = 3
private const val TEMPO_DETECTION_TIMEOUT_MS = 3_000L
private const val TEMPO_DEBUG_LOG_INTERVAL_MS = 1_000L
private const val TEMPO_PROCESSING_LOG_INTERVAL_MS = 2_000L
private const val TUNER_KEY_SAMPLE_COUNT = 192
private const val SONG_CONTEXT_CHORD_SAMPLE_COUNT = 360
private const val SONG_CONTEXT_MIN_CHORD_SAMPLES = 16

private data class PitchAnalysisState(
    val frequencyHz: Float? = null,
    val noteName: String = "--",
    val cents: Int = 0,
    val recentNotes: List<String> = emptyList(),
    val guessedKey: String? = null,
    val likelyChords: List<String> = emptyList(),
    val chordTones: List<String> = emptyList(),
    val chordProgression: List<String> = emptyList(),
    val sensedA4Hz: Float? = null,
    val sensedA4OffsetCents: Int = 0,
)

private data class TempoAnalysisState(
    val detectedBpm: Int? = null,
    val strictBpm: Int? = null,
    val bassBpm: Int? = null,
    val snareBpm: Int? = null,
    val fluxBpm: Int? = null,
    val smartConfidence: Float = 0f,
    val learningBeats: Int = 0,
    val activeBeatIndex: Int = -1,
    val confident: Boolean = false,
    val meter: Int = 4,
    val musicalBpm: Int? = null,
    val feelLabel: String = "",
    val meterLabel: String = "",
)

private data class FftFrame(
    val magnitudes: FloatArray,
    val binHz: Float,
)

private fun AudioAnalysisState.toPitchAnalysisState(): PitchAnalysisState {
    return PitchAnalysisState(
        frequencyHz = frequencyHz,
        noteName = noteName,
        cents = cents,
        recentNotes = recentNotes,
        guessedKey = guessedKey,
        likelyChords = likelyChords,
        chordTones = chordTones,
        chordProgression = chordProgression,
        sensedA4Hz = sensedA4Hz,
        sensedA4OffsetCents = sensedA4OffsetCents,
    )
}

@Composable
fun rememberAudioAnalysisState(
    enabled: Boolean,
    listenProfile: TunerListenProfile,
    readerMode: SpectrumReaderMode,
    tuningChoice: SpectrumTuningChoice? = null,
    a4ReferenceHz: Int,
    includeSpectrum: Boolean,
): AudioAnalysisState {
    var analysisState by remember { mutableStateOf(AudioAnalysisState()) }
    val listenRange = remember(listenProfile, readerMode, tuningChoice) {
        listenProfile.audioListenRangeFor(readerMode, tuningChoice)
    }

    LaunchedEffect(enabled, listenRange, a4ReferenceHz, includeSpectrum) {
        if (!enabled) {
            analysisState = AudioAnalysisState()
            return@LaunchedEffect
        }

        runAudioAnalyzer(
            listenRange = listenRange,
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
    listenRange: AudioListenRange,
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
            "frameSize=$AUDIO_FRAME_SIZE pitchStride=$pitchFrameStride listen=${listenRange.label} " +
            "range=${listenRange.minHz.roundToInt()}-${listenRange.maxHz.roundToInt()}Hz " +
            "spectrum=$includeSpectrum",
    )
    val buffer = ShortArray(AUDIO_FRAME_SIZE)
    val pitchAverager = TunerPitchAverager(listenRange)
    val tempoEstimator = MicTempoEstimator()
    var frameIndex = 0L
    var audioSamplePosition = 0L
    var lastUiUpdateElapsedMs = 0L
    var lastPublishedTempoBpm: Int? = null
    var lastPublishedTempoBeatIndex = -1
    var lastPublishedTempoLearningBeats = 0
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
                    listenRange = listenRange,
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
                val tempoChanged = analysis.detectedTempoBpm != lastPublishedTempoBpm ||
                    analysis.tempoActiveBeatIndex != lastPublishedTempoBeatIndex ||
                    analysis.tempoLearningBeats != lastPublishedTempoLearningBeats
                if (
                    nowElapsedMs - lastUiUpdateElapsedMs >= AUDIO_UI_UPDATE_INTERVAL_MS ||
                    tempoChanged
                ) {
                    onAnalysis(analysis)
                    lastUiUpdateElapsedMs = nowElapsedMs
                    lastPublishedTempoBpm = analysis.detectedTempoBpm
                    lastPublishedTempoBeatIndex = analysis.tempoActiveBeatIndex
                    lastPublishedTempoLearningBeats = analysis.tempoLearningBeats
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
    listenRange: AudioListenRange,
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
    val fftFrame = buildFftFrame(buffer, sampleCount)
    val bassPulseLevel = fftBandLevel(fftFrame, minHz = 40f, maxHz = 180f, gain = 70f)
    val snarePulseLevel = fftBandLevel(fftFrame, minHz = 180f, maxHz = 4_000f, gain = 34f)
    val fluxPulseLevel = tempoEstimator.spectralFluxLevel(fftFrame, minHz = 55f, maxHz = 5_500f, gain = 42f)
    val transientAudioTimeMs = frameStartAudioMs + ((peakIndex * 1_000L) / AUDIO_SAMPLE_RATE)
    val pitchAnalysis = if (analyzePitch && level >= TUNER_MIN_PITCH_LEVEL) {
        val frequency = pitchAverager.average(detectPitchHz(buffer, sampleCount, listenRange))
        val note = pitchAverager.stableNoteReading(
            noteReading = frequency?.toNoteReading(a4ReferenceHz),
            audioTimeMs = frameStartAudioMs,
        )
        val sensedA4Hz = if (note != null) {
            pitchAverager.sensedA4Reference(frequency)
        } else {
            null
        }
        val spectrumNoteClasses = if (level >= TUNER_MIN_KEY_LEVEL) {
            dominantSpectrumNoteClasses(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = listenRange.minHz.coerceAtLeast(55f),
                maxHz = listenRange.maxHz.coerceAtMost(2_200f),
                stableNoteClass = note?.first?.toNoteClass(),
            )
        } else {
            emptyList()
        }
        val keyAnalysis = if (level >= TUNER_MIN_KEY_LEVEL && note != null) {
            val stableNoteVotes = if (spectrumNoteClasses.size >= 2) 2 else 4
            pitchAverager.noteSummary(
                List(stableNoteVotes) { note.first } + spectrumNoteClasses,
            )
        } else if (level >= TUNER_MIN_KEY_LEVEL) {
            pitchAverager.noteSummary(spectrumNoteClasses)
        } else {
            pitchAverager.currentKeySummary()
        }
        PitchAnalysisState(
            frequencyHz = frequency,
            noteName = note?.first ?: "--",
            cents = note?.second ?: 0,
            recentNotes = keyAnalysis.recentNotes,
            guessedKey = keyAnalysis.guessedKey,
            likelyChords = keyAnalysis.likelyChords,
            chordTones = keyAnalysis.chordTones,
            chordProgression = keyAnalysis.chordProgression,
            sensedA4Hz = sensedA4Hz,
            sensedA4OffsetCents = sensedA4Hz?.let { sensed ->
                centsBetween(sensed, a4ReferenceHz.toFloat()).roundToInt()
            } ?: 0,
        )
    } else if (level < TUNER_MIN_PITCH_LEVEL) {
        pitchAverager.clear()
        PitchAnalysisState()
    } else {
        previousPitchAnalysis
    }
    val tempoAnalysis = tempoEstimator.estimate(
        strictLevel = transientLevel,
        bassLevel = bassPulseLevel,
        snareLevel = snarePulseLevel,
        fluxLevel = fluxPulseLevel,
        audioTimeMs = transientAudioTimeMs,
    )

    return AudioAnalysisState(
        frequencyHz = pitchAnalysis.frequencyHz,
        noteName = pitchAnalysis.noteName,
        cents = pitchAnalysis.cents,
        level = level,
        detectedTempoBpm = tempoAnalysis.detectedBpm,
        strictTempoBpm = tempoAnalysis.strictBpm,
        bassTempoBpm = tempoAnalysis.bassBpm,
        snareTempoBpm = tempoAnalysis.snareBpm,
        fluxTempoBpm = tempoAnalysis.fluxBpm,
        smartTempoConfidence = tempoAnalysis.smartConfidence,
        tempoLearningBeats = tempoAnalysis.learningBeats,
        tempoActiveBeatIndex = tempoAnalysis.activeBeatIndex,
        tempoConfident = tempoAnalysis.confident,
        tempoMeter = tempoAnalysis.meter,
        musicalTempoBpm = tempoAnalysis.musicalBpm,
        tempoFeelLabel = tempoAnalysis.feelLabel,
        tempoMeterLabel = tempoAnalysis.meterLabel,
        recentNotes = pitchAnalysis.recentNotes,
        guessedKey = pitchAnalysis.guessedKey,
        likelyChords = pitchAnalysis.likelyChords,
        chordTones = pitchAnalysis.chordTones,
        chordProgression = pitchAnalysis.chordProgression,
        sensedA4Hz = pitchAnalysis.sensedA4Hz,
        sensedA4OffsetCents = pitchAnalysis.sensedA4OffsetCents,
        spectrum = if (includeSpectrum) buildSpectrum(fftFrame) else emptyList(),
    )
}

private fun detectPitchHz(
    buffer: ShortArray,
    sampleCount: Int,
    listenRange: AudioListenRange,
): Float? {
    val minLag = (AUDIO_SAMPLE_RATE / listenRange.maxHz).roundToInt().coerceAtLeast(1)
    val maxLag = (AUDIO_SAMPLE_RATE / listenRange.minHz).roundToInt().coerceAtMost(sampleCount - 2)
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
    private val listenRange: AudioListenRange,
) {
    private val recentFrequencies = mutableListOf<Float>()
    private val recentA4References = mutableListOf<Float>()
    private val recentNoteClasses = mutableListOf<String>()
    private var stableNoteReading: Pair<String, Int>? = null
    private var stableNoteUpdatedAtMs = 0L
    private var pendingNoteName: String? = null
    private var pendingNoteCount = 0
    private var stableKeyAnalysis: KeyAnalysis? = null
    private var pendingKeyName: String? = null
    private var pendingKeyCount = 0
    private val songKeyTracker = SongKeyTracker()

    fun average(frequency: Float?): Float? {
        if (frequency == null || frequency !in listenRange.minHz..listenRange.maxHz) {
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
        return noteSummary(listOfNotNull(noteName?.toNoteClass()))
    }

    fun noteSummary(noteClasses: List<String>): KeyAnalysis {
        noteClasses
            .mapNotNull { it.toNoteClass() }
            .forEach { noteClass ->
                recentNoteClasses += noteClass
                while (recentNoteClasses.size > TUNER_KEY_SAMPLE_COUNT) {
                    recentNoteClasses.removeAt(0)
                }
            }

        val rawAnalysis = analyzeMusicalKey(recentNoteClasses)
        val scaleFirstAnalysis = rawAnalysis
        val songContext = songKeyTracker.update(
            chordLabels = scaleFirstAnalysis.likelyChords,
            scalarKey = scaleFirstAnalysis.guessedKey,
            scalarConfidence = scaleFirstAnalysis.scalarConfidence,
        )
        val nextAnalysis = scaleFirstAnalysis.copy(
            guessedKey = songContext.keySuggestion ?: stableKeyAnalysis?.guessedKey,
            chordProgression = songContext.progression,
        )
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

        val switchConfirmationCount = if (stableKey.isMinorOrBluesKey() && !stableKey.hasSameKeyRoot(nextKey)) {
            TUNER_KEY_SWITCH_CONFIRMATION_COUNT * 2
        } else {
            TUNER_KEY_SWITCH_CONFIRMATION_COUNT
        }

        return if (pendingKeyCount >= switchConfirmationCount) {
            stableKeyAnalysis = nextAnalysis
            pendingKeyName = null
            pendingKeyCount = 0
            nextAnalysis
        } else {
            stableKeyAnalysis
                ?.copy(
                    recentNotes = nextAnalysis.recentNotes,
                    likelyChords = nextAnalysis.likelyChords,
                    chordTones = nextAnalysis.chordTones,
                )
                ?: nextAnalysis
        }
    }

    fun currentKeySummary(): KeyAnalysis {
        return stableKeyAnalysis?.copy(recentNotes = recentNoteClasses.takeLast(8))
            ?: KeyAnalysis(
                recentNotes = recentNoteClasses.takeLast(8),
                guessedKey = null,
                likelyChords = emptyList(),
                chordTones = emptyList(),
                chordProgression = emptyList(),
            )
    }

    fun sensedA4Reference(frequency: Float?): Float? {
        val estimate = frequency?.estimatedA4Reference() ?: return recentA4References.smartAverageOrNull()
        recentA4References += estimate
        while (recentA4References.size > TUNER_A4_REFERENCE_SAMPLE_COUNT) {
            recentA4References.removeAt(0)
        }
        return recentA4References.smartAverageOrNull()
    }

    fun clearKeySummary() {
        recentNoteClasses.clear()
        recentA4References.clear()
        stableKeyAnalysis = null
        pendingKeyName = null
        pendingKeyCount = 0
        songKeyTracker.clear()
    }

    fun clear() {
        recentFrequencies.clear()
        stableNoteReading = null
        stableNoteUpdatedAtMs = 0L
        pendingNoteName = null
        pendingNoteCount = 0
        clearKeySummary()
    }
}

private class MicTempoEstimator {
    private val onsetTimesMs = mutableListOf<Long>()
    private val onsetStrengths = mutableListOf<Float>()
    private val bassTempoEngine = AuxiliaryTempoEngine(
        levelThresholdFloor = 0.005f,
        jumpThresholdFloor = 0.002f,
        smoothing = 0.94f,
    )
    private val snareTempoEngine = AuxiliaryTempoEngine(
        levelThresholdFloor = 0.004f,
        jumpThresholdFloor = 0.0018f,
        smoothing = 0.93f,
    )
    private val fluxTempoEngine = AuxiliaryTempoEngine(
        levelThresholdFloor = 0.004f,
        jumpThresholdFloor = 0.0016f,
        smoothing = 0.92f,
    )
    private var smoothedLevel = 0f
    private var previousLevel = 0f
    private var lastOnsetMs = 0L
    private var stableTempoBpm: Int? = null
    private var pendingTempoBpm: Int? = null
    private var pendingTempoCount = 0
    private var lastDebugLogMs = 0L
    private var lastLoggedStableTempoBpm: Int? = null
    private var stableMeter = 4
    private var previousFluxMagnitudes: FloatArray? = null

    fun estimate(
        strictLevel: Float,
        bassLevel: Float,
        snareLevel: Float,
        fluxLevel: Float,
        audioTimeMs: Long,
    ): TempoAnalysisState {
        val safeLevel = strictLevel.coerceIn(0f, 1f)
        val bassBpm = bassTempoEngine.estimate(bassLevel, audioTimeMs)
        val snareBpm = snareTempoEngine.estimate(snareLevel, audioTimeMs)
        val fluxBpm = fluxTempoEngine.estimate(fluxLevel, audioTimeMs)
        val intervalSinceLastOnsetMs = if (lastOnsetMs > 0L) audioTimeMs - lastOnsetMs else null
        if (lastOnsetMs > 0L && audioTimeMs - lastOnsetMs > TEMPO_DETECTION_TIMEOUT_MS) {
            clearTempoState()
            stableTempoBpm = null
            pendingTempoBpm = null
            pendingTempoCount = 0
        }
        val smartTempo = combineTempoVotes(
            strictBpm = stableTempoBpm,
            bassBpm = bassBpm,
            snareBpm = snareBpm,
            fluxBpm = fluxBpm,
        )
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
            return tempoState(
                activeBeatIndex = -1,
                bassBpm = bassBpm,
                snareBpm = snareBpm,
                fluxBpm = fluxBpm,
                smartTempo = smartTempo,
            )
        }

        lastOnsetMs = audioTimeMs
        onsetTimesMs += audioTimeMs
        onsetStrengths += (safeLevel + levelJump.coerceAtLeast(0f))
        while (onsetTimesMs.size > TEMPO_ONSET_HISTORY_COUNT) {
            onsetTimesMs.removeAt(0)
            onsetStrengths.removeAt(0)
        }

        val candidateBpm = estimateTempoFromOnsets(hasStableTempo = stableTempoBpm != null)
        candidateBpm?.let { candidate ->
            updateStableTempo(candidate)
        }
        stableMeter = estimateMeterFromAccents()
        logTempoDebug(
            audioTimeMs = audioTimeMs,
            safeLevel = safeLevel,
            levelJump = levelJump,
            isOnset = true,
            intervalSinceLastOnsetMs = intervalSinceLastOnsetMs,
            candidateBpm = candidateBpm,
        )
        return tempoState(
            activeBeatIndex = (onsetTimesMs.size - 1).floorMod(stableMeter),
            bassBpm = bassBpm,
            snareBpm = snareBpm,
            fluxBpm = fluxBpm,
            smartTempo = combineTempoVotes(
                strictBpm = stableTempoBpm,
                bassBpm = bassBpm,
                snareBpm = snareBpm,
                fluxBpm = fluxBpm,
            ),
        )
    }

    private fun tempoState(
        activeBeatIndex: Int,
        bassBpm: Int?,
        snareBpm: Int?,
        fluxBpm: Int?,
        smartTempo: SmartTempoVote,
    ): TempoAnalysisState {
        val auxiliaryFallbackBpm = auxiliaryTempoFallback(
            bassBpm = bassBpm,
            snareBpm = snareBpm,
            fluxBpm = fluxBpm,
        )
        val rawDetectedBpm = smartTempo.bpm ?: stableTempoBpm ?: auxiliaryFallbackBpm
        val interpretationConfidence = if (smartTempo.bpm != null || stableTempoBpm != null) {
            smartTempo.confidence
        } else if (auxiliaryFallbackBpm != null) {
            0.28f
        } else {
            0f
        }
        val musicalInterpretation = interpretMusicalTempo(
            detectedBpm = rawDetectedBpm,
            meter = stableMeter,
            confidence = interpretationConfidence,
        )
        return TempoAnalysisState(
            detectedBpm = rawDetectedBpm,
            strictBpm = stableTempoBpm,
            bassBpm = bassBpm,
            snareBpm = snareBpm,
            fluxBpm = fluxBpm,
            smartConfidence = smartTempo.confidence,
            learningBeats = onsetTimesMs.size.coerceIn(0, stableMeter),
            activeBeatIndex = activeBeatIndex,
            confident = stableTempoBpm != null || smartTempo.confidence >= 0.55f,
            meter = stableMeter,
            musicalBpm = musicalInterpretation.bpm,
            feelLabel = musicalInterpretation.feelLabel,
            meterLabel = musicalInterpretation.meterLabel,
        )
    }

    private fun clearTempoState() {
        onsetTimesMs.clear()
        onsetStrengths.clear()
        bassTempoEngine.clear()
        snareTempoEngine.clear()
        fluxTempoEngine.clear()
        previousFluxMagnitudes = null
        stableMeter = 4
    }

    fun spectralFluxLevel(
        fftFrame: FftFrame,
        minHz: Float,
        maxHz: Float,
        gain: Float,
    ): Float {
        val previous = previousFluxMagnitudes
        previousFluxMagnitudes = fftFrame.magnitudes.copyOf()
        if (previous == null || previous.size != fftFrame.magnitudes.size) return 0f

        val startBin = (minHz / fftFrame.binHz).roundToInt().coerceIn(1, fftFrame.magnitudes.lastIndex)
        val endBin = (maxHz / fftFrame.binHz).roundToInt().coerceIn(startBin, fftFrame.magnitudes.lastIndex)
        var flux = 0f
        var count = 0
        for (bin in startBin..endBin) {
            flux += (fftFrame.magnitudes[bin] - previous[bin]).coerceAtLeast(0f)
            count += 1
        }
        if (count == 0) return 0f
        return (flux / count * gain).coerceIn(0f, 1f)
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

    private fun estimateMeterFromAccents(): Int {
        val currentMeter = stableMeter
        if (stableTempoBpm == null || onsetStrengths.size < 9) return currentMeter

        val strengths = onsetStrengths.takeLast(16)
        fun scoreMeter(meter: Int): Float {
            if (strengths.size < meter * 3) return 0f
            var bestScore = 0f
            for (phase in 0 until meter) {
                var downbeatSum = 0f
                var downbeatCount = 0
                var otherSum = 0f
                var otherCount = 0
                strengths.forEachIndexed { index, strength ->
                    if (index % meter == phase) {
                        downbeatSum += strength
                        downbeatCount += 1
                    } else {
                        otherSum += strength
                        otherCount += 1
                    }
                }
                if (downbeatCount < 3 || otherCount <= 0) continue
                val downbeatAverage = downbeatSum / downbeatCount
                val otherAverage = otherSum / otherCount
                val contrast = (downbeatAverage - otherAverage).coerceAtLeast(0f)
                val coverage = downbeatCount.coerceAtMost(5) / 5f
                bestScore = max(bestScore, contrast * coverage)
            }
            return bestScore
        }

        val threeScore = scoreMeter(3)
        val fourScore = scoreMeter(4)
        return when {
            threeScore > 0.0045f && threeScore > fourScore * 1.28f -> 3
            fourScore > 0.0045f && fourScore > threeScore * 1.18f -> 4
            else -> currentMeter
        }
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

private class SongKeyTracker {
    private val chordHistory = mutableListOf<SongChordVote>()
    private val progression = mutableListOf<String>()
    private var currentSuggestion: String? = null
    private var pendingProgressionChord: String? = null
    private var pendingProgressionCount = 0

    fun update(
        chordLabels: List<String>,
        scalarKey: String?,
        scalarConfidence: Float,
    ): SongContext {
        chordLabels.take(3)
            .mapIndexedNotNull { index, label -> label.toSongChordVote(weight = 1f / (index + 1f)) }
            .forEach { chord ->
                chordHistory += chord
            }
        updateProgression(chordLabels.firstOrNull())
        if (chordLabels.isNotEmpty()) {
            while (chordHistory.size > SONG_CONTEXT_CHORD_SAMPLE_COUNT) {
                chordHistory.removeAt(0)
            }
        }

        val chordSuggestion = suggestKey()
        currentSuggestion = chordSuggestion
            ?.withScalarMinorQuality(scalarKey)
            ?: currentSuggestion
        return SongContext(
            keySuggestion = currentSuggestion,
            progression = progression.toList(),
        )
    }

    fun clear() {
        chordHistory.clear()
        progression.clear()
        currentSuggestion = null
        pendingProgressionChord = null
        pendingProgressionCount = 0
    }

    private fun updateProgression(chordLabel: String?) {
        val coreChord = chordLabel?.toCoreProgressionChord() ?: return
        if (pendingProgressionChord == coreChord) {
            pendingProgressionCount += 1
        } else {
            pendingProgressionChord = coreChord
            pendingProgressionCount = 1
        }
        if (pendingProgressionCount < 3) return
        if (progression.lastOrNull() == coreChord) return
        if (progression.size >= 3 && progression.firstOrNull() == coreChord) return
        progression += coreChord
        while (progression.size > 8) {
            progression.removeAt(0)
        }
    }

    private fun suggestKey(): String? {
        if (chordHistory.size < SONG_CONTEXT_MIN_CHORD_SAMPLES) return currentSuggestion

        val rootCounts = FloatArray(SongKeyNoteClasses.size)
        val minorCounts = FloatArray(SongKeyNoteClasses.size)
        val majorCounts = FloatArray(SongKeyNoteClasses.size)
        val dominantCounts = FloatArray(SongKeyNoteClasses.size)
        chordHistory.forEachIndexed { index, chord ->
            val recency = if (chordHistory.size <= 1) {
                1f
            } else {
                index.toFloat() / (chordHistory.lastIndex)
            }
            val recencyWeight = 0.35f + recency * 1.65f
            val weightedVote = chord.weight * recencyWeight
            rootCounts[chord.rootIndex] += weightedVote
            when (chord.quality) {
                SongChordQuality.Minor -> minorCounts[chord.rootIndex] += weightedVote
                SongChordQuality.Major -> majorCounts[chord.rootIndex] += weightedVote
                SongChordQuality.Dominant -> dominantCounts[chord.rootIndex] += weightedVote
                SongChordQuality.Other -> Unit
            }
        }

        val usedRoots = rootCounts.count { it > 0f }
        if (usedRoots <= 2) {
            return currentSuggestion
        }

        val candidates = rootCounts.indices.flatMap { root ->
            listOf(
                SongKeyCandidate(
                    label = SongKeyNoteClasses[root],
                    rootIndex = root,
                    score = majorFunctionScore(root, rootCounts, minorCounts, majorCounts, dominantCounts),
                ),
                SongKeyCandidate(
                    label = "${SongKeyNoteClasses[root]}m",
                    rootIndex = root,
                    score = minorFunctionScore(root, rootCounts, minorCounts, majorCounts, dominantCounts),
                ),
            )
        }.sortedByDescending { it.score }
        val best = candidates.firstOrNull() ?: return currentSuggestion
        val runnerUp = candidates.drop(1).firstOrNull { it.rootIndex != best.rootIndex } ?: return currentSuggestion
        val totalRootWeight = rootCounts.sum().coerceAtLeast(0.01f)
        val tonicSupport = rootCounts[best.rootIndex] / totalRootWeight
        if (best.score < runnerUp.score * 1.16f || tonicSupport < 0.12f) {
            return currentSuggestion
        }
        return best.label
    }
}

private data class SongContext(
    val keySuggestion: String?,
    val progression: List<String>,
)

private data class SongKeyCandidate(
    val label: String,
    val rootIndex: Int,
    val score: Float,
)

private fun majorFunctionScore(
    root: Int,
    rootCounts: FloatArray,
    minorCounts: FloatArray,
    majorCounts: FloatArray,
    dominantCounts: FloatArray,
): Float {
    val secondRoot = (root + 2).floorMod(SongKeyNoteClasses.size)
    val fourthRoot = (root + 5).floorMod(SongKeyNoteClasses.size)
    val fifthRoot = (root + 7).floorMod(SongKeyNoteClasses.size)
    val sixthRoot = (root + 9).floorMod(SongKeyNoteClasses.size)
    return rootCounts[root] * 2.4f +
        majorCounts[root] * 2.6f +
        (rootCounts[fourthRoot] + majorCounts[fourthRoot]) * 1.8f +
        (rootCounts[fifthRoot] + majorCounts[fifthRoot] + dominantCounts[fifthRoot] * 1.35f) * 2.1f +
        minorCounts[sixthRoot] * 2.0f +
        minorCounts[secondRoot] * 0.35f +
        dominantCounts[secondRoot] * 0.2f
}

private fun minorFunctionScore(
    root: Int,
    rootCounts: FloatArray,
    minorCounts: FloatArray,
    majorCounts: FloatArray,
    dominantCounts: FloatArray,
): Float {
    val fourthRoot = (root + 5).floorMod(SongKeyNoteClasses.size)
    val fifthRoot = (root + 7).floorMod(SongKeyNoteClasses.size)
    val flatSixRoot = (root + 8).floorMod(SongKeyNoteClasses.size)
    val flatSevenRoot = (root + 10).floorMod(SongKeyNoteClasses.size)
    return rootCounts[root] * 2.2f +
        minorCounts[root] * 3.0f +
        minorCounts[fourthRoot] * 1.6f +
        (minorCounts[fifthRoot] + dominantCounts[fifthRoot] * 1.15f) * 1.8f +
        majorCounts[flatSixRoot] * 1.1f +
        majorCounts[flatSevenRoot] * 1.25f +
        dominantCounts[root] * 0.35f
}

private data class SongChordVote(
    val rootIndex: Int,
    val quality: SongChordQuality,
    val weight: Float,
)

private enum class SongChordQuality {
    Major,
    Minor,
    Dominant,
    Other,
}

private val SongKeyNoteClasses = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val SongFlatNoteClassAliases = mapOf(
    "Db" to "C#",
    "Eb" to "D#",
    "Gb" to "F#",
    "Ab" to "G#",
    "Bb" to "A#",
)

private fun String.toSongChordVote(weight: Float): SongChordVote? {
    val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
    val normalizedRoot = SongFlatNoteClassAliases[root] ?: root
    val rootIndex = SongKeyNoteClasses.indexOf(normalizedRoot)
    if (rootIndex < 0) return null
    val suffix = removePrefix(root)
    val quality = when {
        suffix.startsWith("m") -> SongChordQuality.Minor
        suffix.startsWith("7") -> SongChordQuality.Dominant
        suffix.isBlank() -> SongChordQuality.Major
        suffix.contains("sus") -> SongChordQuality.Other
        suffix.startsWith("aug") || suffix.startsWith("dim") -> SongChordQuality.Other
        else -> SongChordQuality.Major
    }
    return SongChordVote(rootIndex = rootIndex, quality = quality, weight = weight)
}

private fun String.toCoreProgressionChord(): String? {
    val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
    val normalizedRoot = SongFlatNoteClassAliases[root] ?: root
    if (SongKeyNoteClasses.indexOf(normalizedRoot) < 0) return null
    val suffix = removePrefix(root)
    val coreSuffix = when {
        suffix.startsWith("m7b5") -> "m7b5"
        suffix.startsWith("dim") -> "dim"
        suffix.startsWith("aug") -> "aug"
        suffix.startsWith("m") -> "m"
        suffix.contains("sus") && suffix.contains("7") -> "7sus"
        suffix.contains("sus") -> "sus"
        suffix.contains("7") -> "7"
        else -> ""
    }
    return "$root$coreSuffix"
}

private fun String.withScalarMinorQuality(scalarKey: String?): String {
    if (contains("m")) return this
    val scalar = scalarKey ?: return this
    if (!scalar.contains("m")) return this
    return if (keyRoot() == scalar.keyRoot()) "${this}m" else this
}

private fun Int.songKeyLabel(
    minorScore: Float,
    majorScore: Float,
    bluesSupport: Float,
): String {
    val root = SongKeyNoteClasses[this]
    return if (minorScore >= majorScore * 0.85f) {
        "${root}m"
    } else {
        root
    }
}

private fun auxiliaryTempoFallback(
    bassBpm: Int?,
    snareBpm: Int?,
    fluxBpm: Int?,
): Int? {
    val votes = listOfNotNull(snareBpm, bassBpm, fluxBpm)
    if (votes.isEmpty()) return null
    val bestCluster = votes
        .map { seed -> votes.filter { bpm -> abs(bpm - seed) <= 8 } }
        .maxByOrNull { it.size } ?: return votes.firstOrNull()
    return bestCluster.average().roundToInt()
}

private data class MusicalTempoInterpretation(
    val bpm: Int?,
    val feelLabel: String,
    val meterLabel: String,
)

private fun interpretMusicalTempo(
    detectedBpm: Int?,
    meter: Int,
    confidence: Float,
): MusicalTempoInterpretation {
    if (detectedBpm == null) {
        return MusicalTempoInterpretation(null, "", "")
    }

    val likelyCompoundShuffle = meter == 3 &&
        detectedBpm in 88..132 &&
        confidence >= 0.25f
    if (likelyCompoundShuffle) {
        return MusicalTempoInterpretation(
            bpm = (detectedBpm / 2f).roundToInt(),
            feelLabel = "Slow shuffle",
            meterLabel = "12/8",
        )
    }

    return MusicalTempoInterpretation(
        bpm = detectedBpm,
        feelLabel = "",
        meterLabel = "$meter/4",
    )
}

private data class SmartTempoVote(
    val bpm: Int?,
    val confidence: Float,
)

private class AuxiliaryTempoEngine(
    private val levelThresholdFloor: Float,
    private val jumpThresholdFloor: Float,
    private val smoothing: Float,
) {
    private val onsetTimesMs = mutableListOf<Long>()
    private var smoothedLevel = 0f
    private var previousLevel = 0f
    private var lastOnsetMs = 0L
    private var stableTempoBpm: Int? = null

    fun estimate(level: Float, audioTimeMs: Long): Int? {
        val safeLevel = level.coerceIn(0f, 1f)
        if (lastOnsetMs > 0L && audioTimeMs - lastOnsetMs > TEMPO_DETECTION_TIMEOUT_MS) {
            clear()
        }
        smoothedLevel = if (smoothedLevel <= 0f) {
            safeLevel
        } else {
            (smoothedLevel * smoothing) + (safeLevel * (1f - smoothing))
        }

        val levelJump = safeLevel - previousLevel
        val adaptiveLevelThreshold = max(levelThresholdFloor, smoothedLevel * 1.38f)
        val adaptiveJumpThreshold = max(jumpThresholdFloor, smoothedLevel * 0.22f)
        val isOnset = safeLevel > adaptiveLevelThreshold &&
            levelJump > adaptiveJumpThreshold &&
            audioTimeMs - lastOnsetMs >= MIN_TEMPO_ONSET_INTERVAL_MS

        previousLevel = safeLevel
        if (!isOnset) return stableTempoBpm

        lastOnsetMs = audioTimeMs
        onsetTimesMs += audioTimeMs
        while (onsetTimesMs.size > TEMPO_ONSET_HISTORY_COUNT) {
            onsetTimesMs.removeAt(0)
        }
        estimateTempoFromIntervals()?.let { nextTempo ->
            stableTempoBpm = stableTempoBpm?.let { current ->
                if (abs(current - nextTempo) <= TEMPO_CLUSTER_TOLERANCE_BPM) {
                    ((current * 0.76f) + (nextTempo * 0.24f)).roundToInt()
                } else {
                    nextTempo
                }
            } ?: nextTempo
        }
        return stableTempoBpm
    }

    fun clear() {
        onsetTimesMs.clear()
        smoothedLevel = 0f
        previousLevel = 0f
        lastOnsetMs = 0L
        stableTempoBpm = null
    }

    private fun estimateTempoFromIntervals(): Int? {
        if (onsetTimesMs.size < 4) return null
        val intervals = onsetTimesMs
            .zipWithNext { previous, current -> current - previous }
            .filter { it in MIN_TEMPO_ONSET_INTERVAL_MS..MAX_TEMPO_ONSET_INTERVAL_MS }
            .takeLast(TEMPO_INTERVAL_WINDOW_COUNT)
        if (intervals.size < 3) return null

        val candidates = intervals.map { intervalMs ->
            (60_000f / intervalMs).roundToInt()
                .normalizedDetectedTempo()
                .coerceIn(MIN_BPM, MAX_BPM)
        }
        val median = candidates.sorted()[candidates.size / 2]
        val clustered = candidates.filter { bpm -> abs(bpm - median) <= TEMPO_CLUSTER_TOLERANCE_BPM }
        if (clustered.size < 3) return null
        return clustered.average().roundToInt()
    }
}

private fun combineTempoVotes(
    strictBpm: Int?,
    bassBpm: Int?,
    snareBpm: Int?,
    fluxBpm: Int?,
): SmartTempoVote {
    val votes = listOfNotNull(
        strictBpm?.let { TempoVote(it, 2.0f) },
        bassBpm?.let { TempoVote(it, 1.1f) },
        snareBpm?.let { TempoVote(it, 1.15f) },
        fluxBpm?.let { TempoVote(it, 1.3f) },
    )
    if (votes.isEmpty()) return SmartTempoVote(null, 0f)

    val bestCluster = votes
        .map { seed ->
            votes.filter { vote -> abs(vote.bpm - seed.bpm) <= TEMPO_CLUSTER_TOLERANCE_BPM }
        }
        .maxByOrNull { cluster -> cluster.sumOf { it.weight.toDouble() } } ?: return SmartTempoVote(null, 0f)
    val score = bestCluster.sumOf { it.weight.toDouble() }.toFloat()
    val totalScore = votes.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
    val confidence = (score / totalScore).coerceIn(0f, 1f)
    if (score < 2.3f) return SmartTempoVote(strictBpm, confidence * 0.55f)

    val weightedTempo = bestCluster.sumOf { vote -> vote.bpm.toDouble() * vote.weight } /
        bestCluster.sumOf { vote -> vote.weight.toDouble() }
    return SmartTempoVote(weightedTempo.roundToInt(), confidence)
}

private data class TempoVote(
    val bpm: Int,
    val weight: Float,
)

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

private fun Float.estimatedA4Reference(): Float? {
    if (this <= 0f) return null
    val nearestMidiAt440 = (69f + 12f * log2(this / DEFAULT_A4_REFERENCE_HZ)).roundToInt()
    val estimatedReference = this / 2f.pow((nearestMidiAt440 - 69) / 12f)
    return estimatedReference.takeIf {
        it in MIN_A4_REFERENCE_HZ.toFloat()..MAX_A4_REFERENCE_HZ.toFloat()
    }
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

private fun buildSpectrum(fftFrame: FftFrame): List<Float> {
    val rawLevels = List(SPECTRUM_BAR_COUNT) { index ->
        val frequency = spectrumFrequencyForIndex(index, SPECTRUM_BAR_COUNT)
        fftMagnitudeAt(fftFrame, frequency)
    }
    val peak = rawLevels.maxOrNull() ?: 0f
    if (peak < 0.0006f) return List(SPECTRUM_BAR_COUNT) { 0f }

    val floor = (rawLevels.average().toFloat() * 0.45f).coerceAtMost(peak * 0.72f)
    val range = (peak - floor).coerceAtLeast(0.0001f)
    return rawLevels.map { level ->
        sqrt(((level - floor) / range).coerceIn(0f, 1f))
    }
}

private fun dominantSpectrumNoteClasses(
    fftFrame: FftFrame,
    a4ReferenceHz: Int,
    minHz: Float,
    maxHz: Float,
    stableNoteClass: String?,
): List<String> {
    val classLevels = mutableMapOf<String, Float>()
    val startBin = (minHz / fftFrame.binHz).roundToInt().coerceIn(1, fftFrame.magnitudes.lastIndex)
    val endBin = (maxHz / fftFrame.binHz).roundToInt().coerceIn(startBin, fftFrame.magnitudes.lastIndex)
    for (bin in startBin..endBin) {
        val frequency = bin * fftFrame.binHz
        val magnitude = fftFrame.magnitudes[bin]
        if (magnitude <= 0f) continue
        val noteClass = frequency.toNoteReading(a4ReferenceHz).first.toNoteClass() ?: continue
        val weightedMagnitude = magnitude / sqrt((frequency / 110f).coerceAtLeast(1f))
        classLevels[noteClass] = (classLevels[noteClass] ?: 0f) + weightedMagnitude
    }

    val peak = classLevels.values.maxOrNull() ?: return emptyList()
    if (peak < 0.0008f) return emptyList()
    val harmonicClasses = stableNoteClass?.harmonicNoteClasses().orEmpty()
    return classLevels.entries
        .filter { (noteClass, level) ->
            val threshold = if (noteClass in harmonicClasses && noteClass != stableNoteClass) {
                peak * 0.42f
            } else {
                peak * 0.26f
            }
            level >= threshold
        }
        .sortedByDescending { it.value }
        .take(6)
        .map { it.key }
}

private fun String.harmonicNoteClasses(): Set<String> {
    val rootIndex = noteClassIndex() ?: return emptySet()
    return listOf(7, 4, 10, 2).map { offset ->
        TunerAnalyzerNoteClasses[(rootIndex + offset).floorMod(TunerAnalyzerNoteClasses.size)]
    }.toSet()
}

private fun String.noteClassIndex(): Int? {
    return TunerAnalyzerNoteClasses.indexOf(this).takeIf { it >= 0 }
}

private val TunerAnalyzerNoteClasses = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private fun fftBandLevel(
    fftFrame: FftFrame,
    minHz: Float,
    maxHz: Float,
    gain: Float,
): Float {
    val startBin = (minHz / fftFrame.binHz).roundToInt().coerceIn(1, fftFrame.magnitudes.lastIndex)
    val endBin = (maxHz / fftFrame.binHz).roundToInt().coerceIn(startBin, fftFrame.magnitudes.lastIndex)
    var sum = 0f
    var count = 0
    for (bin in startBin..endBin) {
        sum += fftFrame.magnitudes[bin]
        count += 1
    }
    if (count == 0) return 0f
    return (sum / count * gain).coerceIn(0f, 1f)
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

private fun fftMagnitudeAt(
    fftFrame: FftFrame,
    frequency: Float,
): Float {
    val bin = (frequency / fftFrame.binHz).roundToInt().coerceIn(1, fftFrame.magnitudes.lastIndex)
    return fftFrame.magnitudes[bin]
}

private fun buildFftFrame(
    buffer: ShortArray,
    sampleCount: Int,
): FftFrame {
    val size = AUDIO_FRAME_SIZE
    val real = FloatArray(size)
    val imaginary = FloatArray(size)
    val usableSamples = sampleCount.coerceIn(1, size)
    for (index in 0 until usableSamples) {
        val window = (0.5f - 0.5f * cos((2.0 * PI * index) / (size - 1)).toFloat())
        real[index] = (buffer[index] / Short.MAX_VALUE.toFloat()) * window
    }

    fft(real, imaginary)

    val magnitudes = FloatArray(size / 2)
    for (bin in magnitudes.indices) {
        magnitudes[bin] = (sqrt(real[bin] * real[bin] + imaginary[bin] * imaginary[bin]) * 2f / size)
            .coerceAtLeast(0f)
    }
    return FftFrame(
        magnitudes = magnitudes,
        binHz = AUDIO_SAMPLE_RATE.toFloat() / size,
    )
}

private fun fft(
    real: FloatArray,
    imaginary: FloatArray,
) {
    val size = real.size
    var j = 0
    for (i in 1 until size) {
        var bit = size shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            val tempReal = real[i]
            real[i] = real[j]
            real[j] = tempReal
            val tempImaginary = imaginary[i]
            imaginary[i] = imaginary[j]
            imaginary[j] = tempImaginary
        }
    }

    var length = 2
    while (length <= size) {
        val angle = (-2.0 * PI / length).toFloat()
        val wLengthReal = cos(angle)
        val wLengthImaginary = sin(angle)
        var start = 0
        while (start < size) {
            var wReal = 1f
            var wImaginary = 0f
            val halfLength = length / 2
            for (offset in 0 until halfLength) {
                val evenIndex = start + offset
                val oddIndex = evenIndex + halfLength
                val oddReal = real[oddIndex] * wReal - imaginary[oddIndex] * wImaginary
                val oddImaginary = real[oddIndex] * wImaginary + imaginary[oddIndex] * wReal
                real[oddIndex] = real[evenIndex] - oddReal
                imaginary[oddIndex] = imaginary[evenIndex] - oddImaginary
                real[evenIndex] += oddReal
                imaginary[evenIndex] += oddImaginary
                val nextWReal = wReal * wLengthReal - wImaginary * wLengthImaginary
                wImaginary = wReal * wLengthImaginary + wImaginary * wLengthReal
                wReal = nextWReal
            }
            start += length
        }
        length = length shl 1
    }
}

private fun String?.isMinorOrBluesKey(): Boolean {
    return this?.contains("m") == true || this?.contains("blues") == true
}

private fun String?.isLeadScaleKey(): Boolean {
    return this?.contains("m") == true
}

private fun String?.hasSameKeyRoot(other: String?): Boolean {
    return keyRoot() == other.keyRoot()
}

private fun String?.keyRoot(): String? {
    val key = this ?: return null
    val root = buildString {
        key.firstOrNull()?.takeIf { it in 'A'..'G' }?.let { append(it) }
        key.getOrNull(1)?.takeIf { it == '#' }?.let { append(it) }
    }
    return root.takeIf { it.isNotEmpty() }
}

private fun Float.debugLevel(): String {
    return ((this * 10_000f).roundToInt() / 10_000f).toString()
}
