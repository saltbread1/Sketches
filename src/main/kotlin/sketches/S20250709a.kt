package sketches

import processing.core.PVector

class S20250709a : ExtendedPApplet(P2D)
{
    private val palette = createPalette("f72585-b5179e-7209b7-560bad-480ca8-3a0ca3-3f37c9-4361ee-4895ef-4cc9f0")
    private val curveSequences = mutableListOf<CurveSequence>()
    private val maxCurveSequences = 100

    override fun setup()
    {
        init()
        noLoop()
    }

    private fun init()
    {
        curveSequences.clear()
        repeat(maxCurveSequences)
        {
            curveSequences.add(CurveSequence(CubicBezier(
                PVector(width * 0.5f, height * 0.5f),
                PVector(random(-1.0f, 1.0f), random(-1.0f, 1.0f)).normalize(),
                width * random(0.05f, 0.2f),
                choose(-1, 0, 1),
            )))
        }
        while (curveSequences.any { !it.isFull })
        {
            curveSequences.shuffled().filter { !it.isFull }.forEach { it.addCurve(curveSequences) }
        }
        curveSequences.shuffle()
    }

    override fun draw()
    {
        background(0)

        curveSequences.forEach { it.draw() }

        if (isSave)
        {
            saveFrame(saveName(this::class))
        }
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

    private abstract inner class Curve(private val initialLength: Float)
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

        fun nextCurve(): Curve?
        {
            val nextLength = length * (0.8f + random(0.2f))
            if (nextLength < 1.0f) return null
            val nextType = if (nextLength < initialLength * 0.5f)
            {
                if (curveType == 0) choose(-1, 1) else choose(0, curveType)
            }
            else
            {
                if (curveType == 0) choose(-1, 0, 1) else choose(0, -curveType)
            }

            var nextCurve: Curve? = null
            repeat(100)
            {
                val rand = random(1.0f)
                nextCurve = when
                {
                    rand < 0.5f -> Arc(endPosition, endDirection, nextLength, nextType, initialLength)
                    else -> CubicBezier(endPosition, endDirection, nextLength, nextType, initialLength)
                }
                if (true)
                {
                    return@repeat
                }
            }
            return nextCurve
        }

        abstract fun draw()
    }

    private inner class Arc(
        override val startPosition: PVector,
        override val startDirection: PVector,
        override val length: Float,
        override val curveType: Int,
        initialLength: Float = length,
    ) : Curve(initialLength)
    {
        override val endPosition: PVector
        override val endDirection: PVector
        private val radius: Float
        private val center: PVector
        private val startRad: Float
        private val endRad: Float
        private val color = palette.random()

        init
        {
            val sign = if (curveType == 0) choose(-1, 1) else curveType

            val ns = startDirection.copy().rotate(-HALF_PI * sign)
            val startToEnd = ns.copy().rotate(-random(1.2f) * sign).mult(-length)
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

        override fun draw()
        {
            pushStyle()
            noFill()
            stroke(color)
            strokeWeight(4.0f)
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
        initialLength: Float = length,
    ) : Curve(initialLength)
    {
        override val endPosition: PVector
        override val endDirection: PVector
        private val controlPosition1: PVector
        private val controlPosition2: PVector
        private val color = palette.random()

        init
        {
            val sign1 = if (curveType == 0) choose(-1, 1) else curveType
            val sign2 = if (curveType == 0) -sign1 else sign1
            val startToEnd = startDirection.copy().rotate(random(1.34f * sign1)).mult(length)
            endPosition = PVector.add(startPosition, startToEnd)
            endDirection = startToEnd.copy().rotate(random(1.34f * sign2)).normalize()
            val t = random(0.4f, 0.6f)
            controlPosition1 = PVector.mult(startDirection, length * random(0.3f, t)).add(startPosition)
            controlPosition2 = PVector.mult(endDirection, -length * random(t, 0.7f)).add(endPosition)
        }

        override fun draw()
        {
            pushStyle()
            noFill()
            stroke(color)
            strokeWeight(4.0f)
            bezier(
                startPosition.x, startPosition.y,
                controlPosition1.x, controlPosition1.y,
                controlPosition2.x, controlPosition2.y,
                endPosition.x, endPosition.y
            )
            popStyle()
        }
    }

    private inner class CurveSequence(initialCurve: Curve)
    {
        private val curves = MutableList(1) { initialCurve }
        var isFull: Boolean = false

        fun addCurve(curveSequences: List<CurveSequence>, triableItr: Int = 100)
        {
            repeat(triableItr)
            {
                val lastCurve = curves.last()
                val nextCurve = lastCurve.nextCurve() ?: return@repeat
                if (curveSequences.count { e -> e.curves.any {
                    linesIntersect(it.startPosition, it.endPosition,
                        nextCurve.startPosition, nextCurve.endPosition)
                } } <= sqrt(triableItr.toFloat()))
                {
                    curves.add(nextCurve)
                    return
                }
            }
            curves.toList()
            isFull = true
        }

        fun draw()
        {
            curves.forEach { it.draw() }
        }
    }
}
