package s20250623a

import processing.core.PApplet
import processing.core.PVector
import util.clamp
import util.createPalette

class S20250623a : PApplet()
{
    private var pixelChecker = booleanArrayOf()
    private var pixelList = mutableListOf<Pixel>()
    private val palette = createPalette("ae8b70-fa81cd-664864-efefef-9c3f26-c4c7bf-87a0ad-89d6c3-384a4d-77b764")
    private val numPixels = 64

    override fun settings()
    {
        size(640, 360, P2D)
    }

    override fun setup()
    {
        init()
    }

    private fun init()
    {
        pixelChecker = BooleanArray((width + 2) * (height + 2)) { false }

        // boundary
        for (i in 0 until width + 2)
        {
            pixelChecker[i] = true
            pixelChecker[(width + 2) * (height + 1) + i] = true
        }
        for (i in 0 until height + 2)
        {
            pixelChecker[i * (width + 2)] = true
            pixelChecker[i * (width + 2) + width + 1] = true
        }

        pixelList.clear()
    }

    override fun draw()
    {
        background(0)

        if (pixelChecker.asSequence().filter { it }.count() > pixelChecker.size * 0.5f)
        {
            noLoop()
        }

        pixelList.removeAll { !it.isAlive && it.trailCount() <= 8 }

        repeat(20)
        {
            while (pixelList.count { pixel -> pixel.isAlive } < numPixels)
            {
                pixelList.add(Pixel())
            }
            pixelList.forEach { pixel -> pixel.update() }
        }

        pixelList.forEach { pixel -> pixel.draw() }
    }

    override fun keyPressed()
    {
        init()
        loop()
    }

    private inner class Pixel
    {
        private var x: Int
        private var y: Int
        private val color: Int
        private var life: Int
        private var dir: Int = 0
        private val step: Int
        private val trails = mutableListOf<PVector>()

        val isAlive: Boolean get() = life > 0

        private val dirs = arrayOf(
            PVector( 0.0f, -1.0f),
            PVector( 1.0f, -1.0f),
            PVector( 1.0f,  0.0f),
            PVector( 1.0f,  1.0f),
            PVector( 0.0f,  1.0f),
            PVector(-1.0f,  1.0f),
            PVector(-1.0f,  0.0f),
            PVector(-1.0f, -1.0f),
        )

        constructor()
        {
            this.x = random(width.toFloat()).toInt()
            this.y = random(height.toFloat()).toInt()
            this.color = palette[random(palette.size.toFloat()).toInt()]
            this.life = random(500.0f, 2000.0f).toInt()
            this.dir = random(8.0f).toInt()
            this.step = if (random(1.0f) < 0.7f) 1 else 2
            trails.add(PVector(x.toFloat(), y.toFloat()))

            val idx = (x + 1 + (y + 1) * (width + 2))
            pixelChecker[idx] = true
        }

        fun update()
        {
            if (life <= 0)
            {
                return
            }
            
            var attempts = 0
            while (attempts < 8)
            {
                val newX = clamp(x + dirs[dir].x.toInt() * step, 0, width)
                val newY = clamp(y + dirs[dir].y.toInt() * step, 0, height)
                val idx = (newX + 1 + (newY + 1) * (width + 2))
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
            stroke(color)
            trails.forEach { trail -> point(trail.x, trail.y) }
        }

        fun trailCount(): Int = trails.size
    }
}
