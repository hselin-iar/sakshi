package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.testkit.SeededRandomness
import com.kleos.sakshi.engine.testkit.Zones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** DOC4's T2.10 Done-when: "re-running yields identical patterns", plus the upsert/retire plumbing. */
class PatternEngineTest {

    private fun pattern(key: String, firstSeen: Long, lastSeen: Long) = Pattern(
        kind = PatternKind.RHYTHM, key = key, strength = 1.0, evidenceWindows = 4, evidenceDays = 3,
        firstSeen = EpochMs(firstSeen), lastSeen = EpochMs(lastSeen), args = emptyMap(),
    )

    private fun detectorReturning(patterns: List<Pattern>) = object : PatternDetector {
        override val kind = PatternKind.RHYTHM
        override fun detect(ctx: PatternContext): List<Pattern> = patterns
    }

    private fun emptyCtx(asOf: EpochMs) = PatternContext(
        windows = emptyList(), weeks = emptyList(), days = emptyList(), baseline = null,
        asOf = asOf, random = SeededRandomness(1L), zone = Zones.KOLKATA,
    )

    @Test
    fun `a re-detected pattern keeps its previous firstSeen but takes the fresh lastSeen`() {
        val existing = listOf(pattern("cell:A", firstSeen = 100, lastSeen = 200))
        val detector = detectorReturning(listOf(pattern("cell:A", firstSeen = 999, lastSeen = 999)))

        val result = PatternEngine.run(emptyCtx(EpochMs(500)), listOf(detector), existing)

        val merged = result.single { it.key == "cell:A" }
        assertEquals(100L, merged.firstSeen.value)
        assertEquals(999L, merged.lastSeen.value)
    }

    @Test
    fun `a pattern not redetected is kept, not deleted`() {
        val existing = listOf(pattern("cell:STALE", firstSeen = 100, lastSeen = 200))
        val detector = detectorReturning(emptyList())

        val result = PatternEngine.run(emptyCtx(EpochMs(500)), listOf(detector), existing)

        assertEquals(1, result.size)
        assertEquals("cell:STALE", result.single().key)
        assertEquals(200L, result.single().lastSeen.value)
    }

    @Test
    fun `a brand-new pattern with no prior entry is added as-is`() {
        val fresh = pattern("cell:NEW", firstSeen = 500, lastSeen = 500)
        val detector = detectorReturning(listOf(fresh))

        val result = PatternEngine.run(emptyCtx(EpochMs(500)), listOf(detector), emptyList())

        assertEquals(fresh, result.single())
    }

    @Test
    fun `re-running on identical input yields identical patterns`() {
        val existing = listOf(pattern("cell:A", firstSeen = 100, lastSeen = 200))
        val detector = detectorReturning(listOf(pattern("cell:A", firstSeen = 999, lastSeen = 999)))
        val ctx = emptyCtx(EpochMs(500))

        val first = PatternEngine.run(ctx, listOf(detector), existing)
        val second = PatternEngine.run(ctx, listOf(detector), existing)

        assertEquals(first, second)
    }

    @Test
    fun `allDetectors registers all seven P1-P5, CrossDay and Clustering detectors`() {
        assertEquals(7, PatternEngine.allDetectors.size)
        assertEquals(
            setOf(Rhythm, Trend, Shift, WindowShape, BreakPoint, CrossDay, Clustering),
            PatternEngine.allDetectors.toSet(),
        )
    }

    @Test
    fun `isRetired is true only after 14 days without redetection`() {
        val p = pattern("cell:A", firstSeen = 0, lastSeen = 0)
        val thirteenDaysMs = 13L * 24 * 60 * 60 * 1_000
        val fifteenDaysMs = 15L * 24 * 60 * 60 * 1_000

        assertFalse(PatternEngine.isRetired(p, EpochMs(thirteenDaysMs)))
        assertTrue(PatternEngine.isRetired(p, EpochMs(fifteenDaysMs)))
    }
}
