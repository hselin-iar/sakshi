package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.model.Candidate
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.StartReason
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning

/** What a tap on "Try this" did. STALE is the typed result for a screen that is no longer current (F11 edge case). */
enum class TapOutcome { STARTED, ALREADY_RUNNING, STALE }

/**
 * F11: "Try this" starts one experiment, if none is live. `currentTask` is the task the engine would offer right now; a tap
 * for anything else was made on a stale screen and creates nothing.
 */
object TapTryThis {
    private const val DAY_MS = 86_400_000L

    fun run(ports: Ports, kind: SuggestionKind, subject: Pkg?, currentTask: Candidate?, asOf: EpochMs): TapOutcome {
        val existing = ports.state.experiments()
        if (existing.any { it.verdict == Verdict.PENDING }) return TapOutcome.ALREADY_RUNNING

        val target = currentTask?.takeIf { it.kind == kind && it.subject == subject }?.target ?: return TapOutcome.STALE

        // ids are assigned here so a later experiment can never replace an earlier one, whatever the store does with 0
        ports.state.saveExperiment(
            Experiment(
                id = (existing.maxOfOrNull { it.id } ?: 0L) + 1, kind = kind, subject = subject, startedAt = asOf,
                startReason = StartReason.TAP, target = target, beforeValue = null, afterValue = null,
                windowEnd = EpochMs(asOf.value + Tuning.JUDGE_DAYS * DAY_MS), verdict = Verdict.PENDING, approxMix = false, shown = false,
            ),
        )
        return TapOutcome.STARTED
    }
}
