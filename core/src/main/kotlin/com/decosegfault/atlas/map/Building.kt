package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.decosegfault.atlas.util.AtlasUtils
import com.decosegfault.atlas.util.Triangle
import eu.mihosoft.jcsg.Extrude
import eu.mihosoft.jcsg.Vertex
import eu.mihosoft.vvecmath.Vector3d
import org.tinylog.kotlin.Logger
import kotlin.IllegalStateException

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
    // is in Atlas coords?
    private var isAtlas = false

    // limit the height of buildings in case of errors, and if a height is missing give a default
    // height of 1 floor
    val height = MathUtils.clamp(floors.toFloat(), 1f, MAX_STOREYS) * STOREY_HEIGHT

    /** Converts WGS84 lat/long vertex coords (in that order) to Atlas */
    fun toAtlas() {
        if (isAtlas) throw IllegalStateException("Already in Atlas coords")

        for (i in 0 until polygon.vertexCount) {
            val vertex = polygon.getVertex(i, Vector2())
            // at this point, we were already transformed into lat/long, we just need to convert to Atlas coords
            val atlasCoord = AtlasUtils.latLongToAtlas(vertex)
            polygon.setVertex(i, atlasCoord.x, atlasCoord.y)
        }

        isAtlas = true
    }

    /** Extrudes this building into a 3D model using CSG */
    fun extrudeToModelCSG(mpb: MeshPartBuilder) {
        if (!isAtlas) throw IllegalStateException("Must be in Atlas coords")

        val csgPoints = mutableListOf<Vector3d>()
        for (i in 0 until polygon.vertexCount) {
            val vertex = polygon.getVertex(i, Vector2())
            val point = Vector3d.xy(vertex.x.toDouble(), vertex.y.toDouble())
            if (point !in csgPoints) {
                csgPoints.add(point)
            }
        }
        Logger.info("Have ${csgPoints.size} points")
        Logger.info(csgPoints.joinToString(","))

        val csg = Extrude.points(Vector3d.z(height.toDouble()), csgPoints)
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
        /** Approximate height in metres of one storey. Source: https://en.wikipedia.org/wiki/Storey#Overview */
        private const val STOREY_HEIGHT = 4.3f * 3f

        /**
         * The tallest building in Brisbane is currently the Brisbane Skytower which is 90 storeys tall.
         * We clamp building storeys to this many in case of OSM data misinputs so they're not mega tall.
         */
        private val MAX_STOREYS = 90f
    }
}
