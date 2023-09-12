package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.math.Vector3
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.tinylog.kotlin.Logger
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Class that handles the asynchronous generation of building models from OpenStreetMap PostGIS data.
 * For more info see docs/atlas_buildings_design.md.
 *
 * @author Matt Young
 */
object BuildingGenerator {
    private lateinit var conn: Connection

    /** Connects to the PostGIS database */
    fun connect() {
        Logger.info("Connecting to PostGIS database")
        val props = Properties()
        props["user"] = "renderer"
        props["password"] = "renderer"

        conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/gis", props)
        Logger.info("Connected to PostGIS successfully! ${conn.metaData.databaseProductName} ${conn.metaData.databaseProductVersion}")
    }

    /**
     * Generates a building chunk. Buildings are packaged together into a ModelCache.
     */
    fun generateBuildingChunk(chunk: Vector3): ModelCache {
        val cache = ModelCache()
        cache.begin()
        cache.end()
        return cache
    }
}
