package com.decosegfault.atlas.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Frustum
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import net.mgsx.gltf.scene3d.scene.Scene

/**
 * Atlas's representation of a vehicle, includes gdx-gltf high detail and low detail models and bounding bo.
 *
 * @author Matt Young
 *
 * @param sceneHigh high poly 3D model
 * @param sceneLow low poly 3D model
 */
data class AtlasVehicle(val sceneHigh: Scene, val sceneLow: Scene,) {
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
        sceneHigh.modelInstance.transform.set(transform)
        sceneLow.modelInstance.transform.set(transform)

        // just calculate bounding box for high and assume it's the same as low for performance reasons
        sceneHigh.modelInstance.calculateBoundingBox(bbox)
    }

    fun intersectFrustum(frustum: Frustum): Boolean {
        return Intersector.intersectFrustumBounds(frustum, bbox)
    }

    fun distanceToCam(camera: Camera): Float {
        return camera.position.dst(transform.getTranslation(Vector3()))
    }
}
