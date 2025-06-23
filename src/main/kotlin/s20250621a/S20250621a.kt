package s20250621a

import processing.core.PApplet
import processing.core.PGraphics
import util.*

class S20250621a : PApplet()
{
    private val far = 100.0f
    private var aspect = 0.0f
    private val palette = createPalette("ae8b70-fa81cd-664864-efefef-9c3f26-c4c7bf-87a0ad-89d6c3-384a4d-77b764")
    private var pg: PGraphics? = null
    private val isSave = false

    override fun settings()
    {
        if (isSave)
        { // 4K
            size(1920, 1080, P2D)
            pixelDensity(2)
        }
        else
        {
            size(1280, 720, P2D)
        }
    }

    override fun setup()
    {
        this.aspect = width.toFloat() / height.toFloat()
        val fov: Float = HALF_PI

        pg = createGraphics(width, height, P3D)
        pg!!.beginDraw()
        pg!!.blendMode(ADD)
        pg!!.hint(DISABLE_DEPTH_TEST)
        pg!!.ortho(-aspect, aspect, -1.0f, 1.0f, 0.1f, far)
        pg!!.camera(0.0f, 0.0f, 1.0f / tan(fov / 2.0f), 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        pg!!.endDraw()

        noLoop()
    }

    override fun draw()
    {
        val rectList = mutableListOf<Rect>()
        rectList.add(Rect(-aspect, -1.0f, 2.0f * aspect, 2.0f))
        (0 until 14).forEach { _ ->
            val newRectList = mutableListOf<Rect>()
            rectList.forEach { rect ->
                if (rect.w * rect.h < 1.6e-4f) return@forEach
                newRectList.addAll(divide(rect))
            }
            rectList.clear()
            rectList.addAll(newRectList)
        }

        // render
        pg!!.beginDraw()
        pg!!.clear()
        pg!!.noStroke()
        rectList.forEach { rect ->
            val cx = rect.x + rect.w / 2.0f
            val cy = rect.y + rect.h / 2.0f
            pg!!.pushMatrix()
            trans(cx, cy, pg!!)
            pg!!.scale(rect.w, rect.h, rect.h)
            pg!!.fill(getColor(0.42f))
            pg!!.box(1.0f)
            pg!!.popMatrix()
        }
        pg!!.endDraw()

        background(0xff4242ec.toInt())
        noStroke()
        rectList.forEach { rect ->
            val x = map(rect.x, -aspect, aspect, 0.0f, width.toFloat())
            val y = map(rect.y, -1.0f, 1.0f, 0.0f, height.toFloat())
            val w = rect.w * width / (2.0f * aspect)
            val h = rect.h * height / 2.0f
            fill(getColor(1.0f))
            rect(x, y, w, h)
        }
        image(pg, 0.0f, 0.0f)

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    private fun getColor(alpha: Float): Int
    {
        val col = palette[random(palette.size.toFloat()).toInt()]
        return (col and 0xffffff) or (((alpha * 0xff).toInt() shl 24) and 0xff000000.toInt())
    }

    private fun divide(rect: Rect): Collection<Rect>
    {
        val x = rect.x
        val y = rect.y
        val w = rect.w
        val h = rect.h
        var r = randomGaussian() * 1.2f
        r = sigmoid(r)
        if (w > h * 4.0f)
        {
            r *= w
            return listOf(Rect(x, y, r, h), Rect(x + r, y, w - r, h))
        }
        else
        {
            r *= h
            return listOf(Rect(x, y, w, r), Rect(x, y + r, w, h - r))
        }
    }

    private fun trans(x: Float, y: Float, pg: PGraphics)
    {
        val z = -noise(x * 0.64f, y * 0.64f) * far
        pg.translate(x, y, z)

        val pitch = (noise(x * 1.92f, y * 1.84f) * 2.0f - 1.0f) * 3.35f
        val yaw = (noise(x * 2.12f, y * 2.12f) * 2.0f - 1.0f) * 2.82f

        pg.rotateX(pitch)
        pg.rotateZ(yaw)
    }

    private fun sigmoid(x: Float): Float
    {
        return 1.0f / (1.0f + exp(-x))
    }

    override fun keyPressed()
    {
        if (key == ESC)
        {
            return
        }
        val seed = System.currentTimeMillis()
        noiseSeed(seed)
        randomSeed(seed)
        redraw()
    }

    private data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)
}
