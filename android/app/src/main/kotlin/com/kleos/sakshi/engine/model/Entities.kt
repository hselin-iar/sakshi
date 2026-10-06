package com.kleos.sakshi.engine.model

// ---- Facts (LC-1): written only by the collectors ----
enum class RawType { ACTIVITY_RESUMED, ACTIVITY_PAUSED, SCREEN_INTERACTIVE, SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN, KEYGUARD_HIDDEN }
data class RawEvent(val ts: EpochMs, val type: RawType, val pkg: Pkg?)      // pkg null for screen and keyguard events

enum class NotifKind { POSTED, REMOVED }
enum class RemovalKind { CLICK, OTHER }
data class NotifEvent(
    val ts: EpochMs, val pkg: Pkg, val category: String?, val kind: NotifKind,
    val removal: RemovalKind?, val ongoing: Boolean)
// There is NO title, text, extras, icon, intent or channel field. Adding one fails PrivacyBoundaryTest.

data class ListenerSession(val connectedAt: EpochMs, val disconnectedAt: EpochMs?)   // null while connected

enum class GapKind { PAUSED, NOT_SEEN }
data class DataGap(val kind: GapKind, val start: EpochMs, val end: EpochMs?)         // end null while a pause is open

// ---- Derived and state entities (LC-2); Room entities mirror these field for field ----
// [CONTRACT GAP: DOC 3 names the enums below but lists their members only in feature text or not at all; members are taken from that text.]
enum class UserClass { IN_SET, DEPENDS, NONE }
enum class WindowSource { STUDY_HOURS, INFERRED, BOTH }
enum class Shape { HELD, PINGED, REACHED }
enum class EndedBy { STAY, PUT_DOWN, WINDOW_END }
enum class Origin { STONE, SELF_STARTED, UNKNOWN }
enum class StartReason { TAP, FOOTPRINT }
enum class SuggestionKind { S1, S2, S3, S4, S5, S6, S7, S8, S9, S10, S11, S12 }
enum class TargetMetric {
    STRETCH_IN_SLOT, STAYS_PER_HOUR_FROM_PKG, SELF_STARTED_PER_HOUR, PINGS_FROM_PKG_IN_WINDOWS, MEDIAN_RETURN,
    FLINCH_RATE, WORKSET_COVERAGE, NEXT_DAY_FIRST_STRETCH, PUTDOWN_SHARE
}
enum class Verdict { PENDING, MOVED, NO_CHANGE, TOO_LITTLE, UNCLEAR }
enum class GoalAnswer { YES, PARTLY, NOT_YET }
enum class LakeState { LEARNING, NO_DATA, STILL, RIPPLED, CHOPPY }
enum class PatternKind { RHYTHM, TREND, SHIFT, SHAPE, BREAK_POINT, CROSS_DAY }
enum class DataFlag { PING_OFF, PARTIAL_PING, NOT_SEEN, PAUSED, TOO_LITTLE_DATA, UNUSUAL_WEEK, INTERNAL_PARTIAL, FIRST_LOOK }
enum class DataState { OK, LEARNING_BASELINE, TOO_LITTLE_DATA }

data class StudyBlock(val startMinute: Int, val endMinute: Int)      // minutes from local midnight; end < start crosses midnight

data class Settings(
    val studyBlocks: List<StudyBlock>, val learnStudyHours: Boolean, val gentleMode: Boolean, val gentleExplicit: Boolean,
    val weeklyNoteEnabled: Boolean, val ageUnder18: Boolean, val batteryHelperShown: Boolean,
    val lapseAcknowledgedThrough: StudyDay?, val firstReadAt: EpochMs?, val createdAt: EpochMs)

data class AppMeta(val pkg: Pkg, val label: String, val systemCategory: Int?, val userClass: UserClass, val addedAt: EpochMs)

data class DaySummary(
    val day: StudyDay, val valid: Boolean, val windowMinutes: Double, val quietMinutes: Double, val inSetMinutes: Double,
    val coverage: Double, val pickups: Int, val switchesPerHour: Double?, val flinch: Boolean?, val rampUpMin: Double?,
    val lastScreenOffTs: EpochMs?, val firstStretchMin: Double?, val externalResumes: Int)

