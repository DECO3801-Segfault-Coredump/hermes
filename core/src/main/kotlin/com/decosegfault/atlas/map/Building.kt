package com.decosegfault.atlas.map

import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.decosegfault.atlas.util.AtlasUtils
import com.decosegfault.atlas.util.Triangle

/**
 * An OpenStreetMap building
 *
 * @author Matt Young
 */
data class Building(
    /** OpenStreetMap ID */
    val osmId: Long,
    /** Footprint polygons */
    val polygons: List<Polygon>,
    /** Number of floors */
    val floors: Int,
) {
    // each instance needs a private triangulator due to memory re-use by the triangulator class
    // we were using Delaunay triangulation before but there appears to be an infinite loop
    // on Westfield Chermside lmao (OSM ID 10072620)
    private val triangulator = EarClippingTriangulator()

    private var isAtlas = false

    /** Triangulates the polygons in this building. Returns a list of triangles for each polygon (so a list of lists). */
    fun triangulate(): List<List<Triangle>> {
        if (!isAtlas) throw IllegalStateException("Must be in Atlas coords")
        val out = mutableListOf<List<Triangle>>()

        for (polygon in polygons) {
            val indices = triangulator.computeTriangles(
                polygon.transformedVertices,
                0,
                polygon.transformedVertices.size
            )
            val polyOut = mutableListOf<Triangle>()

            var i = 0
            while (i < indices.size) {
                // retrieve vertices, this tells us where to start looking for coordinates in polygon.vertices
                val i1 = indices[i + 0]
                val i2 = indices[i + 1]
                val i3 = indices[i + 2]

                // construct points from vertices (x is +0, y is +1)
                val v1 = polygon.getVertex(i1.toInt(), Vector2())
                val v2 = polygon.getVertex(i2.toInt(), Vector2())
                val v3 = polygon.getVertex(i3.toInt(), Vector2())
                val triangle = Triangle(v1, v2, v3)
                polyOut.add(triangle)

                i += 3
            }
            out.add(polyOut)
        }
        return out
    }

    /** Converts WGS84 lat/long vertex coords (in that order) to Atlas */
    fun toAtlas() {
        if (isAtlas) throw IllegalStateException("Already in Atlas coords")

        for (polygon in polygons) {
            for (i in 0 until polygon.vertexCount) {
                val vertex = polygon.getVertex(i, Vector2())
                // at this point, we were already transformed into lat/long, we just need to convert to Atlas coords
                val atlasCoord = AtlasUtils.latLongToAtlas(vertex)
                polygon.setVertex(i, atlasCoord.x, atlasCoord.y)
            }
        }

        isAtlas = true
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
}
