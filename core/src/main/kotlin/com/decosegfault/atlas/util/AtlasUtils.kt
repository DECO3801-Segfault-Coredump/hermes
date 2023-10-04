package com.decosegfault.atlas.util

import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.BoundingBox
import org.tinylog.kotlin.Logger
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.math.*
import com.decosegfault.hermes.types.SimType
import java.io.IOException
import java.lang.IllegalArgumentException
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.*

/**
 * @author Various (see comments)
 */
object AtlasUtils {

    /** Range of zoom levels to generate tiles in. */
    val MAX_ZOOM = 20
    val MIN_ZOOM = 13

    /** Size of grid to draw based on number of the largest tiles. */
    val NUM_X_TILES = 20
    val NUM_Y_TILES = 32

    /** Latitude and Longitude of NW most tile in lookup. */
    val NW_BRISBANE_LAT_LONG = Vector2(-26.809364f, 152.58018f)

    /** Maximum distance to be from tile, for resolution scaling. */
    val MAX_DIST = 1024

    /** Grid size of the smallest possible tile unit, and its corresponding zoom level. */
    val MIN_TILE = 16f
    val TOTAL_MAX_ZOOM = 23
    val PIXEL_ZOOM = TOTAL_MAX_ZOOM + log2(MIN_TILE)

    /** Largest possible resolution for a tile based on the min zoom level. */
    val MAX_SIZE = MIN_TILE * 2.0.pow(TOTAL_MAX_ZOOM - MIN_ZOOM).toFloat()

    /** Smallest possible resolution for a tile based on the max zoom level. */
    val MIN_SIZE = MIN_TILE * 2.0.pow(TOTAL_MAX_ZOOM - MAX_ZOOM).toFloat()

    /** Dynamically calculate the slippy centre of the map based on NW corner and size */
    val NW_BRISBANE_SLIPPY_CORRD = latLongZoomToSlippyCoord(Vector3(NW_BRISBANE_LAT_LONG, MIN_ZOOM.toFloat()))
    val MAP_CENTRE_SLIPPY = latLongZoomToSlippyCoord(slippyCoordToLatLongZoom(
        Vector3(NW_BRISBANE_SLIPPY_CORRD).add(NUM_X_TILES / 2f, NUM_Y_TILES / 2f, 0f))
        .add(0f, 0f, (PIXEL_ZOOM - MIN_ZOOM))).sub(0f, 0f, PIXEL_ZOOM)

    // https://gdbooks.gitbooks.io/3dcollisions/content/Chapter1/closest_point_aabb.html
    fun bboxClosestPoint(point: Vector3, bounds: BoundingBox): Vector3 {
        return Vector3(
            // This is fucking insane, but coerceIn fucking ends up turning these into infinity
            // somehow. So yes a standard Kotlin function is bugged to shit. Good one.
//            point.x.coerceIn(bounds.min.x, bounds.max.x),
//            point.y.coerceIn(bounds.min.y, bounds.max.y),
//            point.z.coerceIn(bounds.min.z, bounds.max.z)

            MathUtils.clamp(point.x, bounds.min.x, bounds.max.x),
            MathUtils.clamp(point.y, bounds.min.y, bounds.max.y),
            MathUtils.clamp(point.z, bounds.min.z, bounds.max.z)
        )
    }

    fun writeHermesPreset(name: String) {
        val path = Paths.get(System.getProperty("user.home"), "Documents", "DECOSegfault", "hermes.txt")
        if (!path.exists()) {
            Logger.debug("Creating hermes.txt file")
            path.createFile()
        }
        Logger.info("Writing Hermes preset $name to path: $path")
        path.writeText(name.uppercase())
    }

    fun readHermesPreset(): SimType {
        val path = Paths.get(System.getProperty("user.home"), "Documents", "DECOSegfault", "hermes.txt")
        Logger.info("Loading Hermes preset from: $path")
        var name: String
        try {
            name = path.readText().trim()
            Logger.info("Using saved Hermes preset: $name")
        } catch (e: IOException) {
            Logger.info("Using default hermes preset 'History', saved does not exist")
            name = "History"
        }
        return try {
            SimType.valueOf(name.uppercase())
        } catch (e: IllegalArgumentException) {
            Logger.error("Invalid Hermes preset $name!!! Using standard!")
            SimType.HISTORY
        }
    }

