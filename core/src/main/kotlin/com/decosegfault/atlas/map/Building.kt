package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.particles.values.MeshSpawnShapeValue
import com.badlogic.gdx.math.DelaunayTriangulator
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.decosegfault.atlas.util.Triangle
import org.tinylog.kotlin.Logger

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
        val out = mutableListOf<Triangle>()
//        Logger.debug("Have ${indices.size} indices (${polygon.vertices.size} verts)")

        var i = 0
        while (i < indices.size) {
//            Logger.debug("Requesting ${i + 0}, ${i + 1}, ${i + 2}")
            // retrieve vertices, this tells us where to start looking for coordinates in polygon.vertices
            val i1 = indices[i + 0]
            val i2 = indices[i + 1]
            val i3 = indices[i + 2]

            // construct points from vertices (x is +0, y is +1)
            val v1 = Vector2(polygon.vertices[i1 + 0], polygon.vertices[i1 + 1])
            val v2 = Vector2(polygon.vertices[i2 + 0], polygon.vertices[i2 + 1])
            val v3 = Vector2(polygon.vertices[i3 + 0], polygon.vertices[i3 + 1])
            val triangle = Triangle(v1, v2, v3)
            out.add(triangle)

            i += 3
        }
        return out
    }

    // compare and hashcode both use just the osmId, which should be unique, for performance
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Building

        return osmId == other.osmId
    }

    override fun hashCode(): Int {
        return osmId.hashCode()
    }

    companion object {
        /**
         * Some buildings seem to triangulate incorrectly and produce too many vertices. If the triangulation
         * is above this threshold, the building is skipped.
         */
        private val MAX_VERTS = 1024
    }
}
