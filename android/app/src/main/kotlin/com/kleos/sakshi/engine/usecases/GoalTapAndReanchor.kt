package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.baseline.BaselineService
import com.kleos.sakshi.engine.baseline.ValidDayData
import com.kleos.sakshi.engine.metrics.Weeks
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GoalAnswer
import com.kleos.sakshi.engine.model.GoalTap
import com.kleos.sakshi.engine.model.ReanchorResult
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/** The one-tap "did this week match what you hoped?" answer (F6 step 5). */
object GoalTapUseCase {
    /** The answer belongs to the week the Mirror was showing: the latest completed week with enough valid days, else the current one. */
    fun run(ports: Ports, answer: GoalAnswer, week: WeekStart) = ports.state.saveGoalTap(GoalTap(week, answer))
}

/** F5: a new baseline from the 8 most recent valid days, once, from week 4. The old row is kept, inactive. */
object Reanchor {
    const val REASON_NO_BASELINE = "NO_BASELINE"
    const val REASON_TOO_EARLY = "TOO_EARLY"
    const val REASON_ALREADY_USED = "ALREADY_USED"
    const val REASON_TOO_LITTLE = "TOO_LITTLE_DATA"

    fun run(ports: Ports, asOf: EpochMs, zone: ZoneId): ReanchorResult {
        val current = ports.state.baseline()?.takeIf { it.isActive } ?: return ReanchorResult(false, REASON_NO_BASELINE)
        if (current.id != UpdateBaseline.FIRST_BASELINE_ID) return ReanchorResult(false, REASON_ALREADY_USED)
        val weeksSince = Weeks.between(Weeks.of(current.frozenAt, zone), Weeks.of(asOf, zone)) + 1
        if (weeksSince < Tuning.REANCHOR_MIN_WEEK) return ReanchorResult(false, REASON_TOO_EARLY)

        val today = StudyDay.of(asOf, zone)
        val valid = ports.derived.days(StudyDay(0), today).filter { it.valid }.sortedBy { it.day.epochDay }.takeLast(Tuning.BASELINE_VALID_DAYS)
        val data = valid.map { summary ->
            val windows = ports.derived.windows(summary.day.startEpochMs(zone), summary.day.endEpochMs(zone)).filter { !it.window.partial }
            ValidDayData(summary.day, summary.day.endEpochMs(zone), windows.map { it.window }, windows.flatMap { it.stretches }, windows.flatMap { it.stays })
        }
        val fresh = BaselineService.computeBaseline(data) ?: return ReanchorResult(false, REASON_TOO_LITTLE)

        ports.state.saveBaseline(current.copy(isActive = false))
        ports.state.saveBaseline(fresh.copy(id = UpdateBaseline.REANCHORED_BASELINE_ID, frozenAt = asOf))
        return ReanchorResult(true, null)
    }
}
