package sketches

import processing.core.PVector

class S20250709a : ExtendedPApplet(P3D)
{
    private val palettes = arrayOf(
        createPalette("780000-c1121f-fdf0d5-003049-669bbc"),
        createPalette("001524-15616d-ffecd1-ff7d00-78290f"),
        createPalette("ff6700-ebebeb-c0c0c0-3a6ea5-004e98"),
    )
    private val radialCurves = mutableListOf<RadialCurve>()
    private val gridRes = 4
    private val curveDirRad = 1.34f
    private val near = 0.0f
    private val far = 10.0f

    override fun setup()
    {
        camera(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        ortho(-aspect, aspect, -1.0f, 1.0f, near, far + 1.0f)
        init()
        noLoop()
    }

    private fun init()
    {
        radialCurves.clear()

        val resX = (gridRes * aspect).toInt()
        val resY = gridRes
        val depth = FloatArray(resX * resY) { i -> -sqrt(i.toFloat() / (resX * resY)) * (far - near) }
        depth.shuffle()
        repeat(choose(1, 2))
        {
            for (iy in 0 until resY)
            {
                for (ix in 0 until resX)
                {
                    val x = ((ix + random(1.0f)) / resX * 2.0f - 1.0f) * aspect
                    val y = (iy + random(1.0f)) / resY * 2.0f - 1.0f

                    radialCurves.add(RadialCurve(PVector(x, y, depth[ix + iy * resX])))
                }
            }
        }
    }

    override fun draw()
    {
        background(0)

        radialCurves.forEach { it.draw() }

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
        init()
        redraw()
    }

    private fun easeStrokeWeight(x: Float) = exp(-x) * sq(cos(PI * x))

    private abstract inner class Curve(protected val initialLength: Float)
    {
        abstract val startPosition: PVector
        abstract val startDirection: PVector
        abstract val endPosition: PVector
        abstract val endDirection: PVector
        abstract val length: Float
        abstract val curveType: Int

        init
        {
            if (abs(curveType) > 1) throw IllegalArgumentException("curveType must be either -1, 1, or 0")
        }

        fun nextCurve(times: Int, color: Int): Curve?
        {
            if (length < initialLength * 0.01f) return null

            var nextLength = length
            repeat(times) { nextLength *= (0.8f + random(0.2f)) }

            val nextType = if (nextLength < initialLength * 0.5f)
            {
                if (curveType == 0) choose(-1, 1) else choose(0, curveType)
            }
            else
            {
                if (curveType == 0) choose(-1, 0, 1) else choose(0, -curveType)
            }

            val rand = random(1.0f)
            return when
            {
                rand < 0.5f -> Arc(endPosition, endDirection, nextLength, nextType, color, initialLength)
                else -> CubicBezier(endPosition, endDirection, nextLength, nextType, color, initialLength)
            }
        }

        abstract fun draw(alpha: Float)
    }

    private inner class Arc(
        override val startPosition: PVector,
        override val startDirection: PVector,
        override val length: Float,
        override val curveType: Int,
        private val color: Int,
        initialLength: Float = length,
    ) : Curve(initialLength)
    {
        override val endPosition: PVector
        override val endDirection: PVector
        private val radius: Float
        private val center: PVector
        private val startRad: Float
        private val endRad: Float

        init
        {
            val sign = if (curveType == 0) choose(-1, 1) else curveType

            val ns = startDirection.copy().rotate(-HALF_PI * sign)
            val startToEnd = ns.copy().rotate(-random(1.0f) * curveDirRad * sign).mult(-length)
            endPosition = PVector.add(startPosition, startToEnd)
            radius = startToEnd.magSq() / (2.0f * abs(PVector.dot(startToEnd, ns)))
            center = PVector.mult(ns, -radius).add(startPosition)
            val ne = PVector.sub(endPosition, center).normalize()
            endDirection = ne.copy().rotate(HALF_PI * sign)

            val start = atan2(ns.y, ns.x)
            val end = atan2(ne.y, ne.x)
            startRad = if (sign > 0) start else end
            endRad = (if (sign > 0) end else start).let { if (it < startRad) it + TWO_PI else it }
        }

        override fun draw(alpha: Float)
        {
            pushStyle()
            noFill()
            stroke(color, alpha * 255.0f)
            strokeWeight(easeStrokeWeight(length / initialLength) * 4.0f + 0.5f)
            arc(center.x, center.y, radius * 2.0f, radius * 2.0f, startRad, endRad)
//            circle(center.x, center.y, radius * 2.0f)
            popStyle()
        }
    }

    private inner class CubicBezier(
        override val startPosition: PVector,
        override val startDirection: PVector,
        override val length: Float,
        override val curveType: Int,
        private val color: Int,
        initialLength: Float = length,
    ) : Curve(initialLength)
    {
        override val endPosition: PVector
        override val endDirection: PVector
        private val controlPosition1: PVector
        private val controlPosition2: PVector

        init
        {
            val sign1 = if (curveType == 0) choose(-1, 1) else curveType
            val sign2 = if (curveType == 0) -sign1 else sign1
            val startToEnd = startDirection.copy().rotate(random(1.0f) * curveDirRad * sign1).mult(length)
            endPosition = PVector.add(startPosition, startToEnd)
            endDirection = startToEnd.copy().rotate(random(1.0f) * curveDirRad * sign2).normalize()
            val t = random(0.4f, 0.6f)
            controlPosition1 = PVector.mult(startDirection, length * random(0.3f, t)).add(startPosition)
            controlPosition2 = PVector.mult(endDirection, -length * random(t, 0.7f)).add(endPosition)
        }

        override fun draw(alpha: Float)
        {
            pushStyle()
            noFill()
            stroke(color, alpha * 255.0f)
            strokeWeight(easeStrokeWeight(length / initialLength) * 4.0f + 0.5f)
            bezier(
                startPosition.x, startPosition.y,
                controlPosition1.x, controlPosition1.y,
                controlPosition2.x, controlPosition2.y,
                endPosition.x, endPosition.y
            )
            popStyle()
        }
    }

    private inner class CurveSequence(initialCurve: Curve, private val palette: IntArray)
    {
        private val curves = MutableList(1) { initialCurve }
        private val pitch = random(TAU)
        private val yaw = random(TAU)
        private val roll = random(TAU)
        var isFull: Boolean = false

        fun addCurve(curveSequences: List<CurveSequence>, triableItr: Int = 100)
        {
            for (i in 1..triableItr)
            {
                val k = sqrt(i.toFloat())
                val lastCurve = curves.last()
                val nextCurve = lastCurve.nextCurve(k.toInt(), palette.random()) ?: break
                if (curveSequences.count { e ->
                        e.curves.any {
                            linesIntersect(
                                it.startPosition, it.endPosition,
                                nextCurve.startPosition, nextCurve.endPosition
                            )
                        }
                    } <= k * k)
                {
                    curves.add(nextCurve)
                    return
                }
            }
            curves.toList()
            isFull = true
        }

        fun draw(alpha: Float)
        {
            pushMatrix()
            rotateZ(pitch)
            rotateY(yaw)
            rotateX(roll)
            curves.forEach { it.draw(alpha) }
            popMatrix()
        }
    }

    private inner class RadialCurve(private val center: PVector)
    {
        private val curveSequences = mutableListOf<CurveSequence>()
        private val maxCurveSequences = 100
        private val palette = palettes.random()

        init
        {
            curveSequences.clear()
            repeat(maxCurveSequences)
            {
                curveSequences.add(
                    CurveSequence(
                        CubicBezier(
                            PVector(),
                            PVector.random2D(),
                            random(0.05f, 0.3f),
                            choose(-1, 0, 1),
                            palette.random(),
                        ),
                        palette,
                    ),
                )
            }
            while (curveSequences.any { !it.isFull })
            {
                curveSequences.shuffled().filter { !it.isFull }.forEach { it.addCurve(curveSequences) }
            }
        }

        fun draw()
        {
            pushMatrix()
            translate(center.x, center.y, center.z)
            curveSequences.forEach { it.draw(map(center.z, -far, -near, 0.05f, 0.65f)) }
            popMatrix()
        }
    }
}
