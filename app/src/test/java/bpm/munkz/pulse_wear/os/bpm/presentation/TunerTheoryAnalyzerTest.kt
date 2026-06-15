package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerTheoryAnalyzerTest {
    @Test
    fun matchesBasicChordTemplatesFromSyntheticChroma() {
        assertEquals("C", TunerTheoryTestApi.matchChord(listOf("C", "E", "G")))
        assertEquals("Am", TunerTheoryTestApi.matchChord(listOf("A", "C", "E")))
        assertEquals("G7", TunerTheoryTestApi.matchChord(listOf("G", "B", "D", "F")))
        assertEquals("Fmaj7", TunerTheoryTestApi.matchChord(listOf("F", "A", "C", "E")))
        assertEquals("Bm7b5", TunerTheoryTestApi.matchChord(listOf("B", "D", "F", "A")))
        assertEquals("Asus2", TunerTheoryTestApi.matchChord(listOf("A", "B", "E"), preferredRoot = "A"))
        assertEquals("Dsus4", TunerTheoryTestApi.matchChord(listOf("D", "G", "A"), preferredRoot = "D"))
    }

    @Test
    fun detectsMajorAndMinorKeysFromSyntheticProfiles() {
        assertEquals(
            "C",
            TunerTheoryTestApi.detectKey(
                mapOf(
                    "C" to 10f,
                    "D" to 5f,
                    "E" to 8f,
                    "F" to 5f,
                    "G" to 9f,
                    "A" to 4f,
                    "B" to 3f,
                ),
            ),
        )
        assertEquals(
            "Am",
            TunerTheoryTestApi.detectKey(
                mapOf(
                    "A" to 10f,
                    "B" to 4f,
                    "C" to 8f,
                    "D" to 5f,
                    "E" to 9f,
                    "F" to 5f,
                    "G" to 5f,
                ),
            ),
        )
    }

    @Test
    fun combinesChromaAndChordSequenceForKeyDetection() {
        val cMajorLikeChroma = mapOf(
            "C" to 10f,
            "D" to 5f,
            "E" to 8f,
            "F" to 5f,
            "G" to 9f,
            "A" to 6f,
            "B" to 3f,
        )
        assertEquals(
            "C",
            TunerTheoryTestApi.estimateSongKey(
                weightedNotes = cMajorLikeChroma,
                chordLabels = listOf("C", "G", "Am", "F", "G7", "C"),
            ),
        )
        assertEquals(
            "Am",
            TunerTheoryTestApi.estimateSongKey(
                weightedNotes = cMajorLikeChroma,
                chordLabels = listOf("Am", "F", "G", "Am", "E7", "Am"),
            ),
        )
    }

    @Test
    fun smootherIgnoresOneWeakOutlierButAllowsStrongFifthMotion() {
        val heldChord = TunerTheoryTestApi.smoothCandidateLabels(
            listOf(
                listOf("C" to 0.92f, "G7" to 0.55f),
                listOf("C" to 0.9f, "G7" to 0.56f),
                listOf("F#" to 0.62f, "C" to 0.59f),
                listOf("C" to 0.91f, "F#" to 0.52f),
            ),
        )
        assertEquals(listOf("C", "C", "C", "C"), heldChord)

        val fifthMove = TunerTheoryTestApi.smoothCandidateLabels(
            listOf(
                listOf("C" to 0.9f, "G7" to 0.52f),
                listOf("C" to 0.86f, "G7" to 0.58f),
                listOf("G7" to 0.9f, "C" to 0.48f),
                listOf("G7" to 0.88f, "C" to 0.5f),
            ),
        )
        assertEquals("G7", fifthMove.last())
    }

    @Test
    fun chordMatchExposesConfidenceAndAlternate() {
        val (label, confidence, alternate) = TunerTheoryTestApi.matchChordWithDetails(listOf("G", "B", "D", "F"))
        assertEquals("G7", label)
        assertTrue(confidence >= 0.5f)
        assertNotNull(alternate)
    }

    @Test
    fun preferredBassRootKeepsDominantFromCollapsingIntoSixChordColor() {
        assertEquals(
            "A7",
            TunerTheoryTestApi.matchWeightedChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "C#" to 0.72f,
                    "G" to 0.66f,
                    "F" to 0.58f,
                    "C" to 0.54f,
                    "D" to 0.48f,
                ),
                preferredRoot = "A",
            ),
        )
        assertEquals(
            "D7",
            TunerTheoryTestApi.matchWeightedChord(
                weightedNotes = mapOf(
                    "D" to 1.0f,
                    "F#" to 0.72f,
                    "C" to 0.68f,
                    "A" to 0.5f,
                    "F" to 0.45f,
                    "G" to 0.38f,
                ),
                preferredRoot = "D",
            ),
        )
        assertEquals(
            "E7",
            TunerTheoryTestApi.matchWeightedChord(
                weightedNotes = mapOf(
                    "E" to 1.0f,
                    "G#" to 0.7f,
                    "D" to 0.66f,
                    "B" to 0.48f,
                    "F" to 0.44f,
                    "A" to 0.36f,
                ),
                preferredRoot = "E",
            ),
        )
    }

    @Test
    fun structureDetectorLocksRepeatedFourBarLoop() {
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(
            listOf("C", "Am", "F", "G", "C", "Am", "F", "G"),
        )
        assertEquals(listOf("C", "Am", "F", "G"), display)
        assertEquals(4, length)
        assertTrue(confidence >= 0.78f)
    }

    @Test
    fun structureDetectorLocksRepeatedTwelveBarLoopWithoutGenreAssumption() {
        val phrase = listOf("A7", "A7", "A7", "A7", "D7", "D7", "A7", "A7", "E7", "D7", "A7", "E7")
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(phrase + phrase)
        assertEquals(phrase, display)
        assertEquals(12, length)
        assertTrue(confidence >= 0.78f)
    }

    @Test
    fun structureDetectorAllowsSixteenBarVersePhrase() {
        val phrase = listOf(
            "Am", "Am", "G", "G",
            "Am", "Am", "F", "F",
            "Am", "Am", "G", "G",
            "F", "F", "E", "E",
        )
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(phrase + phrase)
        assertEquals(phrase, display)
        assertEquals(16, length)
        assertTrue(confidence >= 0.78f)
    }

    @Test
    fun structureDetectorDoesNotLockNearRepeatTooEarly() {
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(
            listOf("C", "Am", "F", "G", "C", "Dm", "F", "G"),
        )
        assertEquals(listOf("C", "Am", "F", "G", "C", "Dm", "F", "G"), display)
        assertEquals(null, length)
        assertTrue(confidence < 0.42f)
    }
}
