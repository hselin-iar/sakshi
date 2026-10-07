package com.kleos.sakshi.host

import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.host.gen.*
import java.lang.reflect.Modifier
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class MappersTest {
    private fun e(ms: Long) = EpochMs(ms)

    private val parts = PartsView(10.0, 32.5, 0.7, 0.36, 3.2, 14, 4.5, PartLines("s", "p", "r", "q"), listOf("extra one"))
    private val full = MirrorView(
        isDemo = true, provisional = false, gentle = false, weekStart = e(1_759_000_000_000), weekLabel = "5–11 Oct",
        dataState = DataState.OK, dataFlags = listOf(DataFlag.PARTIAL_PING, DataFlag.UNUSUAL_WEEK), dataLines = listOf("a line", "b line"),
        headline = "headline", parts = parts, steadiness = SteadinessView(116, "Steadier"),
        stones = StonesView(17, 11, 4, 2, "Chat", 0.25, "stones line"), clearHour = ClearHourView(21, 23, 18.5, "clear line"),
        patterns = listOf(PatternLine("RHYTHM", "pattern (based on 9 windows over 4 days)", 9, 4)),
        suggestion = SuggestionView("S2", "com.x", "sug line", "Move it", "MOVE_ICON", false),
        observation = ObservationView("S12", "obs line"), nothingToFix = false,
        verdict = VerdictView("MOVED", "verdict line", 4.1, 3.0, false), goalTap = GoalTapView(true, "PARTLY"),
        teacher = TeacherView(2, 3, 11.5, "teacher line"), lapseLine = "lapse", saying = SayingView("sy01", "I am watching my mind act", "src", "reported by others", 86),
        returnLine = "return", reanchorOffered = true, suggestedStudyBlock = StudyBlockView(1320, 90))
    private val provisional = MirrorView(
        isDemo = false, provisional = true, gentle = false, weekStart = e(1_759_000_000_000), weekLabel = "",
        dataState = DataState.LEARNING_BASELINE, dataFlags = emptyList(), dataLines = emptyList(), headline = "", parts = null, steadiness = null,
        stones = null, clearHour = null, patterns = emptyList(), suggestion = null, observation = null, nothingToFix = false, verdict = null,
        goalTap = GoalTapView(false, null), teacher = null, lapseLine = null, saying = null, returnLine = null, reanchorOffered = false,
        suggestedStudyBlock = null)

    private fun golden(name: String, dto: Any) = assertNull(GoldenJson.check(name, dto))

    @Test fun mirrorGoldens() {
        golden("mirror_provisional", provisional.toDto())
        golden("mirror_full", full.toDto())
    }

    @Test fun todayGoldens() {
        golden("today_empty", TodayView(false, emptyList(), null, "No finished window yet today.", emptyList(), emptyList()).toDto())
        golden("today_two_windows", TodayView(
            true, listOf(TodayWindowView(e(1_000), e(2_000), "HELD", 12.0, 1, 3.5), TodayWindowView(e(3_000), e(4_000), null, null, 0, null)),
            parts, "Today so far: 2 finished windows.", listOf(DataFlag.FIRST_LOOK), listOf("first look")).toDto())
    }

    @Test fun lakeGoldensForAllFiveStatesAndTheDemoFlag() {
        LakeState.entries.forEach { golden("lake_${it.name.lowercase()}", LakeView(it, "phrase ${it.name}", if (it == LakeState.NO_DATA) null else e(5_000)).toDto(isDemo = false)) }
        golden("lake_demo", LakeView(LakeState.STILL, "Still water. (demo)", e(5_000)).toDto(isDemo = true))
    }

    @Test fun whatISeeGoldens() {
        golden("what_i_see_empty", WhatISeeView(false, false, false, 0, 0, null, 0, null, null, 0, false, null, 0, emptyList()).toDto())
        golden("what_i_see_full", WhatISeeView(false, true, true, 8_400, 120, e(1_000), 9, 0.61, e(2_000), 61, false, "INGEST_X", 2, listOf("line a", "line b")).toDto())
    }

    @Test fun weekListSayingAndSetupStateMap() {
        golden("week_refs", listOf(WeekRef(e(1_000), "28 Sep–4 Oct", true), WeekRef(e(2_000), "5–11 Oct", false)).map { it.toDto() }.let { WeekRefList(it) })
        assertEquals(SayingDto("sy01", "t", "s", "reported by others", 86), Saying("sy01", "Q086", "t", "s", 'C', "u").toDto())
        val setup = setupStateDto(true, false, true, Settings(listOf(StudyBlock(1200, 1380)), false, true, false, true, false, true, null, null, e(0)),
            workSetSaved = true, ingest = IngestState(null, e(9), false, null, null, e(8), 61, "E", 0), listenerCoverage7d = null, isDemo = false)
        assertTrue(setup.usageAccessGranted && !setup.notificationAccessGranted && setup.restrictedSettingsSuspected)
        assertTrue(setup.workSetSaved && setup.studyHoursSaved && setup.gentleMode && setup.batteryHelperShown && setup.weeklyNoteEnabled)
        assertEquals(CollectionHealthDto(61, false, 8, null, "E"), setup.health)   // coverage stays null, never 0
    }

    private data class WeekRefList(val weeks: List<WeekRefDto>)

    @Test fun tierDIsNeverLabelledHisOwnWriting() {
        assertEquals("his own writing or letter", tierLabel('A'))
        assertEquals("recorded lecture", tierLabel('B'))
        assertEquals("reported by others", tierLabel('C'))
        assertEquals("type not resolved in the Outcome Map", tierLabel('D'))
        assertNotEquals("his own writing or letter", tierLabel('D'))
    }

    @Test fun requestMappersAndKindIds() {
        assertEquals(WorkSetEntry(Pkg("a"), UserClass.DEPENDS), WorkSetEntryDto("a", UserClassDto.DEPENDS).toModel())
        assertEquals(StudyHours(listOf(StudyBlock(1320, 90)), true), StudyHoursDto(listOf(StudyBlockDto(1320, 90)), true).toModel())
        assertEquals(SuggestionKind.S12, suggestionKindOf("S12"))
        assertNull(suggestionKindOf("S13")); assertNull(suggestionKindOf("s1")); assertNull(suggestionKindOf(""))
        assertEquals(GoalAnswer.NOT_YET, GoalAnswerDto.NOT_YET.toModel())
    }

    @Test fun weekStartIsTheMondayOfTheStudyDayAt0400() {
        val zone = ZoneId.of("Asia/Kolkata")
        fun ms(s: String) = ZonedDateTime.parse(s).toInstant().toEpochMilli()
        // Wed 2026-10-07 01:30 local is still study day Tue 10-06, whose week starts Mon 2026-10-05
        val monday = weekStartOf(ms("2026-10-07T01:30:00+05:30"), zone).studyDay.epochDay
        assertEquals(java.time.LocalDate.of(2026, 10, 5).toEpochDay(), monday)
        // Mon 2026-10-05 03:59 still belongs to the week before; 04:00 starts the new one
        assertEquals(java.time.LocalDate.of(2026, 9, 28).toEpochDay(), weekStartOf(ms("2026-10-05T03:59:00+05:30"), zone).studyDay.epochDay)
        assertEquals(java.time.LocalDate.of(2026, 10, 5).toEpochDay(), weekStartOf(ms("2026-10-05T04:00:00+05:30"), zone).studyDay.epochDay)
    }

    @Test fun errorCodesAreExactlyTheSevenOfLc4EachWithAUserMessage() {
        assertEquals(setOf("NO_PERMISSION", "DEMO_ACTIVE", "BAD_REQUEST", "STALE_SUGGESTION", "REANCHOR_NOT_ALLOWED", "EXPORT_FAILED", "INTERNAL"), HostErrors.codes())
        HostErrors.codes().forEach {
            val err = HostErrors.error(it, "Detail")
            assertEquals(it, err.code); assertTrue(err.message!!.isNotBlank()); assertFalse(err.message!!.contains("!"))
        }
    }

    // ---- the guard: a View field without a DTO field fails here ----
    private fun fields(c: Class<*>) = c.declaredFields.filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }.map { it.name }.toSet()

    /** Epoch-time fields get an "EpochMs" suffix; these two are also renamed in the Pigeon file. */
    private val renamed = mapOf("oldestRawEvent" to "oldestRawEpochMs", "lastWorkerRun" to "lastWorkerRunEpochMs")
    private val epochFields = setOf("weekStart", "start", "end", "asOf")

    private fun expectedDtoName(viewField: String, viewClass: Class<*>): String = when {
        viewField in renamed -> renamed.getValue(viewField)
        viewField in epochFields && viewClass.declaredFields.first { it.name == viewField }.type.let { it == Long::class.javaPrimitiveType || it == Long::class.javaObjectType || it == EpochMs::class.java } -> viewField + "EpochMs"
        else -> viewField
    }

    private val pairs: List<Triple<Class<*>, Class<*>, Set<String>>> = listOf(
        Triple(MirrorView::class.java, MirrorDto::class.java, emptySet()),
        Triple(PartsView::class.java, PartsDto::class.java, emptySet()),
        Triple(PartLines::class.java, PartLinesDto::class.java, emptySet()),
        Triple(SteadinessView::class.java, SteadinessDto::class.java, emptySet()),
        Triple(StonesView::class.java, StonesDto::class.java, emptySet()),
        Triple(ClearHourView::class.java, ClearHourDto::class.java, emptySet()),
        Triple(PatternLine::class.java, PatternLineDto::class.java, emptySet()),
        Triple(SuggestionView::class.java, SuggestionDto::class.java, emptySet()),
        Triple(ObservationView::class.java, ObservationDto::class.java, emptySet()),
        Triple(VerdictView::class.java, VerdictDto::class.java, emptySet()),
        Triple(GoalTapView::class.java, GoalTapDto::class.java, emptySet()),
        Triple(TeacherView::class.java, TeacherDto::class.java, emptySet()),
        Triple(SayingView::class.java, SayingDto::class.java, emptySet()),
        Triple(StudyBlockView::class.java, StudyBlockDto::class.java, emptySet()),
        Triple(WeekRef::class.java, WeekRefDto::class.java, emptySet()),
        Triple(TodayView::class.java, TodayDto::class.java, emptySet()),
        Triple(TodayWindowView::class.java, TodayWindowDto::class.java, emptySet()),
        Triple(WhatISeeView::class.java, WhatISeeDto::class.java, emptySet()),
        Triple(LakeView::class.java, LakeDto::class.java, setOf("isDemo")))   // isDemo is a host fact, not an engine view field

    @Test fun everyViewFieldHasADtoFieldAndNothingElseIsInTheDto() {
        pairs.forEach { (view, dto, hostOnly) ->
            val expected = fields(view).map { expectedDtoName(it, view) }.toSet() + hostOnly
            assertEquals("${view.simpleName} vs ${dto.simpleName}", expected, fields(dto))
        }
    }

    @Test fun theGuardWouldCatchAFieldAddedToAViewOnly() {
        // a stand-in view with one extra field must not match its DTO under the same rule
        class ObservationViewPlus(val kindId: String, val line: String, val extra: Int)
        val expected = fields(ObservationViewPlus::class.java).map { expectedDtoName(it, ObservationViewPlus::class.java) }.toSet()
        assertNotEquals(expected, fields(ObservationDto::class.java))
    }
}
