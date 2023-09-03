package com.decosegfault.atlas.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.decosegfault.atlas.util.AtlasUtils

/**
 * A square planar tile with recursive sub-tiles, and image decal for texturing.
 *
 * @author Henry Batt
 * @param x     X-coordinate of tile centre
 * @param z     Z-coordinate of the tile centre
 * @param size  The size of the square tile
 *
 * The basic framework and culling behaviour is based upon AtlasVehicle by Matt Young
 */
data class Tile(val x: Float, val z: Float, val size: Float) : Disposable {

    /** Bounding box of the tile*/
    private var bbox = BoundingBox()

    /** If the tile was culled in the last render pass */
    var didCull = false

    /** If the tile was divided into sub-tiles in the last render pass */
    var didUseSubTiles = false

    /** Decal display of tile */
    private var decal : Decal

    /** The texture region to load onto tile */
    private var texture : TextureRegion

    private var texturePath : String

    /** Breakdown of tile in 2x2 sub-tiles */
    private var subTiles = mutableListOf<Tile>()

    init {
        val shift = size / 2
        val minX = x
        val minZ = z
        bbox = BoundingBox(Vector3(minX, 0f, minZ), Vector3(minX + size, 0f, minZ + size))
        texturePath = "tiles/scallop128.png"
        texture = TextureRegion(Texture(texturePath))
        decal = Decal.newDecal(size, size, texture)
        decal.setPosition(x + shift, 0f, z + shift)
        decal.setRotationX(270f)
    }

    /**
     * Recursively generate 4 sub-tiles till min size reached
     *
     * @param min_size  Minimum size of sub-tiles to generate.
     */
    fun generateSubTiles(min_size: Float) {
        // Smallest resolution tile reached. Base Case
        if (size == min_size) {
            return
        }

        val subSize = size / 2

        // Create the 4 sub-tiles
        val q1 = Tile(x, z, subSize)
        val q2 = Tile(x + subSize, z, subSize)
        val q3 = Tile(x , z + subSize, subSize)
        val q4 = Tile(x + subSize, z + subSize, subSize)
        subTiles.add(q1)
        subTiles.add(q2)
        subTiles.add(q3)
        subTiles.add(q4)

        // Generate the sub-tile's sub-tiles
        q1.generateSubTiles(min_size)
        q2.generateSubTiles(min_size)
        q3.generateSubTiles(min_size)
        q4.generateSubTiles(min_size)
    }


    /** Return the decal that represents this tile */
    fun getDecal(): Decal {
        return decal
    }

    /**
     * Get all sub-tiles that make up this tile at desired resolution.
     *
     * @param scale The desired tile resolution to get tiles for
     *
     * @return A list of all the sub-tiles at given scale
     */
    fun getTiles(scale: Float): MutableList<Tile> {

        val allTiles = mutableListOf<Tile>()

        // Found desired resolution, grab it. Base Case
        if (size == scale) {
            allTiles.add(this)

        // Not at resolution yet, add sub-tiles tiles at scale
        } else if (size > scale) {
            for (tile in subTiles) {
                for (subTile in tile.getTiles(scale)) {
                    allTiles.add(subTile)
                }
            }
        }
        return allTiles
    }

    /**
     * Get all sub-tiles that make up tile at a given resolution,
     * but apply culling methods to reduce amount to only desired.
     *
     * @param scale     The desired tile resolution to get.
     * @param cam       Scene camera to cull in relation to.
     * @param graphics  Graphics preset to dictate culling amount.
     *
     * @return All the sub-tiles at given scale that haven't been culled
     */
    fun getTilesCulled(scale: Float, cam: Camera, graphics: GraphicsPreset):  MutableList<Tile>{
        val allTiles = mutableListOf<Tile>()
        didCull = false
        didUseSubTiles = false

        // Check iff can be culled
        val closestPoint = AtlasUtils.bboxClosestPoint(cam.position, bbox)
        val dist = cam.position.dst(closestPoint)
        if (dist >= graphics.tileDrawDist) {
            didCull = true
            return allTiles
        }
        if (!cam.frustum.boundsInFrustum(bbox)) {
            didCull = true
            return allTiles
        }

        // We are at correct resolution check now
        if (size <= scale) {
            allTiles.add(this)
        }

        // Can get more detailed, check them all and see
        if (size > scale) {
            for (tile in subTiles) {
                didUseSubTiles = true
                for (subTile in tile.getTilesCulled(scale, cam, graphics)) {
                    allTiles.add(subTile)
                }
            }
        }
        return allTiles
    }

    /** Clean up tile and all corresponding sub-tiles */
    override fun dispose() {
        for (tile in subTiles) {
            tile.dispose()
        }
        subTiles.clear()
    }

    fun debug(render: ShapeRenderer) {
        if (didCull) return
        render.color = if (didUseSubTiles) Color.GREEN else Color.RED
        render.box(bbox.min.x, bbox.min.y, bbox.max.z, bbox.width, bbox.height, bbox.depth)
    }
}
