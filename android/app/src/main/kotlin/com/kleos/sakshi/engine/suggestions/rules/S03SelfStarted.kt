package com.kleos.sakshi.engine.suggestions.rules

import com.kleos.sakshi.engine.model.ActionType
import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.suggestions.SuggestionContext
import com.kleos.sakshi.engine.suggestions.SuggestionRule
import com.kleos.sakshi.engine.tuning.Tuning

// DOC 3 names "needs >= 10 known-origin stays" but Tuning.kt has no constant for it.
private const val S3_MIN_KNOWN = 10

/** S3: >= S3_SHARE of known-origin stays are SELF_STARTED. Needs notification access + coverage (origin needs StoneWave). */
object S03SelfStarted : SuggestionRule {
    override val kind = SuggestionKind.S3

    override fun evaluate(ctx: SuggestionContext): Candidate? {
        if (!ctx.notificationAccessGranted || ctx.listenerCoverageFraction14d < 0.50) return null

        val known = ctx.windows.flatMap { it.stays }.filter { it.origin == Origin.STONE || it.origin == Origin.SELF_STARTED }
        if (known.size < S3_MIN_KNOWN) return null
        val share = known.count { it.origin == Origin.SELF_STARTED }.toDouble() / known.size
        if (share < Tuning.S3_SHARE) return null

        return Candidate(
            kind = SuggestionKind.S3, subject = null, impactShare = share,
            target = TargetMetric.SELF_STARTED_PER_HOUR,
            args = mapOf("count" to known.count { it.origin == Origin.SELF_STARTED }.toString(), "of" to known.size.toString()), action = ActionType.PHONE_FACE_DOWN,
        )
    }
}
