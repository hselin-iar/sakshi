package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.DayDerivation
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.ports.DerivedStore

/**
 * Dumb in-memory store keyed the same way a real (Room) adapter would be:
 * one DayDerivation per StudyDay, one WeekSummary per WeekStart.
 */
class FakeDerivedStore : DerivedStore {
    private val byDay = linkedMapOf<Long, DayDerivation>()
    private val byWeek = linkedMapOf<Long, WeekSummary>()
    private val patternList = mutableListOf<Pattern>()

    override fun replaceDay(day: StudyDay, d: DayDerivation) {
        byDay[day.epochDay] = d
    }

    override fun windows(from: EpochMs, to: EpochMs): List<WindowWithDetail> =
        byDay.values.flatMap { it.windows }
            .filter { it.window.start.value < to.value && it.window.end.value > from.value }
            .sortedBy { it.window.start.value }

    override fun days(from: StudyDay, to: StudyDay): List<DaySummary> =
        byDay.values.map { it.summary }
            .filter { it.day.epochDay in from.epochDay..to.epochDay }
            .sortedBy { it.day.epochDay }

    override fun upsertWeek(w: WeekSummary) {
        byWeek[w.weekStart.studyDay.epochDay] = w
    }

    override fun weeks(): List<WeekSummary> =
        byWeek.values.sortedBy { it.weekStart.studyDay.epochDay }

    override fun upsertPatterns(p: List<Pattern>) {
        patternList += p
    }

    override fun patterns(): List<Pattern> = patternList.toList()

    override fun clearDerived() {
        byDay.clear()
        byWeek.clear()
        patternList.clear()
    }

    override fun clearAll() {
        clearDerived()
    }
}
