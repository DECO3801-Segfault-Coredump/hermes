package com.decosegfault.atlas.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import kotlin.math.pow

/**
 * New camera controller for Atlas, based on streets-gl:
 * [https://github.com/StrandedKitty/streets-gl/blob/dev/src/app/controls/GroundControlsNavigator.ts] (MIT licence)
 *
 * STATUS: GIVEN UP, because kitty does their matrix maths differently which makes it too hard
 *
 * @author StrandedKitty
 * @author Matt Young (Kotlin port)
 */
class StreetsGLCamController(private val cam: PerspectiveCamera) : InputAdapter() {
    val target = Vector3();
    private var direction = Vector3();
    var distance = 0f
    private var distanceTarget = 0f
    var pitch = toRad(45f);
    var yaw = 0f
    private var isLMBDown = false;
    private var isRMBDown = false;
    private var LMBDownPosition: Vector2? = null;
    private var lastLMBMoveEvent: Vector2? = null;
    private var forwardKeyPressed = false
    private var leftKeyPressed = false
    private var rightKeyPressed = false
    private var backwardKeyPressed = false
    private var fastMovementKeyPressed = false
    private var pitchMinusKeyPressed = false
    private var pitchPlusKeyPressed = false
    private var yawMinusKeyPressed = false
    private var yawPlusKeyPressed = false
    private val pointerPosition = Vector2()
    private val camMatrix = Matrix4()
    private val camMatrixWorld = Matrix4()
    private val camRotation = Vector3()

    // https://github.com/StrandedKitty/streets-gl/blob/dev/src/lib/math/MathUtils.ts

    private fun polarToCartesian(azimuth: Float, altitude: Float): Vector3 {
        return Vector3(
            MathUtils.cos(altitude) * MathUtils.cos(azimuth),
            MathUtils.sin(altitude),
            MathUtils.cos(altitude) * MathUtils.sin(azimuth)
        )
    }

    private fun normalizeAngle(angle: Float): Float {
        val newAngle = angle % 2f * MathUtils.PI
        return if (newAngle >= 0f) {
            angle
        } else {
            angle + 2f * MathUtils.PI
        }
    }

    private fun toRad(deg: Float): Float {
        return deg * MathUtils.degreesToRadians
    }

    // https://github.com/StrandedKitty/streets-gl/blob/dev/src/lib/core/Object3D.ts#L23

    private fun updateMatrix() {
        camMatrix.idt()
        camMatrix.translate(cam.position)
        // ignore scale
        camMatrix.rotate(Vector3.X, camRotation.x)
        camMatrix.rotate(Vector3.Y, camRotation.y)
        camMatrix.rotate(Vector3.Z, camRotation.z)

        // according to https://github.com/StrandedKitty/streets-gl/blob/dev/src/lib/core/Object3D.ts#L50
        // if parent == null
    }

    // https://github.com/StrandedKitty/streets-gl/blob/dev/src/lib/math/Vec3.ts

    private fun unproject(v: Vector3, cam: PerspectiveCamera, useWorldMatrix: Boolean = true): Vector3 {
        val cameraSpace = v.cpy().prj(cam.invProjectionView)
//        return v.cpy().prj(cameraSpace, if (useWorldMatrix) )
        return Vector3.Zero
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == 0) {
            isLMBDown = true
            LMBDownPosition = projectOnGround(screenX.toFloat(), screenY.toFloat())
        } else if (button == 2) {
            isRMBDown = true
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == 0) {
            isLMBDown = false
            LMBDownPosition = null
            lastLMBMoveEvent = null
        } else if (button == 2) {
            isRMBDown = false
        }
        return true
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        pointerPosition.set(screenX / Gdx.graphics.width * 2f - 1f, -screenY / Gdx.graphics.height * 2f + 1f)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (isRMBDown) {
            yaw += toRad(Gdx.input.deltaX.toFloat()) * 0.25f
            pitch += toRad(Gdx.input.deltaY.toFloat()) * 0.25f
        }
        if (isLMBDown) {
            lastLMBMoveEvent = Vector2(screenX.toFloat(), screenY.toFloat())
        }
        return true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        val zoomSpeed = CAMERA_ZOOM_SPEED
        val logSpaceDistance = MathUtils.log2(distanceTarget)
        val newLogSpaceDistance = logSpaceDistance + amountY * zoomSpeed

