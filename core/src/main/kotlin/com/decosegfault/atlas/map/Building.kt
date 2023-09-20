package com.decosegfault.atlas.map

import com.badlogic.gdx.math.DelaunayTriangulator
import com.badlogic.gdx.math.EarClippingTriangulator
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
    // we were using Delaunay triangulation before but there appears to be an infinite loop
    // on Westfield Chermside lmao (OSM ID 10072620)
    private val triangulator = EarClippingTriangulator()
//    private val triangulator = DelaunayTriangulator()

    /** Triangulates the polygon [polygon] in this building */
    fun triangulate(): List<Triangle> {
        val indices = triangulator.computeTriangles(polygon.transformedVertices, 0, polygon.transformedVertices.size)
        val out = mutableListOf<Triangle>()

        var i = 0
        while (i < indices.size) {
            // retrieve vertices, this tells us where to start looking for coordinates in polygon.vertices
            val i1 = indices[i + 0]
            val i2 = indices[i + 1]
            val i3 = indices[i + 2]

            // construct points from vertices (x is +0, y is +1)
            val v1 = Vector2(polygon.transformedVertices[i1 + 0], polygon.transformedVertices[i1 + 1])
            val v2 = Vector2(polygon.transformedVertices[i2 + 0], polygon.transformedVertices[i2 + 1])
            val v3 = Vector2(polygon.transformedVertices[i3 + 0], polygon.transformedVertices[i3 + 1])
            val triangle = Triangle(v1, v2, v3)
            out.add(triangle)

            i += 3
        }
        return out
    }

    /** Translates the polygon floor plan to be about the origin by first calculating the centroid */
    fun normalise() {
        val centroid = polygon.getCentroid(Vector2())
        polygon.translate(-centroid.x, -centroid.y)
        // attempt to force an update of transformed vertices
        polygon.transformedVertices
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
