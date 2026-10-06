package com.kleos.sakshi.host

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.LakeRow
import com.kleos.sakshi.engine.model.NoteDecision
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekStart
import com.kleos.sakshi.engine.model.LakeState
import java.io.File

/**
 * Debug builds only (src/debug): writes the platform's last N hours of RawEvents to files/dumps for spikes S-A and S-B
 * and for Track 2's real fixtures. Nothing here is compiled into release.
 *
 *   adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.DUMP_EVENTS --ei hours 24
 *   adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.RUN_INGEST_NOW
 *   adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.SET_LAKE_STATE --es state choppy
 *   adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.FORCE_WEEKLY_NOTE [--el week <epochDay of a Monday>]
 *   adb exec-out run-as com.kleos.sakshi cat files/dumps/events.json > fixtures/real_s_a.json
 */
class DebugTools : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == RUN_NOW) {
            // Runs the same catch-up as the periodic job, right now, and logs what ingest_state says afterwards.
            Scheduler.runNow(context)
            Log.i(TAG, "queued an ingest run")
            return
        }
        if (intent.action == SET_LAKE) {
            // Writes a lake_state row and redraws, to see each widget state without waiting for a window to finish.
            val state = LakeState.entries.firstOrNull { it.name.equals(intent.getStringExtra("state"), ignoreCase = true) }
            if (state == null) { Log.w(TAG, "state must be one of ${LakeState.entries.joinToString()}"); return }
            val container = AppContainer.from(context)
            val asOf = if (state == LakeState.NO_DATA) null else EpochMs(System.currentTimeMillis())
            Thread {
                container.state.saveLake(LakeRow(state, DEBUG_PHRASES.getValue(state), asOf))
                LakeWidget.refresh(context)
                Log.i(TAG, "lake_state set to $state")
            }.start()
            return
        }
        if (intent.action == FORCE_NOTE) {
            // Pretends a Mirror is ready for the given week (epoch day of its Monday; default: this Monday) and forces a "post" decision.
            val container = AppContainer.from(context)
            val week = intent.getLongExtra("week", java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY).toEpochDay())
            Thread {
                val note = container.real.state.note()
                container.real.state.saveNote(note.copy(mirrorReadyWeek = WeekStart(StudyDay(week))))
                val outcome = container.noteNotifier.maybePost(NoteDecision(post = true, reason = "debug"))
                Log.i(TAG, "weekly note decision for week $week: $outcome")
            }.start()
            return
        }
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
        const val RUN_NOW = "com.kleos.sakshi.RUN_INGEST_NOW"
        const val SET_LAKE = "com.kleos.sakshi.SET_LAKE_STATE"
        const val FORCE_NOTE = "com.kleos.sakshi.FORCE_WEEKLY_NOTE"
        /** The fixed phrases from DOC 3 F7 (normal set). In real builds the engine's SentenceBuilder supplies them. */
        private val DEBUG_PHRASES = mapOf(
            LakeState.STILL to "Still water.", LakeState.RIPPLED to "A few ripples.", LakeState.CHOPPY to "Choppy water.",
            LakeState.LEARNING to "Learning your normal.", LakeState.NO_DATA to "Nothing to show yet.")
        private const val TAG = "SakshiDebug"
    }
}
