package bpm.munkz.pulse_wear.os.bpm.presentation

private val KeyNoteClasses = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val FlatKeyNoteClasses = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
private val MajorScaleOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
private val MinorScaleOffsets = listOf(0, 2, 3, 5, 7, 8, 10)
private val MinorPentatonicScaleOffsets = listOf(0, 3, 5, 7, 10)
private val MinorBluesScaleOffsets = listOf(0, 3, 5, 6, 7, 10)
private val TunerScaleProfiles = listOf(
    ScaleProfile(
        displaySuffix = "",
        offsets = MajorScaleOffsets,
        chordQualities = listOf("", "m", "m", "", "", "m", "dim"),
    ),
    ScaleProfile(
        displaySuffix = "m",
        offsets = MinorScaleOffsets,
        chordQualities = listOf("m", "dim", "", "m", "m", "", ""),
    ),
    ScaleProfile(
        displaySuffix = "m pent",
        offsets = MinorPentatonicScaleOffsets,
        chordQualities = listOf("m", "", "m", "m", ""),
    ),
    ScaleProfile(
        displaySuffix = "m blues",
        offsets = MinorBluesScaleOffsets,
        chordQualities = listOf("m", "", "m", "dim", "m", ""),
    ),
    ScaleProfile(
        displaySuffix = " Dor",
        offsets = listOf(0, 2, 3, 5, 7, 9, 10),
        chordQualities = listOf("m", "m", "", "", "m", "dim", ""),
    ),
    ScaleProfile(
        displaySuffix = " Mix",
        offsets = listOf(0, 2, 4, 5, 7, 9, 10),
        chordQualities = listOf("", "m", "dim", "", "m", "m", ""),
    ),
)

data class KeyAnalysis(
    val recentNotes: List<String>,
    val guessedKey: String?,
    val keyConfidence: Float = 0f,
    val likelyChords: List<String>,
    val chordConfidence: Float = 0f,
    val alternateChord: String? = null,
    val chordCandidates: List<String> = emptyList(),
    val keyCandidates: List<String> = emptyList(),
    val chordTones: List<String>,
    val chordProgression: List<String> = emptyList(),
    val harmonyProgression: List<String> = emptyList(),
    val phraseLengthBars: Int? = null,
    val phraseConfidence: Float = 0f,
    val phraseBarIndex: Int = 0,
    val phraseLocked: Boolean = false,
    val phraseSource: String = "",
    val phraseSectionLabel: String = "",
    val downbeatGuess: Int = 0,
    val downbeatConfidence: Float = 0f,
    val downbeatRoot: String? = null,
    val downbeatLandingStrength: Float = 0f,
    val harmonySummary: String = "",
    val songDebugLine: String = "",
    val scalarConfidence: Float = 0f,
    val leadScaleKey: String? = null,
)

private data class KeyGuess(
    val rootIndex: Int,
    val displayName: String,
    val scale: ScaleProfile,
    val score: Float,
)

private data class ScaleProfile(
    val displaySuffix: String,
    val offsets: List<Int>,
    val chordQualities: List<String>,
) {
    val prefersMinorTonality: Boolean
        get() = displaySuffix == "m" || displaySuffix == "m pent" || displaySuffix == "m blues" || displaySuffix == " Dor"
}

private data class ChordGuess(
    val label: String,
    val tones: List<String>,
    val score: Float,
    val rootIndex: Int,
    val suffix: String,
)

private data class ChordTemplate(
    val suffix: String,
    val offsets: List<Int>,
    val weights: List<Float>,
    val requiredOffsets: List<Int>,
    val colorOffsets: List<Int>,
)

private data class BluesProgressionGuess(
    val rootIndex: Int,
    val score: Float,
    val chords: List<ChordGuess>,
)

private data class MajorTriadEvidence(
    val rootIndex: Int,
    val score: Float,
)

