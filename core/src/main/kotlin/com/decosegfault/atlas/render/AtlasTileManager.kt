package com.decosegfault.atlas.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.decosegfault.atlas.util.AtlasUtils
import kotlin.math.*

/**
 * Tile manager that generates the basic and largest possible resolution tiles.
 * Controls how tiles are chosen to be rendered.
 *
 * @author Henry Batt
 */
class AtlasTileManager {

    /** Collection of all largest-resolution tiles that make up plane */
    private var tileSurface = mutableListOf<Tile>()

    /** Range of zoom levels to generate tiles in. */
    private var MAX_ZOOM = 20
    private var MIN_ZOOM = 13

    /** Grid size of smallest possible tile unit. */
    private var MIN_TILE = 16f

    /** Largest possible resolution for a tile based on the min zoom level. */
    private var MAX_SIZE = MIN_TILE * 2.0.pow(23 - MIN_ZOOM).toFloat()

    /** Smallest possible resolution for a tile based on the max zoom level. */
    private var MIN_SIZE = MIN_TILE * 2.0.pow(23 - MAX_ZOOM).toFloat()

    /** Maximum distance to be from tile, for resolution scaling. */
    private var MAX_DIST = 1024

    /** Latitude and Longitude of NW most tile in lookup. */
    private val BRISBANE_LAT = -26.80936358805377
    private val BRISBANE_LON = 152.58018493652344

    /** Size of grid to draw based on number of the largest tiles. */
    private val NUM_X_TILES = 20
    private val NUM_Y_TILES = 36

    /** Number of tiles retrieved in last getTiles/getTilesCulled call. */
    var numRetrievedTiles = 0

    init {
        // Shift to centre tile plane on (0, 0, 0)
        val xShift = NUM_X_TILES/2
        val zShift = NUM_Y_TILES/2

        // Tile lookup calculation from Open Street Map Wiki
        // [https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames]
        val n = 1 shl MIN_ZOOM
        val xTile = ((BRISBANE_LON + 180.0) / 360.0 * n).toInt().toFloat() + xShift
        val yTile = ((1.0 - asinh(tan(BRISBANE_LAT * PI / 180)) / PI) / 2.0 * n).toInt().toFloat() + zShift

        // Create all the largest tiles
        for (i in -xShift until xShift) {
            for (j in -zShift until zShift) {
                tileSurface.add(Tile(i * MAX_SIZE, j * MAX_SIZE, MAX_SIZE,
                    Vector3(xTile + i, yTile + j, MIN_ZOOM.toFloat())
                ))
            }
        }
    }

    /**
     * Calculates the desired size of tiles to be rendered based
     * on the vertical distance of the camera.
     *
     * @param cam   The camera from where calculations relate to.
     *
     * @return  The size of the tiles to find.
     */
    private fun calculateSize(cam: Camera): Float {
        val dist = cam.position.y
        val distance = 2.0.pow((dist / MAX_DIST).toInt()).toFloat()
        return MIN_SIZE * distance
    }

    /**
     * Calculates the desired size of a tile to be rendered based
     * on its distance from the camera.
     *
     * @param cam   The camera from where calculations relate to.
     * @param tile  The tile to calculate size of
     *
     * @return  The size of the tiles to find.
     */
    private fun calculateSize(cam: Camera, tile: Tile): Float {
        val closestPoint = AtlasUtils.bboxClosestPoint(cam.position, tile.bbox)
        val dist = cam.position.dst(closestPoint)
        val distance = 2.0.pow((dist / MAX_DIST).toInt()).toFloat()
        return MIN_SIZE * distance
    }

    /**
     * Get all the tiles that make up the plane for a given size/resolution.
     *
     * @param size  The size/resolution of the tiles to find.
     *
     * @return A collection of all the tiles at this size.
     */
    fun getTiles(size: Float): MutableList<Tile> {
        val tiles = mutableListOf<Tile>()
        for (tile: Tile in tileSurface) {
            for (subTile in tile.getTiles(size)) {
                tiles.add(subTile)
            }
        }
        numRetrievedTiles = tiles.size
        return tiles
    }

    /**
     * Get all the tiles that make up a plane at a calculated size.
     *
     * @param cam   Camera to calculate tile size in relation to.
     *
     * @return A collection of all the tiles of calculated size.
     */
    fun getTiles(cam: Camera): MutableList<Tile> {
        val size = calculateSize(cam)
        val tiles = mutableListOf<Tile>()
        for (tile: Tile in tileSurface) {
            for (subTile in tile.getTiles(size)) {
                tiles.add(subTile)
            }
        }
        numRetrievedTiles = tiles.size
        return tiles
    }

    /**
     * Get all the tiles in the plane of a given size, culling unnecessary ones
     * in relation to the camera and graphics settings.
     *
     * @param size      The size of tiles to find.
     * @param cam       The camera to cull in relation to.
     * @param graphics  Graphics setting for culling.
     *
     * @return  Collection of the tiles in the plane that satisfy conditions.

     */
    fun getTilesCulled(size: Float, cam: Camera, graphics: GraphicsPreset): MutableList<Tile> {
        val tiles = mutableListOf<Tile>()
        for (tile in tileSurface) {
            for (subTile in tile.getTilesCulled(size, cam, graphics)) {
                tiles.add(subTile)
            }
        }
        numRetrievedTiles = tiles.size
        return tiles
    }

    /**
     * Get all the tiles in the plane, culling unnecessary ones
     * in relation to the camera and graphics settings.
     *
     * @param cam       The camera to cull in relation to.
     * @param graphics  Graphics setting for culling.
     *
     * @return  Collection of the tiles in the plane that satisfy conditions.
     */
    fun getTilesCulled(cam: Camera, graphics: GraphicsPreset): MutableList<Tile> {
        val tiles = mutableListOf<Tile>()
        for (tile in tileSurface) {
            for (subTile in tile.getTilesCulled(calculateSize(cam, tile), cam, graphics)) {
                tiles.add(subTile)
            }
        }
        numRetrievedTiles = tiles.size
        return tiles
    }
}
