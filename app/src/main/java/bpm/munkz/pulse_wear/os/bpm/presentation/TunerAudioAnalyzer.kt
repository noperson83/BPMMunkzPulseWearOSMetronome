package bpm.munkz.pulse_wear.os.bpm.presentation

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val AUDIO_SAMPLE_RATE = 44_100
private const val AUDIO_FRAME_SIZE = 2_048
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
private const val TUNER_KEY_SAMPLE_COUNT = 192
private const val SONG_CONTEXT_CHORD_SAMPLE_COUNT = 360
private const val SONG_CONTEXT_MIN_CHORD_SAMPLES = 10

private data class PitchAnalysisState(
    val frequencyHz: Float? = null,
    val noteName: String = "--",
    val cents: Int = 0,
    val recentNotes: List<String> = emptyList(),
    val guessedKey: String? = null,
    val keyConfidence: Float = 0f,
    val likelyChords: List<String> = emptyList(),
    val chordConfidence: Float = 0f,
    val alternateChord: String? = null,
    val chordCandidates: List<String> = emptyList(),
    val keyCandidates: List<String> = emptyList(),
    val chordTones: List<String> = emptyList(),
    val chordProgression: List<String> = emptyList(),
    val phraseLengthBars: Int? = null,
    val phraseConfidence: Float = 0f,
    val phraseBarIndex: Int = 0,
    val phraseLocked: Boolean = false,
    val phraseSource: String = "",
    val downbeatGuess: Int = 0,
    val downbeatConfidence: Float = 0f,
    val downbeatRoot: String? = null,
    val downbeatLandingStrength: Float = 0f,
    val harmonySummary: String = "",
    val songDebugLine: String = "",
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
        keyConfidence = keyConfidence,
        likelyChords = likelyChords,
        chordConfidence = chordConfidence,
        alternateChord = alternateChord,
        chordCandidates = chordCandidates,
        keyCandidates = keyCandidates,
        chordTones = chordTones,
        chordProgression = chordProgression,
        phraseLengthBars = phraseLengthBars,
        phraseConfidence = phraseConfidence,
        phraseBarIndex = phraseBarIndex,
        phraseLocked = phraseLocked,
        phraseSource = phraseSource,
        downbeatGuess = downbeatGuess,
        downbeatConfidence = downbeatConfidence,
        downbeatRoot = downbeatRoot,
        downbeatLandingStrength = downbeatLandingStrength,
        harmonySummary = harmonySummary,
        songDebugLine = songDebugLine,
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

    LaunchedEffect(enabled, listenRange, readerMode, a4ReferenceHz, includeSpectrum) {
        if (!enabled) {
            analysisState = AudioAnalysisState()
            return@LaunchedEffect
        }

        runAudioAnalyzer(
            listenRange = listenRange,
            readerMode = readerMode,
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
    readerMode: SpectrumReaderMode,
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
                    readerMode = readerMode,
                    pitchAverager = pitchAverager,
                    tempoEstimator = tempoEstimator,
                    a4ReferenceHz = a4ReferenceHz,
                    includeSpectrum = includeSpectrum,
                    analyzePitch = shouldAnalyzePitch,
                    previousPitchAnalysis = lastPitchAnalysis,
                )
                lastPitchAnalysis = analysis.toPitchAnalysisState()
                val nowElapsedMs = SystemClock.elapsedRealtime()
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
    readerMode: SpectrumReaderMode,
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
    val tempoAnalysis = tempoEstimator.estimate(
        strictLevel = transientLevel,
        bassLevel = bassPulseLevel,
        snareLevel = snarePulseLevel,
        fluxLevel = fluxPulseLevel,
        audioTimeMs = transientAudioTimeMs,
    )
    val pitchAnalysis = if (analyzePitch && level >= TUNER_MIN_PITCH_LEVEL) {
        val frequency = pitchAverager.average(detectPitchHz(buffer, sampleCount, listenRange))
        val rawNote = frequency?.toNoteReading(a4ReferenceHz)
        val stableNote = pitchAverager.stableNoteReading(
            noteReading = rawNote,
            audioTimeMs = frameStartAudioMs,
        )
        val note = if (readerMode == SpectrumReaderMode.Instrument) rawNote else stableNote
        val sensedA4Hz = if (rawNote != null) {
            pitchAverager.sensedA4Reference(frequency)
        } else {
            null
        }
        val rawSongPitchClassLevels = if (level >= TUNER_MIN_KEY_LEVEL) {
            spectrumPitchClassLevels(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = listenRange.minHz.coerceAtLeast(55f),
                maxHz = listenRange.maxHz.coerceAtMost(2_200f),
            )
        } else {
            FloatArray(TunerAnalyzerNoteClasses.size)
        }
        val subBassPitchClassLevels = if (level >= TUNER_MIN_KEY_LEVEL && readerMode == SpectrumReaderMode.Song) {
            spectrumPitchClassLevels(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = 45f,
                maxHz = 120f,
            )
        } else {
            FloatArray(TunerAnalyzerNoteClasses.size)
        }
        val bassPitchClassLevels = if (level >= TUNER_MIN_KEY_LEVEL && readerMode == SpectrumReaderMode.Song) {
            spectrumPitchClassLevels(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = 55f,
                maxHz = 240f,
            )
        } else {
            FloatArray(TunerAnalyzerNoteClasses.size)
        }
        val bassPitchClassOctaves = if (level >= TUNER_MIN_KEY_LEVEL && readerMode == SpectrumReaderMode.Song) {
            spectrumPitchClassOctaveCounts(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = 55f,
                maxHz = 240f,
            )
        } else {
            IntArray(TunerAnalyzerNoteClasses.size)
        }
        val lowMidPitchClassLevels = if (level >= TUNER_MIN_KEY_LEVEL && readerMode == SpectrumReaderMode.Song) {
            spectrumPitchClassLevels(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = 120f,
                maxHz = 520f,
            )
        } else {
            FloatArray(TunerAnalyzerNoteClasses.size)
        }
        val midPitchClassLevels = if (level >= TUNER_MIN_KEY_LEVEL && readerMode == SpectrumReaderMode.Song) {
            spectrumPitchClassLevels(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = 350f,
                maxHz = 1_500f,
            )
        } else {
            FloatArray(TunerAnalyzerNoteClasses.size)
        }
        val highPitchClassLevels = if (level >= TUNER_MIN_KEY_LEVEL && readerMode == SpectrumReaderMode.Song) {
            spectrumPitchClassLevels(
                fftFrame = fftFrame,
                a4ReferenceHz = a4ReferenceHz,
                minHz = 1_200f,
                maxHz = listenRange.maxHz.coerceAtMost(3_500f),
            )
        } else {
            FloatArray(TunerAnalyzerNoteClasses.size)
        }
        val songPitchClassLevels = if (readerMode == SpectrumReaderMode.Song) {
            cleanedSongChroma(
                subBassLevels = subBassPitchClassLevels,
                bassLevels = bassPitchClassLevels,
                lowMidLevels = lowMidPitchClassLevels,
                midLevels = midPitchClassLevels,
                highLevels = highPitchClassLevels,
                fullLevels = rawSongPitchClassLevels,
            )
        } else {
            rawSongPitchClassLevels
        }
        val keyAnalysis = if (level >= TUNER_MIN_KEY_LEVEL) {
            if (readerMode == SpectrumReaderMode.Instrument && note != null) {
                pitchAverager.tunerNoteSummary(note.first)
            } else if (readerMode == SpectrumReaderMode.Song) {
                val subBassRootEvidence = dominantChromaRootEvidence(
                    classLevels = subBassPitchClassLevels,
                    minPeak = 0.00032f,
                    dominanceRatio = 1.08f,
                )
                val bandBassRootEvidence = dominantChromaRootEvidence(
                    classLevels = bassPitchClassLevels,
                    octaveCounts = bassPitchClassOctaves,
                    minPeak = 0.00045f,
                    dominanceRatio = 1.12f,
                )
                val pitchRootEvidence = note
                    ?.first
                    ?.toNoteClass()
                    ?.let { noteClass -> ChordRootEvidence(noteClass, 0.74f) }
                val rootEvidence = selectSongRootEvidence(
                    subBass = subBassRootEvidence,
                    bass = bandBassRootEvidence,
                    pitch = pitchRootEvidence,
                )
                pitchAverager.rootThirdSongSummary(
                    rootNoteClass = rootEvidence?.label,
                    rootConfidence = rootEvidence?.confidence ?: 0f,
                    subBassRootClass = subBassRootEvidence?.label,
                    subBassRootConfidence = subBassRootEvidence?.confidence ?: 0f,
                    bandBassRootClass = bandBassRootEvidence?.label,
                    bandBassRootConfidence = bandBassRootEvidence?.confidence ?: 0f,
                    classLevels = songPitchClassLevels,
                    audioTimeMs = frameStartAudioMs,
                    tempoBpm = tempoAnalysis.detectedBpm,
                    activeBeatIndex = tempoAnalysis.activeBeatIndex,
                    meter = 4,
                    tempoConfident = tempoAnalysis.confident,
                )
            } else {
                pitchAverager.currentKeySummary()
            }
        } else {
            pitchAverager.currentKeySummary()
        }
        PitchAnalysisState(
            frequencyHz = frequency,
            noteName = note?.first ?: "--",
            cents = note?.second ?: 0,
            recentNotes = keyAnalysis.recentNotes,
            guessedKey = keyAnalysis.guessedKey,
            keyConfidence = keyAnalysis.keyConfidence,
            likelyChords = keyAnalysis.likelyChords,
            chordConfidence = keyAnalysis.chordConfidence,
            alternateChord = keyAnalysis.alternateChord,
            chordCandidates = keyAnalysis.chordCandidates,
            keyCandidates = keyAnalysis.keyCandidates,
            chordTones = keyAnalysis.chordTones,
            chordProgression = keyAnalysis.chordProgression,
            phraseLengthBars = keyAnalysis.phraseLengthBars,
            phraseConfidence = keyAnalysis.phraseConfidence,
            phraseBarIndex = keyAnalysis.phraseBarIndex,
            phraseLocked = keyAnalysis.phraseLocked,
            phraseSource = keyAnalysis.phraseSource,
            downbeatGuess = keyAnalysis.downbeatGuess,
            downbeatConfidence = keyAnalysis.downbeatConfidence,
            downbeatRoot = keyAnalysis.downbeatRoot,
            downbeatLandingStrength = keyAnalysis.downbeatLandingStrength,
            harmonySummary = keyAnalysis.harmonySummary,
            songDebugLine = keyAnalysis.songDebugLine,
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
        keyConfidence = pitchAnalysis.keyConfidence,
        likelyChords = pitchAnalysis.likelyChords,
        chordConfidence = pitchAnalysis.chordConfidence,
        alternateChord = pitchAnalysis.alternateChord,
        chordCandidates = pitchAnalysis.chordCandidates,
        keyCandidates = pitchAnalysis.keyCandidates,
        chordTones = pitchAnalysis.chordTones,
        chordProgression = pitchAnalysis.chordProgression,
        phraseLengthBars = pitchAnalysis.phraseLengthBars,
        phraseConfidence = pitchAnalysis.phraseConfidence,
        phraseBarIndex = pitchAnalysis.phraseBarIndex,
        phraseLocked = pitchAnalysis.phraseLocked,
        phraseSource = pitchAnalysis.phraseSource,
        downbeatGuess = pitchAnalysis.downbeatGuess,
        downbeatConfidence = pitchAnalysis.downbeatConfidence,
        downbeatRoot = pitchAnalysis.downbeatRoot,
        downbeatLandingStrength = pitchAnalysis.downbeatLandingStrength,
        harmonySummary = pitchAnalysis.harmonySummary,
        songDebugLine = pitchAnalysis.songDebugLine,
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
    private val rootThirdSongTracker = RootThirdSongTracker()

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

    fun tunerNoteSummary(noteName: String?): KeyAnalysis {
        noteName
            ?.toNoteClass()
            ?.let { noteClass ->
                recentNoteClasses += noteClass
                while (recentNoteClasses.size > TUNER_KEY_SAMPLE_COUNT) {
                    recentNoteClasses.removeAt(0)
                }
            }
        return KeyAnalysis(
            recentNotes = recentNoteClasses.takeLast(8),
            guessedKey = null,
            likelyChords = emptyList(),
            chordTones = emptyList(),
            chordProgression = emptyList(),
        )
    }

    fun rootThirdSongSummary(
        rootNoteClass: String?,
        rootConfidence: Float = 0f,
        subBassRootClass: String? = null,
        subBassRootConfidence: Float = 0f,
        bandBassRootClass: String? = null,
        bandBassRootConfidence: Float = 0f,
        classLevels: FloatArray,
        audioTimeMs: Long,
        tempoBpm: Int?,
        activeBeatIndex: Int,
        meter: Int,
        tempoConfident: Boolean,
    ): KeyAnalysis {
        val rootIndex = rootNoteClass
            ?.let { TunerAnalyzerNoteClasses.indexOf(it) }
            ?.takeIf { it >= 0 }

        if (rootIndex != null) {
            recentNoteClasses += TunerAnalyzerNoteClasses[rootIndex]
            while (recentNoteClasses.size > TUNER_KEY_SAMPLE_COUNT) {
                recentNoteClasses.removeAt(0)
            }
        }

        val songContext = rootThirdSongTracker.update(
            preferredRootIndex = rootIndex,
            preferredRootConfidence = rootConfidence,
            subBassRootClass = subBassRootClass,
            subBassRootConfidence = subBassRootConfidence,
            bandBassRootClass = bandBassRootClass,
            bandBassRootConfidence = bandBassRootConfidence,
            classLevels = classLevels,
            audioTimeMs = audioTimeMs,
            tempoBpm = tempoBpm,
            activeBeatIndex = activeBeatIndex,
            meter = meter,
            tempoConfident = tempoConfident,
        )
        return KeyAnalysis(
            recentNotes = recentNoteClasses.takeLast(8),
            guessedKey = songContext.keySuggestion,
            keyConfidence = songContext.keyConfidence,
            likelyChords = listOfNotNull(songContext.liveChord),
            chordConfidence = songContext.chordConfidence,
            alternateChord = songContext.alternateChord,
            chordCandidates = songContext.chordCandidates,
            keyCandidates = songContext.keyCandidates,
            chordTones = songContext.chordTones,
            chordProgression = songContext.progression,
            phraseLengthBars = songContext.phraseLengthBars,
            phraseConfidence = songContext.phraseConfidence,
            phraseBarIndex = songContext.phraseBarIndex,
            phraseLocked = songContext.phraseLocked,
            phraseSource = songContext.phraseSource,
            downbeatGuess = songContext.downbeatGuess,
            downbeatConfidence = songContext.downbeatConfidence,
            downbeatRoot = songContext.downbeatRoot,
            downbeatLandingStrength = songContext.downbeatLandingStrength,
            harmonySummary = songContext.harmonySummary,
            songDebugLine = songContext.debugLine,
        )
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
        return KeyAnalysis(
            recentNotes = recentNoteClasses.takeLast(8),
            guessedKey = null,
            likelyChords = emptyList(),
            chordTones = emptyList(),
            chordProgression = emptyList(),
        )
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
        rootThirdSongTracker.clear()
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

private data class RootThirdChord(
    val rootIndex: Int,
    val label: String,
    val tones: List<String>,
    val confident: Boolean,
    val score: Float = 0f,
)

private data class ChordCandidate(
    val chord: RootThirdChord,
    val emissionScore: Float,
)

private data class SongKeyEstimate(
    val label: String,
    val confidence: Float,
)

internal data class FunctionalHarmonyInfo(
    val roman: String,
    val function: String,
    val tags: List<String>,
)

private data class RootThirdSongContext(
    val liveChord: String?,
    val chordConfidence: Float,
    val alternateChord: String?,
    val chordCandidates: List<String>,
    val keySuggestion: String?,
    val keyConfidence: Float,
    val keyCandidates: List<String>,
    val chordTones: List<String>,
    val progression: List<String>,
    val phraseLengthBars: Int?,
    val phraseConfidence: Float,
    val phraseBarIndex: Int,
    val phraseLocked: Boolean,
    val phraseSource: String,
    val downbeatGuess: Int,
    val downbeatConfidence: Float,
    val downbeatRoot: String?,
    val downbeatLandingStrength: Float,
    val harmonySummary: String,
    val debugLine: String,
)

private data class ChordRootHint(
    val rootIndex: Int,
    val confidence: Float,
)

private data class DownbeatLandingState(
    val beatNumber: Int = 0,
    val confidence: Float = 0f,
    val root: String? = null,
    val landingStrength: Float = 0f,
)

private data class BeatChromaSnapshot(
    val currentChroma: FloatArray,
    val committedChroma: FloatArray?,
)

private class DownbeatLandingTracker {
    private val beatScores = FloatArray(8)
    private val beatRoots = Array(8) { FloatArray(TunerAnalyzerNoteClasses.size) }
    private var observationCount = 0

    fun update(
        activeBeatIndex: Int,
        meter: Int,
        tempoConfident: Boolean,
        rootHint: ChordRootHint?,
        classLevels: FloatArray,
    ): DownbeatLandingState {
        val safeMeter = meter.coerceIn(2, 8)
        if (!tempoConfident || activeBeatIndex !in 0 until safeMeter) {
            return currentState(safeMeter)
        }

        val peak = classLevels.maxOrNull()?.coerceAtLeast(0.0001f) ?: 0.0001f
        val rootIndex = rootHint?.rootIndex
        val rootStrength = rootIndex
            ?.let { index -> (classLevels.getOrNull(index) ?: 0f) / peak }
            ?: 0f
        val rootConfidence = rootHint?.confidence ?: 0f
        val energyStrength = (peak / 3.0f).coerceIn(0f, 1f)
        val landingStrength = (
            rootConfidence * 0.4f +
                rootStrength.coerceIn(0f, 1f) * 0.2f +
                energyStrength * 0.4f
            )
            .coerceIn(0f, 1f)
        if (landingStrength <= 0.08f) return currentState(safeMeter)

        decay()
        beatScores[activeBeatIndex] += landingStrength
        if (rootIndex != null) {
            beatRoots[activeBeatIndex][rootIndex] += landingStrength
        }
        observationCount = (observationCount + 1).coerceAtMost(128)
        return currentState(safeMeter)
    }

    fun clear() {
        beatScores.fill(0f)
        beatRoots.forEach { roots -> roots.fill(0f) }
        observationCount = 0
    }

    private fun currentState(meter: Int): DownbeatLandingState {
        if (observationCount < 3) return DownbeatLandingState()
        val bestIndex = (0 until meter).maxByOrNull { index -> beatScores[index] } ?: return DownbeatLandingState()
        val best = beatScores[bestIndex]
        if (best <= 0.01f) return DownbeatLandingState()
        val runnerUp = (0 until meter)
            .filter { index -> index != bestIndex }
            .maxOfOrNull { index -> beatScores[index] }
            ?: 0f
        val total = (0 until meter).sumOf { index -> beatScores[index].toDouble() }.toFloat().coerceAtLeast(0.01f)
        val share = best / total
        val separation = if (runnerUp <= 0f) 1f else ((best / runnerUp) - 1f) / 1.4f
        val confidence = (share * 0.62f + separation.coerceIn(0f, 1f) * 0.38f)
            .coerceIn(0f, 1f)
        val rootIndex = beatRoots[bestIndex].indices.maxByOrNull { index -> beatRoots[bestIndex][index] }
        val root = rootIndex
            ?.takeIf { index -> beatRoots[bestIndex][index] > 0.01f }
            ?.let { index -> TunerAnalyzerNoteClasses[index] }
        return DownbeatLandingState(
            beatNumber = bestIndex + 1,
            confidence = confidence,
            root = root,
            landingStrength = best,
        )
    }

    private fun decay() {
        for (index in beatScores.indices) {
            beatScores[index] *= 0.985f
            for (rootIndex in beatRoots[index].indices) {
                beatRoots[index][rootIndex] *= 0.985f
            }
        }
    }
}

private class RootThirdSongTracker {
    private val chordVotes = mutableListOf<RootThirdChord>()
    private val bassRootBarTracker = BassRootBarTracker()
    private val beatChromaTracker = BeatChromaTracker()
    private val downbeatLandingTracker = DownbeatLandingTracker()
    private val chordSmoother = ChordSequenceSmoother()
    private val barTracker = BarChordTracker()
    private val structureDetector = StructurePhraseDetector()
    private val progression = mutableListOf<String>()
    private val learningProgression = mutableListOf<String>()
    private val keyChromaSum = FloatArray(TunerAnalyzerNoteClasses.size)
    private var structureState = StructurePhraseState()
    private var stableChord: RootThirdChord? = null
    private var pendingChord: RootThirdChord? = null
    private var pendingChordCount = 0
    private var pendingProgressionChord: String? = null
    private var pendingProgressionCount = 0
    private var learningLoopLength: Int? = null
    private var learningLoopNextIndex = 0
    private var stableKey: String? = null
    private var keyFrameCount = 0
    private var lastKeyChromaAudioMs = 0L

    fun update(
        preferredRootIndex: Int?,
        preferredRootConfidence: Float,
        subBassRootClass: String?,
        subBassRootConfidence: Float,
        bandBassRootClass: String?,
        bandBassRootConfidence: Float,
        classLevels: FloatArray,
        audioTimeMs: Long,
        tempoBpm: Int?,
        activeBeatIndex: Int,
        meter: Int,
        tempoConfident: Boolean,
    ): RootThirdSongContext {
        val instantRootHint = preferredRootIndex?.let { rootIndex ->
            ChordRootHint(
                rootIndex = rootIndex,
                confidence = preferredRootConfidence.coerceIn(0f, 1f),
            )
        }
        val barRootHint = bassRootBarTracker.update(
            instantRootHint = instantRootHint,
            audioTimeMs = audioTimeMs,
            tempoBpm = tempoBpm,
            meter = meter,
            tempoConfident = tempoConfident,
        )
        val beatChroma = beatChromaTracker.update(
            classLevels = classLevels,
            audioTimeMs = audioTimeMs,
            tempoBpm = tempoBpm,
            tempoConfident = tempoConfident,
        )
        beatChroma.committedChroma?.let { committedChroma ->
            observeKeyChroma(committedChroma)
            lastKeyChromaAudioMs = audioTimeMs
        } ?: run {
            if (!tempoConfident && audioTimeMs - lastKeyChromaAudioMs >= 500L) {
                observeKeyChroma(beatChroma.currentChroma)
                lastKeyChromaAudioMs = audioTimeMs
            }
        }
        val chordCandidates = matchChromaChordCandidates(
            classLevels = beatChroma.currentChroma,
            preferredRootIndex = barRootHint?.rootIndex,
            preferredRootConfidence = barRootHint?.confidence ?: 0f,
        )
        val rootGroundedCandidates = chordCandidates.groundedByRootEvidence(barRootHint?.confidence ?: 0f)
        val songCoreCandidates = rootGroundedCandidates.preferMajorMinorCoreHarmony(beatChroma.currentChroma)
        val smoothedChord = chordSmoother.update(songCoreCandidates)
        val displayRootHint = barRootHint
            ?: instantRootHint?.takeIf { hint -> hint.confidence >= 0.62f }
        val chord = rootAnchoredDisplayChord(
            rootHint = displayRootHint,
            classLevels = beatChroma.currentChroma,
            candidate = smoothedChord,
        )
        val downbeatState = downbeatLandingTracker.update(
            activeBeatIndex = activeBeatIndex,
            meter = meter,
            tempoConfident = tempoConfident,
            rootHint = instantRootHint,
            classLevels = classLevels,
        )
        val displayedChordCandidates = (
            listOfNotNull(chord?.label, smoothedChord?.label) +
                songCoreCandidates.map { it.chord.label } +
                chordCandidates.map { it.chord.label }
            )
            .distinct()
            .take(8)
        val alternateChord = chordCandidates
            .map { it.chord }
            .firstOrNull { candidate -> candidate.label != chord?.label }
        val observedChord = chord?.let { nextChord ->
            val stable = stableChord
            if (
                stable != null &&
                stable.rootIndex == nextChord.rootIndex &&
                stable.label != nextChord.label &&
                stable.toMajorMinorCoreChord().label == nextChord.toMajorMinorCoreChord().label
            ) {
                nextChord.asUncertainRoot()
            } else {
                nextChord
            }
        }
        var committedBarObservation: BarChordObservation? = null
        if (observedChord != null) {
            if (pendingChord?.label == observedChord.label) {
                pendingChordCount += 1
            } else {
                pendingChord = observedChord
                pendingChordCount = 1
            }

            if (pendingChordCount >= 2) {
                stableChord = observedChord
                if (observedChord.confident) {
                    observeLearningProgression(observedChord)
                    chordVotes += observedChord
                    while (chordVotes.size > 96) {
                        chordVotes.removeAt(0)
                    }
                    committedBarObservation = barTracker.update(
                        chord = observedChord,
                        classLevels = beatChroma.currentChroma,
                        instantRootHint = instantRootHint,
                        audioTimeMs = audioTimeMs,
                        tempoBpm = tempoBpm,
                        meter = meter,
                        tempoConfident = tempoConfident,
                    )
                    if (committedBarObservation != null) {
                        structureState = structureDetector.observe(committedBarObservation)
                        progression.clear()
                        progression += structureState.displayChords
                    }
                } else {
                    removeProgressionRoot(observedChord.rootIndex)
                }
            }
        }

        val displayProgression = when {
            structureState.phraseLengthBars != null -> progression
            learningProgression.isNotEmpty() -> learningProgression
            else -> progression
        }
        val phraseSource = when {
            structureState.phraseLengthBars != null -> "Lock"
            learningProgression.isNotEmpty() -> "Learn"
            progression.isNotEmpty() -> "Bars"
            else -> ""
        }
        val phraseBarIndex = when {
            structureState.phraseLengthBars != null -> structureState.currentBarIndex
            displayProgression.isNotEmpty() -> displayProgression.size
            else -> 0
        }
        val keyCandidates = estimateSongKeyCandidates(
            chroma = keyChromaSum,
            chordHistory = chordVotes,
            progression = displayProgression,
        )
        val keyEstimate = selectStableSongKey(keyCandidates)
        stableKey = keyEstimate?.label ?: dominantKeySuggestion() ?: stableKey
        val liveChord = stableChord
        val harmonyInfo = functionalHarmonyInfo(stableKey, liveChord?.label)
        val harmonySummary = harmonyInfo?.compactHarmonySummary().orEmpty()
        val debugLine = songPipelineDebugLine(
            bassRootClass = instantRootHint?.rootIndex?.let { TunerAnalyzerNoteClasses[it] },
            rootConfidence = instantRootHint?.confidence ?: 0f,
            barRootClass = barRootHint?.rootIndex?.let { TunerAnalyzerNoteClasses[it] },
            chord = chord?.label,
            observedChord = observedChord?.label,
            stableChord = liveChord?.label,
            committedBarChord = committedBarObservation?.chord?.label,
            phraseLengthBars = structureState.phraseLengthBars,
            phraseConfidence = structureState.phraseConfidence,
            phraseBarIndex = phraseBarIndex,
            phraseSource = phraseSource,
            downbeatState = downbeatState,
            progressionSize = displayProgression.size,
            candidates = displayedChordCandidates,
            keyCandidates = keyCandidates,
            harmonySummary = harmonySummary,
        )
        return RootThirdSongContext(
            liveChord = liveChord?.label,
            chordConfidence = liveChord?.displayConfidence() ?: 0f,
            alternateChord = alternateChord?.label,
            chordCandidates = displayedChordCandidates,
            keySuggestion = stableKey,
            keyConfidence = keyEstimate?.confidence ?: if (stableKey != null) 0.34f else 0f,
            keyCandidates = keyCandidates.take(4).map { estimate -> "${estimate.label} ${twoDecimals(estimate.confidence)}" },
            chordTones = liveChord?.tones.orEmpty(),
            progression = displayProgression.toList(),
            phraseLengthBars = structureState.phraseLengthBars,
            phraseConfidence = structureState.phraseConfidence,
            phraseBarIndex = phraseBarIndex,
            phraseLocked = structureState.phraseLengthBars != null,
            phraseSource = phraseSource,
            downbeatGuess = downbeatState.beatNumber,
            downbeatConfidence = downbeatState.confidence,
            downbeatRoot = downbeatState.root,
            downbeatLandingStrength = downbeatState.landingStrength,
            harmonySummary = harmonySummary,
            debugLine = debugLine,
        )
    }

    fun clear() {
        chordVotes.clear()
        bassRootBarTracker.clear()
        beatChromaTracker.clear()
        downbeatLandingTracker.clear()
        chordSmoother.clear()
        barTracker.clear()
        structureDetector.clear()
        progression.clear()
        learningProgression.clear()
        keyChromaSum.fill(0f)
        structureState = StructurePhraseState()
        stableChord = null
        pendingChord = null
        pendingChordCount = 0
        pendingProgressionChord = null
        pendingProgressionCount = 0
        learningLoopLength = null
        learningLoopNextIndex = 0
        stableKey = null
        keyFrameCount = 0
        lastKeyChromaAudioMs = 0L
    }

    private fun observeKeyChroma(classLevels: FloatArray) {
        val peak = classLevels.maxOrNull() ?: return
        if (peak < 0.0008f) return
        if (keyFrameCount > 720) {
            for (index in keyChromaSum.indices) {
                keyChromaSum[index] *= 0.72f
            }
            keyFrameCount = (keyFrameCount * 0.72f).roundToInt()
        }
        classLevels.forEachIndexed { index, level ->
            keyChromaSum[index] += sqrt((level / peak).coerceAtLeast(0f))
        }
        keyFrameCount += 1
    }

    private fun removeProgressionRoot(rootIndex: Int) {
        structureState = structureDetector.removeRoot(rootIndex)
        progression.clear()
        progression += structureState.displayChords
    }

    private fun songPipelineDebugLine(
        bassRootClass: String?,
        rootConfidence: Float,
        barRootClass: String?,
        chord: String?,
        observedChord: String?,
        stableChord: String?,
        committedBarChord: String?,
        phraseLengthBars: Int?,
        phraseConfidence: Float,
        phraseBarIndex: Int,
        phraseSource: String,
        downbeatState: DownbeatLandingState,
        progressionSize: Int,
        candidates: List<String>,
        keyCandidates: List<SongKeyEstimate>,
        harmonySummary: String,
    ): String {
        val rootText = bassRootClass?.let { root -> "$root ${twoDecimals(rootConfidence)}" } ?: "--"
        val phraseText = phraseLengthBars?.let { bars -> "${bars}b ${twoDecimals(phraseConfidence)}" } ?: "learn"
        val slotText = phraseBarIndex.takeIf { it > 0 }?.let { "#$it" } ?: "--"
        val oneText = downbeatState.beatNumber.takeIf { it > 0 }?.let { beat ->
            "1->$beat:${twoDecimals(downbeatState.confidence)}:${downbeatState.root ?: "--"}"
        } ?: "1->--"
        val chordText = candidates.take(4).joinToString("/")
        val keyText = keyCandidates.take(3).joinToString("/") { estimate ->
            "${estimate.label}:${twoDecimals(estimate.confidence)}"
        }
        val harmonyText = harmonySummary.takeIf { it.isNotBlank() } ?: "--"
        return "R$rootText BR${barRootClass ?: "--"} raw${chord ?: "--"} obs${observedChord ?: "--"} st${stableChord ?: "--"} bar${committedBarChord ?: "--"} P${phraseSource.ifBlank { "--" }}:$phraseText:$slotText/$progressionSize $oneText C[$chordText] K[$keyText] H[$harmonyText]"
    }

    private fun observeLearningProgression(
        chord: RootThirdChord,
        minConfirmFrames: Int = 1,
    ) {
        val core = chord.toMajorMinorCoreChord().label
        if (pendingProgressionChord == core) {
            pendingProgressionCount += 1
        } else {
            pendingProgressionChord = core
            pendingProgressionCount = 1
        }
        if (pendingProgressionCount < minConfirmFrames.coerceAtLeast(1)) return
        if (learningProgression.lastOrNull() == core) return
        val lockedLength = learningLoopLength
        if (lockedLength != null) {
            val expected = learningProgression.getOrNull(learningLoopNextIndex)
            if (expected == core) {
                learningLoopNextIndex = (learningLoopNextIndex + 1).floorMod(lockedLength)
                return
            }
            learningLoopLength = null
            learningLoopNextIndex = 0
            learningProgression.clear()
        }
        if (learningProgression.size >= 4 && learningProgression.firstOrNull() == core) {
            learningLoopLength = learningProgression.size
            learningLoopNextIndex = 1.floorMod(learningProgression.size)
            return
        }
        learningProgression += core
        while (learningProgression.size > 8) {
            learningProgression.removeAt(0)
        }
    }

    private fun dominantKeySuggestion(): String? {
        if (chordVotes.size < 6) return null
        val scores = mutableMapOf<String, Float>()
        chordVotes.forEachIndexed { index, chord ->
            val recency = if (chordVotes.size <= 1) 1f else index.toFloat() / chordVotes.lastIndex
            scores[chord.label] = (scores[chord.label] ?: 0f) + 0.45f + recency * 1.55f
        }
        val sorted = scores.entries.sortedByDescending { it.value }
        val best = sorted.firstOrNull() ?: return null
        val runnerUp = sorted.drop(1).firstOrNull()?.value ?: 0f
        val total = scores.values.sum().coerceAtLeast(0.01f)
        if (best.value / total < 0.42f) return null
        if (runnerUp > 0f && best.value < runnerUp * 1.22f) return null
        return best.key.toPlainKeyLabel()
    }
}

private class BarChordTracker {
    private val chromaSum = FloatArray(TunerAnalyzerNoteClasses.size)
    private val rootVotes = FloatArray(TunerAnalyzerNoteClasses.size)
    private val chordVotes = mutableMapOf<String, BarChordVote>()
    private var activeBarIndex: Long? = null
    private var frameCount = 0

    fun update(
        chord: RootThirdChord,
        classLevels: FloatArray,
        instantRootHint: ChordRootHint?,
        audioTimeMs: Long,
        tempoBpm: Int?,
        meter: Int,
        tempoConfident: Boolean,
    ): BarChordObservation? {
        if (!tempoConfident || tempoBpm == null) {
            clear()
            return null
        }
        val barMs = barDurationMs(tempoBpm, meter)
        val barIndex = audioTimeMs / barMs
        val committed = if (activeBarIndex == null) {
            activeBarIndex = barIndex
            null
        } else if (activeBarIndex != barIndex) {
            commitCurrentBar().also {
                resetBar(barIndex)
            }
        } else {
            null
        }

        observe(chord, classLevels, instantRootHint)
        return committed
    }

    fun clear() {
        activeBarIndex = null
        frameCount = 0
        chromaSum.fill(0f)
        rootVotes.fill(0f)
        chordVotes.clear()
    }

    private fun observe(
        chord: RootThirdChord,
        classLevels: FloatArray,
        instantRootHint: ChordRootHint?,
    ) {
        val peak = classLevels.maxOrNull()?.coerceAtLeast(0.0001f) ?: 0.0001f
        classLevels.forEachIndexed { index, level ->
            chromaSum[index] += level / peak
        }
        frameCount += 1
        if (instantRootHint != null && instantRootHint.confidence >= 0.45f) {
            rootVotes[instantRootHint.rootIndex] += instantRootHint.confidence
        } else if (chord.confident) {
            rootVotes[chord.rootIndex] += 0.38f
        }

        val rootWeight = (classLevels.getOrNull(chord.rootIndex) ?: 0f) / peak
        val thirdWeight = chord.tones.getOrNull(1)
            ?.noteClassIndex()
            ?.let { classLevels[it] / peak }
            ?: 0f
        val weight = 1f + rootWeight.coerceIn(0f, 1f) + thirdWeight.coerceIn(0f, 1f)
        val vote = chordVotes[chord.label]
        if (vote == null) {
            chordVotes[chord.label] = BarChordVote(chord = chord, weight = weight)
        } else {
            vote.weight += weight
        }
    }

    private fun commitCurrentBar(): BarChordObservation? {
        if (frameCount < 2 || chordVotes.isEmpty()) return null
        val totalWeight = chordVotes.values.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.01f)
        val bestVote = chordVotes.values.maxByOrNull { it.weight } ?: return null
        if (bestVote.weight < totalWeight * 0.48f) return null
        val barRootIndex = rootVotes.indices.maxByOrNull { rootVotes[it] }
        val rootVoteTotal = rootVotes.sum().coerceAtLeast(0.01f)
        val barRootConfidence = barRootIndex?.let { rootVotes[it] / rootVoteTotal } ?: 0f
        if (barRootIndex != null && rootVoteTotal >= 0.9f && barRootConfidence >= 0.55f && bestVote.chord.rootIndex != barRootIndex) {
            return null
        }

        val averagedChroma = FloatArray(TunerAnalyzerNoteClasses.size) { index ->
            chromaSum[index] / frameCount.coerceAtLeast(1)
        }
        if (barRootIndex == null) return null
        val rootAgreesWithChord = bestVote.chord.rootIndex == barRootIndex
        val hasStrongRootVote = rootVoteTotal >= 1.25f && barRootConfidence >= 0.58f
        val hasRepeatedMidRootVote = rootAgreesWithChord && rootVoteTotal >= 0.9f && barRootConfidence >= 0.46f
        if (!hasStrongRootVote && !hasRepeatedMidRootVote) return null
        val rootChord = if (bestVote.chord.rootIndex == barRootIndex) {
            bestVote.chord.toMajorMinorCoreChord()
        } else {
            bestVote.chord.copy(rootIndex = barRootIndex).asPlainRootChord()
        }
        return BarChordObservation(
            chord = rootChord,
            chroma = averagedChroma,
        )
    }

    private fun resetBar(nextBarIndex: Long) {
        activeBarIndex = nextBarIndex
        frameCount = 0
        chromaSum.fill(0f)
        rootVotes.fill(0f)
        chordVotes.clear()
    }

}

private fun barDurationMs(
    tempoBpm: Int?,
    meter: Int,
): Long {
    val beats = meter.coerceIn(2, 8)
    val bpm = tempoBpm?.takeIf { it in 40..220 } ?: 120
    return ((60_000f / bpm) * beats)
        .roundToInt()
        .toLong()
        .coerceIn(900L, 8_000L)
}

private data class BarChordVote(
    val chord: RootThirdChord,
    var weight: Float,
)

private class BassRootBarTracker {
    private val rootWeights = FloatArray(TunerAnalyzerNoteClasses.size)
    private var activeBarIndex: Long? = null
    private var evidenceFrameCount = 0
    private var lastCommittedHint: ChordRootHint? = null

    fun update(
        instantRootHint: ChordRootHint?,
        audioTimeMs: Long,
        tempoBpm: Int?,
        meter: Int,
        tempoConfident: Boolean,
    ): ChordRootHint? {
        if (!tempoConfident || tempoBpm == null) {
            clear()
            return instantRootHint?.takeIf { it.confidence >= 0.62f }
        }

        val barIndex = audioTimeMs / barDurationMs(tempoBpm, meter)
        if (activeBarIndex == null) {
            activeBarIndex = barIndex
        } else if (activeBarIndex != barIndex) {
            lastCommittedHint = currentHint(minConfidence = 0.42f) ?: lastCommittedHint?.weakened()
            resetBar(barIndex)
        }

        observe(instantRootHint)
        return currentHint(minConfidence = 0.34f)
            ?: instantRootHint?.takeIf { it.confidence >= 0.72f }
    }

    fun clear() {
        activeBarIndex = null
        evidenceFrameCount = 0
        lastCommittedHint = null
        rootWeights.fill(0f)
    }

    private fun observe(hint: ChordRootHint?) {
        if (hint == null || hint.confidence < 0.18f) return
        evidenceFrameCount += 1
        rootWeights[hint.rootIndex] += 0.18f + hint.confidence * hint.confidence
    }

    private fun currentHint(minConfidence: Float): ChordRootHint? {
        if (evidenceFrameCount < 2) return null
        val bestIndex = rootWeights.indices.maxByOrNull { rootWeights[it] } ?: return null
        val best = rootWeights[bestIndex]
        val total = rootWeights.sum().coerceAtLeast(0.001f)
        if (best < 0.28f) return null
        val runnerUp = rootWeights.indices
            .filter { it != bestIndex }
            .maxOfOrNull { rootWeights[it] }
            ?: 0f
        val share = best / total
        val separation = if (runnerUp <= 0f) 1f else ((best / runnerUp) - 1f) / 1.1f
        val sampleConfidence = (evidenceFrameCount / 5f).coerceIn(0f, 1f)
        val confidence = (share * 0.58f + separation.coerceIn(0f, 1f) * 0.28f + sampleConfidence * 0.14f)
            .coerceIn(0f, 1f)
        if (confidence < minConfidence) return null
        return ChordRootHint(rootIndex = bestIndex, confidence = confidence)
    }

    private fun resetBar(nextBarIndex: Long) {
        activeBarIndex = nextBarIndex
        evidenceFrameCount = 0
        rootWeights.fill(0f)
    }

    private fun ChordRootHint.weakened(): ChordRootHint {
        return copy(confidence = confidence * 0.72f)
    }
}

private data class BarChordObservation(
    val chord: RootThirdChord,
    val chroma: FloatArray,
)

private data class StructurePhraseState(
    val displayChords: List<String> = emptyList(),
    val phraseLengthBars: Int? = null,
    val phraseConfidence: Float = 0f,
    val currentBarIndex: Int = 0,
)

private fun List<String>.currentBarIndexForHistory(historySize: Int): Int {
    if (isEmpty() || historySize <= 0) return 0
    return ((historySize - 1).floorMod(size)) + 1
}

private fun List<String>.toCompactLoopDisplay(): List<String> {
    val compacted = fold(mutableListOf<String>()) { acc, chord ->
        if (acc.lastOrNull() != chord) acc += chord
        acc
    }
    if (compacted.size <= 1) return compacted
    val maxLength = minOf(8, compacted.size)
    for (length in 2..maxLength) {
        val prefix = compacted.take(length)
        if (compacted.indices.all { index -> compacted[index] == prefix[index.floorMod(length)] }) {
            return prefix
        }
    }
    return compacted.takeLast(STRUCTURE_MAX_DISPLAY_BARS)
}

private class StructurePhraseDetector {
    private val bars = mutableListOf<BarChordObservation>()
    private var lockedPhraseLength: Int? = null
    private var lockedConfidence = 0f

    fun observe(bar: BarChordObservation): StructurePhraseState {
        bars += bar
        while (bars.size > STRUCTURE_BAR_HISTORY_LIMIT) {
            bars.removeAt(0)
        }

        val detected = detectBestPhrase()
        if (detected != null && detected.confidence >= STRUCTURE_LOCK_CONFIDENCE) {
            lockedPhraseLength = detected.length
            lockedConfidence = detected.confidence
        } else {
            lockedConfidence *= 0.94f
            if (lockedConfidence < STRUCTURE_KEEP_CONFIDENCE) {
                lockedPhraseLength = null
                lockedConfidence = 0f
            }
        }

        return currentState()
    }

    fun removeRoot(rootIndex: Int): StructurePhraseState {
        bars.removeAll { it.chord.rootIndex == rootIndex }
        if (lockedPhraseLength != null && bars.takeLast(lockedPhraseLength ?: 0).any { it.chord.rootIndex == rootIndex }) {
            lockedPhraseLength = null
            lockedConfidence = 0f
        }
        return currentState()
    }

    fun clear() {
        bars.clear()
        lockedPhraseLength = null
        lockedConfidence = 0f
    }

    private fun currentState(): StructurePhraseState {
        val phraseLength = lockedPhraseLength
        val display = when {
            phraseLength != null && bars.size >= phraseLength -> orientLockedPhrase(
                bars.takeLast(phraseLength),
            ).map { it.chord.label }
            else -> bars.takeLast(STRUCTURE_MAX_DISPLAY_BARS).map { it.chord.label }
        }
        return StructurePhraseState(
            displayChords = display,
            phraseLengthBars = phraseLength,
            phraseConfidence = lockedConfidence.coerceIn(0f, 1f),
            currentBarIndex = display.currentBarIndexForHistory(bars.size),
        )
    }

    private fun orientLockedPhrase(phrase: List<BarChordObservation>): List<BarChordObservation> {
        if (phrase.size < 4) return phrase
        if (phrase.startsWithRepeatedMinorChord()) return phrase
        val keyRootIndex = estimateStructureKeyRoot(phrase) ?: return phrase
        val startIndex = phrase.indices
            .filter { index -> phrase[index].chord.rootIndex == keyRootIndex }
            .maxByOrNull { index -> phraseStartScore(phrase, index) }
            ?: return phrase
        if (startIndex == 0) return phrase
        return phrase.drop(startIndex) + phrase.take(startIndex)
    }

    private fun List<BarChordObservation>.startsWithRepeatedMinorChord(): Boolean {
        val first = firstOrNull()?.chord ?: return false
        val second = getOrNull(1)?.chord ?: return false
        return first.label.chordSuffix() == "m" && second.rootIndex == first.rootIndex
    }

    private fun estimateStructureKeyRoot(phrase: List<BarChordObservation>): Int? {
        val chroma = FloatArray(TunerAnalyzerNoteClasses.size)
        phrase.forEach { bar ->
            bar.chroma.forEachIndexed { index, level -> chroma[index] += level }
        }
        val key = estimateSongKey(
            chroma = chroma,
            chordHistory = phrase.map { it.chord },
            progression = phrase.map { it.chord.label },
        )?.label ?: return null
        return key.toPlainKeyLabel()
            ?.removeSuffix("m")
            ?.noteClassIndex()
    }

    private fun phraseStartScore(
        phrase: List<BarChordObservation>,
        index: Int,
    ): Float {
        val current = phrase[index].chord
        val previous = phrase[(index - 1).floorMod(phrase.size)].chord
        val next = phrase.getOrNull(index + 1)?.chord
        var score = 1f
        if (next?.rootIndex == current.rootIndex) score += 0.32f
        if (previous.rootIndex == (current.rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)) score += 0.42f
        if (previous.rootIndex == (current.rootIndex + 5).floorMod(TunerAnalyzerNoteClasses.size)) score += 0.28f
        if (current.label.chordSuffix() in listOf("", "m")) score += 0.08f
        score -= index * 0.001f
        return score
    }

    private fun detectBestPhrase(): StructurePhraseCandidate? {
        return STRUCTURE_PHRASE_LENGTHS
            .asSequence()
            .filter { length -> bars.size >= length.requiredStructureBars() }
            .map { length ->
                val current = bars.takeLast(length)
                val previous = bars.dropLast(length).takeLast(length)
                if (length == 4) {
                    val previousPrevious = bars.dropLast(length * 2).takeLast(length)
                    StructurePhraseCandidate(
                        length = length,
                        confidence = min(
                            phraseSimilarity(previousPrevious, previous),
                            phraseSimilarity(previous, current),
                        ),
                        rootAgreement = min(
                            phraseRootAgreement(previousPrevious, previous),
                            phraseRootAgreement(previous, current),
                        ),
                    )
                } else {
                    StructurePhraseCandidate(
                        length = length,
                        confidence = phraseSimilarity(previous, current),
                        rootAgreement = phraseRootAgreement(previous, current),
                    )
                }
            }
            .filter { candidate -> candidate.confidence >= STRUCTURE_MIN_CANDIDATE_CONFIDENCE }
            .filter { candidate -> candidate.rootAgreement >= STRUCTURE_ROOT_AGREEMENT_LOCK }
            .sortedWith(
                compareByDescending<StructurePhraseCandidate> { it.confidence }
                    .thenBy { it.length },
            )
            .firstOrNull()
    }

    private fun phraseSimilarity(
        previous: List<BarChordObservation>,
        current: List<BarChordObservation>,
    ): Float {
        if (previous.isEmpty() || previous.size != current.size) return 0f
        var total = 0f
        previous.indices.forEach { index ->
            total += barSimilarity(previous[index], current[index])
        }
        return total / previous.size
    }

    private fun phraseRootAgreement(
        previous: List<BarChordObservation>,
        current: List<BarChordObservation>,
    ): Float {
        if (previous.isEmpty() || previous.size != current.size) return 0f
        val matches = previous.indices.count { index ->
            previous[index].chord.rootIndex == current[index].chord.rootIndex
        }
        return matches.toFloat() / previous.size
    }

    private fun barSimilarity(
        previous: BarChordObservation,
        current: BarChordObservation,
    ): Float {
        val chordScore = when {
            previous.chord.label == current.chord.label -> 1f
            previous.chord.rootIndex == current.chord.rootIndex -> 0.72f
            circleOfFifthsDistance(previous.chord.rootIndex, current.chord.rootIndex) <= 1 -> 0.38f
            else -> 0f
        }
        val chromaScore = cosineSimilarity(previous.chroma, current.chroma).coerceIn(0f, 1f)
        return chordScore * 0.68f + chromaScore * 0.32f
    }
}

private data class StructurePhraseCandidate(
    val length: Int,
    val confidence: Float,
    val rootAgreement: Float,
)

private fun Int.requiredStructureBars(): Int {
    return if (this == 4) this * 3 else this * 2
}

private val STRUCTURE_PHRASE_LENGTHS = listOf(4, 8, 12, 16)
private const val STRUCTURE_MAX_DISPLAY_BARS = 16
private const val STRUCTURE_BAR_HISTORY_LIMIT = 64
private const val STRUCTURE_MIN_CANDIDATE_CONFIDENCE = 0.7f
private const val STRUCTURE_LOCK_CONFIDENCE = 0.78f
private const val STRUCTURE_KEEP_CONFIDENCE = 0.42f
private const val STRUCTURE_ROOT_AGREEMENT_LOCK = 0.86f

private data class ChromaChordTemplate(
    val suffix: String,
    val displaySuffix: String,
    val intervals: IntArray,
    val weights: FloatArray,
)

private val ChromaChordTemplates = listOf(
    ChromaChordTemplate("maj", "", intArrayOf(0, 4, 7), floatArrayOf(1.25f, 1.05f, 0.82f)),
    ChromaChordTemplate("min", "m", intArrayOf(0, 3, 7), floatArrayOf(1.25f, 1.05f, 0.82f)),
    ChromaChordTemplate("dim", "dim", intArrayOf(0, 3, 6), floatArrayOf(1.2f, 1.0f, 0.95f)),
    ChromaChordTemplate("aug", "aug", intArrayOf(0, 4, 8), floatArrayOf(1.2f, 1.0f, 0.95f)),
    ChromaChordTemplate("sus2", "sus2", intArrayOf(0, 2, 7), floatArrayOf(1.18f, 0.96f, 0.82f)),
    ChromaChordTemplate("sus4", "sus4", intArrayOf(0, 5, 7), floatArrayOf(1.18f, 0.96f, 0.82f)),
    ChromaChordTemplate("maj7", "maj7", intArrayOf(0, 4, 7, 11), floatArrayOf(1.18f, 1.0f, 0.72f, 0.86f)),
    ChromaChordTemplate("dom7", "7", intArrayOf(0, 4, 7, 10), floatArrayOf(1.18f, 1.0f, 0.72f, 0.96f)),
    ChromaChordTemplate("min7", "m7", intArrayOf(0, 3, 7, 10), floatArrayOf(1.18f, 1.0f, 0.72f, 0.96f)),
    ChromaChordTemplate("m7b5", "m7b5", intArrayOf(0, 3, 6, 10), floatArrayOf(1.18f, 1.0f, 0.94f, 0.92f)),
    ChromaChordTemplate("dim7", "dim7", intArrayOf(0, 3, 6, 9), floatArrayOf(1.14f, 0.96f, 0.9f, 0.86f)),
    ChromaChordTemplate("minMaj7", "mMaj7", intArrayOf(0, 3, 7, 11), floatArrayOf(1.12f, 0.96f, 0.7f, 0.84f)),
    ChromaChordTemplate("7sus4", "7sus4", intArrayOf(0, 5, 7, 10), floatArrayOf(1.14f, 0.96f, 0.7f, 0.92f)),
    ChromaChordTemplate("add9", "add9", intArrayOf(0, 4, 7, 2), floatArrayOf(1.12f, 0.96f, 0.68f, 0.74f)),
    ChromaChordTemplate("6", "6", intArrayOf(0, 4, 7, 9), floatArrayOf(1.12f, 0.96f, 0.68f, 0.78f)),
    ChromaChordTemplate("min6", "m6", intArrayOf(0, 3, 7, 9), floatArrayOf(1.12f, 0.96f, 0.68f, 0.78f)),
)

private fun matchChromaChord(
    classLevels: FloatArray,
    preferredRootIndex: Int? = null,
    preferredRootConfidence: Float = 0f,
): RootThirdChord? {
    return matchChromaChordCandidates(classLevels, preferredRootIndex, preferredRootConfidence).firstOrNull()?.chord
}

private fun matchChromaChordCandidates(
    classLevels: FloatArray,
    preferredRootIndex: Int? = null,
    preferredRootConfidence: Float = 0f,
    maxCandidates: Int = 6,
): List<ChordCandidate> {
    val normalized = normalizedChroma(classLevels) ?: return emptyList()
    val scored = mutableListOf<ChordCandidate>()
    for (rootIndex in TunerAnalyzerNoteClasses.indices) {
        for (template in ChromaChordTemplates) {
            val score = chordTemplateScore(
                chroma = normalized,
                rootIndex = rootIndex,
                template = template,
                preferredRootIndex = preferredRootIndex,
                preferredRootConfidence = preferredRootConfidence,
            )
            scored += ChordCandidate(
                chord = template.toChord(rootIndex, score),
                emissionScore = score,
            )
        }
    }
    val sorted = scored
        .sortedByDescending { it.emissionScore }
        .take(maxCandidates)
    val bestScore = sorted.firstOrNull()?.emissionScore ?: return emptyList()
    val runnerUp = sorted.drop(1).firstOrNull()?.emissionScore ?: -1f
    return sorted.map { candidate ->
        val confident = candidate.emissionScore >= 0.54f &&
            (
                bestScore >= 0.86f ||
                    candidate.emissionScore >= bestScore * 0.94f &&
                    (runnerUp <= 0f || bestScore >= runnerUp * 1.025f || candidate.emissionScore < bestScore)
                )
        val chord = if (confident) {
            candidate.chord.copy(confident = true)
        } else {
            candidate.chord.asUncertainRoot().copy(score = candidate.emissionScore)
        }
        candidate.copy(chord = chord)
    }
}

private fun List<ChordCandidate>.groundedByRootEvidence(rootConfidence: Float): List<ChordCandidate> {
    if (rootConfidence >= 0.25f) return this
    return map { candidate ->
        val score = candidate.emissionScore
        val chord = candidate.chord
        if (chord.confident && chord.label.hasExtendedOrSeventhColor()) {
            candidate.copy(
                chord = chord.asUncertainRoot().copy(score = score),
                emissionScore = score - (0.08f * (1f - rootConfidence.coerceIn(0f, 1f))),
            )
        } else {
            candidate
        }
    }
}

private fun List<ChordCandidate>.preferSongCoreHarmony(): List<ChordCandidate> {
    val core = filter { candidate -> candidate.chord.label.isSongCoreHarmonyLabel() }
    if (core.isNotEmpty()) return core
    return take(4).map { candidate ->
        candidate.copy(
            chord = candidate.chord.asUncertainRoot().copy(score = candidate.emissionScore),
            emissionScore = candidate.emissionScore - 0.1f,
        )
    }
}

private fun List<ChordCandidate>.preferMajorMinorCoreHarmony(classLevels: FloatArray): List<ChordCandidate> {
    val mapped = map { candidate ->
        val coreChord = candidate.chord.toMajorMinorCoreChord()
        candidate.copy(
            chord = coreChord.copy(score = candidate.emissionScore),
            emissionScore = candidate.emissionScore +
                coreChord.majorMinorCoreBonus() +
                coreChord.majorMinorThirdEvidenceBonus(classLevels),
        )
    }
    return mapped
        .groupBy { it.chord.label }
        .map { (_, candidates) -> candidates.maxBy { it.emissionScore } }
        .sortedByDescending { it.emissionScore }
        .take(4)
}

private fun rootAnchoredDisplayChord(
    rootHint: ChordRootHint?,
    classLevels: FloatArray,
    candidate: RootThirdChord?,
): RootThirdChord? {
    val hint = rootHint?.takeIf { it.confidence >= 0.42f } ?: return candidate
    val rootIndex = hint.rootIndex
    val peak = classLevels.maxOrNull()?.coerceAtLeast(0.0001f) ?: return candidate
    val rootLevel = classLevels[rootIndex] / peak
    fun intervalLevel(offset: Int): Float {
        return classLevels[(rootIndex + offset).floorMod(TunerAnalyzerNoteClasses.size)] / peak
    }
    val majorThird = intervalLevel(4)
    val minorThird = intervalLevel(3)
    val second = intervalLevel(2)
    val fourth = intervalLevel(5)
    val flatFifth = intervalLevel(6)
    val fifth = intervalLevel(7)
    val sharpFifth = intervalLevel(8)
    val sixth = intervalLevel(9)
    val flatSeventh = intervalLevel(10)
    val majorSeventh = intervalLevel(11)
    val sameRootCandidate = candidate?.takeIf { it.rootIndex == rootIndex }
    val hasRootFrame = rootLevel >= 0.18f || fifth >= 0.18f || sameRootCandidate != null
    val thirdFloor = if (hasRootFrame) 0.24f else 0.28f
    val suspensionFloor = if (hasRootFrame) 0.42f else 0.5f
    val delta = majorThird - minorThird
    val clearMajor = hasRootFrame && majorThird >= thirdFloor && delta >= 0.11f
    val clearMinor = hasRootFrame && minorThird >= thirdFloor && -delta >= 0.045f
    val thirdEvidenceIsAbsent = max(majorThird, minorThird) < thirdFloor
    val score = max(hint.confidence, candidate?.score ?: 0f).coerceIn(0f, 1f)
    val thirdBeatsSuspension = max(majorThird, minorThird) > max(second, fourth) + 0.06f
    val colorSuffix = rootColorSuffix(
        second = second,
        fourth = fourth,
        flatFifth = flatFifth,
        fifth = fifth,
        sharpFifth = sharpFifth,
        sixth = sixth,
        flatSeventh = flatSeventh,
        majorSeventh = majorSeventh,
        hasRootFrame = hasRootFrame,
    )
    return when {
        rootLevel >= 0.18f && fourth >= suspensionFloor && fourth > max(majorThird, minorThird) + 0.12f && fourth > second + 0.04f -> {
            rootSuspendedChord(rootIndex = rootIndex, suffix = "sus4", score = score, colorSuffix = colorSuffix)
        }
        rootLevel >= 0.18f && second >= suspensionFloor && second > max(majorThird, minorThird) + 0.12f -> {
            rootSuspendedChord(rootIndex = rootIndex, suffix = "sus2", score = score, colorSuffix = colorSuffix)
        }
        clearMajor && thirdBeatsSuspension -> rootQualityChord(rootIndex = rootIndex, minor = false, score = score, colorSuffix = colorSuffix)
        clearMinor && thirdBeatsSuspension -> rootQualityChord(rootIndex = rootIndex, minor = true, score = score, colorSuffix = colorSuffix)
        hasRootFrame && thirdEvidenceIsAbsent && fourth >= suspensionFloor && fourth > second + 0.04f -> {
            rootSuspendedChord(rootIndex = rootIndex, suffix = "sus4", score = score, colorSuffix = colorSuffix)
        }
        hasRootFrame && thirdEvidenceIsAbsent && second >= suspensionFloor -> {
            rootSuspendedChord(rootIndex = rootIndex, suffix = "sus2", score = score, colorSuffix = colorSuffix)
        }
        sameRootCandidate != null && !hasRootFrame -> {
            rootOnlyChord(rootIndex = rootIndex, score = score)
        }
        sameRootCandidate != null -> rootOnlyChord(rootIndex = rootIndex, score = score.coerceAtLeast(sameRootCandidate.score))
        else -> rootOnlyChord(rootIndex = rootIndex, score = score)
    }
}

private fun rootQualityChord(
    rootIndex: Int,
    minor: Boolean,
    score: Float,
    colorSuffix: String = "",
): RootThirdChord {
    val root = TunerAnalyzerNoteClasses[rootIndex]
    val thirdIndex = (rootIndex + if (minor) 3 else 4).floorMod(TunerAnalyzerNoteClasses.size)
    val fifthIndex = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    val qualitySuffix = if (minor) "m" else ""
    return RootThirdChord(
        label = "$root$qualitySuffix$colorSuffix",
        rootIndex = rootIndex,
        tones = listOf(root, TunerAnalyzerNoteClasses[thirdIndex], TunerAnalyzerNoteClasses[fifthIndex]),
        score = score,
        confident = true,
    )
}

private fun rootSuspendedChord(
    rootIndex: Int,
    suffix: String,
    score: Float,
    colorSuffix: String,
): RootThirdChord {
    val root = TunerAnalyzerNoteClasses[rootIndex]
    val color = colorSuffix
        .takeIf { it != suffix }
        .takeIf { !(suffix == "sus2" && it == "add9") }
        .orEmpty()
    val suspensionIndex = (rootIndex + if (suffix == "sus4") 5 else 2).floorMod(TunerAnalyzerNoteClasses.size)
    val fifthIndex = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    return RootThirdChord(
        label = "$root$suffix$color",
        rootIndex = rootIndex,
        tones = listOf(root, TunerAnalyzerNoteClasses[suspensionIndex], TunerAnalyzerNoteClasses[fifthIndex]),
        score = score,
        confident = true,
    )
}

private fun rootColorSuffix(
    second: Float,
    fourth: Float,
    flatFifth: Float,
    fifth: Float,
    sharpFifth: Float,
    sixth: Float,
    flatSeventh: Float,
    majorSeventh: Float,
    hasRootFrame: Boolean,
): String {
    if (!hasRootFrame) return ""
    return when {
        flatSeventh >= 0.18f && flatSeventh > majorSeventh + 0.05f -> "7"
        majorSeventh >= 0.18f && majorSeventh > flatSeventh + 0.05f -> "maj7"
        sixth >= 0.2f && sixth > flatSeventh + 0.06f -> "6"
        sharpFifth >= 0.22f && sharpFifth > fifth + 0.08f -> "aug"
        flatFifth >= 0.22f && flatFifth > fifth + 0.08f -> "b5"
        second >= 0.28f && fourth < 0.16f -> "add9"
        else -> ""
    }
}

private fun rootOnlyChord(
    rootIndex: Int,
    score: Float,
): RootThirdChord {
    val root = TunerAnalyzerNoteClasses[rootIndex]
    val fifthIndex = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    return RootThirdChord(
        label = root,
        rootIndex = rootIndex,
        tones = listOf(root, "--", TunerAnalyzerNoteClasses[fifthIndex]),
        score = score,
        confident = true,
    )
}

private fun uncertainRootChord(
    rootIndex: Int,
    score: Float,
): RootThirdChord {
    val root = TunerAnalyzerNoteClasses[rootIndex]
    val fifthIndex = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    return RootThirdChord(
        label = "$root?",
        rootIndex = rootIndex,
        tones = listOf(root, "--", TunerAnalyzerNoteClasses[fifthIndex]),
        score = score,
        confident = false,
    )
}

private fun RootThirdChord.toMajorMinorCoreChord(): RootThirdChord {
    val root = TunerAnalyzerNoteClasses[rootIndex]
    val suffix = label.chordSuffix()
    val minor = suffix.startsWith("m") && !suffix.startsWith("maj")
    val thirdOffset = if (minor) 3 else 4
    val fifthIndex = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    val thirdIndex = (rootIndex + thirdOffset).floorMod(TunerAnalyzerNoteClasses.size)
    return copy(
        label = if (minor) "${root}m" else root,
        tones = listOf(root, TunerAnalyzerNoteClasses[thirdIndex], TunerAnalyzerNoteClasses[fifthIndex]),
        confident = confident,
    )
}

private fun RootThirdChord.majorMinorCoreBonus(): Float {
    return when (label.chordSuffix()) {
        "", "m" -> 0.035f
        "7", "m7", "maj7" -> 0.015f
        else -> 0f
    }
}

private fun RootThirdChord.majorMinorThirdEvidenceBonus(classLevels: FloatArray): Float {
    val peak = classLevels.maxOrNull()?.coerceAtLeast(0.0001f) ?: return 0f
    val majorThird = classLevels[(rootIndex + 4).floorMod(TunerAnalyzerNoteClasses.size)] / peak
    val minorThird = classLevels[(rootIndex + 3).floorMod(TunerAnalyzerNoteClasses.size)] / peak
    val thirdDelta = if (label.chordSuffix() == "m") {
        minorThird - majorThird
    } else {
        majorThird - minorThird
    }
    return (thirdDelta * 0.22f).coerceIn(-0.16f, 0.16f)
}

private fun RootThirdChord.asPlainRootChord(): RootThirdChord {
    val fifthIndex = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    val root = TunerAnalyzerNoteClasses[rootIndex]
    return copy(
        label = root,
        tones = listOf(root, "--", TunerAnalyzerNoteClasses[fifthIndex]),
        confident = true,
        score = score.coerceAtMost(0.72f),
    )
}

private fun String.isSongCoreHarmonyLabel(): Boolean {
    if (endsWith("?")) return true
    val suffix = chordSuffix()
    return suffix.isBlank() ||
        suffix == "m" ||
        suffix == "7" ||
        suffix == "m7" ||
        suffix == "sus2" ||
        suffix == "sus4" ||
        suffix == "7sus4"
}

private fun String.isMajorMinorCoreLabel(): Boolean {
    val suffix = chordSuffix()
    return suffix.isBlank() || suffix == "m"
}

private fun String.chordSuffix(): String {
    val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
    return removePrefix(root)
}

private fun String.hasExtendedOrSeventhColor(): Boolean {
    return contains("7") || contains("6") || contains("9") || contains("sus") || contains("aug") || contains("dim")
}

private fun normalizedChroma(classLevels: FloatArray): FloatArray? {
    val peak = classLevels.maxOrNull() ?: return null
    if (peak < 0.0008f) return null
    val compressed = FloatArray(TunerAnalyzerNoteClasses.size) { index ->
        sqrt((classLevels[index] / peak).coerceAtLeast(0f))
    }
    val norm = sqrt(compressed.sumOf { (it * it).toDouble() }.toFloat())
    if (norm <= 0f) return null
    return FloatArray(compressed.size) { index -> compressed[index] / norm }
}

private fun chordTemplateScore(
    chroma: FloatArray,
    rootIndex: Int,
    template: ChromaChordTemplate,
    preferredRootIndex: Int?,
    preferredRootConfidence: Float,
): Float {
    val templateVector = FloatArray(TunerAnalyzerNoteClasses.size)
    template.intervals.forEachIndexed { index, interval ->
        templateVector[(rootIndex + interval).floorMod(TunerAnalyzerNoteClasses.size)] = template.weights[index]
    }
    val templateNorm = sqrt(templateVector.sumOf { (it * it).toDouble() }.toFloat()).coerceAtLeast(0.0001f)
    var positive = 0f
    var outsideEnergy = 0f
    for (index in chroma.indices) {
        val expected = templateVector[index] / templateNorm
        if (expected > 0f) {
            positive += chroma[index] * expected
        } else {
            outsideEnergy += chroma[index]
        }
    }
    val rootEnergy = chroma[rootIndex]
    val majorThirdEnergy = chroma[(rootIndex + 4).floorMod(TunerAnalyzerNoteClasses.size)]
    val flatSeventhEnergy = chroma[(rootIndex + 10).floorMod(TunerAnalyzerNoteClasses.size)]
    val augmentedFifthEnergy = chroma[(rootIndex + 8).floorMod(TunerAnalyzerNoteClasses.size)]
    val rootSupport = (rootEnergy * 0.18f).coerceIn(0f, 0.11f)
    val rootHintConfidence = preferredRootConfidence.coerceIn(0f, 1f)
    val preferredSupport = when {
        preferredRootIndex == null -> 0f
        preferredRootIndex == rootIndex -> 0.045f + rootHintConfidence * 0.095f
        else -> -(0.045f + rootHintConfidence * 0.16f)
    }
    val extensionPenalty = when (template.suffix) {
        "6", "min6", "add9" -> if (preferredRootIndex == rootIndex) -0.025f else -0.08f
        else -> 0f
    }
    val dominantShellSupport = if (template.suffix == "dom7" && majorThirdEnergy > 0.18f && flatSeventhEnergy > 0.18f) {
        0.08f
    } else {
        0f
    }
    val alteredColorPenalty = if (template.suffix == "aug" && flatSeventhEnergy >= augmentedFifthEnergy * 0.7f) {
        0.1f
    } else {
        0f
    }
    val colorPenalty = outsideEnergy * if (template.intervals.size <= 3) 0.05f else 0.035f
    return positive + rootSupport + preferredSupport + extensionPenalty + dominantShellSupport - alteredColorPenalty - colorPenalty
}

private fun ChromaChordTemplate.toChord(
    rootIndex: Int,
    score: Float,
): RootThirdChord {
    val rootName = TunerAnalyzerNoteClasses[rootIndex]
    val tones = intervals.map { interval ->
        TunerAnalyzerNoteClasses[(rootIndex + interval).floorMod(TunerAnalyzerNoteClasses.size)]
    }
    return RootThirdChord(
        rootIndex = rootIndex,
        label = "$rootName$displaySuffix",
        tones = tones,
        confident = true,
        score = score,
    )
}

private val KrumhanslMajorProfile = floatArrayOf(
    6.35f, 2.23f, 3.48f, 2.33f, 4.38f, 4.09f, 2.52f, 5.19f, 2.39f, 3.66f, 2.29f, 2.88f,
)

private val KrumhanslMinorProfile = floatArrayOf(
    6.33f, 2.68f, 3.52f, 5.38f, 2.60f, 3.53f, 2.54f, 4.75f, 3.98f, 2.69f, 3.34f, 3.17f,
)

private fun detectKrumhanslKey(chroma: FloatArray): String? {
    return estimateKrumhanslKey(chroma)?.label
}

private fun estimateSongKey(
    chroma: FloatArray,
    chordHistory: List<RootThirdChord>,
    progression: List<String>,
): SongKeyEstimate? {
    return selectStableSongKey(estimateSongKeyCandidates(chroma, chordHistory, progression))
}

private data class ParsedTheoryLabel(
    val root: String,
    val rootIndex: Int,
    val suffix: String,
)

private fun functionalHarmonyInfo(
    keyLabel: String?,
    chordLabel: String?,
): FunctionalHarmonyInfo? {
    val key = keyLabel?.parseTheoryLabel() ?: return null
    val chord = chordLabel?.parseTheoryLabel() ?: return null
    val keyIsMinor = key.suffix.startsWith("m")
    val interval = (chord.rootIndex - key.rootIndex).floorMod(TunerAnalyzerNoteClasses.size)
    val chordQuality = chord.functionalQuality()
    val roman = functionalRoman(interval, keyIsMinor, chordQuality, chord.suffix)
    val tags = functionalTags(interval, keyIsMinor, chord, chordQuality)
    return FunctionalHarmonyInfo(
        roman = roman,
        function = primaryHarmonyFunction(interval, tags),
        tags = tags,
    )
}

private fun FunctionalHarmonyInfo.compactHarmonySummary(): String {
    val tagText = tags
        .filterNot { tag -> tag == function }
        .take(2)
        .joinToString(" ")
    return listOf(roman, function, tagText)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

private fun String.parseTheoryLabel(): ParsedTheoryLabel? {
    val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
    val rootIndex = (SongFlatNoteClassAliases[root] ?: root).noteClassIndex() ?: return null
    return ParsedTheoryLabel(
        root = root,
        rootIndex = rootIndex,
        suffix = removePrefix(root),
    )
}

private enum class FunctionalChordQuality {
    Major,
    Minor,
    Dominant,
    Diminished,
    HalfDiminished,
    Suspended,
    RootOnly,
}

private fun ParsedTheoryLabel.functionalQuality(): FunctionalChordQuality {
    return when {
        suffix.isBlank() -> FunctionalChordQuality.Major
        suffix == "m" || suffix.startsWith("m6") || suffix.startsWith("m9") -> FunctionalChordQuality.Minor
        suffix.startsWith("m7b5") -> FunctionalChordQuality.HalfDiminished
        suffix.startsWith("dim") -> FunctionalChordQuality.Diminished
        suffix.contains("sus") -> FunctionalChordQuality.Suspended
        suffix.startsWith("7") || suffix.contains("7#") || suffix.contains("7b") || suffix.endsWith("alt") ||
            suffix.contains("13") || suffix.contains("9") -> FunctionalChordQuality.Dominant
        suffix.startsWith("maj") || suffix.startsWith("6") || suffix.startsWith("add") ||
            suffix.startsWith("aug") -> FunctionalChordQuality.Major
        else -> FunctionalChordQuality.RootOnly
    }
}

private fun functionalRoman(
    interval: Int,
    keyIsMinor: Boolean,
    quality: FunctionalChordQuality,
    suffix: String,
): String {
    val base = when (interval) {
        0 -> if (keyIsMinor) "i" else "I"
        1 -> "bII"
        2 -> if (quality == FunctionalChordQuality.Minor) "ii" else "II"
        3 -> if (keyIsMinor) "III" else "bIII"
        4 -> "III"
        5 -> if (quality == FunctionalChordQuality.Minor) "iv" else "IV"
        6 -> "bV"
        7 -> "V"
        8 -> "bVI"
        9 -> if (quality == FunctionalChordQuality.Minor) "vi" else "VI"
        10 -> "bVII"
        11 -> "VII"
        else -> "?"
    }
    val color = when (quality) {
        FunctionalChordQuality.Dominant -> if (suffix.contains("7") || suffix.contains("9") || suffix.contains("13")) "7" else ""
        FunctionalChordQuality.Diminished -> "dim"
        FunctionalChordQuality.HalfDiminished -> "m7b5"
        FunctionalChordQuality.Suspended -> "sus"
        else -> ""
    }
    return "$base$color"
}

private fun functionalTags(
    interval: Int,
    keyIsMinor: Boolean,
    chord: ParsedTheoryLabel,
    quality: FunctionalChordQuality,
): List<String> {
    val tags = mutableListOf<String>()
    when (interval) {
        0 -> tags += "tonic"
        2, 5 -> if (quality != FunctionalChordQuality.Dominant) tags += "predominant"
        7 -> tags += "dominant"
    }
    if (!keyIsMinor && interval in setOf(3, 5, 8, 10) && (quality == FunctionalChordQuality.Minor || interval != 5)) {
        tags += "borrowed"
    }
    if (keyIsMinor && interval in setOf(8, 10)) {
        tags += "minor color"
    }
    if (quality == FunctionalChordQuality.Dominant && interval != 7) {
        tags += secondaryDominantTag(chord.rootIndex)
    }
    if (quality == FunctionalChordQuality.Dominant && chord.suffix.isAlteredDominantSuffix()) {
        tags += "altered dominant"
    }
    if (quality == FunctionalChordQuality.Diminished || quality == FunctionalChordQuality.HalfDiminished) {
        tags += if (quality == FunctionalChordQuality.HalfDiminished) "half-diminished" else "diminished"
        tags += "leading/passing"
    }
    if (quality == FunctionalChordQuality.Suspended) {
        tags += "suspended"
    }
    return tags.distinct()
}

private fun secondaryDominantTag(rootIndex: Int): String {
    val targetIndex = (rootIndex + 5).floorMod(TunerAnalyzerNoteClasses.size)
    return "secondary dominant V/${TunerAnalyzerNoteClasses[targetIndex]}"
}

private fun String.isAlteredDominantSuffix(): Boolean {
    return contains("alt") ||
        contains("#9") ||
        contains("b9") ||
        contains("#5") ||
        contains("b13") ||
        contains("#11") ||
        startsWith("7aug") ||
        startsWith("aug")
}

private fun primaryHarmonyFunction(
    interval: Int,
    tags: List<String>,
): String {
    return when {
        "tonic" in tags -> "tonic"
        "dominant" in tags || tags.any { it.startsWith("secondary dominant") } -> "dominant"
        "predominant" in tags -> "predominant"
        "leading/passing" in tags -> "passing"
        interval in setOf(3, 8, 10) -> "color"
        else -> "color"
    }
}

private fun estimateSongKeyCandidates(
    chroma: FloatArray,
    chordHistory: List<RootThirdChord>,
    progression: List<String>,
): List<SongKeyEstimate> {
    val chromaCandidates = krumhanslKeyCandidates(chroma)
    if (chromaCandidates.isEmpty() && chordHistory.size < 4) return emptyList()

    return TunerAnalyzerNoteClasses.indices.flatMap { rootIndex ->
        listOf(
            SongKeyEstimate(
                label = TunerAnalyzerNoteClasses[rootIndex],
                confidence = combinedKeyScore(rootIndex, minor = false, chromaCandidates, chordHistory, progression),
            ),
            SongKeyEstimate(
                label = "${TunerAnalyzerNoteClasses[rootIndex]}m",
                confidence = combinedKeyScore(rootIndex, minor = true, chromaCandidates, chordHistory, progression),
            ),
        )
    }.sortedByDescending { it.confidence }
        .take(8)
}

private fun selectStableSongKey(candidates: List<SongKeyEstimate>): SongKeyEstimate? {
    val best = candidates.firstOrNull() ?: return null
    val runnerUp = candidates.drop(1).firstOrNull()?.confidence ?: 0f
    if (best.confidence < 0.36f) return null
    if (best.label.endsWith("m")) {
        return best
    }
    if (runnerUp > 0f && best.confidence < runnerUp + 0.045f) return null
    return best
}

private fun combinedKeyScore(
    rootIndex: Int,
    minor: Boolean,
    chromaCandidates: List<SongKeyEstimate>,
    chordHistory: List<RootThirdChord>,
    progression: List<String>,
): Float {
    val label = if (minor) "${TunerAnalyzerNoteClasses[rootIndex]}m" else TunerAnalyzerNoteClasses[rootIndex]
    val chromaScore = chromaCandidates.firstOrNull { it.label == label }?.confidence ?: 0f
    val chordScore = chordKeyEvidenceScore(rootIndex, minor, chordHistory, progression)
    val hasChordEvidence = progression.size >= 3 || chordHistory.size >= 6
    return if (hasChordEvidence) {
        chromaScore * 0.42f + chordScore * 0.58f
    } else {
        chromaScore * 0.62f + chordScore * 0.38f
    }
}

private fun chordKeyEvidenceScore(
    rootIndex: Int,
    minor: Boolean,
    chordHistory: List<RootThirdChord>,
    progression: List<String>,
): Float {
    val roots = if (progression.size >= 3) {
        progression.mapNotNull { label -> label.toSongChordVote(weight = 1f) }
    } else {
        chordHistory.takeLast(32).map { chord -> chord.label.toSongChordVote(weight = 1f) }.filterNotNull()
    }
    if (roots.isEmpty()) return 0f

    val total = roots.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(0.01f)
    fun rootWeight(offset: Int): Float {
        val wanted = (rootIndex + offset).floorMod(TunerAnalyzerNoteClasses.size)
        return roots.filter { it.rootIndex == wanted }.sumOf { it.weight.toDouble() }.toFloat() / total
    }

    val tonic = rootWeight(0)
    val fourth = rootWeight(5)
    val fifth = rootWeight(7)
    val relative = if (minor) rootWeight(3) else rootWeight(9)
    val first = roots.firstOrNull()
    val last = roots.lastOrNull()
    val firstSupport = if (first?.rootIndex == rootIndex) {
        if (minor && first.quality == SongChordQuality.Minor) 0.24f else 0.16f
    } else {
        0f
    }
    val lastSupport = if (last?.rootIndex == rootIndex) {
        if (minor && last.quality == SongChordQuality.Minor) 0.2f else 0.12f
    } else {
        0f
    }
    val cadenceSupport = cadenceScore(rootIndex, minor, roots)
    val qualitySupport = roots.filter { it.rootIndex == rootIndex }.sumOf { vote ->
        when {
            minor && vote.quality == SongChordQuality.Minor -> 0.2
            !minor && vote.quality == SongChordQuality.Major -> 0.11
            !minor && vote.quality == SongChordQuality.Dominant -> 0.06
            else -> 0.0
        }
    }.toFloat().coerceAtMost(if (minor) 0.34f else 0.22f)
    val relativeWeight = if (minor) 0.06f else 0.015f

    return (
        tonic * if (minor) 0.5f else 0.42f +
            fourth * 0.16f +
            fifth * 0.2f +
            relative * relativeWeight +
            firstSupport +
            lastSupport +
            cadenceSupport +
            qualitySupport
        ).coerceIn(0f, 1f)
}

private fun cadenceScore(
    rootIndex: Int,
    minor: Boolean,
    roots: List<SongChordVote>,
): Float {
    if (roots.size < 2) return 0f
    val fifthRoot = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    val fourthRoot = (rootIndex + 5).floorMod(TunerAnalyzerNoteClasses.size)
    var score = 0f
    roots.zipWithNext().forEach { (previous, current) ->
        if (current.rootIndex == rootIndex && previous.rootIndex == fifthRoot) {
            score += if (previous.quality == SongChordQuality.Dominant) 0.16f else 0.1f
        }
        if (!minor && current.rootIndex == rootIndex && previous.rootIndex == fourthRoot) {
            score += 0.08f
        }
        if (minor && current.rootIndex == rootIndex && previous.rootIndex == fourthRoot) {
            score += 0.06f
        }
    }
    return score.coerceAtMost(0.22f)
}

private fun estimateKrumhanslKey(chroma: FloatArray): SongKeyEstimate? {
    return krumhanslKeyCandidates(chroma).firstOrNull()
}

private fun krumhanslKeyCandidates(chroma: FloatArray): List<SongKeyEstimate> {
    val total = chroma.sum()
    if (total < 8f) return emptyList()
    val rawScores = mutableListOf<Pair<String, Float>>()
    for (rootIndex in TunerAnalyzerNoteClasses.indices) {
        rawScores += TunerAnalyzerNoteClasses[rootIndex] to pearsonRotated(chroma, KrumhanslMajorProfile, rootIndex)
        rawScores += "${TunerAnalyzerNoteClasses[rootIndex]}m" to pearsonRotated(chroma, KrumhanslMinorProfile, rootIndex)
    }
    val sorted = rawScores.sortedByDescending { it.second }
    val best = sorted.firstOrNull()?.second ?: return emptyList()
    if (best < 0.42f) return emptyList()
    return sorted.take(6).map { (label, score) ->
        SongKeyEstimate(
            label = label,
            confidence = ((score - 0.28f) / 0.72f).coerceIn(0f, 1f),
        )
    }
}

private fun legacyDetectKrumhanslKey(chroma: FloatArray): String? {
    val total = chroma.sum()
    if (total < 8f) return null
    var bestLabel: String? = null
    var bestScore = -2f
    var runnerUp = -2f
    for (rootIndex in TunerAnalyzerNoteClasses.indices) {
        val majorScore = pearsonRotated(chroma, KrumhanslMajorProfile, rootIndex)
        if (majorScore > bestScore) {
            runnerUp = bestScore
            bestScore = majorScore
            bestLabel = TunerAnalyzerNoteClasses[rootIndex]
        } else if (majorScore > runnerUp) {
            runnerUp = majorScore
        }
        val minorScore = pearsonRotated(chroma, KrumhanslMinorProfile, rootIndex)
        if (minorScore > bestScore) {
            runnerUp = bestScore
            bestScore = minorScore
            bestLabel = "${TunerAnalyzerNoteClasses[rootIndex]}m"
        } else if (minorScore > runnerUp) {
            runnerUp = minorScore
        }
    }
    if (bestScore < 0.42f) return null
    if (runnerUp > -2f && bestScore < runnerUp + 0.055f) return null
    return bestLabel
}

private fun pearsonRotated(
    chroma: FloatArray,
    profile: FloatArray,
    rootIndex: Int,
): Float {
    val chromaMean = chroma.average().toFloat()
    val profileMean = profile.average().toFloat()
    var numerator = 0f
    var chromaDenom = 0f
    var profileDenom = 0f
    for (index in TunerAnalyzerNoteClasses.indices) {
        val chromaCentered = chroma[index] - chromaMean
        val profileValue = profile[(index - rootIndex).floorMod(TunerAnalyzerNoteClasses.size)]
        val profileCentered = profileValue - profileMean
        numerator += chromaCentered * profileCentered
        chromaDenom += chromaCentered * chromaCentered
        profileDenom += profileCentered * profileCentered
    }
    val denom = sqrt(chromaDenom * profileDenom)
    return if (denom <= 0f) 0f else numerator / denom
}

internal object TunerTheoryTestApi {
    fun matchChord(
        notes: List<String>,
        preferredRoot: String? = null,
    ): String? {
        return matchChromaChord(
            classLevels = notes.toSyntheticChroma(),
            preferredRootIndex = preferredRoot?.toNoteClass()?.noteClassIndex(),
            preferredRootConfidence = if (preferredRoot != null) 1f else 0f,
        )?.label
    }

    fun matchChordWithDetails(
        notes: List<String>,
        preferredRoot: String? = null,
    ): Triple<String?, Float, String?> {
        val candidates = matchChromaChordCandidates(
            classLevels = notes.toSyntheticChroma(),
            preferredRootIndex = preferredRoot?.toNoteClass()?.noteClassIndex(),
            preferredRootConfidence = if (preferredRoot != null) 1f else 0f,
        )
        val best = candidates.firstOrNull()?.chord
        val alternate = candidates.drop(1).firstOrNull()?.chord
        return Triple(best?.label, best?.displayConfidence() ?: 0f, alternate?.label)
    }

    fun matchWeightedChord(
        weightedNotes: Map<String, Float>,
        preferredRoot: String? = null,
        rootConfidence: Float = if (preferredRoot != null) 1f else 0f,
    ): String? {
        return matchChromaChord(
            classLevels = weightedNotes.toSyntheticChroma(),
            preferredRootIndex = preferredRoot?.toNoteClass()?.noteClassIndex(),
            preferredRootConfidence = rootConfidence,
        )?.label
    }

    fun matchWeightedChordGrounded(
        weightedNotes: Map<String, Float>,
        preferredRoot: String? = null,
        rootConfidence: Float = if (preferredRoot != null) 1f else 0f,
    ): String? {
        return matchChromaChordCandidates(
            classLevels = weightedNotes.toSyntheticChroma(),
            preferredRootIndex = preferredRoot?.toNoteClass()?.noteClassIndex(),
            preferredRootConfidence = rootConfidence,
        ).groundedByRootEvidence(rootConfidence).firstOrNull()?.chord?.label
    }

    fun matchWeightedSongCoreChord(
        weightedNotes: Map<String, Float>,
        preferredRoot: String? = null,
        rootConfidence: Float = if (preferredRoot != null) 1f else 0f,
    ): String? {
        return matchChromaChordCandidates(
            classLevels = weightedNotes.toSyntheticChroma(),
            preferredRootIndex = preferredRoot?.toNoteClass()?.noteClassIndex(),
            preferredRootConfidence = rootConfidence,
        )
            .groundedByRootEvidence(rootConfidence)
            .preferSongCoreHarmony()
            .firstOrNull()
            ?.chord
            ?.label
    }

    fun matchWeightedMajorMinorCoreChord(
        weightedNotes: Map<String, Float>,
        preferredRoot: String? = null,
        rootConfidence: Float = if (preferredRoot != null) 1f else 0f,
    ): String? {
        val chroma = weightedNotes.toSyntheticChroma()
        return matchChromaChordCandidates(
            classLevels = chroma,
            preferredRootIndex = preferredRoot?.toNoteClass()?.noteClassIndex(),
            preferredRootConfidence = rootConfidence,
        )
            .groundedByRootEvidence(rootConfidence)
            .preferMajorMinorCoreHarmony(chroma)
            .firstOrNull()
            ?.chord
            ?.label
    }

    fun matchRootAnchoredDisplayChord(
        weightedNotes: Map<String, Float>,
        preferredRoot: String,
        rootConfidence: Float = 0.82f,
    ): String? {
        return matchRootAnchoredDisplayChordDetails(
            weightedNotes = weightedNotes,
            preferredRoot = preferredRoot,
            rootConfidence = rootConfidence,
        )?.first
    }

    fun matchRootAnchoredDisplayChordDetails(
        weightedNotes: Map<String, Float>,
        preferredRoot: String,
        rootConfidence: Float = 0.82f,
    ): Pair<String, List<String>>? {
        val chroma = weightedNotes.toSyntheticChroma()
        val rootIndex = preferredRoot.toNoteClass()?.noteClassIndex() ?: return null
        val candidates = matchChromaChordCandidates(
            classLevels = chroma,
            preferredRootIndex = rootIndex,
            preferredRootConfidence = rootConfidence,
        )
            .groundedByRootEvidence(rootConfidence)
            .preferMajorMinorCoreHarmony(chroma)
        return rootAnchoredDisplayChord(
            rootHint = ChordRootHint(rootIndex = rootIndex, confidence = rootConfidence),
            classLevels = chroma,
            candidate = candidates.firstOrNull()?.chord,
        )?.let { chord -> chord.label to chord.tones }
    }

    fun learnedMajorMinorProgression(
        chordLabels: List<String>,
        weightedNotes: Map<String, Float> = emptyMap(),
        tempoBpm: Int = 96,
        meter: Int = 4,
        frameStepMs: Long = 320L,
    ): List<String> {
        val tracker = RootThirdSongTracker()
        var progression = emptyList<String>()
        val fallbackChroma = weightedNotes
            .takeIf { it.isNotEmpty() }
            ?.toSyntheticChroma()
        chordLabels.forEachIndexed { index, label ->
            val chord = label.toRootThirdChordForTest(score = 0.88f) ?: return@forEachIndexed
            val chroma = fallbackChroma ?: chord.tones.filterNot { it == "--" }.toSyntheticChroma()
            val context = tracker.update(
                preferredRootIndex = chord.rootIndex,
                preferredRootConfidence = 0.82f,
                subBassRootClass = TunerAnalyzerNoteClasses[chord.rootIndex],
                subBassRootConfidence = 0.82f,
                bandBassRootClass = TunerAnalyzerNoteClasses[chord.rootIndex],
                bandBassRootConfidence = 0.82f,
                classLevels = chroma,
                audioTimeMs = index * frameStepMs,
                tempoBpm = tempoBpm,
                activeBeatIndex = -1,
                meter = meter,
                tempoConfident = false,
            )
            progression = context.progression
        }
        return progression
    }

    fun learnedMajorMinorProgressionFromWeightedFrames(
        frames: List<Pair<String, Map<String, Float>>>,
        tempoBpm: Int = 96,
        meter: Int = 4,
        frameStepMs: Long = 320L,
        tempoConfident: Boolean = false,
    ): List<String> {
        val tracker = RootThirdSongTracker()
        var progression = emptyList<String>()
        frames.forEachIndexed { index, (root, weightedNotes) ->
            val rootIndex = root.toNoteClass()?.noteClassIndex() ?: return@forEachIndexed
            val context = tracker.update(
                preferredRootIndex = rootIndex,
                preferredRootConfidence = 0.82f,
                subBassRootClass = TunerAnalyzerNoteClasses[rootIndex],
                subBassRootConfidence = 0.82f,
                bandBassRootClass = TunerAnalyzerNoteClasses[rootIndex],
                bandBassRootConfidence = 0.82f,
                classLevels = weightedNotes.toSyntheticChroma(),
                audioTimeMs = index * frameStepMs,
                tempoBpm = tempoBpm,
                activeBeatIndex = if (tempoConfident) index.floorMod(meter.coerceIn(2, 8)) else -1,
                meter = meter,
                tempoConfident = tempoConfident,
            )
            progression = context.progression
        }
        return progression
    }

    fun downbeatGuessFromWeightedFrames(
        frames: List<Pair<String, Map<String, Float>>>,
        tempoBpm: Int = 120,
        meter: Int = 4,
        frameStepMs: Long = 500L,
    ): Pair<Int, Float> {
        val tracker = RootThirdSongTracker()
        var beat = 0
        var confidence = 0f
        frames.forEachIndexed { index, (root, weightedNotes) ->
            val rootIndex = root.toNoteClass()?.noteClassIndex() ?: return@forEachIndexed
            val context = tracker.update(
                preferredRootIndex = rootIndex,
                preferredRootConfidence = 0.82f,
                subBassRootClass = TunerAnalyzerNoteClasses[rootIndex],
                subBassRootConfidence = 0.82f,
                bandBassRootClass = TunerAnalyzerNoteClasses[rootIndex],
                bandBassRootConfidence = 0.82f,
                classLevels = weightedNotes.toSyntheticChroma(),
                audioTimeMs = index * frameStepMs,
                tempoBpm = tempoBpm,
                activeBeatIndex = index.floorMod(meter.coerceIn(2, 8)),
                meter = meter,
                tempoConfident = true,
            )
            beat = context.downbeatGuess
            confidence = context.downbeatConfidence
        }
        return beat to confidence
    }

    fun committedBarChordsFromWeightedFrames(
        frames: List<Pair<String, Map<String, Float>>>,
        tempoBpm: Int = 96,
        meter: Int = 4,
        frameStepMs: Long = 320L,
    ): List<String> {
        val tracker = BarChordTracker()
        val committed = mutableListOf<String>()
        frames.forEachIndexed { index, (root, weightedNotes) ->
            val rootIndex = root.toNoteClass()?.noteClassIndex() ?: return@forEachIndexed
            val chord = matchChromaChordCandidates(
                classLevels = weightedNotes.toSyntheticChroma(),
                preferredRootIndex = rootIndex,
                preferredRootConfidence = 0.82f,
            )
                .groundedByRootEvidence(0.82f)
                .preferMajorMinorCoreHarmony(weightedNotes.toSyntheticChroma())
                .firstOrNull()
                ?.chord
                ?: return@forEachIndexed
            val observation = tracker.update(
                chord = chord,
                classLevels = weightedNotes.toSyntheticChroma(),
                instantRootHint = ChordRootHint(rootIndex = rootIndex, confidence = 0.82f),
                audioTimeMs = index * frameStepMs,
                tempoBpm = tempoBpm,
                meter = meter,
                tempoConfident = true,
            )
            observation?.chord?.label?.let { label -> committed += label }
        }
        return committed
    }

    fun barBassRoots(
        rootFrames: List<Pair<String?, Float>>,
        tempoBpm: Int = 96,
        meter: Int = 4,
        frameStepMs: Long = 280L,
    ): List<String> {
        val tracker = BassRootBarTracker()
        return rootFrames.mapIndexed { index, (root, confidence) ->
            val hint = root
                ?.toNoteClass()
                ?.noteClassIndex()
                ?.let { rootIndex -> ChordRootHint(rootIndex = rootIndex, confidence = confidence) }
            tracker.update(
                instantRootHint = hint,
                audioTimeMs = index * frameStepMs,
                tempoBpm = tempoBpm,
                meter = meter,
                tempoConfident = true,
            )?.rootIndex?.let { TunerAnalyzerNoteClasses[it] } ?: "--"
        }
    }

    fun dominantRootWithOctaveSupport(
        weightedNotes: Map<String, Float>,
        octaveCounts: Map<String, Int> = emptyMap(),
    ): Pair<String?, Float> {
        val counts = IntArray(TunerAnalyzerNoteClasses.size)
        octaveCounts.forEach { (note, count) ->
            note.toNoteClass()
                ?.noteClassIndex()
                ?.let { index -> counts[index] = count }
        }
        val evidence = dominantChromaRootEvidence(
            classLevels = weightedNotes.toSyntheticChroma(),
            octaveCounts = counts,
            minPeak = 0.2f,
            dominanceRatio = 1.08f,
        )
        return evidence?.label to (evidence?.confidence ?: 0f)
    }

    fun selectedSongRootEvidence(
        subBass: Pair<String, Float>?,
        bass: Pair<String, Float>?,
        pitch: Pair<String, Float>? = null,
    ): Pair<String?, Float> {
        val evidence = selectSongRootEvidence(
            subBass = subBass?.let { (label, confidence) -> ChordRootEvidence(label, confidence) },
            bass = bass?.let { (label, confidence) -> ChordRootEvidence(label, confidence) },
            pitch = pitch?.let { (label, confidence) -> ChordRootEvidence(label, confidence) },
        )
        return evidence?.label to (evidence?.confidence ?: 0f)
    }

    fun committedBarChord(
        frameChords: List<String>,
        weightedNotes: Map<String, Float>,
        rootFrames: List<Pair<String?, Float>> = emptyList(),
        tempoBpm: Int = 120,
        meter: Int = 4,
        frameStepMs: Long = 1_100L,
    ): String? {
        val tracker = BarChordTracker()
        var committed: BarChordObservation? = null
        frameChords.forEachIndexed { index, label ->
            val chord = label.toRootThirdChordForTest(score = 0.9f) ?: return@forEachIndexed
            val rootHint = rootFrames.getOrNull(index)
                ?.let { (root, confidence) ->
                    root
                        ?.toNoteClass()
                        ?.noteClassIndex()
                        ?.let { rootIndex -> ChordRootHint(rootIndex = rootIndex, confidence = confidence) }
                }
            committed = tracker.update(
                chord = chord,
                classLevels = weightedNotes.toSyntheticChroma(),
                instantRootHint = rootHint,
                audioTimeMs = index * frameStepMs,
                tempoBpm = tempoBpm,
                meter = meter,
                tempoConfident = true,
            ) ?: committed
        }
        return committed?.chord?.label
    }

    fun detectKey(weightedNotes: Map<String, Float>): String? {
        val chroma = FloatArray(TunerAnalyzerNoteClasses.size)
        weightedNotes.forEach { (note, weight) ->
            note.toNoteClass()
                ?.noteClassIndex()
                ?.let { index -> chroma[index] += weight }
        }
        return detectKrumhanslKey(chroma)
    }

    fun estimateSongKey(
        weightedNotes: Map<String, Float>,
        chordLabels: List<String>,
        progression: List<String> = chordLabels,
    ): String? {
        val chroma = FloatArray(TunerAnalyzerNoteClasses.size)
        weightedNotes.forEach { (note, weight) ->
            note.toNoteClass()
                ?.noteClassIndex()
                ?.let { index -> chroma[index] += weight }
        }
        val chords = chordLabels.mapNotNull { label -> label.toRootThirdChordForTest(score = 0.88f) }
        return estimateSongKey(chroma, chords, progression)?.label
    }

    fun functionalHarmony(
        keyLabel: String,
        chordLabel: String,
    ): FunctionalHarmonyInfo? {
        return functionalHarmonyInfo(keyLabel, chordLabel)
    }

    fun functionalHarmonySummary(
        keyLabel: String,
        chordLabel: String,
    ): String {
        return functionalHarmonyInfo(keyLabel, chordLabel)?.compactHarmonySummary().orEmpty()
    }

    fun cleanedSongChromaForTest(
        subBass: Map<String, Float> = emptyMap(),
        bass: Map<String, Float> = emptyMap(),
        lowMid: Map<String, Float> = emptyMap(),
        mid: Map<String, Float> = emptyMap(),
        high: Map<String, Float> = emptyMap(),
        full: Map<String, Float> = emptyMap(),
    ): Map<String, Float> {
        val cleaned = cleanedSongChroma(
            subBassLevels = subBass.toSyntheticChroma(),
            bassLevels = bass.toSyntheticChroma(),
            lowMidLevels = lowMid.toSyntheticChroma(),
            midLevels = mid.toSyntheticChroma(),
            highLevels = high.toSyntheticChroma(),
            fullLevels = full.toSyntheticChroma(),
        )
        return TunerAnalyzerNoteClasses
            .mapIndexed { index, note -> note to cleaned[index] }
            .toMap()
    }

    fun smoothCandidateLabels(frames: List<List<Pair<String, Float>>>): List<String> {
        val smoother = ChordSequenceSmoother()
        return frames.map { frame ->
            val candidates = frame.mapNotNull { (label, score) ->
                label.toRootThirdChordForTest(score)?.let { chord ->
                    ChordCandidate(chord = chord, emissionScore = score)
                }
            }
            smoother.update(candidates)?.label ?: "--"
        }
    }

    fun detectStructure(chordLabels: List<String>): Triple<List<String>, Int?, Float> {
        val detector = StructurePhraseDetector()
        var state = StructurePhraseState()
        chordLabels.forEach { label ->
            val chord = label.toRootThirdChordForTest(score = 0.88f) ?: return@forEach
            state = detector.observe(
                BarChordObservation(
                    chord = chord,
                    chroma = chord.tones.filterNot { it == "--" }.toSyntheticChroma(),
                ),
            )
        }
        return Triple(state.displayChords, state.phraseLengthBars, state.phraseConfidence)
    }

    fun detectStructureBarIndex(chordLabels: List<String>): Pair<Int, Boolean> {
        val detector = StructurePhraseDetector()
        var state = StructurePhraseState()
        chordLabels.forEach { label ->
            val chord = label.toRootThirdChordForTest(score = 0.88f) ?: return@forEach
            state = detector.observe(
                BarChordObservation(
                    chord = chord,
                    chroma = chord.tones.filterNot { it == "--" }.toSyntheticChroma(),
                ),
            )
        }
        return state.currentBarIndex to (state.phraseLengthBars != null)
    }

    private fun List<String>.toSyntheticChroma(): FloatArray {
        val chroma = FloatArray(TunerAnalyzerNoteClasses.size)
        forEach { note ->
            note.toNoteClass()
                ?.noteClassIndex()
                ?.let { index -> chroma[index] += 1f }
        }
        return chroma
    }

    private fun Map<String, Float>.toSyntheticChroma(): FloatArray {
        val chroma = FloatArray(TunerAnalyzerNoteClasses.size)
        forEach { (note, weight) ->
            note.toNoteClass()
                ?.noteClassIndex()
                ?.let { index -> chroma[index] += weight }
        }
        return chroma
    }

    private fun String.toRootThirdChordForTest(score: Float): RootThirdChord? {
        val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
        val rootIndex = (SongFlatNoteClassAliases[root] ?: root).noteClassIndex() ?: return null
        val suffix = removePrefix(root)
        val template = ChromaChordTemplates.firstOrNull { it.displaySuffix == suffix } ?: return null
        return template.toChord(rootIndex, score).copy(score = score)
    }
}

private fun RootThirdChord.asUncertainRoot(): RootThirdChord {
    val fifthIndex = (rootIndex + 7).floorMod(TunerAnalyzerNoteClasses.size)
    return copy(
        label = "${TunerAnalyzerNoteClasses[rootIndex]}?",
        tones = listOf(TunerAnalyzerNoteClasses[rootIndex], "--", TunerAnalyzerNoteClasses[fifthIndex]),
        confident = false,
    )
}

private fun RootThirdChord.displayConfidence(): Float {
    val base = ((score - 0.42f) / 0.58f).coerceIn(0f, 1f)
    return if (confident) base.coerceAtLeast(0.52f) else base.coerceAtMost(0.49f)
}

private class ChordSequenceSmoother {
    private val candidateHistory = mutableListOf<List<ChordCandidate>>()

    fun update(candidates: List<ChordCandidate>): RootThirdChord? {
        if (candidates.isEmpty()) return decodeBestPath().lastOrNull()
        candidateHistory += candidates
        while (candidateHistory.size > CHORD_SMOOTHER_HISTORY_BEATS) {
            candidateHistory.removeAt(0)
        }
        return decodeBestPath().lastOrNull()
    }

    fun clear() {
        candidateHistory.clear()
    }

    private fun decodeBestPath(): List<RootThirdChord> {
        if (candidateHistory.isEmpty()) return emptyList()
        val scores = candidateHistory.map { FloatArray(it.size) { Float.NEGATIVE_INFINITY } }
        val backPointers = candidateHistory.map { IntArray(it.size) { -1 } }
        candidateHistory.first().forEachIndexed { index, candidate ->
            scores.first()[index] = candidate.emissionScore
        }

        for (frameIndex in 1 until candidateHistory.size) {
            val previousCandidates = candidateHistory[frameIndex - 1]
            val currentCandidates = candidateHistory[frameIndex]
            currentCandidates.forEachIndexed { currentIndex, current ->
                var bestPreviousIndex = 0
                var bestScore = Float.NEGATIVE_INFINITY
                previousCandidates.forEachIndexed { previousIndex, previous ->
                    val transitionScore = chordTransitionScore(previous.chord, current.chord)
                    val candidateScore = scores[frameIndex - 1][previousIndex] + transitionScore + current.emissionScore
                    if (candidateScore > bestScore) {
                        bestScore = candidateScore
                        bestPreviousIndex = previousIndex
                    }
                }
                scores[frameIndex][currentIndex] = bestScore
                backPointers[frameIndex][currentIndex] = bestPreviousIndex
            }
        }

        var bestIndex = scores.last().indices.maxByOrNull { scores.last()[it] } ?: return emptyList()
        val path = MutableList(candidateHistory.size) { candidateHistory[it].first().chord }
        for (frameIndex in candidateHistory.lastIndex downTo 0) {
            path[frameIndex] = candidateHistory[frameIndex][bestIndex].chord
            bestIndex = backPointers[frameIndex].getOrElse(bestIndex) { -1 }
            if (bestIndex < 0 && frameIndex > 0) {
                bestIndex = 0
            }
        }
        return path
    }
}

private const val CHORD_SMOOTHER_HISTORY_BEATS = 6

private fun chordTransitionScore(
    previous: RootThirdChord,
    current: RootThirdChord,
): Float {
    if (previous.label == current.label) return 0.28f
    val rootDistance = circleOfFifthsDistance(previous.rootIndex, current.rootIndex)
    val sameRoot = previous.rootIndex == current.rootIndex
    val qualityChangePenalty = if (sameRoot) -0.02f else -0.05f
    val relationScore = when {
        sameRoot -> 0.16f
        rootDistance == 1 -> 0.08f
        rootDistance == 2 -> 0.03f
        rootDistance >= 5 -> -0.16f
        else -> -0.055f
    }
    val uncertaintyPenalty = if (!current.confident) -0.045f else 0f
    return relationScore + qualityChangePenalty + uncertaintyPenalty
}

private fun circleOfFifthsDistance(
    fromRoot: Int,
    toRoot: Int,
): Int {
    val fromPosition = CircleOfFifthsOrder.indexOf(fromRoot)
    val toPosition = CircleOfFifthsOrder.indexOf(toRoot)
    if (fromPosition < 0 || toPosition < 0) return 6
    val clockwise = abs(fromPosition - toPosition)
    return minOf(clockwise, CircleOfFifthsOrder.size - clockwise)
}

private val CircleOfFifthsOrder = listOf(0, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10, 5)

private class BeatChromaTracker {
    private val chromaSum = FloatArray(TunerAnalyzerNoteClasses.size)
    private var activeBeatIndex: Long? = null
    private var frameCount = 0

    fun update(
        classLevels: FloatArray,
        audioTimeMs: Long,
        tempoBpm: Int?,
        tempoConfident: Boolean,
    ): BeatChromaSnapshot {
        if (!tempoConfident || tempoBpm == null) {
            clear()
            return BeatChromaSnapshot(
                currentChroma = classLevels.copyOf(),
                committedChroma = null,
            )
        }

        val beatMs = beatDurationMs(tempoBpm)
        val beatIndex = audioTimeMs / beatMs
        val committed = if (activeBeatIndex == null) {
            activeBeatIndex = beatIndex
            null
        } else if (activeBeatIndex != beatIndex) {
            averageCurrentBeat().also {
                resetBeat(beatIndex)
            }
        } else {
            null
        }

        observe(classLevels)
        return BeatChromaSnapshot(
            currentChroma = averageCurrentBeat() ?: classLevels.copyOf(),
            committedChroma = committed,
        )
    }

    fun clear() {
        activeBeatIndex = null
        frameCount = 0
        chromaSum.fill(0f)
    }

    private fun observe(classLevels: FloatArray) {
        val peak = classLevels.maxOrNull() ?: return
        if (peak < 0.0008f) return
        classLevels.forEachIndexed { index, level ->
            chromaSum[index] += sqrt((level / peak).coerceAtLeast(0f))
        }
        frameCount += 1
    }

    private fun averageCurrentBeat(): FloatArray? {
        if (frameCount <= 0) return null
        return FloatArray(TunerAnalyzerNoteClasses.size) { index ->
            chromaSum[index] / frameCount
        }
    }

    private fun resetBeat(nextBeatIndex: Long) {
        activeBeatIndex = nextBeatIndex
        frameCount = 0
        chromaSum.fill(0f)
    }

    private fun beatDurationMs(tempoBpm: Int): Long {
        return (60_000f / tempoBpm.coerceIn(40, 220))
            .roundToInt()
            .toLong()
            .coerceIn(272L, 1_500L)
    }
}

private fun String.chordRootIndex(): Int {
    val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
    val normalizedRoot = SongFlatNoteClassAliases[root] ?: root
    return TunerAnalyzerNoteClasses.indexOf(normalizedRoot)
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

        val chordSuggestion = suggestKey(
            scalarKey = scalarKey,
            scalarConfidence = scalarConfidence,
        )
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

    private fun suggestKey(
        scalarKey: String?,
        scalarConfidence: Float,
    ): String? {
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

        val dominantTonic = dominantTonicSuggestion(rootCounts, minorCounts, majorCounts, dominantCounts)
        val usedRoots = rootCounts.count { it > 0f }
        if (usedRoots <= 2) {
            return dominantTonic ?: scalarKey?.takeIf { scalarConfidence >= 0.9f } ?: currentSuggestion
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
            return dominantTonic ?: currentSuggestion
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

private fun dominantTonicSuggestion(
    rootCounts: FloatArray,
    minorCounts: FloatArray,
    majorCounts: FloatArray,
    dominantCounts: FloatArray,
): String? {
    val totalRootWeight = rootCounts.sum().coerceAtLeast(0.01f)
    val rootIndex = rootCounts.indices.maxByOrNull { rootCounts[it] } ?: return null
    val rootSupport = rootCounts[rootIndex] / totalRootWeight
    if (rootSupport < 0.42f || rootCounts[rootIndex] < 4.5f) return null

    val runnerUpRoot = rootCounts.indices
        .filter { it != rootIndex }
        .maxOfOrNull { rootCounts[it] }
        ?: 0f
    if (runnerUpRoot > 0f && rootCounts[rootIndex] < runnerUpRoot * 1.25f) return null

    val minorSignal = minorCounts[rootIndex]
    val majorSignal = majorCounts[rootIndex] + dominantCounts[rootIndex] * 0.45f
    val rootName = SongKeyNoteClasses[rootIndex]
    return when {
        minorSignal >= 2.2f && minorSignal >= majorSignal * 1.08f -> "${rootName}m"
        majorSignal >= 2.2f && majorSignal >= minorSignal * 1.08f -> rootName
        else -> null
    }
}

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
        suffix.isBlank() -> SongChordQuality.Major
        suffix.startsWith("maj") -> SongChordQuality.Major
        suffix.startsWith("mMaj") -> SongChordQuality.Minor
        suffix.startsWith("m") -> SongChordQuality.Minor
        suffix.startsWith("7") -> SongChordQuality.Dominant
        suffix.contains("7") && !suffix.contains("maj") -> SongChordQuality.Dominant
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
        suffix.startsWith("mMaj") -> "mMaj7"
        suffix.startsWith("maj7") -> "maj7"
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

private fun String.toPlainKeyLabel(): String? {
    val root = take(2).takeIf { it.length == 2 && (it[1] == '#' || it[1] == 'b') } ?: take(1)
    val normalizedRoot = SongFlatNoteClassAliases[root] ?: root
    if (SongKeyNoteClasses.indexOf(normalizedRoot) < 0) return null
    val suffix = removePrefix(root)
    return if (suffix.startsWith("m")) "${root}m" else root
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

private fun twoDecimals(value: Float): String {
    return (value * 100f).roundToInt()
        .let { scaled -> "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}" }
}

private fun cosineSimilarity(
    left: FloatArray,
    right: FloatArray,
): Float {
    val size = minOf(left.size, right.size)
    if (size <= 0) return 0f
    var dot = 0f
    var leftEnergy = 0f
    var rightEnergy = 0f
    for (index in 0 until size) {
        dot += left[index] * right[index]
        leftEnergy += left[index] * left[index]
        rightEnergy += right[index] * right[index]
    }
    val denom = sqrt(leftEnergy * rightEnergy)
    return if (denom <= 0f) 0f else dot / denom
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

private fun spectrumPitchClassLevels(
    fftFrame: FftFrame,
    a4ReferenceHz: Int,
    minHz: Float,
    maxHz: Float,
): FloatArray {
    val classLevels = FloatArray(TunerAnalyzerNoteClasses.size)
    val startBin = (minHz / fftFrame.binHz).roundToInt().coerceIn(1, fftFrame.magnitudes.lastIndex)
    val endBin = (maxHz / fftFrame.binHz).roundToInt().coerceIn(startBin, fftFrame.magnitudes.lastIndex)
    for (bin in startBin..endBin) {
        val frequency = bin * fftFrame.binHz
        val magnitude = fftFrame.magnitudes[bin]
        if (magnitude <= 0f) continue
        val noteClass = frequency.toNoteReading(a4ReferenceHz).first.toNoteClass()
        val noteIndex = noteClass?.noteClassIndex() ?: continue
        val weightedMagnitude = magnitude / sqrt((frequency / 110f).coerceAtLeast(1f))
        classLevels[noteIndex] += weightedMagnitude
    }
    return classLevels
}

private fun cleanedSongChroma(
    subBassLevels: FloatArray,
    bassLevels: FloatArray,
    lowMidLevels: FloatArray,
    midLevels: FloatArray,
    highLevels: FloatArray,
    fullLevels: FloatArray,
): FloatArray {
    val subBass = compressedBandChroma(subBassLevels, power = 0.52f)
    val bass = compressedBandChroma(bassLevels, power = 0.55f)
    val lowMid = compressedBandChroma(lowMidLevels, power = 0.58f)
    val mid = compressedBandChroma(midLevels, power = 0.64f)
    val high = compressedBandChroma(highLevels, power = 0.48f)
    val full = compressedBandChroma(fullLevels, power = 0.48f)
    val cleaned = FloatArray(TunerAnalyzerNoteClasses.size)
    val rootBand = FloatArray(TunerAnalyzerNoteClasses.size) { index ->
        subBass[index] * 0.58f + bass[index] * 0.42f
    }
    val bassPeakIndex = rootBand.indices.maxByOrNull { rootBand[it] }
    val bassPeak = bassPeakIndex?.let { rootBand[it] } ?: 0f
    val bassRootIsClear = bassPeak >= 0.22f &&
        rootBand.indices
            .filter { it != bassPeakIndex }
            .maxOfOrNull { rootBand[it] }
            ?.let { runnerUp -> bassPeak >= runnerUp * 1.06f }
            ?: true

    for (index in cleaned.indices) {
        val bassWeight = if (bassRootIsClear && index == bassPeakIndex) 0.62f else 0.42f
        val combined = rootBand[index] * bassWeight +
            lowMid[index] * 0.32f +
            mid[index] * 0.34f +
            high[index] * 0.06f +
            full[index] * 0.1f
        val localSupport = maxOf(rootBand[index], lowMid[index], mid[index])
        val highOnlyPenalty = if (localSupport < 0.14f && high[index] > 0.24f) high[index] * 0.22f else 0f
        val fullOnlyPenalty = if (localSupport < 0.1f && full[index] > 0.28f) full[index] * 0.12f else 0f
        val harmonicGhostPenalty = if (
            bassRootIsClear &&
            bassPeakIndex != null &&
            index != bassPeakIndex &&
            localSupport < 0.22f &&
            isLikelyBassHarmonicGhost(bassPeakIndex, index)
        ) {
            (high[index] * 0.18f + full[index] * 0.12f).coerceAtMost(combined * 0.62f)
        } else {
            0f
        }
        cleaned[index] = (combined - highOnlyPenalty).coerceAtLeast(0f)
            .let { value -> (value - fullOnlyPenalty).coerceAtLeast(0f) }
            .let { value -> (value - harmonicGhostPenalty).coerceAtLeast(0f) }
    }
    return cleaned
}

private fun isLikelyBassHarmonicGhost(
    bassRootIndex: Int,
    noteIndex: Int,
): Boolean {
    return (noteIndex - bassRootIndex).floorMod(TunerAnalyzerNoteClasses.size) in setOf(2, 4, 7, 8, 9, 10)
}

private fun compressedBandChroma(
    levels: FloatArray,
    power: Float,
): FloatArray {
    val peak = levels.maxOrNull() ?: return FloatArray(TunerAnalyzerNoteClasses.size)
    if (peak < 0.00045f) return FloatArray(TunerAnalyzerNoteClasses.size)
    val floor = (levels.average().toFloat() * 0.45f).coerceAtMost(peak * 0.55f)
    val range = (peak - floor).coerceAtLeast(0.0001f)
    return FloatArray(TunerAnalyzerNoteClasses.size) { index ->
        ((levels[index] - floor) / range)
            .coerceIn(0f, 1f)
            .pow(power)
    }
}

private data class ChordRootEvidence(
    val label: String,
    val confidence: Float,
)

private fun selectSongRootEvidence(
    subBass: ChordRootEvidence?,
    bass: ChordRootEvidence?,
    pitch: ChordRootEvidence? = null,
): ChordRootEvidence? {
    if (pitch != null && bass != null && pitch.label == bass.label) {
        return pitch.copy(confidence = (pitch.confidence * 0.7f + bass.confidence * 0.5f).coerceIn(0f, 1f))
    }
    if (pitch != null && bass != null && pitch.label != bass.label) {
        return if (bass.confidence >= 0.62f) {
            bass.copy(confidence = (bass.confidence * 0.82f).coerceIn(0.42f, 0.72f))
        } else {
            pitch
        }
    }
    if (pitch != null && bass == null) {
        return pitch
    }
    if (subBass != null && bass != null && subBass.label == bass.label) {
        val agreedConfidence = subBass.confidence * 0.72f + bass.confidence * 0.48f
        return subBass.copy(confidence = agreedConfidence.coerceIn(0f, 1f))
    }
    if (subBass != null && bass != null && subBass.label != bass.label) {
        return if (bass.confidence >= 0.3f) {
            bass.copy(confidence = (bass.confidence * 0.9f).coerceIn(0.28f, 0.62f))
        } else {
            null
        }
    }
    if (subBass != null) return null
    if (bass != null && bass.confidence >= 0.3f) {
        return bass.copy(confidence = (bass.confidence * 0.96f).coerceIn(0f, 0.72f))
    }
    return null
}

private fun dominantChromaRootEvidence(
    classLevels: FloatArray,
    octaveCounts: IntArray? = null,
    minPeak: Float = 0.0008f,
    dominanceRatio: Float = 1.08f,
): ChordRootEvidence? {
    val peakIndex = classLevels.indices.maxByOrNull { classLevels[it] } ?: return null
    val peak = classLevels[peakIndex]
    if (peak < minPeak) return null
    val runnerUp = classLevels.indices
        .filter { it != peakIndex }
        .maxOfOrNull { classLevels[it] }
        ?: 0f
    if (runnerUp > 0f && peak < runnerUp * dominanceRatio) return null
    val ratioConfidence = if (runnerUp <= 0f) {
        1f
    } else {
        ((peak / runnerUp) - 1f) / 0.55f
    }
    val energyConfidence = (peak / (minPeak * 10f)).coerceIn(0f, 1f)
    val octaveCount = octaveCounts?.getOrNull(peakIndex) ?: 0
    val octaveConfidence = if (octaveCounts == null) {
        1f
    } else {
        (octaveCount / 3f).coerceIn(0.35f, 1f)
    }
    if (octaveCounts != null && octaveCount <= 1 && peak < minPeak * 24f) return null
    return ChordRootEvidence(
        label = TunerAnalyzerNoteClasses[peakIndex],
        confidence = ((ratioConfidence * 0.68f + energyConfidence * 0.22f) * octaveConfidence + octaveConfidence * 0.16f)
            .coerceIn(0.12f, 1f),
    )
}

private fun spectrumPitchClassOctaveCounts(
    fftFrame: FftFrame,
    a4ReferenceHz: Int,
    minHz: Float,
    maxHz: Float,
): IntArray {
    val octaveMasks = IntArray(TunerAnalyzerNoteClasses.size)
    val startBin = (minHz / fftFrame.binHz).roundToInt().coerceIn(1, fftFrame.magnitudes.lastIndex)
    val endBin = (maxHz / fftFrame.binHz).roundToInt().coerceIn(startBin, fftFrame.magnitudes.lastIndex)
    if (endBin <= startBin) return IntArray(TunerAnalyzerNoteClasses.size)
    var peakMagnitude = 0f
    for (bin in startBin..endBin) {
        val magnitude = fftFrame.magnitudes[bin]
        if (magnitude > peakMagnitude) peakMagnitude = magnitude
    }
    if (peakMagnitude <= 0f) return IntArray(TunerAnalyzerNoteClasses.size)
    val threshold = peakMagnitude * 0.18f
    for (bin in startBin..endBin) {
        val magnitude = fftFrame.magnitudes[bin]
        if (magnitude < threshold) continue
        val frequency = bin * fftFrame.binHz
        if (frequency !in minHz..maxHz) continue
        val noteClass = frequency.toNoteReading(a4ReferenceHz).first.toNoteClass()
        val noteIndex = noteClass?.noteClassIndex() ?: continue
        val octave = log2((frequency / 16.35f).coerceAtLeast(1f)).roundToInt().coerceIn(0, 30)
        octaveMasks[noteIndex] = octaveMasks[noteIndex] or (1 shl octave)
    }
    return IntArray(TunerAnalyzerNoteClasses.size) { index ->
        Integer.bitCount(octaveMasks[index])
    }
}

private fun dominantSpectrumNoteClasses(
    classLevels: FloatArray,
    stableNoteClass: String?,
): List<String> {
    val peak = classLevels.maxOrNull() ?: return emptyList()
    if (peak < 0.0008f) return emptyList()
    val harmonicClasses = stableNoteClass?.harmonicNoteClasses().orEmpty()
    return classLevels
        .mapIndexed { index, level -> TunerAnalyzerNoteClasses[index] to level }
        .filter { (noteClass, level) ->
            val threshold = if (noteClass in harmonicClasses && noteClass != stableNoteClass) {
                peak * 0.42f
            } else {
                peak * 0.26f
            }
            level >= threshold
        }
        .sortedByDescending { it.second }
        .take(6)
        .map { it.first }
}

private fun String.harmonicNoteClasses(): Set<String> {
    val rootIndex = noteClassIndex() ?: return emptySet()
    return listOf(7, 2).map { offset ->
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
