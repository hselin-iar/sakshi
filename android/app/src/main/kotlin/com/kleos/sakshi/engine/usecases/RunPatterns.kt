package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.patterns.PatternContext
import com.kleos.sakshi.engine.patterns.PatternEngine
import com.kleos.sakshi.engine.ports.Ports
import java.time.ZoneId

/** Detect patterns over everything derived so far and upsert them by (kind, key). No detector reads raw events. */
object RunPatterns {
    fun run(ports: Ports, asOf: EpochMs, zone: ZoneId) {
        val days = ports.derived.days(StudyDay(0), StudyDay.of(asOf, zone))
        if (days.isEmpty()) return
        val ctx = PatternContext(
            windows = ports.derived.windows(EpochMs(0), asOf), weeks = ports.derived.weeks(), days = days,
            baseline = ports.state.baseline(), asOf = asOf, random = ports.random, zone = zone,
        )
        ports.derived.upsertPatterns(PatternEngine.run(ctx, existing = ports.derived.patterns()))
    }
}
