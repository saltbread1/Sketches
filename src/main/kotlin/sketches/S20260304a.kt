package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PVector
import processing.opengl.PGraphicsOpenGL


@Suppress("unused")
class S20260304a : ExtendedPApplet(P3D)
{
    private val fov = PI * 0.5f
    private val far = 100.0f
    private val panorama by lazy {
        loadImage(resourcePath("textures/scythian_tombs_2_1k.jpg"))
    }
    private val envShader by lazy { loadShader(
        resourcePath("shaders/S20260304a/panorama.frag")!!,
        resourcePath("shaders/S20260304a/panorama.vert")!!,
    ) }
    private val objShader by lazy { loadShader(
        resourcePath("shaders/S20260304a/refraction.frag")!!,
        resourcePath("shaders/S20260304a/refraction.vert")!!,
    ) }
    private val icosphereMesh = HalfEdgeMesh()
    private val envSphere by lazy { createShape() }
    private val cube by lazy { createCubeFlat(1.0f) }
    private val sphere by lazy { createShape(SPHERE, 1.0f).apply {
        setStrokeWeight(0.0f)
        setFill(color(255))
    } }
    private val eye = PVector(0.0f, 0.0f, 2.0f)

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)

        envShader.set("panorama", panorama)
        objShader.set("panorama", panorama)
        objShader.set("eta", 0.64f)

        icosphereMesh.buildMesh(Icosahedron())
        repeat(6)
        {
            icosphereMesh.subdivide { v0, v1 ->
                val v = PVector.lerp(v0, v1, random(0.3f, 0.7f)).normalize()
                v.mult((customNoise(v, 4.0f) - 0.5f) * 1.2f + 1.0f)
            }
        }
        val error = icosphereMesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        envSphere.beginShape(TRIANGLES)
        icosphereMesh.forEachFace { _, vertices ->
            vertices.forEach {
                val n = icosphereMesh.getVertexNormal(it) ?: PVector()
                val v = icosphereMesh.getVertexPosition(it) ?: PVector()
                envSphere.normal(n.x, n.y, n.z)
                envSphere.vertex(v.x, v.y, v.z)
            }
        }
        envSphere.noStroke()
        envSphere.endShape()
        envSphere.setFill(color(255))
    }

    override fun draw()
    {
        background(NIGHT_BLACK)

        val rad = frameCount * 0.01f

        val eyeDist = eye.mag()
        eye.set(sin(rad), 0.0f, cos(rad)).mult(eyeDist)
        camera(eye.x, eye.y - 0.9f, eye.z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        objShader.set("eyePosition", eye)

        val pg = this.g as PGraphicsOpenGL
        objShader.set("tViewMatrix", pg.camera.get()) // row-major (=transposed column-major)

        val mMat = pg.modelview.get().apply {
            preApply(pg.cameraInv)
            transpose() // row-major to column major
        }
        objShader.set("modelMatrix", mMat)

        pushMatrix()
        scale(10.0f)
        shader(envShader)
        envSphere.draw(g)
        popMatrix()

        pushMatrix()
        scale(0.75f)
        shader(objShader)
        sphere.draw(g)
        popMatrix()

        resetShader()
    }

    fun customNoise(v0: PVector, scale: Float) : Float
    {
        val phi = atan2(v0.x, v0.z)// / TAU + 0.5f
//        val theta = asin(v0.y) / PI + 0.5f
        return noise((cos(phi) * 0.5f + 0.5f) * scale, (sin(phi) * 0.5f + 1.5f) * scale, v0.y * scale)
    }
}
