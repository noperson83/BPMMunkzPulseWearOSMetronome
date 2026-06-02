package bpm.munkz.pulse_wear.os.metronome.presentation

internal const val PULSE_PAGE_COUNT = 4
internal const val RHYTHM_VISUAL_MAX_DELAY_MS = 24L
internal const val RHYTHM_VISUAL_WAKE_AHEAD_MS = 6L

internal val TimeSignatureBeatOptions = (2..16).toList()
internal val SubdivisionOptions = listOf(1, 2, 3, 4, 6)

internal data class BeatVisualState(
    val currentBeatIndex: Int,
    val currentSubdivisionIndex: Int,
    val beatFlash: Boolean,
)

internal enum class RhythmChoicePicker {
    TimeSignature,
    Subdivision,
}

internal fun Int.toSupportedPulseSubdivisionCount(): Int {
    return when {
        this <= 1 -> 1
        this == 2 -> 2
        this == 3 -> 3
        this == 4 -> 4
        else -> 6
    }
}
