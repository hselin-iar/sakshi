package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.DataFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F17's golden checks (DOC 3/DOC4): coverage 0.69/0.70 boundary; 3/4 window boundary. */
class DataStatesTest {

    private fun baseFlags(
        coverage: Double = 1.0,
        nonPartialWindowCount: Int = 10,
        validDayCount: Int = 7,
    ) = DataStates.flags(
        notificationAccessGranted = true,
        everHadListenerSession = true,
        listenerCoverageFraction = coverage,
        notSeenDays = 0,
        pausedDays = 0,
        nonPartialWindowCount = nonPartialWindowCount,
        validDayCount = validDayCount,
        weekWindowMinutes = 1000.0,
        baselineMeanWeeklyMinutes = null,
        provisional = false,
    )

    @Test
    fun `coverage 0-69 is PARTIAL_PING`() {
        assertTrue(DataFlag.PARTIAL_PING in baseFlags(coverage = 0.69))
    }

    @Test
    fun `coverage 0-70 is not PARTIAL_PING`() {
        assertFalse(DataFlag.PARTIAL_PING in baseFlags(coverage = 0.70))
    }

    @Test
    fun `no notification access is PING_OFF regardless of coverage`() {
        val flags = DataStates.flags(
            notificationAccessGranted = false, everHadListenerSession = true, listenerCoverageFraction = 1.0,
            notSeenDays = 0, pausedDays = 0, nonPartialWindowCount = 10, validDayCount = 7,
            weekWindowMinutes = 1000.0, baselineMeanWeeklyMinutes = null, provisional = false,
        )
        assertTrue(DataFlag.PING_OFF in flags)
        assertFalse(DataFlag.PARTIAL_PING in flags)
    }

    @Test
    fun `3 non-partial windows is TOO_LITTLE_DATA`() {
        assertTrue(DataFlag.TOO_LITTLE_DATA in baseFlags(nonPartialWindowCount = 3))
    }

    @Test
    fun `4 non-partial windows is not TOO_LITTLE_DATA`() {
        assertFalse(DataFlag.TOO_LITTLE_DATA in baseFlags(nonPartialWindowCount = 4))
    }

    @Test
    fun `zero valid days is TOO_LITTLE_DATA even with enough windows`() {
        assertTrue(DataFlag.TOO_LITTLE_DATA in baseFlags(nonPartialWindowCount = 10, validDayCount = 0))
    }

    @Test
    fun `an unusual week is flagged against the baseline mean`() {
        val flags = DataStates.flags(
            notificationAccessGranted = true, everHadListenerSession = true, listenerCoverageFraction = 1.0,
            notSeenDays = 0, pausedDays = 0, nonPartialWindowCount = 10, validDayCount = 7,
            weekWindowMinutes = 100.0, baselineMeanWeeklyMinutes = 300.0, provisional = false,
        )
        assertTrue(DataFlag.UNUSUAL_WEEK in flags)
    }

    @Test
    fun `sentences render the fixed wording for each flag, INTERNAL_PARTIAL has none`() {
        val sentences = DataStates.sentences(
            flags = listOf(DataFlag.NOT_SEEN, DataFlag.PAUSED, DataFlag.INTERNAL_PARTIAL),
            notSeenDays = 2, pausedDays = 1, lastPingDaysAgo = null,
        )
        assertEquals(2, sentences.size)
        assertTrue(sentences[0].contains("2 days"))
        assertTrue(sentences[1].contains("1 days"))
    }
}
