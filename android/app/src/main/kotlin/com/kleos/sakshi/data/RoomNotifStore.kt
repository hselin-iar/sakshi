package com.kleos.sakshi.data

import com.kleos.sakshi.data.entities.ListenerSessionEntity
import com.kleos.sakshi.data.entities.toEntity
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ListenerSession
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.ports.ListenerCoverage
import com.kleos.sakshi.engine.ports.NotifStore

/**
 * NotifStore and ListenerCoverage share one class because both describe what the listener heard.
 * Ranges are half-open. A session with no end is still connected, so it extends to the end of whatever interval is asked about.
 */
class RoomNotifStore(private val db: SakshiDatabase) : NotifStore, ListenerCoverage {
    private val notifs get() = db.notifEvents()
    private val sessions get() = db.listenerSessions()

    // NotifStore
    override fun append(e: NotifEvent) = notifs.insert(e.toEntity())
    override fun range(from: EpochMs, to: EpochMs): List<NotifEvent> = notifs.range(from.value, to.value).map { it.toModel() }
    override fun count(): Int = notifs.count()
    override fun purgeBefore(ts: EpochMs): Int = notifs.purgeBefore(ts.value)

    // ListenerCoverage
    override fun sessions(from: EpochMs, to: EpochMs): List<ListenerSession> =
        sessions.overlapping(from.value, to.value).map { it.toModel() }

    /** True when the listener was connected for the whole of [from, to]. An empty interval is covered if a session holds that instant. */
    override fun coversInterval(from: EpochMs, to: EpochMs): Boolean {
        if (to.value <= from.value) return coveredMs(from.value, from.value + 1) > 0
        return coveredMs(from.value, to.value) == to.value - from.value
    }

    /** Share of [from, to] the listener was connected, 0.0 to 1.0. An empty interval gives 0.0. */
    override fun coverageFraction(from: EpochMs, to: EpochMs): Double {
        val length = to.value - from.value
        if (length <= 0) return 0.0
        return coveredMs(from.value, to.value).toDouble() / length
    }

    /** A second open session is not recorded: one connection is one session. */
    override fun openSession(at: EpochMs) {
        if (sessions.openCount() == 0) sessions.insert(ListenerSessionEntity(connectedAt = at.value, disconnectedAt = null))
    }

    override fun closeSession(at: EpochMs) {
        sessions.closeOpen(at.value)
    }

    private fun coveredMs(from: Long, to: Long): Long {
        var covered = 0L
        var runStart = -1L
        var runEnd = -1L
        for (s in sessions.overlapping(from, to)) {
            val a = maxOf(s.connectedAt, from)
            val b = minOf(s.disconnectedAt ?: to, to)
            if (b <= a) continue
            if (runStart < 0) { runStart = a; runEnd = b }
            else if (a <= runEnd) runEnd = maxOf(runEnd, b)
            else { covered += runEnd - runStart; runStart = a; runEnd = b }
        }
        if (runStart >= 0) covered += runEnd - runStart
        return covered
    }
}
