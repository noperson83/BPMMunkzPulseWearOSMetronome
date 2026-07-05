package bpm.munkz.pulse_wear.os.bpm.presentation

import bpm.munkz.pulse_wear.os.bpm.BuildConfig

internal enum class AppEdition {
    Bpm,
    Tune,
    Rhythm,
    Playlist,
    Pro,
    HearNoEvil,
    FidgetToy,
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
    val isHearNoEvilOnly: Boolean = edition == AppEdition.HearNoEvil
    val isFidgetToyOnly: Boolean = edition == AppEdition.FidgetToy
}

internal object AppEditionConfig {
    private fun editionFor(packageName: String): AppEdition {
        return when {
            BuildConfig.APP_EDITION == "bpm" -> AppEdition.Bpm
            BuildConfig.APP_EDITION == "tune" -> AppEdition.Tune
            BuildConfig.APP_EDITION == "rhythm" -> AppEdition.Rhythm
            BuildConfig.APP_EDITION == "playlist" -> AppEdition.Playlist
            BuildConfig.APP_EDITION == "pro" -> AppEdition.Pro
            BuildConfig.APP_EDITION == "hearnoevil" -> AppEdition.HearNoEvil
            BuildConfig.APP_EDITION == "fidgettoy" -> AppEdition.FidgetToy
            packageName.endsWith(".metronome") -> AppEdition.Bpm
            packageName.endsWith(".tune") ||
                packageName.endsWith(".tuner") -> AppEdition.Tune
            packageName.endsWith(".rhythm") -> AppEdition.Rhythm
            packageName.endsWith(".playlist") -> AppEdition.Playlist
            packageName.endsWith(".pro") -> AppEdition.Pro
            packageName.endsWith(".hearnoevil") -> AppEdition.HearNoEvil
            packageName.endsWith(".fidgettoy") -> AppEdition.FidgetToy
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
            AppEdition.HearNoEvil -> 1
            AppEdition.FidgetToy -> 1
            },
            showTunerEntry = edition == AppEdition.Pro || edition == AppEdition.Tune || edition == AppEdition.HearNoEvil,
            showSpectrumEntry = edition == AppEdition.Pro || edition == AppEdition.Tune || edition == AppEdition.HearNoEvil,
            showTapRhythmChoices = edition == AppEdition.Pro || edition == AppEdition.Rhythm,
        )
    }
}
