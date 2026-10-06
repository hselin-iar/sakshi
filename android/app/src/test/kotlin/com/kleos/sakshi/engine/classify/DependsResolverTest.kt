package com.kleos.sakshi.engine.classify

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.testkit.epochMsAt
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * F4's Depends rule (DEPENDS_JOIN_SEC = 90): a DEPENDS interval is
 * effectively IN_SET when its start is within 90s after the end of an
 * effectively in-set interval (neutral intervals in between are
 * transparent), or is contiguous (gap <= 90s) with an effectively in-set
 * DEPENDS interval; otherwise OFF_SET.
 */
class DependsResolverTest {

    @Test
    fun `a Depends interval starting 89s after an in-set interval resolves to in-set`() {
        val inSetEnd = epochMsAt("10:00:00")
        val dependsStart = EpochMs(inSetEnd.value + 89_000)
        val intervals = listOf(
            ForegroundInterval(Pkg("A"), epochMsAt("09:55:00"), inSetEnd),
            ForegroundInterval(Pkg("B"), dependsStart, EpochMs(dependsStart.value + 60_000)),
        )
        val base = listOf(AppClass.IN_SET, AppClass.DEPENDS)

        assertEquals(listOf(AppClass.IN_SET, AppClass.IN_SET), DependsResolver.resolve(intervals, base))
    }

    @Test
    fun `a Depends interval starting 91s after an in-set interval resolves to off-set`() {
        val inSetEnd = epochMsAt("10:00:00")
        val dependsStart = EpochMs(inSetEnd.value + 91_000)
        val intervals = listOf(
            ForegroundInterval(Pkg("A"), epochMsAt("09:55:00"), inSetEnd),
            ForegroundInterval(Pkg("B"), dependsStart, EpochMs(dependsStart.value + 60_000)),
        )
        val base = listOf(AppClass.IN_SET, AppClass.DEPENDS)

        assertEquals(listOf(AppClass.IN_SET, AppClass.OFF_SET), DependsResolver.resolve(intervals, base))
    }

    @Test
    fun `a chain of Depends intervals each within 90s stays in-set`() {
        val t0 = epochMsAt("10:00:00")
        val inSet = ForegroundInterval(Pkg("A"), t0, EpochMs(t0.value + 60_000))
        val depends1Start = EpochMs(inSet.end.value + 50_000) // 50s gap
        val depends1 = ForegroundInterval(Pkg("B"), depends1Start, EpochMs(depends1Start.value + 30_000))
        val depends2Start = EpochMs(depends1.end.value + 60_000) // 60s gap from the Depends chain, not the original
        val depends2 = ForegroundInterval(Pkg("C"), depends2Start, EpochMs(depends2Start.value + 20_000))

        val intervals = listOf(inSet, depends1, depends2)
        val base = listOf(AppClass.IN_SET, AppClass.DEPENDS, AppClass.DEPENDS)

        assertEquals(
            listOf(AppClass.IN_SET, AppClass.IN_SET, AppClass.IN_SET),
            DependsResolver.resolve(intervals, base),
        )
    }

    @Test
    fun `a neutral interval between two in-set intervals stays neutral`() {
        val intervals = listOf(
            ForegroundInterval(Pkg("A"), epochMsAt("10:00:00"), epochMsAt("10:01:00")),
            ForegroundInterval(Pkg("N"), epochMsAt("10:01:00"), epochMsAt("10:02:00")),
            ForegroundInterval(Pkg("C"), epochMsAt("10:02:00"), epochMsAt("10:03:00")),
        )
        val base = listOf(AppClass.IN_SET, AppClass.NEUTRAL, AppClass.IN_SET)

        assertEquals(
            listOf(AppClass.IN_SET, AppClass.NEUTRAL, AppClass.IN_SET),
            DependsResolver.resolve(intervals, base),
        )
    }

    @Test
    fun `a neutral interval in between does not change the 90s chain outcome`() {
        val inSetEnd = epochMsAt("10:00:00")
        val neutral = ForegroundInterval(Pkg("N"), EpochMs(inSetEnd.value + 10_000), EpochMs(inSetEnd.value + 20_000))
        val dependsStart = EpochMs(inSetEnd.value + 89_000) // same 89s boundary as the no-neutral case
        val intervals = listOf(
            ForegroundInterval(Pkg("A"), epochMsAt("09:55:00"), inSetEnd),
            neutral,
            ForegroundInterval(Pkg("B"), dependsStart, EpochMs(dependsStart.value + 60_000)),
        )
        val base = listOf(AppClass.IN_SET, AppClass.NEUTRAL, AppClass.DEPENDS)

        assertEquals(
            listOf(AppClass.IN_SET, AppClass.NEUTRAL, AppClass.IN_SET),
            DependsResolver.resolve(intervals, base),
        )
    }

    @Test
    fun `an off-set interval breaks the chain even if the next Depends is within 90s`() {
        val inSetEnd = epochMsAt("10:00:00")
        val offSet = ForegroundInterval(Pkg("X"), EpochMs(inSetEnd.value + 10_000), EpochMs(inSetEnd.value + 15_000))
        val dependsStart = EpochMs(offSet.end.value + 10_000) // well within 90s of inSetEnd, but off-set is not transparent
        val intervals = listOf(
            ForegroundInterval(Pkg("A"), epochMsAt("09:55:00"), inSetEnd),
            offSet,
            ForegroundInterval(Pkg("B"), dependsStart, EpochMs(dependsStart.value + 60_000)),
        )
        val base = listOf(AppClass.IN_SET, AppClass.OFF_SET, AppClass.DEPENDS)

        assertEquals(
            listOf(AppClass.IN_SET, AppClass.OFF_SET, AppClass.OFF_SET),
            DependsResolver.resolve(intervals, base),
        )
    }
}
