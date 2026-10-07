package com.kleos.sakshi.engine.patterns

import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val MIN_WINDOW_MS = 20 * 60_000L

private enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }
private enum class WeekPart { WEEKDAY, WEEKEND }

/** F11's S1 input: the quietest, longest-stretched 2-hour local slot. Not a Pattern itself. */
data class ClearHourResult(val startHour: Int, val endHour: Int, val stretchMin: Double)

/** P1: stay rate by day-part x weekday/weekend (8 cells), plus the clear-hour slot for S1. */
object Rhythm : PatternDetector {
    override val kind = PatternKind.RHYTHM

    override fun detect(ctx: PatternContext): List<Pattern> {
        val windows = qualifyingWindows(ctx).filter { (it.window.end.value - it.window.start.value) >= MIN_WINDOW_MS }
        if (windows.isEmpty()) return emptyList()

        // Not associateWith: WindowWithDetail is a data class, and two structurally-identical
        // windows (same fields, different fixture entries) would collapse into one map key,
        // silently dropping a real window instead of just reusing its rate.
        val overall = median(windows.map { rate(it) })

        return windows.groupBy { cellKey(it, ctx.zone) }.mapNotNull { (key, cellWindows) ->
            val cellValue = median(cellWindows.map { rate(it) })
            val ratio = (cellValue + Tuning.RATE_EPS) / (overall + Tuning.RATE_EPS)
            val cellDays = cellWindows.map { it.window.day }.distinct().size
            if (!EvidenceGate.passes(cellWindows.size, cellDays, ratio)) return@mapNotNull null

            val direction = if (ratio >= Tuning.EVIDENCE_RATIO_HI) "CHOPPY" else "CLEAR"
            Pattern(
                kind = PatternKind.RHYTHM,
                key = "cell:$key",
                strength = ratio,
                evidenceWindows = cellWindows.size,
                evidenceDays = cellDays,
                firstSeen = ctx.asOf,
                lastSeen = ctx.asOf,
                args = mapOf(
                    "cell" to key,
                    "cellRate" to cellValue.toString(),
                    "overallRate" to overall.toString(),
                    "direction" to direction,
                ),
            )
        }
    }

    /** The slot with stays/hour <= 0.5x overall AND stretch >= 1.5x overall stretch, across >= 3 windows. */
    fun clearHour(ctx: PatternContext): ClearHourResult? {
        val windows = qualifyingWindows(ctx)
        if (windows.isEmpty()) return null

        val overallRate = median(windows.map { rate(it) })
        val overallStretchMin = median(windows.map { stretchMinutesOf(it) })

        return windows.groupBy { slotStartOf(it, ctx.zone) }
            .filterValues { it.size >= 3 }
            .entries
            .map { (slotStart, slotWindows) ->
                ClearHourResult(
                    startHour = slotStart,
                    endHour = slotStart + 2,
                    stretchMin = median(slotWindows.map { stretchMinutesOf(it) }),
                ) to median(slotWindows.map { rate(it) })
            }
            .firstOrNull { (result, slotRate) ->
                slotRate <= 0.5 * overallRate && result.stretchMin >= 1.5 * overallStretchMin
            }
            ?.first
    }

    private fun rate(wd: WindowWithDetail): Double {
        val hours = (wd.window.end.value - wd.window.start.value) / 3_600_000.0
        return wd.stays.size / hours
    }

    private fun stretchMinutesOf(wd: WindowWithDetail): Double =
        (wd.window.end.value - wd.window.start.value) / 60_000.0

    internal fun cellKey(wd: WindowWithDetail, zone: ZoneId): String {
        val hour = Instant.ofEpochMilli(wd.window.start.value).atZone(zone).hour
        return "${dayPartOf(hour)}-${weekPartOf(wd.window.day.epochDay)}"
    }

    private fun dayPartOf(hour: Int): DayPart = when {
        hour in 4 until 12 -> DayPart.MORNING
        hour in 12 until 17 -> DayPart.AFTERNOON
        hour in 17 until 22 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }

    private fun weekPartOf(epochDay: Long): WeekPart {
        val dow = LocalDate.ofEpochDay(epochDay).dayOfWeek
        return if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) WeekPart.WEEKEND else WeekPart.WEEKDAY
    }

    private fun slotStartOf(wd: WindowWithDetail, zone: ZoneId): Int {
        val hour = Instant.ofEpochMilli(wd.window.start.value).atZone(zone).hour
        return (hour / 2) * 2
    }
}
