package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/** S4: >= S4_SHARE of known-origin stays are STONE and one package >= 50% of the stones. */
object S04PingDriven : SuggestionRule {
    override val kind = SuggestionKind.S4

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        if (!ctx.notificationAccessGranted || ctx.listenerCoverageFraction14d < 0.50) return null

        val known = ctx.windows.flatMap { it.stays }.filter { it.origin == Origin.STONE || it.origin == Origin.SELF_STARTED }
        if (known.isEmpty()) return null
        val stoneStays = known.filter { it.origin == Origin.STONE }
        val stoneShare = stoneStays.size.toDouble() / known.size
        if (stoneShare < Tuning.S4_SHARE) return null

        val topPkgEntry = stoneStays.groupBy { it.stonePkg }.maxByOrNull { it.value.size } ?: return null
        val topPkg = topPkgEntry.key ?: return null
        val topPkgShare = topPkgEntry.value.size.toDouble() / stoneStays.size
        if (topPkgShare < 0.50) return null

        return Candidate(
            kind = SuggestionKind.S4, subject = topPkg, impactShare = stoneShare,
            target = TargetMetric.PINGS_FROM_PKG_IN_WINDOWS,
            args = mapOf("count" to topPkgEntry.value.size.toString(), "of" to stoneStays.size.toString()), action = ActionType.OPEN_NOTIFICATION_SETTINGS,
        )
    }
}
