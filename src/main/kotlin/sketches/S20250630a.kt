package sketches
import mesh.HalfEdgeMesh
import mesh.MeshData
import processing.core.PVector


class S20250630a : ExtendedPApplet(P3D)
{
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val fov = HALF_PI
    private val far = 10.0f
    private val icosphere = HalfEdgeMesh()

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

        icosphere.buildMesh(Icosahedron())
        icosphere.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        icosphere.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        icosphere.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        val error = icosphere.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

//        noLoop()
    }

    override fun draw()
    {
        background(0)

        push()
        rotateY(frameCount * 0.005f)
        rotateX(frameCount * 0.01f)
        stroke(255)
        fill(50.0f, 100.0f, 150.0f, 255.0f)
        icosphere.forEachFace { _, vertices ->
            beginShape()
            vertices.forEach {
                val p = icosphere.getVertexPosition(it) ?: return@forEach
                vertex(p.x, p.y, p.z)
            }
            endShape()
        }
        pop()
    }

    override fun keyPressed()
    {
        super.keyPressed()
        redraw()
    }

    private inner class Icosahedron : MeshData
    {
        private val vertices: MutableList<PVector> = mutableListOf()
        private val faces: MutableList<Triple<Int, Int, Int>> = mutableListOf()

        init
        {
            val hRad = TWO_PI / 5.0f
            val vRad = atan(0.5f)

            // create vertices
            vertices.add(PVector(0.0f, 0.0f, 1.0f)) // top
            // middle 10 vertices
            val r = cos(vRad)
            val z = sin(vRad)
            for (i in 0 until 5)
            { // first row
                val rad = hRad * i
                vertices.add(PVector(r * cos(rad), r * sin(rad), z))
            }
            for (i in 0 until 5)
            { // second row
                val rad = hRad * i + hRad / 2.0f
                vertices.add(PVector(r * cos(rad), r * sin(rad), -z))
            }
            vertices.add(PVector(0.0f, 0.0f, -1.0f)) // bottom

            // create CCW faces
            for (i in 1 .. 5)
            { // 0 and 1--5
                faces.add(Triple(0, i, i % 5 + 1))
            }
            for (i in 1 .. 5)
            { // middle 10 faces
                faces.add(Triple(i, i + 5, i % 5 + 1))
                faces.add(Triple(i % 5 + 1, i + 5, i % 5 + 6))
            }
            for (i in 1 .. 5)
            { // 11 and 6--10
                faces.add(Triple(i % 5 + 6, i + 5, 11))
            }
        }

        override fun getVertices(): List<PVector> = vertices.toList()

        override fun getFaces(): List<Triple<Int, Int, Int>> = faces.toList()

        fun draw()
        {
            faces.forEach {
                beginShape()
                vertex(vertices[it.first].x, vertices[it.first].y, vertices[it.first].z)
                vertex(vertices[it.second].x, vertices[it.second].y, vertices[it.second].z)
                vertex(vertices[it.third].x, vertices[it.third].y, vertices[it.third].z)
                endShape()
            }
        }
    }
}
