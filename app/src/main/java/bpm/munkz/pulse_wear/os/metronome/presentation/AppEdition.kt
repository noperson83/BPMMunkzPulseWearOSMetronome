package bpm.munkz.pulse_wear.os.metronome.presentation

internal enum class AppEdition {
    Bpmer,
    Max,
}

internal data class AppFeatureSet(
    val edition: AppEdition,
    val pageCount: Int,
    val showTunerEntry: Boolean,
    val showSpectrumEntry: Boolean,
    val showTapRhythmChoices: Boolean,
) {
    val isBpmerOnly: Boolean = edition == AppEdition.Bpmer
}

internal object AppEditionConfig {
    val features = AppFeatureSet(
        edition = AppEdition.Bpmer,
        pageCount = 1,
        showTunerEntry = false,
        showSpectrumEntry = false,
        showTapRhythmChoices = false,
    )
}
