package com.decosegfault.atlas.util

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.decosegfault.atlas.render.GraphicsPreset
import com.decosegfault.atlas.render.GraphicsPresets
import com.decosegfault.hermes.types.SimType
import org.tinylog.kotlin.Logger
import java.io.IOException
import java.lang.IllegalArgumentException
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * @author Various (see comments)
 */
object AtlasUtils {
    // https://gdbooks.gitbooks.io/3dcollisions/content/Chapter1/closest_point_aabb.html
    fun bboxClosestPoint(point: Vector3, bounds: BoundingBox): Vector3 {
        return Vector3(
            point.x.coerceIn(bounds.min.x, bounds.max.x),
            point.y.coerceIn(bounds.min.y, bounds.max.y),
            point.z.coerceIn(bounds.min.z, bounds.max.z)
        )
    }

    fun writeHermesPreset(name: String) {
        val path = Paths.get(System.getProperty("user.home"), "Documents", "DECOSegfault", "hermes.txt")
        if (!path.exists()) {
            Logger.debug("Creating hermes.txt file")
            path.createFile()
        }
        Logger.info("Writing Hermes preset $name to path: $path")
        path.writeText(name)
    }

    fun readHermesPreset(): SimType {
        val path = Paths.get(System.getProperty("user.home"), "Documents", "DECOSegfault", "hermes.txt")
        Logger.info("Loading Hermes preset from: $path")
        var name: String
        try {
            name = path.readText().trim()
            Logger.info("Using saved Hermes preset: $name")
        } catch (e: IOException) {
            Logger.info("Using default hermes preset 'History', saved does not exist")
            name = "History"
        }
        return try {
            SimType.valueOf(name.uppercase())
        } catch (e: IllegalArgumentException) {
            Logger.error("Invalid Hermes preset $name!!! Using standard!")
            SimType.HISTORY
        }
    }

    fun BoundingBox.pad(padding: Float): BoundingBox {
        val padScalar = Matrix4().scl(padding)
        return mul(padScalar)
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
