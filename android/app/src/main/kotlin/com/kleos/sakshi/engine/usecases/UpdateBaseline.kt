package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.baseline.BaselineService
import com.kleos.sakshi.engine.baseline.ValidDayData
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/**
 * F5: freeze the earliest BASELINE_VALID_DAYS valid days with study day >=
 * day(first_read_at); never overwritten once active. Re-anchor (once, at
 * week >= 4) is not wired here — T2.8's own scope stops at the first
 * freeze; nothing yet calls for a re-anchor.
 */
object UpdateBaseline {
    fun run(ports: Ports, asOf: EpochMs, zone: ZoneId): Boolean {
        val existing = ports.state.baseline()
        if (existing != null && existing.isActive) return false

        val firstReadAt = ports.state.settings().firstReadAt ?: return false
        val firstReadDay = StudyDay.of(firstReadAt, zone)
        val today = StudyDay.of(asOf, zone)
        if (today.epochDay < firstReadDay.epochDay) return false

        val validDays = ports.derived.days(firstReadDay, today).filter { it.valid }
        if (validDays.size < Tuning.BASELINE_VALID_DAYS) return false

        val earliest = validDays.sortedBy { it.day.epochDay }.take(Tuning.BASELINE_VALID_DAYS)
        val dayData = earliest.map { summary ->
            val windows = ports.derived.windows(summary.day.startEpochMs(zone), summary.day.endEpochMs(zone))
                .filter { !it.window.partial }
            ValidDayData(
                day = summary.day,
                endOfDay = summary.day.endEpochMs(zone),
                windows = windows.map { it.window },
                stretches = windows.flatMap { it.stretches },
                stays = windows.flatMap { it.stays },
            )
        }

        val baseline = BaselineService.computeBaseline(dayData) ?: return false
        ports.state.saveBaseline(baseline)
        return true
    }
}
