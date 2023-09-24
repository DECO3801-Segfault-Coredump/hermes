package com.decosegfault.atlas.util

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

/**
 * This is a port of ThreeJS OrbitControls.
 *
 * Source: https://github.com/mrdoob/three.js/blob/dev/examples/jsm/controls/OrbitControls.js
 */
class OrbitCamController : InputAdapter() {
    val target = Vector3()

    val minDistance = 5f
    val maxDistance = 5000f

    val minPolarAngle = 0f
    val maxPolarAngle = MathUtils.PI
}
