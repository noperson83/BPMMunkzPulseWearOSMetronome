package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HearNoEvilFunctionalHarmonyStageTest {
    @Test
    fun majorKeyLabelsCoreFunctions() {
        assertHarmony("C", "C", roman = "I", function = "tonic", tags = listOf("tonic"))
        assertHarmony("C", "Dm", roman = "ii", function = "predominant", tags = listOf("predominant"))
        assertHarmony("C", "F", roman = "IV", function = "predominant", tags = listOf("predominant"))
        assertHarmony("C", "G7", roman = "V7", function = "dominant", tags = listOf("dominant"))
        assertHarmony("C", "Am", roman = "vi", function = "color", tags = emptyList())
    }

    @Test
    fun minorKeyLabelsNaturalMinorColors() {
        assertHarmony("Am", "Am", roman = "i", function = "tonic", tags = listOf("tonic"))
        assertHarmony("Am", "Dm", roman = "iv", function = "predominant", tags = listOf("predominant"))
        assertHarmony("Am", "E7", roman = "V7", function = "dominant", tags = listOf("dominant"))
        assertHarmony("Am", "F", roman = "bVI", function = "color", tags = listOf("minor color"))
        assertHarmony("Am", "G", roman = "bVII", function = "color", tags = listOf("minor color"))
    }

    @Test
    fun borrowedAndSecondaryDominantsStayAnnotations() {
        assertHarmony("C", "Fm", roman = "iv", function = "predominant", tags = listOf("predominant", "borrowed"))
        assertHarmony("C", "Bb", roman = "bVII", function = "color", tags = listOf("borrowed"))
        assertHarmony("C", "A7", roman = "VI7", function = "dominant", tags = listOf("secondary dominant V/D"))
        assertHarmony("C", "D7#9", roman = "II7", function = "dominant", tags = listOf("secondary dominant V/G", "altered dominant"))
    }

    @Test
    fun diminishedAndSuspendedColorsAreTaggedButDoNotChangeChord() {
        assertHarmony("C", "Bm7b5", roman = "VIIm7b5", function = "passing", tags = listOf("half-diminished", "leading/passing"))
        assertHarmony("C", "Bdim", roman = "VIIdim", function = "passing", tags = listOf("diminished", "leading/passing"))
        assertHarmony("C", "Gsus4", roman = "Vsus", function = "dominant", tags = listOf("dominant", "suspended"))
    }

    @Test
    fun compactSummaryIsReadyForLiveDisplay() {
        assertEquals("V7 dominant", TunerTheoryTestApi.functionalHarmonySummary("C", "G7"))
        assertEquals("iv predominant borrowed", TunerTheoryTestApi.functionalHarmonySummary("C", "Fm"))
        assertEquals("II7 dominant secondary dominant V/G altered dominant", TunerTheoryTestApi.functionalHarmonySummary("C", "D7#9"))
    }

    private fun assertHarmony(
        key: String,
        chord: String,
        roman: String,
        function: String,
        tags: List<String>,
    ) {
        val info = TunerTheoryTestApi.functionalHarmony(key, chord)
            ?: error("Expected harmony info for $key / $chord")

        assertEquals(roman, info.roman)
        assertEquals(function, info.function)
        tags.forEach { tag ->
            assertTrue("Expected $chord in $key to include tag $tag, got ${info.tags}", tag in info.tags)
        }
        assertEquals("Unexpected extra tags for $chord in $key", tags.sorted(), info.tags.sorted())
    }
}
