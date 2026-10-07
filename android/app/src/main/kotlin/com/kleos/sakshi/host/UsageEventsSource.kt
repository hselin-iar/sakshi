package com.kleos.sakshi.host

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType

/** What runIngest needs from the platform. Throws SecurityException when usage access is not granted. */
fun interface EventSource {
    fun read(from: EpochMs, to: EpochMs): List<RawEvent>
}

/**
 * Translates UsageStatsManager events into RawEvents and judges nothing: every package is kept, Sakshi's own included (D6).
 * Types are matched by the symbolic constant and stored by name, never by the integer value.
 */
class UsageEventsSource(context: Context) : EventSource {
    private val manager = context.applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun read(from: EpochMs, to: EpochMs): List<RawEvent> {
        val usage = manager.queryEvents(from.value, to.value) ?: return emptyList()
        val out = ArrayList<RawEvent>()
        val event = UsageEvents.Event()
        while (usage.hasNextEvent()) {
            usage.getNextEvent(event)
            val type = typeFor(event.eventType) ?: continue
            // Screen and keyguard events carry the system package; the vocabulary says pkg is null for them.
            val pkg = if (type == RawType.ACTIVITY_RESUMED || type == RawType.ACTIVITY_PAUSED) event.packageName?.let(::Pkg) else null
            out.add(RawEvent(EpochMs(event.timeStamp), type, pkg))
        }
        return out.sortedBy { it.ts.value }
    }

    companion object {
        /** The six supported event types; every other type is skipped. */
        fun typeFor(eventType: Int): RawType? = when (eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> RawType.ACTIVITY_RESUMED
            UsageEvents.Event.ACTIVITY_PAUSED -> RawType.ACTIVITY_PAUSED
            UsageEvents.Event.SCREEN_INTERACTIVE -> RawType.SCREEN_INTERACTIVE
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> RawType.SCREEN_NON_INTERACTIVE
            UsageEvents.Event.KEYGUARD_SHOWN -> RawType.KEYGUARD_SHOWN
            UsageEvents.Event.KEYGUARD_HIDDEN -> RawType.KEYGUARD_HIDDEN
            else -> null
        }
    }
}
