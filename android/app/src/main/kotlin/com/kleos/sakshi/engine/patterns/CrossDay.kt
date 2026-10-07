package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

private const val LATE_THRESHOLD_MIN = 20 * 60 + 30 // 20h30m after the study day's own 04:00 start = 00:30 next calendar day

// DOC 3 says "over the last 14 days" as a fixed lookback, but Tuning.kt has no constant for it
// (checked: RAW_RETENTION_DAYS and JUDGE_DAYS are both unrelated 14s). Same gap shape as T2.4's
// learn-study-hours and T2.10's pattern-retirement -- flagged here rather than silently hardcoded.
private const val LOOKBACK_DAYS = 14

/** CrossDay: last screen-off vs the next day's first stretch (the S8 source). */
object CrossDay : PatternDetector {
    override val kind = PatternKind.CROSS_DAY

    override fun detect(ctx: PatternContext): List<Pattern> {
        val days = ctx.days.sortedBy { it.day.epochDay }.takeLast(LOOKBACK_DAYS)
        if (days.size < 2) return emptyList()
        val byEpochDay = days.associateBy { it.day.epochDay }

        val lateFirstStretches = mutableListOf<Double>()
        val nonLateFirstStretches = mutableListOf<Double>()
        val lateScreenOffMinutes = mutableListOf<Int>()

        for (summary in days) {
            val next = byEpochDay[summary.day.epochDay + 1] ?: continue
            if (!next.valid || next.firstStretchMin == null) continue

            val elapsed = minutesSinceDayStart(summary, ctx.zone)
            if (elapsed != null && elapsed >= LATE_THRESHOLD_MIN) {
                lateFirstStretches += next.firstStretchMin
                lateScreenOffMinutes += elapsed
            } else {
                nonLateFirstStretches += next.firstStretchMin
            }
        }

        if (lateFirstStretches.size < Tuning.CROSS_DAY_MIN_NIGHTS || nonLateFirstStretches.isEmpty()) return emptyList()

        val meanLate = lateFirstStretches.average()
        val meanNonLate = nonLateFirstStretches.average()
        // CROSS_DAY_SHORTER (0.25) is "shorter BY this fraction", not the comparison multiplier
        // itself -- DOC 3's formula is "<= 0.75 x mean", i.e. (1 - CROSS_DAY_SHORTER) x mean.
        if (meanLate > (1.0 - Tuning.CROSS_DAY_SHORTER) * meanNonLate) return emptyList()

        return listOf(
            Pattern(
                kind = PatternKind.CROSS_DAY,
                key = "crossday",
                strength = 1.0 - (meanLate / meanNonLate),
                evidenceWindows = 0,
                evidenceDays = lateFirstStretches.size,
                firstSeen = ctx.asOf,
                lastSeen = ctx.asOf,
                args = mapOf(
                    "lateNights" to lateFirstStretches.size.toString(),
                    "shorterByMin" to (meanNonLate - meanLate).toString(),
                    "lateAfter" to lateAfterOf(lateScreenOffMinutes),
                ),
            ),
        )
    }

    private fun minutesSinceDayStart(summary: DaySummary, zone: ZoneId): Int? {
        val ts = summary.lastScreenOffTs ?: return null
        val dayStart = summary.day.startEpochMs(zone).value
        return ((ts.value - dayStart) / 60_000L).toInt()
    }

    // Median elapsed-minutes-since-day-start among late nights, converted back to a clock time
    // and rounded to 30 minutes. [JUDGMENT CALL: no golden example gives a concrete value.]
    private fun lateAfterOf(lateScreenOffMinutes: List<Int>): String {
        val medianMinutes = median(lateScreenOffMinutes.map { it.toDouble() })
        val sinceMidnight = (4 * 60 + medianMinutes).toInt() % (24 * 60) // day starts at 04:00
        val rounded = ((sinceMidnight + 15) / 30) * 30
        return "%02d:%02d".format((rounded / 60) % 24, rounded % 60)
    }
}
