package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StateStoreTest : DbTest() {
    private val store get() = RoomStateStore(db)

    @Test fun unsetSingleRowStateReadsAsPlainDefaults() {
        assertNull(store.baseline()); assertNull(store.lake())
        assertEquals(emptyList<StudyBlock>(), store.settings().studyBlocks)
        assertEquals(NoteState(null, null, null), store.note())
        assertEquals(null, store.ingest().cursor)
        assertEquals(0, store.ingest().workerRuns7d)
    }

    @Test fun settingsRoundTripIncludingStudyBlocksCrossingMidnight() {
        val s = Settings(listOf(StudyBlock(1200, 1380), StudyBlock(1320, 90)), true, true, false, true, true, true, StudyDay(120), t(5_000), t(1_000))
        store.saveSettings(s)
        assertEquals(s, store.settings())
    }

    @Test fun reanchoringKeepsOneActiveBaselineAndTheOldOneInactive() {
        store.saveBaseline(Baseline(0, t(10), 10.0, 4.0, 6.0, 0.36, 8, true))
        val first = db.state().activeBaseline()!!
        store.saveBaseline(first.toModel().copy(isActive = false))
        store.saveBaseline(Baseline(0, t(20), 12.0, 3.5, 5.0, 0.40, 8, true))
        assertEquals(12.0, store.baseline()!!.c0, 0.0)
        assertEquals(2, db.openHelper.readableDatabase.query("SELECT * FROM baseline").use { it.count })
    }

    @Test fun replaceAppsSwapsTheWholeSet() {
        store.replaceApps(listOf(AppMeta(Pkg("a"), "A", 5, UserClass.IN_SET, t(1)), AppMeta(Pkg("b"), "B", null, UserClass.DEPENDS, t(1))))
        store.replaceApps(listOf(AppMeta(Pkg("c"), "C", null, UserClass.IN_SET, t(2))))
        assertEquals(listOf("c"), store.apps().map { it.pkg.value })
    }

    @Test fun suggestionStateWithNoSubjectIsAKeyOfItsOwn() {
        val none = SuggestionState(SuggestionKind.S3, null, t(1), null, null, null, null, "new")
        val some = none.copy(subject = Pkg("x"))
        store.saveSuggestionState(none); store.saveSuggestionState(some)
        store.saveSuggestionState(none.copy(status = "shown"))   // updates, does not duplicate
        assertEquals(setOf(none.copy(status = "shown"), some), store.suggestionStates().toSet())
        assertEquals(2, store.suggestionStates().size)
    }

    @Test fun experimentsPicksTapsLakeNoteAndIngestRoundTrip() {
        val e = Experiment(0, SuggestionKind.S2, Pkg("x"), t(1), StartReason.TAP, TargetMetric.STAYS_PER_HOUR_FROM_PKG, 4.1, null, t(99), Verdict.PENDING, false, false)
        store.saveExperiment(e)
        assertEquals(e.copy(id = store.experiments().single().id), store.experiments().single())

        store.savePick(SayingPick(0, "sy01", t(7)))
        assertEquals("sy01", store.sayingPicks().single().sayingId)

        val tap = GoalTap(WeekStart(StudyDay(98)), GoalAnswer.PARTLY)
        store.saveGoalTap(tap); store.saveGoalTap(tap.copy(answer = GoalAnswer.YES))
        assertEquals(listOf(tap.copy(answer = GoalAnswer.YES)), store.goalTaps())

        val lake = LakeRow(LakeState.STILL, "Still water.", t(9))
        store.saveLake(lake); assertEquals(lake, store.lake())

        val note = NoteState(WeekStart(StudyDay(91)), null, WeekStart(StudyDay(98)))
        store.saveNote(note); assertEquals(note, store.note())

        val ingest = IngestState(t(1), t(2), true, t(3), t(4), t(5), 6, "E1", 7)
        store.saveIngest(ingest); assertEquals(ingest, store.ingest())
    }
}
