package com.kleos.sakshi.data

import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.host.GoldenJson
import java.io.File
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class ExporterTest : DbTest() {
    private val cacheDir get() = RuntimeEnvironment.getApplication().cacheDir
    private val exporter get() = Exporter(cacheDir, RoomEventStore(db), RoomNotifStore(db), RoomDerivedStore(db), RoomStateStore(db), ZoneOffset.UTC)
    private val asOf = t(1_759_000_000_000)   // 2025-09-27 19:46:40 UTC

    private fun fixture() {
        val state = RoomStateStore(db)
        state.saveSettings(Settings(listOf(StudyBlock(1200, 1380)), false, true, true, false, false, true, null, t(100), t(50)))
        state.saveBaseline(Baseline(0, t(900), 10.0, 4.0, 6.0, 0.36, 8, true))
        state.suggestionStates().let { }
        state.saveSuggestionState(SuggestionState(SuggestionKind.S2, Pkg("com.chat"), t(1), t(2), WeekStart(StudyDay(20_000)), null, null, "shown"))
        state.saveExperiment(Experiment(0, SuggestionKind.S2, Pkg("com.chat"), t(10), StartReason.TAP, TargetMetric.STAYS_PER_HOUR_FROM_PKG, 4.1, 3.0, t(99), Verdict.MOVED, false, true))
        state.saveGoalTap(GoalTap(WeekStart(StudyDay(20_000)), GoalAnswer.PARTLY)); state.savePick(SayingPick(0, "sy01", t(77)))
        val derived = RoomDerivedStore(db)
        derived.replaceDay(StudyDay(100), derivation(100, 1, 2))
        derived.upsertWeek(WeekSummary(WeekStart(StudyDay(98)), 10.0, 30.0, 4.0, 6.0, 0.36, 0.7, 100, "Steady", 12, 5, false, 3, 20.0, 0))
        derived.upsertPatterns(listOf(Pattern(PatternKind.RHYTHM, "cell:NIGHT-WEEKDAY", 1.8, 9, 4, t(1_000), t(2_000), mapOf("direction" to "CHOPPY", "cell" to "NIGHT-WEEKDAY"))))
        RoomEventStore(db).append(listOf(raw(1_000, RawType.ACTIVITY_RESUMED, "com.notes"), raw(2_000, RawType.SCREEN_NON_INTERACTIVE, null)))
        RoomNotifStore(db).append(notif(1_500, "com.chat"))
    }

    @Test fun exportOfAFixtureEqualsTheGoldenJsonDerivedOnly() {
        fixture()
        val result = exporter.export(includeRaw = false, asOf = asOf)
        assertEquals("sakshi_export_20250927.json", result.fileName)
        assertNull(GoldenJson.checkText("export_derived", result.file.readText()))
        assertEquals(result.file.length(), result.byteSize)
    }

    @Test fun rawEventsAreIncludedOnlyWhenAsked() {
        fixture()
        val without = exporter.export(false, asOf).file.readText()
        assertFalse(without.contains("rawEvents")); assertFalse(without.contains("\"raw\""))
        val withRaw = exporter.export(true, asOf)
        assertNull(GoldenJson.checkText("export_with_raw", withRaw.file.readText()))
        assertTrue(withRaw.file.readText().contains("rawEvents"))
    }

    @Test fun theExportHoldsNoTitleTextOrAppLabel() {
        fixture()
        RoomStateStore(db).replaceApps(listOf(AppMeta(Pkg("com.chat"), "Super Secret Label", null, UserClass.IN_SET, t(1))))
        val text = exporter.export(true, asOf).file.readText()
        listOf("\"title\"", "\"text\"", "\"extras\"", "\"label\"", "Super Secret Label").forEach { assertFalse("export contains $it", text.contains(it)) }
        assertTrue(text.contains("com.chat"))     // package names only
    }

    @Test fun nothingNullIsTurnedIntoZero() {
        fixture()
        RoomDerivedStore(db).replaceDay(StudyDay(101), DayDerivation(emptyList(), daySummary(101).copy(switchesPerHour = null, flinch = null, firstStretchMin = null)))
        val text = exporter.export(false, asOf).file.readText()
        assertTrue(Regex("\"switchesPerHour\": null").containsMatchIn(text))
        assertTrue(Regex("\"firstStretchMin\": null").containsMatchIn(text))
    }

    @Test fun aTempFileIsRenamedOnSuccessAndNoTempIsLeft() {
        fixture()
        exporter.export(false, asOf)
        assertEquals(listOf("sakshi_export_20250927.json"), File(cacheDir, "exports").list()!!.toList())
    }

    @Test fun aFailureLeavesNothingPartial() {
        fixture()
        File(cacheDir, "exports").deleteRecursively()
        File(cacheDir, "exports").writeText("a file where the folder should be")     // exports/ cannot be created
        try { exporter.export(false, asOf); fail("expected a failure") } catch (_: Exception) { }
        assertTrue(File(cacheDir, "exports").isFile)       // untouched; no tmp or half file anywhere under cacheDir
        assertEquals(emptyList<String>(), cacheDir.walkTopDown().filter { it.name.endsWith(".tmp") }.map { it.name }.toList())
        File(cacheDir, "exports").delete()
    }

    @Test fun clearExportsRemovesWhatWasExported() {
        fixture(); exporter.export(false, asOf)
        exporter.clearExports()
        assertFalse(File(cacheDir, "exports").exists())
    }
}
