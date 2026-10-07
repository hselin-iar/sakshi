package com.kleos.sakshi.engine.metrics

import com.kleos.sakshi.engine.model.EndedBy
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Stretch
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * F5/D1: a stretch runs from window start (or the end of the previous stay)
 * until the next stay start, the window end, or a put-down. Quiet time
 * joins the stretch it follows; a quiet span >= PUT_DOWN_MIN ends it as
 * PUT_DOWN, absorbing the quiet span itself. Glance and neutral-app time
 * (anything that isn't a confirmed Stay) count as holding, so a stretch's
 * minutes are exactly inSetMinutes + quietMinutes.
 *
 * A quiet span that runs through to the window's own end does not count as
 * a put-down break — there is nothing to contrast it with "coming back" to,
 * so that stretch simply ends at WINDOW_END.
 */
object StretchBuilder {
    fun build(window: Window, stays: List<Stay>, screenOff: List<ScreenSpan>): List<Stretch> {
        val windowStart = window.start.value
        val windowEnd = window.end.value
        val putDownMs = Tuning.PUT_DOWN_MIN * 60_000L

        data class Break(val anchor: Long, val stretchEnd: Long, val nextCursor: Long, val endedBy: EndedBy)

        val putDownBreaks = screenOff.mapNotNull { span ->
            val start = maxOf(span.start.value, windowStart)
            val end = minOf(span.end?.value ?: windowEnd, windowEnd)
            if (end - start >= putDownMs && end < windowEnd) {
                Break(anchor = start, stretchEnd = end, nextCursor = end, endedBy = EndedBy.PUT_DOWN)
            } else {
                null
            }
        }
        val stayBreaks = stays.map { stay ->
            Break(anchor = stay.start.value, stretchEnd = stay.start.value, nextCursor = stay.end.value, endedBy = EndedBy.STAY)
        }

        val breaks = (putDownBreaks + stayBreaks).sortedBy { it.anchor }

        val stretches = mutableListOf<Stretch>()
        var cursor = windowStart
        for (b in breaks) {
            if (b.stretchEnd > cursor) {
                stretches += buildStretch(window.id, cursor, b.stretchEnd, b.endedBy, screenOff, windowEnd)
            }
            cursor = b.nextCursor
        }
        if (cursor < windowEnd) {
            stretches += buildStretch(window.id, cursor, windowEnd, EndedBy.WINDOW_END, screenOff, windowEnd)
        }
        return stretches
    }

    private fun buildStretch(
        windowId: Long,
        start: Long,
        end: Long,
        endedBy: EndedBy,
        screenOff: List<ScreenSpan>,
        windowEnd: Long,
    ): Stretch {
        val quietMs = screenOff.sumOf { span ->
            val s = maxOf(span.start.value, start)
            val e = minOf(span.end?.value ?: windowEnd, end)
            maxOf(0L, e - s)
        }
        val totalMs = end - start
        return Stretch(
            id = 0L,
            windowId = windowId,
            start = EpochMs(start),
            end = EpochMs(end),
            minutes = totalMs / 60_000.0,
            inSetMinutes = (totalMs - quietMs) / 60_000.0,
            quietMinutes = quietMs / 60_000.0,
            endedBy = endedBy,
        )
    }
}
