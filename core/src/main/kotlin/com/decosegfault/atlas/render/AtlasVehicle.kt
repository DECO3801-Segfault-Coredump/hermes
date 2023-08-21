package com.decosegfault.atlas.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.decosegfault.atlas.util.AtlasUtils

/**
 * Atlas's representation of a vehicle, includes gdx-gltf high detail and low detail models and bounding box.
 *
 * @author Matt Young
 * @param modelHigh high poly 3D model
 * @param sceneLow low poly 3D model
 */
data class AtlasVehicle(val modelHigh: ModelInstance, val modelLow: ModelInstance) {
    private val transform = Matrix4()
    private val bbox = BoundingBox()

    /**
     * @param trans new transform: x, y, theta (rad)
     */
    fun updateTransform(trans: Vector3) {
        // update shared transformation for the model
        transform.setToTranslation(trans.x, 0.0f, trans.z) // TODO check axes
        transform.setToRotation(Vector3.Z, 0.0f) // TODO check axes

        // translate models
        modelHigh.transform.set(transform)
        modelLow.transform.set(transform)

        // just calculate bounding box for high and assume it's the same as low for performance reasons
        modelHigh.calculateBoundingBox(bbox)
    }

    fun debug(render: ShapeRenderer, cam: Camera) {
        // TODO change colour if we culled or did not cull
        render.color = Color.RED
        render.box(bbox.min.x , bbox.min.y, bbox.max.z,
            bbox.max.x - bbox.min.x, bbox.max.y - bbox.min.y, bbox.max.z - bbox.min.z)

//        render.end()
//        render.begin(ShapeRenderer.ShapeType.Filled)
//        val centre = bbox.getCenter(Vector3())
//        render.point(centre.x, centre.y, centre.z)
////        render.point(bbox.min.x, bbox.min.y, bbox.min.z)
////        render.point(bbox.max.x, bbox.max.x, bbox.max.z)
//        val closest = AtlasUtils.closestPoint(cam.position, bbox)
//        render.point(closest.x, closest.y, closest.z)
//        render.end()
    }

    /** @return The model to render, or null if we should not render this vehicle */
    fun getRenderModel(cam: Camera, graphics: GraphicsPreset): ModelInstance? {
        // first do distance thresholding since it's cheap
        // distance thresholding we compute as distance to the dist from the camera to the closest point
        // on the bounding box
//        val bboxMinDist = cam.position.dst(bbox.min)
//        val bboxMaxDist = cam.position.dst(bbox.max)
//        val bboxCentroid = cam.position.dst(bbox.getCenter(Vector3()))
        val closestPoint = AtlasUtils.closestPoint(cam.position, bbox)
        val dist = cam.position.dst(closestPoint)
        if (dist >= graphics.vehicleDrawDist) {
            return null
        }

        // now do the more expensive frustum culling
        if (!cam.frustum.boundsInFrustum(bbox)) {
            return null
        }

        // finally do LoDs
        return if (dist >= graphics.vehicleLodDist) {
            modelLow
        } else {
            modelHigh
        }
    }
}
