package com.kleos.sakshi.data

import com.kleos.sakshi.data.entities.toEntity
import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.ports.GapStore

/** `overlapping` uses half-open ranges; a gap with no end is a pause that is still open. */
class RoomGapStore(private val db: SakshiDatabase) : GapStore {
    private val dao get() = db.gaps()

    override fun add(gap: DataGap) = dao.insert(gap.toEntity())
    override fun closeOpenPause(at: EpochMs) {
        dao.closeOpenPause(at.value)
    }
    override fun overlapping(from: EpochMs, to: EpochMs): List<DataGap> = dao.overlapping(from.value, to.value).map { it.toModel() }
}
