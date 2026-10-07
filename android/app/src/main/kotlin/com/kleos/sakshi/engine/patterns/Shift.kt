package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.tuning.Tuning
import kotlin.math.abs
import kotlin.math.max

private enum class ShiftPart { STRETCH, STAYS, RETURN, QUIET }

/**
 * P3: CUSUM change detector per weekly part series. DOC 3 doesn't explicitly
 * say whether "persistent" gates whether a Pattern is reported, only that
 * an alarm fires and persistence is then checked -- a single-week blip that
 * isn't persistent is treated here as not a pattern, matching "a pattern
 * needs recurrence" (this section's own opening line). Flagged as a
 * judgment call since no golden example distinguishes the two.
 */
object Shift : PatternDetector {
    override val kind = PatternKind.SHIFT

    override fun detect(ctx: PatternContext): List<Pattern> {
        val weeks = nonUnusualWeeks(ctx).sortedBy { it.weekStart.studyDay.epochDay }
        return ShiftPart.entries.mapNotNull { part -> detectForPart(part, weeks, ctx) }
    }

    private fun detectForPart(part: ShiftPart, weeks: List<WeekSummary>, ctx: PatternContext): Pattern? {
        val weeksWithValue = weeks.mapNotNull { week -> valueOf(part, week)?.let { week to it } }
        val values = weeksWithValue.map { it.second }
        if (values.size < 4) return null

        val baseCount = Tuning.CUSUM_BASE_WEEKS
        val mu = median(values.take(baseCount))
        val s = max(mad(values.take(baseCount)), Tuning.CUSUM_SCALE_FLOOR * mu)
        val k = Tuning.CUSUM_K * s
        val h = Tuning.CUSUM_H * s

        var sPlus = 0.0
        var sMinus = 0.0
        var alarmIndex: Int? = null
        var alarmDirection = ""
        for (t in values.indices) {
            sPlus = max(0.0, sPlus + (values[t] - mu - k))
            sMinus = max(0.0, sMinus + (mu - values[t] - k))
            if (alarmIndex == null && t > 2) {
                if (sPlus > h) {
                    alarmIndex = t
                    alarmDirection = "UP"
                } else if (sMinus > h) {
                    alarmIndex = t
                    alarmDirection = "DOWN"
                }
            }
        }
        val t = alarmIndex ?: return null

        val meanFromT = values.subList(t, values.size).average()
        val persistent = abs(meanFromT - mu) >= 2 * k
        if (!persistent) return null

        val evidence = weeksWithValue.subList(t, weeksWithValue.size)
        return Pattern(
            kind = PatternKind.SHIFT,
            key = "shift:${part.name.lowercase()}",
            strength = if (s > 0.0) abs(meanFromT - mu) / s else 0.0,
            evidenceWindows = evidence.sumOf { it.first.windowsCount },
            evidenceDays = evidence.sumOf { it.first.validDays },
            firstSeen = ctx.asOf,
            lastSeen = ctx.asOf,
            args = mapOf("part" to part.name.lowercase(), "weekIndex" to t.toString(), "direction" to alarmDirection),
        )
    }

    private fun valueOf(part: ShiftPart, week: WeekSummary): Double? = when (part) {
        ShiftPart.STRETCH -> week.stretchMedianMin
        ShiftPart.STAYS -> week.staysPerHour
        ShiftPart.RETURN -> week.returnMedianMin
        ShiftPart.QUIET -> week.quietShare
    }
}
