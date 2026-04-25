package sketches

import processing.core.PVector
import util.Quaternion


@Suppress("unused")
class S20260425a : ExtendedPApplet(P3D, false)
{
    private val fov = PI * 0.5f
    private val far = 10.0f
    private val fogShader by lazy { loadShader(
        resourcePath("shaders/fog.frag")!!,
        resourcePath("shaders/fog.vert")!!,
    ) }
    private val fogColor = 0xff121216.toInt()

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
        camera(0.0f, 0.0f, 0.0f, 0.0f, -0.5f, -2.0f, 0.0f, 1.0f, 0.0f)

        fogShader.set("fogRange", 2.0f, 5.0f)
        fogShader.set("fogColor", red(fogColor) / 255.0f, green(fogColor) / 255.0f, blue(fogColor) / 255.0f)

        noLoop()
    }

    override fun draw()
    {
        background(fogColor)

        val seed = random(Int.MAX_VALUE.toFloat())

        shader(fogShader)
        repeat(200) {
            val ymax = tan(fov * 0.5f)
            val u = random(0.0f, 1.0f)
            val x = sign(u - 0.5f) * pow(abs(2.0f * u - 1.0f), 2.6f)
            var y = randomGaussian()
            while (y !in -1.0f .. 1.0f)
            {
                y = randomGaussian()
            }
            val cdir = PVector(
                x * ymax * aspect,
                lerp(y, noise(x * 16.0f, seed) * 2.0f - 1.0f, 0.6f) * 0.5f * ymax,
                -random(1.0f),
            ).normalize()
            val dir = viewToWorld(cdir).normalize()
            val prism = CircularPrism(0.3f, 4.0f, PVector.mult(dir, 5.0f))
            prism.draw(2000)
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

    inner class CircularPrism(val radius: Float, val height: Float, val bottomCenter: PVector)
    {
        val seed = random(Int.MAX_VALUE.toFloat())
        val maxROff = random(1.0f, 2.5f)
        val noiseScale = random(2.0f, 4.0f)
        val col = color(random(4.0f, 12.0f).toInt() / 20.0f * 255.0f)

        fun randomPosition(): Circle
        {
            val z0 = random(0.0f, 1.0f)
            val y = sq(z0) * height
            val r = radius * (1.0f - sq(z0))
            val phi = random(0.0f, TAU)
            val rOff = maxROff * radius * noise(z0 * noiseScale, seed)
            val phiOff = TAU * noise(z0 * noiseScale, seed + 100.0f)
            val x = r * sin(phi) + rOff * sin(phiOff)
            val z = r * cos(phi) + rOff * cos(phiOff)

            val axis1 = PVector(0.0f, 1.0f, 0.0f)
            val axis2 = PVector.mult(bottomCenter, -1.0f).normalize()
            val qua = Quaternion.frontToRotation(axis1, axis2)

            val pos =  rotate(qua, PVector(x, y, z)).add(bottomCenter)
            val ext = radius * (0.1f + sqrt(1.0f - sq(z0))) * 0.2f
            return Circle(ext, pos, col)
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
