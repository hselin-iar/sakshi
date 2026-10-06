package com.kleos.sakshi.data

import android.util.JsonWriter
import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.ports.DerivedStore
import com.kleos.sakshi.engine.ports.EventStore
import com.kleos.sakshi.engine.ports.NotifStore
import com.kleos.sakshi.engine.ports.StateStore
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Export (F10): derived rows, settings and the user's own choices as JSON, raw events only when asked. Package names, never app
 * labels, and no title or text anywhere (there is none to write). Streamed to a temp file and renamed on success, so a failure
 * leaves nothing half-written.
 * [REFACTOR CANDIDATE: DOC 3 builds the document in the engine (ExportBuilder). Until Track 2 delivers it, this reads the ports directly.]
 */
class Exporter(
    private val cacheDir: File, private val events: EventStore, private val notifs: NotifStore,
    private val derived: DerivedStore, private val state: StateStore, private val zone: ZoneId = ZoneId.systemDefault()) {

    data class Result(val fileName: String, val byteSize: Long, val file: File)

    private val dir get() = File(cacheDir, "exports")

    fun export(includeRaw: Boolean, asOf: EpochMs): Result {
        dir.mkdirs()
        check(dir.isDirectory) { "exports directory unavailable" }
        val name = "sakshi_export_" + DateTimeFormatter.ofPattern("yyyyMMdd").format(Instant.ofEpochMilli(asOf.value).atZone(zone)) + ".json"
        val temp = File(dir, "$name.tmp")
        val target = File(dir, name)
        try {
            temp.bufferedWriter().use { out -> write(JsonWriter(out).apply { setIndent("  ") }, includeRaw, asOf) }
            if (target.exists()) target.delete()
            check(temp.renameTo(target)) { "rename failed" }
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
        return Result(name, target.length(), target)
    }

    /** Delete everything also removes what was exported. */
    fun clearExports() { dir.deleteRecursively() }

    private fun write(w: JsonWriter, includeRaw: Boolean, asOf: EpochMs) {
        w.beginObject()
        w.name("exportVersion").value(1)
        w.name("exportedAt").value(asOf.value)
        settings(w, state.settings())
        w.name("baselines").beginArray(); state.baseline()?.let { baseline(w, it) }; w.endArray()
        w.name("weeks").beginArray(); derived.weeks().forEach { week(w, it) }; w.endArray()
        w.name("days").beginArray(); derived.days(StudyDay(0), StudyDay(Long.MAX_VALUE)).forEach { day(w, it) }; w.endArray()
        w.name("windows").beginArray(); derived.windows(EpochMs(0), EpochMs(Long.MAX_VALUE)).forEach { window(w, it) }; w.endArray()
        w.name("patterns").beginArray(); derived.patterns().forEach { pattern(w, it) }; w.endArray()
        w.name("suggestionStates").beginArray(); state.suggestionStates().forEach { suggestionState(w, it) }; w.endArray()
        w.name("experiments").beginArray(); state.experiments().forEach { experiment(w, it) }; w.endArray()
        w.name("goalTaps").beginArray(); state.goalTaps().forEach { w.beginObject().name("weekStart").value(it.weekStart.studyDay.epochDay).name("answer").value(it.answer.name).endObject() }; w.endArray()
        w.name("sayingPicks").beginArray(); state.sayingPicks().forEach { w.beginObject().name("sayingId").value(it.sayingId).name("pickedAt").value(it.pickedAt.value).endObject() }; w.endArray()
        if (includeRaw) {
            w.name("raw").beginObject()
            w.name("rawEvents").beginArray()
            events.range(EpochMs(0), EpochMs(Long.MAX_VALUE)).forEach {
                w.beginObject().name("ts").value(it.ts.value).name("type").value(it.type.name).name("pkg").nullable(it.pkg?.value).endObject()
            }
            w.endArray()
            w.name("notifEvents").beginArray()
            notifs.range(EpochMs(0), EpochMs(Long.MAX_VALUE)).forEach {
                w.beginObject().name("ts").value(it.ts.value).name("pkg").value(it.pkg.value).name("category").nullable(it.category)
                    .name("kind").value(it.kind.name).name("removal").nullable(it.removal?.name).name("ongoing").value(it.ongoing).endObject()
            }
            w.endArray()
            w.endObject()
        }
        w.endObject()
    }

    private fun JsonWriter.nullable(v: String?): JsonWriter = if (v == null) nullValue() else value(v)
    private fun JsonWriter.nullable(v: Double?): JsonWriter = if (v == null) nullValue() else value(v)
    private fun JsonWriter.nullable(v: Long?): JsonWriter = if (v == null) nullValue() else value(v)
    private fun JsonWriter.nullable(v: Int?): JsonWriter = if (v == null) nullValue() else value(v.toLong())
    private fun JsonWriter.nullable(v: Boolean?): JsonWriter = if (v == null) nullValue() else value(v)

    private fun settings(w: JsonWriter, s: Settings) {
        w.name("settings").beginObject()
        w.name("studyBlocks").beginArray(); s.studyBlocks.forEach { w.beginObject().name("startMinute").value(it.startMinute.toLong()).name("endMinute").value(it.endMinute.toLong()).endObject() }; w.endArray()
        w.name("learnStudyHours").value(s.learnStudyHours).name("gentleMode").value(s.gentleMode).name("gentleExplicit").value(s.gentleExplicit)
        w.name("weeklyNoteEnabled").value(s.weeklyNoteEnabled).name("ageUnder18").value(s.ageUnder18).name("batteryHelperShown").value(s.batteryHelperShown)
        w.name("lapseAcknowledgedThrough").nullable(s.lapseAcknowledgedThrough?.epochDay).name("firstReadAt").nullable(s.firstReadAt?.value).name("createdAt").value(s.createdAt.value)
        w.endObject()
    }

    private fun baseline(w: JsonWriter, b: Baseline) {
        w.beginObject().name("id").value(b.id).name("frozenAt").value(b.frozenAt.value).name("c0").value(b.c0).name("p0").value(b.p0)
            .name("r0").value(b.r0).name("q0").value(b.q0).name("daysUsed").value(b.daysUsed.toLong()).name("isActive").value(b.isActive).endObject()
    }

    private fun week(w: JsonWriter, s: WeekSummary) {
        w.beginObject().name("weekStart").value(s.weekStart.studyDay.epochDay).name("stretchMedianMin").nullable(s.stretchMedianMin)
            .name("longestStretchMin").nullable(s.longestStretchMin).name("staysPerHour").nullable(s.staysPerHour)
            .name("returnMedianMin").nullable(s.returnMedianMin).name("quietShare").nullable(s.quietShare).name("inSetShare").nullable(s.inSetShare)
            .name("steadiness").nullable(s.steadiness).name("word").nullable(s.word).name("windowsCount").value(s.windowsCount.toLong())
            .name("validDays").value(s.validDays.toLong()).name("unusual").value(s.unusual).name("appOpens").value(s.appOpens.toLong())
            .name("appMinutes").value(s.appMinutes).name("returnsAfterLapse").value(s.returnsAfterLapse.toLong()).endObject()
    }

    private fun day(w: JsonWriter, d: DaySummary) {
        w.beginObject().name("day").value(d.day.epochDay).name("valid").value(d.valid).name("windowMinutes").value(d.windowMinutes)
            .name("quietMinutes").value(d.quietMinutes).name("inSetMinutes").value(d.inSetMinutes).name("coverage").value(d.coverage)
            .name("pickups").value(d.pickups.toLong()).name("switchesPerHour").nullable(d.switchesPerHour).name("flinch").nullable(d.flinch)
            .name("rampUpMin").nullable(d.rampUpMin).name("lastScreenOffTs").nullable(d.lastScreenOffTs?.value)
            .name("firstStretchMin").nullable(d.firstStretchMin).name("externalResumes").value(d.externalResumes.toLong()).endObject()
    }

    private fun window(w: JsonWriter, d: WindowWithDetail) {
        val x = d.window
        w.beginObject().name("id").value(x.id).name("day").value(x.day.epochDay).name("start").value(x.start.value).name("end").value(x.end.value)
            .name("source").value(x.source.name).name("partial").value(x.partial).name("finalised").value(x.finalised).name("shape").nullable(x.shape?.name)
            .name("quietMinutes").value(d.quietMinutes).name("glances").value(d.glances.toLong())
        w.name("stretches").beginArray()
        d.stretches.forEach {
            w.beginObject().name("id").value(it.id).name("start").value(it.start.value).name("end").value(it.end.value).name("minutes").value(it.minutes)
                .name("inSetMinutes").value(it.inSetMinutes).name("quietMinutes").value(it.quietMinutes).name("endedBy").value(it.endedBy.name).endObject()
        }
        w.endArray()
        w.name("stays").beginArray()
        d.stays.forEach {
            w.beginObject().name("id").value(it.id).name("start").value(it.start.value).name("end").value(it.end.value).name("firstPkg").value(it.firstPkg.value)
                .name("pkgMain").value(it.pkgMain.value).name("origin").value(it.origin.name).name("stonePkg").nullable(it.stonePkg?.value)
                .name("notifClicked").value(it.notifClicked).name("returnMinutes").nullable(it.returnMinutes).name("glancesBefore").value(it.glancesBefore.toLong()).endObject()
        }
        w.endArray()
        w.endObject()
    }

    private fun pattern(w: JsonWriter, p: Pattern) {
        w.beginObject().name("kind").value(p.kind.name).name("key").value(p.key).name("strength").value(p.strength)
            .name("evidenceWindows").value(p.evidenceWindows.toLong()).name("evidenceDays").value(p.evidenceDays.toLong())
            .name("firstSeen").value(p.firstSeen.value).name("lastSeen").value(p.lastSeen.value)
        w.name("args").beginObject(); p.args.toSortedMap().forEach { (k, v) -> w.name(k).value(v) }; w.endObject()
        w.endObject()
    }

    private fun suggestionState(w: JsonWriter, s: SuggestionState) {
        w.beginObject().name("kind").value(s.kind.name).name("subject").nullable(s.subject?.value).name("firstEligibleAt").nullable(s.firstEligibleAt?.value)
            .name("lastShownAt").nullable(s.lastShownAt?.value).name("shownInWeek").nullable(s.shownInWeek?.studyDay?.epochDay)
            .name("dismissedUntil").nullable(s.dismissedUntil?.value).name("retiredUntil").nullable(s.retiredUntil?.value).name("status").value(s.status).endObject()
    }

    private fun experiment(w: JsonWriter, e: Experiment) {
        w.beginObject().name("id").value(e.id).name("kind").value(e.kind.name).name("subject").nullable(e.subject?.value).name("startedAt").value(e.startedAt.value)
            .name("startReason").value(e.startReason.name).name("target").value(e.target.name).name("beforeValue").nullable(e.beforeValue)
            .name("afterValue").nullable(e.afterValue).name("windowEnd").value(e.windowEnd.value).name("verdict").value(e.verdict.name)
            .name("approxMix").value(e.approxMix).name("shown").value(e.shown).endObject()
    }
}
