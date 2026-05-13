package sketches

import processing.core.PVector
import util.Quaternion


@Suppress("unused")
class S20260512a : ExtendedPApplet(P3D)
{
    private val fov = PI * 0.5f
    private val far = 10.0f
    private val floorShader by lazy { loadShader(
        resourcePath("shaders/S20260512a/floor.frag")!!,
        resourcePath("shaders/S20260512a/floor.vert")!!,
    ) }
    private val eyeShader by lazy { loadShader(
        resourcePath("shaders/S20260512a/eye.frag")!!,
        resourcePath("shaders/S20260512a/eye.vert")!!,
    ) }
    private val vhsShader by lazy { loadShader("shaders/noise_vhs.frag")!! }
    private val bgColor = NIGHT_BLACK
    private val lightPosition = PVector(0.0f, -5.0f, 2.0f)

    override fun setup()
    {
        textureMode(NORMAL)

        perspective(fov, aspect, 0.1f, far)
        camera(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f)

        floorShader.set("lightPosition", lightPosition.x, lightPosition.y, lightPosition.z, 1.0f)
        floorShader.set("fogRange", far * 0.5f, far * 0.9f)
        floorShader.set("fogColor", red(bgColor) / 256.0f, green(bgColor) / 256.0f, blue(bgColor) / 256.0f)

        eyeShader.set("lightPosition", lightPosition.x, lightPosition.y, lightPosition.z, 1.0f)

        noLoop()
    }

    override fun draw()
    {
        background(bgColor)

        shader(floorShader)
        floor()

        shader(eyeShader)
        eye()

        resetShader()
        filter(vhsShader)

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    fun floor(y: Float = 0.6f)
    {
        val nz = 26
        val nx = nz * 3
        val cube = createCubeFlat()
        cube.setStrokeWeight(0.0f)
        cube.setFill(color(249, 222, 204))
        for (j in 0 until nz)
        {
            val z = lerp(0.0f, far, j / nz.toFloat())
            for (i in 0 until nx)
            {
                val x = lerp(-far, far, i / nx.toFloat())
                val pos = PVector(x, y, z)
                pushMatrix()
                translate(pos.x, pos.y + far * 0.5f, pos.z)
                scale(2.0f * far / nx * 0.3f, far * 0.5f, far / nz * 0.3f)
                cube.draw(g)
                popMatrix()
            }
        }
    }

    fun eye(position: PVector = PVector(-6.0f, -3.5f, far * 0.7f))
    {
        val eye = createHemiSphere().apply {
            setStrokeWeight(0.0f)
            setFill(color(255))
        }
        pushMatrix()
        translate(position.x, position.y, position.z)
        rotate(g, Quaternion.fromToRotation(PVector(0.0f, 0.0f, 1.0f), PVector.mult(position, -1.0f).normalize()))
        scale(3.0f)
        eye.draw(g)
        popMatrix()
    }
}
