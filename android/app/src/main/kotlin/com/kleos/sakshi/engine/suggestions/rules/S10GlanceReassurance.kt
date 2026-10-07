package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/** S10: an exempt observation (never a task) -- glances >= S10_MIN_GLANCES and glances/stays >= S10_GLANCE_PER_STAY. */
object S10GlanceReassurance : SuggestionRule {
    override val kind = SuggestionKind.S10

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        val glances = ctx.windows.sumOf { it.glances }
        val stays = ctx.windows.sumOf { it.stays.size }
        if (glances < Tuning.S10_MIN_GLANCES) return null
        if (stays == 0 || glances.toDouble() / stays < Tuning.S10_GLANCE_PER_STAY) return null

        return Candidate(kind = SuggestionKind.S10, subject = null, impactShare = 0.0, target = null,
            args = mapOf("glances" to glances.toString(), "stays" to stays.toString()), action = ActionType.NONE)
    }
}
