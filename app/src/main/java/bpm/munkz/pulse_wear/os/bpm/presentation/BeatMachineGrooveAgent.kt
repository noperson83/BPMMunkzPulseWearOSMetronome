package bpm.munkz.pulse_wear.os.bpm.presentation

import kotlin.math.abs
import kotlin.math.max

internal object BeatMachineGrooveAgent {
    private const val STEP_COUNT = 16
    private const val KICK_PAD_INDEX = 0
    private const val SNARE_PAD_INDEX = 1
    private const val HAT_PAD_INDEX = 2
    private const val CLAP_PAD_INDEX = 3
    private const val PERC_PAD_INDEX = 6

    fun stepDurationMs(bpm: Int): Long {
        return max(72L, 60_000L / bpm.coerceIn(60, 180) / 4L)
    }

    fun timingOffsetMs(
        padIndex: Int,
        step: Int,
        stepDurationMs: Long,
    ): Long {
        val humanizeMs = when (padIndex) {
            KICK_PAD_INDEX -> if (step in listOf(6, 14)) 18L else 0L
            SNARE_PAD_INDEX -> 22L
            CLAP_PAD_INDEX -> 28L
            HAT_PAD_INDEX -> if (step % 2 == 1) 12L else 4L
            PERC_PAD_INDEX -> 16L
            else -> 8L
        }
        return humanizeMs.coerceAtMost(stepDurationMs / 3L)
    }

    fun evaluate(patternMasks: List<Int>): BeatMachineGrooveReport {
        val normalizedMasks = patternMasks.take(8).let { masks ->
            masks + List((8 - masks.size).coerceAtLeast(0)) { 0 }
        }
        val kickSteps = normalizedMasks[KICK_PAD_INDEX].steps()
        val snareSteps = normalizedMasks[SNARE_PAD_INDEX].steps()
        val hatSteps = normalizedMasks[HAT_PAD_INDEX].steps()
        val ghostSteps = (normalizedMasks[CLAP_PAD_INDEX] or normalizedMasks[PERC_PAD_INDEX]).steps()
        val issues = mutableListOf<String>()

        val backbeatScore = if (snareSteps.anyNear(4) && snareSteps.anyNear(12)) {
            25
        } else {
            issues += "Needs a clear 2-and-4 backbeat."
            8
        }

        val kickScore = when {
            kickSteps.containsAll(listOf(0, 8)) && kickSteps.any { it in listOf(6, 7, 10, 14, 15) } -> 25
            kickSteps.containsAll(listOf(0, 8)) -> {
                issues += "Kick pattern is solid but too straight."
                16
            }
            else -> {
                issues += "Kick needs a stronger downbeat and turnaround."
                8
            }
        }

        val hatScore = when {
            hatSteps.size >= 8 && hatSteps.hasAlternatingMotion() -> 20
            hatSteps.size >= 6 -> {
                issues += "Hats need more push-pull motion."
                13
            }
            else -> {
                issues += "Hats are too sparse to judge pocket."
                6
            }
        }

        val ghostScore = if (ghostSteps.any { it in listOf(3, 7, 10, 11, 15) }) {
            15
        } else {
            issues += "Add a ghost clap or percussion pickup before the backbeat."
            5
        }

        val timingScore = if (normalizedMasks.any { mask ->
                mask.steps().any { step -> timingOffsetMs(0, step, 156L) != timingOffsetMs(2, step, 156L) }
            }
        ) {
            15
        } else {
            issues += "Playback timing is too locked to the grid."
            5
        }

        val score = backbeatScore + kickScore + hatScore + ghostScore + timingScore
        return BeatMachineGrooveReport(
            score = score.coerceIn(0, 100),
            label = when {
                score >= 82 -> "loose pocket"
                score >= 64 -> "promising pocket"
                else -> "too straight"
            },
            issues = issues,
        )
    }

    private fun Int.steps(): List<Int> {
        return (0 until STEP_COUNT).filter { step -> this and (1 shl step) != 0 }
    }

    private fun List<Int>.anyNear(target: Int): Boolean {
        return any { step -> abs(step - target) <= 1 }
    }

    private fun List<Int>.hasAlternatingMotion(): Boolean {
        if (size < 4) return false
        return zipWithNext().any { (first, second) -> second - first == 1 } &&
            zipWithNext().any { (first, second) -> second - first == 2 }
    }
}

internal data class BeatMachineGrooveReport(
    val score: Int,
    val label: String,
    val issues: List<String>,
)
