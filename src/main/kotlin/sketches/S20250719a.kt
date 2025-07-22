package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PVector

class S20250719a : ExtendedPApplet(P3D)
{
    private val palette = createPalette("03045e-023e8a-0077b6-0096c7-00b4d8-48cae4-90e0ef-ade8f4-caf0f8-03071e-370617-6a040f-9d0208-d00000-dc2f02-e85d04-f48c06-faa307-ffba08")
    private val viewFactor = 1.77f
    private val meshData = Icosahedron()
    private val mesh = HalfEdgeMesh()
    private val polygons = mutableListOf<FractalPolygon>()

    override fun setup()
    {
        mesh.buildMesh(meshData)
        repeat(2) { mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() } }

        val error = mesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        perspective(2.0f * atan(viewFactor / 2.0f), aspect, 0.1f, 10.0f)
        camera(0.0f, 0.0f, 2.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        hint(DISABLE_DEPTH_TEST)

        init()
        noLoop()
    }

    private fun init()
    {
        val remainFaces = mutableListOf<Int>()
        val initPolygons = mutableListOf<FractalPolygon>()
        val parentVertices = MutableList(mesh.getVertexCount() / 4) { it }

        // add pentagon meshes
        parentVertices.forEach { v ->
            val adjacent = mesh.getAdjacentVertices(v).reversed()
            initPolygons.add(FractalPolygon(
                adjacent.map { mesh.getVertexPosition(it) ?: return@forEach },
                mesh.getVertexPosition(v) ?: return@forEach,
            ))
        }

        // add remained triangle meshes
        repeat(mesh.getFaceCount()) { remainFaces.add(it) }
        remainFaces.removeAll(parentVertices.map { mesh.getAdjacentFaces(it) }.flatten())
        remainFaces.forEach { face ->
            val vertices = mesh.getFaceVertices(face)
            initPolygons.add(FractalPolygon(
                vertices.map { mesh.getVertexPosition(it) ?: return@forEach },
                vertices.map { mesh.getVertexPosition(it) }.reduce { acc, v -> PVector.add(acc, v) }?.normalize() ?: return@forEach,
            ))
        }

        // division polygons
        var tmpPolygon = initPolygons.toList()
        repeat(4) { tmpPolygon = tmpPolygon.flatMap { it.subdivision() } }

        polygons.clear()
        polygons.addAll(tmpPolygon)
    }

    override fun draw()
    {
        background(PAPER_WHITE)

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

    private fun customRandom() = clamp(randomGaussian() * 1.7f, -1.0f, 1.0f) + 0.5f

    private fun project(pos: PVector): PVector
    {
        val k = (1.0f / (1.0f - signOrZero(pos.z) * sqrt(1.0f - pos.x * pos.x - pos.y * pos.y))).let { if (it.isFinite() && it > 0.0f) it else 1.0f }
        return PVector(pos.x * k, pos.y * k)
    }

    private fun applyStyle(style: Style, color: Int)
    {
        when (style)
        {
            Style.FILL ->
            {
                noStroke()
                fill(color, 160.0f)
            }
            Style.STROKE ->
            {
                noFill()
                stroke(color, 80.0f)
            }
        }
    }

    private enum class Style
    {
        FILL,
        STROKE,
    }

    private inner class FractalPolygon(
        private val vertices: List<PVector>,
        private val center: PVector,
        private val style: Style = Style.entries.random(),
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
                FractalPolygon(newVertices, calcCenter(newVertices), style)
            }.toMutableList()

            ret.add(FractalPolygon(centerVertices, calcCenter(centerVertices), style))

            return ret
        }

        private fun calcCenter(points: List<PVector>): PVector = points.reduce { acc, v -> PVector.add(acc, v) }.normalize()

        fun draw()
        {
            val projVertPos = vertices.map { project(it) }
            val projCenter = project(center)
            val colors = palette.toList().shuffled().take(2)

            if (projVertPos.none { it.x >= -aspect * viewFactor && it.x <= aspect * viewFactor && it.y >= -viewFactor && it.y <= viewFactor }) return // out of range
            if (projVertPos.any { PVector.sub(it, projCenter).magSq() > sq(0.3f) }) return // too large

            pushStyle()
            beginShape(TRIANGLE_FAN)
            applyStyle(style, colors[0])
            vertex(projCenter.x, projCenter.y, calcHeight(projCenter))
            applyStyle(style, colors[1])
            projVertPos.forEach {
                vertex(it.x, it.y, calcHeight(it))
            }
            vertex(projVertPos.first().x, projVertPos.first().y, calcHeight(projVertPos.first()))
            endShape()
            popStyle()
        }

        private fun calcHeight(coord: PVector): Float
        {
            return noise((coord.x + 100.0f) * 2.3f, (coord.y + 200.0f) * 2.3f)
        }
    }
}
