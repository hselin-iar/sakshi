package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
import java.time.LocalDate
import java.time.ZoneId

/**
 * Hand-written event streams for golden tests, e.g.
 *
 *   val stream = events {
 *       at("10:00:00") resume ("A")
 *       at("10:05:00") pause ("A")
 *       at("10:05:00") resume ("B")
 *       at("10:07:00").nonInteractive()
 *   }
 *
 * `at(...)` sets the current timestamp on the builder; the call right after
 * it (resume/pause/interactive/nonInteractive/keyguardShown/keyguardHidden)
 * reads that timestamp to append one RawEvent. No production logic lives
 * here — this only builds LC-1 vocabulary values.
 */
class EventStreamBuilder(private val date: LocalDate, private val zone: ZoneId) {
    private val built = mutableListOf<RawEvent>()
    private var pendingTs: EpochMs? = null

    fun at(time: String): EventStreamBuilder {
        pendingTs = epochMsAt(time, date, zone)
        return this
    }

    infix fun resume(pkg: String) = add(RawType.ACTIVITY_RESUMED, Pkg(pkg))
    infix fun pause(pkg: String) = add(RawType.ACTIVITY_PAUSED, Pkg(pkg))
    fun interactive() = add(RawType.SCREEN_INTERACTIVE, null)
    fun nonInteractive() = add(RawType.SCREEN_NON_INTERACTIVE, null)
    fun keyguardShown() = add(RawType.KEYGUARD_SHOWN, null)
    fun keyguardHidden() = add(RawType.KEYGUARD_HIDDEN, null)

    private fun add(type: RawType, pkg: Pkg?) {
        val ts = checkNotNull(pendingTs) { "call at(\"HH:MM:SS\") before resume/pause/etc." }
        built += RawEvent(ts, type, pkg)
        pendingTs = null
    }

    fun build(): List<RawEvent> = built.sortedBy { it.ts.value }
}

fun events(
    date: LocalDate = GOLDEN_DATE,
    zone: ZoneId = Zones.KOLKATA,
    block: EventStreamBuilder.() -> Unit,
): List<RawEvent> = EventStreamBuilder(date, zone).apply(block).build()
