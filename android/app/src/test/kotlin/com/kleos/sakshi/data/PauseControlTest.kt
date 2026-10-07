package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.host.PauseControl
import org.junit.Assert.*
import org.junit.Test

class PauseControlTest : DbTest() {
    private val state get() = RoomStateStore(db)
    private val gaps get() = RoomGapStore(db)
    private val pause get() = PauseControl(state, gaps)
    private fun allGaps() = gaps.overlapping(t(0), t(Long.MAX_VALUE))

    @Test fun pausingCreatesExactlyOneOpenPausedGap() {
        pause.set(true, t(1_000))
        pause.set(true, t(2_000))      // a second tap changes nothing
        val gap = allGaps().single()
        assertEquals(GapKind.PAUSED, gap.kind); assertEquals(1_000L, gap.start.value); assertNull(gap.end)
        assertTrue(state.ingest().paused); assertEquals(1_000L, state.ingest().pausedSince!!.value)
    }

    @Test fun resumingClosesThatGapAndMovesTheCursorToNowWithoutBackfilling() {
        state.saveIngest(state.ingest().copy(cursor = t(500)))
        pause.set(true, t(1_000)); pause.set(false, t(9_000))
        assertEquals(9_000L, allGaps().single().end!!.value)
        val ingest = state.ingest()
        assertFalse(ingest.paused); assertNull(ingest.pausedSince); assertEquals(9_000L, ingest.cursor!!.value)
    }

    @Test fun resumingWhenNotPausedDoesNothing() {
        state.saveIngest(state.ingest().copy(cursor = t(500)))
        pause.set(false, t(9_000))
        assertEquals(0, allGaps().size); assertEquals(500L, state.ingest().cursor!!.value)
    }

    @Test fun eachPauseResumeCycleIsItsOwnGap() {
        pause.set(true, t(1_000)); pause.set(false, t(2_000)); pause.set(true, t(3_000)); pause.set(false, t(4_000))
        assertEquals(listOf(1_000L to 2_000L, 3_000L to 4_000L), allGaps().map { it.start.value to it.end!!.value })
    }
}
