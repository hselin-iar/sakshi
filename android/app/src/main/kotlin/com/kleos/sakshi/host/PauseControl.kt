package com.kleos.sakshi.host

import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.ports.GapStore
import com.kleos.sakshi.engine.ports.StateStore

/**
 * Pause collection (F10). Plain Kotlin over ports. Pausing writes exactly one open PAUSED gap; resuming closes it and moves the
 * cursor to now, so the paused time is never back-filled from Android's log and never counted as quiet.
 * [REFACTOR CANDIDATE: DOC 3 puts this in the engine as the PauseCollection use case (engine.pause). That is still the canned
 *  stub, so the host does it for now; switch HostApiImpl.pause to the façade when T2 delivers it.]
 */
class PauseControl(private val state: StateStore, private val gaps: GapStore) {
    fun set(on: Boolean, at: EpochMs) {
        val ingest = state.ingest()
        if (on && !ingest.paused) {
            gaps.add(DataGap(GapKind.PAUSED, at, null))
            state.saveIngest(ingest.copy(paused = true, pausedSince = at))
        } else if (!on && ingest.paused) {
            gaps.closeOpenPause(at)
            state.saveIngest(ingest.copy(paused = false, pausedSince = null, cursor = at))
        }
    }
}
