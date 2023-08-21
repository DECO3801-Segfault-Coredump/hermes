package com.decosegfault.atlas.util

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

object AtlasUtils {
    // https://gdbooks.gitbooks.io/3dcollisions/content/Chapter1/closest_point_aabb.html
    fun closestPoint(point: Vector3, bounds: BoundingBox): Vector3 {
        return Vector3(
            point.x.coerceIn(bounds.min.x, bounds.max.x),
            point.y.coerceIn(bounds.min.y, bounds.max.y),
            point.z.coerceIn(bounds.min.z, bounds.max.z)
        )
    }
}
