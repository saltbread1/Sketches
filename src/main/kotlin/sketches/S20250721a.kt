package sketches

import mesh.HalfEdgeMesh
import mesh.data.Icosahedron
import processing.core.PVector
import kotlin.collections.map

class S20250721a : ExtendedPApplet(P3D, true)
{
    private val meshData = Icosahedron()
    private val mesh = HalfEdgeMesh()
    private val drawableFaces: MutableList<DrawableFace> = mutableListOf()
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)

    override fun setup()
    {
        mesh.buildMesh(meshData)
        repeat(5) { mesh.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() } }

        val error = mesh.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        perspective(PI * 0.3f, aspect, 0.1f, 10.0f)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

        colorMode(RGB, 1.0f, 1.0f, 1.0f, 1.0f)

        init()
        noLoop()
    }

    private fun init()
    {
        val remainedFaces = List(mesh.getFaceCount()) { it }.shuffled().toMutableList()
        val initSize = remainedFaces.size

        val removedFaces = removeFaces(remainedFaces.toMutableList(), (initSize * 0.64f).toInt())

        drawableFaces.clear()
        drawableFaces += getContinuousFaces(remainedFaces, (initSize * 0.78f).toInt())
            .map { face -> DrawableFace(face, mesh.getFaceVertices(face).map { calcContinentColor(mesh.getVertexPosition(it) ?: PVector()) }) }
        drawableFaces += remainedFaces.map { face -> DrawableFace(face, mesh.getFaceVertices(face).map { calcOceanColor(mesh.getVertexPosition(it) ?: PVector()) }) }
        drawableFaces.removeIf { it.face in removedFaces }
    }

    private fun removeFaces(remainedFaces: MutableList<Int>, finishRemainedSize: Int): List<Int>
    {
        val ret = mutableListOf<Int>()
        ret += remainedFaces.removeFirst()
        var a = 0.0f
        while (remainedFaces.size > finishRemainedSize)
        {
            val last = ret.last()
            val neighbors = mesh.getFaceNeighbors(last).filter { it in remainedFaces }
            if (neighbors.isEmpty() || random(a * a) > 0.1f)
            {
                val next = remainedFaces.removeFirst()
                remainedFaces -= next
                ret += next
                a = 0.0f
                continue
            }
            val next = neighbors.random()
            remainedFaces -= next
            ret += next
            a += 0.005f
        }

        while (true)
        {
            val removedFaces = remainedFaces.filter { face -> mesh.getFaceNeighbors(face).count { it in ret } >= 2 }
            if (removedFaces.isEmpty()) break
            remainedFaces -= removedFaces
            ret += removedFaces
        }

        return ret
    }

    private fun getContinuousFaces(remainedFaces: MutableList<Int>, finishRemainedSize: Int): List<Int>
    {
        val ret = removeFaces(remainedFaces, finishRemainedSize).toMutableList()

        repeat(2)
        {
            val neighbors = ret.flatMap { mesh.getFaceNeighbors(it) }.filter { it in remainedFaces }
            remainedFaces -= neighbors
            ret += neighbors
        }

        return ret
    }

    private fun positionNoise(p: PVector, n: Float) = noise((p.x + 100.0f) * n, (p.y + 200.0f) * n, (p.z + 300.0f) * n)

    private fun calcContinentColor(p: PVector): Int
    {
        val n = positionNoise(p, 2.1f)
        val r = 0.4f * n * n
        val g = n
        val b = 0.3f * n
        return color(r, g, b, 1.0f)
    }

    private fun calcOceanColor(p: PVector): Int
    {
        val n = positionNoise(p, 3.4f)
        val r = 0.3f * n * n
        val b = sqrt(n)
        val g = 0.3f * n
        return color(r, g, b, 1.0f)
    }

    private fun calcCoreColor(p: PVector): Int
    {
        val n = positionNoise(p, 4.2f)
        val r = min(sqrt(n) * 1.3f, 1.0f)
        val g = 0.6f * n * n
        val b = 0.0f
        return color(r, g, b, 1.0f)
    }

    override fun draw()
    {
        background(0)

        pushStyle()
        noStroke()
        pointLight(0.96f, 0.88f, 0.88f, 0.0f, 0.0f, eye.z)
        pointLight(0.56f, 0.56f, 0.59f, 0.0f, 0.0f, 0.0f)
        ambientLight(0.07f, 0.07f, 0.15f)
        pushMatrix()
        rotateZ(random(TAU))
        rotateY(random(TAU))
        rotateX(random(TAU))
        drawableFaces.forEach { it.draw() }
        popMatrix()
        popStyle()

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    override fun keyPressed()
    {
        val seed = System.currentTimeMillis()
        randomSeed(seed)
        noiseSeed(seed)
        init()
        redraw()
    }

    private inner class DrawableFace(val face: Int, private val vertexColors: List<Int>)
    {
        private val faceNormal = mesh.getFaceNormal(face) ?: PVector()
        private val centerPosition = PVector.mult(faceNormal, random(0.62f, 0.84f))
        private val vertexPositions: List<PVector>
        private val vertexNormals: List<PVector>
        private val centerNormal: PVector
        private val centerColor: Int

        init
        {
            val vertices = mesh.getFaceVertices(face)
            val grav = vertices.map { mesh.getVertexPosition(it) }
                .reduce { acc, v -> PVector.add(acc, v) }?.div(vertices.size.toFloat())
                ?: PVector()
            val k = sigmoid(positionNoise(grav, 3.4f) * 2.0f - 1.0f, 2.9f) * 0.2f
            vertexPositions = vertices.map {
                val p = mesh.getVertexPosition(it) ?: PVector()
                PVector.mult(faceNormal, k).add(p)
            }
            vertexNormals = vertices.map { mesh.getVertexNormal(it) ?: PVector() }
            centerNormal = PVector.mult(faceNormal, -1.0f)
            centerColor = calcCoreColor(grav)
        }

        fun draw()
        {
            drawSurface()
            vertexPositions.forEachIndexed { i, _ -> drawSide(i, (i + 1) % vertexPositions.size) }
        }

        private fun drawSurface()
        {
            beginShape()
            vertexPositions.forEachIndexed { i, p ->
                val n = vertexNormals[i]
                fill(vertexColors[i])
                normal(n.x, n.y, n.z)
                vertex(p.x, p.y, p.z)
            }
            endShape()
        }

        private fun drawSide(i0: Int, i1: Int)
        {
            val p0 = vertexPositions[i0]
            val p1 = vertexPositions[i1]
            val n0 = vertexNormals[i0]
            val n1 = vertexNormals[i1]
            beginShape()
            fill(sideColor(vertexColors[i1]))
            normal(n1.x, n1.y, n1.z)
            vertex(p1.x, p1.y, p1.z)
            fill(sideColor(vertexColors[i0]))
            normal(n0.x, n0.y, n0.z)
            vertex(p0.x, p0.y, p0.z)
            fill(centerColor)
            normal(centerNormal.x, centerNormal.y, centerNormal.z)
            vertex(centerPosition.x, centerPosition.y, centerPosition.z)
            endShape()
        }

        private fun sideColor(color: Int): Int
        {
            val r = red(color)
            val g = green(color)
            val b = blue(color)

            return color(r * r, g * g, b * b, 1.0f)
        }
    }
}
