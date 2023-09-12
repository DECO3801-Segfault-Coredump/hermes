package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.decosegfault.atlas.render.GraphicsPreset

/**
 * This class manages fetching and rendering buildings. Basically the building equivalent of
 * AtlasTileManager. For more info see docs/atlas_buildings_design.md
 *
 * @author Matt Young
 */
class BuildingManager {
    fun getBuildingChunksCulled(cam: Camera, graphics: GraphicsPreset): List<ModelCache> {
        return listOf()
    }
}
