package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import io.github.sebasbaumh.postgis.PGgeometry
import org.postgresql.PGConnection
import org.postgresql.geometric.PGpolygon
import org.tinylog.kotlin.Logger
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * Class that handles the asynchronous generation of building models from OpenStreetMap PostGIS data.
 * For more info see docs/atlas_buildings_design.md.
 *
 * @author Matt Young
 */
object BuildingGenerator {
    /** PostGIS JDBC URI */
    private const val DB_URI = "jdbc:postgresql://localhost:5432/gis"

    /** PostGIS DB username & password */
    private const val DB_USERPASS = "renderer"

    /** WGS84 projection ID according to EPSG: https://epsg.io/3857 */
    private const val WGS84_ID = 4326

    /** Web Mercator projection ID according to EPSG: https://epsg.io/3857 */
    private const val WEB_MERCATOR_ID = 3857

    /** Approximate height in metres of one storey. Source: https://en.wikipedia.org/wiki/Storey#Overview */
    private const val STOREY_HEIGHT = 4.3f

    /**
     * Query to find polygons and tags for OSM buildings in a bounding box. Assumes coordinates in Web
     * Mercator (EPSG coord ID 3857), which is true for this osm2pgsql generated PostGIS DB.
     * Selects the polygon of the building ("way") and the number of levels if available. For more info see
     * the notes section of atlas_buildings_design.md.
     *
     * Original source: https://gis.stackexchange.com/a/460730 (rewritten by Matt to fix it up quite a bit)
     */
    private val BUILDING_QUERY = """SELECT ST_AsText(way), tags->'building:levels'
        |FROM planet_osm_polygon
        |WHERE (way && st_makebox2d(st_point(?, ?), st_point(?, ?)))
        |AND building IS NOT NULL;
    """.trimMargin()

    private lateinit var conn: Connection

    /** Prepared statement consisting of [BUILDING_QUERY] */
    private lateinit var statement: PreparedStatement

    /** Connects to the PostGIS database */
    fun connect() {
        Logger.info("Connecting to PostGIS database")

        conn = DriverManager.getConnection(DB_URI, DB_USERPASS, DB_USERPASS)
        with (conn as PGConnection) {
            addDataType("geometry", PGgeometry::class.java)
            addDataType("polygon", PGpolygon::class.java)
        }
        Logger.info("Connected to PostGIS successfully! ${conn.metaData.databaseProductName} ${conn.metaData.databaseProductVersion}")

        statement = conn.prepareStatement(BUILDING_QUERY)
        // test generated using scripts/wgs84_to_webmercator.py
        val min = Vector2(17032550.567945454f, -3186449.72959389f)
        val max = Vector2(17033933.18596778f, -3184777.3303385503f)
        getBuildingsInBounds(min, max)
    }

    private fun getBuildingsInBounds(min: Vector2, max: Vector2) {
        statement.setFloat(1, min.x)
        statement.setFloat(2, min.y)
        statement.setFloat(3, max.x)
        statement.setFloat(4, max.y)
        try {
            val result = statement.executeQuery()
            Logger.debug("Results: ${result}")
        } catch (e: SQLException) {
            Logger.warn("Unable to query building in bounds, min: $min, max: $max, $e")
            Logger.warn(e)
        }
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
