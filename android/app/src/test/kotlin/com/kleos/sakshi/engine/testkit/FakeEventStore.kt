package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.ports.EventStore

/** Dumb in-memory store. No dedupe, no validation — honours the contract only. */
class FakeEventStore : EventStore {
    private val events = mutableListOf<RawEvent>()

    override fun append(events: List<RawEvent>) {
        this.events += events
    }

    override fun range(from: EpochMs, to: EpochMs): List<RawEvent> =
        events.filter { it.ts.value >= from.value && it.ts.value < to.value }
            .sortedBy { it.ts.value }

    override fun oldest(): EpochMs? = events.minByOrNull { it.ts.value }?.ts

    override fun count(): Int = events.size

    override fun purgeBefore(ts: EpochMs): Int {
        val before = events.size
        events.removeAll { it.ts.value < ts.value }
        return before - events.size
    }
}
