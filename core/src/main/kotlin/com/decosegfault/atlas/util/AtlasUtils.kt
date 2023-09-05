package com.decosegfault.atlas.util

import com.badlogic.gdx.math.Quaternion
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

    // https://github.com/mrdoob/three.js/blob/dev/src/math/Vector3.js#L252
    fun Vector3.applyQuaternion(q: Quaternion): Vector3 {
        val x = this.x
        val y = this.y
        val z = this.z

        val qx = q.x
        val qy = q.y
        val qz = q.z
        val qw = q.w

        // calculate quat * vector

        val ix = qw * x + qy * z - qz * y;
        val iy = qw * y + qz * x - qx * z;
        val iz = qw * z + qx * y - qy * x;
        val iw = - qx * x - qy * y - qz * z;

        // calculate result * inverse quat

        this.x = ix * qw + iw * - qx + iy * - qz - iz * - qy;
        this.y = iy * qw + iw * - qy + iz * - qx - ix * - qz;
        this.z = iz * qw + iw * - qz + ix * - qy - iy * - qx;

        return this;
    }
}
