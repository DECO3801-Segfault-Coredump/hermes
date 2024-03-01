/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas.util

import com.badlogic.gdx.math.WindowedMean
import com.badlogic.gdx.utils.Disposable
import com.google.common.util.concurrent.ThreadFactoryBuilder
import ktx.assets.disposeSafely
import org.tinylog.kotlin.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * A generic garbage-collected cache for items that take a long time to instantiate. Items in the cache are
 * instantiated asynchronously as required. The cache will start garbage collecting
 * unused items (items not marked as used) once it reaches [startGcThreshold] out of [maxItems].
 * It will try and remove enough items to reach the end threshold [endGcThreshold].
 *
 * Note that [maxItems] is not a hard limit, and the cache can _theoretically_ go beyond this bound in
 * extreme situations. It only garbage collects on a "best effort" basis.
 *
 * @param K key type
 * @param V value type
 * @param name Name of the cache e.g. "TileGC"
 * @param maxItems **Soft** limit of items in VRAM, size will be approx 100 KiB * this
 * @param startGcThreshold If the cache is this% full, start trying to evict items
 *
 * @author Matt Young
 */
abstract class AbstractGarbageCollectedCache<K, V>(
    private val name: String,
    private val maxItems: Double,
    private val startGcThreshold: Double,
    private val endGcThreshold: Double,
    threadPoolSize: Int,
) : Disposable {
    private val cache = ConcurrentHashMap<K, V>() // threaded (executor)

    /** List of items which are currently in the process of being fetched and should not be re-fetched */
    private val pendingFetches = ConcurrentHashMap.newKeySet<K>() // threaded (executor)

    /** Items marked as currently in use on screen this frame */
    private val itemsInUse = mutableSetOf<K>() // serial (only called from main)

    /** Queue used to manage tasks the executor should run when it's full (overflow) */
    private val executorQueue = LinkedBlockingQueue<Runnable>()

    /**
     * Executor used for HTTP requests.
     * This is the exact same as `Executors.newFixedThreadPool`, but we control the queue.
     */
    private val executor = ThrowingThreadExecutor(
        threadPoolSize, threadPoolSize,
        0L, TimeUnit.MILLISECONDS,
        executorQueue,
        ThreadFactoryBuilder()
            .setNameFormat("$name-%d")
            .setDaemon(true)
            .build()
    )

    private val fetchTimes = WindowedMean(50)
    private var gcs = 0
    private var hits = 0
    private var misses = 0
    private var total = 0
    private var shouldGcNextFrame = false

    /** Implementers should override this function to instantiate a new item. Can block for as long as you want. */
    abstract fun newItem(key: K): V

    /**
     * Asynchronously retrieves an item. If the item isn't in the cache, it will be
     * downloaded asynchronously. [onRetrieved] is invoked after the item is made available.
     */
    fun retrieve(item: K, onRetrieved: (V) -> Unit) {
        val begin = System.nanoTime()
        val maybeItem = cache[item]
        if (maybeItem != null) {
            // item was already in cache
            onRetrieved(maybeItem)
            hits++
            return
        }

        // check if a pending fetch is in progress, and if so, tell caller to sod off
        if (pendingFetches.contains(item)) return
        pendingFetches.add(item)

        executor.submit {
            val newItem = newItem(item)
            cache[item] = newItem

            val end = (System.nanoTime() - begin) / 1e6f
            fetchTimes.addValue(end)
            total++

            pendingFetches.remove(item)
            onRetrieved(newItem)
        }
        misses++
    }

    /**
     * Garbage collect unused items that are not currently mark as used (being rendered on screen)
     * @param force If true, ignores [startGcThreshold] and [endGcThreshold] and forces collection
     * of all onscreen items
     */
    fun garbageCollect(force: Boolean = false) {
        var fillRate = cache.size / maxItems

        // perform a GC if we're above START_GC_THRESHOLD% or we were forced to
        if (fillRate >= startGcThreshold || force) {
            Logger.info("Garbage collecting $name, fill rate: ${fillRate * 100.0}%, in use: ${itemsInUse.size}")
            var evicted = 0
            val maybeCanEvict = cache.keys().toList().toMutableList()

            while ((fillRate >= endGcThreshold || force) && maybeCanEvict.isNotEmpty()) {
                // check if this item can be GC'd
                // remove it from the back of the list to save a shift down
                val item = maybeCanEvict.removeAt(maybeCanEvict.size - 1)
                if (item !in itemsInUse) {
                    fillRate = cache.size / maxItems
                    evicted++
                    evict(item)
                }
            }

            Logger.info("Evicted $evicted items this GC from $name, reached fill rate of ${fillRate * 100}%, left: ${maybeCanEvict.size}")
            gcs++
        }
    }

    /** Performs garbage collection and clears items in use for next frame */
    fun nextFrame() {
        if (shouldGcNextFrame) {
            Logger.debug("Forcing GC this frame")
            garbageCollect(true)
            shouldGcNextFrame = false
        } else {
            garbageCollect()
        }
        itemsInUse.clear()
    }

    /** Tells the garbage collector to GC on the next frame */
    fun gcNextFrame() {
        shouldGcNextFrame = true
    }

    /** Tells the cache that the item is in use and should not be GCd */
    fun markUsed(item: K) {
        itemsInUse.add(item)
    }

    /** Evict an item, **must be on the main thread** */
    open fun evict(key: K) {
        if (cache[key] is Disposable) {
            (cache[key] as Disposable).disposeSafely()
        }
        cache.remove(key)
    }

    /** Clear and reset cache */
    fun purge() {
        Logger.debug("Purging cache $name")
        for (key in cache.keys()) {
            evict(key)
        }
        cache.clear()
        itemsInUse.clear()
        fetchTimes.clear()
        gcs = 0
        hits = 1
        misses = 0
    }

    /** @return cache hit rate stats for displaying */
    fun getStats(): String {
        return "$name    size: ${cache.size}     GCs: $gcs    executor: ${executorQueue.size}    " +
            "pending: ${pendingFetches.size}    " +
            "fetch: ${fetchTimes.mean.roundToInt()} ms"
    }

    override fun dispose() {
        Logger.debug("Shutdown cache $name")
        purge()
        executor.shutdownNow()
    }
}
