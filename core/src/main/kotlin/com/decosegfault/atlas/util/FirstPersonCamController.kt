/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas.util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3

/**
 * This is a port of ThreeJS PointerLockControls:
 *
 * Sources:
 * - https://github.com/mrdoob/three.js/blob/dev/examples/jsm/controls/PointerLockControls.js
 * - https://sbcode.net/threejs/pointerlock-controls/
 *
 * @author threeJS
 * @author Matt Young (libGDX+Kotlin port, simplifications)
 */
class FirstPersonCamController(private val cam: PerspectiveCamera) : InputAdapter() {
    private val minPitch = -89f // deg
    private val maxPitch = 89f // deg
    private val pointerSpeed = 1.6f
    private val speed = 300f // units per second
    private val boostFactor = 4f
    private val minHeight = 1f
    private val maxHeight = 8000f
    val quat = Quaternion()
    var enabled = true

    init {
        Gdx.input.isCursorCatched = System.getProperty("nolockmouse") == null
        quat.setFromMatrix(cam.view)
    }

    private fun moveForward(distance: Float) {
        val delta = cam.direction.cpy().crs(cam.up).nor().rotate(Vector3.Y, 90f).scl(distance)
        cam.position.add(delta)
    }

    private fun moveRight(distance: Float) {
        // https://gamedev.stackexchange.com/q/26622
        val delta = cam.direction.cpy().crs(cam.up).nor().scl(distance)
        cam.position.add(delta)
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        if (!enabled) return false

        val movementX = Gdx.input.deltaX.toFloat()
        val movementY = Gdx.input.deltaY.toFloat()

        var pitch = quat.pitch
        val roll = quat.roll
        var yaw = quat.yaw

        pitch += -movementY * 0.1f * pointerSpeed
        yaw += -movementX * 0.1f * pointerSpeed

        pitch = pitch.coerceIn(minPitch, maxPitch)

        quat.setEulerAngles(yaw, pitch, roll)

        // we want to SET the rotation of the camera, so first reset direction and up
        cam.direction.set(0f, 0f, -1f)
        cam.up.set(0f, 1f, 0f)

        // now we can actually apply the rotation
        cam.rotate(quat)
        cam.normalizeUp()
        cam.update()

        return true
    }

    fun update(delta: Float) {
        if (!enabled) return
        val heightFactor = cam.position.y / 128f
        val actualSpeed = if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) speed * heightFactor * boostFactor else speed * heightFactor

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            moveForward(actualSpeed * delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            moveForward(-actualSpeed * delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            moveRight(-actualSpeed * delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            moveRight(actualSpeed * delta)
        }
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            cam.position.y -= actualSpeed * delta
        }
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            cam.position.y += actualSpeed * delta
        }

        cam.position.y = cam.position.y.coerceIn(minHeight, maxHeight)
        cam.update()
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        if (!enabled) return false
        cam.fieldOfView += amountY
        cam.update()
        return true
    }
}
