package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.intervals.reconstruct
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.tuning.Tuning

/** opens: count of merged own-package intervals >= 3s. minutes: total merged time, any length. */
data class OwnUse(val opens: Int, val minutes: Double)

/**
 * F14: reconstruct foreground intervals for Sakshi's own package only (the
 * caller pre-filters ownEvents), merge gaps < OWN_OPEN_MERGE_SEC, then an
 * "open" is one merged interval >= 3s. The falling-vs-plain line choice
 * (comparing this week's opens to the previous week's) is SentenceBuilder's
 * job (T2.14) — this returns data only.
 */
object TeacherLeaves {
    private const val MIN_OPEN_MS = 3_000L

    fun meter(ownEvents: List<RawEvent>, asOf: EpochMs): OwnUse {
        val intervals = reconstruct(ownEvents, asOf).intervals.sortedBy { it.start.value }
        if (intervals.isEmpty()) return OwnUse(opens = 0, minutes = 0.0)

        val mergeGapMs = Tuning.OWN_OPEN_MERGE_SEC * 1_000L
        val merged = mutableListOf(intervals.first().start.value to intervals.first().end.value)
        for (interval in intervals.drop(1)) {
            val (lastStart, lastEnd) = merged.last()
            if (interval.start.value - lastEnd < mergeGapMs) {
                merged[merged.lastIndex] = lastStart to interval.end.value
            } else {
                merged += interval.start.value to interval.end.value
            }
        }

        val durationsMs = merged.map { (s, e) -> e - s }
        val opens = durationsMs.count { it >= MIN_OPEN_MS }
        val minutes = durationsMs.sum() / 60_000.0
        return OwnUse(opens, minutes)
    }
}
