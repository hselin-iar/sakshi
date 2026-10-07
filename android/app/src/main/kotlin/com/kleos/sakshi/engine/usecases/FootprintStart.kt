package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.judging.TargetMetrics
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.StartReason
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.SuggestionKind
import com.kleos.sakshi.engine.model.TargetMetric
import com.kleos.sakshi.engine.model.Verdict
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/**
 * F11: the user may have acted without tapping. For S2 and S4, once the suggestion has been shown and no experiment is live, if
 * the last 7 days' rate for the subject is at most FOOTPRINT_DROP of the 7 days before (3 or more windows in each), an experiment
 * starts by itself, dated from the start of the last 7 days.
 */
object FootprintStart {
    private const val DAY_MS = 86_400_000L

    // DOC 3 says "7 days" and "(>= 3 windows in each)" in prose; Tuning.kt has no names for either. Flagged local constants.
    private const val FOOTPRINT_DAYS = 7L
    private const val FOOTPRINT_MIN_WINDOWS = 3

    private val targets = mapOf(
        SuggestionKind.S2 to TargetMetric.STAYS_PER_HOUR_FROM_PKG,
        SuggestionKind.S4 to TargetMetric.PINGS_FROM_PKG_IN_WINDOWS,
    )

    fun run(ports: Ports, asOf: EpochMs, zone: ZoneId): Experiment? {
        val existing = ports.state.experiments()
        if (existing.any { it.verdict == Verdict.PENDING }) return null

        val candidates = ports.state.suggestionStates()
            .filter { it.kind in targets && it.subject != null && it.lastShownAt != null }
            .sortedBy { it.kind.ordinal }
        if (candidates.isEmpty()) return null

        val lastFrom = asOf.value - FOOTPRINT_DAYS * DAY_MS
        val priorFrom = lastFrom - FOOTPRINT_DAYS * DAY_MS
        val validDays = ports.derived.days(StudyDay.of(EpochMs(priorFrom), zone), StudyDay.of(asOf, zone)).filter { it.valid }.map { it.day }.toSet()
        val usable = ports.derived.windows(EpochMs(priorFrom), asOf).filter { !it.window.partial && it.window.day in validDays }
        val last = usable.filter { it.window.start.value >= lastFrom }
        val prior = usable.filter { it.window.start.value < lastFrom }
        if (last.size < FOOTPRINT_MIN_WINDOWS || prior.size < FOOTPRINT_MIN_WINDOWS) return null

        for (state in candidates) {
            val target = targets.getValue(state.kind)
            val rateLast = TargetMetrics.value(target, state.subject, last) ?: continue
            val ratePrior = TargetMetrics.value(target, state.subject, prior) ?: continue
            if (ratePrior <= 0.0 || rateLast > Tuning.FOOTPRINT_DROP * ratePrior) continue

            val experiment = Experiment(
                id = (existing.maxOfOrNull { it.id } ?: 0L) + 1, kind = state.kind, subject = state.subject,
                startedAt = EpochMs(lastFrom), startReason = StartReason.FOOTPRINT, target = target,
                beforeValue = null, afterValue = null, windowEnd = EpochMs(lastFrom + Tuning.JUDGE_DAYS * DAY_MS),
                verdict = Verdict.PENDING, approxMix = false, shown = false,
            )
            ports.state.saveExperiment(experiment)
            return experiment
        }
        return null
    }
}
