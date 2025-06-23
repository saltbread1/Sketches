package s20250623a

import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PVector
import util.*

class S20250623a : PApplet()
{
    private val palette = createPalette("ae8b70-fa81cd-664864-efefef-9c3f26-c4c7bf-87a0ad-89d6c3-384a4d-77b764")
    private val numFillers = 32
    private var pixelFillers: MutableList<PixelFiller> = mutableListOf()
    private val isSave = false

    private val fovy = HALF_PI
    private val far = 100.0f
    private var aspect = 0.0f

    override fun settings()
    {
        size(1280, 720, P3D)
    }

    override fun setup()
    {
        // setup pixel fillers
        (0 until numFillers)
            .forEach { _ ->
                val pixelFiller = PixelFiller(320, 320)
                pixelFiller.init()
                pixelFillers.add(pixelFiller)
            }
        while (pixelFillers.count { pixelFiller -> pixelFiller.update() } > 0)
        {
            pixelFillers.forEach { pixelFiller ->
                pixelFiller.update()
                pixelFiller.render()
            }
        }

        this.aspect = width.toFloat() / height.toFloat()
        perspective(fovy, aspect, 0.1f, far)
        camera(0.0f, 0.0f, 2.0f / tan(fovy/ 2.0f), 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)

        textureMode(NORMAL)

        noLoop()
    }

