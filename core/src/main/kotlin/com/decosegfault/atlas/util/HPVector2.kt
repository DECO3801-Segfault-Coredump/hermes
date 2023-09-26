package com.decosegfault.atlas.util

import com.badlogic.gdx.math.MathUtils
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * High precision version of Vector2 intended for WGS84 lat long coordinates
 * @author Matt Young
 */
data class HPVector2(
    var x: Double,
    var y: Double
) {
    fun add(v: HPVector2): HPVector2 {
        x += v.x
        y += v.y
        return this
    }

    fun sub(v: HPVector2): HPVector2 {
        x -= v.x
        y -= v.y
        return this
    }

    fun dst(other: HPVector2): Double {
        val x_d = other.x - x
        val y_d = other.y - y
        return sqrt((x_d * x_d + y_d * y_d))
    }

    fun crs(v: HPVector2): Double {
        return x * v.y - y * v.x
    }

    fun dot(v: HPVector2): Double {
        return x * v.x + y * v.y
    }

    fun angleDeg(reference: HPVector2): Double {
        var angle = atan2(reference.crs(this), reference.dot(this)) * MathUtils.radiansToDegrees
        if (angle < 0) angle += 360f
        return angle
    }
}
