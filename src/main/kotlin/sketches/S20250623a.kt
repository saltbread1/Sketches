package sketches

import processing.core.PGraphics
import processing.core.PVector
import kotlin.repeat

class S20250623a : ExtendedPApplet(P2D)
{
    private val palette = createPalette("d3cfdf-5db1b8-650f24-b10b24-1d38ac-0a1534-3a5431-bdb28d-2e140a-689578")
    private val numFillers = 6
    private var pixelFillers: MutableList<PixelFiller> = mutableListOf()

    override fun setup()
    {
        (0 until numFillers).forEach { _ -> pixelFillers.add(PixelFiller(1280, 720)) }

        blendMode(SUBTRACT)
        noLoop()
    }

    override fun draw()
    {
        // setup pixel fillers
        pixelFillers.forEach { pixelFiller -> pixelFiller.init() }
        while (pixelFillers.count { pixelFiller -> pixelFiller.update() } > 0)
        {
            pixelFillers.forEach { pixelFiller ->
                pixelFiller.update()
                pixelFiller.render()
            }
        }

        background(0xffc2c2c8.toInt())

        var alpha = 240.0f
        pushStyle()
        pixelFillers.shuffled().forEach { pixelFiller ->
            tint(0xffffffff.toInt(), alpha)
            image(pixelFiller.getImage(), 0.0f, 0.0f, width.toFloat(), height.toFloat())
            alpha *= 0.75f
        }
        popStyle()

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    override fun keyPressed()
    {
        super.keyPressed()
        redraw()
    }

    private inner class PixelFiller
    {
        private val pg: PGraphics
        private val numPixels: Int
        private val pixelList = mutableListOf<Pixel>()
        private val fillChecker: BooleanArray
        private val bgColor = 0xff000000.toInt()
        private var numInitUncheck: Int = 0

        constructor(pWidth: Int, pHeight: Int)
        {
            this.pg = createGraphics(pWidth, pHeight, P2D)
            this.numPixels = max(3, pWidth * pHeight / 20000)
            fillChecker = BooleanArray((pg.width + 2) * (pg.height + 2)) { false }
        }

        fun init()
        {
            pixelList.clear()
            fillChecker.fill(false)

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

            // create rectangles without overlap
            val rects = mutableListOf<Rect>()
            val maxIter = 20
            loop@ while (true)
            {
                for (i in 0..maxIter)
                {
                    if (i == maxIter)
                    {
                        break@loop
                    }
                    val newRect = Rect(
                        random((pg.width + 2).toFloat()),
                        random((pg.height + 2).toFloat()),
                        sq(random(1.0f)) * (pg.width + 2) * 0.12f,
                        sq(random(1.0f)) * (pg.height + 2) * 0.12f
                    )
                    if (rects.none { rect -> rect.isOverlapped(newRect) })
                    {
                        rects.add(newRect)
                        break
                    }
                }
            }
            // rectangle holes
            for (i in 0 until fillChecker.size)
            {
                val x = (i % (pg.width + 2)) - 1
                val y = (i / (pg.width + 2)) - 1
                fillChecker[i] = rects.any { rect -> rect.isInterior(x.toFloat(), y.toFloat()) }
            }

            // count uncheck cells
            numInitUncheck = fillChecker.count { !it }

            // background
            pg.beginDraw()
            pg.background(bgColor)
            pg.pushStyle()
            pg.noStroke()
            rects.forEach { rect ->
                pg.fill(palette.random(), 235.0f)
                rect.draw(pg)
            }
            pg.popStyle()
            pg.endDraw()
        }

        fun update(): Boolean
        {
            if (fillChecker.count { it } > numInitUncheck * 0.78f)
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
                    pixelList.add(Pixel(pg))
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
            color = palette.random()
            life = random(500.0f, 2000.0f).toInt()
            dir = random(8.0f).toInt()
            val rand = random(1.0f)
            step = if (rand < 0.7f) 1 else if (rand < 0.9f) 2 else 3
        }

        private fun getIndex(x: Int, y: Int): Int = (x + 1 + (y + 1) * (pg.width + 2))

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

    private class Rect
    {
        private val x: Float
        private val y: Float
        private val w: Float
        private val h: Float

        constructor(x: Float, y: Float, w: Float, h: Float)
        {
            this.x = x
            this.y = y
            this.w = w
            this.h = h
        }

        fun isInterior(x: Float, y: Float): Boolean = x >= this.x && x <= this.x + w && y >= this.y && y <= this.y + h

        fun isOverlapped(other: Rect): Boolean = x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y

        fun draw(pg: PGraphics)
        {
            pg.rect(x, y, w, h)
        }
    }
}
