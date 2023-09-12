package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.math.Vector3
import com.decosegfault.atlas.util.AbstractGarbageCollectedCache

/** **Soft** limit of buildings */
private const val MAX_BUILDINGS_RAM = 2048.0

/** If the cache is this% full, start trying to evict items */
private const val START_GC_THRESHOLD = 0.90

/** If the cache is below this% full, stop evicting */
private const val END_GC_THRESHOLD = 0.50

/**
 * Implementation of [AbstractGarbageCollectedCache] used to store buildings
 *
 * @author Matt Young
 */
object GCBuildingCache : AbstractGarbageCollectedCache<Vector3, ModelCache>(
    "GCBuildingCache",
    MAX_BUILDINGS_RAM,
    START_GC_THRESHOLD,
    END_GC_THRESHOLD,
    Runtime.getRuntime().availableProcessors()
) {
    override fun newItem(key: Vector3): ModelCache {
        return BuildingGenerator.generateBuildingChunk(key)
    }
}
