package com.kleos.sakshi.engine.windows

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.StudyBlock
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowSource
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.LocalDate
import java.time.ZoneId

/**
 * F3's window-finding algorithm. Learn-study-hours derivation ("median
 * start/end of >= 14 inferred windows") is deliberately NOT built here: DOC 3
 * names "14" as the threshold but Tuning.kt has no constant for it, and every
 * number must be a named Tuning constant (LC-7 locks the names). Adding one
 * is a Contract Change, not something this step can do on its own — flagged
 * to the Integration Owner instead of hardcoding or silently adding a name.
 */
object WindowFinder {
    fun find(
        day: StudyDay,
        intervals: List<ForegroundInterval>,
        classes: List<AppClass>,
        studyBlocks: List<StudyBlock>,
        gaps: List<DataGap>,
        asOf: EpochMs,
        zone: ZoneId,
    ): List<Window> {
        val placed = studyBlocks.map { placeOnDay(day, it, zone) to WindowSource.STUDY_HOURS }
        val paddedRuns = runs(intervals, classes).map { it to WindowSource.INFERRED }
        val merged = mergeTagged(placed + paddedRuns)

        return merged.map { (span, source) ->
            val (start, end) = span
            Window(
                id = 0L,
                day = day,
                start = EpochMs(start),
                end = EpochMs(end),
                source = source,
                partial = overlapsAnyGap(start, end, gaps),
                finalised = end + Tuning.WINDOW_FINALISE_LAG_MIN * 60_000L <= asOf.value,
                shape = null,
            )
        }
    }

    fun isValidDay(windows: List<Window>): Boolean {
        val nonPartialMinutes = windows.filter { !it.partial }
            .sumOf { (it.end.value - it.start.value) / 60_000.0 }
        return nonPartialMinutes >= Tuning.VALID_DAY_WINDOW_MIN
    }

    private fun placeOnDay(day: StudyDay, block: StudyBlock, zone: ZoneId): Pair<Long, Long> {
        val date = LocalDate.ofEpochDay(day.epochDay)
        val midnight = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val start = midnight + block.startMinute * 60_000L
        val crossesMidnight = block.endMinute < block.startMinute
        val endMidnight = if (crossesMidnight) {
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        } else {
            midnight
        }
        val end = endMidnight + block.endMinute * 60_000L
        return start to end
    }

    // Chain effective-in-set intervals with gaps <= RUN_MAX_GAP_MIN, keep chains whose total
    // in-set time >= RUN_MIN_IN_SET_MIN, then pad each surviving chain by WINDOW_PAD_MIN.
    private fun runs(intervals: List<ForegroundInterval>, classes: List<AppClass>): List<Pair<Long, Long>> {
        val inSet = intervals.indices.filter { classes[it] == AppClass.IN_SET }.map { intervals[it] }
        if (inSet.isEmpty()) return emptyList()

        val maxGapMs = Tuning.RUN_MAX_GAP_MIN * 60_000L
        val minInSetMs = Tuning.RUN_MIN_IN_SET_MIN * 60_000L
        val padMs = Tuning.WINDOW_PAD_MIN * 60_000L

        val chains = mutableListOf(mutableListOf(inSet.first()))
        for (interval in inSet.drop(1)) {
            val chain = chains.last()
            val gap = interval.start.value - chain.last().end.value
            if (gap <= maxGapMs) {
                chain += interval
            } else {
                chains += mutableListOf(interval)
            }
        }

        return chains
            .filter { chain -> chain.sumOf { it.end.value - it.start.value } >= minInSetMs }
            .map { chain -> (chain.first().start.value - padMs) to (chain.last().end.value + padMs) }
    }

    // Union, merging spans that overlap or touch; BOTH when a merged window combines both sources.
    private fun mergeTagged(tagged: List<Pair<Pair<Long, Long>, WindowSource>>): List<Pair<Pair<Long, Long>, WindowSource>> {
        if (tagged.isEmpty()) return emptyList()
        val sorted = tagged.sortedBy { it.first.first }

        val merged = mutableListOf<Pair<Pair<Long, Long>, MutableSet<WindowSource>>>()
        for ((span, source) in sorted) {
            val last = merged.lastOrNull()
            if (last != null && span.first <= last.first.second) {
                val newSpan = last.first.first to maxOf(last.first.second, span.second)
                merged[merged.lastIndex] = newSpan to last.second.apply { add(source) }
            } else {
                merged += span to mutableSetOf(source)
            }
        }

        return merged.map { (span, sources) ->
            val source = when {
                WindowSource.STUDY_HOURS in sources && WindowSource.INFERRED in sources -> WindowSource.BOTH
                WindowSource.STUDY_HOURS in sources -> WindowSource.STUDY_HOURS
                else -> WindowSource.INFERRED
            }
            span to source
        }
    }

    private fun overlapsAnyGap(start: Long, end: Long, gaps: List<DataGap>): Boolean =
        gaps.any { gap -> gap.start.value < end && (gap.end?.value ?: Long.MAX_VALUE) > start }
}
