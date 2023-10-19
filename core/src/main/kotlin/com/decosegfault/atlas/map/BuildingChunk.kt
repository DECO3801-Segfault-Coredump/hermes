package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.decosegfault.atlas.render.GraphicsPreset
import com.decosegfault.atlas.util.AtlasUtils

/**
 * A square planar chunk with recursive sub-chunks to hold building models.
 *
 * @param x     X-coordinate of chunk centre
 * @param z     Z-coordinate of the chunk centre
 * @param size  The size of the square chunk in pixels
 *
 * @author Henry Batt
 * @author Matt Young
 */
class BuildingChunk(val x: Float, val z: Float, val size: Float) {
    var didCull = false
    var didUseSubChunk = false

    private val bbox = BoundingBox(Vector3(x, 0f, z), Vector3(x + size, 0f, z + size))

    private var subChunks = mutableListOf<BuildingChunk>()

    private var modelCache: ModelCache? = null

    /**
     * Returns all chucks that make up this grid chunk including itself.
     *
     * @return A list of all the sub-chunks and self.
     */
    fun getAllChunks(): MutableList<BuildingChunk> {
        val allChunks = mutableListOf<BuildingChunk>()
        allChunks.add(this)

        for (chunk in subChunks) {
            for (subChunk in chunk.getAllChunks()) {
                allChunks.add(subChunk)
            }
        }
        return allChunks
    }

    /**
     * Returns all the sub-chunks of a given size that make up this chunk
     *
     * @param targetSize    The size of the chunks to return
     * @return  A list of all the sub-chunk at this size
     */
    fun getSizeChunks(targetSize: Float): MutableList<BuildingChunk> {
        val allChunks = mutableListOf<BuildingChunk>()

        // At target size, return this. Base Case
        if (size <= targetSize) {
            allChunks.add(this)

            // Not at target yet, add all sub-chunks at size.
        } else {
            for (chunk in subChunks) {
                for (subChunk in chunk.getAllChunks()) {
                    allChunks.add(subChunk)
                }
            }
        }
        return allChunks
    }

    /**
     * Returns all the sub-chunks of a given size that make up this chunk
     * but applies culling methods to reduce amount of chunks.
     *
     * @param targetSize    The size of the chunks to return
     * @param cam           The Scene camera to cull chunks in relation to.
     * @param graphics      The graphics preset to dictate culling amounts.
     */
    fun getChunksCulled(targetSize: Float, cam: Camera, graphics: GraphicsPreset): MutableList<BuildingChunk> {
        val allChunks = mutableListOf<BuildingChunk>()
        didCull = false
        didUseSubChunk = false

        // Check iff the chunk should be culled
        val closestPoint = AtlasUtils.bboxClosestPoint(Vector3(cam.position.x, 0f, cam.position.z), bbox)
        val dist = cam.position.dst(closestPoint)
        if (dist > graphics.tileDrawDist || !cam.frustum.boundsInFrustum(bbox)) {
            didCull = true
            subChunks.clear()
            return allChunks
        }

        // At correct chunk size, use this one. Base Case
        if (size <= targetSize) {
            allChunks.add(this)

        // Not at correct size, keep going
        } else {
            if (subChunks.size == 0) {
                generateSubChunk()
            }

            // Add sub-chunks sub-chunks
            for (chunk in subChunks) {
                for (subChunk in chunk.getChunksCulled(targetSize, cam, graphics)) {
                    allChunks.add(subChunk)
                }
            }
        }
        return allChunks
    }

    /**
     * Draw a debug box around the chunk.
     *
     * @param render    The ShapeRenderer to add the box to.
     */
    fun debugBBox(render: ShapeRenderer) {
        if (didCull) return
        render.color = if (didUseSubChunk) Color.GREEN else Color.BLUE
        render.box(bbox.min.x, bbox.min.y, bbox.max.z, bbox.width, bbox.height, bbox.depth)
        // TODO draw bbox of buildings
    }

    fun getBuildingCache(): ModelCache? {
        GCBuildingCache.retrieve(Pair(Vector2(x, z), Vector2(x + size, z + size))) {
            this.modelCache = it
        }
        return modelCache
    }

    /**
     * Generate the 4 sub-chunks that make up this chunk
     */
    private fun generateSubChunk() {
        val subSize = size / 2
        for (i in 0..2) {
            for (j in 0..2) {
                subChunks.add(BuildingChunk(x + subSize * j, z + subSize * i, subSize))
            }
        }
    }
}
