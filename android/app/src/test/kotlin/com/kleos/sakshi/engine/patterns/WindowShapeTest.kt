package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Shape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** G-P4 is the spec (DOC 3/DOC4, Pattern Layer: WindowShape). User median: 3.0 stays/h, stretch 10. */
class WindowShapeTest {
    private val medianStaysPerHour = 3.0
    private val medianStretchMin = 10.0

    private fun origins(stones: Int, selfStarted: Int, unknown: Int = 0) =
        List(stones) { Origin.STONE } + List(selfStarted) { Origin.SELF_STARTED } + List(unknown) { Origin.UNKNOWN }

    @Test
    fun `W-A below-median stays and above-median stretch is HELD`() {
        val shape = WindowShape.label(2.0, 14.0, origins(stones = 1, selfStarted = 3), medianStaysPerHour, medianStretchMin)
        assertEquals(Shape.HELD, shape)
    }

    @Test
    fun `W-B 75pct stone share is PINGED`() {
        val shape = WindowShape.label(5.0, 6.0, origins(stones = 3, selfStarted = 1), medianStaysPerHour, medianStretchMin)
        assertEquals(Shape.PINGED, shape)
    }

    @Test
    fun `W-C 25pct stone share is REACHED`() {
        val shape = WindowShape.label(5.0, 6.0, origins(stones = 1, selfStarted = 3), medianStaysPerHour, medianStretchMin)
        assertEquals(Shape.REACHED, shape)
    }

    @Test
    fun `W-D zero stays is HELD`() {
        val shape = WindowShape.label(0.0, 0.0, emptyList(), medianStaysPerHour, medianStretchMin)
        assertEquals(Shape.HELD, shape)
    }

    @Test
    fun `W-E exactly 60pct stone share still qualifies as PINGED`() {
        val shape = WindowShape.label(2.0, 8.0, origins(stones = 3, selfStarted = 2), medianStaysPerHour, medianStretchMin)
        assertEquals(Shape.PINGED, shape)
    }

    @Test
    fun `W-F only 1 known-origin stay is null`() {
        val shape = WindowShape.label(5.0, 6.0, origins(stones = 1, selfStarted = 0), medianStaysPerHour, medianStretchMin)
        assertNull(shape)
    }
}
