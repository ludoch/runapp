package com.example.runapp

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.ResourceBuilders
import java.util.concurrent.CompletableFuture
import com.google.common.util.concurrent.ListenableFuture

class RaceTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val lastRace = getSharedPreferences("race_prefs", Context.MODE_PRIVATE).getString("last_race_summary", "No races yet")

        val layout = LayoutElementBuilders.Layout.Builder().setRoot(
            LayoutElementBuilders.Text.Builder()
                .setText("RunApp: ${lastRace ?: "Ready?"}")
                .build()
        ).build()

        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTimeline(
                TimelineBuilders.Timeline.Builder().addTimelineEntry(
                    TimelineBuilders.TimelineEntry.Builder().setLayout(layout).build()
                ).build()
            ).build()

        // Using a manual implementation of ListenableFuture to avoid Guava dependency if missing
        return object : ListenableFuture<TileBuilders.Tile>, java.util.concurrent.Future<TileBuilders.Tile> by CompletableFuture.completedFuture(tile) {
            override fun addListener(listener: Runnable, executor: java.util.concurrent.Executor) {
                listener.run()
            }
        }
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder().setVersion("1").build()
        return object : ListenableFuture<ResourceBuilders.Resources>, java.util.concurrent.Future<ResourceBuilders.Resources> by CompletableFuture.completedFuture(resources) {
            override fun addListener(listener: Runnable, executor: java.util.concurrent.Executor) {
                listener.run()
            }
        }
    }
}
