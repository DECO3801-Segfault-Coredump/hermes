package com.decosegfault.atlas.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import ktx.assets.disposeSafely
import org.tinylog.kotlin.Logger
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * An LRU cache for tiles from the OSM tile server, implemented using the Caffeine library. Caches up
 * to [MAX_TILES_RAM] tiles. This is done to balance performance, latency and VRAM usage.
 *
 * @author Matt Young
 */
object LRUTileCache : Disposable {
    /** Maximum number of tiles in VRAM, size will be approx 100 KiB * this */
    private const val MAX_TILES_RAM = 2048L

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
            val tileStr = "(${key?.x?.toInt()},${key?.y?.toInt()},${key?.z?.toInt()})"
            Logger.debug("Tile $tileStr being removed by $cause (evicted ${cause.wasEvicted()})")
            Gdx.app.postRunnable { value.disposeSafely() }
        }
        .recordStats()
        .build()

    /** Limit thread pool size to 2x the number of processors to prevent memory issues */
    private val threadPoolSize = Runtime.getRuntime().availableProcessors() * 2

    /** Executor used for HTTP requests */
    private val executor = ThreadPoolExecutor(
        0, threadPoolSize,
        60L, TimeUnit.SECONDS,
        SynchronousQueue(),
    )

    init {
        Logger.info("LRUTileCache using $threadPoolSize threads and $MAX_TILES_RAM tiles max")
    }

    /**
     * Asynchronously retrieves a tile from the tile server. If the tile isn't in the cache, it will be
     * downloaded asynchronously. [onRetrieved] is invoked after the tile is made available.
     * @return tile texture if it was possible to load, otherwise null
     */
    fun retrieve(pos: Vector3, onRetrieved: (Texture) -> Unit) {
        val maybeTexture = tileCache.getIfPresent(pos)
        if (maybeTexture != null) {
            // tile was already in cache
            onRetrieved(maybeTexture)
            return
        }
        executor.submit {
            // download the tile async on the executor thread
            val pixmap = TileServerManager.fetchTileAsPixmap(pos) ?: return@submit
            // now that we have the pixmap, we need to context switch into the render thread in order to
            // upload the texture
            Gdx.app.postRunnable {
                val texture = Texture(pixmap)
                pixmap.dispose()
                tileCache.put(pos, texture)
                onRetrieved(texture)
            }
        }
    }

    fun purge() {
        Logger.debug("Purging LRUTileCache")
        tileCache.cleanUp()
        tileCache.invalidateAll()
        tileCache.cleanUp()
    }

    /** @return cache hit rate stats for displaying */
    fun getStats(): String {
        val cache = tileCache
        return "Tile LRU    hit: ${(cache.stats().hitRate() * 100.0).roundToInt()}%    " +
         "size: ${cache.estimatedSize()}     evictions: ${cache.stats().evictionCount()}    " +
          "fetch: ${cache.stats().averageLoadPenalty() / 1e6} ms"
    }

    override fun dispose() {
        Logger.debug("Shutdown LRUTileCache")
        purge()
        executor.shutdownNow()
    }
}
