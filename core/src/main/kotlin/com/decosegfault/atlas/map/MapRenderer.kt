package com.decosegfault.atlas.map

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.*
import com.decosegfault.atlas.render.GraphicsPreset
import com.decosegfault.atlas.util.AtlasUtils

/**
 * Renders OpenStreetMap map tiles as a 2D plane in a 3D world
 *
 * @author Matt Young
 */
class MapRenderer(private val cam: Camera, private val graphics: GraphicsPreset) {
    // ground plane: normal is pointing upwards, and origin is on the plane
    private val groundPlane = Plane(Vector3(0f, 1f, 0f), Vector3.Zero)

    // intersection points on the ground plane for where the camera intersection occurred
    // ORDER: top left, top right, bottom left, bottom right
    private val intersections = listOf(Vector3(), Vector3(), Vector3(), Vector3())

    fun update() {
        // clear existing data
        for (i in intersections) {
            i.setZero()
        }

        // first, calculate the maximum possible rectangle on the plane (2D) based on our tile draw distance
        // just consider X and Z position of the camera, so ignore height (since Y is our height)
        val camPos2D = Vector2(cam.position.x, cam.position.z)
        val half = graphics.tileDrawDist / 2f
        val tileBounds = Rectangle(camPos2D.x - half, camPos2D.y + half, graphics.tileDrawDist, graphics.tileDrawDist)

        // get rays corresponding to the four corners of the camera and determine where they intersect
        // the ground plane
        // it has to be in this order because cam.getPickRay recycles the Ray instance
        val tl = cam.getPickRay(0f, 0f)
        Intersector.intersectRayPlane(tl, groundPlane, intersections[0])

        val tr = cam.getPickRay(Gdx.graphics.width.toFloat(), 0f)
        Intersector.intersectRayPlane(tr, groundPlane, intersections[1])

        val bl = cam.getPickRay(0f, Gdx.graphics.height.toFloat())
        Intersector.intersectRayPlane(bl, groundPlane, intersections[2])

        val br = cam.getPickRay(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        Intersector.intersectRayPlane(br, groundPlane, intersections[3])

        // clamp each position to tile bounds
        for (i in intersections) {
            AtlasUtils.clampToRect3D(i, tileBounds)
        }

        // FIXME our biggest problem now is what to do if a ray doesn't collide
        //  we want to use the tileBounds rect IFF the ray didn't intersect the plane

        // if all points hit, use the rectangle defind by tl, tr, bl, br
        // if >=2 points land, compute minimum bounding rectangle and use that?

        // simpler answer is simply just use GraphicsPreset tile distance unless all points land; or only use
        // tile distance

        // it may be cheaper and easier to simply compute all the tiles in the tile draw distance and then
        // check for each tile if it's in the camera frustum (should be very cheap)
    }

    fun debug(shape: ShapeRenderer) {
        shape.end()

        shape.begin(ShapeRenderer.ShapeType.Filled)
        shape.color = Color.ORANGE
        for (i in intersections) {
            shape.point(i.x, i.y, i.z)
        }
    }
}
