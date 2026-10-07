package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.RawType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Done-when: a test using the DSL builds the G-I1 stream and the fakes round-trip it. */
class EventStreamDslTest {

    @Test
    fun `G-I1 stream builds and round-trips through FakeEventStore`() {
        val stream = events {
            at("10:00:00") resume ("A")
            at("10:05:00") pause ("A")
            at("10:05:00") resume ("B")
            at("10:07:00").nonInteractive()
        }

        assertEquals(4, stream.size)
        assertEquals(RawType.ACTIVITY_RESUMED, stream[0].type)
        assertEquals(RawType.SCREEN_NON_INTERACTIVE, stream.last().type)

        val store = FakeEventStore()
        store.append(stream)

        val from = epochMsAt("00:00:00")
        val to = epochMsAt("23:59:59")
        val roundTripped = store.range(from, to)

        assertEquals(stream.map { it.ts.value to it.type }, roundTripped.map { it.ts.value to it.type })
        assertEquals(4, store.count())
        assertEquals(stream.first().ts, store.oldest())
    }
}
