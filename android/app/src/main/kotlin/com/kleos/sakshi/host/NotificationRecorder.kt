package com.kleos.sakshi.host

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.model.NotifKind
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RemovalKind
import com.kleos.sakshi.engine.ports.ListenerCoverage
import com.kleos.sakshi.engine.ports.NotifStore
import com.kleos.sakshi.engine.ports.StateStore
import java.util.concurrent.atomic.AtomicInteger

/**
 * The only fields a notification is ever reduced to. There is no title, text, extras, icon, intent or channel here
 * or anywhere downstream (PrivacyBoundaryTest holds NotifEvent to the same list).
 */
data class NotificationFacts(val pkg: String, val postTime: Long, val category: String?, val ongoing: Boolean)

/** Plain Kotlin over the ports, so it runs against in-memory stores. Records facts and judges nothing. */
class NotificationRecorder(
    private val notifs: NotifStore,
    private val coverage: ListenerCoverage,
    private val state: StateStore,
    private val ownPackage: String,
    private val now: () -> EpochMs) {

    private val errors = AtomicInteger(0)

    /** Callbacks that threw and were swallowed. A listener must never crash the process. */
    val errorCount: Int get() = errors.get()

    fun onConnected() = guarded {
        val at = now()
        closeStaleOpenSession(at)
        coverage.openSession(at)
    }

    fun onDisconnected() = guarded { coverage.closeSession(now()) }

    fun onPosted(facts: NotificationFacts) = guarded {
        if (!accepts(facts)) return@guarded
        notifs.append(NotifEvent(EpochMs(facts.postTime), Pkg(facts.pkg), facts.category, NotifKind.POSTED, null, facts.ongoing))
    }

    /** `clicked` is true for the system's click reason; every other reason is OTHER. */
    fun onRemoved(facts: NotificationFacts, clicked: Boolean) = guarded {
        if (!accepts(facts)) return@guarded
        notifs.append(
            NotifEvent(now(), Pkg(facts.pkg), facts.category, NotifKind.REMOVED, if (clicked) RemovalKind.CLICK else RemovalKind.OTHER, facts.ongoing))
    }

    /** Dropped while paused, and Sakshi's own notifications are ignored. */
    private fun accepts(facts: NotificationFacts): Boolean = facts.pkg != ownPackage && !state.ingest().paused

    /**
     * A crash can leave a session open. Before opening a new one it is closed at the last notification heard since it
     * opened, or at its own start if none: the time it was last known to be alive.
     */
    private fun closeStaleOpenSession(at: EpochMs) {
        val stale = coverage.sessions(EpochMs(0), EpochMs(at.value + 1)).lastOrNull { it.disconnectedAt == null } ?: return
        val lastHeard = notifs.range(stale.connectedAt, EpochMs(at.value + 1)).lastOrNull()?.ts ?: stale.connectedAt
        coverage.closeSession(lastHeard)
    }

    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
            errors.incrementAndGet()
        }
    }
}