private val TunerChordTemplates = listOf(
    ChordTemplate(
        suffix = "7#9b13",
        offsets = listOf(0, 4, 10, 3, 8),
        weights = listOf(3.6f, 2.6f, 3.0f, 3.2f, 3.3f),
        requiredOffsets = listOf(0, 4, 10, 3, 8),
        colorOffsets = listOf(3, 8),
    ),
    ChordTemplate(
        suffix = "7b9b13",
        offsets = listOf(0, 4, 10, 1, 8),
        weights = listOf(3.6f, 2.6f, 3.0f, 3.2f, 3.3f),
        requiredOffsets = listOf(0, 4, 10, 1, 8),
        colorOffsets = listOf(1, 8),
    ),
    ChordTemplate(
        suffix = "7#9",
        offsets = listOf(0, 4, 10, 3),
        weights = listOf(3.4f, 2.6f, 3.0f, 3.1f),
        requiredOffsets = listOf(0, 4, 10, 3),
        colorOffsets = listOf(3),
    ),
    ChordTemplate(
        suffix = "7b9",
        offsets = listOf(0, 4, 10, 1),
        weights = listOf(3.4f, 2.6f, 3.0f, 3.1f),
        requiredOffsets = listOf(0, 4, 10, 1),
        colorOffsets = listOf(1),
    ),
    ChordTemplate(
        suffix = "7#11",
        offsets = listOf(0, 4, 7, 10, 6),
        weights = listOf(3.4f, 2.6f, 1.4f, 3.0f, 3.1f),
        requiredOffsets = listOf(0, 4, 10, 6),
        colorOffsets = listOf(6),
    ),
    ChordTemplate(
        suffix = "7b13",
        offsets = listOf(0, 4, 10, 8),
        weights = listOf(3.4f, 2.6f, 3.0f, 3.3f),
        requiredOffsets = listOf(0, 4, 10, 8),
        colorOffsets = listOf(8),
    ),
    ChordTemplate(
        suffix = "7b5",
        offsets = listOf(0, 4, 6, 10),
        weights = listOf(3.4f, 2.5f, 3.0f, 2.9f),
        requiredOffsets = listOf(0, 4, 6, 10),
        colorOffsets = listOf(6),
    ),
    ChordTemplate(
        suffix = "9",
        offsets = listOf(0, 4, 7, 10, 2),
        weights = listOf(3.4f, 2.3f, 1.5f, 2.7f, 2.8f),
        requiredOffsets = listOf(0, 4, 10, 2),
        colorOffsets = listOf(2, 10),
    ),
    ChordTemplate(
        suffix = "m9",
        offsets = listOf(0, 3, 7, 10, 2),
        weights = listOf(3.2f, 2.3f, 1.5f, 2.5f, 2.7f),
        requiredOffsets = listOf(0, 3, 10, 2),
        colorOffsets = listOf(2, 10),
    ),
    ChordTemplate(
        suffix = "7sus4",
        offsets = listOf(0, 5, 7, 10),
        weights = listOf(3.3f, 2.7f, 1.5f, 2.9f),
        requiredOffsets = listOf(0, 5, 10),
        colorOffsets = listOf(5, 10),
    ),
    ChordTemplate(
        suffix = "sus2",
        offsets = listOf(0, 2, 7),
        weights = listOf(3.1f, 2.7f, 1.6f),
        requiredOffsets = listOf(0, 2),
        colorOffsets = listOf(2),
    ),
    ChordTemplate(
        suffix = "sus4",
        offsets = listOf(0, 5, 7),
        weights = listOf(3.1f, 2.7f, 1.6f),
        requiredOffsets = listOf(0, 5),
        colorOffsets = listOf(5),
    ),
    ChordTemplate(
        suffix = "7",
        offsets = listOf(0, 4, 7, 10),
        weights = listOf(3.5f, 2.5f, 1.7f, 3.0f),
        requiredOffsets = listOf(0, 4, 10),
        colorOffsets = listOf(10),
    ),
    ChordTemplate(
        suffix = "m7b5",
        offsets = listOf(0, 3, 6, 10),
        weights = listOf(3.2f, 2.1f, 3.0f, 2.2f),
        requiredOffsets = listOf(0, 3, 6, 10),
        colorOffsets = listOf(6, 10),
    ),
    ChordTemplate(
        suffix = "dim7",
        offsets = listOf(0, 3, 6, 9),
        weights = listOf(3.1f, 2.0f, 3.0f, 2.4f),
        requiredOffsets = listOf(0, 3, 6, 9),
        colorOffsets = listOf(6, 9),
    ),
    ChordTemplate(
        suffix = "aug7",
        offsets = listOf(0, 4, 8, 10),
        weights = listOf(3.2f, 2.2f, 3.2f, 2.2f),
        requiredOffsets = listOf(0, 4, 8, 10),
        colorOffsets = listOf(8, 10),
    ),
    ChordTemplate(
        suffix = "dim",
        offsets = listOf(0, 3, 6),
        weights = listOf(3.0f, 2.0f, 3.0f),
        requiredOffsets = listOf(0, 3, 6),
        colorOffsets = listOf(6),
    ),
    ChordTemplate(
        suffix = "aug",
        offsets = listOf(0, 4, 8),
        weights = listOf(3.0f, 2.0f, 3.0f),
        requiredOffsets = listOf(0, 4, 8),
        colorOffsets = listOf(8),
    ),
    ChordTemplate(
        suffix = "m7",
        offsets = listOf(0, 3, 7, 10),
        weights = listOf(3.0f, 2.0f, 1.6f, 2.4f),
        requiredOffsets = listOf(0, 3, 10),
        colorOffsets = listOf(10),
    ),
    ChordTemplate(
        suffix = "m6",
        offsets = listOf(0, 3, 7, 9),
        weights = listOf(3.0f, 2.1f, 1.5f, 2.4f),
        requiredOffsets = listOf(0, 3, 9),
        colorOffsets = listOf(9),
    ),
    ChordTemplate(
        suffix = "m",
        offsets = listOf(0, 3, 7),
        weights = listOf(3.0f, 2.0f, 1.6f),
        requiredOffsets = listOf(0, 3),
        colorOffsets = emptyList(),
    ),
    ChordTemplate(
        suffix = "",
        offsets = listOf(0, 4, 7),
        weights = listOf(3.0f, 2.0f, 1.6f),
        requiredOffsets = listOf(0, 4),
        colorOffsets = emptyList(),
    ),
)

