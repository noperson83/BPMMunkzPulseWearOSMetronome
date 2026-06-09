package bpm.munkz.pulse_wear.os.bpm.presentation

private val KeyNoteClasses = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private val MajorScaleOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
private val MinorScaleOffsets = listOf(0, 2, 3, 5, 7, 8, 10)
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
    val likelyChords: List<String>,
    val chordTones: List<String>,
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
)

private data class ChordGuess(
    val label: String,
    val tones: List<String>,
    val score: Float,
)

fun String.toNoteClass(): String? {
    val noteClass = takeWhile { it.isLetter() || it == '#' }
    return noteClass.takeIf { it in KeyNoteClasses }
}

fun analyzeMusicalKey(noteClasses: List<String>): KeyAnalysis {
    if (noteClasses.size < 6) {
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

    var bestGuess: KeyGuess? = null
    KeyNoteClasses.forEachIndexed { rootIndex, rootName ->
        TunerScaleProfiles.forEach { scale ->
            val score = scaleScore(
                counts = weightedCounts,
                rootIndex = rootIndex,
                scaleOffsets = scale.offsets,
                tonalCenterIndex = noteClasses.lastOrNull()?.let { KeyNoteClasses.indexOf(it) } ?: -1,
            )
            if (bestGuess == null || score > bestGuess!!.score) {
                bestGuess = KeyGuess(
                    rootIndex = rootIndex,
                    displayName = "$rootName${scale.displaySuffix}",
                    scale = scale,
                    score = score,
                )
            }
        }
    }

    val guess = bestGuess
    val chordGuesses = guess?.likelyChordGuesses(weightedCounts).orEmpty()
    return KeyAnalysis(
        recentNotes = noteClasses.takeLast(8),
        guessedKey = guess?.displayName,
        likelyChords = chordGuesses.map { it.label },
        chordTones = chordGuesses.firstOrNull()?.tones.orEmpty(),
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
            2 -> 2.4f
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
        score += 4f
    }
    return score
}

private fun KeyGuess.likelyChordGuesses(counts: FloatArray): List<ChordGuess> {
    return scale.offsets.asSequence()
        .mapIndexed { degreeIndex, offset ->
            val chordRootIndex = (rootIndex + offset).floorMod(KeyNoteClasses.size)
            val thirdIndex = (chordRootIndex + if (scale.chordQualities[degreeIndex] == "") 4 else 3)
                .floorMod(KeyNoteClasses.size)
            val fifthIndex = (chordRootIndex + if (scale.chordQualities[degreeIndex] == "dim") 6 else 7)
                .floorMod(KeyNoteClasses.size)
            val rootWeight = if (degreeIndex == 0) 1.5f else 1f
            val score = (counts[chordRootIndex] * 3.2f * rootWeight) +
                (counts[thirdIndex] * 2f) +
                (counts[fifthIndex] * 2f)
            ChordGuess(
                label = "${KeyNoteClasses[chordRootIndex]}${scale.chordQualities[degreeIndex]}",
                tones = listOf(
                    KeyNoteClasses[chordRootIndex],
                    KeyNoteClasses[thirdIndex],
                    KeyNoteClasses[fifthIndex],
                ),
                score = score,
            )
        }
        .sortedByDescending { it.score }
        .distinctBy { it.label }
        .take(3)
        .toList()
}

private fun Int.floorMod(divisor: Int): Int {
    return ((this % divisor) + divisor) % divisor
}
