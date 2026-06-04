package bpm.munkz.pulse_wear.os.bpm.presentation

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import androidx.core.content.edit

private const val LATEST_MUSIC_PREFS = "bpm_munkz_latest_music"
private const val LATEST_BPM_KEY = "latest_bpm"
private const val LATEST_KEY_KEY = "latest_key"
private const val DEFAULT_LATEST_BPM = 0
private const val DEFAULT_LATEST_KEY = "--"

fun Context.saveLatestMusicReading(bpm: Int, musicalKey: String) {
    getSharedPreferences(LATEST_MUSIC_PREFS, Context.MODE_PRIVATE)
        .edit {
            putInt(LATEST_BPM_KEY, bpm.coerceIn(MIN_BPM, MAX_BPM))
            putString(LATEST_KEY_KEY, musicalKey.trim().ifBlank { DEFAULT_LATEST_KEY })
        }
    requestLatestMusicComplicationUpdates()
}

private fun Context.requestLatestMusicComplicationUpdates() {
    listOf(
        LatestBpmComplicationDataSourceService::class.java,
        LatestKeyComplicationDataSourceService::class.java,
    ).forEach { serviceClass ->
        ComplicationDataSourceUpdateRequester.create(
            context = this,
            complicationDataSourceComponent = ComponentName(this, serviceClass),
        ).requestUpdateAll()
    }
}

abstract class LatestMusicComplicationDataSourceService(
    private val title: String,
    private val textProvider: Context.() -> String,
) : SuspendingComplicationDataSourceService() {
    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return buildShortText(title = title, text = previewText)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        return buildShortText(title = title, text = textProvider())
    }

    protected abstract val previewText: String

    private fun buildShortText(title: String, text: String): ComplicationData {
        val complicationText = PlainComplicationText.Builder(text).build()
        val complicationTitle = PlainComplicationText.Builder(title).build()
        return ShortTextComplicationData.Builder(
            text = complicationText,
            contentDescription = PlainComplicationText.Builder("$title $text").build(),
        )
            .setTitle(complicationTitle)
            .setTapAction(openAppPendingIntent())
            .build()
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(ACTION_OPEN_SPECTRUM)
            .putExtra(EXTRA_OPEN_SPECTRUM, true)
        return PendingIntent.getActivity(
            this,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class LatestBpmComplicationDataSourceService : LatestMusicComplicationDataSourceService(
    title = "BPM",
    textProvider = {
        val bpm = getSharedPreferences(LATEST_MUSIC_PREFS, Context.MODE_PRIVATE)
            .getInt(LATEST_BPM_KEY, DEFAULT_LATEST_BPM)
        if (bpm > 0) {
            bpm.toString()
        } else {
            DEFAULT_LATEST_KEY
        }
    },
) {
    override val previewText: String = "120"
}

class LatestKeyComplicationDataSourceService : LatestMusicComplicationDataSourceService(
    title = "Key",
    textProvider = {
        getSharedPreferences(LATEST_MUSIC_PREFS, Context.MODE_PRIVATE)
            .getString(LATEST_KEY_KEY, DEFAULT_LATEST_KEY)
            ?.substringBefore(' ')
            .orEmpty()
            .ifBlank { DEFAULT_LATEST_KEY }
    },
) {
    override val previewText: String = "C"
}
