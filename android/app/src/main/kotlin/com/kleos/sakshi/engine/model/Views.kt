package com.kleos.sakshi.engine.model

// Views (LC-3): plain immutable data classes of primitives, Strings, lists, enums and other Views.

data class StudyBlockView(val startMinute: Int, val endMinute: Int)                 // minutes from local midnight; end < start crosses midnight
data class PartLines(val stretch: String?, val stays: String?, val ret: String?, val quiet: String?)   // `ret` because `return` is reserved
data class PartsView(
    val stretchMin: Double?, val longestStretchMin: Double?, val inSetShare: Double?, val quietShare: Double?,
    val staysPerHour: Double?, val glances: Int, val returnMin: Double?, val lines: PartLines, val extrasLines: List<String>)
data class SteadinessView(val value: Int, val word: String)                         // word ∈ Wavering | Steady | Steadier
data class StonesView(
    val totalStays: Int, val stoneCount: Int, val selfStartedCount: Int, val unknownCount: Int,
    val topStoneLabel: String?, val noRippleRate: Double?, val line: String)
data class ClearHourView(val startHour: Int, val endHour: Int, val stretchMin: Double, val line: String)
data class PatternLine(val kindId: String, val line: String, val evidenceWindows: Int, val evidenceDays: Int)
data class SuggestionView(
    val kindId: String, val subjectKey: String?, val line: String, val actionLabel: String, val actionType: String,
    val opensSettings: Boolean)
data class ObservationView(val kindId: String, val line: String)
data class VerdictView(val verdict: String, val line: String, val beforeValue: Double?, val afterValue: Double?, val approxMix: Boolean)
data class GoalTapView(val offered: Boolean, val answer: String?)                   // YES | PARTLY | NOT_YET
data class TeacherView(val opensThisWeek: Int, val opensPrevWeek: Int?, val minutesThisWeek: Double, val line: String)
data class SayingView(val id: String, val text: String, val source: String, val tierLabel: String, val question: Int)

data class MirrorView(
    val isDemo: Boolean, val provisional: Boolean, val gentle: Boolean,
    val weekStart: EpochMs, val weekLabel: String,                                  // "5–11 Oct"
    val dataState: DataState,
    val dataFlags: List<DataFlag>, val dataLines: List<String>,
    val headline: String,
    val parts: PartsView?, val steadiness: SteadinessView?,
    val stones: StonesView?, val clearHour: ClearHourView?,
    val patterns: List<PatternLine>,                                                // at most 4
    val suggestion: SuggestionView?, val observation: ObservationView?, val nothingToFix: Boolean,
    val verdict: VerdictView?, val goalTap: GoalTapView, val teacher: TeacherView?,
    val lapseLine: String?, val saying: SayingView?, val returnLine: String?,
    val reanchorOffered: Boolean, val suggestedStudyBlock: StudyBlockView?)         // v1.1 CA-1

data class WeekRef(val weekStart: EpochMs, val label: String, val completed: Boolean)

data class TodayWindowView(
    val start: EpochMs, val end: EpochMs, val shape: String?, val stretchMin: Double?, val stays: Int, val returnMin: Double?)
data class TodayView(
    val isDemo: Boolean, val windows: List<TodayWindowView>, val parts: PartsView?, val line: String,
    val dataFlags: List<DataFlag>, val dataLines: List<String>)

data class LakeView(val state: LakeState, val phrase: String, val asOf: EpochMs?)

data class WhatISeeView(
    val isDemo: Boolean, val usageAccessGranted: Boolean, val notificationAccessGranted: Boolean,
    val rawEventCount: Int, val notifEventCount: Int, val oldestRawEvent: EpochMs?, val derivedDays: Int,
    val listenerCoverage7d: Double?, val lastWorkerRun: EpochMs?, val workerRuns7d: Int, val paused: Boolean,
    val lastError: String?, val oddEventPairs: Int, val lines: List<String>)

data class HostFacts(
    val usageAccessGranted: Boolean, val notificationAccessGranted: Boolean, val canPostNotifications: Boolean,
    val lastError: String?, val oddEventPairsUnknown: Boolean = false)

// [CONTRACT GAP: the following façade types are named in LC-3 but never defined in DOC 3. Minimal shapes, derived from
//  Pigeon DTOs and feature text, so the façade compiles. Integration Owner to confirm or replace before T2.15.]
data class WorkSetEntry(val pkg: Pkg, val userClass: UserClass)
data class StudyHours(val blocks: List<StudyBlock>, val learnForMe: Boolean)
data class SaveResult(val ok: Boolean, val savedCount: Int, val userMessage: String?)
data class ProcessReport(val lakeChanged: Boolean)
data class TapResult(val ok: Boolean, val reason: String?)
data class ReanchorResult(val ok: Boolean, val reason: String?)
data class ExportDocument(val exportVersion: Int, val exportedAt: EpochMs, val includeRaw: Boolean)
data class NoteDecision(val post: Boolean, val reason: String?)
