package sketches

import processing.core.*
import processing.opengl.PGraphicsOpenGL
import util.Quaternion
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

abstract class ExtendedPApplet(private val renderer: String, protected val isSave: Boolean = false) : PApplet()
{
    protected val aspect by lazy { width.toFloat() / height.toFloat() }

    companion object
    {
        const val PAPER_WHITE = 0xfff5f5f5.toInt()
        const val REFINED_WHITE = 0xffe7eff5.toInt()
        const val RETRO_WHITE = 0xfff1efe2.toInt()
        const val NIGHT_BLACK = 0xff0c0c12.toInt()
        const val RICH_BLACK = 0xff0d0e16.toInt()
    }

    override fun settings()
    {
        if (isSave)
        { // 4K
            size(1920, 1080, renderer)
            pixelDensity(2)
        }
        else
        { // HD
            size(1280, 720, renderer)
            pixelDensity(1)
        }
    }

    override fun keyPressed()
    {
        if (key == ESC) { return }
    }

    // -------- Sketch Utils -------- //

    protected fun createPalette(colors: String): IntArray =
        colors.split("-").map { it.toInt(16) or (0xff000000.toInt()) }.toIntArray()

    protected fun timestamp(format: String = "yyyyMMddHHmmss"): String
    {
        val now = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
        val formatter = DateTimeFormatter.ofPattern(format)
        return now.format(formatter)
    }

    protected fun saveName(clazz: KClass<*>): String =
        "output${File.separator}${clazz.simpleName}${File.separator}${timestamp()}-######.png"

    protected fun saveFrameName(clazz: KClass<*>): String =
        "output${File.separator}${clazz.simpleName}${File.separator}######.png"

    protected fun makeMovie(clazz: KClass<*>, inputFPS: Int, outputFPS: Int = inputFPS): String
    {
        val outputDir = "output${File.separator}${clazz.simpleName}${File.separator}"
        val imgName = "$outputDir%06d.png"
        val movieName = "$outputDir${timestamp()}.mp4"
        val command = "ffmpeg -y -loglevel 16 -r $inputFPS -i $imgName -vcodec libx264 -pix_fmt yuv420p -r $outputFPS $movieName"
        val process = ProcessBuilder(*command.split(" ").toTypedArray()).redirectErrorStream(true).start()
        return process.inputStream.bufferedReader().readText()
    }

    protected fun resourcePath(filename: String): String? = this::class.java.classLoader.getResource(filename)?.path

    protected fun <T> choose(vararg items: T): T = items.random()

    // -------- Math Utils -------- //

    protected fun clamp(value: Int, min: Int, max: Int): Int = min(max(min, value), max)

    protected fun clamp(value: Float, min: Float, max: Float): Float = min(max(min, value), max)

    protected fun sign(x: Int): Int = if (x < 0) -1 else 1

    protected fun sign(x: Float): Float = if (x < 0.0f) -1.0f else 1.0f

    protected fun signOrZero(x: Int): Int = if (x < 0) -1 else if (x == 0) 0 else 1

    protected fun signOrZero(x: Float): Float = if (x < 0.0f) -1.0f else if (x == 0.0f) 0.0f else 1.0f

    protected fun smoothstep(a: Float, b: Float, x: Float): Float
    {
        if (x <= a) { return 0.0f }
        if (x >= b) { return 1.0f }
        val t = (x - a) / (b - a)
        return t * t * (3.0f - 2.0f * t)
    }

    protected fun fract(x: Float): Float = mod(x, 1.0f)

    protected fun mod(x: Float, y: Float): Float = x - y * floor(x / y)

    protected fun easeInPolynomial(x: Float, d: Float): Float = pow(x, d)

    protected fun easeOutPolynomial(x: Float, d: Float): Float = 1.0f - pow(1.0f - x, d)

    protected fun easeInOutPolynomial(x: Float, d: Float): Float =
        if (x < 0.5f) 0.5f * pow(2.0f * x, d) else 1.0f - 0.5f * pow(2.0f * (1.0f - x), d)

