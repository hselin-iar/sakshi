package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.judging.ExperimentJudge
import com.kleos.sakshi.engine.judging.JudgeContext
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionState
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/**
 * F12: finds experiments whose 14 days are over and writes their verdicts, then applies the verdict's effect on the kind.
 * Returns the experiments judged by this run. An experiment that already has a verdict is never looked at again.
 */
object JudgeExperiments {
    private const val DAY_MS = 86_400_000L
    private const val WEEK_MS = 7 * DAY_MS

    // DOC 3 says NO_CHANGE retires the kind "for this user 8 weeks" but Tuning.kt has no name for it; flagged local constant.
    private const val NO_CHANGE_RETIRE_WEEKS = 8L

    fun run(ports: Ports, asOf: EpochMs, zone: ZoneId): List<Experiment> {
        val due = ports.state.experiments().filter { it.verdict == Verdict.PENDING && it.windowEnd.value <= asOf.value }
        if (due.isEmpty()) return emptyList()

        val baseline = ports.state.baseline()
        val weeks = ports.derived.weeks()
        val installed = ports.catalog.launcherApps().map { it.pkg }.toSet()
        val periodMs = Tuning.JUDGE_DAYS * DAY_MS

        return due.mapNotNull { exp ->
            val from = EpochMs(exp.startedAt.value - periodMs)
            val to = EpochMs(exp.startedAt.value + periodMs)
            val ctx = JudgeContext(
                windows = ports.derived.windows(from, to),
                days = ports.derived.days(StudyDay.of(from, zone), StudyDay.of(to, zone)),
                weeks = weeks, baseline = baseline,
                subjectInstalled = exp.subject == null || exp.subject in installed,
                asOf = asOf, zone = zone,
            )
            val out = ExperimentJudge.outcome(exp, ctx)
            if (out.verdict == Verdict.PENDING) return@mapNotNull null

            val judged = exp.copy(verdict = out.verdict, beforeValue = out.beforeValue, afterValue = out.afterValue, approxMix = out.approxMix)
            ports.state.saveExperiment(judged)
            applyEffect(ports, judged, asOf)
            judged
        }
    }

    // MOVED needs no state here: the selector reads the verdict from the experiments (weight 1.25 from week 8).
    // NO_CHANGE retires the kind for NO_CHANGE_RETIRE_WEEKS; TOO_LITTLE and UNCLEAR close with no claim and free the kind after four weeks.
    private fun applyEffect(ports: Ports, exp: Experiment, asOf: EpochMs) {
        val weeks = when (exp.verdict) {
            Verdict.NO_CHANGE -> NO_CHANGE_RETIRE_WEEKS
            Verdict.TOO_LITTLE, Verdict.UNCLEAR -> Tuning.SUGGEST_DISMISS_WEEKS.toLong()
            else -> return
        }
        val existing = ports.state.suggestionStates().firstOrNull { it.kind == exp.kind && it.subject == exp.subject }
        val state = existing ?: SuggestionState(exp.kind, exp.subject, null, null, null, null, null, exp.verdict.name)
        ports.state.saveSuggestionState(state.copy(retiredUntil = EpochMs(asOf.value + weeks * WEEK_MS), status = exp.verdict.name))
    }
}
