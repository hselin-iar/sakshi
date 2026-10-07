package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

// DOC 3 names "(>= 10 windows)" but Tuning.kt has no constant for it.
private const val S6_MIN_WINDOWS = 10

/** S6: first stay inside the first FLINCH_FIRST_MIN minutes of >= S6_SHARE of windows. */
object S06EarlyFlinch : SuggestionRule {
    override val kind = SuggestionKind.S6

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        if (ctx.windows.size < S6_MIN_WINDOWS) return null

        val flinched = ctx.windows.count { wd ->
            val firstStay = wd.stays.minByOrNull { it.start.value }
            firstStay != null && (firstStay.start.value - wd.window.start.value) <= Tuning.FLINCH_FIRST_MIN * 60_000L
        }
        val share = flinched.toDouble() / ctx.windows.size
        if (share < Tuning.S6_SHARE) return null

        return Candidate(
            kind = SuggestionKind.S6, subject = null, impactShare = share,
            target = TargetMetric.FLINCH_RATE,
            args = mapOf("count" to flinched.toString(), "of" to ctx.windows.size.toString(), "minutes" to Tuning.FLINCH_FIRST_MIN.toString()), action = ActionType.PHONE_IN_OTHER_ROOM,
        )
    }
}
