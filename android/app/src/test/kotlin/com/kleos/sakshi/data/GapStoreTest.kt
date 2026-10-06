package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.GapKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GapStoreTest : DbTest() {
    private val store get() = RoomGapStore(db)

    @Test fun pausingOpensAGapAndClosingItSetsTheEndOnce() {
        store.add(DataGap(GapKind.PAUSED, t(1_000), null))
        assertNull(store.overlapping(t(0), t(9_000)).single().end)
        store.closeOpenPause(t(2_000))
        store.closeOpenPause(t(3_000))   // nothing open now; must not move the end
        assertEquals(2_000L, store.overlapping(t(0), t(9_000)).single().end!!.value)
    }

    @Test fun closingAPauseNeverTouchesANotSeenGap() {
        store.add(DataGap(GapKind.NOT_SEEN, t(1_000), t(4_000)))
        store.add(DataGap(GapKind.PAUSED, t(5_000), null))
        store.closeOpenPause(t(6_000))
        val gaps = store.overlapping(t(0), t(9_000))
        assertEquals(4_000L, gaps.first { it.kind == GapKind.NOT_SEEN }.end!!.value)
        assertEquals(6_000L, gaps.first { it.kind == GapKind.PAUSED }.end!!.value)
    }

    @Test fun overlappingIsHalfOpenAndIncludesAnOpenGap() {
        store.add(DataGap(GapKind.NOT_SEEN, t(1_000), t(2_000)))
        store.add(DataGap(GapKind.PAUSED, t(5_000), null))
        assertEquals(0, store.overlapping(t(2_000), t(5_000)).size)   // first ended at 2_000, second starts at 5_000
        assertEquals(1, store.overlapping(t(1_999), t(2_001)).size)
        assertEquals(1, store.overlapping(t(9_000), t(10_000)).size)  // the open pause reaches here
    }
}