    protected fun easeOutBack(x: Float, c1: Float = 1.70158f): Float
    {
        val c3 = c1 + 1.0f
        return 1.0f + (c3 * (x - 1.0f) + c1) * sq(x - 1.0f)
    }

    protected fun sigmoid(x: Float, a: Float = 1.0f): Float
    {
        return 1.0f / (1.0f + exp(-a * x))
    }

    /**
     * Calculate the distance between two points on the sphere.
     */
    protected fun haversine(latitude1: Float, longitude1: Float, latitude2: Float, longitude2: Float, radius: Float = 1.0f): Float
    {
        val latitude = latitude2 - latitude1
        val longitude = longitude2 - longitude1
        return 2.0f * radius * asin(sqrt(sq(sin(latitude / 2.0f)) + cos(latitude1) * cos(latitude2) * sq(sin(longitude / 2.0f))))
    }

    protected fun linesIntersect(p1: PVector, p2: PVector, q1: PVector, q2: PVector): Boolean
    {
        val v1 = PVector.sub(p2, p1)
        val v2 = PVector.sub(q1, p1)
        val v3 = PVector.sub(q2, p1)

        val s = PVector.cross(v1, v2, null).z
        val t = PVector.cross(v1, v3, null).z

        return s * t < 0.0f
    }

    protected fun viewToWorld(v: PVector): PVector = viewToWorld(v, g)

    protected fun viewToWorld(v: PVector, pg: PGraphics): PVector
    {
        val viewMat = (pg as PGraphicsOpenGL).camera.get()
        val side = PVector(viewMat.m00, viewMat.m01, viewMat.m02)
        val up = PVector(-viewMat.m10, -viewMat.m11, -viewMat.m12)
        val forward = PVector(viewMat.m20, viewMat.m21, viewMat.m22)
        return PVector.mult(side, v.x).add(PVector.mult(up, v.y)).add(PVector.mult(forward, v.z))
    }

    protected fun applyBillboard() = applyBillboard(g)

    protected fun applyBillboard(pg: PGraphics)
    {
        val pg = pg as PGraphicsOpenGL
        val viewMat = pg.camera.get()
        viewMat.m03 = 0.0f
        viewMat.m13 = 0.0f
        viewMat.m23 = 0.0f
        viewMat.transpose()
        pg.applyMatrix(viewMat)
    }

    protected fun rotate(q: Quaternion, v: PVector): PVector
    {
        val ret = q.mul(Quaternion(v)).mul(q.inverse())
        return PVector(ret.x, ret.y, ret.z)
    }

    protected fun rotate(pg: PGraphics, q: Quaternion)
    {
        val rMat = PMatrix3D()
        rMat.m00 = 1.0f - 2.0f * (q.y * q.y + q.z * q.z)
        rMat.m01 = 2.0f * (q.x * q.y - q.w * q.z)
        rMat.m02 = 2.0f * (q.x * q.z + q.w * q.y)
        rMat.m10 = 2.0f * (q.x * q.y + q.w * q.z)
        rMat.m11 = 1.0f - 2.0f * (q.x * q.x + q.z * q.z)
        rMat.m12 = 2.0f * (q.y * q.z - q.w * q.x)
        rMat.m20 = 2.0f * (q.x * q.z - q.w * q.y)
        rMat.m21 = 2.0f * (q.y * q.z + q.w * q.x)
        rMat.m22 = 1.0f - 2.0f * (q.x * q.x + q.y * q.y)
        pg.applyMatrix(rMat)
    }

    // -------- Shapes -------- //

