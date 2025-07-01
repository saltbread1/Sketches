package sketches
import processing.core.PVector


class S20250630a : ExtendedPApplet(P3D)
{
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val fov = HALF_PI
    private val far = 10.0f

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

//        noLoop()
    }

    override fun draw()
    {
        val icosahedron = Icosahedron()

        background(0)

        push()
        rotateY(frameCount * 0.005f)
        rotateX(frameCount * 0.01f)
        stroke(255)
        fill(50.0f, 100.0f, 150.0f, 255.0f)
        icosahedron.draw()
        pop()
    }

    override fun keyPressed()
    {
        super.keyPressed()
        redraw()
    }

    private data class Face(val v0: Int, val v1: Int, val v2: Int)

    private inner class Icosahedron
    {
        private val vertices: MutableList<PVector> = mutableListOf()
        private val faces: MutableList<Face> = mutableListOf()

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
                faces.add(Face(0, i, i % 5 + 1))
            }
            for (i in 1 .. 5)
            { // middle 10 faces
                faces.add(Face(i, i + 5, i % 5 + 1))
                faces.add(Face(i % 5 + 1, i + 5, i % 5 + 6))
            }
            for (i in 1 .. 5)
            { // 11 and 6--10
                faces.add(Face(i % 5 + 6, i + 5, 11))
            }
        }

        fun getVertices(): List<PVector> = vertices.toList()

        fun getFaces(): List<Face> = faces.toList()

        fun draw()
        {
            faces.forEach {
                beginShape()
                vertex(vertices[it.v0].x, vertices[it.v0].y, vertices[it.v0].z)
                vertex(vertices[it.v1].x, vertices[it.v1].y, vertices[it.v1].z)
                vertex(vertices[it.v2].x, vertices[it.v2].y, vertices[it.v2].z)
                endShape()
            }
        }
    }
}
