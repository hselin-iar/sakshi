package com.kleos.sakshi.engine.metrics

import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * F5: pooling for a week. The caller filters to non-partial windows with
 * study day in the week, from valid days only, before calling pool() — this
 * is the only place pooling is defined, but it does not decide which
 * windows/stretches/stays qualify.
 */
object WeekMetrics {
    fun pool(stretches: List<Stretch>, stays: List<Stay>, windows: List<Window>): Parts {
        val windowHours = windows.sumOf { (it.end.value - it.start.value) / 3_600_000.0 }
        val windowMinutes = windows.sumOf { (it.end.value - it.start.value) / 60_000.0 }
        val quietMinutes = stretches.sumOf { it.quietMinutes }
        val inSetMinutes = stretches.sumOf { it.inSetMinutes }

        return Parts(
            stretchMin = lengthWeightedMedian(stretches.map { it.minutes }),
            longestStretchMin = stretches.maxOfOrNull { it.minutes },
            staysPerHour = (stays.size + Tuning.STAYS_SMOOTH_STAYS) / (windowHours + Tuning.STAYS_SMOOTH_HOURS),
            returnMin = median(stays.mapNotNull { it.returnMinutes }),
            quietShare = if (windowMinutes > 0) quietMinutes / windowMinutes else null,
            inSetShare = if (windowMinutes > 0) inSetMinutes / windowMinutes else null,
        )
    }

    // C: sort stretches by length descending; add lengths until the running total >= 50% of
    // the total stretch minutes; C = the length at which that happens.
    private fun lengthWeightedMedian(lengths: List<Double>): Double? {
        if (lengths.isEmpty()) return null
        val sorted = lengths.sortedDescending()
        val total = sorted.sum()
        if (total <= 0.0) return null
        var running = 0.0
        for (length in sorted) {
            running += length
            if (running >= 0.5 * total) return length
        }
        return sorted.last()
    }

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }
}
