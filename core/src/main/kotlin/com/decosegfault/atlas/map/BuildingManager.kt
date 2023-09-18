package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Vector3
import com.decosegfault.atlas.render.GraphicsPreset
import com.decosegfault.atlas.util.AtlasUtils
import org.tinylog.Logger
import kotlin.math.pow

/**
 * This class manages fetching and rendering buildings. Basically the building equivalent of
 * TileManager. For more info see docs/atlas_buildings_design.md
 *
 * @author Matt Young
 * @author Henry Batt
 */
class BuildingManager {

    private val buildingChunkSurface = mutableListOf<BuildingChunk>()
    var numRetrievedBuildingChunks = 0

    private val CHUNK_ZOOM = 15
    private val CHUNK_SIZE = AtlasUtils.MIN_TILE * 2.0.pow(AtlasUtils.TOTAL_MAX_ZOOM - CHUNK_ZOOM).toFloat()

    init {
        val xShift = AtlasUtils.NUM_X_TILES / 2
        val zShift = AtlasUtils.NUM_Y_TILES / 2
        // Create all the largest chunks
        for (i in -xShift until xShift) {
            for (j in -zShift until zShift) {
                buildingChunkSurface.add(BuildingChunk(i * AtlasUtils.MAX_SIZE, j * AtlasUtils.MAX_SIZE,
                    AtlasUtils.MAX_SIZE))
            }
        }
    }

    fun getBuildingChunksCulled(cam: Camera, graphics: GraphicsPreset): List<BuildingChunk> {
        val chunks = mutableListOf<BuildingChunk>()
        for (buildingChunk in buildingChunkSurface) {
            for (subChunk in buildingChunk.getChunksCulled(CHUNK_SIZE, cam, graphics)) {
                chunks.add(subChunk)
            }
        }
        numRetrievedBuildingChunks = chunks.size
        return chunks
    }
}
