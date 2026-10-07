package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.WeekSummary
import kotlin.math.abs

private enum class TrendPart(val higherIsBetter: Boolean) {
    STRETCH(true), STAYS(false), RETURN(false), QUIET(true)
}

/** P2: direction of each part over the last 4 non-unusual weekly values. */
object Trend : PatternDetector {
    override val kind = PatternKind.TREND

    override fun detect(ctx: PatternContext): List<Pattern> {
        val weeks = nonUnusualWeeks(ctx).sortedBy { it.weekStart.studyDay.epochDay }
        return TrendPart.entries.mapNotNull { part -> detectForPart(part, weeks, ctx) }
    }

    private fun detectForPart(part: TrendPart, weeks: List<WeekSummary>, ctx: PatternContext): Pattern? {
        val weeksWithValue = weeks.mapNotNull { week -> valueOf(part, week)?.let { week to it } }.takeLast(4)
        if (weeksWithValue.size < 4) return null
        val values = weeksWithValue.map { it.second }

        val changes = (1 until values.size).map { values[it] - values[it - 1] }
        val sameSign = changes.all { it > 0 } || changes.all { it < 0 }
        val eachAtLeast5Pct = (1 until values.size).all { i ->
            val base = values[i - 1]
            base != 0.0 && abs((values[i] - base) / base) >= 0.05
        }
        val run3 = sameSign && eachAtLeast5Pct

        val lastTwo = median(values.takeLast(2))
        val priorTwo = median(values.take(2))
        val relativeChange = if (priorTwo != 0.0) (lastTwo - priorTwo) / priorTwo else 0.0

        val direction = when {
            abs(relativeChange) <= 0.05 -> "FLAT"
            (relativeChange > 0) == part.higherIsBetter -> "BETTER"
            else -> "WORSE"
        }
        if (direction == "FLAT") return null

        return Pattern(
            kind = PatternKind.TREND,
            key = "trend:${part.name.lowercase()}",
            strength = abs(relativeChange),
            evidenceWindows = weeksWithValue.sumOf { it.first.windowsCount },
            evidenceDays = weeksWithValue.sumOf { it.first.validDays },
            firstSeen = ctx.asOf,
            lastSeen = ctx.asOf,
            args = mapOf(
                "part" to part.name.lowercase(),
                "values" to values.joinToString(","),
                "direction" to direction,
                "run3" to run3.toString(),
            ),
        )
    }

    private fun valueOf(part: TrendPart, week: WeekSummary): Double? = when (part) {
        TrendPart.STRETCH -> week.stretchMedianMin
        TrendPart.STAYS -> week.staysPerHour
        TrendPart.RETURN -> week.returnMedianMin
        TrendPart.QUIET -> week.quietShare
    }
}