    /**
     * Converts the given latitude, longitude and zoom level into Slippy Tile Map lookup
     *
     * @param latLongZoom   Vector containing the latitude, longitude, and zoom level to find.
     * @return A vector containing the x, y, zoom values for the tile.
     */
    fun latLongZoomToSlippyCoord(latLongZoom: Vector3) : Vector3 {
        // Tile lookup calculation from Open Street Map Wiki
        // [https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames]
        val n = 1 shl latLongZoom.z.toInt()
        val xTile = ((latLongZoom.y + 180.0) / 360.0 * n).toInt().toFloat()
        val yTile = ((1.0 - asinh(tan(latLongZoom.x * PI / 180)) / PI) / 2.0 * n).toInt().toFloat()
        return Vector3(xTile, yTile, latLongZoom.z)
    }

    /**
     * See [latLongZoomToSlippyCoord]
     */
    fun latLongZoomToSlippyCoord(lat: Double, long: Double) : Vector3 {
        // Tile lookup calculation from Open Street Map Wiki
        // [https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames]
        val n = 1 shl PIXEL_ZOOM.toInt()
        val xTile = ((long + 180.0) / 360.0 * n)
        val yTile = ((1.0 - asinh(tan(lat * PI / 180)) / PI) / 2.0 * n)
        return Vector3(xTile.toFloat(), yTile.toFloat(), 0f)
    }

    /**
     * Converts a given x, y, and zoom level for a slippy tile into a latitude, and longitude.
     * Inverse of latLongZoomToSlippyCoord method.
     *
     * @param xYZoom    Vector containing the x, y, zoom of a tile to get latitude and longitude of.
     * @return  A vector with the latitude, and longitude of the given tile.
     */
    fun slippyCoordToLatLongZoom(xYZoom: Vector3) : Vector3 {
        // Tile conversion calculation from Open Street Map Wiki
        // [https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames]
        val n = 1 shl xYZoom.z.toInt()
        val long = xYZoom.x / n * 360f - 180f
        val lat = atan(sinh(PI * (1 - 2 * xYZoom.y / n))) * 180 / PI
        return Vector3(lat.toFloat(), long, xYZoom.z)
    }

    /**
     * Converts lat/long coords into Atlas coords
     * **suitable for calling AtlasVehicle.updateTransform**
     *
     * @param latLong   Vector containing latitude, longitude to find x,y pixel coords for.
     * @return Vector with x,y pixel coords for latLong, with z same as latLong param.
     */
    fun latLongToAtlas(latLong: Vector3): Vector3 {
        val coords = latLongZoomToSlippyCoord(Vector3(latLong.x, latLong.y, PIXEL_ZOOM))
        coords.z = latLong.z
        return coords.sub(MAP_CENTRE_SLIPPY)
    }

    /**
     * See [latLongToAtlas]
     */
    fun latLongToAtlas(lat: Double, long: Double, theta: Double): Vector3 {
        val coords = latLongZoomToSlippyCoord(lat, long)
        coords.z = theta.toFloat()
        return coords.sub(MAP_CENTRE_SLIPPY)
    }

    /**
     * @see [latLongToAtlas]
     */
    fun latLongToAtlas(latLong: Vector2): Vector2 {
        val coords = latLongZoomToSlippyCoord(Vector3(latLong.x, latLong.y, PIXEL_ZOOM))
        coords.sub(MAP_CENTRE_SLIPPY)
        return Vector2(coords.x, coords.y)
    }

    /**
     * Converts Atlas coords into lat/long coords.
     *
     * @param atlasCoords   Vector with x,y pixel coords for latLong, with z same as latLong param.
     * @return Vector containing latitude, longitude to find x,y pixel coords for.
     */
    fun atlasToLatLong(atlasCoords: Vector3): Vector3 {
        val coords = Vector3(atlasCoords).add(MAP_CENTRE_SLIPPY)
        val latLong = slippyCoordToLatLongZoom(Vector3(coords.x, coords.y, PIXEL_ZOOM))
        latLong.z = coords.z
        return latLong
    }

    /**
     * @see [atlasToLatLong]
     */
    fun atlasToLatLong(atlasCoords: Vector2): Vector2 {
        val coords = Vector2(atlasCoords).add(Vector2(MAP_CENTRE_SLIPPY.x, MAP_CENTRE_SLIPPY.y))
        val latLong = slippyCoordToLatLongZoom(Vector3(coords.x, coords.y, PIXEL_ZOOM))
        return Vector2(latLong.x, latLong.y)
    }
}
