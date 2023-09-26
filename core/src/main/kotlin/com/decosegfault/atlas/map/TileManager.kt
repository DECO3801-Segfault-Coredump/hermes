package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.decosegfault.atlas.render.GraphicsPreset
import com.decosegfault.atlas.util.AtlasUtils
import kotlin.math.*

/**
 * Tile manager that generates the basic and largest possible resolution tiles.
 * Controls how tiles are chosen to be rendered.
 *
 * @author Henry Batt
 */
class TileManager {

    /** Collection of all largest-resolution tiles that make up plane */
    private var tileSurface = mutableListOf<Tile>()

    /** Number of tiles retrieved in last getTiles/getTilesCulled call. */
    var numRetrievedTiles = 0

    init {
        val nwTile = AtlasUtils.latLongZoomToSlippyCoord(Vector3(AtlasUtils.NW_BRISBANE_LAT_LONG, 13f))
        val xShift = AtlasUtils.NUM_X_TILES/2
        val zShift = AtlasUtils.NUM_Y_TILES/2
        // Create all the largest tiles
        for (i in -xShift until xShift) {
            for (j in -zShift until zShift) {
                tileSurface.add(Tile(i * AtlasUtils.MAX_SIZE, j * AtlasUtils.MAX_SIZE, AtlasUtils.MAX_SIZE,
                    Vector3(nwTile.x + i + xShift, nwTile.y + j + zShift, AtlasUtils.MIN_ZOOM.toFloat())
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
        val distance = 2.0.pow((dist / AtlasUtils.MAX_DIST).toInt()).toFloat()
        return AtlasUtils.MIN_SIZE * distance
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
        val distance = 2.0.pow((dist / AtlasUtils.MAX_DIST).toInt()).toFloat()
        return AtlasUtils.MIN_SIZE * distance
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
            for (subTile in tile.getTilesCulledHeightScaled(size, cam, graphics)) {
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
    fun getTilesCulledHeightScaled(cam: Camera, graphics: GraphicsPreset): MutableList<Tile> {
        val tiles = mutableListOf<Tile>()
        val height = max(cam.position.y/400, 1f)

        for (tile in tileSurface) {
                for (subTile in tile.getTilesCulledHeightScaled(height, cam, graphics)) {
                    tiles.add(subTile)
            }
        }
        numRetrievedTiles = tiles.size
        return tiles
    }
}
