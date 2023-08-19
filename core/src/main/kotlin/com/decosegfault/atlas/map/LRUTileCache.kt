package com.decosegfault.atlas.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector3
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import ktx.assets.disposeSafely
import org.tinylog.kotlin.Logger
import kotlin.math.roundToInt

/**
 * An LRU cache for tiles from the OSM tile server, implemented using the Caffeine library. Caches up
 * to [MAX_TILES_RAM] tiles. This is done to balance performance, latency and VRAM usage.
 *
 * @author Matt Young
 */
class LRUTileCache {
    /**
     * Mapping between (x, y, zoom) and the tile texture.
     *
     * **Implementation details:**
     *
     * Instantiating a [Texture] in LibGDX implies uploading the texture to the GPU, which can only be done
     * on the OpenGL render thread. Normally, we'd use `Gdx.app.postRunnable`, but this only lets us schedule
     * some code to run - we can't get a return value out of it. What we _want_ to do is download the texture
     * as a CPU [Pixmap] in an async Executor from the tile server, then post a small runnable with
     * Gdx.app.postRunnable that allows us to upload the [Pixmap] into a [Texture].
     *
     * Unfortunately, due to the way [Caffeine] works, we can't do this - buildAsync requires us to return a
     * value _right now_, not just say "Ok, sure, we'll add it to the cache later for you." Because of this,
     * I've decided to implement tileCache as a [Cache], not an [AsyncLoadingCache], and do the async threading
     * stuff myself instead.
     */
    private val tileCache: Cache<Vector3?, Texture?> = Caffeine.newBuilder()
        .maximumSize(MAX_TILES_RAM)
        .removalListener { key: Vector3?, value: Texture?, cause ->
            Logger.debug("Tile $key being removed because of $cause (evicted ${cause.wasEvicted()})")
            Gdx.app.postRunnable { value.disposeSafely() }
        }
        .recordStats()
        .build()
//        .buildAsync { pos: Vector3? ->
//            if (pos == null) return@buildAsync null
//            val pixmap = TileServerManager.fetchTileAsPixmap(pos)
//            Gdx.app.postRunnable {
//                val texture = Texture(pixmap)
//                return@buildAsync texture
//            }
//        }

    /** @return cache hit rate stats for displaying */
    fun getStats(): String {
        val cache = tileCache
        return "Tile LRU    hit: ${(cache.stats().hitRate() * 100.0).roundToInt()}%    " +
         "size: ${cache.estimatedSize()}     evictions: ${cache.stats().evictionCount()}    " +
          "fetch: ${cache.stats().averageLoadPenalty() / 1e6} ms"
    }

    companion object {
        /** Maximum number of tiles in VRAM, size will be approx 100 KiB * this */
        private const val MAX_TILES_RAM = 1024L
    }
}