data class Window(
    val id: Long, val day: StudyDay, val start: EpochMs, val end: EpochMs, val source: WindowSource,
    val partial: Boolean, val finalised: Boolean, val shape: Shape?)

data class Stretch(
    val id: Long, val windowId: Long, val start: EpochMs, val end: EpochMs, val minutes: Double,
    val inSetMinutes: Double, val quietMinutes: Double, val endedBy: EndedBy)

data class Stay(
    val id: Long, val windowId: Long, val start: EpochMs, val end: EpochMs, val firstPkg: Pkg, val pkgMain: Pkg,
    val origin: Origin, val stonePkg: Pkg?, val notifClicked: Boolean, val returnMinutes: Double?, val glancesBefore: Int)

data class WindowWithDetail(
    val window: Window, val stretches: List<Stretch>, val stays: List<Stay>, val quietMinutes: Double, val glances: Int)

data class DayDerivation(val windows: List<WindowWithDetail>, val summary: DaySummary)

data class WeekSummary(
    val weekStart: WeekStart, val stretchMedianMin: Double?, val longestStretchMin: Double?, val staysPerHour: Double?,
    val returnMedianMin: Double?, val quietShare: Double?, val inSetShare: Double?, val steadiness: Int?, val word: String?,
    val windowsCount: Int, val validDays: Int, val unusual: Boolean, val appOpens: Int, val appMinutes: Double,
    val returnsAfterLapse: Int)

data class Baseline(
    val id: Long, val frozenAt: EpochMs, val c0: Double, val p0: Double, val r0: Double, val q0: Double,
    val daysUsed: Int, val isActive: Boolean)

data class Pattern(
    val kind: PatternKind, val key: String, val strength: Double, val evidenceWindows: Int, val evidenceDays: Int,
    val firstSeen: EpochMs, val lastSeen: EpochMs, val args: Map<String, String>)

data class SuggestionState(
    val kind: SuggestionKind, val subject: Pkg?, val firstEligibleAt: EpochMs?, val lastShownAt: EpochMs?,
    val shownInWeek: WeekStart?, val dismissedUntil: EpochMs?, val retiredUntil: EpochMs?, val status: String)

data class Experiment(
    val id: Long, val kind: SuggestionKind, val subject: Pkg?, val startedAt: EpochMs, val startReason: StartReason,
    val target: TargetMetric, val beforeValue: Double?, val afterValue: Double?, val windowEnd: EpochMs,
    val verdict: Verdict, val approxMix: Boolean, val shown: Boolean)

data class SayingPick(val id: Long, val sayingId: String, val pickedAt: EpochMs)
data class GoalTap(val weekStart: WeekStart, val answer: GoalAnswer)
data class LakeRow(val state: LakeState, val phrase: String, val asOf: EpochMs?)
data class NoteState(val lastNoteWeek: WeekStart?, val mirrorReadyWeek: WeekStart?, val mirrorViewedWeek: WeekStart?)
data class IngestState(
    val cursor: EpochMs?, val lastRunAt: EpochMs?, val paused: Boolean, val pausedSince: EpochMs?, val firstReadAt: EpochMs?,
    val lastWorkerRunAt: EpochMs?, val workerRuns7d: Int, val lastError: String?, val oddEventPairs: Int)

data class Saying(val id: String, val q: String, val text: String, val source: String, val tier: Char, val usedFor: String)

// [CONTRACT GAP: AppInfo is used by AppCatalog.launcherApps() but never defined in DOC 3; shaped from AppDto (F2) plus the category port method.]
data class AppInfo(val pkg: Pkg, val label: String, val category: Int?)

// ---- T2.2: Foreground Intervals (engine-internal; not part of LC-1/LC-2) ----
data class ForegroundInterval(val pkg: Pkg, val start: EpochMs, val end: EpochMs)
data class ScreenSpan(val start: EpochMs, val end: EpochMs?)        // a screen-off (non-interactive) span
data class Reconstruction(
    val intervals: List<ForegroundInterval>,
    val screenOff: List<ScreenSpan>,
    val openEnded: Boolean,
    val anomalies: Int,          // odd event pairs tolerated by R1/R2; surfaced on the What I See page
)
