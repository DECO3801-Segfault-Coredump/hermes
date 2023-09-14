package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue
import com.badlogic.gdx.math.DelaunayTriangulator
import com.badlogic.gdx.math.Polygon
import com.decosegfault.atlas.util.Triangle

/**
 * An OpenStreetMap building
 *
 * @author Matt Young
 */
data class Building(
    /** OpenStreetMap ID */
    val osmId: Long,
    /** Footprint polygon */
    val polygon: Polygon,
    /** Number of floors */
    val floors: Int,
) {
    // each instance needs a private triangulator due to memory re-use by the triangulator class
    private val triangulator = DelaunayTriangulator()

    /** Triangulates the polygon [polygon] in this building */
    fun triangulate(): List<Triangle> {
        val indices = triangulator.computeTriangles(polygon.vertices, false)
        for (i in 0 until indices.items.size step 3) {

        }

        return emptyList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Building

        return osmId == other.osmId
    }

    override fun hashCode(): Int {
        return osmId.hashCode()
    }
}
