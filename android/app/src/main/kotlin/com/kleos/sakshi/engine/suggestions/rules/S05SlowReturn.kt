package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

/** S5: median return > S5_RATIO x baseline for S5_WEEKS consecutive weeks, OR >= S5_MIN_MIN absolute. */
object S05SlowReturn : SuggestionRule {
    override val kind = SuggestionKind.S5

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        val r0 = ctx.baseline?.r0 ?: return null
        val currentMedian = ctx.week?.returnMedianMin ?: return null

        val recentWeeks = (ctx.priorWeeks.takeLast(Tuning.S5_WEEKS - 1) + ctx.week).takeLast(Tuning.S5_WEEKS)
        val ratioCondition = recentWeeks.size == Tuning.S5_WEEKS &&
            recentWeeks.all { (it.returnMedianMin ?: 0.0) > Tuning.S5_RATIO * r0 }
        val absoluteCondition = currentMedian >= Tuning.S5_MIN_MIN
        if (!ratioCondition && !absoluteCondition) return null

        val stays = ctx.windows.flatMap { it.stays }
        val withReturn = stays.mapNotNull { it.returnMinutes }
        if (withReturn.isEmpty()) return null
        val exceededShare = withReturn.count { it > Tuning.S5_RATIO * r0 }.toDouble() / withReturn.size

        return Candidate(
            kind = SuggestionKind.S5, subject = null, impactShare = exceededShare,
            target = TargetMetric.MEDIAN_RETURN,
            args = mapOf("count" to withReturn.count { it > Tuning.S5_RATIO * r0 }.toString(), "of" to withReturn.size.toString()), action = ActionType.LEAVE_PAGE_OPEN,
        )
    }
}
