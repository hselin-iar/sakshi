package com.kleos.sakshi.host

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kleos.sakshi.engine.model.EpochMs
import java.io.File

/**
 * Debug builds only (src/debug): writes the platform's last N hours of RawEvents to files/dumps for spikes S-A and S-B
 * and for Track 2's real fixtures. Nothing here is compiled into release.
 *
 *   adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.DUMP_EVENTS --ei hours 24
 *   adb exec-out run-as com.kleos.sakshi cat files/dumps/events.json > fixtures/real_s_a.json
 */
class DebugTools : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val hours = intent.getIntExtra("hours", 24).coerceAtLeast(1)
        val now = System.currentTimeMillis()
        try {
            val events = UsageEventsSource(context).read(EpochMs(now - hours * 3_600_000L), EpochMs(now))
            val file = File(context.filesDir, "dumps").apply { mkdirs() }.resolve("events.json")
            file.writeText(toJson(events, now))
            Log.i(TAG, "dumped ${events.size} events, oldest ${events.firstOrNull()?.ts?.value}, to ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "dump failed: ${e::class.simpleName}")
        }
    }

    private fun toJson(events: List<com.kleos.sakshi.engine.model.RawEvent>, dumpedAt: Long): String = buildString {
        append("{\"dumpedAt\":").append(dumpedAt).append(",\"events\":[")
        events.forEachIndexed { i, e ->
            if (i > 0) append(',')
            append("{\"ts\":").append(e.ts.value).append(",\"type\":\"").append(e.type.name).append("\",\"pkg\":")
            if (e.pkg == null) append("null") else append('"').append(e.pkg!!.value).append('"')
            append('}')
        }
        append("]}")
    }

    companion object {
        const val ACTION = "com.kleos.sakshi.DUMP_EVENTS"
        private const val TAG = "SakshiDebug"
    }
}