fun String.toNoteClass(): String? {
    val noteClass = takeWhile { it.isLetter() || it == '#' }
    return noteClass.takeIf { it in KeyNoteClasses }
}

fun analyzeMusicalKey(noteClasses: List<String>): KeyAnalysis {
    if (noteClasses.size < 10) {
        return KeyAnalysis(
            recentNotes = noteClasses.takeLast(8),
            guessedKey = null,
            likelyChords = emptyList(),
            chordTones = emptyList(),
        )
    }

    val weightedCounts = FloatArray(KeyNoteClasses.size)
    noteClasses.forEachIndexed { position, noteClass ->
        val noteIndex = KeyNoteClasses.indexOf(noteClass)
        if (noteIndex >= 0) {
            val recencyWeight = 0.7f + (position.toFloat() / noteClasses.lastIndex.coerceAtLeast(1)) * 1.3f
            weightedCounts[noteIndex] += recencyWeight
        }
    }

    val guesses = mutableListOf<KeyGuess>()
    KeyNoteClasses.forEachIndexed { rootIndex, rootName ->
        TunerScaleProfiles.forEach { scale ->
            val score = scaleScore(
                counts = weightedCounts,
                rootIndex = rootIndex,
                scaleOffsets = scale.offsets,
                tonalCenterIndex = noteClasses.lastOrNull()?.let { KeyNoteClasses.indexOf(it) } ?: -1,
            )
            guesses += KeyGuess(
                rootIndex = rootIndex,
                displayName = "$rootName${scale.displaySuffix}",
                scale = scale,
                score = score,
            )
        }
    }

    val bestGuess = guesses.maxByOrNull { it.score }
    val majorTriadEvidence = majorTriadEvidence(weightedCounts)
    val leadScaleGuess = leadScaleGuess(weightedCounts, guesses, majorTriadEvidence)
    val bluesGuess = dominantBluesProgressionGuess(weightedCounts)
    val scalarGuess = bestGuess?.let { best ->
        val majorTriadGuess = majorTriadEvidence?.let { evidence ->
            guesses
                .firstOrNull { it.rootIndex == evidence.rootIndex && it.scale.displaySuffix.isBlank() }
                ?.takeIf { major ->
                    val bestScore = best.score.coerceAtLeast(0.01f)
                    major.score >= bestScore * 0.72f || evidence.score >= rootChordSignal(weightedCounts, best.rootIndex) * 1.08f
                }
        }
        val tonicMinorSeventh = guesses
            .filter { it.scale.displaySuffix == "m blues" }
            .filter { tonicMinorSeventhSignal(weightedCounts, it.rootIndex) >= 5.5f }
            .maxWithOrNull(
                compareBy<KeyGuess> { tonicMinorSeventhSignal(weightedCounts, it.rootIndex) }
                    .thenBy { it.score },
            )
            ?.takeIf { minor -> minor.score >= best.score * 0.72f }
        val preferredMinor = guesses
            .filter { it.scale.prefersMinorTonality }
            .filter { it.score >= best.score * 0.82f }
            .maxWithOrNull(
                compareBy<KeyGuess> { rootChordSignal(weightedCounts, it.rootIndex) }
                    .thenBy { it.score },
            )
        preferredMinor
            ?.takeIf { minor ->
                val minorChordSignal = rootChordSignal(weightedCounts, minor.rootIndex)
                val bestChordSignal = rootChordSignal(weightedCounts, best.rootIndex)
                minorChordSignal >= 4f &&
                    (minorChordSignal >= bestChordSignal * 0.85f || minor.score >= best.score * 0.94f)
            }
        majorTriadGuess
            ?: tonicMinorSeventh
            ?: preferredMinor
            ?: best
    }
    val guess = bluesGuess
        ?.takeIf { blues ->
            val scalarRootSignal = scalarGuess?.let { rootChordSignal(weightedCounts, it.rootIndex) } ?: 0f
            val tonicMinorSignal = scalarGuess?.let { tonicMinorSeventhSignal(weightedCounts, it.rootIndex) } ?: 0f
            val scalarIsMinorCenter = scalarGuess?.scale?.prefersMinorTonality == true && tonicMinorSignal >= 5.5f
            !scalarIsMinorCenter && blues.score >= scalarRootSignal * 1.35f
        }
        ?.let { blues ->
            KeyGuess(
                rootIndex = blues.rootIndex,
                displayName = "${KeyNoteClasses[blues.rootIndex]} blues",
                scale = ScaleProfile(
                    displaySuffix = " blues",
                    offsets = MinorBluesScaleOffsets,
                    chordQualities = listOf("7", "", "7", "dim", "7", ""),
                ),
                score = blues.score,
            )
        }
        ?: scalarGuess
    val scalarConfidence = if (bestGuess != null && guess != null) {
        (guess.score / bestGuess.score.coerceAtLeast(0.01f)).coerceIn(0f, 1.5f)
    } else {
        0f
    }
    return KeyAnalysis(
        recentNotes = noteClasses.takeLast(8),
        guessedKey = guess?.displayName?.withoutVisibleBluesLabel(),
        likelyChords = emptyList(),
        chordTones = emptyList(),
        scalarConfidence = scalarConfidence,
        leadScaleKey = leadScaleGuess?.displayName?.withoutVisibleBluesLabel(),
    )
}

