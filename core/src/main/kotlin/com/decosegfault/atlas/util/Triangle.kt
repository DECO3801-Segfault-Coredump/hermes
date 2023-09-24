package com.decosegfault.atlas.util

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

/** @author Matt Young */
data class Triangle(
    val v1: Vector2,
    val v2: Vector2,
    val v3: Vector2
) {
    fun centroid(): Vector2 {
        return Vector2((v1.x + v2.x + v3.x) / 3f, (v1.y + v2.y + v3.y) / 3f)
    }

    /** Assumes this triangle is in Atlas coords, returns the WKT string in WGS84 lat/long coords */
    fun dumpAsWgs84Wkt(): String {
        val v1W = AtlasUtils.atlasToLatLong(v1)
        val v2W = AtlasUtils.atlasToLatLong(v2)
        val v3W = AtlasUtils.atlasToLatLong(v3)

        // NOTE: WKT is long/lat, not lat/long
        return "POLYGON ((${v1W.y} ${v1W.x}, ${v2W.y} ${v2W.x}, ${v3W.y} ${v3W.x}))"
    }

    /**
     * Assuming this triangle lies flat on the ground, this method extrudes it upwards from a 2D triangle
     * into a 3D prism shape. The shape will be inserted into the MeshPartBuilder [mpb].
     *
     * Based on: https://lindenreidblog.com/2017/11/05/procedural-mesh-extrusion-tutorial/
     *
     * @param height metres to extrude up
     * @return 6 vertices that form the prism shape, can be sent off to a model builder directly
     */
    fun extrudeUpToPrismMesh(mpb: MeshPartBuilder, height: Float) {
        // calculate 3D positions of the base (v1, v2, v3 in 3d)
        val i1V = Vector3(v1.x, 0f, v1.y)
        val i2V = Vector3(v2.x, 0f, v2.y)
        val i3V = Vector3(v3.x, 0f, v3.y)

        // generate new vertices of the top: $n_x = v_x + (normal * height)$ (v4, v5, v6)
        // since we are on the ground, normal is easily calculated as just Vector3.Y (up)
        val up = Vector3.Y.cpy().scl(height)
        val n1V = i1V.cpy().add(up)
        val n2V = i2V.cpy().add(up)
        val n3V = i3V.cpy().add(up)

        // generate vertices as per the article
        mpb.ensureVertices(7)
        mpb.ensureTriangleIndices(7)
        val i1 = mpb.vertex(i1V, Vector3.Y, null, null)
        val i2 = mpb.vertex(i2V, Vector3.Y, null, null)
        val i3 = mpb.vertex(i3V, Vector3.Y, null, null)
        val n1 = mpb.vertex(n1V, Vector3.Y, null, null)
        val n2 = mpb.vertex(n2V, Vector3.Y, null, null)
        val n3 = mpb.vertex(n3V, Vector3.Y, null, null)

        // these are all the vertices from the blog
        // this actually seems all correct except for the roof
        // side note though it allows you to see how the triangles are constructed!
        mpb.triangle(n1, n3, n2)
        mpb.triangle(n1, i1, n2)
        mpb.triangle(n2, i1, i2)
        mpb.triangle(n3, n2, i2)
        mpb.triangle(n3, i2, i3)
        mpb.triangle(n3, i3, i1)
        mpb.triangle(n3, i1, n1)

        // top v2 (this is just top but with CW vertex ordering rather than CCW)
        // if you remove this, you can see through the roof to debug the building triangulation!
        mpb.triangle(n2, n3, n1)

        // it works!!!!!!!
    }
}
