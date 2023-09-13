package com.decosegfault.atlas.util

import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.BoundingBox
import com.decosegfault.atlas.render.GraphicsPreset
import com.decosegfault.atlas.render.GraphicsPresets
import com.decosegfault.hermes.types.SimType
import org.tinylog.kotlin.Logger
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.*

/**
 * @author Various (see comments)
 */
object AtlasUtils {
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


    /** Size of grid to draw based on number of the largest tiles. */
    private val NUM_X_TILES = 20
    private val NUM_Y_TILES = 36
    private var MIN_ZOOM = 13


    fun latLongToTileNum(latLong: Vector2) : Vector2 {
        // Shift to centre tile plane on (0, 0, 0)
        val xShift = NUM_X_TILES/2
        val zShift = NUM_Y_TILES/2

        // Tile lookup calculation from Open Street Map Wiki
        // [https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames]
        val n = 1 shl MIN_ZOOM
        val xTile = ((latLong.y + 180.0) / 360.0 * n).toInt().toFloat() + xShift
        val yTile = ((1.0 - asinh(tan(latLong.x * PI / 180)) / PI) / 2.0 * n).toInt().toFloat() + zShift
        return Vector2(xTile, yTile)
    }



    /**
     * Converts lat/long coords into Atlas coords
     * **suitable for calling AtlasVehicle.updateTransform**
     */
    fun latLongToAtlas(latLong: Vector3): Vector3 {
        val xShift = NUM_X_TILES/2
        val zShift = NUM_Y_TILES/2

        val width = 20f * 2f.pow(7f) // FIXME @Henry Batt
        val height = 36f * 2f.pow(7f) // FIXME @Henry Batt
        val x = (latLong.y + 180) * width / 360

        val latRad = latLong.x * PI /180
        val mercN = ln(tan((PI/4) + (latRad/2)))
        val y = (height/2)-(width*mercN/(2*PI))


        return Vector3(x, y.toFloat(), latLong.z)
//        val n = 1 shl 20
//        val xTile = ((latLong.y + 180.0) / 360.0 * n) + xShift
//        val yTile = ((1.0 - asinh(tan(latLong.x * PI / 180)) / PI) / 2.0 * n) + zShift
//
//        return Vector3(xTile.toFloat(), yTile.toFloat(), latLong.z)
    }
}
