package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HearNoEvilRootChordStageTest {
    @Test
    fun singleNoteLocksRootWithoutInventingChordQuality() {
        assertEquals("C" to listOf("C", "--", "G"), rootAnchored("C", "C"))
        assertEquals("A" to listOf("A", "--", "E"), rootAnchored("A", "A"))
    }

    @Test
    fun rootEvidenceIsAvailableBeforeChordQuality() {
        val (root, confidence) = TunerTheoryTestApi.selectedSongRootEvidence(
            subBass = "A" to 0.78f,
            bass = "A" to 0.72f,
        )

        assertEquals("A", root)
        assertTrue(confidence >= 0.7f)
        assertEquals("A" to listOf("A", "--", "E"), rootAnchored("A", "A"))
    }

    @Test
    fun majorAndMinorThirdDoNotCollapseIntoRelativeKey() {
        assertEquals("C", TunerTheoryTestApi.matchChord(listOf("C", "E", "G"), preferredRoot = "C"))
        assertEquals("Am", TunerTheoryTestApi.matchChord(listOf("A", "C", "E"), preferredRoot = "A"))

        assertEquals(
            "C",
            TunerTheoryTestApi.estimateSongKey(
                weightedNotes = mapOf("C" to 10f, "E" to 8f, "G" to 9f),
                chordLabels = listOf("C", "C", "C", "C", "G", "C"),
            ),
        )
        assertTrue(
            TunerTheoryTestApi.estimateSongKey(
                weightedNotes = mapOf("A" to 10f, "C" to 8f, "E" to 9f),
                chordLabels = listOf("Am", "Am", "Am", "Am", "E7", "Am"),
            ) != "C",
        )
    }

    @Test
    fun rootFifthOnlyStaysNoThirdInsteadOfForcedMajorOrMinor() {
        assertEquals("A" to listOf("A", "--", "E"), rootAnchored("A", "A", "E"))
        assertEquals("E" to listOf("E", "--", "B"), rootAnchored("E", "E", "B"))
        assertEquals("D" to listOf("D", "--", "A"), rootAnchored("D", "D", "A"))
    }

    @Test
    fun weakThirdDoesNotOverrideStrongRootFifthEvidence() {
        assertEquals(
            "A" to listOf("A", "--", "E"),
            rootAnchored("A", mapOf("A" to 1f, "E" to 0.96f, "C" to 0.13f)),
        )
        assertEquals(
            "C" to listOf("C", "--", "G"),
            rootAnchored("C", mapOf("C" to 1f, "G" to 0.94f, "E" to 0.13f)),
        )
    }

    @Test
    fun strongThirdEvidenceRanksMajorAndMinorHonestly() {
        assertEquals(
            "C" to listOf("C", "E", "G"),
            rootAnchored("C", mapOf("C" to 1f, "G" to 0.92f, "E" to 0.62f)),
        )
        assertEquals(
            "Am" to listOf("A", "C", "E"),
            rootAnchored("A", mapOf("A" to 1f, "E" to 0.92f, "C" to 0.62f)),
        )
    }

    @Test
    fun suspensionRequiresSecondOrFourthToBeatThirdEvidence() {
        assertEquals(
            "Asus2" to listOf("A", "B", "E"),
            rootAnchored("A", mapOf("A" to 1f, "E" to 0.9f, "B" to 0.66f, "C" to 0.18f)),
        )
        assertEquals(
            "Dsus4" to listOf("D", "G", "A"),
            rootAnchored("D", mapOf("D" to 1f, "A" to 0.9f, "G" to 0.66f, "F#" to 0.18f)),
        )
        assertEquals(
            "A" to listOf("A", "--", "E"),
            rootAnchored("A", mapOf("A" to 1f, "E" to 0.9f, "B" to 0.24f, "C" to 0.2f)),
        )
    }

    @Test
    fun seventhColorRequiresRealFlatSeventhEvidence() {
        assertEquals(
            "C7" to listOf("C", "E", "G"),
            rootAnchored("C", mapOf("C" to 1f, "G" to 0.9f, "E" to 0.62f, "A#" to 0.34f)),
        )
        assertEquals(
            "Am7" to listOf("A", "C", "E"),
            rootAnchored("A", mapOf("A" to 1f, "E" to 0.9f, "C" to 0.62f, "G" to 0.34f)),
        )
        assertEquals(
            "C" to listOf("C", "E", "G"),
            rootAnchored("C", mapOf("C" to 1f, "G" to 0.9f, "E" to 0.62f, "A#" to 0.12f)),
        )
    }

    private fun rootAnchored(
        root: String,
        vararg notes: String,
    ): Pair<String, List<String>> {
        return rootAnchored(root, notes.toList().toWeightedNotes())
    }

    private fun rootAnchored(
        root: String,
        weightedNotes: Map<String, Float>,
    ): Pair<String, List<String>> {
        return TunerTheoryTestApi.matchRootAnchoredDisplayChordDetails(
            weightedNotes = weightedNotes,
            preferredRoot = root,
            rootConfidence = 0.82f,
        ) ?: error("Expected root-anchored chord for $root")
    }

    private fun List<String>.toWeightedNotes(): Map<String, Float> {
        return groupingBy { it }
            .eachCount()
            .mapValues { (_, count) -> count.toFloat() }
    }
}
