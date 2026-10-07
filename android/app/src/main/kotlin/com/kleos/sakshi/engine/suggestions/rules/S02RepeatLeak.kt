package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/** S2: one OFF_SET package is first_pkg of >= S2_SHARE of stays over >= S2_MIN_WINDOWS windows. */
object S02RepeatLeak : SuggestionRule {
    override val kind = SuggestionKind.S2

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        if (ctx.windows.size < Tuning.S2_MIN_WINDOWS) return null
        val stays = ctx.windows.flatMap { it.stays }
        if (stays.isEmpty()) return null

        val (pkg, pkgStays) = stays.groupBy { it.firstPkg }.maxByOrNull { it.value.size } ?: return null
        val share = pkgStays.size.toDouble() / stays.size
        if (share < Tuning.S2_SHARE) return null

        return Candidate(
            kind = SuggestionKind.S2, subject = pkg, impactShare = share,
            target = TargetMetric.STAYS_PER_HOUR_FROM_PKG,
            args = mapOf("count" to pkgStays.size.toString(), "of" to stays.size.toString()), action = ActionType.MOVE_ICON,
        )
    }
}
