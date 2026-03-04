package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PShape
import processing.core.PVector


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
    private val icosphere = HalfEdgeMesh()
    private val mesh = Icosahedron()
    private val sphere by lazy { createShape() }

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
//        ortho(-aspect, aspect, -1.0f, 1.0f, -0.1f, far)
        camera(0.0f, 0.1f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)

        envShader.set("panorama", panorama)

        icosphere.buildMesh(mesh)
        repeat(6)
        {
            icosphere.subdivide { v0, v1 ->
                val v = PVector.lerp(v0, v1, random(1.0f)).normalize()
                v.mult((customNoise(v, 4.0f) - 0.5f) * 1.2f + 1.0f)
            }
        }
        val error = icosphere.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        sphere.beginShape(TRIANGLES)
        icosphere.forEachFace { _, vertices ->
            vertices.forEach {
                val v = icosphere.getVertexPosition(it) ?: PVector()
                val n = icosphere.getVertexNormal(it) ?: PVector()
                sphere.normal(n.x, n.y, n.z)
                sphere.vertex(v.x, v.y, v.z)
            }
        }
        sphere.noStroke()
        sphere.setFill(color(255))
        sphere.endShape()
    }

    override fun draw()
    {
        background(NIGHT_BLACK)

        pushMatrix()
        scale(10.0f)
        rotateY(frameCount * 0.01f)
//        rotateY(PI)
        shader(envShader)
        sphere.draw(g)
        resetShader()
        popMatrix()
    }

    fun customNoise(v0: PVector, scale: Float) : Float
    {
        val phi = atan2(v0.x, v0.z)// / TAU + 0.5f
//        val theta = asin(v0.y) / PI + 0.5f
        return noise((cos(phi) * 0.5f + 0.5f) * scale, v0.y * scale)
    }

    fun createCube(size: Float) : PShape
    {
        val cube = createShape()
        cube.beginShape(TRIANGLES)

        // Bottom (y = -size)
        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)

        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)
        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)

        // Top (y = +size)
        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)

        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)

        // Front (z = -size)
        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)

        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)
        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)

        // Back (z = +size)
        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)

        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)

        // Left (x = -size)
        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f, -1.0f,  1.0f); cube.vertex(-size, -size,  size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)

        cube.normal(-1.0f, -1.0f, -1.0f); cube.vertex(-size, -size, -size)
        cube.normal(-1.0f,  1.0f,  1.0f); cube.vertex(-size,  size,  size)
        cube.normal(-1.0f,  1.0f, -1.0f); cube.vertex(-size,  size, -size)

        // Right (x = +size)
        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f,  1.0f, -1.0f); cube.vertex( size,  size, -size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)

        cube.normal( 1.0f, -1.0f, -1.0f); cube.vertex( size, -size, -size)
        cube.normal( 1.0f,  1.0f,  1.0f); cube.vertex( size,  size,  size)
        cube.normal( 1.0f, -1.0f,  1.0f); cube.vertex( size, -size,  size)

        cube.endShape()
        cube.setStrokeWeight(0.0f)
        cube.setFill(color(255))

        return cube
    }
}
