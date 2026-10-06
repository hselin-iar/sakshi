package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.Saying
import com.kleos.sakshi.engine.ports.SayingShelf

class FakeSayingShelf(private var sayings: List<Saying> = emptyList()) : SayingShelf {
    override fun all(): List<Saying> = sayings

    fun setSayings(value: List<Saying>) {
        sayings = value
    }
}
