package com.decosegfault.atlas.util

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

/**
 * A 2D triangle geometry which can be extruded into 3D.
 * @author Matt Young
 */
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

    private fun calcUV(pos: Vector3): Vector2 {
        // https://stackoverflow.com/a/34960116
        val u = MathUtils.atan2(pos.x, pos.z) / MathUtils.PI * 0.5f + 0.5f
        val v = pos.y / 1000f
        return Vector2(u, v)
    }

    /**
     * @see [extrudeUpToPrismMesh]
     */
    fun extrudeUpToPrismMesh(mpb: MeshPartBuilder, height: Float) {
        // UVs computed in UVTexturingScreen
        val uvs: Array<Vector2?> = arrayOf(
            Vector2(1.0f, 1.0f),
            Vector2(0.62f, 1.0f),
            Vector2(1.0f, 0.0f),
            Vector2(0.78f, 1.0f),
            Vector2(0.0f, 1.0f),
            Vector2(0.78f, 0.0f),
            Vector2(1.0f, 0.0f),
        )
        extrudeUpToPrismMesh(mpb, height, uvs)
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
    fun extrudeUpToPrismMesh(mpb: MeshPartBuilder, height: Float, uvs: Array<Vector2?>) {
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
        mpb.ensureVertices(14)
        mpb.ensureTriangleIndices(14)
        // These UV coordinates were generated by exporting to blender and using Smart UV unwrap in there
        // This is a list of functions that return vertices
        val i1 = { mpb.vertex(i1V, null, null, uvs[0]) }
        val i2 = { mpb.vertex(i2V, null, null, uvs[1]) }
        val i3 = { mpb.vertex(i3V, null, null, uvs[2]) }
        val n1 = { mpb.vertex(n1V, null, null, uvs[3]) }
        val n2 = { mpb.vertex(n2V, null, null, uvs[4]) }
        val n3 = { mpb.vertex(n3V, null, null, uvs[5]) }

        // these are all the vertices from the blog
        // this actually seems all correct except for the roof
        // side note though it allows you to see how the triangles are constructed!
        // This is a list of functions that calculate vertices, which we can then call later
        val vertexFuncs = mutableListOf<Array<() -> Short>>()
        vertexFuncs.add(arrayOf(n1, n3, n2))
        vertexFuncs.add(arrayOf(n1, i1, n2))
        vertexFuncs.add(arrayOf(n2, i1, i2))
        vertexFuncs.add(arrayOf(n3, n2, i2))
        vertexFuncs.add(arrayOf(n3, i2, i3))
        vertexFuncs.add(arrayOf(n3, i3, i1))
        vertexFuncs.add(arrayOf(n3, i1, n1))

        // Go through each triangle, add forward then backwards
        // This makes it so internal faces are not culled by backface culling
        for (func in vertexFuncs) {
            mpb.triangle(func[0](), func[1](), func[2]())
            mpb.triangle(func[2](), func[1](), func[0]())
        }

        // it works!!!!!!!
    }
}
