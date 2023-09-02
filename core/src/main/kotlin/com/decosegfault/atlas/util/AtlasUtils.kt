package com.decosegfault.atlas.util

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox

object AtlasUtils {
    // https://gdbooks.gitbooks.io/3dcollisions/content/Chapter1/closest_point_aabb.html
    fun bboxClosestPoint(point: Vector3, bounds: BoundingBox): Vector3 {
        return Vector3(
            point.x.coerceIn(bounds.min.x, bounds.max.x),
            point.y.coerceIn(bounds.min.y, bounds.max.y),
            point.z.coerceIn(bounds.min.z, bounds.max.z)
        )
    }

    /**
     * Modifies [point] in place so that it is clamped inside the rectangle [rect]
     * The rectangle should lie on the ground plane. Ignores height.
     */
    fun clampToRect3D(point: Vector3, rect: Rectangle) {
        point.x = point.x.coerceIn(rect.x, rect.x + rect.width)
        point.z = point.z.coerceIn(rect.y, rect.y + rect.height)
    }
}