        distanceTarget = MathUtils.clamp(newLogSpaceDistance.pow(2.0f), MIN_CAMERA_DISTANCE, Float.MAX_VALUE)
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Keys.W -> forwardKeyPressed = true
            Keys.A -> leftKeyPressed = true
            Keys.S -> backwardKeyPressed = true
            Keys.D -> rightKeyPressed = true
            Keys.SHIFT_LEFT -> fastMovementKeyPressed = true
            Keys.Q -> yawMinusKeyPressed = true
            Keys.E -> yawPlusKeyPressed = true
            Keys.R -> pitchMinusKeyPressed = true
            Keys.F -> pitchPlusKeyPressed = true
        }
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        when (keycode) {
            Keys.W -> forwardKeyPressed = false
            Keys.A -> leftKeyPressed = false
            Keys.S -> backwardKeyPressed = false
            Keys.D -> rightKeyPressed = false
            Keys.SHIFT_LEFT -> fastMovementKeyPressed = false
            Keys.Q -> yawMinusKeyPressed = false
            Keys.E -> yawPlusKeyPressed = false
            Keys.R -> pitchMinusKeyPressed = false
            Keys.F -> pitchPlusKeyPressed = false
        }
        return true
    }

    private fun moveTarget(dx: Float, dy: Float) {
        val v = Vector2(dx, dy)
        val v2 = Vector2()

        val sin = MathUtils.sin(yaw)
        val cos = MathUtils.cos(yaw)

        v2.x = (cos * v.x) - (sin * v.y);
        v2.y = (sin * v.x) + (cos * v.y);

        target.x += v.x;
        target.z += v.y;
    }

    private fun projectOnGround(clientX: Float, clientY: Float): Vector2 {
        val vector = cam.unproject(Vector3(clientX, clientY, 0f)).sub(cam.position)

        val distanceToGround = (cam.position.y - target.y) / vector.y
        // vectorToGround
        vector.scl(distanceToGround)
        // positionOnGround
        vector.sub(cam.position)

        return Vector2(vector.x, vector.z)
    }

    private fun projectOnGroundPredict(x: Float, y: Float, distance: Float): Vector2 {
        // TODO oldMatrix ?

        val cameraOffset = direction.cpy().scl(-distance)
        val cameraPosition = target.cpy().add(cameraOffset)
        cam.position.set(cameraPosition)
//        cam.lookAt(target, false)
        // TODO
        return Vector2.Zero.cpy()
    }

    private fun updateDistance(delta: Float) {
        val alpha = 1f - (1f - 0.3f).pow(delta / (1f / 60f))

        val oldDistance = distance
        var newDistance = MathUtils.lerp(distance, distanceTarget, alpha)

        if (MathUtils.isEqual(newDistance, distanceTarget, 0.001f)) {
            newDistance = distanceTarget
        }

        val oldPosition = projectOnGroundPredict(pointerPosition.x, pointerPosition.y, oldDistance)
        val newPosition = projectOnGroundPredict(pointerPosition.x, pointerPosition.y, newDistance)

        target.x += oldPosition.x - newPosition.x
        target.z += oldPosition.y - newPosition.y
        distance = newDistance
    }

    private fun clampPitchAndYaw() {
        this.pitch = MathUtils.clamp(
            this.pitch,
            MathUtils.degreesToRadians * (MIN_PITCH),
            MathUtils.degreesToRadians * (MAX_PITCH)
        )
        this.yaw = normalizeAngle(this.yaw)
    }

    private fun processMovementByKeys(delta: Float) {
        val forwardDir = cam.direction.cpy().nor()
        // https://gamedev.stackexchange.com/q/26622
        val rightDir = cam.direction.cpy().crs(cam.up).nor()
        val speed = if (fastMovementKeyPressed) CAM_SPEED_FAST else CAM_SPEED

        val movementDelta = Vector3()
        if (forwardKeyPressed) {
            movementDelta.add(forwardDir.cpy().scl(-delta))
        }
        if (backwardKeyPressed) {
            movementDelta.add(forwardDir.cpy().scl(delta))
        }
        if (leftKeyPressed) {
            movementDelta.add(rightDir.cpy().scl(-delta))
        }
        if (rightKeyPressed) {
            movementDelta.add(rightDir.cpy().scl(delta))
        }
        movementDelta.scl(speed)

        this.target.x += movementDelta.x
        this.target.z += movementDelta.y

        if (yawPlusKeyPressed) {
            yaw += delta * YAW_SPEED
        }
        if (yawMinusKeyPressed) {
            yaw -= delta * YAW_SPEED
        }
        if (pitchMinusKeyPressed) {
            pitch -= delta * PITCH_SPEED
        }
        if (pitchPlusKeyPressed) {
            pitch += delta * PITCH_SPEED
        }
    }

    fun update(delta: Float) {
        clampPitchAndYaw()
        direction = polarToCartesian(yaw, -pitch).nor()

        processMovementByKeys(delta)
        updateDistance(delta)

        val cameraOffset = direction.cpy().scl(-distance)
        val cameraPosition = target.cpy().add(cameraOffset)

        cam.position.set(cameraPosition)
        cam.lookAt(target)

        if (lastLMBMoveEvent != null && LMBDownPosition != null) {
            cam.update()

            val positionOnGround = projectOnGround(lastLMBMoveEvent!!.x, lastLMBMoveEvent!!.y)
            val movementDelta = LMBDownPosition!!.cpy().sub(positionOnGround)

            moveTarget(movementDelta.x, movementDelta.y)

            cameraOffset.set(direction.cpy().scl(-distance))
            cameraPosition.set(target.cpy().add(cameraOffset))

            cam.position.set(cameraPosition)
            cam.lookAt(target)
        }

        cam.update()
    }

    companion object {
        private const val CAM_SPEED = 400f
        private const val CAM_SPEED_FAST = 1200f
        private const val YAW_SPEED = 0.8f
        private const val PITCH_SPEED = 0.8f
        private const val MIN_PITCH = 5f
        private const val MAX_PITCH = 89.99f
        private const val CAMERA_ZOOM_SPEED = 0.0005f
        private const val MIN_CAMERA_DISTANCE = 10f
    }
}
