package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PVector

class S20250719a : ExtendedPApplet(P3D)
{
    private val palette = createPalette("001219-005f73-0a9396-94d2bd-e9d8a6-ee9b00-ca6702-bb3e03-ae2012-9b2226")
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val fov = HALF_PI
    private val far = 100.0f
    private val meshData = Icosahedron()
    private val mesh = HalfEdgeMesh()
    private val pentagons = mutableListOf<Pentagon>()
    private val remainFaces = mutableListOf<Int>()

    override fun setup()
    {
        mesh.buildMesh(meshData)
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f) }
        val parentNumVertices = MutableList(mesh.getVertexCount()) { it }
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f) }

        val error = mesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        parentNumVertices.forEach { v ->
            val adjacent = mesh.getAdjacentVertices(v).reversed()
            pentagons.add(Pentagon(
                mesh.getVertexPosition(v) ?: return@forEach,
                adjacent.map { mesh.getVertexPosition(it) ?: return@forEach },
                mesh.getVertexNormal(v) ?: return@forEach,
                adjacent.map { mesh.getVertexNormal(it) ?: return@forEach },
                ))
        }

        repeat(mesh.getFaceCount()) { remainFaces.add(it) }
        remainFaces.removeAll(parentNumVertices.map { mesh.getAdjacentFaces(it) }.flatten())

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
//        noStroke()
        stroke(0.0f)

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

    private inner class Pentagon(private val center: PVector,
                                 private val vertices: List<PVector>,
                                 private val cNormals: PVector,
                                 private val vNormals: List<PVector>,
    )
    {
        private val color = palette.random()

        fun subdivision()
        {

        }

        fun draw()
        {
            pushStyle()
            fill(color)
            vertices.forEachIndexed { i, p0 ->
                beginShape()
                val p1 = vertices[(i + 1) % vertices.size]
                val n0 = vNormals.getOrNull(i) ?: return@forEachIndexed
                val n1 = vNormals.getOrNull((i + 1) % vertices.size) ?: return@forEachIndexed
                normal(cNormals.x, cNormals.y, cNormals.z)
                vertex(center.x, center.y, center.z)
                normal(n0.x, n0.y, n0.z)
                vertex(p0.x, p0.y, p0.z)
                normal(n1.x, n1.y, n1.z)
                vertex(p1.x, p1.y, p1.z)
                endShape()
            }
            popStyle()
        }
    }
}
