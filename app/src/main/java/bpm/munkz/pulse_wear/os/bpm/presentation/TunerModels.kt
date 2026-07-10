package bpm.munkz.pulse_wear.os.bpm.presentation

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

const val SPECTRUM_BAR_COUNT = 28
const val DEFAULT_A4_REFERENCE_HZ = 440
const val MIN_A4_REFERENCE_HZ = 400
const val MAX_A4_REFERENCE_HZ = 480

data class AudioAnalysisState(
    val frequencyHz: Float? = null,
    val noteName: String = "--",
    val cents: Int = 0,
    val level: Float = 0f,
    val detectedTempoBpm: Int? = null,
    val strictTempoBpm: Int? = null,
    val bassTempoBpm: Int? = null,
    val snareTempoBpm: Int? = null,
    val fluxTempoBpm: Int? = null,
    val smartTempoConfidence: Float = 0f,
    val tempoLearningBeats: Int = 0,
    val tempoActiveBeatIndex: Int = -1,
    val tempoConfident: Boolean = false,
    val tempoMeter: Int = 4,
    val musicalTempoBpm: Int? = null,
    val tempoFeelLabel: String = "",
    val tempoMeterLabel: String = "",
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
    val harmonyProgression: List<String> = emptyList(),
    val phraseLengthBars: Int? = null,
    val phraseConfidence: Float = 0f,
    val phraseBarIndex: Int = 0,
    val phraseLocked: Boolean = false,
    val phraseSource: String = "",
    val phraseSectionLabel: String = "",
    val downbeatGuess: Int = 0,
    val downbeatConfidence: Float = 0f,
    val downbeatRoot: String? = null,
    val downbeatLandingStrength: Float = 0f,
    val harmonySummary: String = "",
    val songDebugLine: String = "",
    val sensedA4Hz: Float? = null,
    val sensedA4OffsetCents: Int = 0,
    val spectrum: List<Float> = List(SPECTRUM_BAR_COUNT) { 0f },
)

data class SongPhraseState(
    val source: String = "",
    val lengthBars: Int? = null,
    val confidence: Float = 0f,
    val currentBarIndex: Int = 0,
    val locked: Boolean = false,
    val sectionLabel: String = "",
    val progressionSize: Int = 0,
    val slotsPerBar: Int = 1,
    val downbeatGuess: Int = 0,
    val downbeatConfidence: Float = 0f,
    val downbeatRoot: String? = null,
    val downbeatLandingStrength: Float = 0f,
) {
    val isLearning: Boolean
        get() = !locked

    fun compactLabel(): String {
        val sourceLabel = when {
            locked && sectionLabel.isNotBlank() -> sectionLabel
            locked -> "Lock"
            source.isNotBlank() -> source
            else -> "Learn"
        }
        return lengthBars?.let { bars ->
            val slot = currentBarIndex.takeIf { it > 0 }?.let { index -> " $index/$bars" } ?: ""
            "$sourceLabel ${bars}b$slot ${(confidence * 100f).roundToInt()}%"
        } ?: run {
            val count = progressionSize.takeIf { it > 0 } ?: currentBarIndex
            val slot = count.takeIf { it > 0 }?.let { value -> " $value" } ?: ""
            "$sourceLabel$slot"
        }
    }

    fun downbeatLabel(): String {
        if (downbeatGuess <= 0 || downbeatConfidence <= 0f) return "1 --"
        val root = downbeatRoot?.let { " $it" } ?: ""
        return "1->$downbeatGuess$root ${(downbeatConfidence * 100f).roundToInt()}%"
    }
}

fun AudioAnalysisState.songPhraseState(): SongPhraseState {
    return SongPhraseState(
        source = phraseSource,
        lengthBars = phraseLengthBars,
        confidence = phraseConfidence,
        currentBarIndex = phraseBarIndex,
        locked = phraseLocked,
        sectionLabel = phraseSectionLabel,
        progressionSize = chordProgression.size,
        downbeatGuess = downbeatGuess,
        downbeatConfidence = downbeatConfidence,
        downbeatRoot = downbeatRoot,
        downbeatLandingStrength = downbeatLandingStrength,
    )
}

