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

    fun set(x: Double, y: Double): HPVector2 {
        this.x = x
        this.y = y
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

    fun scl(scalar: Double): HPVector2 {
        x *= scalar
        y *= scalar
        return this
    }

    fun angleDeg(reference: HPVector2): Double {
        var angle = atan2(reference.crs(this), reference.dot(this)) * MathUtils.radiansToDegrees
        if (angle < 0) angle += 360f
        return angle
    }

    fun angleDeg(): Double {
        var angle = MathUtils.atan2(y.toFloat(), x.toFloat()) * MathUtils.radiansToDegrees
        if (angle < 0) angle += 360f
        return angle.toDouble()
    }
}