private fun scaleScore(
    counts: FloatArray,
    rootIndex: Int,
    scaleOffsets: List<Int>,
    tonalCenterIndex: Int,
): Float {
    var score = 0f
    val scaleNoteIndices = scaleOffsets.map { offset ->
        (rootIndex + offset).floorMod(KeyNoteClasses.size)
    }
    scaleOffsets.forEachIndexed { scaleDegree, offset ->
        val noteIndex = (rootIndex + offset).floorMod(KeyNoteClasses.size)
        val weight = when (scaleDegree) {
            0 -> 5f
            4 -> 3.5f
            2 -> if (offset == 3) 3.2f else 2.4f
            6 -> 1.8f
            else -> 1.2f
        }
        score += counts[noteIndex] * weight
    }
    counts.forEachIndexed { noteIndex, count ->
        if (noteIndex !in scaleNoteIndices) {
            score -= count * 1.4f
        }
    }
    if (tonalCenterIndex == rootIndex) {
        score += 1.8f
    }
    return score
}

private fun leadScaleGuess(
    counts: FloatArray,
    guesses: List<KeyGuess>,
    majorTriadEvidence: MajorTriadEvidence?,
): KeyGuess? {
    val total = counts.sum()
    if (total < 12f) return null
    val peak = counts.maxOrNull() ?: return null
    val presenceFloor = maxOf(0.8f, peak * 0.16f)
    return guesses
        .filter { it.scale.displaySuffix == "m pent" || it.scale.displaySuffix == "m blues" }
        .mapNotNull { guess ->
            val scaleNoteIndices = guess.scale.offsets.map { offset ->
                (guess.rootIndex + offset).floorMod(KeyNoteClasses.size)
            }.toSet()
            val scaleToneWeight = scaleNoteIndices.sumOf { counts[it].toDouble() }.toFloat()
            val presentScaleToneCount = scaleNoteIndices.count { counts[it] >= presenceFloor }
            val rootSignal = counts[guess.rootIndex]
            val minorThirdSignal = counts[(guess.rootIndex + 3).floorMod(KeyNoteClasses.size)]
            val flatSeventhSignal = counts[(guess.rootIndex + 10).floorMod(KeyNoteClasses.size)]
            val outOfScaleWeight = total - scaleToneWeight
            val coverage = scaleToneWeight / total.coerceAtLeast(0.01f)
            val tonicSignal = rootSignal + minorThirdSignal * 0.8f + flatSeventhSignal * 0.55f
            val hasMinorColor = minorThirdSignal >= presenceFloor || flatSeventhSignal >= presenceFloor
            if (presentScaleToneCount < 4 || !hasMinorColor) return@mapNotNull null
            if (coverage < 0.72f || tonicSignal < total * 0.12f) return@mapNotNull null
            if (majorTriadEvidence != null && majorTriadEvidence.score >= tonicSignal * 1.12f) {
                return@mapNotNull null
            }
            guess to (coverage * 8f + tonicSignal - outOfScaleWeight * 0.8f)
        }
        .maxByOrNull { it.second }
        ?.takeIf { it.second >= 6.5f }
        ?.first
}

