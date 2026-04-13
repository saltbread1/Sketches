package util

import processing.core.PVector
import java.util.Objects
import kotlin.math.*

class Quaternion private constructor(val x: Float, val y: Float, val z: Float, val w: Float)
{
    data class AxisRadians(val axis: PVector, val radians: Float)

    constructor(v: PVector) : this(v.x, v.y, v.z, 0.0f)

    /**
     * Copy constructor.
     */
    constructor(q: Quaternion) : this(q.x, q.y, q.z, q.w)

    fun toAxisRadians(): AxisRadians
    {
        val halfRad = acos(w)
        val s = sin(halfRad)
        val axis = if (abs(s) < 1e-7f) PVector(1.0f, 0.0f, 0.0f) else PVector(x / s, y / s, z / s).normalize()
        return AxisRadians(axis, 2.0f * halfRad)
    }

    /**
     * Add a quaternion.
     */
    private fun add(q: Quaternion): Quaternion = Quaternion(x + q.x, y + q.y, z + q.z, w + q.w)

    /**
     * Subtract a quaternion.
     */
    private fun sub(q: Quaternion): Quaternion = Quaternion(x - q.x, y - q.y, z - q.z, w - q.w)

    /**
     * Multiply a quaternion from the right.
     */
    fun mul(q: Quaternion): Quaternion = Quaternion(
        x * q.w + w * q.x + y * q.z - z * q.y,
        y * q.w + w * q.y + z * q.x - x * q.z,
        z * q.w + w * q.z + x * q.y - y * q.x,
        w * q.w - x * q.x - y * q.y - z * q.z
    )

    /**
     * Multiply quaternion by a scalar.
     */
    private fun mul(a: Float): Quaternion = Quaternion(x * a, y * a, z * a, w * a)

    /**
     * Divide quaternion by a scalar.
     */
    private fun div(a: Float): Quaternion = mul(1.0f / a)

    /**
     * Inverse of the quaternion.
     */
    fun inverse(): Quaternion
    {
        return Quaternion(-x, -y, -z, w)
    }

    /**
     * Dot product of two quaternions.
     */
    fun dot(q: Quaternion): Float
    {
        return x * q.x + y * q.y + z * q.z + w * q.w
    }

    /**
     * Normalize the quaternion.
     */
    private fun normalize(): Quaternion
    {
        val mag = sqrt(dot(this))
        return div(mag)
    }

    companion object
    {
        /**
         * Create an identity quaternion.
         */
        fun identity(): Quaternion = Quaternion(0.0f, 0.0f, 0.0f, 1.0f)

        /**
         * Create a uint quaternion from axis and angle.
         *
         * @param axis    axis of rotation (normalized)
         * @param radians angle of rotation in radians
         */
        fun axisRadians(axis: PVector, radians: Float): Quaternion
        {
            val halfRad = radians * 0.5f
            val s = sin(halfRad)
            val x = axis.x * s
            val y = axis.y * s
            val z = axis.z * s
            val w = cos(halfRad)
            return Quaternion(x, y, z, w)
        }

        /**
         * Create a unit quaternion from {@code fromDirection} to {@code toDirection}.
         *
         * @param fromDirection direction of rotation from (normalized)
         * @param toDirection   direction of rotation to (normalized)
         */
        fun frontToRotation(fromDirection: PVector, toDirection: PVector): Quaternion
        {
            val axis = PVector.cross(fromDirection, toDirection, null).normalize()
            val rad = acos(PVector.dot(fromDirection, toDirection))
            return axisRadians(axis, rad)
        }

        /**
         * Linear interpolation between two quaternions.
         */
        fun lerp(q1: Quaternion, q2: Quaternion, t: Float): Quaternion
        {
            return q1.mul(1.0f - t).add(q2.mul(t)).normalize()
        }

        /**
         * Spherical linear interpolation between two quaternions.
         */
        fun slerp(q1: Quaternion, q2: Quaternion, t: Float): Quaternion
        {
            val rad = acos(q1.dot(q2))
            val s = sin(rad)
            val s1 = sin((1.0f - t) * rad) / s
            val s2 = sin(t * rad) / s
            return q1.mul(s1).add(q2.mul(s2)).normalize()
        }
    }

    override fun toString(): String
    {
        return "Quaternion(x=$x, y=$y, z=$z, w=$w)"
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (other is Quaternion)
        {
            return x == other.x && y == other.y && z == other.z && w == other.w
        }
        return false
    }

    override fun hashCode(): Int
    {
        return Objects.hash(x, y, z, w)
    }
}
