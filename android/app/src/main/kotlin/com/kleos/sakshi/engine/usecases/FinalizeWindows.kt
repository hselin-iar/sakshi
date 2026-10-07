package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.ZoneId

/**
 * F1: "a window ends >= 15 min before asOf -> finalised=true". WindowFinder
 * already applies this fresh every time a day is recomputed, so a day just
 * touched by RecomputeDay never needs this. The gap this closes is a day
 * NOT touched by new events, whose stored windows were finalised=false the
 * last time it was derived but should flip now that time has passed —
 * bounded to the `lookback` days the caller chooses to re-check.
 */
object FinalizeWindows {
    fun staleDays(ports: Ports, asOf: EpochMs, lookback: List<StudyDay>, zone: ZoneId): List<StudyDay> =
        lookback.filter { day ->
            val windows = ports.derived.windows(day.startEpochMs(zone), day.endEpochMs(zone))
            windows.any { detail ->
                !detail.window.finalised && detail.window.end.value + Tuning.WINDOW_FINALISE_LAG_MIN * 60_000L <= asOf.value
            }
        }
}
