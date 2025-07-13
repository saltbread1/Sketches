package sketches

import processing.core.PVector

class S20250709a : ExtendedPApplet(P2D)
{
    private val palette = createPalette("f72585-b5179e-7209b7-560bad-480ca8-3a0ca3-3f37c9-4361ee-4895ef-4cc9f0")
    private val curves = mutableListOf<Curve>()

    override fun setup()
    {
        noLoop()
    }

    override fun draw()
    {
        background(0)

        curves.clear()

        curves.add(CubicBezier(
            PVector(width * 0.5f, height * 0.5f),
            PVector(random(-1.0f, 1.0f), random(-1.0f, 1.0f)).normalize(),
            200.0f,
            1
        ))
        repeat(100)
        {
            curves.add(curves.last().nextCurve() ?: return@repeat)
        }
        curves.forEach { it.draw() }
    }

    override fun keyPressed()
    {
        super.keyPressed()
        val seed = System.currentTimeMillis()
        noiseSeed(seed)
        randomSeed(seed)
        redraw()
    }

    private abstract inner class Curve
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
            if (length < 1.0f) return null

            val nextLength = length * (0.8f + random(0.2f))
            val nextType = if (curveType == 0) choose(-1, 1) else choose(0, -curveType)

            if (nextType == 0)
            {
                return CubicBezier(endPosition, endDirection, nextLength, nextType)
            }
            val rand = random(1.0f)
            return when
            {
                rand < 0.5f -> Arc(endPosition, endDirection, nextLength, nextType)
                else -> CubicBezier(endPosition, endDirection, nextLength, nextType)
            }
        }

        abstract fun draw()
    }

    private inner class Arc(
        override val startPosition: PVector,
        override val startDirection: PVector,
        override val length: Float,
        override val curveType: Int,
    ) : Curve()
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
            val sign = curveType
            if (curveType == 0) IllegalArgumentException("curveType must be either -1 or 1")

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
//            arc(center.x, center.y, radius * 2.0f, radius * 2.0f, startRad, endRad)
            circle(center.x, center.y, radius * 2.0f)
            popStyle()
        }
    }

    private inner class CubicBezier(
        override val startPosition: PVector,
        override val startDirection: PVector,
        override val length: Float,
        override val curveType: Int,
    ) : Curve()
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
}
