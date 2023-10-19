package com.decosegfault.atlas.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.math.Vector3
import com.decosegfault.atlas.screens.SimulationScreen
import com.decosegfault.atlas.util.AbstractGarbageCollectedCache
import ktx.assets.disposeSafely
import org.tinylog.kotlin.Logger
import java.util.concurrent.CompletableFuture

/** **Soft** limit of tiles in VRAM, size will be approx 100 KiB * this */
private const val MAX_TILES_RAM = 4096.0

/** If the cache is this% full, start trying to evict items */
private const val START_GC_THRESHOLD = 0.90

/** If the cache is below this% full, stop evicting */
private const val END_GC_THRESHOLD = 0.50

/**
 * Implementation of [AbstractGarbageCollectedCache] used to store OpenStreetMap tiles
 *
 * @author Matt Young
 */
object GCTileCache : AbstractGarbageCollectedCache<Vector3, Texture>(
    "GCTileCache",
    MAX_TILES_RAM,
    START_GC_THRESHOLD,
    END_GC_THRESHOLD,
    Runtime.getRuntime().availableProcessors()
) {

    private lateinit var defaultTexture: Texture

    fun init() {
        Logger.info("GCTileCache using ${Runtime.getRuntime().availableProcessors()} threads and $MAX_TILES_RAM tiles max")
        defaultTexture = Texture(Gdx.files.internal("sprite/notileserver.png"))
    }

    override fun newItem(key: Vector3): Texture {
        // download the tile async on the executor thread
        val pixmap = TileServerManager.fetchTileAsPixmap(key) ?: run {
            // failed to download texture
            return defaultTexture
        }
        // now that we have the pixmap, we need to context switch into the render thread in order to
        // upload the texture
        // transfer our work to the simulation screen's concurrent work queue
        val future = CompletableFuture<Texture>()
        val runnable = Runnable {
            val tex = Texture(pixmap)
            tex.setFilter(TextureFilter.Linear, TextureFilter.Linear)
            future.complete(tex)
        }
        SimulationScreen.addWork(runnable)

        // now wait for the future to get back to us
        val texture = future.get()
        pixmap.dispose()
        return texture
    }

    override fun dispose() {
        Logger.debug("Shutdown GCTileCache")
        defaultTexture.disposeSafely()
        super.dispose()
    }
}
