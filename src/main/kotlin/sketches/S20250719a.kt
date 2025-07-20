package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PVector

class S20250719a : ExtendedPApplet(P3D)
{
    private val palette = createPalette("001219-005f73-0a9396-94d2bd-e9d8a6-ee9b00-ca6702-bb3e03-ae2012-9b2226")
    private val eye = PVector(0.0f, 0.0f, 1.5f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val fov = HALF_PI
    private val far = 10.0f
    private val meshData = Icosahedron()
    private val mesh = HalfEdgeMesh()
    private val pentagons = mutableListOf<Pentagon>()
    private val remainFaces = mutableListOf<Int>()

    override fun setup()
    {
        mesh.buildMesh(meshData)
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f) }
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f) }
        val parentNumVertices = MutableList(mesh.getVertexCount()) { it }
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f) }

        val error = mesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        pentagons.clear()
        remainFaces.clear()
        val initPentagons = mutableListOf<Pentagon>()

        parentNumVertices.forEach { v ->
            val adjacent = mesh.getAdjacentVertices(v).reversed()
            initPentagons.add(Pentagon(
                adjacent.map { Attribute(
                    mesh.getVertexPosition(it) ?: return@forEach,
                    mesh.getVertexNormal(it) ?: return@forEach
                ) },
                Attribute(
                    mesh.getVertexPosition(v) ?: return@forEach,
                    mesh.getVertexNormal(v) ?: return@forEach
                ),
                ))
        }

        // remain triangle polygons
        repeat(mesh.getFaceCount()) { remainFaces.add(it) }
        remainFaces.removeAll(parentNumVertices.map { mesh.getAdjacentFaces(it) }.flatten())

        // division pentagon
        var tmpPentagons = initPentagons.toList()
        repeat(3)
        {
            tmpPentagons = tmpPentagons.flatMap { it.subdivision() }
        }

        // pentagon meshes
        pentagons.addAll(tmpPentagons)

        perspective(fov, aspect, 0.1f, far)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

//        noLoop()
    }

    override fun draw()
    {
        background(12.0f, 12.0f, 18.0f)

//        val lightPos = PVector(3.0f, -6.0f, 3.0f)
        val lightPos = PVector(0.0f, 0.0f, eye.z)
        val ambient = PVector(0.1f, 0.1f, 0.1f).mult(255.0f)
        val diffuse = PVector(0.8f, 0.8f, 0.8f).mult(255.0f)

        push()
        noStroke()
//        stroke(0.0f)

        pointLight(diffuse.x, diffuse.y, diffuse.z, lightPos.x, lightPos.y, lightPos.z)
        ambientLight(ambient.x, ambient.y, ambient.z)
        specular(255.0f)
        shininess(60.0f)

        rotateY(frameCount * 0.005f)

        pentagons.forEach { it.draw() }

        fill(255.0f)
        remainFaces.forEach { face ->
            val vertices = mesh.getFaceVertices(face)
            beginShape()
            vertices.forEach inner@ { v ->
                val pos = mesh.getVertexPosition(v) ?: return@inner
                val nor = mesh.getVertexNormal(v) ?: return@inner
                normal(nor.x, nor.y, nor.z)
                vertex(pos.x, pos.y, pos.z)
            }
            endShape()
        }

        pop()
    }

    private inner class Attribute(val position: PVector, val normal: PVector)
    {
        fun vertex()
        {
            normal(normal.x, normal.y, normal.z)
            vertex(position.x, position.y, position.z)
        }

        fun add(attrib: Attribute) = Attribute(
            PVector.add(position, attrib.position),
            PVector.add(normal, attrib.normal).normalize(),
        )

        fun lerp(attrib: Attribute, amt: Float) = Attribute(
            PVector.lerp(position, attrib.position, amt),
            PVector.lerp(normal, attrib.normal, amt).normalize(),
        )
    }

    private inner class Pentagon(
        private val vertices: List<Attribute>,
        private val center: Attribute,
    )
    {
        private val color = palette.random()

        fun subdivision() : List<Pentagon>
        {
            val middles = vertices.mapIndexed { i, v0 ->
                val v1 = vertices[(i + 1) % vertices.size]
                v0.lerp(v1, 0.5f)
            }

            val centerVertices = middles.map { mid ->
                mid.lerp(center, 0.5f)
            }

            val ret = vertices.mapIndexed { i, _ ->
                val i2 = (i + 1) % vertices.size
                val newVertices = listOf(vertices[i2], middles[i2], centerVertices[i2], centerVertices[i], middles[i])
                val newCenter = newVertices.reduce { acc, v -> acc.add(v) }
                newCenter.position.div(newVertices.size.toFloat())
                Pentagon(newVertices, newCenter)
            }.toMutableList()

            ret.add(Pentagon(centerVertices, center))

            return ret
        }

        fun draw()
        {
            pushStyle()
            fill(color)
            vertices.forEachIndexed { i, attrib0 ->
                val attrib1 = vertices[(i + 1) % vertices.size]
                beginShape()
                center.vertex()
                attrib0.vertex()
                attrib1.vertex()
                endShape()
            }
            popStyle()
        }
    }
}
