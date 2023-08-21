package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.Vector3
import org.tinylog.kotlin.Logger
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * This file manages the Docker tile server
 *
 * @author Matt Young
 */
object TileServerManager {
    /** URL of the tile server */
    private const val TILESERVER_URL = "http://localhost:8080/tile/{z}/{x}/{y}.png"

    /** Command to start the OSM docker container */
    private val DOCKER_START_CMD = "docker run -p 8080:80 -p 5432:5432 -e THREADS=16 -v osm-data:/data/database -v osm-tiles:/data/tiles -d overv/openstreetmap-tile-server run".split(" ")

    /** Name of the OSM tile server container */
    private const val CONTAINER_NAME = "overv/openstreetmap-tile-server"

    /** Returns true if tile server container is already running */
    private fun isTileServerAlreadyStarted(): Boolean {
        val cmd = ProcessBuilder().command("docker", "ps").start()
        if (!cmd.waitFor(10, TimeUnit.SECONDS)) {
            Logger.error("docker ps timed out!!?")
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
        if (isTileServerAlreadyStarted()) {
            Logger.info("Tile server is already started")
            return
        }

        Logger.info("Starting tile server: $DOCKER_START_CMD")
        val cmd = ProcessBuilder().command(DOCKER_START_CMD).start()
        if (!cmd.waitFor(10, TimeUnit.SECONDS)) {
            throw RuntimeException("Docker command timed out")
        }

        val stderr = cmd.errorStream.reader().readText()
        if (stderr.isNotEmpty()) {
            Logger.error("Failed to start tile server, command: $DOCKER_START_CMD, stderr:\n$stderr")
            throw RuntimeException("Failed to start tile server! See logs")
        }
    }

    /**
     * Fetch a tile from the tile server. Returns the PNG bytes if the response was successful, otherwise
     * null.
     * @param pos tile pos (x, y, zoom)
     */
    private fun fetchTile(pos: Vector3): ByteArray? {
        val url = URL(
            TILESERVER_URL
                .replace("{x}", pos.x.toInt().toString())
                .replace("{y}", pos.y.toInt().toString())
                .replace("{z}", pos.z.toInt().toString()))
        Logger.debug("Fetching tile: $url")

        var conn: InputStream? = null
        return try {
            conn = url.openStream()
            val bytes = conn?.readAllBytes()
            bytes
        } catch (e: IOException) {
            Logger.warn("Unable to contact tile server: $e")
            null
        } finally {
            conn?.close()
        }
    }

    /**
     * Calls [fetchTile] and converts the result into a [Pixmap] if the URL returned a valid response
     */
    fun fetchTileAsPixmap(pos: Vector3): Pixmap? {
        val bytes = fetchTile(pos) ?: return null
        return Pixmap(bytes, 0, bytes.size)
    }

    /**
     * Polls the tile server for a response. Uses HttpUrlConnection, so **blocks thread**.
     * @return true if the tile server is up and running
     */
    fun pollTileServer(): Boolean {
        return fetchTile(Vector3.Zero) != null
    }
}
