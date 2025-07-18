package sketches

class S20250718a : ExtendedPApplet(P3D)
{
    private val palette = createPalette("001219-005f73-0a9396-94d2bd-e9d8a6-ee9b00-ca6702-bb3e03-ae2012-9b2226")
    private val gridRes = 2
    private val layer = 10
    private val far = layer + 1.0f

    override fun setup()
    {
        val scaleFactor = 0.9f
        ortho(-aspect * scaleFactor, aspect * scaleFactor, -scaleFactor, scaleFactor, -1.0f, far)
        camera(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        hint(DISABLE_DEPTH_TEST)
        noLoop()
    }

    override fun draw()
    {
        background(12.0f, 12.0f, 18.0f)

        val resX = gridRes//(gridRes * aspect).toInt()
        val resY = gridRes

        for (zi in layer - 1 downTo 0)
        {
            val z = -zi.toFloat()
            val alpha = easeInPolynomial(1.0f - zi.toFloat() / layer, 4.0f) * 192.0f
            pushMatrix()
            translate(0.0f, 0.0f, z)
            grid(resX, resY, 2, 9) {
                pushStyle()
                noStroke()
                fill(palette.random(), alpha)
                circle(0.0f, 0.0f, 2.0f)
                popStyle()
            }
            popMatrix()
        }
    }

    private fun grid(resX: Int, resY: Int, minDepth: Int, maxDepth: Int, action: () -> Unit)
    {
        fun gridImpl(currentDepth: Int = 0)
        {
            for (iy in 0 until resY)
            {
                for (ix in 0 until resX)
                {
                    val x = ((ix + customRandom()) / resX * 2.0f - 1.0f) * aspect
                    val y = (iy + customRandom()) / resY * 2.0f - 1.0f

                    pushMatrix()
                    translate(x, y)
                    scale(1.0f / resX, 1.0f / resY)
                    if (currentDepth >= minDepth && random(1.0f) < easeInPolynomial(currentDepth.toFloat() / maxDepth.toFloat(), 2.0f))
                    {
                        action.invoke()
                    }
                    else
                    {
                        gridImpl(currentDepth + 1)
                    }
                    popMatrix()
                }
            }
        }

        gridImpl()
    }

    private fun customRandom() = sigmoid(randomGaussian() * 1.2f)

    override fun keyPressed()
    {
        super.keyPressed()
        val seed = System.currentTimeMillis()
        randomSeed(seed)
        redraw()
    }
}
