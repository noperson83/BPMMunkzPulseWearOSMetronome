package bpm.munkz.pulse_wear.os.bpm.presentation

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

private const val PLAYLIST_PREFS = "bpm_munkz_playlists"
private const val PLAYLIST_LIBRARY_KEY = "playlist_library"

internal val SongNameOptions = listOf(
    "Intro",
    "Verse",
    "Chorus",
    "Bridge",
    "Solo",
    "Break",
    "Outro",
)

internal val SongNoteOptions = listOf(
    "Count in",
    "Hold tempo",
    "Big accents",
    "Keep pocket",
    "Breakdown",
    "Loop twice",
    "End tight",
)

internal data class PlaylistSong(
    val name: String,
    val bpm: Int,
    val beatsPerMeasure: Int,
    val accentBeat: Int,
    val subdivisionCount: Int,
    val beatAccentTypes: List<BeatAccentType>,
    val accentIntensityMode: AccentIntensityMode,
    val musicalKey: String,
    val note: String,
    val beatMachineSequenceMasks: List<Int>? = null,
)

internal data class SavedPlaylist(
    val name: String,
    val songs: List<PlaylistSong>,
)

internal fun defaultSavedPlaylists(): List<SavedPlaylist> {
    return listOf(
        SavedPlaylist(
            name = "Set 1",
            songs = listOf(
                PlaylistSong(
                    name = "Intro",
                    bpm = 64,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "C",
                    note = "Count in",
                ),
                PlaylistSong(
                    name = "Verse",
                    bpm = 92,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "G",
                    note = "Keep pocket",
                ),
                PlaylistSong(
                    name = "Chorus",
                    bpm = 116,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "D",
                    note = "Big accents",
                ),
            ),
        ),
        SavedPlaylist(
            name = "Practice",
            songs = listOf(
                PlaylistSong(
                    name = "Warmup",
                    bpm = 72,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "Am",
                    note = "Hold tempo",
                ),
                PlaylistSong(
                    name = "Break",
                    bpm = 108,
                    beatsPerMeasure = 4,
                    accentBeat = 1,
                    subdivisionCount = 1,
                    beatAccentTypes = defaultBeatAccentTypes(4, 1),
                    accentIntensityMode = AccentIntensityMode.Big,
                    musicalKey = "Em",
                    note = "Loop twice",
                ),
            ),
        ),
    )
}

internal fun defaultSavedPlaylist(number: Int): SavedPlaylist {
    return SavedPlaylist(
        name = "Set $number",
        songs = listOf(defaultPlaylistSong(1)),
    )
}

internal fun defaultPlaylistSong(number: Int): PlaylistSong {
    val name = SongNameOptions[(number - 1).wrap(SongNameOptions.size)]
    return PlaylistSong(
        name = name,
        bpm = 64,
        beatsPerMeasure = 4,
        accentBeat = 1,
        subdivisionCount = 1,
        beatAccentTypes = defaultBeatAccentTypes(4, 1),
        accentIntensityMode = AccentIntensityMode.Big,
        musicalKey = "C",
        note = "Count in",
    )
}

internal fun List<SavedPlaylist>.updatePlaylist(
    playlistIndex: Int,
    update: (SavedPlaylist) -> SavedPlaylist,
): List<SavedPlaylist> {
    return mapIndexed { index, playlist ->
        if (index == playlistIndex) update(playlist) else playlist
    }
}

internal fun List<SavedPlaylist>.updateSong(
    playlistIndex: Int,
    songIndex: Int,
    update: (PlaylistSong) -> PlaylistSong,
): List<SavedPlaylist> {
    return updatePlaylist(playlistIndex) { playlist ->
        playlist.copy(
            songs = playlist.songs.mapIndexed { index, song ->
                if (index == songIndex) update(song) else song
            },
        )
    }
}

