package com.decosegfault.atlas

import org.tinylog.kotlin.Logger
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * This file manages starting the Docker tileserver
 */
object TileServerManager {
    /** URL of the tile server */
    private const val TILESERVER_URL = "http://localhost:8080/tile/{z}/{x}/{y}.png"

    private val DOCKER_CONTAINERS_CMD = listOf("docker", "ps")

    private val DOCKER_START_CMD = "docker run -p 8080:80 -e THREADS=16 -v osm-data:/data/database -d overv/openstreetmap-tile-server run".split(" ")

    /** Name of the OSM tile server container */
    private const val CONTAINER_NAME = "overv/openstreetmap-tile-server"

    /** Returns true if tile server container is already running */
    private fun isTileServerAlreadyRunning(): Boolean {
        val cmd = ProcessBuilder().command(DOCKER_CONTAINERS_CMD).start()
        if (!cmd.waitFor(10, TimeUnit.SECONDS)) {
            Logger.error("Command $DOCKER_CONTAINERS_CMD timed out!!?")
            return false
        }
        val stdout = cmd.inputStream.reader().readLines()
        for (line in stdout) {
            if (CONTAINER_NAME in line && "Up" in line) {
                return true
            }
        }
        return false
    }

    /**
     * Starts the tileserver if it was not yet started.
     * @throws RuntimeException if tile server is unable to be started
     */
    fun maybeStartTileServer() {
        if (isTileServerAlreadyRunning()) {
            Logger.info("Tile server is already running")
            return
        }

        Logger.info("Starting tile server: $DOCKER_START_CMD")
        val cmd = ProcessBuilder().command(DOCKER_START_CMD).start()
        if (!cmd.waitFor(10, TimeUnit.SECONDS)) {
            throw RuntimeException("Docker command timed out")
        }

        val stdout = cmd.inputStream.reader().readText()
        val stderr = cmd.errorStream.reader().readText()
        // TODO
    }

    /**
     * Polls the tile server for a response. Uses HttpUrlConnection, so **blocks thread**.
     * @return true if the tile server is up and running
     */
    fun pollTileServer(): Boolean {
        val url = URL(TILESERVER_URL
            .replace("{x}", "0")
            .replace("{y}", "0")
            .replace("{z}", "0"))
        Logger.debug("Attempting to contact tile server: $url")

        return try {
            val conn = url.openStream()
            conn.readAllBytes()
            conn.close()
            true
        } catch (e: IOException) {
            Logger.warn("Unable to contact tile server: $e")
            false
        }
    }
}