    protected fun createCube(size: Float = 1.0f) : PShape
    {
        val cube = createShape()
        cube.beginShape(TRIANGLES)

        // Bottom (y = -size)
        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)

        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)
        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)

        // Top (y = +size)
        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)

        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)

        // Front (z = -size)
        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)

        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)
        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)

        // Back (z = +size)
        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)

        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)

        // Left (x = -size)
        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)

        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)
        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)

        // Right (x = +size)
        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)

        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)

        cube.endShape()

        return cube
    }

    protected fun createCubeFlat(size: Float = 1.0f) : PShape
    {
        val cube = createShape()
        cube.beginShape(TRIANGLES)

        // Bottom (y = -size), normal = (0, -1, 0)
        cube.normal( 0.0f, -1.0f,  0.0f); cube.vertex(-size, -size, -size)
        cube.normal( 0.0f, -1.0f,  0.0f); cube.vertex( size, -size, -size)
        cube.normal( 0.0f, -1.0f,  0.0f); cube.vertex( size, -size,  size)

        cube.normal( 0.0f, -1.0f,  0.0f); cube.vertex(-size, -size, -size)
        cube.normal( 0.0f, -1.0f,  0.0f); cube.vertex( size, -size,  size)
        cube.normal( 0.0f, -1.0f,  0.0f); cube.vertex(-size, -size,  size)

        // Top (y = +size), normal = (0, +1, 0)
        cube.normal( 0.0f,  1.0f,  0.0f); cube.vertex(-size,  size, -size)
        cube.normal( 0.0f,  1.0f,  0.0f); cube.vertex(-size,  size,  size)
        cube.normal( 0.0f,  1.0f,  0.0f); cube.vertex( size,  size,  size)

        cube.normal( 0.0f,  1.0f,  0.0f); cube.vertex(-size,  size, -size)
        cube.normal( 0.0f,  1.0f,  0.0f); cube.vertex( size,  size,  size)
        cube.normal( 0.0f,  1.0f,  0.0f); cube.vertex( size,  size, -size)

        // Front (z = -size), normal = (0, 0, -1)
        cube.normal( 0.0f,  0.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 0.0f,  0.0f, -1.0f); cube.vertex(-size,  size, -size)
        cube.normal( 0.0f,  0.0f, -1.0f); cube.vertex( size,  size, -size)

        cube.normal( 0.0f,  0.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 0.0f,  0.0f, -1.0f); cube.vertex( size,  size, -size)
        cube.normal( 0.0f,  0.0f, -1.0f); cube.vertex( size, -size, -size)

        // Back (z = +size), normal = (0, 0, +1)
        cube.normal( 0.0f,  0.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal( 0.0f,  0.0f,  1.0f); cube.vertex( size, -size,  size)
        cube.normal( 0.0f,  0.0f,  1.0f); cube.vertex( size,  size,  size)

        cube.normal( 0.0f,  0.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal( 0.0f,  0.0f,  1.0f); cube.vertex( size,  size,  size)
        cube.normal( 0.0f,  0.0f,  1.0f); cube.vertex(-size,  size,  size)

        // Left (x = -size), normal = (-1, 0, 0)
        cube.normal(-1.0f,  0.0f,  0.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f,  0.0f,  0.0f); cube.vertex(-size, -size,  size)
        cube.normal(-1.0f,  0.0f,  0.0f); cube.vertex(-size,  size,  size)

        cube.normal(-1.0f,  0.0f,  0.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f,  0.0f,  0.0f); cube.vertex(-size,  size,  size)
        cube.normal(-1.0f,  0.0f,  0.0f); cube.vertex(-size,  size, -size)

        // Right (x = +size), normal = (+1, 0, 0)
        cube.normal( 1.0f,  0.0f,  0.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f,  0.0f,  0.0f); cube.vertex( size,  size, -size)
        cube.normal( 1.0f,  0.0f,  0.0f); cube.vertex( size,  size,  size)

        cube.normal( 1.0f,  0.0f,  0.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f,  0.0f,  0.0f); cube.vertex( size,  size,  size)
        cube.normal( 1.0f,  0.0f,  0.0f); cube.vertex( size, -size,  size)

        cube.endShape()

        return cube
    }

    protected fun createRect(size: Float = 1.0f) : PShape
    {
        val rect = createShape()
        rect.beginShape(TRIANGLES)

        rect.normal(-1.0f, -1.0f, -1.0f); rect.vertex(-size, -size, 0.0f, 0.0f, 0.0f)
        rect.normal(-1.0f,  1.0f, -1.0f); rect.vertex(-size,  size, 0.0f, 0.0f, 1.0f)
        rect.normal( 1.0f,  1.0f, -1.0f); rect.vertex( size,  size, 0.0f, 1.0f, 1.0f)

        rect.normal(-1.0f, -1.0f, -1.0f); rect.vertex(-size, -size, 0.0f, 0.0f, 0.0f)
        rect.normal( 1.0f,  1.0f, -1.0f); rect.vertex( size,  size, 0.0f, 1.0f, 1.0f)
        rect.normal( 1.0f, -1.0f, -1.0f); rect.vertex( size, -size, 0.0f, 1.0f, 0.0f)

        rect.endShape()

        return rect
    }

    protected fun createRectFlat(size: Float = 1.0f) : PShape
    {
        val rect = createShape()
        rect.beginShape(TRIANGLES)

        rect.normal( 0.0f,  0.0f, -1.0f); rect.vertex(-size, -size, 0.0f, 0.0f, 0.0f)
        rect.normal( 0.0f,  0.0f, -1.0f); rect.vertex(-size,  size, 0.0f, 0.0f, 1.0f)
        rect.normal( 0.0f,  0.0f, -1.0f); rect.vertex( size,  size, 0.0f, 1.0f, 1.0f)

        rect.normal( 0.0f,  0.0f, -1.0f); rect.vertex(-size, -size, 0.0f, 0.0f, 0.0f)
        rect.normal( 0.0f,  0.0f, -1.0f); rect.vertex( size,  size, 0.0f, 1.0f, 1.0f)
        rect.normal( 0.0f,  0.0f, -1.0f); rect.vertex( size, -size, 0.0f, 1.0f, 0.0f)

        rect.endShape()

        return rect
    }

    protected fun createHemiSphere(size: Float = 1.0f, res: Int = 32) : PShape
    {
        val sphere = createShape()
        val vertices = mutableListOf<PVector>()
        // upper left to lower right
        for (j in 0 .. res)
        {
            val v = j.toFloat() / res
            for (i in 0 .. res)
            {
                val u = i.toFloat() / res
                val x = cos(PI * u) * sin(PI * v) * size
                val y = cos(PI * v) * size
                val z = sin(PI * u) * sin(PI * v) * size
                vertices.add(PVector(x, y, z))
            }
        }
        sphere.beginShape(TRIANGLES)
        for (j in 0 until res)
        {
            for (i in 0 until res)
            {
                val idx = j * (res + 1) + i

                // quad vertices in CCW
                val pos0 = vertices[idx]; val uv0 = PVector(i.toFloat() / res, j.toFloat() / res)
                val pos1 = vertices[idx + res + 1]; val uv1 = PVector(i.toFloat() / res, (j + 1).toFloat() / res)
                val pos2 = vertices[idx + res + 2]; val uv2 = PVector((i + 1).toFloat() / res, (j + 1).toFloat() / res)
                val pos3 = vertices[idx + 1]; val uv3 = PVector((i + 1).toFloat() / res, j.toFloat() / res)

                sphere.normal(pos0.x, pos0.y, pos0.z); sphere.vertex(pos0.x, pos0.y, pos0.z, uv0.x, uv0.y)
                sphere.normal(pos1.x, pos1.y, pos1.z); sphere.vertex(pos1.x, pos1.y, pos1.z, uv1.x, uv1.y)
                sphere.normal(pos2.x, pos2.y, pos2.z); sphere.vertex(pos2.x, pos2.y, pos2.z, uv2.x, uv2.y)

                sphere.normal(pos0.x, pos0.y, pos0.z); sphere.vertex(pos0.x, pos0.y, pos0.z, uv0.x, uv0.y)
                sphere.normal(pos2.x, pos2.y, pos2.z); sphere.vertex(pos2.x, pos2.y, pos2.z, uv2.x, uv2.y)
                sphere.normal(pos3.x, pos3.y, pos3.z); sphere.vertex(pos3.x, pos3.y, pos3.z, uv3.x, uv3.y)
            }
        }
        sphere.endShape()

        return sphere
    }
}
