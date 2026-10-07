package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/**
 * S7: >= S7_COVERAGE of window time in apps neither in-set nor neutral (OFF_SET), over >=
 * S7_MIN_WINDOWS windows. Approximated via stay duration grouped by pkgMain -- a stay is the
 * engine's own measure of off-set departure time.
 */
object S07WorkSetLeak : SuggestionRule {
    override val kind = SuggestionKind.S7

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        if (ctx.windows.size < Tuning.S7_MIN_WINDOWS) return null
        val totalWindowMs = ctx.windows.sumOf { it.window.end.value - it.window.start.value }
        if (totalWindowMs <= 0L) return null

        val pkgTimeMs = ctx.windows.flatMap { it.stays }.groupBy { it.pkgMain }
            .mapValues { (_, stays) -> stays.sumOf { it.end.value - it.start.value } }
        val (topPkg, topTimeMs) = pkgTimeMs.entries.maxByOrNull { it.value } ?: return null

        val coverage = topTimeMs.toDouble() / totalWindowMs
        if (coverage < Tuning.S7_COVERAGE) return null

        return Candidate(
            kind = SuggestionKind.S7, subject = topPkg, impactShare = coverage,
            target = TargetMetric.WORKSET_COVERAGE, args = emptyMap(), action = ActionType.ADD_TO_WORK_SET,
        )
    }
}
