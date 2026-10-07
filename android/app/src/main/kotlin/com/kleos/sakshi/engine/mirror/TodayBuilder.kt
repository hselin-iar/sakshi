package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.DataFlag
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.TodayView
import com.kleos.sakshi.engine.model.TodayWindowView
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.usecases.BuildWeekSummary
import com.kleos.sakshi.engine.windows.WindowLabeler
import java.time.ZoneId

/**
 * F8: today's finished windows only. A window still running is never shown, because a half-finished window would read as a verdict on
 * a day that is not over. No Steadiness, no suggestion, no comparison with anyone: just what finished and how it looked.
 */
object TodayBuilder {
    fun build(ports: Ports, asOf: EpochMs, zone: ZoneId, isDemo: Boolean): TodayView {
        val today = StudyDay.of(asOf, zone)
        val allDays = ports.derived.days(StudyDay(0), today)
        val validDays = allDays.filter { it.valid }.map { it.day }.toSet() + today   // today is not "valid" until it is over; its finished windows still count
        val history = BuildWeekSummary.qualifying(ports.derived.windows(EpochMs(0), asOf), validDays)
        val labels = WindowLabeler.labelAll(history)

        val finished = history.withIndex()
            .filter { (_, w) -> w.window.day == today && w.window.end.value <= asOf.value }
            .sortedBy { it.value.window.start.value }
        val views = finished.map { (i, w) ->
            TodayWindowView(
                start = w.window.start, end = w.window.end, shape = SentenceBuilder.shapeWord(labels[i]),
                stretchMin = w.stretches.maxOfOrNull { it.minutes }, stays = w.stays.size, returnMin = w.stays.mapNotNull { it.returnMinutes }.let { r ->
                    if (r.isEmpty()) null else r.sorted().let { s -> if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2 }
                },
            )
        }
        val parts = BuildWeekSummary.pool(finished.map { it.value })
        val flags = buildList {
            if (ports.events.count() == 0) add(DataFlag.FIRST_LOOK)
        }
        return TodayView(
            isDemo = isDemo, windows = views,
            parts = if (parts.stretchMin == null) null else MirrorBuilder.partsView(parts, finished.sumOf { it.value.glances }),
            line = SentenceBuilder.todayLine(views.size, parts.stretchMin), dataFlags = flags, dataLines = emptyList(),
        )
    }
}
