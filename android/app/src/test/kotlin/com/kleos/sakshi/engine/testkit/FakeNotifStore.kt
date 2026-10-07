package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.ports.NotifStore

class FakeNotifStore : NotifStore {
    private val events = mutableListOf<NotifEvent>()

    override fun append(e: NotifEvent) {
        events += e
    }

    override fun range(from: EpochMs, to: EpochMs): List<NotifEvent> =
        events.filter { it.ts.value >= from.value && it.ts.value < to.value }
            .sortedBy { it.ts.value }

    override fun count(): Int = events.size

    override fun purgeBefore(ts: EpochMs): Int {
        val before = events.size
        events.removeAll { it.ts.value < ts.value }
        return before - events.size
    }
}
