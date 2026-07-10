package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HearNoEvilRootColorSplitStageTest {
    @Test
    fun bassBackedRootSurvivesLoudUpperColorNotes() {
        assertEquals(
            "G",
            TunerTheoryTestApi.rootColorSeparatedChord(
                root = "G",
                rootConfidence = 0.72f,
                subBass = mapOf("G" to 1.0f),
                bass = mapOf("G" to 0.9f, "D" to 0.42f),
                color = mapOf(
                    "A" to 1.35f,
                    "B" to 0.92f,
                    "D" to 0.86f,
                    "G" to 0.25f,
                ),
            ),
        )
    }

    @Test
    fun splitKeepsMinorThirdAsQualityEvidence() {
        assertEquals(
            "Am7",
            TunerTheoryTestApi.rootColorSeparatedChord(
                root = "A",
                rootConfidence = 0.78f,
                subBass = mapOf("A" to 1.0f),
                bass = mapOf("A" to 0.86f, "E" to 0.52f),
                color = mapOf(
                    "A" to 0.34f,
                    "C" to 0.78f,
                    "E" to 0.72f,
                    "G" to 0.26f,
                    "B" to 0.31f,
                ),
            ),
        )
    }

    @Test
    fun liveDisplayKeepsMinorCoreBeforeColorIsStable() {
        val frames = List(2) {
            "A" to mapOf(
                "A" to 1.0f,
                "C" to 0.82f,
                "E" to 0.76f,
                "G" to 0.42f,
            )
        }

        assertEquals(
            listOf("--", "Am"),
            TunerTheoryTestApi.liveSongChordLabelsFromWeightedFrames(frames),
        )
    }

    @Test
    fun liveDisplayFlipsFromMajorToMinorOnClearSameRootThird() {
        val majorFrames = List(2) {
            "A" to mapOf("A" to 1.0f, "C#" to 0.74f, "E" to 0.82f)
        }
        val minorFrame = "A" to mapOf("A" to 1.0f, "C" to 0.62f, "E" to 0.82f, "C#" to 0.18f)

        assertEquals(
            "Am",
            TunerTheoryTestApi.liveSongChordLabelsFromWeightedFrames(majorFrames + minorFrame).last(),
        )
    }

    @Test
    fun suspendedDominantDoesNotUseMalformedName() {
        val frames = List(4) {
            "E" to mapOf(
                "E" to 1.0f,
                "A" to 0.84f,
                "B" to 0.78f,
                "D" to 0.48f,
            )
        }

        TunerTheoryTestApi.liveSongChordLabelsFromWeightedFrames(frames)
            .forEach { label -> assertFalse(label.contains("sus47")) }
    }
}
