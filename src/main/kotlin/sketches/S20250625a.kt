package sketches

import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PImage
import util.createPalette
import util.saveName

class S20250625a : PApplet()
{
    private val palette = createPalette("d3cfdf-5db1b8-650f24-b10b24-1d38ac-0a1534-3a5431-bdb28d-2e140a-689578")
    private val textures = mutableListOf<PGraphics>()
    private val numKindTextures = 16
    private val far = 100.0f
    private val isSave = true

    override fun settings()
    {
        if (isSave)
        { // 4K
            size(1920, 1080, P3D)
            pixelDensity(2)
        }
        else
        {
            size(1280, 720, P3D)
            pixelDensity(1)
        }
    }

    override fun setup()
    {
        (0 until numKindTextures).forEach { i ->
            val pg = createGraphics(256, 256, P2D)
            pg.beginDraw()
            pg.background(0xff000000.toInt())
            pg.blendMode(ADD)
            var gridWidth = 32
            var gridHeight = 32
            (0 until 5).forEach { _ ->
                gridRects(gridWidth, gridHeight, pg)
                gridWidth /= 2
                gridHeight /= 2
            }
            pg.endDraw()
            textures.add(pg)
        }

        ortho(-width.toFloat() * 0.5f, width.toFloat() * 0.5f, -height.toFloat() * 0.5f, height.toFloat() * 0.5f, 0.0f, far)
        camera(width.toFloat() / 2.0f, height.toFloat() / 2.0f, 1.0f,
            width.toFloat() / 2.0f, height.toFloat() / 2.0f, 0.0f,
            0.0f, 1.0f, 0.0f)

        textureMode(NORMAL)
        hint(DISABLE_DEPTH_TEST)
        noLoop()
    }

    override fun draw()
    {
        background(0xff000000.toInt())
        noStroke()
        gridCube(width / 10, height / 10, -far * 0.5f, textures)

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    private fun gridRects(gridWidth: Int, gridHeight: Int, pg: PGraphics)
    {
        for (iy in 0..gridHeight)
        {
            for (ix in 0..gridWidth)
            {
                val w = pg.width / gridWidth.toFloat()
                val h = pg.height / gridHeight.toFloat()
                val x = w * ix
                val y = h * iy
                pg.push()
                pg.translate(x, y)
                pg.rotate(random(TWO_PI))
                pg.rectMode(CENTER)
                pg.noStroke()
                pg.fill(palette[random(palette.size.toFloat()).toInt()], 77.0f)
                pg.rect(0.0f, 0.0f, w * random(1.0f, 1.35f), h * random(1.0f, 1.35f))
                pg.pop()
            }
        }
    }

    private fun gridCube(gridWidth: Int, gridHeight: Int, depth: Float, textures: Collection<PGraphics>)
    {
        for (iy in 0..gridHeight)
        {
            for (ix in 0..gridWidth)
            {
                val w = width / gridWidth.toFloat()
                val h = height / gridHeight.toFloat()
                val x = w * ix
                val y = h * iy
                push()
                translate(x, y, depth)
                rotateZ(random(TWO_PI))
                rotateY(random(TWO_PI))
                rotateX(random(TWO_PI))
                val s = 1.5f + noise(x / width * 10.0f, y / height * 10.0f) * 16.0f
                drawTexturedCube(max(w, h) * s, textures.shuffled().take(6).toTypedArray())
                pop()
            }
        }
    }

    private fun drawTexturedCube(size: Float, textures: Array<PImage>)
    {
        if (textures.size < 6)
        {
            throw IllegalArgumentException("Texture array must have 6 textures! Got ${textures.size} instead.")
        }

        // front face
        beginShape(QUADS)
        texture(textures[0])
        vertex(-size / 2.0f, -size / 2.0f, size / 2.0f, 0.0f, 0.0f)
        vertex(size / 2.0f, -size / 2.0f, size / 2.0f, 1.0f, 0.0f)
        vertex(size / 2.0f, size / 2.0f, size / 2.0f, 1.0f, 1.0f)
        vertex(-size / 2.0f, size / 2.0f, size / 2.0f, 0.0f, 1.0f)
        endShape()

        // back face
        beginShape(QUADS)
        texture(textures[1])
        vertex(size / 2.0f, -size / 2.0f, -size / 2.0f, 0.0f, 0.0f)
        vertex(-size / 2.0f, -size / 2.0f, -size / 2.0f, 1.0f, 0.0f)
        vertex(-size / 2.0f, size / 2.0f, -size / 2.0f, 1.0f, 1.0f)
        vertex(size / 2.0f, size / 2.0f, -size / 2.0f, 0.0f, 1.0f)
        endShape()

        // right face
        beginShape(QUADS)
        texture(textures[2])
        vertex(size / 2.0f, -size / 2.0f, size / 2.0f, 0.0f, 0.0f)
        vertex(size / 2.0f, -size / 2.0f, -size / 2.0f, 1.0f, 0.0f)
        vertex(size / 2.0f, size / 2.0f, -size / 2.0f, 1.0f, 1.0f)
        vertex(size / 2.0f, size / 2.0f, size / 2.0f, 0.0f, 1.0f)
        endShape()

        // left face
        beginShape(QUADS)
        texture(textures[3])
        vertex(-size / 2.0f, -size / 2.0f, -size / 2.0f, 0.0f, 0.0f)
        vertex(-size / 2.0f, -size / 2.0f, size / 2.0f, 1.0f, 0.0f)
        vertex(-size / 2.0f, size / 2.0f, size / 2.0f, 1.0f, 1.0f)
        vertex(-size / 2.0f, size / 2.0f, -size / 2.0f, 0.0f, 1.0f)
        endShape()

        // top face
        beginShape(QUADS)
        texture(textures[4])
        vertex(-size / 2.0f, -size / 2.0f, -size / 2.0f, 0.0f, 0.0f)
        vertex(size / 2.0f, -size / 2.0f, -size / 2.0f, 1.0f, 0.0f)
        vertex(size / 2.0f, -size / 2.0f, size / 2.0f, 1.0f, 1.0f)
        vertex(-size / 2.0f, -size / 2.0f, size / 2.0f, 0.0f, 1.0f)
        endShape()

        // bottom face
        beginShape(QUADS)
        texture(textures[5])
        vertex(-size / 2.0f, size / 2.0f, size / 2.0f, 0.0f, 0.0f)
        vertex(size / 2.0f, size / 2.0f, size / 2.0f, 1.0f, 0.0f)
        vertex(size / 2.0f, size / 2.0f, -size / 2.0f, 1.0f, 1.0f)
        vertex(-size / 2.0f, size / 2.0f, -size / 2.0f, 0.0f, 1.0f)
        endShape()
    }

    override fun keyPressed()
    {
        if (key == ESC)
        {
            return
        }
        redraw()
    }
}