data class SpectrumPeak(
    val frequencyHz: Float,
    val level: Float,
    val bandLabel: String,
)

data class SpectrumBand(
    val startHz: Float,
    val endHz: Float,
    val label: String,
    val color: Color,
)

data class AudioListenRange(
    val label: String,
    val minHz: Float,
    val maxHz: Float,
)

data class SpectrumTarget(
    val label: String,
    val frequencyHz: Float,
)

enum class SpectrumTuningChoice(
    val label: String,
    val profile: TunerListenProfile,
) {
    GuitarStandard("E", TunerListenProfile.Guitar),
    GuitarDropD("Drop D", TunerListenProfile.Guitar),
    GuitarOpenG("Open G", TunerListenProfile.Guitar),
    GuitarDStandard("D Std", TunerListenProfile.Guitar),
    GuitarCStandard("C Std", TunerListenProfile.Guitar),
    GuitarAllC("All C", TunerListenProfile.Guitar),
    BassStandard("E", TunerListenProfile.Bass),
    BassDropD("Drop D", TunerListenProfile.Bass),
    BassFiveString("5 Str", TunerListenProfile.Bass),
    BassCStandard("C Std", TunerListenProfile.Bass),
    BassAllC("All C", TunerListenProfile.Bass),
    VoiceBass("Bass", TunerListenProfile.Voice),
    VoiceBaritone("Bari", TunerListenProfile.Voice),
    VoiceTenor("Tenor", TunerListenProfile.Voice),
    VoiceAlto("Alto", TunerListenProfile.Voice),
    VoiceMezzo("Mezzo", TunerListenProfile.Voice),
    VoiceSoprano("Sop", TunerListenProfile.Voice),
    ViolinStandard("GDAE", TunerListenProfile.Violin),
    ViolinLow("Low", TunerListenProfile.Violin),
    ViolinHigh("High", TunerListenProfile.Violin),
    TrumpetBb("Bb", TunerListenProfile.Trumpet),
    TrumpetC("C", TunerListenProfile.Trumpet),
    TrumpetRange("Range", TunerListenProfile.Trumpet),
}

enum class SpectrumReaderMode {
    Instrument,
    Song,
}

enum class TunerListenProfile(
    val label: String,
    val spanishLabel: String,
    val minHz: Float,
    val maxHz: Float,
) {
    Full("Full", "Completo", 31f, 3_000f),
    High("High", "Agudo", 250f, 3_500f),
    Guitar("Gtr", "Guit", 70f, 1_400f),
    Voice("Vox", "Voz", 80f, 800f),
    Violin("Vln", "Viol", 190f, 3_500f),
    Trumpet("Trpt", "Trp", 150f, 2_500f),
    Bass("Bass", "Bajo", 38f, 330f),
}

val SpectrumTunerListenProfiles = listOf(
    TunerListenProfile.Full,
    TunerListenProfile.Bass,
    TunerListenProfile.Guitar,
    TunerListenProfile.Voice,
    TunerListenProfile.Violin,
    TunerListenProfile.Trumpet,
)

fun TunerListenProfile.labelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> label
        AppLanguage.Spanish -> spanishLabel
    }
}

fun TunerListenProfile.frequencyRangeLabel(): String {
    return "${minHz.toInt()}-${maxHz.toInt()} Hz"
}

