package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PShape
import processing.core.PVector


@Suppress("unused")
class S20260304a : ExtendedPApplet(P3D)
{
    private val fov = PI * 0.5f
    private val far = 10.0f
    private val envShader by lazy { loadShader(
        resourcePath("shaders/S20260304a/panorama.frag")!!,
        resourcePath("shaders/S20260304a/panorama.vert")!!,
    ) }

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
        camera(0.0f, 0.0f, 3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)

        noLoop()
    }

    override fun draw()
    {
        noiseSeed(System.currentTimeMillis())

        val envMesh = HalfEdgeMesh().apply { buildMesh(Icosahedron()) }
        repeat(6)
        {
            envMesh.subdivide { v0, v1 ->
                val v = PVector.lerp(v0, v1, random(0.3f, 0.7f)).normalize()
                v.mult((customNoise(v, 4.0f) - 0.5f) * 1.2f + 1.0f)
            }
        }
        val error = envMesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }
        val envSphere = createShape()
        envSphere.setStrokeWeight(0.0f)
        envSphere.setFill(color(255))
        createShapeFromMesh(envMesh, envSphere)

        background(0)
        shader(envShader)
        pushMatrix()
        scale(5.0f)
        envSphere.draw(g)
        popMatrix()

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    fun createShapeFromMesh(mesh: HalfEdgeMesh, shape: PShape)
    {
        shape.beginShape(TRIANGLES)
        mesh.forEachFace { _, vertices ->
            vertices.forEach {
                val n = mesh.getVertexNormal(it) ?: PVector()
                val v = mesh.getVertexPosition(it) ?: PVector()
                shape.normal(n.x, n.y, n.z)
                shape.vertex(v.x, v.y, v.z)
            }
        }
        shape.endShape()
    }

    fun customNoise(v0: PVector, scale: Float) : Float
    {
        val phi = atan2(v0.x, v0.z)
        return noise((cos(phi) * 0.5f + 0.5f) * scale, (sin(phi) * 0.5f + 1.5f) * scale, v0.y * scale)
    }

    override fun keyPressed()
    {
        super.keyPressed()
        redraw()
    }
}
