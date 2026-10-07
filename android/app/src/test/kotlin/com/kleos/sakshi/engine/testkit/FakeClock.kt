package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.ports.Clock

class FakeClock(private var current: EpochMs) : Clock {
    override fun now(): EpochMs = current

    fun set(ts: EpochMs) {
        current = ts
    }

    fun advanceMs(ms: Long) {
        current = EpochMs(current.value + ms)
    }
}
