package com.decosegfault.atlas.render

import com.badlogic.gdx.graphics.Camera
import kotlin.math.pow

/**
 * Tile manager that generates the basic and largest possible resolution tiles.
 * Controls how tiles are chosen to be rendered.
 *
 * @author Henry Batt
 */
class AtlasTileManager {

    /** Collection of all largest-resolution tiles that make up plane */
    private var tileSurface = mutableListOf<Tile>()

    /** Largest possible resolution for a tile. */
    private var MAX_SIZE = 128f

    /** Smallest possible resolution for a tile. */
    private var MIN_SIZE = 64f

    /** Maximum distance to be from tile, for resolution scaling. */
    private var MAX_DIST = 128

    var numRetrievedTiles = 0

    init {
        for (i in -512 until 512 step MAX_SIZE.toInt()) {
            for (j in -512 until 512 step MAX_SIZE.toInt()) {
                val tile = Tile(i.toFloat(), j.toFloat(), MAX_SIZE)
                tileSurface.add(tile)
                tile.generateSubTiles(MIN_SIZE)
            }
        }
    }

    /**
     * Calculates the desired size of tiles to be rendered.
     *
     * @param cam   The camera from where calculations relate to.
     *
     * @return  The size of the tiles to find.
     */
    private fun calculateSize(cam: Camera): Float {
//        val closestPoint = AtlasUtils.bboxClosestPoint(cam.position, tile.bbox)
//        val dist = cam.position.dst(closestPoint)
        val dist = cam.position.y
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
     * @return A collection of all the tiles.
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
     */
    fun getTilesCulled(cam: Camera, graphics: GraphicsPreset): MutableList<Tile> {
        val size = calculateSize(cam)
        val tiles = mutableListOf<Tile>()
        for (tile in tileSurface) {
            for (subTile in tile.getTilesCulled(size, cam, graphics)) {
                tiles.add(subTile)
            }
        }
        numRetrievedTiles = tiles.size
        return tiles
    }
}
