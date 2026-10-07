package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.ports.Ports

/**
 * F10: pausing writes exactly one open PAUSED gap; resuming closes it and moves the cursor to now, so the paused time is never
 * back-filled from Android's log and never counted as quiet. Repeating either call changes nothing.
 */
object PauseCollection {
    fun run(ports: Ports, on: Boolean, at: EpochMs) {
        val ingest = ports.state.ingest()
        if (on && !ingest.paused) {
            ports.gaps.add(DataGap(GapKind.PAUSED, at, null))
            ports.state.saveIngest(ingest.copy(paused = true, pausedSince = at))
        } else if (!on && ingest.paused) {
            ports.gaps.closeOpenPause(at)
            ports.state.saveIngest(ingest.copy(paused = false, pausedSince = null, cursor = at))
        }
    }
}
