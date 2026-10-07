package com.kleos.sakshi.engine.mirror

import com.kleos.sakshi.engine.model.GoalTapView
import com.kleos.sakshi.engine.model.MirrorView
import com.kleos.sakshi.engine.model.PartLines
import com.kleos.sakshi.engine.model.PartsView

/**
 * F16: the only place the gentle restriction is defined. What stays: the return line (as the headline), the stay count in words,
 * the Saying, the lapse line and the honest data states. What goes: Steadiness, patterns, suggestions, observations, verdicts,
 * the goal tap, the teacher line, stones, the clear hour, the re-anchor offer and the study-block suggestion.
 *
 * Idempotent, and applied last by MirrorBuilder so no feature can bypass it. If it ever fails it falls back to the most
 * restrictive view (headline and return line only), never to the full one.
 */
object GentlePolicy {
    fun apply(view: MirrorView): MirrorView = try {
        restrict(view)
    } catch (_: Exception) {
        mostRestrictive(view)
    }

    private fun headlineOf(view: MirrorView): String = view.returnLine ?: SentenceBuilder.learningHeadline()

    private fun restrict(view: MirrorView): MirrorView = view.copy(
        gentle = true,
        headline = headlineOf(view),
        parts = view.parts?.let {
            PartsView(
                stretchMin = null, longestStretchMin = null, inSetShare = null, quietShare = null,
                staysPerHour = it.staysPerHour, glances = it.glances, returnMin = it.returnMin,
                lines = PartLines(stretch = null, stays = it.lines.stays, ret = it.lines.ret, quiet = null), extrasLines = emptyList(),
            )
        },
        steadiness = null, stones = null, clearHour = null, patterns = emptyList(), suggestion = null, observation = null,
        nothingToFix = false, verdict = null, goalTap = GoalTapView(offered = false, answer = null), teacher = null,
        reanchorOffered = false, suggestedStudyBlock = null,
    )

    private fun mostRestrictive(view: MirrorView): MirrorView = view.copy(
        gentle = true, headline = headlineOf(view), parts = null, steadiness = null, stones = null, clearHour = null,
        patterns = emptyList(), suggestion = null, observation = null, nothingToFix = false, verdict = null,
        goalTap = GoalTapView(offered = false, answer = null), teacher = null, saying = null, lapseLine = null,
        reanchorOffered = false, suggestedStudyBlock = null,
    )
}