private fun majorTriadEvidence(counts: FloatArray): MajorTriadEvidence? {
    val peak = counts.maxOrNull() ?: return null
    if (peak <= 0f) return null
    val presenceFloor = maxOf(0.8f, peak * 0.14f)

    return KeyNoteClasses.indices.asSequence()
        .mapNotNull { rootIndex ->
            val thirdIndex = (rootIndex + 4).floorMod(KeyNoteClasses.size)
            val fifthIndex = (rootIndex + 7).floorMod(KeyNoteClasses.size)
            val minorThirdIndex = (rootIndex + 3).floorMod(KeyNoteClasses.size)
            val rootSignal = counts[rootIndex]
            val thirdSignal = counts[thirdIndex]
            val fifthSignal = counts[fifthIndex]
            val minorThirdSignal = counts[minorThirdIndex]
            val relativeMinorRootSignal = counts[(rootIndex - 3).floorMod(KeyNoteClasses.size)]
            val relativeMinorSeventhSignal = counts[(rootIndex + 7).floorMod(KeyNoteClasses.size)]
            if (rootSignal < presenceFloor || thirdSignal < presenceFloor) {
                return@mapNotNull null
            }
            if (thirdSignal < minorThirdSignal * 1.18f) return@mapNotNull null
            if (
                relativeMinorRootSignal >= presenceFloor &&
                (fifthSignal < presenceFloor || relativeMinorSeventhSignal >= presenceFloor)
            ) {
                return@mapNotNull null
            }
            MajorTriadEvidence(
                rootIndex = rootIndex,
                score = rootSignal * 3.2f + thirdSignal * 2.8f + fifthSignal * 2.0f,
            )
        }
        .maxByOrNull { it.score }
}

