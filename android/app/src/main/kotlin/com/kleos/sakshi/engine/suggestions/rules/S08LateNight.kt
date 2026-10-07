package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule

/** S8: the CrossDay pattern, if present. impactShare = lateNights / 14. */
object S08LateNight : SuggestionRule {
    override val kind = SuggestionKind.S8

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        val pattern = ctx.patterns.firstOrNull { it.kind == PatternKind.CROSS_DAY } ?: return null
        val lateNights = pattern.args["lateNights"]?.toIntOrNull() ?: return null

        return Candidate(
            kind = SuggestionKind.S8, subject = null, impactShare = lateNights / 14.0,
            target = TargetMetric.NEXT_DAY_FIRST_STRETCH, args = pattern.args, action = ActionType.SCREEN_OFF_BY_0030,
        )
    }
}
