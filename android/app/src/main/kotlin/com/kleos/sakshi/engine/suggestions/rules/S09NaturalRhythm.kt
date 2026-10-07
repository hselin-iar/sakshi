package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/** S9: the BreakPoint pattern, with topBandShare >= S9_SHARE, band >= S9_BAND_MIN, week >= S9_MIN_WEEK. */
object S09NaturalRhythm : SuggestionRule {
    override val kind = SuggestionKind.S9

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        if (ctx.currentWeekNumber < Tuning.S9_MIN_WEEK) return null
        val pattern = ctx.patterns.firstOrNull { it.kind == PatternKind.BREAK_POINT } ?: return null
        val bandShare = pattern.args["bandShare"]?.toDoubleOrNull() ?: return null
        val bandStartMin = pattern.args["bandStartMin"]?.toIntOrNull() ?: return null
        if (bandShare < Tuning.S9_SHARE || bandStartMin < Tuning.S9_BAND_MIN) return null

        return Candidate(
            kind = SuggestionKind.S9, subject = null, impactShare = bandShare,
            target = TargetMetric.PUTDOWN_SHARE, args = pattern.args, action = ActionType.PLAN_SHORT_PUT_DOWN,
        )
    }
}
