package bpm.munkz.pulse_wear.os.bpm.presentation

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.CONTENT_SCALE_MODE_FIT
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FONT_WEIGHT_MEDIUM
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.TEXT_ALIGN_CENTER
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.LayoutElementBuilders.VERTICAL_ALIGN_CENTER
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.expression.ProtoLayoutExperimental
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import bpm.munkz.pulse_wear.os.bpm.R
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

private const val TILE_LOGO_RESOURCE_ID = "bpm_munkz_tile_logo_v2"

@ProtoLayoutExperimental
class BpmMunkzTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        val openApp = clickable(
            action = launchAction(
                ComponentName(
                    packageName,
                    MainActivity::class.java.name,
                ),
            ),
        )
        val openLabel = getString(R.string.tile_open)

        return immediateFuture(
            Tile.Builder()
                .setTileTimeline(
                    Timeline.fromLayoutElement(
                        Box.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                            .setModifiers(
                                Modifiers.Builder()
                                    .setClickable(openApp)
                                    .setBackground(
                                        Background.Builder()
                                            .setColor(argb(0xFF000000.toInt()))
                                            .build(),
                                    )
                                    .build(),
                            )
                            .addContent(
                                Column.Builder()
                                    .setWidth(wrap())
                                    .setHeight(wrap())
                                    .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                                    .addContent(
                                        Image.Builder(requestParams.scope)
                                            .setImageResource(tileLogoResource(packageName), TILE_LOGO_RESOURCE_ID)
                                            .setWidth(dp(158f))
                                            .setHeight(dp(158f))
                                            .setContentScaleMode(CONTENT_SCALE_MODE_FIT)
                                            .build(),
                                    )
                                    .addContent(
                                        Spacer.Builder()
                                            .setHeight(dp(10f))
                                            .build(),
                                    )
                                    .addContent(
                                        Box.Builder()
                                            .setWidth(dp(118f))
                                            .setHeight(dp(42f))
                                            .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
                                            .setVerticalAlignment(VERTICAL_ALIGN_CENTER)
                                            .setModifiers(
                                                Modifiers.Builder()
                                                    .setClickable(openApp)
                                                    .setBackground(
                                                        Background.Builder()
                                                            .setColor(argb(0xFFE9DDFF.toInt()))
                                                            .setCorner(
                                                                Corner.Builder()
                                                                    .setRadius(dp(24f))
                                                                    .build(),
                                                            )
                                                            .build(),
                                                    )
                                                    .build(),
                                            )
                                            .addContent(
                                                Text.Builder()
                                                    .setText(openLabel)
                                                    .setMaxLines(1)
                                                    .setMultilineAlignment(TEXT_ALIGN_CENTER)
                                                    .setFontStyle(
                                                        FontStyle.Builder()
                                                            .setColor(argb(0xFF211934.toInt()))
                                                            .setSize(sp(20f))
                                                            .setWeight(FONT_WEIGHT_MEDIUM)
                                                            .build(),
                                                    )
                                                    .build(),
                                            )
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                    ),
                )
                .build(),
        )
    }
}

private fun tileLogoResource(packageName: String): ImageResource =
    ImageResource.Builder()
        .setAndroidResourceByResId(
            AndroidImageResourceByResId.Builder()
                .setResourceId(tileLogoResIdForPackage(packageName))
                .build(),
        )
        .build()

private fun tileLogoResIdForPackage(packageName: String): Int {
    return when {
        packageName.endsWith(".bpm") ||
            packageName.endsWith(".tune") ||
            packageName.endsWith(".rhythm") ||
            packageName.endsWith(".playlist") ||
            packageName.endsWith(".pro") -> R.drawable.bpm_munkz_app_logo_free
        else -> R.drawable.bpm_munkz_app_logo_edition
    }
}

private fun <T> immediateFuture(value: T): ListenableFuture<T> = ImmediateListenableFuture(value)

private class ImmediateListenableFuture<T>(
    private val value: T,
) : ListenableFuture<T> {
    override fun addListener(listener: Runnable, executor: Executor) {
        executor.execute(listener)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

    override fun isCancelled(): Boolean = false

    override fun isDone(): Boolean = true

    override fun get(): T = value

    override fun get(timeout: Long, unit: TimeUnit): T = value
}
