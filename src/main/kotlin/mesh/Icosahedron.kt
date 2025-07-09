package mesh

import processing.core.PApplet.*
import processing.core.PConstants.TWO_PI
import processing.core.PVector

class Icosahedron : MeshData
{
    override val vertices: MutableList<PVector> = mutableListOf()
    override val faces: MutableList<Triple<Int, Int, Int>> = mutableListOf()

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
}
