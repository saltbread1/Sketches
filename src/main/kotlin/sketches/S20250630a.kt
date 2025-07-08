package sketches
import mesh.HalfEdgeMesh
import mesh.MeshData
import processing.core.PVector
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Random walk on the icosphere.
 */
class S20250630a : ExtendedPApplet(P3D)
{
    private val palette = createPalette("f72585-b5179e-7209b7-560bad-480ca8-3a0ca3-3f37c9-4361ee-4895ef-4cc9f0")
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val fov = HALF_PI
    private val far = 10.0f
    private val icosphere = HalfEdgeMesh()
    private val walkers = List(3) { Walker() }
    private val fps = 60.0f

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, far)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)

        icosphere.buildMesh(Icosahedron())
        icosphere.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        icosphere.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        icosphere.subdivide { v0, v1 -> PVector.lerp(v0, v1, 0.5f).normalize() }
        val error = icosphere.validate()
        if (error.isNotEmpty())
        {
            throw IllegalStateException("Invalid mesh state: $error")
        }

        walkers.forEach { it.init() }

        frameRate(fps)
    }

    override fun draw()
    {
        background(0)

        walkers.forEach { it.update() }

        pushMatrix()
        rotateY(frameCount * 0.008f)
        rotateX(frameCount * 0.013f)
        walkers.forEach { it.draw() }
        popMatrix()
    }

    override fun keyPressed()
    {
        super.keyPressed()
        redraw()
    }

    private inner class Icosahedron : MeshData
    {
        private val vertices: MutableList<PVector> = mutableListOf()
        private val faces: MutableList<Triple<Int, Int, Int>> = mutableListOf()

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

        override fun getVertices(): List<PVector> = vertices.toList()

        override fun getFaces(): List<Triple<Int, Int, Int>> = faces.toList()
    }

    private inner class Walker
    {
        private val elements: Queue<WalkerElement> = ConcurrentLinkedDeque()
        private val maxElements = 32

        fun init()
        {
            elements.clear()
            elements.add(WalkerElement(hashCode(), random(icosphere.getFaceCount().toFloat()).toInt()))
        }

        fun getElements(): List<WalkerElement> = elements.toList()

        fun update()
        {
            val newElement = WalkerElement(hashCode(), elements.last().getNextFace())
            newElement.stretch(300L) { easeOutPolynomial(it, 4.0f) }
            elements.add(newElement)

            if (elements.count { !it.isRemoved() } > maxElements)
            {
                val first = elements.first { !it.isRemoved() }
                first.remove()
                first.stretch(300L, { elements.remove(first) }) { easeOutPolynomial(1.0f - it, 4.0f) }
            }
        }

        fun draw()
        {
            elements.forEach { it.draw() }
        }
    }

    private inner class WalkerElement(private val group: Int, private val face: Int)
    {
        private val bottomFace: List<PVector>
        private val center: PVector
        private val normal: PVector
        private val color = palette.random()
        @Volatile private var alpha = 0.0f
        private val maxHeight: Float
        @Volatile private var currHeight = 0.0f
        private var stretchThread: Thread? = null
        @Volatile private var isStretchInterrupted = false
        private var isRemoved = false

        init
        {
            bottomFace = icosphere.getFaceVertices(face)
                .map { icosphere.getVertexPosition(it) ?: throw IllegalArgumentException("Invalid face index: $face") }
                .toList()
            center = PVector.add(PVector.add(bottomFace[0], bottomFace[1]), bottomFace[2]).div(3.0f)
            normal = center.copy().normalize()
            val k = 1.7f
            maxHeight = noise(
                ((center.x * 0.5f + 0.5f) + (center.y + 0.5f + 1.5f) + (center.z + 0.5f + 2.5f)) * k,
                frameCount / fps * 0.2f
            ) * 0.71f
        }

        fun getFace(): Int = face

        fun remove()
        {
            isRemoved = true
        }

        fun isRemoved(): Boolean = isRemoved

        fun stretch(maxMillis: Long, onFinished: () -> Unit = {}, easing: (Float) -> Float)
        {
            // finish the thread
            isStretchInterrupted = true
            stretchThread?.join()
            isStretchInterrupted = false

            // start a new thread
            stretchThread = Thread {
                var t = 0L
                val deltaT = (1000L / fps).toLong()
                val maxT = maxMillis
                while (t < maxT)
                {
                    if (isStretchInterrupted) break
                    t += deltaT
                    val factor = easing(t.toFloat() / maxT.toFloat())
                    currHeight = factor * maxHeight
                    alpha = factor
                    if (t > maxT) break
                    Thread.sleep(deltaT)
                }
                onFinished()
            }
            stretchThread?.start()
        }

        fun getNextFace(): Int
        {
            val others = walkers.map { it.getElements() }.flatten().filter { it.getFace() != face }

            val neighbors = icosphere.getFaceNeighbors(face).toMutableList()
            // remove the location where walkers are already located
            neighbors.removeIf { neighbor -> others.any { it.face == neighbor } }
            if (neighbors.isEmpty())
            {
                return icosphere.getFaceNeighbors(face).random()
            }

            val weights = neighbors.map { neighbor ->
                // calc the center of gravity
                val c = icosphere.getFaceVertices(neighbor).map { v -> icosphere.getVertexPosition(v) }
                    .reduce { acc, p -> PVector.add(acc, p) }?.div(3.0f)!!
                1.0f / calcLocalDensity(c, others)
            }.toMutableList()

            // finite check
            weights.forEach { if (!it.isFinite()) return neighbors.random() }

            // normalize weights
            val sumWeights = weights.sum()
            weights.forEachIndexed { i, weight -> weights[i] = weight / sumWeights }

            // return neighbors[weights.withIndex().maxBy { it.value }.index]

            // randomly retrieve considering weights
            val rand = random(1.0f)
            var sum = 0.0f
            for (i in 0 until weights.size - 1)
            {
                sum += weights[i]
                if (rand < sum)
                {
                    return neighbors[i]
                }
            }
            return neighbors.last()
        }

        private fun calcLocalDensity(pos: PVector, others: Collection<WalkerElement>): Float
        {
            val theta = acos(pos.z)
            val phi = atan2(pos.y, pos.x)

            // calculate density based on spherical distance
            return others.map { other ->
                val oPos = other.center
                val oTheta = acos(oPos.z)
                val oPhi = atan2(oPos.y, oPos.x)
                val d = haversine(theta, phi, oTheta, oPhi, 1.0f)
                val k = if (other.group == this.group) 4.0f else 1.0f
                k * exp(-16.0f * d)
            }.sum()
        }

        fun draw()
        {
            val top = PVector.mult(normal, currHeight).add(center)

            pushStyle()
            noStroke()
            fill(color, alpha * 120.0f)

            // bottom
//            beginShape()
//            bottomFace.forEach { vertex(it.x, it.y, it.z) }
//            endShape(CLOSE)

            // sides
            for (i in 0 until 3)
            {
                beginShape()
                vertex(bottomFace[i].x, bottomFace[i].y, bottomFace[i].z)
                vertex(bottomFace[(i + 1) % 3].x, bottomFace[(i + 1) % 3].y, bottomFace[(i + 1) % 3].z)
                vertex(top.x, top.y, top.z)
                endShape(CLOSE)
            }

            popStyle()
        }
    }
}
