package sketches
import mesh.HalfEdgeMesh
import mesh.MeshData
import processing.core.PVector
import processing.opengl.PGraphicsOpenGL
import processing.opengl.PShader
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Random walk on the icosphere.
 */
class S20250630a : ExtendedPApplet(P3D)
{
    private val palette1 = createPalette("250902-38040e-640d14-800e13-ad2831")
    private val palette2 = createPalette("f72585-b5179e-7209b7-560bad-480ca8-3a0ca3-3f37c9-4361ee-4895ef-4cc9f0")
    private val eye = PVector(0.0f, 0.0f, 2.0f)
    private val center = PVector(0.0f, 0.0f, 0.0f)
    private val fov = PI * 0.25f
    private val fps = 60.0f
    private val mesh = Icosahedron()
    private val randomWalks = listOf(
        RandomWalk(palette1, 0.6f, 3, 3, 36),
        RandomWalk(palette2, 0.0f, 4, 8, 111),
    )
    private var bgShader: PShader? = null
    private val totalSaveSec = 20.0f

    override fun settings()
    {
        // full HD framebuffer
        size(960, 540, P3D)
        pixelDensity(2)

        aspect = width.toFloat() / height.toFloat()
    }

    override fun setup()
    {
        perspective(fov, aspect, 0.1f, 10.0f)
        camera(eye.x, eye.y, eye.z, center.x, center.y, center.z, 0.0f, 1.0f, 0.0f)
        randomWalks.forEach { it.init() }

        bgShader = loadShader(
            this::class.java.classLoader.getResource("shaders/gradation.glsl")?.path)
        bgShader?.set("resolution", pixelWidth.toFloat(), pixelHeight.toFloat())
        bgShader?.set("direction", width.toFloat(), -height.toFloat())
        bgShader?.set("startColor", 0.24f, 0.23f, 0.56f)
        bgShader?.set("endColor", 0.64f, 0.023f, 0.12f)

        frameRate(fps)
    }

    override fun draw()
    {
        background(30.0f, 30.0f, 36.0f)

        hint(DISABLE_DEPTH_MASK)

        // background
        (g as PGraphicsOpenGL).pushProjection()
        ortho(-1.0f, 1.0f, -1.0f, 1.0f, -10.0f, 10.0f)
        shader(bgShader)
        pushStyle()
        noStroke()
        fill(255.0f)
        rect(-1.0f, -1.0f, 2.0f, 2.0f)
        popStyle()
        resetShader()
        (g as PGraphicsOpenGL).popProjection()

        hint(ENABLE_DEPTH_MASK)

        randomWalks.forEach { it.update() }


        // inner
        pushMatrix()
        rotateY(frameCount * 0.008f)
        rotateX(frameCount * 0.013f)
        scale(0.5f)
        randomWalks[0].draw()
        popMatrix()

        // outer
        pushMatrix()
        rotateX(frameCount * 0.003f)
        rotateY(frameCount * 0.007f)
        randomWalks[1].draw()
        popMatrix()

        if (isSave)
        {
            saveFrame(saveFrameName(this::class))
            if (frameCount >= fps * totalSaveSec) exit()
        }
    }

    override fun keyPressed()
    {
        super.keyPressed()
        randomWalks.forEach { it.init() }
    }

    override fun exit()
    {
        if (isSave) println(makeMovie(this::class, fps.toInt(), fps.toInt()))
        super.exit()
    }

    private inner class Icosahedron : MeshData
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

