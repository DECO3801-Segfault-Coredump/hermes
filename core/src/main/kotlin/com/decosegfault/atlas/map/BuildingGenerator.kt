package com.decosegfault.atlas.map

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Class that handles the asynchronous generation and texturing of building meshes
 *
 * @author Matt Young
 */
object BuildingGenerator {
    /** Limit thread pool size to 2x the number of processors to prevent memory issues */
    private val threadPoolSize = Runtime.getRuntime().availableProcessors() / 2

    /** Queue used to manage tasks the executor should run when it's full (overflow) */
    private val executorQueue = LinkedBlockingQueue<Runnable>()

    /**
     * Executor used for generating buildings.
     * This is the exact same as `Executors.newFixedThreadPool`, but we control the queue.
     */
    private val executor = ThreadPoolExecutor(
        threadPoolSize, threadPoolSize,
        0L, TimeUnit.MILLISECONDS,
        executorQueue,
        ThreadFactoryBuilder().setNameFormat("BuildingGen-%d").build()
    )

    /** Connects to the PostGIS database */
    fun connect() {

    }
}
