package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.testkit.epochMsAt
import com.kleos.sakshi.engine.testkit.events
import org.junit.Assert.assertEquals
import org.junit.Test

/** F14's golden checks (DOC 3/DOC4): the 30s merge and the 3s open threshold. */
class TeacherLeavesTest {

    @Test
    fun `two own intervals 20s apart merge into one open`() {
        val ownEvents = events {
            at("10:00:00") resume ("S")
            at("10:00:05") pause ("S")
            at("10:00:25") resume ("S")
            at("10:00:30") pause ("S")
        }
        val result = TeacherLeaves.meter(ownEvents, epochMsAt("12:00:00"))
        assertEquals(1, result.opens)
    }

    @Test
    fun `two own intervals 31s apart stay two separate opens`() {
        val ownEvents = events {
            at("10:00:00") resume ("S")
            at("10:00:05") pause ("S")
            at("10:00:36") resume ("S")
            at("10:00:41") pause ("S")
        }
        val result = TeacherLeaves.meter(ownEvents, epochMsAt("12:00:00"))
        assertEquals(2, result.opens)
    }

    @Test
    fun `an isolated 2-second interval is not an open`() {
        val ownEvents = events { at("10:00:00") resume ("S"); at("10:00:02") pause ("S") }
        val result = TeacherLeaves.meter(ownEvents, epochMsAt("12:00:01"))
        assertEquals(0, result.opens)
    }
}
