package com.kleos.sakshi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifAndCoverageTest : DbTest() {
    private val store get() = RoomNotifStore(db)

    @Test fun notificationRangeIsHalfOpenAndPurgeIsStrict() {
        store.append(notif(1_000)); store.append(notif(2_000)); store.append(notif(3_000))
        assertEquals(listOf(1_000L, 2_000L), store.range(t(1_000), t(3_000)).map { it.ts.value })
        assertEquals(2, store.purgeBefore(t(3_000)))
        assertEquals(1, store.count())
    }

    @Test fun storedNotificationHasOnlyThePermittedFields() {
        val e = notif(1_000, "chat")
        store.append(e)
        assertEquals(e, store.range(t(0), t(2_000)).single())
    }

    // two sessions: [1_000, 2_000] and [3_000, still connected]
    private fun twoSessions() {
        store.openSession(t(1_000)); store.closeSession(t(2_000)); store.openSession(t(3_000))
    }

    @Test fun coverageFractionOverASampleOfSessions() {
        twoSessions()
        assertEquals(0.5, store.coverageFraction(t(1_000), t(3_000)), 1e-9)      // 1_000 of 2_000
        assertEquals(1.0, store.coverageFraction(t(1_200), t(1_800)), 1e-9)
        assertEquals(0.0, store.coverageFraction(t(2_000), t(3_000)), 1e-9)      // the hole
    }

    @Test fun openSessionCountsUpToTheEndOfTheAskedInterval() {
        twoSessions()
        assertEquals(1.0, store.coverageFraction(t(3_500), t(9_000)), 1e-9)
        assertEquals(0.5, store.coverageFraction(t(2_000), t(4_000)), 1e-9)
    }

    @Test fun coversIntervalNeedsTheWholeIntervalInsideConnectedTime() {
        twoSessions()
        assertTrue(store.coversInterval(t(1_000), t(2_000)))
        assertTrue(store.coversInterval(t(3_500), t(8_000)))
        assertFalse(store.coversInterval(t(1_500), t(2_500)))   // runs into the hole
        assertFalse(store.coversInterval(t(500), t(1_500)))     // starts before the first session
    }

    @Test fun touchingSessionsCountAsOneContinuousCover() {
        store.openSession(t(1_000)); store.closeSession(t(2_000)); store.openSession(t(2_000)); store.closeSession(t(3_000))
        assertTrue(store.coversInterval(t(1_000), t(3_000)))
    }

    @Test fun emptyIntervalHasZeroFractionAndIsCoveredOnlyInsideASession() {
        twoSessions()
        assertEquals(0.0, store.coverageFraction(t(1_500), t(1_500)), 0.0)
        assertTrue(store.coversInterval(t(1_500), t(1_500)))
        assertFalse(store.coversInterval(t(2_500), t(2_500)))
    }

    @Test fun sessionsReturnsOnlyThoseTouchingTheInterval() {
        twoSessions()
        assertEquals(1, store.sessions(t(1_500), t(2_500)).size)
        assertEquals(0, store.sessions(t(2_000), t(3_000)).size)   // half-open: ends at 2_000, next begins at 3_000
        assertEquals(2, store.sessions(t(0), t(9_000)).size)
        assertEquals(null, store.sessions(t(3_000), t(9_000)).single().disconnectedAt)
    }

    @Test fun openingTwiceKeepsOneSessionAndClosingWithAnEarlierTimeNeverGoesNegative() {
        store.openSession(t(5_000)); store.openSession(t(6_000))
        assertEquals(1, store.sessions(t(0), t(9_000)).size)
        store.closeSession(t(4_000))
        assertEquals(5_000L, store.sessions(t(0), t(9_000)).single().disconnectedAt!!.value)
    }
}
