package com.decosegfault.atlas.util

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Spherical coordinate system.
 * Source: https://github.com/mrdoob/three.js/blob/dev/src/math/Spherical.js
 *
 * @author threeJS authors
 * @author Matt Young (Kotlin port)
 */
class Spherical(var radius: Float = 1f, var phi: Float = 0f, var theta: Float = 0f) {
    /** restrict phi to be between EPS and PI-EPS */
    fun makeSafe(): Spherical {
        phi = phi.coerceIn(MathUtils.FLOAT_ROUNDING_ERROR, MathUtils.PI - MathUtils.FLOAT_ROUNDING_ERROR)
        return this
    }

    fun setFromVector3(v: Vector3): Spherical {
        radius = v.len()

        if (radius == 0f) {
            theta = 0f
            phi = 0f
        } else {
            theta = MathUtils.atan2(v.x, v.z)
            phi = MathUtils.acos(MathUtils.clamp( v.y / radius, -1f, 1f))
        }

        return this
    }
}
