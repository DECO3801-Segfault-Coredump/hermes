package com.decosegfault.atlas.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelCache.TightMeshPool
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.utils.Disposable
import com.decosegfault.atlas.util.AtlasUtils
import com.decosegfault.hermes.VehicleType
import net.mgsx.gltf.scene3d.scene.SceneAsset

/**
 * Atlas's representation of a vehicle, includes gdx-gltf high detail and low detail models and bounding box.
 *
 * @author Matt Young
 * @param modelHigh high poly 3D model
 * @param modelLow low poly 3D model
 */
class AtlasVehicle(private val modelHigh: SceneAsset, private val modelLow: SceneAsset, val vehicleType: VehicleType) {
    /** actual transform of the vehicle shared between model instances */
    private val transform = Matrix4()
    /** original bbox for the model itself */
    private val bboxOrig = BoundingBox()
    /** transformed bbox for current model */
    private val bbox = BoundingBox()
    /** true if the vehicle was culled in the last render pass */
    var didCull = false
    /** true if the vehicle used low LoD model in the last render pass */
    var didUseLowLod = false

    private val modelInstanceHigh = ModelInstance(modelHigh.scene.model)
    private val modelInstanceLow = ModelInstance(modelLow.scene.model)

    init {
        // only calculate bbox once, then multiply it with transform (see update())
        modelInstanceHigh.calculateBoundingBox(bboxOrig)
    }

    /** Updates LoD transforms according to the shared [transform] */
    private fun update() {
        // translate models
        modelInstanceHigh.transform.set(transform)
        modelInstanceLow.transform.set(transform)

        // update bounding box transform: https://stackoverflow.com/a/20933342/5007892
        bbox.set(bboxOrig)
        bbox.mul(transform)
    }

    /**
     * @param trans new transform: x, y, theta (degrees)
     */
    fun updateTransform(trans: Vector3) {
        transform.setToTranslation(trans.x, 0f, trans.y)
        // note that setToTranslation **zeroes out** the matrix, so we can safely just rotate it now
        // we **don't** call setToRotation, because this would then zero out the matrix again
        transform.rotate(Vector3.Y, trans.z)
        update()
    }

    fun addTransform(trans: Vector3) {
        transform.translate(trans.x, 0f, trans.y)
        transform.rotate(Vector3.Z, trans.z)
        update()
    }

    fun debug(render: ShapeRenderer) {
        if (didCull) return
        render.color = if (didUseLowLod) Color.GREEN else Color.RED
        render.box(bbox.min.x , bbox.min.y, bbox.max.z, bbox.width, bbox.height, bbox.depth)

//        render.end()
//        render.begin(ShapeRenderer.ShapeType.Filled)
//        val centre = bbox.getCenter(Vector3())
//        render.point(centre.x, centre.y, centre.z)
//        val closest = AtlasUtils.closestPoint(cam.position, bbox)
//        render.point(closest.x, closest.y, closest.z)
//        render.end()
    }

    /** @return The model to render, or null if we should not render this vehicle */
    fun getRenderModel(cam: Camera, graphics: GraphicsPreset): RenderableProvider? {
        didCull = false
        didUseLowLod = false

        // first do distance thresholding since it's cheap
        // distance thresholding we compute as distance to the dist from the camera to the closest point
        // on the bounding box
        val closestPoint = AtlasUtils.bboxClosestPoint(cam.position, bbox)
        val dist = cam.position.dst(closestPoint)
        if (dist >= graphics.vehicleDrawDist) {
            didCull = true
            return null
        }

        // now we can do the more expensive frustum culling
        if (!cam.frustum.boundsInFrustum(bbox)) {
            didCull = true
            return null
        }

        // finally do LoDs
        return if (dist >= graphics.vehicleLodDist) {
            didUseLowLod = true
            modelInstanceLow
        } else {
            modelInstanceHigh
        }
    }

    fun intersectRay(ray: Ray): Boolean {
        return Intersector.intersectRayBoundsFast(ray, bbox)
    }
}
