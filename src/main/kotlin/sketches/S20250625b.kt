package sketches

import processing.core.PApplet
import processing.core.PVector
import util.*

class S20250625b : PApplet()
{
    private val palette = createPalette("001219-005f73-0a9396-94d2bd-e9d8a6-ee9b00-ca6702-bb3e03-ae2012-9b2226")
    private val polygons = mutableListOf<Polygon>()
    private var aspect = 0.0f
    private val far = 100.0f
    private val isSave = false

    override fun settings()
    {
        if (isSave)
        { // 4K
            size(1920, 1080, P3D)
            pixelDensity(2)
        }
        else
        {
            size(1280, 720, P3D)
            pixelDensity(1)
        }
    }

    override fun setup()
    {
        this.aspect = width.toFloat() / height.toFloat()
        val fov: Float = HALF_PI
        perspective(fov, aspect, 0.1f, far)
        camera(0.0f, 0.0f, 1.0f / tan(fov / 2.0f), 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        init()
        noLoop()
    }

    override fun draw()
    {
        background(0xfff2f2f6.toInt())

        noStroke()
        polygons.forEach {
            fill(palette[random(palette.size.toFloat()).toInt()])
            it.draw()
        }

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    private fun init()
    {
        polygons.clear()
        while (addPolygon(100, PVector(-aspect, -1.0f), PVector(aspect, 1.0f)))
        {
            // none
        }
    }

    private fun addPolygon(triableItr: Int, regionMin: PVector, regionMax: PVector): Boolean
    {
        repeat(triableItr)
        {
            val c = PVector(
                random(regionMin.x, regionMax.x),
                random(regionMin.y, regionMax.y),
            )
            var rand = 0.0f
            while (rand < 0.1f) { rand = random(1.0f) }
            val r = easeInPolynomial(rand, 2.0f) * min(regionMax.x - regionMin.x, regionMax.y - regionMin.y) * 0.15f
            val n = random(3.0f, 10.0f).toInt()
            val polygon = Polygon(c, r, n)
            if (polygons.none { it.isOverlapped(polygon) })
            {
                polygons.add(polygon)
                return true
            }
        }
        return false
    }

    override fun keyPressed()
    {
        if (key == ESC)
        {
            return
        }
        val seed = System.currentTimeMillis()
        noiseSeed(seed)
        randomSeed(seed)
        init()
        redraw()
    }

    private inner class Polygon
    {
        private val center: PVector
        private val radius: Float
        private val vertices = mutableListOf<PVector>()

        constructor(center: PVector, radius: Float, numVertices: Int)
        {
            this.center = center
            this.radius = radius
            for (i in 0 until numVertices)
            {
                val angle = TWO_PI * i / numVertices
                val vertex = PVector(
                    center.x + cos(angle) * radius,
                    center.y + sin(angle) * radius,
                )
                vertices.add(vertex)
            }
        }

        fun draw()
        {
            beginShape()
            vertices.forEach { vertex -> vertex(vertex.x, vertex.y, vertex.z) }
            endShape(CLOSE)
        }

        private fun pointInside(point: PVector, polygon: Polygon): Boolean
        {
            val vertices = polygon.vertices
            return vertices.indices.count { i ->
                val p0 = vertices[i]
                val p1 = vertices[(i + 1) % vertices.size]
                val v = PVector.sub(p1, p0)
                val pv = PVector.sub(point, p0)
                val c = PVector.cross(v, pv, null)
                c.z >= 0.0f
            } == vertices.size
        }

        private fun linesIntersect(p1: PVector, p2: PVector, q1: PVector, q2: PVector): Boolean
        {
            val v1 = PVector.sub(p2, p1)
            val v2 = PVector.sub(q1, p1)
            val v3 = PVector.sub(q2, p1)

            val s = PVector.cross(v1, v2, null).z
            val t = PVector.cross(v1, v3, null).z

            return s * t < 0.0f
        }

        fun isOverlapped(other: Polygon): Boolean
        {
            val d2 = PVector.sub(center, other.center).magSq()
            if (d2 > sq(radius + other.radius))
            { // not overlap each bounding circle
                return false
            }

            // Check if any vertex is inside the other polygon
            if (other.vertices.any { pointInside(it, this) } ||
                vertices.any { pointInside(it, other) })
            {
                return true
            }

            // Check if any edges intersect
            for (i in vertices.indices)
            {
                val p1 = vertices[i]
                val p2 = vertices[(i + 1) % vertices.size]
                for (j in other.vertices.indices)
                {
                    val q1 = other.vertices[j]
                    val q2 = other.vertices[(j + 1) % other.vertices.size]
                    if (linesIntersect(p1, p2, q1, q2))
                    {
                        return true
                    }
                }
            }
            return false
        }
    }
}
