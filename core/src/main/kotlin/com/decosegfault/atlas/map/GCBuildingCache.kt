package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.decosegfault.atlas.util.AbstractGarbageCollectedCache
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/** **Soft** limit of buildings */
private const val MAX_BUILDINGS_RAM = 2048.0

/** If the cache is this% full, start trying to evict items */
private const val START_GC_THRESHOLD = 0.90

/** If the cache is below this% full, stop evicting */
private const val END_GC_THRESHOLD = 0.50

/**
 * Implementation of [AbstractGarbageCollectedCache] used to store and generate buildings. Users of this
 * class can request the generation of a set of building models for a particular _chunk_, which will be
 * packaged together and returned as a [ModelCache].
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
    /**
     * This set stores a list of buildings that have been reserved by other chunks. This is used to implement
     * the "chunk bags a building" behaviour described in atlas_buildings_design.md, and is used to ensure that
     * we don't have duplicate buildings in the world.
     */
    private val buildingAssignments = ConcurrentHashMap<Vector3, List<Building>>()
    // note if the above doesn't work we can always try using the synchronized keyword instead

    override fun newItem(key: Vector3): ModelCache {
        val buildings = BuildingGenerator.getBuildingsInBounds(Vector2(), Vector2())

        // remove buildings that have been "bagsed" by other threads
        val allOtherBuildings = buildingAssignments.values.flatten().toHashSet()
        buildings.removeAll(allOtherBuildings)

        // now we have the actual list of buildings to process on this thread, register them
        buildingAssignments[key] = buildings.toList()

        throw NotImplementedError()
    }

    override fun evict(key: Vector3) {
        super.evict(key)
        // remove all buildings associated with this chunk
        buildingAssignments.remove(key)
    }
}