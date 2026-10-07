package com.kleos.sakshi.engine.judging

import com.kleos.sakshi.engine.metrics.WeekMetrics
import com.kleos.sakshi.engine.metrics.steadiness
import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/**
 * Everything the judge may look at. `windows` and `days` are whatever is stored around the experiment (any status); the
 * judge itself keeps only non-partial windows on valid days. Not in Entities.kt, for the same reason as PatternContext and
 * SuggestionContext: it sits next to the thing it serves.
 */
data class JudgeContext(
    val windows: List<WindowWithDetail>,
    val days: List<DaySummary>,
    val weeks: List<WeekSummary>,
    val baseline: Baseline?,
    /** False when the experiment's subject app is gone from the phone. Ignored when the experiment has no subject. */
    val subjectInstalled: Boolean,
    val asOf: EpochMs,
    val zone: ZoneId,
)

/** The verdict plus the numbers it rests on. Only MOVED and NO_CHANGE carry numbers: every other verdict makes no claim. */
data class JudgeOutcome(
    val verdict: Verdict,
    val beforeValue: Double? = null,
    val afterValue: Double? = null,
    val approxMix: Boolean = false,
)

/**
 * F12: did the thing the user tried move the number it targeted? Precedence: PENDING, UNCLEAR, TOO_LITTLE, then
 * MOVED or NO_CHANGE. A verdict, once written on the experiment, is returned as it is: it never changes.
 * The wording ("correlation, not cause") belongs to SentenceBuilder, not here.
 */
object ExperimentJudge {
    private const val DAY_MS = 86_400_000L

    fun judge(exp: Experiment, ctx: JudgeContext): Verdict = outcome(exp, ctx).verdict

    fun outcome(exp: Experiment, ctx: JudgeContext): JudgeOutcome {
        if (exp.verdict != Verdict.PENDING) return JudgeOutcome(exp.verdict, exp.beforeValue, exp.afterValue, exp.approxMix)

        val periodMs = Tuning.JUDGE_DAYS * DAY_MS
        val afterFrom = exp.startedAt.value
        val afterTo = afterFrom + periodMs
        val beforeFrom = afterFrom - periodMs

        // 1. still inside its 14 days
        if (ctx.asOf.value < afterTo) return JudgeOutcome(Verdict.PENDING)

        // 3. the subject app is gone, or an unusual week touches either period
        if (exp.subject != null && !ctx.subjectInstalled) return JudgeOutcome(Verdict.UNCLEAR)
        if (touchesUnusualWeek(ctx, beforeFrom, afterTo)) return JudgeOutcome(Verdict.UNCLEAR)

        // 2. the two periods: non-partial windows on valid days, by the time they started
        val validDays = ctx.days.filter { it.valid }.map { it.day }.toSet()
        val usable = ctx.windows.filter { !it.window.partial && it.window.day in validDays }
        val after = usable.filter { it.window.start.value in afterFrom until afterTo }
        val before = usable.filter { it.window.start.value in beforeFrom until afterFrom }

        // 4. too few windows after
        if (after.size < Tuning.JUDGE_MIN_WINDOWS) return JudgeOutcome(Verdict.TOO_LITTLE)

        // 5. before re-weighted to the after period's weekday/weekend mix; a missing metric on either side is TOO_LITTLE
        val measure = { ws: List<WindowWithDetail> ->
            val wsDays = ws.map { it.window.day }.toSet()
            TargetMetrics.value(exp.target, exp.subject, ws, ctx.days.filter { it.valid && it.day in wsDays })
        }
        val mix = WeekdayMix.reweight(before, after, measure)
        val b = mix.beforeValue ?: return JudgeOutcome(Verdict.TOO_LITTLE)
        val a = mix.afterValue ?: return JudgeOutcome(Verdict.TOO_LITTLE)

        // 6. improvement. Nothing to improve from zero is no change (and never a division by zero).
        val improvement = when {
            b <= 0.0 -> 0.0
            TargetMetrics.higherIsBetter(exp.target) -> (a - b) / b
            else -> (b - a) / b
        }

        // 7 and 8. a gain bought with a drop in Steadiness is not a gain
        val moved = improvement >= Tuning.JUDGE_MOVED && !steadinessFell(ctx.baseline, before, after)
        return JudgeOutcome(if (moved) Verdict.MOVED else Verdict.NO_CHANGE, b, a, mix.approxMix)
    }

    private fun touchesUnusualWeek(ctx: JudgeContext, from: Long, to: Long): Boolean = ctx.weeks.any { w ->
        val weekStart = w.weekStart.studyDay.startEpochMs(ctx.zone).value
        w.unusual && weekStart < to && weekStart + 7 * DAY_MS > from
    }

    // Both pooled against the frozen baseline. No baseline, or a part that cannot be computed, cannot say Steadiness fell.
    private fun steadinessFell(baseline: Baseline?, before: List<WindowWithDetail>, after: List<WindowWithDetail>): Boolean {
        if (baseline == null) return false
        fun pooled(ws: List<WindowWithDetail>) =
            steadiness(WeekMetrics.pool(ws.flatMap { it.stretches }, ws.flatMap { it.stays }, ws.map { it.window }), baseline)
        val sb = pooled(before) ?: return false
        val sa = pooled(after) ?: return false
        return sa.value < sb.value
    }
}
