package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HearNoEvilChromaCleaningStageTest {
    @Test
    fun highOnlyBassHarmonicGhostsAreReduced() {
        val cleaned = TunerTheoryTestApi.cleanedSongChromaForTest(
            subBass = mapOf("C" to 1.0f),
            bass = mapOf("C" to 1.0f),
            high = mapOf("E" to 0.9f, "G" to 0.86f, "A#" to 0.78f),
            full = mapOf("E" to 0.82f, "G" to 0.78f, "A#" to 0.7f),
        )

        assertTrue("Root should stay stronger than high-only E ghost: $cleaned", cleaned.level("C") > cleaned.level("E") * 3.0f)
        assertTrue("Root should stay stronger than high-only G ghost: $cleaned", cleaned.level("C") > cleaned.level("G") * 3.0f)
        assertTrue("Root should stay stronger than high-only A# ghost: $cleaned", cleaned.level("C") > cleaned.level("A#") * 3.0f)
    }

    @Test
    fun realThirdSurvivesWhenItHasLowMidAndMidSupport() {
        val cleaned = TunerTheoryTestApi.cleanedSongChromaForTest(
            subBass = mapOf("C" to 1.0f),
            bass = mapOf("C" to 1.0f, "G" to 0.5f),
            lowMid = mapOf("C" to 0.78f, "E" to 0.72f, "G" to 0.7f),
            mid = mapOf("E" to 0.82f, "G" to 0.68f),
            high = mapOf("E" to 0.62f, "G" to 0.56f),
            full = mapOf("C" to 0.95f, "E" to 0.82f, "G" to 0.8f),
        )

        assertTrue("Real major third should survive cleaning: $cleaned", cleaned.level("E") > cleaned.level("C") * 0.45f)
        assertEquals(
            "C" to listOf("C", "E", "G"),
            TunerTheoryTestApi.matchRootAnchoredDisplayChordDetails(
                weightedNotes = cleaned,
                preferredRoot = "C",
                rootConfidence = 0.82f,
            ),
        )
    }

    @Test
    fun unsupportedColorNotesDoNotBecomeAddNineOrSixChords() {
        val cleaned = TunerTheoryTestApi.cleanedSongChromaForTest(
            subBass = mapOf("C" to 1.0f),
            bass = mapOf("C" to 1.0f),
            lowMid = mapOf("C" to 0.7f, "G" to 0.52f),
            mid = mapOf("C" to 0.62f, "G" to 0.5f),
            high = mapOf("D" to 0.9f, "A" to 0.86f),
            full = mapOf("D" to 0.82f, "A" to 0.78f),
        )

        val chord = TunerTheoryTestApi.matchRootAnchoredDisplayChordDetails(
            weightedNotes = cleaned,
            preferredRoot = "C",
            rootConfidence = 0.82f,
        )?.first

        assertTrue("Unsupported D/A color should not become Cadd9/C6, got $chord from $cleaned", chord != "Cadd9" && chord != "C6")
    }

    private fun Map<String, Float>.level(note: String): Float = this[note] ?: 0f
}
