package bpm.munkz.pulse_wear.os.bpm.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerTheoryAnalyzerTest {
    private val chromaticRoots = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

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
    fun recognizesMajorAndMinorThirdIntervalsAcrossAllRoots() {
        chromaticRoots.forEachIndexed { rootIndex, root ->
            val majorThird = noteAt(rootIndex, 4)
            val minorThird = noteAt(rootIndex, 3)

            assertEquals(
                "major third interval from $root should imply major root",
                root,
                TunerTheoryTestApi.matchChord(listOf(root, majorThird), preferredRoot = root),
            )
            assertEquals(
                "minor third interval from $root should imply minor root",
                "${root}m",
                TunerTheoryTestApi.matchChord(listOf(root, minorThird), preferredRoot = root),
            )
        }
    }

    @Test
    fun recognizesMajorAndMinorTriadsAcrossAllRoots() {
        chromaticRoots.forEachIndexed { rootIndex, root ->
            assertEquals(
                "major triad from $root",
                root,
                TunerTheoryTestApi.matchChord(
                    listOf(root, noteAt(rootIndex, 4), noteAt(rootIndex, 7)),
                    preferredRoot = root,
                ),
            )
            assertEquals(
                "minor triad from $root",
                "${root}m",
                TunerTheoryTestApi.matchChord(
                    listOf(root, noteAt(rootIndex, 3), noteAt(rootIndex, 7)),
                    preferredRoot = root,
                ),
            )
        }
    }

    @Test
    fun recognizesSeventhChordQualitiesAcrossAllRoots() {
        chromaticRoots.forEachIndexed { rootIndex, root ->
            assertEquals(
                "dominant seventh from $root",
                "${root}7",
                TunerTheoryTestApi.matchChord(
                    listOf(root, noteAt(rootIndex, 4), noteAt(rootIndex, 7), noteAt(rootIndex, 10)),
                    preferredRoot = root,
                ),
            )
            assertEquals(
                "major seventh from $root",
                "${root}maj7",
                TunerTheoryTestApi.matchChord(
                    listOf(root, noteAt(rootIndex, 4), noteAt(rootIndex, 7), noteAt(rootIndex, 11)),
                    preferredRoot = root,
                ),
            )
            assertEquals(
                "minor seventh from $root",
                "${root}m7",
                TunerTheoryTestApi.matchChord(
                    listOf(root, noteAt(rootIndex, 3), noteAt(rootIndex, 7), noteAt(rootIndex, 10)),
                    preferredRoot = root,
                ),
            )
        }
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
    fun keySuggestionDoesNotDisplayChordExtensions() {
        assertEquals(
            "Am",
            TunerTheoryTestApi.estimateSongKey(
                weightedNotes = mapOf(
                    "A" to 10f,
                    "C" to 8f,
                    "E" to 7f,
                    "G" to 5f,
                ),
                chordLabels = listOf("Am7", "Am7", "Am7", "Am7", "C7sus4", "Am7", "Am7"),
            ),
        )
    }

    @Test
    fun relativeMinorWinsWhenProgressionStartsAndLandsOnMinorTonic() {
        val cMajorOrAMinorChroma = mapOf(
            "C" to 10f,
            "D" to 5f,
            "E" to 8f,
            "F" to 6f,
            "G" to 9f,
            "A" to 9f,
            "B" to 3f,
        )

        assertEquals(
            "Am",
            TunerTheoryTestApi.estimateSongKey(
                weightedNotes = cMajorOrAMinorChroma,
                chordLabels = listOf("Am", "G", "F", "E", "Am", "G", "F", "E", "Am"),
            ),
        )
    }

    @Test
    fun relativeMajorStillWinsWhenProgressionStartsAndLandsOnMajorTonic() {
        val cMajorOrAMinorChroma = mapOf(
            "C" to 10f,
            "D" to 5f,
            "E" to 8f,
            "F" to 6f,
            "G" to 9f,
            "A" to 9f,
            "B" to 3f,
        )

        assertEquals(
            "C",
            TunerTheoryTestApi.estimateSongKey(
                weightedNotes = cMajorOrAMinorChroma,
                chordLabels = listOf("C", "G", "Am", "F", "C", "G", "F", "C"),
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
    fun songMainOutputCollapsesColorChordsToMajorMinorCore() {
        assertEquals(
            "A",
            TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "C#" to 0.72f,
                    "G" to 0.66f,
                    "E" to 0.52f,
                ),
                preferredRoot = "A",
            ),
        )
        assertEquals(
            "Am",
            TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "C" to 0.82f,
                    "G" to 0.66f,
                    "E" to 0.52f,
                ),
                preferredRoot = "A",
            ),
        )
    }

    @Test
    fun majorMinorCoreSurvivesOctaveWeightedDistractors() {
        assertEquals(
            "A",
            TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                weightedNotes = mapOf(
                    "A" to 3.4f,
                    "E" to 2.2f,
                    "C#" to 1.05f,
                    "C" to 0.62f,
                    "G" to 0.54f,
                    "D" to 0.48f,
                ),
                preferredRoot = "A",
                rootConfidence = 0.86f,
            ),
        )
        assertEquals(
            "Am",
            TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                weightedNotes = mapOf(
                    "A" to 3.4f,
                    "E" to 2.2f,
                    "C" to 1.05f,
                    "C#" to 0.58f,
                    "G" to 0.54f,
                    "D" to 0.48f,
                ),
                preferredRoot = "A",
                rootConfidence = 0.86f,
            ),
        )
    }

    @Test
    fun majorMinorCoreIgnoresCommonColorNotes() {
        assertEquals(
            "A",
            TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "C#" to 0.72f,
                    "E" to 0.62f,
                    "G" to 0.55f,
                    "B" to 0.5f,
                    "F#" to 0.46f,
                    "D" to 0.34f,
                ),
                preferredRoot = "A",
                rootConfidence = 0.82f,
            ),
        )
        assertEquals(
            "Am",
            TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "C" to 0.72f,
                    "E" to 0.62f,
                    "G" to 0.55f,
                    "B" to 0.5f,
                    "F" to 0.46f,
                    "D" to 0.34f,
                ),
                preferredRoot = "A",
                rootConfidence = 0.82f,
            ),
        )
    }

    @Test
    fun weakThirdWithStrongColorStaysOnRootButKeepsQualityCautious() {
        assertEquals(
            "A",
            TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "E" to 0.88f,
                    "G" to 0.82f,
                    "D" to 0.78f,
                    "C#" to 0.34f,
                    "C" to 0.3f,
                ),
                preferredRoot = "A",
                rootConfidence = 0.82f,
            ),
        )
    }

    @Test
    fun rootAnchoredDisplayDoesNotJumpToUnrelatedChordWhenThirdIsMessy() {
        assertEquals(
            "E",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf(
                    "E" to 3.0f,
                    "B" to 2.2f,
                    "D" to 1.2f,
                    "F" to 0.92f,
                    "G" to 0.78f,
                    "G#" to 0.8f,
                    "C" to 0.58f,
                ),
                preferredRoot = "E",
                rootConfidence = 0.86f,
            ),
        )
    }

    @Test
    fun rootAnchoredDisplayPromotesMajorAndMinorOnlyWhenThirdIsClear() {
        assertEquals(
            "A",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf("A" to 3.0f, "E" to 2.1f, "C#" to 0.9f, "C" to 0.22f),
                preferredRoot = "A",
                rootConfidence = 0.86f,
            ),
        )
        assertEquals(
            "Am",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf("A" to 3.0f, "E" to 2.1f, "C" to 0.9f, "C#" to 0.22f),
                preferredRoot = "A",
                rootConfidence = 0.86f,
            ),
        )
    }

    @Test
    fun rootAnchoredDisplayDoesNotFlashMajorBeforeMinorThirdArrives() {
        assertEquals(
            "A",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf("A" to 3.0f, "E" to 2.2f, "C#" to 0.32f, "C" to 0.24f),
                preferredRoot = "A",
                rootConfidence = 0.86f,
            ),
        )
        assertEquals(
            "Am",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf("A" to 3.0f, "E" to 2.2f, "C" to 0.5f, "C#" to 0.22f),
                preferredRoot = "A",
                rootConfidence = 0.86f,
            ),
        )
    }

    @Test
    fun rootAnchoredDisplayKeepsGAndDWhenFftColorsAreMisleading() {
        assertEquals(
            "G",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "A#" to 0.92f,
                    "F" to 0.9f,
                    "F#" to 0.86f,
                ),
                preferredRoot = "G",
                rootConfidence = 0.86f,
            ),
        )
        assertEquals(
            "D",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf(
                    "A" to 1.0f,
                    "G" to 0.92f,
                    "C" to 0.8f,
                    "F" to 0.74f,
                ),
                preferredRoot = "D",
                rootConfidence = 0.86f,
            ),
        )
    }

    @Test
    fun rootAnchoredDisplayAddsColorOnlyAroundAnchoredRoot() {
        assertEquals(
            "D7",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf(
                    "D" to 1.0f,
                    "F#" to 0.72f,
                    "A" to 0.78f,
                    "C" to 0.58f,
                ),
                preferredRoot = "D",
                rootConfidence = 0.86f,
            ),
        )
        assertEquals(
            "Gsus4",
            TunerTheoryTestApi.matchRootAnchoredDisplayChord(
                weightedNotes = mapOf(
                    "G" to 1.0f,
                    "C" to 0.62f,
                    "D" to 0.72f,
                    "A#" to 0.2f,
                ),
                preferredRoot = "G",
                rootConfidence = 0.86f,
            ),
        )
    }

    @Test
    fun guitarVoicingsDetectMajorMinorCoreAcrossStringCounts() {
        val cases = listOf(
            GuitarChordCase(
                expected = "C",
                root = "C",
                notes = listOf("C", "E", "G"),
            ),
            GuitarChordCase(
                expected = "D",
                root = "D",
                notes = listOf("D", "A", "D", "F#"),
            ),
            GuitarChordCase(
                expected = "C",
                root = "C",
                notes = listOf("C", "E", "G", "C", "E"),
            ),
            GuitarChordCase(
                expected = "Am",
                root = "A",
                notes = listOf("A", "E", "A", "C", "E"),
            ),
            GuitarChordCase(
                expected = "F",
                root = "F",
                notes = listOf("F", "C", "F", "A", "C", "F"),
            ),
        )

        cases.forEach { case ->
            assertEquals(
                "${case.notes.size}-string ${case.expected} voicing",
                case.expected,
                TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                    weightedNotes = case.notes.toWeightedNoteCounts(),
                    preferredRoot = case.root,
                    rootConfidence = 0.82f,
                ),
            )
        }
    }

    @Test
    fun progressionFinderKeepsGuitarVoicedCAndDChords() {
        val c = "C" to listOf("C", "E", "G", "C", "E").toWeightedNoteCounts()
        val d = "D" to listOf("D", "A", "D", "F#").toWeightedNoteCounts()
        val f = "F" to listOf("F", "C", "F", "A", "C", "F").toWeightedNoteCounts()
        val am = "A" to listOf("A", "E", "A", "C", "E").toWeightedNoteCounts()
        val frames = listOf(c, c, d, d, f, f, am, am, c, c, d, d, f, f, am, am)

        assertEquals(
            listOf("C", "D", "F", "Am"),
            TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(frames),
        )
    }

    @Test
    fun progressionFinderKeepsWeakThirdGuitarCAndDChordsWhenRootIsKnown() {
        val c = "C" to mapOf("C" to 3.0f, "G" to 2.1f, "E" to 0.38f, "D" to 0.18f)
        val d = "D" to mapOf("D" to 3.0f, "A" to 2.1f, "F#" to 0.38f, "E" to 0.18f)
        val f = "F" to mapOf("F" to 3.0f, "C" to 2.1f, "A" to 0.62f, "D" to 0.2f)
        val am = "A" to mapOf("A" to 3.0f, "E" to 2.1f, "C" to 0.62f, "G" to 0.2f)
        val frames = listOf(c, c, d, d, f, f, am, am, c, c, d, d, f, f, am, am)

        assertEquals(
            listOf("C", "D", "F", "Am"),
            TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(frames),
        )
    }

    @Test
    fun fourBarsOfMinorChordStayMinorInDisplayAndProgression() {
        val am = "A" to listOf("A", "E", "A", "C", "E").toWeightedNoteCounts()
        val frames = beatFrames(am, am, am, am)

        val displayChord = TunerTheoryTestApi.matchRootAnchoredDisplayChord(
            weightedNotes = am.second,
            preferredRoot = "A",
            rootConfidence = 0.82f,
        )
        val progression = TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
            frames = frames,
            tempoBpm = 120,
            frameStepMs = 500L,
            tempoConfident = true,
        )

        assertEquals("Am", displayChord)
        assertEquals(listOf("Am"), progression)
    }

    @Test
    fun beatAwareProgressionKeepsGuitarStrumsOnBarGrid() {
        val c = "C" to listOf("C", "E", "G", "C", "E").toWeightedNoteCounts()
        val d = "D" to listOf("D", "A", "D", "F#").toWeightedNoteCounts()
        val f = "F" to listOf("F", "C", "F", "A", "C", "F").toWeightedNoteCounts()
        val am = "A" to listOf("A", "E", "A", "C", "E").toWeightedNoteCounts()
        val frames = beatFrames(c, d, f, am, c, d, f, am)

        assertEquals(
            listOf("C", "D", "F", "Am"),
            TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
                frames = frames,
                tempoBpm = 120,
                frameStepMs = 500L,
                tempoConfident = true,
            ),
        )
    }

    @Test
    fun pianoVoicingsDetectMajorMinorCoreWithLeftHandBass() {
        val cases = listOf(
            GuitarChordCase(
                expected = "C",
                root = "C",
                notes = listOf("C", "C", "G", "E", "G", "C"),
            ),
            GuitarChordCase(
                expected = "D",
                root = "D",
                notes = listOf("D", "D", "A", "F#", "A", "D"),
            ),
            GuitarChordCase(
                expected = "F",
                root = "F",
                notes = listOf("F", "F", "C", "A", "C", "F"),
            ),
            GuitarChordCase(
                expected = "Am",
                root = "A",
                notes = listOf("A", "A", "E", "C", "E", "A"),
            ),
        )

        cases.forEach { case ->
            assertEquals(
                "piano ${case.expected} voicing",
                case.expected,
                TunerTheoryTestApi.matchWeightedMajorMinorCoreChord(
                    weightedNotes = case.notes.toWeightedNoteCounts(),
                    preferredRoot = case.root,
                    rootConfidence = 0.86f,
                ),
            )
        }
    }

    @Test
    fun beatAwareProgressionKeepsPianoCompingOverSixteenBars() {
        val c = "C" to listOf("C", "C", "G", "E", "G", "C").toWeightedNoteCounts()
        val g = "G" to listOf("G", "G", "D", "B", "D", "G").toWeightedNoteCounts()
        val am = "A" to listOf("A", "A", "E", "C", "E", "A").toWeightedNoteCounts()
        val f = "F" to listOf("F", "F", "C", "A", "C", "F").toWeightedNoteCounts()
        val frames = beatFrames(c, c, g, g, am, am, f, f, c, c, g, g, am, am, f, f)

        val progression = TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
            frames = frames,
            tempoBpm = 120,
            frameStepMs = 500L,
            tempoConfident = true,
        )
        assertEquals(listOf("C", "G", "Am", "F"), progression)
    }

    @Test
    fun beatAwareProgressionSurvivesOneTransitionBeatPerBar() {
        val c = "C" to listOf("C", "E", "G", "C", "E").toWeightedNoteCounts()
        val g = "G" to listOf("G", "B", "D", "G", "B", "D").toWeightedNoteCounts()
        val am = "A" to listOf("A", "E", "A", "C", "E").toWeightedNoteCounts()
        val f = "F" to listOf("F", "C", "F", "A", "C", "F").toWeightedNoteCounts()
        val frames = noisyBeatFrames(c, g, am, f, c, g, am, f, c, g, am, f, c)

        val progression = TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
            frames = frames,
            tempoBpm = 120,
            frameStepMs = 500L,
            tempoConfident = true,
        )
        assertEquals(listOf("C", "G", "Am", "F"), progression)
    }

    @Test
    fun beatAwareGuitarProgressionStartedOnThirdBarRotatesBackToOne() {
        val c = "C" to listOf("C", "E", "G", "C", "E").toWeightedNoteCounts()
        val g = "G" to listOf("G", "B", "D", "G", "B", "D").toWeightedNoteCounts()
        val am = "A" to listOf("A", "E", "A", "C", "E").toWeightedNoteCounts()
        val f = "F" to listOf("F", "C", "F", "A", "C", "F").toWeightedNoteCounts()
        val phrase = listOf(c, c, g, g, am, am, f, f)
        val startedOnThirdBar = phrase.drop(2) + phrase.take(2)
        val frames = beatFrames(*(startedOnThirdBar + startedOnThirdBar + startedOnThirdBar).toTypedArray())

        assertEquals(
            listOf("C", "C", "G", "G", "Am", "Am", "F", "F"),
            TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
                frames = frames,
                tempoBpm = 120,
                frameStepMs = 500L,
                tempoConfident = true,
            ),
        )
    }

    @Test
    fun beatAwarePianoProgressionStartedOnThirdBarRotatesBackToOne() {
        val c = "C" to listOf("C", "C", "G", "E", "G", "C").toWeightedNoteCounts()
        val g = "G" to listOf("G", "G", "D", "B", "D", "G").toWeightedNoteCounts()
        val am = "A" to listOf("A", "A", "E", "C", "E", "A").toWeightedNoteCounts()
        val f = "F" to listOf("F", "F", "C", "A", "C", "F").toWeightedNoteCounts()
        val phrase = listOf(c, c, g, g, am, am, f, f)
        val startedOnThirdBar = phrase.drop(2) + phrase.take(2)
        val frames = beatFrames(*(startedOnThirdBar + startedOnThirdBar + startedOnThirdBar).toTypedArray())

        assertEquals(
            listOf("C", "C", "G", "G", "Am", "Am", "F", "F"),
            TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
                frames = frames,
                tempoBpm = 120,
                frameStepMs = 500L,
                tempoConfident = true,
            ),
        )
    }

    @Test
    fun beatAwareThirtyTwoBarsSwitchesBetweenTwoSimpleLoops() {
        val c = "C" to listOf("C", "E", "G", "C", "E").toWeightedNoteCounts()
        val g = "G" to listOf("G", "B", "D", "G", "B", "D").toWeightedNoteCounts()
        val am = "A" to listOf("A", "E", "A", "C", "E").toWeightedNoteCounts()
        val f = "F" to listOf("F", "C", "F", "A", "C", "F").toWeightedNoteCounts()
        val loopA = listOf(c, c, g, g, am, am, f, f)
        val loopB = listOf(am, am, f, f, c, c, g, g)
        val frames = beatFrames(*(loopA + loopA + loopB + loopB + loopB.take(1)).toTypedArray())

        assertEquals(
            listOf("Am", "Am", "F", "F", "C", "C", "G", "G"),
            TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
                frames = frames,
                tempoBpm = 120,
                frameStepMs = 500L,
                tempoConfident = true,
            ),
        )
    }

    @Test
    fun beatAwareThirtyTwoBarsSwitchesLoopsWithMinorSeventhsAndNinths() {
        val cAdd9 = "C" to mapOf("C" to 2.8f, "E" to 1.2f, "G" to 1.6f, "D" to 0.72f)
        val g9 = "G" to mapOf("G" to 2.8f, "B" to 1.2f, "D" to 1.6f, "F" to 0.72f, "A" to 0.58f)
        val am9 = "A" to mapOf("A" to 2.8f, "C" to 1.2f, "E" to 1.6f, "G" to 0.72f, "B" to 0.58f)
        val fAdd9 = "F" to mapOf("F" to 2.8f, "A" to 1.2f, "C" to 1.6f, "G" to 0.72f)
        val loopA = listOf(cAdd9, cAdd9, g9, g9, am9, am9, fAdd9, fAdd9)
        val loopB = listOf(am9, am9, fAdd9, fAdd9, cAdd9, cAdd9, g9, g9)
        val frames = beatFrames(*(loopA + loopA + loopB + loopB + loopB.take(1)).toTypedArray())

        assertEquals(
            listOf("Am", "Am", "F", "F", "C", "C", "G", "G"),
            TunerTheoryTestApi.learnedMajorMinorProgressionFromWeightedFrames(
                frames = frames,
                tempoBpm = 120,
                frameStepMs = 500L,
                tempoConfident = true,
            ),
        )
    }

    @Test
    fun learningProgressionCollectsEightMajorMinorCoreChords() {
        val progression = TunerTheoryTestApi.learnedMajorMinorProgression(
            chordLabels = listOf(
                "A7", "A7", "A7",
                "D7", "D7", "D7",
                "E7", "E7", "E7",
                "A7", "A7", "A7",
                "Am7", "Am7", "Am7",
            ),
        )

        assertEquals(listOf("A", "D", "E", "A", "Am"), progression)
    }

    @Test
    fun learnsFourChordProgressionAcrossSixteenBars() {
        val sixteenBars = listOf(
            "Cmaj7", "Cmaj7", "G7", "G7",
            "Am7", "Am7", "Fmaj7", "Fmaj7",
            "Cmaj7", "Cmaj7", "G7", "G7",
            "Am7", "Am7", "Fmaj7", "Fmaj7",
        )

        assertEquals(
            listOf("C", "G", "Am", "F"),
            TunerTheoryTestApi.learnedMajorMinorProgression(sixteenBars),
        )
    }

    @Test
    fun phraseDetectorLocksFourChordLoopOverSixteenBars() {
        val fourBarPhrase = listOf("C", "G", "Am", "F")
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(
            fourBarPhrase + fourBarPhrase + fourBarPhrase + fourBarPhrase,
        )

        assertEquals(fourBarPhrase, display)
        assertEquals(4, length)
        assertTrue(confidence >= 0.78f)
    }

    @Test
    fun progressionFinderLocksOneFiveSixFourLoopAcrossAllKeys() {
        chromaticRoots.forEachIndexed { rootIndex, root ->
            val five = noteAt(rootIndex, 7)
            val sixMinor = "${noteAt(rootIndex, 9)}m"
            val four = noteAt(rootIndex, 5)
            val phrase = listOf(root, five, sixMinor, four)
            val twoBarPhrase = phrase.flatMap { chord -> listOf(chord, chord) }

            assertEquals(
                "I-V-vi-IV in $root",
                phrase,
                TunerTheoryTestApi.learnedMajorMinorProgression(twoBarPhrase + twoBarPhrase),
            )
        }
    }

    @Test
    fun progressionFinderCollapsesColorChordsIntoFourChordLoop() {
        val progression = TunerTheoryTestApi.learnedMajorMinorProgression(
            chordLabels = listOf(
                "Cadd9", "Cadd9",
                "G7sus4", "G7sus4",
                "Am7", "Am7",
                "F6", "F6",
                "Cadd9", "Cadd9",
                "G7sus4", "G7sus4",
                "Am7", "Am7",
                "F6", "F6",
            ),
        )

        assertEquals(listOf("C", "G", "Am", "F"), progression)
    }

    @Test
    fun progressionFinderRecoversWhenFalseRestartBreaksTheLoop() {
        val progression = TunerTheoryTestApi.learnedMajorMinorProgression(
            chordLabels = listOf(
                "C", "C",
                "G", "G",
                "Am", "Am",
                "F", "F",
                "C", "C",
                "Dm", "Dm",
                "G", "G",
                "C", "C",
            ),
        )

        assertEquals(listOf("Dm", "G", "C"), progression)
    }

    @Test
    fun structureDetectorLocksEightBarLoopThatContainsTwoBarChords() {
        val phrase = listOf("C", "C", "G", "G", "Am", "Am", "F", "F")
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(phrase + phrase)

        assertEquals(phrase, display)
        assertEquals(8, length)
        assertTrue(confidence >= 0.78f)
    }

    @Test
    fun structureDetectorReportsActiveSlotForLockedEightBarLoop() {
        val phrase = listOf("C", "C", "G", "G", "Am", "Am", "F", "F")
        val (barIndex, locked) = TunerTheoryTestApi.detectStructureBarIndex(phrase + phrase)

        assertEquals(8, barIndex)
        assertTrue(locked)
    }

    @Test
    fun downbeatTrackerFindsStrongBeatOneLanding() {
        val beatOne = "A" to mapOf("A" to 3.2f, "E" to 1.4f, "C" to 0.9f)
        val beatTwo = "A" to mapOf("A" to 1.0f, "E" to 1.2f, "C" to 0.85f)
        val beatThree = "A" to mapOf("A" to 1.8f, "E" to 1.2f, "C" to 0.8f)
        val beatFour = "A" to mapOf("A" to 0.8f, "E" to 1.1f, "C" to 0.9f, "G" to 0.6f)
        val frames = listOf(
            beatOne, beatTwo, beatThree, beatFour,
            beatOne, beatTwo, beatThree, beatFour,
            beatOne, beatTwo, beatThree, beatFour,
        )
        val (beat, confidence) = TunerTheoryTestApi.downbeatGuessFromWeightedFrames(frames)

        assertEquals(1, beat)
        assertTrue(confidence > 0.2f)
    }

    @Test
    fun structureDetectorRotatesEightBarLoopStartedOnThirdBarBackToOne() {
        val phrase = listOf("C", "C", "G", "G", "Am", "Am", "F", "F")
        val startedOnThirdBar = phrase.drop(2) + phrase.take(2)
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(
            startedOnThirdBar + startedOnThirdBar + startedOnThirdBar,
        )

        assertEquals(phrase, display)
        assertEquals(8, length)
        assertTrue(confidence >= 0.78f)
    }

    @Test
    fun structureDetectorRotatesMinorEightBarLoopStartedOnThirdBarBackToOne() {
        val phrase = listOf("Am", "Am", "G", "G", "F", "F", "E", "E")
        val startedOnThirdBar = phrase.drop(2) + phrase.take(2)
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(
            startedOnThirdBar + startedOnThirdBar + startedOnThirdBar,
        )

        assertEquals(phrase, display)
        assertEquals(8, length)
        assertTrue(confidence >= 0.78f)
    }

    @Test
    fun structureDetectorDoesNotLockWhenMiddlePassHasWrongChord() {
        val clean = listOf("C", "G", "Am", "F")
        val wrong = listOf("C", "G", "Dm", "F")
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(clean + wrong + clean)

        assertEquals(clean + wrong + clean, display)
        assertEquals(null, length)
        assertTrue(confidence < 0.78f)
    }

    @Test
    fun structureDetectorDoesNotInventPhraseFromNonRepeatingSixteenBars() {
        val bars = listOf(
            "C", "Dm", "Em", "F",
            "G", "Am", "Bm7b5", "Cmaj7",
            "F", "G", "Em", "Am",
            "Dm", "G7", "C", "A7",
        )
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(bars)

        assertEquals(bars, display)
        assertEquals(null, length)
        assertTrue(confidence < 0.78f)
    }

    @Test
    fun weakBassRootKeepsUpperColorChordUncertain() {
        assertEquals(
            "D?",
            TunerTheoryTestApi.matchWeightedChordGrounded(
                weightedNotes = mapOf(
                    "C" to 1.0f,
                    "D" to 0.98f,
                    "F" to 0.95f,
                    "A" to 0.74f,
                ),
                rootConfidence = 0f,
            ),
        )
    }

    @Test
    fun songCoreHarmonyDoesNotCommitSixOrAddNineColorAsProgressionChord() {
        val fColorCloud = mapOf(
            "F" to 1.0f,
            "C" to 0.94f,
            "D" to 0.9f,
            "A" to 0.86f,
        )
        val cColorCloud = mapOf(
            "C" to 1.0f,
            "A" to 0.92f,
            "F" to 0.82f,
            "D" to 0.68f,
        )
        val aColorCloud = mapOf(
            "A" to 1.0f,
            "F" to 0.95f,
            "C" to 0.92f,
            "D" to 0.58f,
        )

        assertTrue(TunerTheoryTestApi.matchWeightedSongCoreChord(fColorCloud, preferredRoot = "F") != "F6")
        assertTrue(TunerTheoryTestApi.matchWeightedSongCoreChord(cColorCloud, preferredRoot = "C") != "Cadd9")
        assertTrue(TunerTheoryTestApi.matchWeightedSongCoreChord(aColorCloud, preferredRoot = "A") != "Am6")
    }

    @Test
    fun barProgressionFallsBackToRootForSusColorCloud() {
        val pollutedSusCloud = mapOf(
            "C" to 1.0f,
            "D" to 0.93f,
            "F" to 0.9f,
            "A" to 0.82f,
        )

        assertEquals(
            null,
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("C7sus4", "C7sus4", "C7sus4", "C7sus4", "A7"),
                weightedNotes = pollutedSusCloud,
            ),
        )
    }

    @Test
    fun barProgressionStillRejectsChordRootAgainstBarBassRoot() {
        val cDominantCloud = mapOf(
            "C" to 1.0f,
            "A" to 0.9f,
            "F" to 0.82f,
            "D" to 0.7f,
        )

        assertEquals(
            null,
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("C7", "C7", "C7", "C7", "A7"),
                weightedNotes = cDominantCloud,
                rootFrames = listOf("A" to 0.92f, "A" to 0.86f, "A" to 0.78f, "A" to 0.72f, "A" to 0.9f),
            ),
        )
    }

    @Test
    fun barProgressionFallsBackToRootWhenSixthOutweighsFlatSeventh() {
        val fSixthColorCloud = mapOf(
            "F" to 1.0f,
            "A" to 0.92f,
            "C" to 0.88f,
            "D" to 0.82f,
            "D#" to 0.24f,
        )

        assertEquals(
            "F",
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("F7", "F7", "F7", "F7", "A7"),
                weightedNotes = fSixthColorCloud,
                rootFrames = listOf("F" to 0.92f, "F" to 0.86f, "F" to 0.78f, "F" to 0.72f, "A" to 0.9f),
            ),
        )
    }

    @Test
    fun barBassRootIgnoresOneFrameRootSpike() {
        val roots = TunerTheoryTestApi.barBassRoots(
            listOf(
                "D" to 0.85f,
                "D" to 0.78f,
                "F" to 1.0f,
                "D" to 0.74f,
                "D" to 0.7f,
            ),
        )
        assertEquals("D", roots.last())
    }

    @Test
    fun barBassRootDoesNotCarryStaleRootIntoEmptyBar() {
        val roots = TunerTheoryTestApi.barBassRoots(
            listOf(
                "A" to 0.9f,
                "A" to 0.86f,
                "A" to 0.82f,
                "A" to 0.78f,
                null to 0f,
                null to 0f,
                null to 0f,
                null to 0f,
            ),
            tempoBpm = 120,
            meter = 4,
            frameStepMs = 500L,
        )

        assertEquals("--", roots.last())
    }

    @Test
    fun barProgressionCommitsRepeatedMidConfidenceRoot() {
        assertEquals(
            "D",
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("D", "D", "D", "D", "G"),
                weightedNotes = listOf("D", "A", "D", "F#").toWeightedNoteCounts(),
                rootFrames = listOf("D" to 0.47f, "D" to 0.47f, "D" to 0.47f, "D" to 0.47f, "G" to 0.9f),
            ),
        )
    }

    @Test
    fun barProgressionCommitsRepeatedMidConfidenceC() {
        assertEquals(
            "C",
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("C", "C", "C", "C", "G"),
                weightedNotes = listOf("C", "E", "G", "C", "E").toWeightedNoteCounts(),
                rootFrames = listOf("C" to 0.49f, "C" to 0.49f, "C" to 0.49f, "C" to 0.49f, "G" to 0.9f),
            ),
        )
    }

    @Test
    fun barProgressionWaitsWhenBassRootDropsOut() {
        assertEquals(
            null,
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("D", "D", "D", "D", "G"),
                weightedNotes = listOf("D", "A", "D", "F#").toWeightedNoteCounts(),
            ),
        )
    }

    @Test
    fun barProgressionRejectsOneFrameMidConfidenceRootSpike() {
        assertEquals(
            null,
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("D", "D", "D", "D", "G"),
                weightedNotes = listOf("D", "A", "D", "F#").toWeightedNoteCounts(),
                rootFrames = listOf(null to 0f, "D" to 0.47f, null to 0f, null to 0f, "G" to 0.9f),
            ),
        )
    }

    @Test
    fun bassRootConfidenceUsesOctaveSupport() {
        val (_, thinConfidence) = TunerTheoryTestApi.dominantRootWithOctaveSupport(
            weightedNotes = mapOf(
                "F" to 1.0f,
                "A" to 0.84f,
                "C" to 0.72f,
            ),
            octaveCounts = mapOf("F" to 1, "A" to 3),
        )
        val (supportedRoot, supportedConfidence) = TunerTheoryTestApi.dominantRootWithOctaveSupport(
            weightedNotes = mapOf(
                "A" to 1.0f,
                "F" to 0.84f,
                "C" to 0.72f,
            ),
            octaveCounts = mapOf("A" to 3, "F" to 1),
        )

        assertTrue(thinConfidence < supportedConfidence)
        assertEquals("A", supportedRoot)
        assertTrue(supportedConfidence >= 0.48f)
    }

    @Test
    fun subBassOnlyRootDoesNotLockSongToColorTone() {
        val (root, confidence) = TunerTheoryTestApi.selectedSongRootEvidence(
            subBass = "C" to 1.0f,
            bass = null,
        )

        assertEquals(null, root)
        assertEquals(0f, confidence)
    }

    @Test
    fun agreedSubBassAndBassRootStaysStrong() {
        val (root, confidence) = TunerTheoryTestApi.selectedSongRootEvidence(
            subBass = "A" to 0.9f,
            bass = "A" to 0.72f,
        )

        assertEquals("A", root)
        assertTrue(confidence >= 0.8f)
    }

    @Test
    fun bandBassRootSurvivesSubBassDisagreementWhenRepeated() {
        val (root, confidence) = TunerTheoryTestApi.selectedSongRootEvidence(
            subBass = "C" to 1.0f,
            bass = "G" to 0.35f,
        )

        assertEquals("G", root)
        assertTrue(confidence >= 0.3f)
    }

    @Test
    fun tunerPitchRootOverridesMisleadingSongChromaWhenBassIsWeak() {
        val (gRoot, gConfidence) = TunerTheoryTestApi.selectedSongRootEvidence(
            subBass = "C" to 1.0f,
            bass = "A#" to 0.41f,
            pitch = "G" to 0.74f,
        )
        val (dRoot, dConfidence) = TunerTheoryTestApi.selectedSongRootEvidence(
            subBass = "C" to 1.0f,
            bass = "A" to 0.49f,
            pitch = "D" to 0.74f,
        )

        assertEquals("G", gRoot)
        assertTrue(gConfidence >= 0.7f)
        assertEquals("D", dRoot)
        assertTrue(dConfidence >= 0.7f)
    }

    @Test
    fun barProgressionDoesNotCommitThinColorRoot() {
        assertEquals(
            null,
            TunerTheoryTestApi.committedBarChord(
                frameChords = listOf("F", "F", "F", "F", "A7"),
                weightedNotes = mapOf(
                    "F" to 1.0f,
                    "A" to 0.86f,
                    "C" to 0.74f,
                    "D" to 0.68f,
                ),
                rootFrames = listOf("F" to 0.42f, "F" to 0.44f, "F" to 0.4f, "F" to 0.43f, "A" to 0.8f),
            ),
        )
    }

    @Test
    fun structureDetectorDoesNotLockFourBarLoopAfterOnlyTwoPasses() {
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(
            listOf("C", "Am", "F", "G", "C", "Am", "F", "G"),
        )
        assertEquals(listOf("C", "Am", "F", "G", "C", "Am", "F", "G"), display)
        assertEquals(null, length)
        assertTrue(confidence < 0.42f)
    }

    @Test
    fun structureDetectorLocksRepeatedFourBarLoopAfterThreePasses() {
        val phrase = listOf("C", "Am", "F", "G")
        val (display, length, confidence) = TunerTheoryTestApi.detectStructure(phrase + phrase + phrase)
        assertEquals(phrase, display)
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

    private fun noteAt(rootIndex: Int, semitones: Int): String {
        return chromaticRoots[(rootIndex + semitones).floorMod(chromaticRoots.size)]
    }

    private fun Int.floorMod(divisor: Int): Int {
        return ((this % divisor) + divisor) % divisor
    }

    private data class GuitarChordCase(
        val expected: String,
        val root: String,
        val notes: List<String>,
    )

    private fun List<String>.toWeightedNoteCounts(): Map<String, Float> {
        return groupingBy { it }
            .eachCount()
            .mapValues { (_, count) -> count.toFloat() }
    }

    private fun beatFrames(
        vararg bars: Pair<String, Map<String, Float>>,
        beatsPerBar: Int = 4,
    ): List<Pair<String, Map<String, Float>>> {
        return bars.flatMap { bar -> List(beatsPerBar) { bar } }
    }

    private fun noisyBeatFrames(
        vararg bars: Pair<String, Map<String, Float>>,
        beatsPerBar: Int = 4,
    ): List<Pair<String, Map<String, Float>>> {
        return bars.flatMapIndexed { index, bar ->
            val next = bars.getOrNull(index + 1)
            List(beatsPerBar) { beat ->
                if (beat == beatsPerBar - 1 && next != null) {
                    bar.first to bar.second.mixWith(next.second, nextWeight = 0.28f)
                } else {
                    bar
                }
            }
        }
    }

    private fun Map<String, Float>.mixWith(
        next: Map<String, Float>,
        nextWeight: Float,
    ): Map<String, Float> {
        val currentWeight = 1f - nextWeight
        val mixed = mutableMapOf<String, Float>()
        forEach { (note, weight) -> mixed[note] = (mixed[note] ?: 0f) + weight * currentWeight }
        next.forEach { (note, weight) -> mixed[note] = (mixed[note] ?: 0f) + weight * nextWeight }
        return mixed
    }
}