    private inner class RandomWalk(private val palette: IntArray, private val elementHeight: Float, numDivision: Int, numWalkers: Int, maxElements: Int)
    {
        private val icosphere = HalfEdgeMesh()
        private val walkers = List(numWalkers) { Walker(maxElements) }

        init
        {
            icosphere.buildMesh(mesh)
            repeat(numDivision)
            {
                icosphere.subdivide { v0, v1 -> PVector.lerp(v0, v1, random(0.3f, 0.7f)).normalize() }
            }
            val error = icosphere.validate()
            if (error.isNotEmpty())
            {
                throw IllegalStateException("Invalid mesh state: $error")
            }
        }

        fun init()
        {
            walkers.forEach { it.init() }
        }

        fun update()
        {
            walkers.forEach { it.update() }
        }

        fun draw()
        {
            walkers.forEach { it.draw() }
        }

        private inner class Walker(private val maxElements: Int)
        {
            private val elements: Queue<WalkerElement> = ConcurrentLinkedDeque()

            fun init()
            {
                elements.clear()
                elements.add(WalkerElement(hashCode(), random(icosphere.getFaceCount().toFloat()).toInt()))
            }

            fun getElementsAsList(): List<WalkerElement> = elements.toList()

            fun update()
            {
                val newElement = WalkerElement(hashCode(), elements.last().getNextFace(), random(1.0f) < 0.5f)
                newElement.transition(0.86f) { easeOutBack(it, 8.0f) }
                elements.add(newElement)

                if (elements.count { !it.isRemoved() } > maxElements)
                {
                    val first = elements.first { !it.isRemoved() }
                    first.remove()
                    first.transition(0.72f, { elements.remove(first) }) { easeOutBack(1.0f - it, 4.0f) }
                }
            }

            fun draw()
            {
                elements.forEach { it.draw() }
            }
        }

        private inner class WalkerElement(private val group: Int, private val face: Int, private val isWire: Boolean = false)
        {
            private val bottomFace: List<PVector> = icosphere.getFaceVertices(face)
                .map { icosphere.getVertexPosition(it) ?: throw IllegalArgumentException("Invalid face index: $face") }
                .toList()
            private val center: PVector = PVector.add(PVector.add(bottomFace[0], bottomFace[1]), bottomFace[2]).div(3.0f)
            private val normal: PVector = center.copy().normalize()
            private val color = palette.random()
            private val maxHeight: Float = noise(
                ((center.x * 0.5f + 0.5f) + (center.y + 0.5f + 1.5f) + (center.z + 0.5f + 2.5f)) * 1.7f,
                frameCount / fps * 0.2f
            ) * elementHeight
            private val maxAlpha = 200.0f
            private var removed = false
            @Volatile private var easeFactor = 0.0f
            @Volatile private var prevThread: Thread? = null

            fun remove()
            {
                removed = true
            }

            fun isRemoved(): Boolean = removed

            fun transition(maxSec: Float, onFinished: () -> Unit = {}, easing: (Float) -> Float)
            {
                Thread {
                    // wait previous thread
                    prevThread?.join()
                    // start a new thread
                    prevThread = Thread {
                        var t = 0L
                        val deltaT = (1000L / fps).toLong()
                        val maxT = maxSec * 1e3f
                        while (t < maxT)
                        {
                            t += deltaT
                            easeFactor = easing(t / maxT)
                            Thread.sleep(deltaT)
                        }
                        onFinished.invoke()
                    }
                    prevThread?.start()
                }.start()
            }

            fun getNextFace(): Int
            {
                val others = walkers.map { it.getElementsAsList() }.flatten().filter { it.face != face }

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
                    val d = haversine(theta, phi, oTheta, oPhi)
                    val k = if (other.group == this.group) 4.0f else 1.0f
                    k * exp(-16.0f * d)
                }.sum()
            }

            fun draw()
            {
                val topPos = PVector.mult(normal, easeFactor * maxHeight).add(center)
                val bottomPos = bottomFace.map { PVector.sub(it, center).mult(easeFactor).add(center) }
                val alpha = easeFactor * maxAlpha

                pushStyle()
                if (isWire)
                {
                    stroke(color, alpha)
                    fill(color, alpha * 0.2f)
                }
                else
                {
                    noStroke()
                    fill(color, alpha)
                }

                for (i in 0 until 3)
                {
                    beginShape()
                    vertex(bottomPos[i].x, bottomPos[i].y, bottomPos[i].z)
                    vertex(bottomPos[(i + 1) % 3].x, bottomPos[(i + 1) % 3].y, bottomPos[(i + 1) % 3].z)
                    vertex(topPos.x, topPos.y, topPos.z)
                    endShape(CLOSE)
                }

                popStyle()
            }
        }
    }
}