private fun KeyGuess.likelyChordGuesses(counts: FloatArray): List<ChordGuess> {
    val peak = counts.maxOrNull() ?: 0f
    if (peak <= 0f) return emptyList()

    val preferredRoots = preferredChordRoots()
    val colorGuesses = KeyNoteClasses.indices.asSequence()
        .flatMap { chordRootIndex ->
            TunerChordTemplates.asSequence().mapNotNull { template ->
                template.scoreChord(
                    counts = counts,
                    peak = peak,
                    chordRootIndex = chordRootIndex,
                    rootWeight = chordRootWeight(chordRootIndex, preferredRoots),
                )
            }
        }

    val bluesGuesses = dominantBluesProgressionGuess(counts)?.chords.orEmpty()
        .asSequence()
        .map { chord -> chord.copy(score = chord.score * 1.35f) }

    return (bluesGuesses + colorGuesses)
        .sortedByDescending { it.score }
        .distinctBy { it.rootIndex }
        .take(3)
        .toList()
}

private fun ChordTemplate.scoreChord(
    counts: FloatArray,
    peak: Float,
    chordRootIndex: Int,
    rootWeight: Float,
): ChordGuess? {
    val presenceFloor = maxOf(0.65f, peak * 0.09f)
    val colorPresenceFloor = maxOf(0.8f, peak * 0.12f)
    val requiredPresent = requiredOffsets.all { offset ->
        counts[(chordRootIndex + offset).floorMod(KeyNoteClasses.size)] >= presenceFloor
    }
    if (!requiredPresent) return null

    val colorPresent = colorOffsets.count { offset ->
        counts[(chordRootIndex + offset).floorMod(KeyNoteClasses.size)] >= colorPresenceFloor
    }
    if (colorOffsets.isNotEmpty() && colorPresent == 0) return null

    val majorThirdSignal = counts[(chordRootIndex + 4).floorMod(KeyNoteClasses.size)]
    val minorThirdSignal = counts[(chordRootIndex + 3).floorMod(KeyNoteClasses.size)]
    val naturalFifthSignal = counts[(chordRootIndex + 7).floorMod(KeyNoteClasses.size)]
    val diminishedFifthSignal = counts[(chordRootIndex + 6).floorMod(KeyNoteClasses.size)]
    val augmentedFifthSignal = counts[(chordRootIndex + 8).floorMod(KeyNoteClasses.size)]
    val suspendedSignal = maxOf(
        counts[(chordRootIndex + 2).floorMod(KeyNoteClasses.size)],
        counts[(chordRootIndex + 5).floorMod(KeyNoteClasses.size)],
    )

    when {
        suffix.isBlank() && majorThirdSignal < minorThirdSignal * 1.08f -> return null
        suffix == "m" && minorThirdSignal < majorThirdSignal * 1.08f -> return null
        suffix.contains("sus") && maxOf(majorThirdSignal, minorThirdSignal) >= suspendedSignal * 0.9f -> return null
        suffix.startsWith("aug") && naturalFifthSignal >= augmentedFifthSignal * 0.72f -> return null
        (suffix.startsWith("dim") || suffix == "m7b5") && naturalFifthSignal >= diminishedFifthSignal * 0.72f -> return null
    }

    var score = 0f
    offsets.forEachIndexed { index, offset ->
        val noteIndex = (chordRootIndex + offset).floorMod(KeyNoteClasses.size)
        score += counts[noteIndex] * weights[index]
    }
    score *= rootWeight
    if (suffix == "7" && rootWeight > 1f) {
        score *= 1.16f
    }
    if (suffix == "aug7" || suffix == "aug") {
        score *= 0.96f
    }
    if (suffix.contains('#') || suffix.contains('b') || suffix == "9" || suffix == "m9" || suffix.contains("sus")) {
        score *= if (suffix.contains("sus")) 0.96f else 1.04f
    }

    val tones = offsets.map { offset ->
        KeyNoteClasses[(chordRootIndex + offset).floorMod(KeyNoteClasses.size)]
    }
    return ChordGuess(
        label = "${KeyNoteClasses[chordRootIndex]}$suffix",
        tones = tones,
        score = score,
        rootIndex = chordRootIndex,
        suffix = suffix,
    )
}

private fun ChordGuess.displayLabel(useFlatNames: Boolean): String {
    return "${rootIndex.noteName(useFlatNames)}$suffix"
}

private fun ChordGuess.displayTones(useFlatNames: Boolean): List<String> {
    return tones.map { tone ->
        val toneIndex = KeyNoteClasses.indexOf(tone)
        if (toneIndex >= 0) toneIndex.noteName(useFlatNames) else tone
    }
}

