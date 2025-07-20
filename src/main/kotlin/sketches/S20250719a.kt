package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PVector

class S20250719a : ExtendedPApplet(P3D)
{
    private val palettes = arrayOf(
//        createPalette("011627-fdfffc-2ec4b6-e71d36-ff9f1c"),
        createPalette("780000-c1121f-fdf0d5-003049-669bbc"),
        createPalette("e63946-f1faee-a8dadc-457b9d-1d3557"),
    )
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val viewFactor = 1.67f
    private val meshData = Icosahedron()
    private val mesh = HalfEdgeMesh()
    private val polygons = mutableListOf<FractalPolygon>()

    override fun setup()
    {
        mesh.buildMesh(meshData)
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }

        val error = mesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        perspective(2.0f * atan(viewFactor / 2.0f), aspect, 0.1f, 10.0f)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

        init()
        noLoop()
    }

    private fun init()
    {
        val remainFaces = mutableListOf<Int>()
        val initPolygons = mutableListOf<FractalPolygon>()
        val parentVertices = MutableList(mesh.getVertexCount() / 4) { it }

        parentVertices.forEach { v ->
            val adjacent = mesh.getAdjacentVertices(v).reversed()
            initPolygons.add(FractalPolygon(
                adjacent.map { mesh.getVertexPosition(it) ?: return@forEach },
                mesh.getVertexPosition(v) ?: return@forEach,
            ))
        }

        // remain triangle polygons
        repeat(mesh.getFaceCount()) { remainFaces.add(it) }
        remainFaces.removeAll(parentVertices.map { mesh.getAdjacentFaces(it) }.flatten())
        remainFaces.forEach { face ->
            val vertices = mesh.getFaceVertices(face)
            initPolygons.add(FractalPolygon(
                vertices.map { mesh.getVertexPosition(it) ?: return@forEach },
                vertices.map { mesh.getVertexPosition(it) }.reduce { acc, v -> PVector.add(acc, v) }?.normalize() ?: return@forEach,
            ))
        }

        // division pentagon
        var tmpPentagons = initPolygons.toList()
        repeat(3) { tmpPentagons = tmpPentagons.flatMap { it.subdivision() } }

        // pentagon meshes
        polygons.clear()
        polygons.addAll(tmpPentagons)
    }

    override fun draw()
    {
        background(12.0f, 12.0f, 18.0f)

        pushStyle()
        noStroke()
        polygons.forEach { it.draw() }
        popStyle()

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    override fun keyPressed()
    {
        super.keyPressed()
        val seed = System.currentTimeMillis()
        randomSeed(seed)
        noiseSeed(seed)
        init()
        redraw()
    }

    private fun customRandom() = sigmoid(randomGaussian(), 0.62f)

    private fun project(pos: PVector): PVector
    {
        val k = (1.0f / (1.0f - signOrZero(pos.z) * sqrt(1.0f - pos.x * pos.x - pos.y * pos.y))).let { if (it.isFinite() && it > 0.0f) it else 1.0f }
        return PVector(pos.x * k, pos.y * k)
    }

    private inner class FractalPolygon(
        val vertices: List<PVector>,
        val center: PVector,
        private val palette: IntArray = palettes.random(),
    )
    {
        fun subdivision(): List<FractalPolygon>
        {
            val middles = vertices.mapIndexed { i, v0 ->
                val v1 = vertices[(i + 1) % vertices.size]
                PVector.lerp(v0, v1, 0.5f).normalize()
            }

            val centerVertices = middles.map { mid ->
                PVector.lerp(mid, center, customRandom()).normalize()
            }

            val ret = vertices.mapIndexed { i, _ ->
                val i2 = (i + 1) % vertices.size
                val newVertices = listOf(vertices[i2], middles[i2], centerVertices[i2], centerVertices[i], middles[i])
                FractalPolygon(newVertices, calcCenter(newVertices), palette)
            }.toMutableList()

            ret.add(FractalPolygon(centerVertices, center, palette))

            return ret
        }

        private fun calcCenter(points: List<PVector>): PVector = points.reduce { acc, v -> PVector.add(acc, v) }.normalize()

        fun draw()
        {
            val projVertPos = vertices.map { project(it) }
            if (projVertPos.none { it.x >= -aspect * viewFactor && it.x <= aspect * viewFactor && it.y >= -viewFactor && it.y <= viewFactor }) return
            val projCenter = project(center)
            val colors = palette.toList().shuffled().take(2)

            pushStyle()
            noStroke()
            beginShape(TRIANGLE_FAN)
            fill(colors[0])
            vertex(projCenter.x, projCenter.y, calcHeight(projCenter))
            fill(colors[1])
            projVertPos.forEach {
                vertex(it.x, it.y, calcHeight(it))
            }
            vertex(projVertPos.first().x, projVertPos.first().y, calcHeight(projVertPos.first()))
            endShape()
            popStyle()
        }

        private fun calcHeight(coord: PVector): Float
        {
            return noise((coord.x + 100.0f) * 2.3f, (coord.y + 200.0f) * 2.3f) * 0.6f
        }
    }
}
