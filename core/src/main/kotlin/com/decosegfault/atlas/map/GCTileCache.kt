package com.decosegfault.atlas.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.WindowedMean
import com.badlogic.gdx.utils.Disposable
import ktx.assets.disposeSafely
import org.tinylog.kotlin.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * A garbage-collected cache for tiles from the OSM tile server. The cache will start garbage collecting
 * unused tiles (tiles not currently on screen) once it reaches [START_GC_THRESHOLD] out of [MAX_TILES_RAM].
 * It will try and remove enough tiles to reach the end threshold [END_GC_THRESHOLD]. Note that [MAX_TILES_RAM]
 * if not a hard limit, and the cache can _theoretically_ go beyond this bound in extreme situations.
 *
 * @author Matt Young
 */
object GCTileCache : Disposable {
    /** **Soft** limit of tiles in VRAM, size will be approx 100 KiB * this */
    private const val MAX_TILES_RAM = 4096.0

    /** If the cache is this% full, start trying to evict items */
    private const val START_GC_THRESHOLD = 0.90

    /** If the cache is below this% full, stop evicting */
    private const val END_GC_THRESHOLD = 0.50

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
     */
    private val tileCache = ConcurrentHashMap<Vector3, Texture>()

    private val tilesInUse = mutableSetOf<Vector3>()

    /** Limit thread pool size to 2x the number of processors to prevent memory issues */
    private val threadPoolSize = Runtime.getRuntime().availableProcessors() * 2

    /** Executor used for HTTP requests */
    private val executor = Executors.newFixedThreadPool(threadPoolSize)

    private val fetchTimes = WindowedMean(50)

    private lateinit var defaultTexture: Texture

    private var gcs = 0

    private var hits = 0

    private var misses = 0

    fun init() {
        Logger.info("GCTileCache using $threadPoolSize threads and $MAX_TILES_RAM tiles max")
        defaultTexture = Texture(Gdx.files.internal("sprite/notileserver.png"))
    }

    /**
     * Asynchronously retrieves a tile from the tile server. If the tile isn't in the cache, it will be
     * downloaded asynchronously. [onRetrieved] is invoked after the tile is made available.
     * @return tile texture if it was possible to load, otherwise null
     */
    fun retrieve(pos: Vector3, onRetrieved: (Texture) -> Unit) {
        val begin = System.nanoTime()
        garbageCollect()
        val maybeTexture = tileCache[pos]
        if (maybeTexture != null) {
            // tile was already in cache
            onRetrieved(maybeTexture)
            hits++
            return
        }
        executor.submit {
            // download the tile async on the executor thread
            val pixmap = TileServerManager.fetchTileAsPixmap(pos) ?: run {
                // failed to download texture
                onRetrieved(defaultTexture)
                return@submit
            }
            // now that we have the pixmap, we need to context switch into the render thread in order to
            // upload the texture
            // TODO spread this work over multiple frames
            Gdx.app.postRunnable {
                val texture = Texture(pixmap)
                texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
                pixmap.dispose()
                tileCache[pos] = texture
                val end = (System.nanoTime() - begin) / 1e6f
                fetchTimes.addValue(end)

                onRetrieved(texture)
            }
        }
        misses++
    }

    /** GC unused tiles */
    private fun garbageCollect() {
        var fillRate = tileCache.size / MAX_TILES_RAM

        if (fillRate >= START_GC_THRESHOLD) {
            Logger.info("Garbage collecting, fill rate: ${fillRate * 100.0}%, used tiles: ${tilesInUse.size}")
            var evicted = 0
            val maybeCanEvict = tileCache.keys().toList().toMutableList()

            while (fillRate >= END_GC_THRESHOLD && maybeCanEvict.isNotEmpty()) {
                // check if this tile can be GC'd
                // remove it from the list of items to check
                val tile = maybeCanEvict.removeAt(0)
                if (tile !in tilesInUse) {
                    fillRate = tileCache.size / MAX_TILES_RAM
                    evicted++
                    evict(tile)
                }
            }

            Logger.info("Evicted $evicted items this GC, reached fill rate of ${fillRate * 100}%")

            tilesInUse.clear()
            gcs++
        }
    }

    /** Tells the cache that the tile is in use and should not be GC'd */
    fun markUsed(tile: Vector3) {
        tilesInUse.add(tile)
    }

    /** Evict a tile, must be on the main thread */
    private fun evict(key: Vector3) {
        val tileStr = "(${key.x.toInt()},${key.y.toInt()},${key.z.toInt()})"
        Logger.trace("Tile $tileStr evicted")
        tileCache[key].disposeSafely()
        tileCache.remove(key)
    }

    fun purge() {
        Logger.debug("Purging GCTileCache")
        for (key in tileCache.keys()) {
            evict(key)
        }
        tileCache.clear()
        tilesInUse.clear()
        fetchTimes.clear()
        gcs = 0
        hits = 1
        misses = 0
    }

    /** @return cache hit rate stats for displaying */
    fun getStats(): String {
        val hitRate = ((hits / (hits + misses).toDouble()) * 100.0).roundToInt()
        return "Tile LRU    hit: $hitRate%    " +
         "size: ${tileCache.size}     GCs: $gcs    " +
          "fetch: ${fetchTimes.mean.roundToInt()} ms"
    }

    override fun dispose() {
        Logger.debug("Shutdown GCTileCache")
        purge()
        executor.shutdownNow()
        defaultTexture.disposeSafely()
    }
}
