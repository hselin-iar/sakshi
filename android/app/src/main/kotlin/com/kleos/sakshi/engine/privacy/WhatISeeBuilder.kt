package com.kleos.sakshi.engine.privacy

import com.kleos.sakshi.engine.mirror.SentenceBuilder
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.HostFacts
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WhatISeeView
import com.kleos.sakshi.engine.ports.Ports
import java.time.ZoneId

/**
 * F10: the What I see page, built from what is actually stored, so it cannot drift from reality. Counts are counts, a missing
 * measure is null and its sentence says so, never a zero.
 */
object WhatISeeBuilder {
    private const val DAY_MS = 86_400_000L

    fun build(ports: Ports, asOf: EpochMs, host: HostFacts, zone: ZoneId, isDemo: Boolean): WhatISeeView {
        val ingest = ports.state.ingest()
        val everHeard = ports.coverage.sessions(EpochMs(0), EpochMs(asOf.value + 1)).isNotEmpty()
        val coverage7d = if (everHeard) ports.coverage.coverageFraction(EpochMs(asOf.value - 7 * DAY_MS), asOf) else null
        val lastRun = ingest.lastWorkerRunAt
        val minutesAgo = lastRun?.let { ((asOf.value - it.value).coerceAtLeast(0) / 60_000L).toInt() }

        return WhatISeeView(
            isDemo = isDemo, usageAccessGranted = host.usageAccessGranted, notificationAccessGranted = host.notificationAccessGranted,
            rawEventCount = ports.events.count(), notifEventCount = ports.notifs.count(), oldestRawEvent = ports.events.oldest(),
            derivedDays = ports.derived.days(StudyDay(0), StudyDay.of(asOf, zone)).size, listenerCoverage7d = coverage7d,
            lastWorkerRun = lastRun, workerRuns7d = ingest.workerRuns7d, paused = ingest.paused,
            lastError = host.lastError ?: ingest.lastError, oddEventPairs = ingest.oddEventPairs,
            lines = SentenceBuilder.whatISeeLines(coverage7d, minutesAgo, ingest.workerRuns7d),
        )
    }
}
