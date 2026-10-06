package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.ports.EventStore
import com.kleos.sakshi.engine.ports.NotifStore
import com.kleos.sakshi.engine.tuning.Tuning

/** Raw and notification events older than RAW_RETENTION_DAYS go. An event exactly that old stays; one millisecond older goes. */
class Retention(private val events: EventStore, private val notifs: NotifStore) {
    data class Purged(val rawEvents: Int, val notifEvents: Int)

    fun purge(asOf: EpochMs): Purged {
        val cutoff = EpochMs(asOf.value - Tuning.RAW_RETENTION_DAYS * DAY_MS)
        return Purged(events.purgeBefore(cutoff), notifs.purgeBefore(cutoff))
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
