package com.example.bpmmunkzpulse.presentation

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

private const val TILE_RESOURCES_VERSION = "1"

class BpmMunkzTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> =
        Futures.immediateFuture(
            Tile.Builder()
                .setResourcesVersion(TILE_RESOURCES_VERSION)
                .setTileTimeline(
                    Timeline.fromLayoutElement(
                        materialScope(this, requestParams.deviceConfiguration) {
                            primaryLayout(
                                mainSlot = {
                                    text(
                                        text = "BPM Munkz Pulse".layoutString,
                                        typography = Typography.DISPLAY_MEDIUM,
                                    )
                                },
                                bottomSlot = {
                                    textEdgeButton(
                                        onClick = clickable(
                                            action = launchAction(
                                                ComponentName(
                                                    packageName,
                                                    MainActivity::class.java.name,
                                                ),
                                            ),
                                        ),
                                    ) {
                                        text("Open".layoutString)
                                    }
                                },
                            )
                        },
                    ),
                )
                .build(),
        )

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<Resources> =
        Futures.immediateFuture(
            Resources.Builder()
                .setVersion(TILE_RESOURCES_VERSION)
                .build(),
        )
}