fun TunerListenProfile.audioListenRangeFor(
    readerMode: SpectrumReaderMode,
    tuningChoice: SpectrumTuningChoice? = null,
): AudioListenRange {
    val tuningTargets = spectrumTargetsFor(readerMode, tuningChoice)
    if (tuningTargets.isNotEmpty()) {
        val minTarget = tuningTargets.minOf { it.frequencyHz }
        val maxTarget = tuningTargets.maxOf { it.frequencyHz }
        return AudioListenRange(
            label = tuningChoice?.label ?: "Instrument",
            minHz = (minTarget * 0.82f).coerceAtLeast(20f),
            maxHz = (maxTarget * 1.35f).coerceAtMost(3_500f),
        )
    }
    return when (readerMode) {
        SpectrumReaderMode.Instrument -> when (this) {
            TunerListenProfile.Full -> AudioListenRange("Auto Inst.", 27.5f, 3_500f)
            TunerListenProfile.Bass -> AudioListenRange("Bass Inst.", 27.5f, 400f)
            TunerListenProfile.Guitar -> AudioListenRange("Guitar Inst.", 55f, 1_400f)
            TunerListenProfile.Voice -> AudioListenRange("Voice Inst.", 80f, 900f)
            TunerListenProfile.Violin -> AudioListenRange("Violin Inst.", 190f, 3_500f)
            TunerListenProfile.Trumpet -> AudioListenRange("Trumpet Inst.", 150f, 2_500f)
            TunerListenProfile.High -> AudioListenRange("High Inst.", 220f, 3_500f)
        }
        SpectrumReaderMode.Song -> when (this) {
            TunerListenProfile.Full -> AudioListenRange("Full Song", 31f, 6_000f)
            TunerListenProfile.Bass -> AudioListenRange("Bass Song", 45f, 1_400f)
            TunerListenProfile.Guitar -> AudioListenRange("Guitar Song", 70f, 2_500f)
            TunerListenProfile.Voice -> AudioListenRange("Voice Song", 80f, 1_500f)
            TunerListenProfile.Violin -> AudioListenRange("Violin Song", 190f, 4_500f)
            TunerListenProfile.Trumpet -> AudioListenRange("Trumpet Song", 150f, 3_500f)
            TunerListenProfile.High -> AudioListenRange("High Song", 180f, 4_500f)
        }
    }
}

fun TunerListenProfile.spectrumTargetsFor(
    readerMode: SpectrumReaderMode,
    tuningChoice: SpectrumTuningChoice? = null,
): List<SpectrumTarget> {
    if (readerMode != SpectrumReaderMode.Instrument) return emptyList()
    val profileTuning = tuningChoice?.takeIf { it.profile == this }
    if (profileTuning != null) {
        return profileTuning.spectrumTargets()
    }
    return when (this) {
        TunerListenProfile.Bass -> listOf(
            SpectrumTarget("A0", 27.5f),
            SpectrumTarget("B0", 30.87f),
            SpectrumTarget("E1", 41.2f),
            SpectrumTarget("A1", 55f),
            SpectrumTarget("D2", 73.42f),
            SpectrumTarget("G2", 98f),
        )
        TunerListenProfile.Guitar -> listOf(
            SpectrumTarget("A1", 55f),
            SpectrumTarget("B1", 61.74f),
            SpectrumTarget("E2", 82.41f),
            SpectrumTarget("A2", 110f),
            SpectrumTarget("D3", 146.83f),
            SpectrumTarget("G3", 196f),
            SpectrumTarget("B3", 246.94f),
            SpectrumTarget("E4", 329.63f),
        )
        TunerListenProfile.Voice -> listOf(
            SpectrumTarget("Low", 110f),
            SpectrumTarget("Mid", 220f),
            SpectrumTarget("High", 440f),
        )
        TunerListenProfile.Violin -> listOf(
            SpectrumTarget("G3", 196f),
            SpectrumTarget("D4", 293.66f),
            SpectrumTarget("A4", 440f),
            SpectrumTarget("E5", 659.25f),
        )
        TunerListenProfile.Trumpet -> listOf(
            SpectrumTarget("Bb3", 233.08f),
            SpectrumTarget("F4", 349.23f),
            SpectrumTarget("Bb4", 466.16f),
            SpectrumTarget("D5", 587.33f),
        )
        TunerListenProfile.High -> listOf(
            SpectrumTarget("A3", 220f),
            SpectrumTarget("A4", 440f),
            SpectrumTarget("A5", 880f),
            SpectrumTarget("A6", 1_760f),
        )
        TunerListenProfile.Full -> emptyList()
    }
}

