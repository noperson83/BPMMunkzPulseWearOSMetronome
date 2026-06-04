package bpm.munkz.pulse_wear.os.bpm.presentation

import androidx.compose.ui.graphics.Color

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
    val recentNotes: List<String> = emptyList(),
    val guessedKey: String? = null,
    val likelyChords: List<String> = emptyList(),
    val spectrum: List<Float> = List(SPECTRUM_BAR_COUNT) { 0f },
)

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

enum class TunerListenProfile(
    val label: String,
    val spanishLabel: String,
    val minHz: Float,
    val maxHz: Float,
) {
    Full("Auto", "Auto", 31f, 3_000f),
    High("High", "Agudo", 250f, 3_500f),
    Guitar("Gtr", "Guit", 70f, 1_400f),
    Voice("Vox", "Voz", 80f, 800f),
    Bass("Bass", "Bajo", 38f, 330f),
}

val SpectrumTunerListenProfiles = listOf(
    TunerListenProfile.Full,
    TunerListenProfile.Bass,
    TunerListenProfile.Guitar,
    TunerListenProfile.Voice,
    TunerListenProfile.High,
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

fun TunerListenProfile.constraintLabelFor(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> when (this) {
            TunerListenProfile.Full -> "Auto"
            TunerListenProfile.High -> "High Instrument"
            TunerListenProfile.Guitar -> "Guitar"
            TunerListenProfile.Voice -> "Voice"
            TunerListenProfile.Bass -> "Bass"
        }
        AppLanguage.Spanish -> when (this) {
            TunerListenProfile.Full -> "Auto"
            TunerListenProfile.High -> "Instrumento agudo"
            TunerListenProfile.Guitar -> "Guitarra"
            TunerListenProfile.Voice -> "Voz"
            TunerListenProfile.Bass -> "Bajo"
        }
    }
}
