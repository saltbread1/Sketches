package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PVector

class S20250719a : ExtendedPApplet(P3D)
{
    private val palette = createPalette("001219-005f73-0a9396-94d2bd-e9d8a6-ee9b00-ca6702-bb3e03-ae2012-9b2226")
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val meshData = Icosahedron()
    private val mesh = HalfEdgeMesh()
    private val polygons = mutableListOf<FractalPolygon>()
    private val viewFactor = 2.0f

    override fun setup()
    {
        mesh.buildMesh(meshData)
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        val parentNumVertices = MutableList(mesh.getVertexCount()) { it }
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }

        val error = mesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        val remainFaces = mutableListOf<Int>()
        val initPolygons = mutableListOf<FractalPolygon>()

        parentNumVertices.forEach { v ->
            val adjacent = mesh.getAdjacentVertices(v).reversed()
            initPolygons.add(FractalPolygon(
                adjacent.map { Attribute(
                    mesh.getVertexPosition(it) ?: return@forEach,
                    mesh.getVertexNormal(it) ?: return@forEach,
                ) },
                Attribute(
                    mesh.getVertexPosition(v) ?: return@forEach,
                    mesh.getVertexNormal(v) ?: return@forEach,
                ),
            ))
        }

        // remain triangle polygons
        repeat(mesh.getFaceCount()) { remainFaces.add(it) }
        remainFaces.removeAll(parentNumVertices.map { mesh.getAdjacentFaces(it) }.flatten())
        remainFaces.forEach { face ->
            val vertices = mesh.getFaceVertices(face)
            initPolygons.add(FractalPolygon(
                vertices.map { Attribute(
                    mesh.getVertexPosition(it) ?: return@forEach,
                    mesh.getVertexNormal(it) ?: return@forEach,
                ) },
                Attribute(
                    vertices.map { mesh.getVertexPosition(it) }.reduce { acc, v -> PVector.add(acc, v) }?.normalize() ?: return@forEach,
                    vertices.map { mesh.getVertexNormal(it) }.reduce { acc, v -> PVector.add(acc, v) }?.normalize() ?: return@forEach,
                ),
            ))
        }

        // division pentagon
        var tmpPentagons = initPolygons.toList()
        repeat(3) { tmpPentagons = tmpPentagons.flatMap { it.subdivision() } }

        // pentagon meshes
        polygons.clear()
        polygons.addAll(tmpPentagons)

//        perspective(PI * 0.3f, aspect, 0.1f, 10.0f)
        ortho(-aspect * viewFactor, aspect * viewFactor, -viewFactor, viewFactor, 0.0f, 10.0f)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

        noLoop()
    }

    override fun draw()
    {
        background(12.0f, 12.0f, 18.0f)

//        val lightPos = PVector(3.0f, -6.0f, 3.0f)
//        val lightPos = PVector(0.0f, 0.0f, eye.z)
//        val ambient = PVector(12.0f, 12.0f, 18.0f)
//        val diffuse = PVector(180.0f, 180.0f, 200.0f)

        pushStyle()
        noStroke()
//        stroke(0.0f)

//        pointLight(diffuse.x, diffuse.y, diffuse.z, lightPos.x, lightPos.y, lightPos.z)
//        ambientLight(ambient.x, ambient.y, ambient.z)
//        specular(255.0f)
//        shininess(60.0f)

//        polygons.forEach { it.draw() }

        polygons.forEach { poly ->
            val projVertPos = poly.vertices.map { project(it.position) }
            val projCenter = project(poly.center.position)
            if (projVertPos.none { it.x >= -aspect * viewFactor && it.x <= aspect * viewFactor && it.y >= -viewFactor && it.y <= viewFactor }) return@forEach
            fill(poly.color)
            beginShape(TRIANGLE_FAN)
            vertex(projCenter.x, projCenter.y)
            projVertPos.forEach { vertex(it.x, it.y) }
            vertex(projVertPos.first().x, projVertPos.first().y)
            endShape()
        }

        popStyle()
    }

    private fun customRandom() = sigmoid(randomGaussian(), 1.2f)

    private fun project(pos: PVector): PVector
    {
        val k = 1.0f - signOrZero(pos.z) * sqrt(1.0f - pos.x * pos.x - pos.y * pos.y).let { if (it.isFinite()) it else 1.0f }
        return PVector(pos.x / k, pos.y / k)
    }

    private inner class Attribute(val position: PVector, val normal: PVector)
    {
        fun vertex()
        {
            normal(normal.x, normal.y, normal.z)
            vertex(position.x, position.y, position.z)
        }

        fun lerp(attrib: Attribute, amt: Float) = Attribute(
            PVector.lerp(position, attrib.position, amt).normalize(),
            PVector.lerp(normal, attrib.normal, amt).normalize(),
        )
    }

    private inner class FractalPolygon(
        val vertices: List<Attribute>,
        val center: Attribute,
    )
    {
        val color = palette.random()

        fun subdivision(): List<FractalPolygon>
        {
            val middles = vertices.mapIndexed { i, v0 ->
                val v1 = vertices[(i + 1) % vertices.size]
                v0.lerp(v1, 0.5f)
            }

            val centerVertices = middles.map { mid ->
                mid.lerp(center, customRandom())
            }

            val ret = vertices.mapIndexed { i, _ ->
                val i2 = (i + 1) % vertices.size
                val newVertices = listOf(vertices[i2], middles[i2], centerVertices[i2], centerVertices[i], middles[i])
                FractalPolygon(newVertices, calcCenter(newVertices))
            }.toMutableList()

            ret.add(FractalPolygon(centerVertices, center))

            return ret
        }

        private fun calcCenter(points: List<Attribute>): Attribute
        {
            val ret = points.reduce { acc, v -> Attribute(
                    PVector.add(acc.position, v.position),
                    PVector.add(acc.normal, v.normal),
                )
            }
            ret.position.normalize()
            ret.normal.normalize()

            return ret
        }

        fun draw()
        {
            pushStyle()
            fill(color)
            beginShape(TRIANGLE_FAN)
            center.vertex()
            vertices.forEach { v -> v.vertex() }
            vertices.first().vertex()
            endShape()
            popStyle()
        }
    }
}
