package com.kleos.sakshi.engine.suggestions

import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.tuning.Tuning

enum class SilenceReason { NOT_ENOUGH_DATA, AFTER_LAPSE, EXPERIMENT_LIVE, TOO_EARLY, DISMISSED, RECENT, QUOTA }

data class SelectionResult(val task: Candidate?, val observation: Candidate?, val reason: SilenceReason?)

// DOC 3 names these week numbers in prose ("eligible from week 2" / "from week 4") but Tuning.kt
// has no numeric constant for either -- SUGGEST_EARLY_KINDS/SUGGEST_LATER_KINDS are just the kind lists.
private const val EARLY_ELIGIBLE_WEEK = 2
private const val LATER_ELIGIBLE_WEEK = 4

/**
 * select(ctx, candidates): candidates are the already-evaluated Candidate? results from every
 * rule in SuggestionRegistry (null ones dropped) -- kept as an explicit parameter so the silence
 * rules and ranking can be tested against synthetic candidates, independent of whether any one
 * rule's own evaluate() is correct.
 */
object SuggestionSelector {
    private val observationPriority = listOf(SuggestionKind.S11, SuggestionKind.S12, SuggestionKind.S10)
    private val earlyKinds = Tuning.SUGGEST_EARLY_KINDS.split(",").map { SuggestionKind.valueOf(it) }.toSet()
    private val laterKinds = Tuning.SUGGEST_LATER_KINDS.split(",").map { SuggestionKind.valueOf(it) }.toSet()

    fun select(ctx: SuggestionContext, candidates: List<Candidate>): SelectionResult {
        val observation = observationPriority.firstNotNullOfOrNull { kind -> candidates.firstOrNull { it.kind == kind } }

        val validDays = ctx.days.count { it.valid }
        if (validDays < Tuning.SUGGEST_MIN_VALID_DAYS || ctx.windows.size < Tuning.SUGGEST_MIN_WINDOWS) {
            return SelectionResult(null, observation, SilenceReason.NOT_ENOUGH_DATA)
        }
        if (ctx.isFirstMirrorAfterLapse) {
            return SelectionResult(null, observation, SilenceReason.AFTER_LAPSE)
        }
        if (ctx.experiments.any { it.verdict == Verdict.PENDING }) {
            return SelectionResult(null, observation, SilenceReason.EXPERIMENT_LIVE)
        }
        if (quotaBlocked(ctx)) {
            return SelectionResult(null, observation, SilenceReason.QUOTA)
        }

        val taskCandidates = candidates.filter { it.kind in earlyKinds || it.kind in laterKinds }
        val checked = taskCandidates.map { c -> c to firstFailingReason(c, ctx) }
        val survivors = checked.filter { it.second == null }.map { it.first }

        if (survivors.isEmpty()) {
            return SelectionResult(null, observation, checked.firstOrNull()?.second)
        }

        val best = survivors.sortedWith(
            compareByDescending<Candidate> { adjustedImpact(it, ctx) }
                .thenByDescending { !everShown(it, ctx) }
                .thenBy { it.kind.ordinal },
        ).first()

        return SelectionResult(best, observation, null)
    }

    private fun firstFailingReason(c: Candidate, ctx: SuggestionContext): SilenceReason? = when {
        tooEarly(c, ctx) -> SilenceReason.TOO_EARLY
        isDismissed(c, ctx) -> SilenceReason.DISMISSED
        isRecent(c, ctx) -> SilenceReason.RECENT
        else -> null
    }

    private fun tooEarly(c: Candidate, ctx: SuggestionContext): Boolean = when {
        c.kind in earlyKinds -> ctx.currentWeekNumber < EARLY_ELIGIBLE_WEEK
        c.kind in laterKinds -> ctx.currentWeekNumber < LATER_ELIGIBLE_WEEK
        else -> false
    }

    private fun isDismissed(c: Candidate, ctx: SuggestionContext): Boolean {
        val state = ctx.suggestionStates.firstOrNull { it.kind == c.kind && it.subject == c.subject } ?: return false
        val until = state.dismissedUntil ?: return false
        return until.value > ctx.asOf.value
    }

    private fun isRecent(c: Candidate, ctx: SuggestionContext): Boolean {
        val state = ctx.suggestionStates.firstOrNull { it.kind == c.kind && it.subject == c.subject } ?: return false
        // JudgeExperiments sets retiredUntil: 8 weeks after NO_CHANGE, 4 weeks after TOO_LITTLE or UNCLEAR (T2.13). Until then the
        // kind stays quiet. (T2.12 retired a NO_CHANGE kind for good, which DOC 3 does not say.)
        if (state.retiredUntil?.let { it.value > ctx.asOf.value } == true) return true
        val shownWeek = state.shownInWeek ?: return false
        val currentWeekStart = ctx.week?.weekStart ?: return false
        val weeksSince = (currentWeekStart.studyDay.epochDay - shownWeek.studyDay.epochDay) / 7
        // RECENT's own 4-week lockout happens to share DOC 3's dismissal duration; no separate
        // Tuning name exists for it, so SUGGEST_DISMISS_WEEKS is reused rather than hardcoded.
        return weeksSince < Tuning.SUGGEST_DISMISS_WEEKS
    }

    private fun quotaBlocked(ctx: SuggestionContext): Boolean {
        val currentWeekStart = ctx.week?.weekStart ?: return false
        val lastShownWeek = ctx.suggestionStates.mapNotNull { it.shownInWeek }.maxByOrNull { it.studyDay.epochDay } ?: return false
        val weeksSince = (currentWeekStart.studyDay.epochDay - lastShownWeek.studyDay.epochDay) / 7
        return if (ctx.currentWeekNumber >= Tuning.SUGGEST_SLOWDOWN_WEEK) {
            weeksSince < Tuning.SUGGEST_SLOWDOWN_GAP_WEEKS
        } else {
            weeksSince < 1
        }
    }

    private fun everShown(c: Candidate, ctx: SuggestionContext): Boolean =
        ctx.suggestionStates.any { it.kind == c.kind && it.subject == c.subject && it.lastShownAt != null }

    private fun adjustedImpact(c: Candidate, ctx: SuggestionContext): Double {
        if (ctx.currentWeekNumber < Tuning.SUGGEST_SLOWDOWN_WEEK) return c.impactShare
        val verdict = ctx.experiments.filter { it.kind == c.kind && it.subject == c.subject }
            .maxByOrNull { it.startedAt.value }?.verdict
        return when (verdict) {
            Verdict.MOVED -> c.impactShare * 1.25
            Verdict.NO_CHANGE -> c.impactShare * 0.5
            else -> c.impactShare
        }
    }
}
