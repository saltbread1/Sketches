package sketches

import processing.core.PVector
import processing.opengl.PShader

class S20250625b : ExtendedPApplet(P3D)
{
    private val palette = createPalette("001219-005f73-0a9396-94d2bd-e9d8a6-ee9b00-ca6702-bb3e03-ae2012-9b2226")
    private val polygons3D = mutableListOf<Polygon3D>()
    private val eye = PVector(0.0f, -0.5f, 1.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val fov = HALF_PI
    private val far = 4.0f
    private val fogColor = 0xff121216.toInt()
    private val heightFogColor = 0x00f2f2f8.toInt()
    private var shader: PShader? = null

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

        shader = loadShader(
            this::class.java.classLoader.getResource("shaders/fog.frag")?.path,
            this::class.java.classLoader.getResource("shaders/fog.vert")?.path)
        shader?.set("fogColor", red(fogColor) / 255.0f, green(fogColor) / 255.0f, blue(fogColor) / 255.0f)
        shader?.set("fogRange", far * 0.2f, far)

        init()
        noLoop()
    }

    override fun draw()
    {
        background(fogColor)

        shader(shader)
        strokeWeight(0.5f)
        stroke(fogColor)
        polygons3D.forEach {
            val col = palette.random()
            fill(col)
            it.draw()
        }

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
    }

    private fun init()
    {
        val polygons = ArrayDeque<RegularPolygon>()
        while (addPolygon(polygons, 200))
        {
            // none
        }

        // convert to 3D
        val minDepth = polygons.asSequence().map { it.getCenter().y }.min()
        val maxDepth = polygons.asSequence().map { it.getCenter().y }.max()
        polygons3D.clear()
        while (polygons.isNotEmpty())
        {
            val polygon = polygons.removeFirst()
            val c = polygon.getCenter()
            val r = random(1.0f) + 0.5f
            val g = abs(randomGaussian())
            val s = easeInPolynomial((c.y - maxDepth) / (minDepth - maxDepth), 2.0f)
            val h = g * 0.04f + r * 0.2f
            val off = g * s
            polygons3D.add(Polygon3D(polygon, h, off))
        }
    }

    private fun addPolygon(polygons: MutableCollection<RegularPolygon>, triableItr: Int): Boolean
    {
        repeat(triableItr)
        {
            val p = getRandomPointInY0Plane()
            val c = PVector(p.x, p.z, 0.0f)
            var rand = 1.0f
            while (rand > 0.9f) { rand = random(1.0f) }
            val r = (1.0f - pow(rand, 0.36f)) * 0.13f
            val n = random(3.0f, 10.0f).toInt()
            val polygon = RegularPolygon(c, r, n)
            if (polygons.none { it.isOverlapped(polygon) })
            {
                polygons.add(polygon)
                return true
            }
        }
        return false
    }

    private fun getRandomPointInY0Plane(): PVector
    {
        val y = tan(fov * 0.5f)
        val cdir = PVector(
            random(-1.0f, 1.0f) * y * aspect,
            random(-1.0f, 1.0f) * y,
            -random(1.0f),
        ).normalize()
        return intersectY0Plane(cdir)
    }

    private fun intersectY0Plane(cdir: PVector): PVector
    {
        val dir = viewToWorld(cdir)
        var t = -eye.y / dir.y
        val maxT = far
        if (abs(dir.y) < 1e-4f || t < 0.0f || t > maxT)
        {
            t = maxT
        }
        val ret = PVector.mult(dir, t).add(eye)
        ret.y = 0.0f
        return ret
    }

    override fun keyPressed()
    {
        super.keyPressed()
        val seed = System.currentTimeMillis()
        noiseSeed(seed)
        randomSeed(seed)
        init()
        redraw()
    }

    private inner class RegularPolygon
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

        fun getCenter(): PVector = center.copy()

        fun getRadius(): Float = radius

        fun getVertices(): List<PVector> = vertices.toList()

        fun draw()
        {
            beginShape()
            vertices.forEach { vertex(it.x, it.y, it.z) }
            endShape(CLOSE)
        }

        private fun pointInside(point: PVector, polygon: RegularPolygon): Boolean
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

        fun isOverlapped(other: RegularPolygon): Boolean
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

    data class Attribute(val vertex: PVector, val color: Int)

    private inner class AttribPolygon(attributes: Collection<Attribute>)
    {
        private val attributes = mutableListOf<Attribute>()

        init
        {
            this.attributes.addAll(attributes)
        }

        fun draw()
        {
            beginShape()
            attributes.forEach {
                fill(it.color)
                vertex(it.vertex.x, it.vertex.y, it.vertex.z)
            }
            endShape(CLOSE)
        }
    }

    private inner class Polygon3D(bottomFace: RegularPolygon, height: Float, yOffset: Float)
    {
        private val surfaces = mutableListOf<AttribPolygon>()

        init
        {
            val vertices = bottomFace.getVertices()
            val hv = PVector(0.0f, -height, 0.0f)
            val col = palette.random()

            // top
            surfaces.add(AttribPolygon(vertices.asSequence()
                .map { Attribute(PVector(it.x, -yOffset * 1.3f, it.y).add(hv), col) }
                .toList())
            )

            // side
            vertices.forEachIndexed { idx, v ->
                val nv = vertices[(idx + 1) % vertices.size]
                val v0 = PVector(v.x, -yOffset, v.y)
                val v1 = PVector(nv.x, -yOffset, nv.y)
                val v2 = PVector.add(v1, hv)
                val v3 = PVector.add(v0, hv)
                surfaces.add(AttribPolygon(listOf(
                    Attribute(v0, heightFogColor),
                    Attribute(v1, heightFogColor),
                    Attribute(v2, col),
                    Attribute(v3, col),
                )))
            }
        }

        fun draw()
        {
            surfaces.forEach { it.draw() }
        }
    }
}
