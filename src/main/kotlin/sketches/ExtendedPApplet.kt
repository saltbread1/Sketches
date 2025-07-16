package sketches

import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector
import processing.opengl.PGraphicsOpenGL
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

abstract class ExtendedPApplet(private val renderer: String, protected val isSave: Boolean = false) : PApplet()
{
    protected val aspect by lazy { width.toFloat() / height.toFloat() }

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
        if (key == ESC)
        {
            return
        }
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
     * Calculate two points on the sphere.
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

    // -------- GL Utils -------- //

    protected fun viewToWorld(v: PVector): PVector
    {
        return viewToWorld(v, g)
    }

    protected fun viewToWorld(v: PVector, pg: PGraphics): PVector
    {
        // Get a view matrix whose up vector is flipped.
        val viewMat = (pg as PGraphicsOpenGL).camera.get()
        viewMat.m00 *= -1.0f
        viewMat.m01 *= -1.0f
        viewMat.m02 *= -1.0f
        viewMat.m10 *= -1.0f
        viewMat.m11 *= -1.0f
        viewMat.m12 *= -1.0f

        val side = PVector(viewMat.m00, viewMat.m10, viewMat.m20)
        val up = PVector(viewMat.m01, viewMat.m11, viewMat.m21)
        val forward = PVector(viewMat.m02, viewMat.m12, viewMat.m22)
        return PVector.mult(side, v.x).add(PVector.mult(up, v.y)).add(PVector.mult(forward, v.z)).normalize()
    }
}
