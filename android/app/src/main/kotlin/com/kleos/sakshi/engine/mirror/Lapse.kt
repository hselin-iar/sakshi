package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.model.LapseInfo
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/**
 * F15 (D12): the most recent run of >= LAPSE_MIN_DAYS consecutive study
 * days, each after firstReadDay, where the day is inactive (no non-own
 * ACTIVITY_RESUMED — DaySummary.externalResumes == 0) AND not >= 50%
 * covered by a NOT_SEEN/PAUSED gap. A gap-covered day doesn't count as
 * inactive OR break a run transparently — it simply isn't eligible, which
 * breaks any run spanning it (the gap explains the silence; it isn't a
 * lapse, per F15's error-handling rule).
 *
 * "Shown once" (settings.lapse_acknowledged_through) is Mirror-building
 * orchestration, not part of this pure detection function.
 */
object Lapse {
    fun detect(days: List<DaySummary>, gaps: List<DataGap>, firstReadDay: StudyDay, zone: ZoneId): LapseInfo? {
        if (days.isEmpty()) return null
        val byEpochDay = days.associateBy { it.day.epochDay }
        val minDay = maxOf(firstReadDay.epochDay, days.minOf { it.day.epochDay })
        val maxDay = days.maxOf { it.day.epochDay }

        var runStart: Long? = null
        var runLength = 0
        var best: Pair<Long, Int>? = null // (endEpochDay, length) of the latest qualifying run

        for (epochDay in minDay..maxDay) {
            val summary = byEpochDay[epochDay]
            val gapCovered = isGapCovered(StudyDay(epochDay), gaps, zone)
            val inactive = !gapCovered && summary != null && summary.externalResumes == 0

            if (inactive) {
                if (runStart == null) runStart = epochDay
                runLength++
                if (runLength >= Tuning.LAPSE_MIN_DAYS) best = epochDay to runLength
            } else {
                runStart = null
                runLength = 0
            }
        }

        val (endDay, length) = best ?: return null
        return LapseInfo(days = length, endedAt = StudyDay(endDay))
    }

    private fun isGapCovered(day: StudyDay, gaps: List<DataGap>, zone: ZoneId): Boolean {
        val dayStart = day.startEpochMs(zone).value
        val dayEnd = day.endEpochMs(zone).value
        val totalMs = dayEnd - dayStart
        val coveredMs = gaps.asSequence()
            .filter { it.kind == GapKind.NOT_SEEN || it.kind == GapKind.PAUSED }
            .sumOf { gap ->
                val s = maxOf(gap.start.value, dayStart)
                val e = minOf(gap.end?.value ?: dayEnd, dayEnd)
                maxOf(0L, e - s)
            }
        return coveredMs >= 0.5 * totalMs
    }
}
