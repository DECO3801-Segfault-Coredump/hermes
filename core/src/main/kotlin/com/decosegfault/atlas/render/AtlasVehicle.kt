package com.decosegfault.atlas.render

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Ray
import com.decosegfault.atlas.util.Assets
import com.decosegfault.atlas.util.AtlasUtils
import com.decosegfault.hermes.types.VehicleType
import net.mgsx.gltf.scene3d.scene.SceneAsset
import org.tinylog.kotlin.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Atlas's representation of a vehicle, includes gdx-gltf high detail and low detail models and bounding box.
 *
 * @author Matt Young
 * @author Henry Batt
 * @param modelHigh high poly 3D model
 * @param modelLow low poly 3D model
 */
class AtlasVehicle(private val modelHigh: SceneAsset, private val modelLow: SceneAsset, val name: String = "") {
    /** actual transform of the vehicle shared between model instances */
    val transform = Matrix4()

    /** original bbox for the model itself */
    private val bboxOrig = BoundingBox()

    /** transformed bbox for current model */
    val bbox = BoundingBox()

    /** true if the vehicle was culled in the last render pass */
    var didCull = false

    /** true if the vehicle used low LoD model in the last render pass */
    var didUseLowLod = false

    /** If true, force this vehicle to be hidden */
    var hidden = false

    val uuid = UUID.randomUUID()

    private val modelInstanceHigh = ModelInstance(modelHigh.scene.model)
    private val modelInstanceLow = ModelInstance(modelLow.scene.model)

    init {
        // only calculate bbox once, then multiply it with transform (see update())
        // also pull it from cache if we are able, since we only have 3 models

        val maybeBbox = BBOX_CACHE[modelHigh.scene.model]
        if (maybeBbox != null) {
            // we hit the cache
            bboxOrig.set(maybeBbox)
        } else {
            // didn't hit the cache, do the slow calculation
            modelInstanceHigh.calculateBoundingBox(bboxOrig)
            BBOX_CACHE[modelHigh.scene.model] = bboxOrig
            Logger.debug("Calculate initial bounding box for model ${modelHigh.scene.model.hashCode()}")
        }

//        modelInstanceHigh.calculateBoundingBox(bboxOrig)

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

    /**
     * Sets vehicle position from Hermes lat/long coord
     * @param trans new transform in hermes coords: x, y, theta (degrees)
     */
    fun updateTransformFromHermes(transLat: Double, transLong: Double, theta: Double) {
        val atlasPos = AtlasUtils.latLongToAtlas(transLat, transLong, theta)
//        val atlasPos = Vector3(transLat, transLong, theta)
//        Logger.debug("updateTformFromHermes: lat/long $trans atlas: $atlasPos for vehicle $this")
        updateTransform(atlasPos)
    }

    fun addTransform(trans: Vector3) {
        transform.translate(trans.x, 0f, trans.y)
        transform.rotate(Vector3.Z, trans.z)
        update()
    }

    fun debug(render: ShapeRenderer) {
        if (didCull || hidden) return
        render.color = if (didUseLowLod) Color.GREEN else Color.RED
        render.box(bbox.min.x, bbox.min.y, bbox.max.z, bbox.width, bbox.height, bbox.depth)
    }

    fun draw(render: ShapeRenderer, selected: Boolean) {
        if (didCull || hidden) return
        render.color = if (selected) Color.valueOf("#00de30") else Color.valueOf("#0d0063")
        render.box(bbox.min.x, bbox.min.y, bbox.max.z, bbox.width, bbox.height, bbox.depth)
    }

    /** @return The model to render, or null if we should not render this vehicle */
    fun getRenderModel(cam: Camera, graphics: GraphicsPreset): RenderableProvider? {
        didCull = false
        didUseLowLod = false

        if (hidden) {
            didCull = true
            return null
        }

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

    override fun toString(): String {
        return "AtlasVehicle(transform=${transform.getTranslation(Vector3())}, didCull=$didCull, didUseLowLod=$didUseLowLod, hidden=$hidden)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AtlasVehicle

        if (name != other.name) return false
        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        return result
    }

    companion object {
        /** Creates a vehicle for a Hermes [VehicleType] */
        fun createFromHermes(type: VehicleType, name: String): AtlasVehicle {
            val modelName = type.name.lowercase()
            val modelLow = Assets.ASSETS["atlas/${modelName}_low.glb", SceneAsset::class.java]
            val modelHigh = Assets.ASSETS["atlas/${modelName}_high.glb", SceneAsset::class.java]
            val vehicle = AtlasVehicle(modelHigh, modelLow, name)
            vehicle.updateTransform(Vector3.Zero)
            return vehicle
        }

        private val BBOX_CACHE = ConcurrentHashMap<Model, BoundingBox>()
    }
}
