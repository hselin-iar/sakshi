package com.kleos.sakshi.data

import com.kleos.sakshi.data.entities.toEntity
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.ports.EventStore

/** Ranges are half-open: `from` is included, `to` is not. Duplicates of (ts, type, pkg) are dropped silently. */
class RoomEventStore(private val db: SakshiDatabase) : EventStore {
    private val dao get() = db.rawEvents()

    override fun append(events: List<RawEvent>) = dao.insertAll(events.map { it.toEntity() })
    override fun range(from: EpochMs, to: EpochMs): List<RawEvent> = dao.range(from.value, to.value).map { it.toModel() }
    override fun oldest(): EpochMs? = dao.oldest()?.let(::EpochMs)
    override fun count(): Int = dao.count()
    override fun purgeBefore(ts: EpochMs): Int = dao.purgeBefore(ts.value)
}