private fun Int.noteName(useFlatNames: Boolean): String {
    return if (useFlatNames) FlatKeyNoteClasses[this] else KeyNoteClasses[this]
}

private fun String?.shouldUseFlatChordNames(): Boolean {
    val key = this ?: return false
    return key.contains("m") ||
        key.contains("blues") ||
        key.startsWith("F") ||
        key.startsWith("Bb") ||
        key.startsWith("Eb") ||
        key.startsWith("Ab") ||
        key.startsWith("Db")
}

private fun String.withoutVisibleBluesLabel(): String {
    return replace("m blues", "m").replace("m pent", "m").replace(" blues", "")
}

private fun KeyGuess.preferredChordRoots(): Set<Int> {
    val scaleRoots = scale.offsets.map { offset ->
        (rootIndex + offset).floorMod(KeyNoteClasses.size)
    }
    val bluesDominantRoots = if (scale.prefersMinorTonality) {
        listOf(0, 5, 7).map { offset -> (rootIndex + offset).floorMod(KeyNoteClasses.size) }
    } else {
        emptyList()
    }
    return (scaleRoots + bluesDominantRoots).toSet()
}

private fun KeyGuess.chordRootWeight(chordRootIndex: Int, preferredRoots: Set<Int>): Float {
    return when {
        chordRootIndex == rootIndex -> 1.24f
        chordRootIndex in preferredRoots -> 1.12f
        else -> 0.92f
    }
}

private fun dominantBluesProgressionGuess(counts: FloatArray): BluesProgressionGuess? {
    val peak = counts.maxOrNull() ?: 0f
    if (peak <= 0f) return null
    val dominantTemplate = TunerChordTemplates.firstOrNull { it.suffix == "7" } ?: return null

    return KeyNoteClasses.indices.asSequence()
        .mapNotNull { rootIndex ->
            val progressionRoots = listOf(
                rootIndex,
                (rootIndex + 5).floorMod(KeyNoteClasses.size),
                (rootIndex + 7).floorMod(KeyNoteClasses.size),
            )
            val chords = progressionRoots.mapNotNull { chordRoot ->
                dominantTemplate.scoreChord(
                    counts = counts,
                    peak = peak,
                    chordRootIndex = chordRoot,
                    rootWeight = if (chordRoot == rootIndex) 1.24f else 1.12f,
                )
            }
            if (chords.size < 2) return@mapNotNull null

            val score = chords.sumOf { it.score.toDouble() }.toFloat() +
                rootChordSignal(counts, rootIndex) * 0.65f
            if (score < peak * 8.5f) return@mapNotNull null

            BluesProgressionGuess(
                rootIndex = rootIndex,
                score = score,
                chords = chords.sortedByDescending { it.score },
            )
        }
        .maxByOrNull { it.score }
}

private fun rootChordSignal(counts: FloatArray, rootIndex: Int): Float {
    val minorThirdIndex = (rootIndex + 3).floorMod(KeyNoteClasses.size)
    val majorThirdIndex = (rootIndex + 4).floorMod(KeyNoteClasses.size)
    val fifthIndex = (rootIndex + 7).floorMod(KeyNoteClasses.size)
    return counts[rootIndex] * 3.2f +
        maxOf(counts[minorThirdIndex], counts[majorThirdIndex]) * 2f +
        counts[fifthIndex] * 2f
}

private fun tonicMinorSeventhSignal(counts: FloatArray, rootIndex: Int): Float {
    val minorThirdIndex = (rootIndex + 3).floorMod(KeyNoteClasses.size)
    val fifthIndex = (rootIndex + 7).floorMod(KeyNoteClasses.size)
    val minorSeventhIndex = (rootIndex + 10).floorMod(KeyNoteClasses.size)
    return counts[rootIndex] * 3.2f +
        counts[minorThirdIndex] * 2.4f +
        counts[fifthIndex] * 1.4f +
        counts[minorSeventhIndex] * 2.2f
}

private fun Int.floorMod(divisor: Int): Int {
    return ((this % divisor) + divisor) % divisor
}
