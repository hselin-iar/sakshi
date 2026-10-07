package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.RawType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventStoreTest : DbTest() {
    private val store get() = RoomEventStore(db)

    @Test fun appendingTheSameEventTwiceStoresItOnce() {
        store.append(listOf(raw(1_000), raw(2_000)))
        store.append(listOf(raw(2_000), raw(3_000)))   // the overlap margin re-reads 2_000
        assertEquals(3, store.count())
    }

    @Test fun screenEventsWithNoPackageAreDedupedToo() {
        // SQLite treats NULLs as distinct in a unique index; the store must not rely on that.
        val screenOn = raw(5_000, RawType.SCREEN_INTERACTIVE, pkg = null)
        store.append(listOf(screenOn)); store.append(listOf(screenOn))
        assertEquals(1, store.count())
        assertNull(store.range(t(0), t(10_000)).single().pkg)
    }

    @Test fun sameTimestampWithDifferentTypeOrPackageIsKept() {
        store.append(listOf(raw(1_000, RawType.ACTIVITY_RESUMED, "a"), raw(1_000, RawType.ACTIVITY_PAUSED, "a"), raw(1_000, RawType.ACTIVITY_RESUMED, "b")))
        assertEquals(3, store.count())
    }

    @Test fun rangeIncludesFromAndExcludesToAndIsSortedByTime() {
        store.append(listOf(raw(3_000), raw(1_000), raw(2_000), raw(4_000)))
        assertEquals(listOf(2_000L, 3_000L), store.range(t(2_000), t(4_000)).map { it.ts.value })
        assertEquals(emptyList<Long>(), store.range(t(2_000), t(2_000)).map { it.ts.value })
    }

    @Test fun oldestIsNullWhenEmptyElseTheEarliestTimestamp() {
        assertNull(store.oldest())
        store.append(listOf(raw(7_000), raw(3_000)))
        assertEquals(3_000L, store.oldest()!!.value)
    }

    @Test fun purgeBeforeRemovesStrictlyOlderAndReturnsTheCount() {
        store.append(listOf(raw(1_000), raw(2_000), raw(3_000)))
        assertEquals(2, store.purgeBefore(t(3_000)))
        assertEquals(listOf(3_000L), store.range(t(0), t(10_000)).map { it.ts.value })
    }

    @Test fun eventsReadBackEqualToWhatWasWritten() {
        val e = raw(9_000, RawType.KEYGUARD_SHOWN, pkg = null)
        store.append(listOf(e))
        assertEquals(e, store.range(t(0), t(10_000)).single())
    }
}
