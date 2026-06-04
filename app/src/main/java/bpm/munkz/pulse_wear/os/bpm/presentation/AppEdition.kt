package bpm.munkz.pulse_wear.os.bpm.presentation

import bpm.munkz.pulse_wear.os.bpm.BuildConfig

internal enum class AppEdition {
    Bpm,
    Tune,
    Pro,
}

internal data class AppFeatureSet(
    val edition: AppEdition,
    val pageCount: Int,
    val showTunerEntry: Boolean,
    val showSpectrumEntry: Boolean,
    val showTapRhythmChoices: Boolean,
) {
    val isFreeOnly: Boolean = edition == AppEdition.Bpm
    val isTuneOnly: Boolean = edition == AppEdition.Tune
}

internal object AppEditionConfig {
    private val edition = when (BuildConfig.APP_EDITION) {
        "tune" -> AppEdition.Tune
        "pro" -> AppEdition.Pro
        else -> AppEdition.Bpm
    }

    val features = AppFeatureSet(
        edition = edition,
        pageCount = when (edition) {
            AppEdition.Bpm -> 2
            AppEdition.Tune -> TUNE_PAGE_COUNT
            AppEdition.Pro -> PULSE_PAGE_COUNT
        },
        showTunerEntry = edition == AppEdition.Pro || edition == AppEdition.Tune,
        showSpectrumEntry = edition == AppEdition.Pro || edition == AppEdition.Tune,
        showTapRhythmChoices = edition == AppEdition.Pro,
    )
}
