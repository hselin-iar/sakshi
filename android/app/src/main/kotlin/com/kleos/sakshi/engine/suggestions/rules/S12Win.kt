package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.PatternKind
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/** S12: an exempt observation -- a part improved >= S12_IMPROVE two weeks running (a P2 run3 BETTER trend). */
object S12Win : SuggestionRule {
    override val kind = SuggestionKind.S12

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        val pattern = ctx.patterns.firstOrNull {
            it.kind == PatternKind.TREND && it.args["direction"] == "BETTER" &&
                it.args["run3"] == "true" && it.strength >= Tuning.S12_IMPROVE
        } ?: return null

        return Candidate(kind = SuggestionKind.S12, subject = null, impactShare = pattern.strength, target = null,
            args = pattern.args + mapOf("evidenceWindows" to pattern.evidenceWindows.toString(), "evidenceDays" to pattern.evidenceDays.toString()),
            action = ActionType.NONE)
    }
}
