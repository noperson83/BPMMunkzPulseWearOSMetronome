package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class HearNoEvilStructureSectionStageTest {
    @Test
    fun repeatedPhraseShapesGetStableSectionLetters() {
        val sectionA = listOf("C", "G", "Am", "F")
        val sectionB = listOf("Dm", "G", "C", "C")

        val labels = TunerTheoryTestApi.detectStructureSectionLabels(
            sectionA + sectionA + sectionA +
                sectionB + sectionB + sectionB +
                sectionA,
        )

        assertEquals("A", labels[sectionA.size * 3 - 1])
        assertEquals("B", labels[sectionA.size * 3 + sectionB.size * 3 - 1])
        assertEquals("A", labels.last())
    }
}