    override fun draw()
    {
        background(0)

        pushMatrix()
        rotateX(random(TWO_PI))
        rotateY(random(TWO_PI))
        rotateZ(random(TWO_PI))
        drawTexturedBox(1.0f)
        popMatrix()

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    private fun drawTexturedBox(boxSize: Float)
    {
        val selectedFillers = pixelFillers.shuffled().take(6)

        // front face
        beginShape(QUADS)
        texture(selectedFillers[0].getImage())
        vertex(-boxSize/ 2.0f, -boxSize/ 2.0f, boxSize/ 2.0f, 0.0f, 0.0f)
        vertex(boxSize/ 2.0f, -boxSize/ 2.0f, boxSize/ 2.0f, 1.0f, 0.0f)
        vertex(boxSize/ 2.0f, boxSize/ 2.0f, boxSize/ 2.0f, 1.0f, 1.0f)
        vertex(-boxSize/ 2.0f, boxSize/ 2.0f, boxSize/ 2.0f, 0.0f, 1.0f)
        endShape()

        // back face
        beginShape(QUADS)
        texture(selectedFillers[1].getImage())
        vertex(boxSize/ 2.0f, -boxSize/ 2.0f, -boxSize/ 2.0f, 0.0f, 0.0f)
        vertex(-boxSize/ 2.0f, -boxSize/ 2.0f, -boxSize/ 2.0f, 1.0f, 0.0f)
        vertex(-boxSize/ 2.0f, boxSize/ 2.0f, -boxSize/ 2.0f, 1.0f, 1.0f)
        vertex(boxSize/ 2.0f, boxSize/ 2.0f, -boxSize/ 2.0f, 0.0f, 1.0f)
        endShape()

        // right face
        beginShape(QUADS)
        texture(selectedFillers[2].getImage())
        vertex(boxSize/ 2.0f, -boxSize/ 2.0f, boxSize/ 2.0f, 0.0f, 0.0f)
        vertex(boxSize/ 2.0f, -boxSize/ 2.0f, -boxSize/ 2.0f, 1.0f, 0.0f)
        vertex(boxSize/ 2.0f, boxSize/ 2.0f, -boxSize/ 2.0f, 1.0f, 1.0f)
        vertex(boxSize/ 2.0f, boxSize/ 2.0f, boxSize/ 2.0f, 0.0f, 1.0f)
        endShape()

        // left face
        beginShape(QUADS)
        texture(selectedFillers[3].getImage())
        vertex(-boxSize/ 2.0f, -boxSize/ 2.0f, -boxSize/ 2.0f, 0.0f, 0.0f)
        vertex(-boxSize/ 2.0f, -boxSize/ 2.0f, boxSize/ 2.0f, 1.0f, 0.0f)
        vertex(-boxSize/ 2.0f, boxSize/ 2.0f, boxSize/ 2.0f, 1.0f, 1.0f)
        vertex(-boxSize/ 2.0f, boxSize/ 2.0f, -boxSize/ 2.0f, 0.0f, 1.0f)
        endShape()
        
        // top face
        beginShape(QUADS)
        texture(selectedFillers[4].getImage())
        vertex(-boxSize/ 2.0f, -boxSize/ 2.0f, -boxSize/ 2.0f, 0.0f, 0.0f)
        vertex(boxSize/ 2.0f, -boxSize/ 2.0f, -boxSize/ 2.0f, 1.0f, 0.0f)
        vertex(boxSize/ 2.0f, -boxSize/ 2.0f, boxSize/ 2.0f, 1.0f, 1.0f)
        vertex(-boxSize/ 2.0f, -boxSize/ 2.0f, boxSize/ 2.0f, 0.0f, 1.0f)
        endShape()

        // bottom face
        beginShape(QUADS)
        texture(selectedFillers[5].getImage())
        vertex(-boxSize/ 2.0f, boxSize/ 2.0f, boxSize/ 2.0f, 0.0f, 0.0f)
        vertex(boxSize/ 2.0f, boxSize/ 2.0f, boxSize/ 2.0f, 1.0f, 0.0f)
        vertex(boxSize/ 2.0f, boxSize/ 2.0f, -boxSize/ 2.0f, 1.0f, 1.0f)
        vertex(-boxSize/ 2.0f, boxSize/ 2.0f, -boxSize/ 2.0f, 0.0f, 1.0f)
        endShape()
    }

    override fun keyPressed()
    {
        redraw()
    }

    private inner class PixelFiller
    {
        private val pg: PGraphics
        private val numPixels: Int
        private val pixelList = mutableListOf<Pixel>()
        private val fillChecker: BooleanArray
        private val bgColor = 0xff000000.toInt()

        constructor(pWidth: Int, pHeight: Int)
        {
            this.pg = createGraphics(pWidth, pHeight, P2D)
            this.numPixels = random(20.0f, 40.0f).toInt()
            fillChecker = BooleanArray((pg.width + 2) * (pg.height + 2)) { false }
        }

        fun init()
        {
            pixelList.clear()
            fillChecker.fill(false);

            // boundary
            for (i in 0 until pg.width + 2)
            {
                fillChecker[i] = true
                fillChecker[(pg.width + 2) * (pg.height + 1) + i] = true
            }
            for (i in 0 until pg.height + 2)
            {
                fillChecker[i * (pg.width + 2)] = true
                fillChecker[i * (pg.width + 2) + pg.width + 1] = true
            }

            // clear framebuffer
            pg.beginDraw()
            pg.background(bgColor)
            pg.endDraw()
        }

        fun update(): Boolean
        {
            if (fillChecker.asSequence().filter { it }.count() > fillChecker.size * 0.5f)
            {
                return false
            }

            val minTrails = 8
            val iterations = 20

            pixelList.removeAll { !it.isAlive && it.trailCount() <= minTrails }

            repeat(iterations)
            {
                while (pixelList.count { pixel -> pixel.isAlive } < numPixels)
                {
                    val pixel = Pixel(pg)
                    pixel.init(fillChecker)
                    pixelList.add(pixel)
                }
                pixelList.forEach { pixel -> pixel.update(fillChecker) }
            }

            return true
        }

        fun render()
        {
            pg.beginDraw()
            pixelList.forEach { pixel -> pixel.draw() }
            pg.endDraw()
        }

        fun getImage(): PGraphics = pg
    }

    private inner class Pixel
    {
        private val pg: PGraphics
        private var x: Int
        private var y: Int
        private val color: Int
        private var life: Int
        private var dir: Int = 0
        private val step: Int
        private val trails = mutableListOf<PVector>()

        val isAlive: Boolean get() = life > 0

        private val dirs = arrayOf(
            PVector(0.0f, -1.0f),
            PVector(1.0f, -1.0f),
            PVector(1.0f, 0.0f),
            PVector(1.0f, 1.0f),
            PVector(0.0f, 1.0f),
            PVector(-1.0f, 1.0f),
            PVector(-1.0f, 0.0f),
            PVector(-1.0f, -1.0f),
        )

        constructor(pg: PGraphics)
        {
            this.pg = pg
            x = random(pg.width.toFloat()).toInt()
            y = random(pg.height.toFloat()).toInt()
            color = palette[random(palette.size.toFloat()).toInt()]
            life = random(500.0f, 2000.0f).toInt()
            dir = random(8.0f).toInt()
            step = if (random(1.0f) < 0.7f) 1 else 2
            trails.add(PVector(x.toFloat(), y.toFloat()))
        }

        private fun getIndex(x: Int, y: Int): Int = (x + 1 + (y + 1) * (pg.width + 2))

        fun init(pixelChecker: BooleanArray)
        {
            pixelChecker[getIndex(x, y)] = true
        }

        fun update(pixelChecker: BooleanArray)
        {
            if (life <= 0)
            {
                return
            }

            var attempts = 0
            while (attempts < 8)
            {
                val newX = clamp(x + dirs[dir].x.toInt() * step, 0, pg.width)
                val newY = clamp(y + dirs[dir].y.toInt() * step, 0, pg.height)
                val idx = getIndex(newX, newY)
                if (!pixelChecker[idx])
                {
                    pixelChecker[idx] = true
                    x = newX
                    y = newY
                    trails.add(PVector(x.toFloat(), y.toFloat()))
                    life--
                    return
                }

                dir = (dir + 1) % 8
                attempts++
            }

            life = 0
        }

        fun draw()
        {
            pg.stroke(color)
            trails.forEach { trail -> pg.point(trail.x, trail.y) }
            trails.clear() // clear buffer
        }

        fun trailCount(): Int = trails.size
    }
}
