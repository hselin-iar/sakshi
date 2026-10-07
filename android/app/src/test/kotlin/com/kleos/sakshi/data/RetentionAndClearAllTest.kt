package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.tuning.Tuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionAndClearAllTest : DbTest() {
    private val asOf = 100L * DAY
    private val cutoff = asOf - Tuning.RAW_RETENTION_DAYS * DAY

    @Test fun retentionKeepsAnEventExactlyFourteenDaysOldAndPurgesOneMillisecondOlder() {
        val events = RoomEventStore(db); val notifs = RoomNotifStore(db)
        events.append(listOf(raw(cutoff - 1), raw(cutoff), raw(cutoff + 1)))
        notifs.append(notif(cutoff - 1)); notifs.append(notif(cutoff))
        val purged = Retention(events, notifs).purge(t(asOf))
        assertEquals(Retention.Purged(rawEvents = 1, notifEvents = 1), purged)
        assertEquals(listOf(cutoff, cutoff + 1), events.range(t(0), t(asOf)).map { it.ts.value })
    }

    @Test fun retentionLeavesDerivedRowsAlone() {
        val derived = RoomDerivedStore(db)
        derived.replaceDay(StudyDay(1), derivation(1, 1))
        Retention(RoomEventStore(db), RoomNotifStore(db)).purge(t(asOf))
        assertEquals(1, derived.days(StudyDay(0), StudyDay(9)).size)
    }

    private fun userTables(): List<String> = db.openHelper.readableDatabase
        .query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name <> 'room_master_table'")
        .use { c -> generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList() }

    private fun rows(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getInt(0) }

    private fun fillEveryTable() {
        RoomEventStore(db).append(listOf(raw(1)))
        RoomNotifStore(db).apply { append(notif(1)); openSession(t(1)) }
        RoomGapStore(db).add(DataGap(GapKind.PAUSED, t(1), null))
        RoomDerivedStore(db).apply {
            replaceDay(StudyDay(1), derivation(1, 1))
            upsertWeek(WeekSummary(WeekStart(StudyDay(1)), null, null, null, null, null, null, null, null, 0, 0, false, 0, 0.0, 0))
            upsertPatterns(listOf(Pattern(PatternKind.TREND, "k", 1.0, 1, 1, t(1), t(1), mapOf("a" to "b"))))
        }
        RoomStateStore(db).apply {
            saveIngest(ingest().copy(workerRuns7d = 1)); replaceApps(listOf(AppMeta(Pkg("a"), "A", null, UserClass.IN_SET, t(1))))
            saveSettings(settings().copy(gentleMode = true)); saveBaseline(Baseline(0, t(1), 1.0, 1.0, 1.0, 1.0, 8, true))
            saveSuggestionState(SuggestionState(SuggestionKind.S1, null, null, null, null, null, null, "new"))
            saveExperiment(Experiment(0, SuggestionKind.S1, null, t(1), StartReason.TAP, TargetMetric.MEDIAN_RETURN, null, null, t(2), Verdict.PENDING, false, false))
            savePick(SayingPick(0, "sy01", t(1))); saveGoalTap(GoalTap(WeekStart(StudyDay(1)), GoalAnswer.YES))
            saveLake(LakeRow(LakeState.STILL, "p", null)); saveNote(NoteState(null, null, WeekStart(StudyDay(1))))
        }
    }

    @Test fun theSchemaHasOneTablePerLc2Entity() {
        val expected = setOf(
            "raw_event", "notif_event", "listener_session", "data_gap", "ingest_state", "app_meta", "settings",
            "day_summary", "window", "stretch", "stay", "week_summary", "baseline", "pattern", "suggestion_state",
            "experiment", "saying_pick", "goal_tap", "lake_state", "note_state")
        assertEquals(expected, userTables().toSet())
        assertEquals(20, userTables().size)
    }

    @Test fun clearAllEmptiesEveryTableEnumeratedFromTheSchema() {
        fillEveryTable()
        val tables = userTables()
        tables.forEach { assertTrue("$it should have a row before clearAll", rows(it) > 0) }
        RoomDerivedStore(db).clearAll()
        tables.forEach { assertEquals("$it should be empty after clearAll", 0, rows(it)) }
    }

    @Test fun afterClearAllTheStoresBehaveLikeAFreshInstall() {
        fillEveryTable()
        RoomDerivedStore(db).clearAll()
        assertEquals(null, RoomStateStore(db).ingest().cursor)
        assertEquals(0, RoomEventStore(db).count())
        RoomEventStore(db).append(listOf(raw(5)))   // still writable
        assertEquals(1, RoomEventStore(db).count())
    }
}