fun TunerListenProfile.defaultSpectrumTuningChoice(): SpectrumTuningChoice? {
    return when (this) {
        TunerListenProfile.Guitar -> SpectrumTuningChoice.GuitarStandard
        TunerListenProfile.Bass -> SpectrumTuningChoice.BassStandard
        TunerListenProfile.Voice -> SpectrumTuningChoice.VoiceTenor
        TunerListenProfile.Violin -> SpectrumTuningChoice.ViolinStandard
        TunerListenProfile.Trumpet -> SpectrumTuningChoice.TrumpetBb
        else -> null
    }
}

fun spectrumTuningChoicesFor(profile: TunerListenProfile): List<SpectrumTuningChoice> {
    return SpectrumTuningChoice.entries.filter { it.profile == profile }
}

private fun SpectrumTuningChoice.spectrumTargets(): List<SpectrumTarget> {
    return when (this) {
        SpectrumTuningChoice.GuitarStandard -> listOf(
            SpectrumTarget("E2", 82.41f),
            SpectrumTarget("A2", 110f),
            SpectrumTarget("D3", 146.83f),
            SpectrumTarget("G3", 196f),
            SpectrumTarget("B3", 246.94f),
            SpectrumTarget("E4", 329.63f),
        )
        SpectrumTuningChoice.GuitarDropD -> listOf(
            SpectrumTarget("D2", 73.42f),
            SpectrumTarget("A2", 110f),
            SpectrumTarget("D3", 146.83f),
            SpectrumTarget("G3", 196f),
            SpectrumTarget("B3", 246.94f),
            SpectrumTarget("E4", 329.63f),
        )
        SpectrumTuningChoice.GuitarOpenG -> listOf(
            SpectrumTarget("D2", 73.42f),
            SpectrumTarget("G2", 98f),
            SpectrumTarget("D3", 146.83f),
            SpectrumTarget("G3", 196f),
            SpectrumTarget("B3", 246.94f),
            SpectrumTarget("D4", 293.66f),
        )
        SpectrumTuningChoice.GuitarDStandard -> listOf(
            SpectrumTarget("D2", 73.42f),
            SpectrumTarget("G2", 98f),
            SpectrumTarget("C3", 130.81f),
            SpectrumTarget("F3", 174.61f),
            SpectrumTarget("A3", 220f),
            SpectrumTarget("D4", 293.66f),
        )
        SpectrumTuningChoice.GuitarCStandard -> listOf(
            SpectrumTarget("C2", 65.41f),
            SpectrumTarget("F2", 87.31f),
            SpectrumTarget("A#2", 116.54f),
            SpectrumTarget("D#3", 155.56f),
            SpectrumTarget("G3", 196f),
            SpectrumTarget("C4", 261.63f),
        )
        SpectrumTuningChoice.GuitarAllC -> listOf(
            SpectrumTarget("C1", 32.7f),
            SpectrumTarget("C2", 65.41f),
            SpectrumTarget("C3", 130.81f),
            SpectrumTarget("C4", 261.63f),
            SpectrumTarget("C5", 523.25f),
            SpectrumTarget("C6", 1_046.5f),
        )
        SpectrumTuningChoice.BassStandard -> listOf(
            SpectrumTarget("E1", 41.2f),
            SpectrumTarget("A1", 55f),
            SpectrumTarget("D2", 73.42f),
            SpectrumTarget("G2", 98f),
        )
        SpectrumTuningChoice.BassDropD -> listOf(
            SpectrumTarget("D1", 36.71f),
            SpectrumTarget("A1", 55f),
            SpectrumTarget("D2", 73.42f),
            SpectrumTarget("G2", 98f),
        )
        SpectrumTuningChoice.BassFiveString -> listOf(
            SpectrumTarget("B0", 30.87f),
            SpectrumTarget("E1", 41.2f),
            SpectrumTarget("A1", 55f),
            SpectrumTarget("D2", 73.42f),
            SpectrumTarget("G2", 98f),
        )
        SpectrumTuningChoice.BassCStandard -> listOf(
            SpectrumTarget("C1", 32.7f),
            SpectrumTarget("F1", 43.65f),
            SpectrumTarget("A#1", 58.27f),
            SpectrumTarget("D#2", 77.78f),
        )
        SpectrumTuningChoice.BassAllC -> listOf(
            SpectrumTarget("C1", 32.7f),
            SpectrumTarget("C2", 65.41f),
            SpectrumTarget("C3", 130.81f),
            SpectrumTarget("C4", 261.63f),
        )
        SpectrumTuningChoice.VoiceBass -> listOf(
            SpectrumTarget("E2", 82.41f),
            SpectrumTarget("A2", 110f),
            SpectrumTarget("E4", 329.63f),
        )
        SpectrumTuningChoice.VoiceBaritone -> listOf(
            SpectrumTarget("A2", 110f),
            SpectrumTarget("D3", 146.83f),
            SpectrumTarget("A4", 440f),
        )
        SpectrumTuningChoice.VoiceTenor -> listOf(
            SpectrumTarget("C3", 130.81f),
            SpectrumTarget("G3", 196f),
            SpectrumTarget("C5", 523.25f),
        )
        SpectrumTuningChoice.VoiceAlto -> listOf(
            SpectrumTarget("F3", 174.61f),
            SpectrumTarget("C4", 261.63f),
            SpectrumTarget("F5", 698.46f),
        )
        SpectrumTuningChoice.VoiceMezzo -> listOf(
            SpectrumTarget("A3", 220f),
            SpectrumTarget("E4", 329.63f),
            SpectrumTarget("A5", 880f),
        )
        SpectrumTuningChoice.VoiceSoprano -> listOf(
            SpectrumTarget("C4", 261.63f),
            SpectrumTarget("G4", 392f),
            SpectrumTarget("C6", 1_046.5f),
        )
        SpectrumTuningChoice.ViolinStandard -> listOf(
            SpectrumTarget("G3", 196f),
            SpectrumTarget("D4", 293.66f),
            SpectrumTarget("A4", 440f),
            SpectrumTarget("E5", 659.25f),
        )
        SpectrumTuningChoice.ViolinLow -> listOf(
            SpectrumTarget("G3", 196f),
            SpectrumTarget("B3", 246.94f),
            SpectrumTarget("D4", 293.66f),
        )
        SpectrumTuningChoice.ViolinHigh -> listOf(
            SpectrumTarget("A4", 440f),
            SpectrumTarget("E5", 659.25f),
            SpectrumTarget("A5", 880f),
        )
        SpectrumTuningChoice.TrumpetBb -> listOf(
            SpectrumTarget("Bb3", 233.08f),
            SpectrumTarget("F4", 349.23f),
            SpectrumTarget("Bb4", 466.16f),
        )
        SpectrumTuningChoice.TrumpetC -> listOf(
            SpectrumTarget("C4", 261.63f),
            SpectrumTarget("G4", 392f),
            SpectrumTarget("C5", 523.25f),
        )
        SpectrumTuningChoice.TrumpetRange -> listOf(
            SpectrumTarget("F#3", 185f),
            SpectrumTarget("Bb4", 466.16f),
            SpectrumTarget("C6", 1_046.5f),
        )
    }
}

fun TunerListenProfile.constraintLabelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> when (this) {
            TunerListenProfile.Full -> "Full"
            TunerListenProfile.High -> "High Instrument"
            TunerListenProfile.Guitar -> "Guitar"
            TunerListenProfile.Voice -> "Voice"
            TunerListenProfile.Violin -> "Violin"
            TunerListenProfile.Trumpet -> "Trumpet"
            TunerListenProfile.Bass -> "Bass"
        }
        AppLanguage.Spanish -> when (this) {
            TunerListenProfile.Full -> "Completo"
            TunerListenProfile.High -> "Instrumento agudo"
            TunerListenProfile.Guitar -> "Guitarra"
            TunerListenProfile.Voice -> "Voz"
            TunerListenProfile.Violin -> "Violín"
            TunerListenProfile.Trumpet -> "Trompeta"
            TunerListenProfile.Bass -> "Bajo"
        }
    }
}
