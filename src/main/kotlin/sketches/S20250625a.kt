package sketches

import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PImage
import util.*

class S20250625a : PApplet()
{
    private val palette = createPalette("d3cfdf-5db1b8-650f24-b10b24-1d38ac-0a1534-3a5431-bdb28d-2e140a-689578")
    private val textures = mutableListOf<PGraphics>()
    private val numKindTextures = 16
    private val far = 100.0f
    private var aspect = 0.0f
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
        repeat(numKindTextures)
        {
            val pg = createGraphics(256, 256, P2D)
            pg.beginDraw()
            pg.background(0xff000000.toInt())
            pg.blendMode(ADD)
            var gridWidth = 32
            var gridHeight = 32
            repeat(5)
            {
                gridRects(gridWidth, gridHeight, pg)
                gridWidth /= 2
                gridHeight /= 2
            }
            pg.endDraw()
            textures.add(pg)
        }

        this.aspect = width.toFloat() / height.toFloat()
        ortho(-aspect, aspect, -1.0f, 1.0f, 0.0f, far)
        camera(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)

        textureMode(NORMAL)
//        blendMode(ADD)
//        hint(DISABLE_DEPTH_TEST)
        noLoop()
    }

    override fun draw()
    {
        background(0xff000000.toInt())
        gridCube(40, textures)

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    private fun gridRects(nx: Int, ny: Int, pg: PGraphics)
    {
        for (iy in 0..ny)
        {
            for (ix in 0..nx)
            {
                val w = pg.width / nx.toFloat()
                val h = pg.height / ny.toFloat()
                pg.push()
                pg.translate(w * ix, h * iy)
                pg.rotate(random(TWO_PI))
                pg.rectMode(CENTER)
                pg.noStroke()
                pg.fill(palette[random(palette.size.toFloat()).toInt()], 77.0f)
                pg.rect(0.0f, 0.0f, w * random(1.0f, 1.35f), h * random(1.0f, 1.35f))
                pg.pop()
            }
        }
    }

    private fun gridCube(ny: Int, textures: Collection<PGraphics>)
    {
        val nx = (ny * aspect).toInt()
        for (iy in 0..ny)
        {
            for (ix in 0..nx)
            {
                val w = 2.0f * aspect / nx.toFloat()
                val h = 2.0f / ny.toFloat()
                val x = w * ix - aspect
                val y = h * iy - 1.0f
                val n = noise((x + aspect) * 1.6f, (y + 1.0f) * 10.2f)
                val z = -far * n
                val s = max(6.5f * n, 1.0f)
                val c = min(easeInPolynomial(1.0f - n, 4.0f) * 600.0f, 255.0f)
                push()
                translate(x, y, z)
                rotateZ((n * 2.0f - 1.0f) * 4.1f)
                rotateX(((1.0f - n) * 2.0f - 1.0f) * 2.9f)
//                stroke(c, 143.0f)
                noStroke()
                tint(c * 1.1f, c, c * 0.95f)
                texturedCube(max(w, h) * s, textures.shuffled().take(6).toTypedArray())
                pop()
            }
        }
    }

    private fun texturedCube(size: Float, textures: Array<PImage>)
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
        val seed = System.currentTimeMillis()
        noiseSeed(seed)
        randomSeed(seed)
        redraw()
    }
}
