package com.decosegfault.atlas

import org.tinylog.kotlin.Logger
import java.util.concurrent.TimeUnit

/**
 * This file manages starting the Docker tileserver
 */
object TileServerManager {
    private val DOCKER_CONTAINERS_CMD = listOf("docker", "ps")
    private val DOCKER_START_CMD = listOf("ls", "-lah") // TODO
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

    }
}
