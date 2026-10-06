package com.kleos.sakshi.engine.stays

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * F4: return = time from stay.start to the first moment the user is
 * effectively in-set for >= RETURN_BACK_IN_SET_SEC, OR to the start of a
 * screen-off span lasting >= RETURN_SCREEN_OFF_MIN (a put-down), whichever
 * is first; null if neither happens before window end ("unresolved").
 * `classes` must already be Depends-resolved, aligned by index with
 * `intervals`, same convention as StayDetector.
 */
object Returns {
    fun returnMinutes(
        stay: Stay,
        intervals: List<ForegroundInterval>,
        classes: List<AppClass>,
        screenOff: List<ScreenSpan>,
        windowEnd: EpochMs,
    ): Double? {
        val from = stay.end.value
        val limit = windowEnd.value

        val inSetStart = firstQualifyingInSetRunStart(intervals, classes, screenOff, from, limit)
        val putDownStart = firstQualifyingPutDownStart(screenOff, from, limit)

        val first = listOfNotNull(inSetStart, putDownStart).minOrNull() ?: return null
        return (first - stay.start.value) / 60_000.0
    }

    private fun firstQualifyingInSetRunStart(
        intervals: List<ForegroundInterval>,
        classes: List<AppClass>,
        screenOff: List<ScreenSpan>,
        from: Long,
        limit: Long,
    ): Long? {
        data class TypedSpan(val start: Long, val end: Long, val cls: AppClass?, val isScreenOff: Boolean)

        val appSpans = intervals.indices.mapNotNull { i ->
            val interval = intervals[i]
            val start = maxOf(interval.start.value, from)
            val end = minOf(interval.end.value, limit)
            if (end > start) TypedSpan(start, end, classes[i], false) else null
        }
        val offSpans = screenOff.mapNotNull { span ->
            val start = maxOf(span.start.value, from)
            val end = minOf(span.end?.value ?: limit, limit)
            if (end > start) TypedSpan(start, end, null, true) else null
        }
        val timeline = (appSpans + offSpans).sortedBy { it.start }

        val minMs = Tuning.RETURN_BACK_IN_SET_SEC * 1_000L
        var runStart: Long? = null
        var cursorEnd = from

        for (span in timeline) {
            if (span.start > cursorEnd) runStart = null // a recording gap ends any in-progress run

            when {
                span.isScreenOff -> runStart = null
                span.cls == AppClass.IN_SET -> {
                    val start = runStart ?: span.start
                    runStart = start
                    if (span.end - start >= minMs) return start
                }
                span.cls == AppClass.NEUTRAL -> Unit // transparent: runStart (if any) passes through
                span.cls == AppClass.OFF_SET -> runStart = null
            }
            cursorEnd = maxOf(cursorEnd, span.end)
        }
        return null
    }

    private fun firstQualifyingPutDownStart(screenOff: List<ScreenSpan>, from: Long, limit: Long): Long? {
        val minMs = Tuning.RETURN_SCREEN_OFF_MIN * 60_000L
        return screenOff.asSequence()
            .map { span ->
                val start = maxOf(span.start.value, from)
                val end = minOf(span.end?.value ?: limit, limit)
                start to end
            }
            .filter { (start, end) -> end > start && end - start >= minMs }
            .map { (start, _) -> start }
            .minOrNull()
    }
}