internal fun Context.loadSavedPlaylists(): List<SavedPlaylist> {
    val rawLibrary = getSharedPreferences(PLAYLIST_PREFS, Context.MODE_PRIVATE)
        .getString(PLAYLIST_LIBRARY_KEY, null)
        ?: return defaultSavedPlaylists()

    return runCatching {
        val array = JSONArray(rawLibrary)
        buildList {
            for (playlistIndex in 0 until array.length()) {
                val playlistObject = array.optJSONObject(playlistIndex) ?: continue
                val songArray = playlistObject.optJSONArray("songs") ?: JSONArray()
                val songs = buildList {
                    for (songIndex in 0 until songArray.length()) {
                        val songObject = songArray.optJSONObject(songIndex) ?: continue
                        val loadedBeatsPerMeasure = songObject.optInt("beatsPerMeasure", 4).coerceIn(2, 16)
                        val loadedAccentBeat = songObject.optInt("accentBeat", 1)
                            .coerceIn(1, loadedBeatsPerMeasure)
                        add(
                            PlaylistSong(
                                name = songObject.optString("name", "Song ${songIndex + 1}"),
                                bpm = songObject.optInt("bpm", 64).coerceIn(MIN_BPM, MAX_BPM),
                                beatsPerMeasure = loadedBeatsPerMeasure,
                                accentBeat = loadedAccentBeat,
                                subdivisionCount = songObject.optInt("subdivisionCount", 1)
                                    .toSupportedPlaylistSubdivisionCount(),
                                beatAccentTypes = songObject.optJSONArray("beatAccentTypes")
                                    .toBeatAccentTypes(loadedBeatsPerMeasure, loadedAccentBeat),
                                accentIntensityMode = AccentIntensityMode.fromPersistedValue(
                                    songObject.optInt(
                                        "accentIntensityMode",
                                        AccentIntensityMode.Big.persistedValue,
                                    ),
                                ),
                                musicalKey = songObject.optString("musicalKey", "C"),
                                note = songObject.optString("note", "Count in"),
                                beatMachineSequenceMasks = songObject.optJSONArray("beatMachineSequenceMasks")
                                    .toBeatMachineSequenceMasks(),
                            ),
                        )
                    }
                }

                add(
                    SavedPlaylist(
                        name = playlistObject.optString("name", "Set ${playlistIndex + 1}"),
                        songs = songs.ifEmpty { listOf(defaultPlaylistSong(1)) },
                    ),
                )
            }
        }.ifEmpty { defaultSavedPlaylists() }
    }.getOrElse {
        defaultSavedPlaylists()
    }
}

internal fun Context.saveSavedPlaylists(playlists: List<SavedPlaylist>) {
    val playlistArray = JSONArray()

    playlists.forEach { playlist ->
        val songArray = JSONArray()
        playlist.songs.forEach { song ->
            songArray.put(
                JSONObject()
                    .put("name", song.name)
                    .put("bpm", song.bpm)
                    .put("beatsPerMeasure", song.beatsPerMeasure)
                    .put("accentBeat", song.accentBeat)
                    .put("subdivisionCount", song.subdivisionCount)
                    .put("beatAccentTypes", song.beatAccentTypes.toJsonArray())
                    .put("accentIntensityMode", song.accentIntensityMode.persistedValue)
                    .put("musicalKey", song.musicalKey)
                    .put("note", song.note)
                    .also { songObject ->
                        song.beatMachineSequenceMasks?.let { masks ->
                            songObject.put("beatMachineSequenceMasks", masks.toBeatMachineJsonArray())
                        }
                    },
            )
        }

        playlistArray.put(
            JSONObject()
                .put("name", playlist.name)
                .put("songs", songArray),
        )
    }

    getSharedPreferences(PLAYLIST_PREFS, Context.MODE_PRIVATE)
        .edit {
            putString(PLAYLIST_LIBRARY_KEY, playlistArray.toString())
        }
}

private fun JSONArray?.toBeatMachineSequenceMasks(): List<Int>? {
    if (this == null || length() == 0) return null
    return List(8) { index ->
        optInt(index, 0).coerceIn(0, 0xFFFF)
    }
}

private fun List<Int>.toBeatMachineJsonArray(): JSONArray {
    return JSONArray().also { array ->
        take(8).forEach { mask ->
            array.put(mask.coerceIn(0, 0xFFFF))
        }
    }
}

private fun JSONArray?.toBeatAccentTypes(
    beatsPerMeasure: Int,
    accentBeat: Int,
): List<BeatAccentType> {
    val defaults = defaultBeatAccentTypes(beatsPerMeasure, accentBeat)
    if (this == null || length() == 0) return defaults

    return List(beatsPerMeasure.coerceIn(2, 16)) { index ->
        if (index < length()) {
            BeatAccentType.fromPersistedValue(optInt(index, BeatAccentType.Silent.persistedValue))
        } else {
            defaults[index]
        }
    }
}

private fun List<BeatAccentType>.toJsonArray(): JSONArray {
    return JSONArray().also { array ->
        forEach { accentType ->
            array.put(accentType.persistedValue)
        }
    }
}

private fun Int.toSupportedPlaylistSubdivisionCount(): Int {
    return when {
        this <= 1 -> 1
        this == 2 -> 2
        this == 3 -> 3
        this == 4 -> 4
        else -> 6
    }
}
