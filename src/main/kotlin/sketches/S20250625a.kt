package sketches

import processing.core.PApplet
import util.createPalette

class S20250625a : PApplet()
{
    private val palette = createPalette("d3cfdf-5db1b8-650f24-b10b24-1d38ac-0a1534-3a5431-bdb28d-2e140a-689578")
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
            size(720, 720, P2D)
            pixelDensity(1)
        }
    }

    override fun setup()
    {
        blendMode(ADD)
        noLoop()
    }

    override fun draw()
    {
        background(0xff000000.toInt())

        var gridWidth = 32
        var gridHeight = 32
        for (i in 0 until 5)
        {
            gridRects(gridWidth, gridHeight)
            gridWidth /= 2
            gridHeight /= 2
        }
    }

    private fun gridRects(gridWidth: Int, gridHeight: Int)
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
                translate(x, y)
                rotate(random(TWO_PI))
                rectMode(CENTER)
                noStroke()
                fill(palette[random(palette.size.toFloat()).toInt()], 77.0f)
                rect(0.0f, 0.0f, w * random(1.0f, 1.3f), h * random(1.0f, 1.3f))
                pop()
            }
        }
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
