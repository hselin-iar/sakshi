package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.Instant
import java.time.ZoneId

/** S1: Pattern P1's clear slot. impactShare = the slot's share of all stretch minutes. */
object S01ClearHour : SuggestionRule {
    override val kind = SuggestionKind.S1

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        val clear = ctx.clearHour ?: return null
        val slotWindows = ctx.windows.filter { slotStartOf(it, ctx.zone) == clear.startHour }
        if (slotWindows.size < Tuning.S1_MIN_WINDOWS) return null

        val totalStretch = ctx.windows.sumOf { wd -> wd.stretches.sumOf { it.minutes } }
        if (totalStretch <= 0.0) return null
        val slotStretch = slotWindows.sumOf { wd -> wd.stretches.sumOf { it.minutes } }
        val impactShare = slotStretch / totalStretch

        return Candidate(
            kind = SuggestionKind.S1, subject = null, impactShare = impactShare,
            target = TargetMetric.STRETCH_IN_SLOT,
            args = mapOf(
                "startHour" to clear.startHour.toString(), "endHour" to clear.endHour.toString(),
                "stretchMin" to clear.stretchMin.toString(),
            ),
            action = ActionType.FIRST_THING_HARDEST,
        )
    }

    private fun slotStartOf(wd: WindowWithDetail, zone: ZoneId): Int {
        val hour = Instant.ofEpochMilli(wd.window.start.value).atZone(zone).hour
        return (hour / 2) * 2
    }
}
