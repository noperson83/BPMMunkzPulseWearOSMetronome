package bpm.munkz.pulse_wear.os.bpm.presentation

import bpm.munkz.pulse_wear.os.bpm.BuildConfig

internal enum class AppEdition {
    Bpm,
    Tune,
    Rhythm,
    Playlist,
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
    val isProFree: Boolean = edition == AppEdition.Pro
    val isTuneOnly: Boolean = edition == AppEdition.Tune
    val isRhythmOnly: Boolean = edition == AppEdition.Rhythm
    val isPlaylistOnly: Boolean = edition == AppEdition.Playlist
}

internal object AppEditionConfig {
    private fun editionFor(packageName: String): AppEdition {
        return when {
            packageName.endsWith(".tune") -> AppEdition.Tune
            packageName.endsWith(".rhythm") -> AppEdition.Rhythm
            packageName.endsWith(".playlist") -> AppEdition.Playlist
            packageName.endsWith(".pro") -> AppEdition.Pro
            BuildConfig.APP_EDITION == "tune" -> AppEdition.Tune
            BuildConfig.APP_EDITION == "rhythm" -> AppEdition.Rhythm
            BuildConfig.APP_EDITION == "playlist" -> AppEdition.Playlist
            BuildConfig.APP_EDITION == "pro" -> AppEdition.Pro
            else -> AppEdition.Bpm
        }
    }

    fun featuresFor(packageName: String): AppFeatureSet {
        val edition = editionFor(packageName)
        return AppFeatureSet(
            edition = edition,
            pageCount = when (edition) {
            AppEdition.Bpm -> 2
            AppEdition.Tune -> TUNE_PAGE_COUNT
            AppEdition.Rhythm -> RHYTHM_PAGE_COUNT
            AppEdition.Playlist -> PLAYLIST_PAGE_COUNT
            AppEdition.Pro -> PULSE_PAGE_COUNT
            },
            showTunerEntry = edition == AppEdition.Pro || edition == AppEdition.Tune,
            showSpectrumEntry = edition == AppEdition.Pro || edition == AppEdition.Tune,
            showTapRhythmChoices = edition == AppEdition.Pro || edition == AppEdition.Rhythm,
        )
    }
}
