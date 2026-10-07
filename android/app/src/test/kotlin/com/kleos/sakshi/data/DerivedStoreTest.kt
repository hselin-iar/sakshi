package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DerivedStoreTest : DbTest() {
    private val store get() = RoomDerivedStore(db)

    @Test fun aDayReadsBackExactlyAsWritten() {
        val d = derivation(day = 100, 1, 2)
        store.replaceDay(StudyDay(100), d)
        assertEquals(d.windows, store.windows(t(0), t(10_000)))
        assertEquals(listOf(d.summary), store.days(StudyDay(100), StudyDay(100)))
    }

    @Test fun replacingADayDropsItsOldRowsAndLeavesOtherDaysAlone() {
        store.replaceDay(StudyDay(100), derivation(100, 1, 2))
        store.replaceDay(StudyDay(101), derivation(101, 3))
        store.replaceDay(StudyDay(100), derivation(100, 4))
        assertEquals(listOf(3L, 4L), store.windows(t(0), t(10_000)).map { it.window.id }.sorted())
        assertEquals(2, db.derived().days(0, 999).size)
        // no orphans: every stretch and stay belongs to a stored window
        val ids = db.derived().windowsStartingIn(0, 100_000).map { it.id }
        assertTrue(db.derived().stretchesOf((0L..100L).toList()).all { it.windowId in ids })
        assertTrue(db.derived().staysOf((0L..100L).toList()).all { it.windowId in ids })
    }

    @Test fun windowsAreSelectedByStartTimeHalfOpen() {
        store.replaceDay(StudyDay(100), derivation(100, 1, 2, 3))   // starts 1_000, 2_000, 3_000
        assertEquals(listOf(2L), store.windows(t(2_000), t(3_000)).map { it.window.id })
    }

    @Test fun daysRangeIsInclusiveAtBothEnds() {
        listOf(100L, 101L, 102L).forEach { store.replaceDay(StudyDay(it), derivation(it, it)) }
        assertEquals(listOf(100L, 101L), store.days(StudyDay(100), StudyDay(101)).map { it.day.epochDay })
    }

    @Test fun weeksAndPatternsUpsertByTheirKey() {
        val w = WeekSummary(WeekStart(StudyDay(98)), 10.0, 30.0, 4.0, 6.0, 0.36, 0.7, 100, "Steady", 12, 5, false, 3, 20.0, 0)
        store.upsertWeek(w); store.upsertWeek(w.copy(steadiness = 116, word = "Steadier"))
        assertEquals(listOf(w.copy(steadiness = 116, word = "Steadier")), store.weeks())

        val p = Pattern(PatternKind.RHYTHM, "cell:NIGHT-WEEKDAY", 1.8, 9, 4, t(1_000), t(2_000), mapOf("cell" to "NIGHT-WEEKDAY", "direction" to "CHOPPY"))
        store.upsertPatterns(listOf(p)); store.upsertPatterns(listOf(p.copy(lastSeen = t(3_000))))
        assertEquals(listOf(p.copy(lastSeen = t(3_000))), store.patterns())
    }

    @Test fun nullablePartsStayNullAndAreNeverTurnedIntoZero() {
        val summary = daySummary(100).copy(switchesPerHour = null, flinch = null, rampUpMin = null, lastScreenOffTs = null, firstStretchMin = null)
        store.replaceDay(StudyDay(100), DayDerivation(emptyList(), summary))
        assertEquals(summary, store.days(StudyDay(100), StudyDay(100)).single())
    }

    @Test fun clearDerivedKeepsTheFrozenBaselineAndSettings() {
        store.replaceDay(StudyDay(100), derivation(100, 1))
        RoomStateStore(db).saveBaseline(Baseline(0, t(5), 10.0, 4.0, 6.0, 0.36, 8, true))
        store.clearDerived()
        assertEquals(0, store.windows(t(0), t(99_999)).size)
        assertEquals(0, store.days(StudyDay(0), StudyDay(999)).size)
        assertEquals(10.0, RoomStateStore(db).baseline()!!.c0, 0.0)
    }
}
