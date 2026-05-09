package sketches

import processing.core.PVector
import util.Quaternion


@Suppress("unused")
class S20260425a : ExtendedPApplet(P3D)
{
    private val fov = PI * 0.5f
    private val far = 10.0f
    private val fogShader by lazy { loadShader(
        resourcePath("shaders/fog.frag")!!,
        resourcePath("shaders/fog.vert")!!,
    ) }
    private val fogColor = PAPER_WHITE

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
        camera(0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)

        fogShader.set("fogRange", 3.0f, 5.0f)
        fogShader.set("fogColor", red(fogColor) / 255.0f, green(fogColor) / 255.0f, blue(fogColor) / 255.0f)

        colorMode(HSB, 1.0f, 1.0f, 1.0f, 1.0f)

        noLoop()
    }

    override fun draw()
    {
        background(fogColor)

        shader(fogShader)
        repeat(200) {
            val ymax = tan(fov * 0.5f)
            val x = random(-2.0f, 2.0f)
            val y = noise(x * 8.0f, frameCount.toFloat()) * 2.0f - 1.0f + randomGaussian() * 0.2f
            val cdir = PVector(
                x * ymax * aspect,
                y * ymax,
                -1.0f,
            ).normalize()
            val dir = viewToWorld(cdir).normalize()
            val baseHue = random(8.0f).toInt() / 8.0f
            val prism = CircularPrism(
                random(0.25f, 0.45f),
                random(3.0f, 4.0f),
                PVector.mult(dir, 5.0f),
                random(1.0f, 2.5f),
                random(1.0f, 4.0f) * TAU) {
                    color(baseHue + random(0.16f), 1.0f, 1.0f)
            }
            prism.draw(4000)
        }
        repeat(1000) {
            val ymax = tan(fov * 0.5f)
            val x = random(-2.0f, 2.0f)
            val y = random(-2.0f, 2.0f)
            val cdir = PVector(
                x * ymax * aspect,
                y * ymax,
                -1.0f,
            ).normalize()
            val dir = viewToWorld(cdir).normalize()
            val baseHue = random(8.0f).toInt() / 8.0f
            val prism = CircularPrism(
                random(0.2f, 0.3f),
                random(2.0f, 3.0f),
                PVector.mult(dir, 5.0f),
                random(1.0f, 2.5f),
                random(1.0f, 2.0f) * TAU) {
                color(baseHue + random(0.08f), 0.8f, 0.6f)
            }
            prism.draw(500)
        }
        resetShader()

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

    data class Circle(val extent: Float, val center: PVector, val color: Int)

    inner class CircularPrism(
        val radius: Float,
        val height: Float,
        val bottomCenter: PVector,
        val maxROff: Float,
        val maxPhiOff: Float,
        val colorGetter: () -> Int)
    {
        val seed = random(Int.MAX_VALUE.toFloat())
        val noiseScale = random(2.0f, 16.0f)

        fun randomPosition(): Circle
        {
            val z0 = random(0.0f, 1.0f)
            val y = sq(z0) * height
            val r = radius * (1.0f - sq(z0))
            val phi = random(0.0f, TAU)
            val rOff = maxROff * radius * noise(z0 * noiseScale, seed)
            val phiOff = maxPhiOff * noise(z0 * noiseScale, seed + 100.0f)
            val x = r * sin(phi) + rOff * sin(phiOff)
            val z = r * cos(phi) + rOff * cos(phiOff)

            val axis1 = PVector(0.0f, 1.0f, 0.0f)
            val axis2 = PVector.mult(bottomCenter, -1.0f).normalize()
            val qua = Quaternion.frontToRotation(axis1, axis2)

            val pos =  rotate(qua, PVector(x, y, z)).add(bottomCenter)
            val ext = radius * (0.1f + sqrt(1.0f - sq(z0))) * 0.2f
            val extOff = sq(random(1.0f)) * ext
            return Circle(ext + extOff, pos, colorGetter.invoke())
        }

        fun draw(count: Int)
        {
            val circles = List(count) { randomPosition() }
            pushStyle()
            noStroke()
            for (c in circles)
            {
                pushMatrix()
                translate(c.center.x, c.center.y, c.center.z)
                applyBillboard()
                fill(c.color)
                circle(0.0f, 0.0f, c.extent)
                popMatrix()
            }
            popStyle()
        }
    }
}
