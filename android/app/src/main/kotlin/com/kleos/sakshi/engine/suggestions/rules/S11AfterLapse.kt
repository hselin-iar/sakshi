package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/** S11: an exempt observation -- a lapse of >= LAPSE_MIN_DAYS days (F15). Highest observation priority. */
object S11AfterLapse : SuggestionRule {
    override val kind = SuggestionKind.S11

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        val lapse = ctx.lapse ?: return null
        if (lapse.days < Tuning.LAPSE_MIN_DAYS) return null

        return Candidate(
            kind = SuggestionKind.S11, subject = null, impactShare = 0.0, target = null,
            args = mapOf("days" to lapse.days.toString()), action = ActionType.NONE,
        )
    }
}
