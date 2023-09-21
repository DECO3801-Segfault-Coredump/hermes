package com.decosegfault.atlas.util

import com.badlogic.gdx.math.Vector2

/** @author Matt Young */
data class Triangle(
    val v1: Vector2,
    val v2: Vector2,
    val v3: Vector2
) {
    fun centroid(): Vector2 {
        return Vector2((v1.x + v2.x + v3.x) / 3f, (v1.y + v2.y + v3.y) / 3f)
    }
}
