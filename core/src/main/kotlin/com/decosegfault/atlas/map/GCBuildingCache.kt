package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.math.Vector2
import com.decosegfault.atlas.util.AbstractGarbageCollectedCache
import org.tinylog.kotlin.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * **Soft** limit of buildings.
 * HACK: Disposing building meshes is too hard, and the chunks are large enough, so we will just never
 * dispose buildings.
 */
private const val MAX_BUILDINGS_RAM = Double.MAX_VALUE

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
object GCBuildingCache : AbstractGarbageCollectedCache<Pair<Vector2, Vector2>, ModelCache>(
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
    private val buildingAssignments = ConcurrentHashMap<Pair<Vector2, Vector2>, Set<Building>>()
    // note if the above doesn't work we can always try using the synchronized keyword instead

    /**
     * Thread local building generator. Due to PostgresSQL connections, we can't share building generator
     * across threads as it crashes and would also reduce performance.
     */
    private val buildingGenerator = ThreadLocal.withInitial { BuildingGenerator() }

    override fun newItem(key: Pair<Vector2, Vector2>): ModelCache {
        val buildings = buildingGenerator.get().getBuildingsInBounds(key.first, key.second)
        val origSize = buildings.size

        // remove buildings that have been "bagsed" by other threads
        val allOtherBuildings = buildingAssignments.values.flatten().toHashSet()
        buildings.removeAll(allOtherBuildings)
        Logger.debug("Removed ${origSize - buildings.size} buildings, processing ${buildings.size}")

        // now we have the actual list of buildings to process on this thread, register and process them
        buildingAssignments[key] = buildings
        return buildingGenerator.get().generateBuildingChunk(buildings)
    }

    override fun evict(key: Pair<Vector2, Vector2>) {
        super.evict(key)
        // remove all buildings associated with this chunk
        buildingAssignments.remove(key)
    }
}
