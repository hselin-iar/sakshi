package com.kleos.sakshi.engine.testkit

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ListenerSession
import com.kleos.sakshi.engine.ports.ListenerCoverage

/**
 * Dumb in-memory store. The interval-merge math here mirrors what a real
 * (Room-backed) adapter would also have to do over its rows — it is not
 * attention logic, so it belongs here rather than in the engine.
 */
class FakeListenerCoverage : ListenerCoverage {
    private val sessions = mutableListOf<ListenerSession>()

    override fun sessions(from: EpochMs, to: EpochMs): List<ListenerSession> =
        sessions.filter { session ->
            session.connectedAt.value < to.value &&
                (session.disconnectedAt?.value ?: Long.MAX_VALUE) > from.value
        }

    override fun coversInterval(from: EpochMs, to: EpochMs): Boolean =
        coverageFraction(from, to) >= 1.0

    override fun coverageFraction(from: EpochMs, to: EpochMs): Double {
        val total = (to.value - from.value).toDouble()
        if (total <= 0.0) return 1.0
        val covered = mergedCoveredMs(from, to)
        return (covered / total).coerceIn(0.0, 1.0)
    }

    override fun openSession(at: EpochMs) {
        sessions += ListenerSession(connectedAt = at, disconnectedAt = null)
    }

    override fun closeSession(at: EpochMs) {
        val openIndex = sessions.indexOfLast { it.disconnectedAt == null }
        if (openIndex >= 0) {
            sessions[openIndex] = sessions[openIndex].copy(disconnectedAt = at)
        }
    }

    private fun mergedCoveredMs(from: EpochMs, to: EpochMs): Long {
        val clipped = sessions
            .map { session ->
                val start = maxOf(session.connectedAt.value, from.value)
                val end = minOf(session.disconnectedAt?.value ?: to.value, to.value)
                start to end
            }
            .filter { (start, end) -> end > start }
            .sortedBy { it.first }

        var coveredEnd = Long.MIN_VALUE
        var total = 0L
        for ((start, end) in clipped) {
            val effectiveStart = maxOf(start, coveredEnd)
            if (end > effectiveStart) {
                total += end - effectiveStart
                coveredEnd = end
            }
        }
        return total
    }
}
