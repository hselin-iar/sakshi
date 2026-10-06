## Table of Contents
- Project Skeleton and Folder Structure
- Event Vocabulary and Tuning
- Collectors and Background Ingestion
- Foreground Intervals and App Classification
- F1. Instant First Look
- F2. Work-Set Setup
- F3. Window Finder
- F4. Stone and Wave
- F5. Steadiness and Its Four Parts
- Pattern Layer (P1 to P5, Cross-Day Link, Clustering)
- F11. The Suggestions Engine
- F12. Self-Judging Verdicts
- F6. The Weekly Mirror
- F7. The Lake (Home-Screen Widget)
- F8. Ask Now (Today So Far)
- F9. The Quiet Note
- F10. What I See (Pause, Export, Delete)
- F13. The Saying Shelf
- F14. The Teacher Leaves
- F15. Lapse and Return
- F16. Gentle Mode
- F17. Honest Data States
- Time Machine Demo and Personas
- Pigeon Bridge and Fake Host
- Flutter App Shell
- Locked Shared Contracts

---

# Sakshi (साक्षी): Module & Coding Architecture (DOC 3, v1.1)

**Builds on:** DOC 1 v7.1 (what), DOC 2 v3 (stack and layers). This document is the bridge to code: real folder paths, real type signatures, and worked examples an agent can be told to "make pass". It has no hour estimates and no build order (that is DOC 4).

**Tags:** `[ASSUMPTION: …]` is a call I made that you can overrule. `[VERIFY ON DEVICE: …]` is settled by a DOC 2 §2.10 spike. `[KNOWN RISK: …]` is accepted on purpose.

**v1.1 amendments (found while writing DOC 4; each closes a gap between DOC 3 and DOC 1, none changes a locked contract's existing fields).**

| # | Gap | Fix |
|---|---|---|
| CA-1 | DOC 1 offers a one-time re-anchor of the starting normal at week 4+ and a study-hours suggestion once the learned block differs, but `MirrorView` and `MirrorDto` carried no field to show either. | `MirrorView` and `MirrorDto` gain `reanchorOffered: Boolean` and `suggestedStudyBlock: StudyBlockView? / StudyBlockDto?` (both additive; a new `StudyBlockView(startMinute, endMinute)`). Rules in F6 step 8. Gentle mode sets both to false/null. The Pigeon call `reanchorBaseline()` already existed. |
| CA-2 | `DemoBanner` was placed in two folders (`lib/core/widgets/` and `lib/features/demo/`). | One location: `lib/features/demo/demo_banner.dart`. Track 3 creates a stub in step T3.1; Track 4 owns it afterwards. `lib/core/widgets/` no longer lists it. |
| CA-3 | The F9 note rule "skip if the Mirror was already viewed" had no writer. | `SakshiEngine.mirror()` records `note_state.mirrorViewedWeek` when the returned week is completed and not demo. It is the **only write on a read path**, kept deliberately, covered by a test. |
| CA-4 | The widget's tap target did not say how Flutter learns to open the Mirror. | `MainActivity.getInitialRoute()` returns `/mirror` when the launching intent carries the extra `route=mirror` (set by the widget and note PendingIntents). |

**Read this first: what DOC 3 decided that DOC 1 and DOC 2 left open.**

You rule by "functionality over understanding", so I closed every ambiguity that an agent would otherwise close by guessing. Each item below is a decision, not a question. Disagree now, not mid-build.

| # | Open point | Decision | Where it bites |
|---|---|---|---|
| D1 | DOC 1 says phone-quiet counts as holding, and also that a screen-off of 3 minutes ends a stretch as a "put-down". | **Quiet time joins the stretch it follows. A quiet span of 3 minutes or more ends that stretch as `PUT_DOWN`; the next in-set use starts a new one.** So 25 minutes of study plus 5 minutes screen-off is one 30-minute stretch ended by put-down. | Foreground Intervals; F5 |
| D2 | DOC 1 says the baseline is "the first 8 valid days after Sakshi first reads the device". First Look shows older retained days. | **Retained days before first read are shown in First Look but are not in the baseline.** Baseline days start at the study day of first read. | F1; F5 |
| D3 | DOC 2 said "the engine emits data, the presenter words it" and also "a sentence builder is tested in the engine". | **The engine builds the final English sentences** (`SentenceBuilder`, Kotlin), so the forbidden-words test runs without a phone. DTOs carry both numbers and finished lines. Flutter displays and never words. | F6; Locked Contracts |
| D4 | DOC 2 had no place for "not seen" and "paused" spans. | **New fact table `data_gap` (kind PAUSED or NOT_SEEN, start, end).** Windows overlapping a gap are `partial` and excluded from every statistic. | F17; Collectors |
| D5 | DOC 2 §2.4.3 had no port for the Saying shelf. | **New port `SayingShelf`.** The host reads `assets/sakshi/sayings.json`. | F13 |
| D6 | The Teacher Leaves meter needs Sakshi's own foreground events. | **`UsageEventsSource` must not filter out Sakshi's own package.** The engine ignores it in every metric and uses it only for the meter. | Collectors; F14 |
| D7 | Suggestion volume "shrinks" at week 8 (DOC 1 F14) with no rule. | **From week 8, at most one suggestion every two weeks.** | F11; F14 |
| D8 | Gentle mode shows "just the return line and the stay count". | **In gentle mode the Mirror shows the return line, the stay count, the Saying, the lapse line and honest data states. No Steadiness, no patterns, no suggestion, no goal tap, no Teacher Leaves line.** | F16 |
| D9 | DOC 1 says a "run" of in-set use of 5 minutes makes a window but never defines a run. | **A run is a chain of in-set intervals with gaps of at most 3 minutes and at least 5 minutes of in-set time.** | F3 |
| D10 | The Lake has three states plus "learning". | **Five `LakeStateDto` values (still, rippled, choppy, learning, noData); the widget draws three vectors and shows a different phrase for learning and noData.** | F7 |
| D11 | DOC 2 §2.4.1 had no call to dismiss a suggestion, but DOC 1 silence rule 6 needs one. | **New Pigeon calls `dismissSuggestion(kindId, subjectKey)` and `listMirrorWeeks()`** (past Mirrors). | F11; Pigeon Bridge |
| D12 | DOC 1 says "away" after "no valid days", but a study-hours block makes every day valid. | **A lapse is 3 or more consecutive days with no phone activity except Sakshi's own.** A paper-book day with a few messages is a real day, not a lapse. | F15 |
| D13 | DOC 1 F7 says the Lake "shows learning" on day 1 but not until when. | **Learning until the baseline freezes; no data until the first finalised window.** | F7 |

---

## Project Skeleton and Folder Structure

**Single Android module, no engine Gradle module (DOC 2 §2.1).** Start from `flutter create` and do not restructure what it generates. The paths below are exactly where code goes; AGENTS.md will quote them.

```
sakshi/                                   (flutter create sakshi --org com.kleos)
├── pubspec.yaml
├── pigeons/
│   └── sakshi_api.dart                  LOCKED CONTRACT LC-4 (Pigeon definition; the only hand-written API source)
├── lib/                                  PRESENTATION (Flutter). No logic, no database, no thresholds.
│   ├── main.dart
│   ├── app.dart                         MaterialApp + router mount
│   ├── gen/sakshi_api.g.dart            GENERATED by Pigeon. Never edited by hand.
│   ├── host/
│   │   ├── host_client.dart             abstract HostClient (1:1 with Pigeon methods) + HostException
│   │   ├── pigeon_host_client.dart      real implementation, wraps generated SakshiHostApi
│   │   ├── fake_host.dart               FakeHost implements HostClient, state machine over fixtures
│   │   └── fixtures/
│   │       ├── setup_fixtures.dart      (Track 3)
│   │       └── mirror_fixtures.dart     (Track 4)
│   ├── core/                            (Track 3 owns; see Flutter App Shell)
│   │   ├── theme.dart · router.dart · providers.dart · widgets/ (shared small widgets)
│   └── features/
│       ├── setup/        (Track 3)
│       ├── what_i_see/   (Track 3)
│       ├── settings/     (Track 3)
│       ├── mirror/       (Track 4)
│       ├── today/        (Track 4)
│       ├── lake/         (Track 4)  Lake painter used inside the app; widget art is separate
│       ├── shelf/        (Track 4)
│       └── demo/         (Track 4)
├── assets/                               (Flutter assets; UI only)
├── test/                                 Flutter widget and contract tests
│
└── android/app/src/
    ├── main/
    │   ├── AndroidManifest.xml          release manifest rules in DOC 2 §2.6
    │   ├── assets/sakshi/sayings.json   LOCKED CONTRACT LC-5 (bundled Saying shelf)
    │   ├── res/xml/lake_widget_info.xml · res/layout/lake_widget_*.xml · res/drawable/lake_*.xml
    │   └── kotlin/com/kleos/sakshi/
    │       ├── MainActivity.kt          registers the Pigeon host (the only Flutter-aware file)
    │       ├── host/                    HOST (Track 1)
    │       │   ├── gen/SakshiApi.g.kt          GENERATED by Pigeon
    │       │   ├── HostApiImpl.kt · Mappers.kt (engine views → DTOs)
    │       │   ├── AppContainer.kt           manual DI; the only place that wires ports to adapters
    │       │   ├── UsageEventsSource.kt · NotificationCollector.kt · IngestWorker.kt
    │       │   ├── LakeWidget.kt · WeeklyNoteNotifier.kt · PermissionGateway.kt
    │       │   ├── AppCatalogImpl.kt · SayingShelfImpl.kt · BatterySetup.kt · DemoController.kt
    │       ├── data/                    DATA (Track 1)
    │       │   ├── SakshiDatabase.kt · entities/*.kt · dao/*.kt
    │       │   ├── Room*Store.kt (one per engine port)
    │       │   └── Retention.kt · Exporter.kt
    │       └── engine/                  APPLICATION + DOMAIN (Track 2). NO `import android.`
    │           ├── SakshiEngine.kt              the façade (LC-3)
    │           ├── model/  Entities.kt · Values.kt · Views.kt
    │           ├── ports/  Ports.kt
    │           ├── tuning/ Tuning.kt
    │           ├── intervals/ ForegroundIntervals.kt
    │           ├── classify/ AppClassifier.kt
    │           ├── windows/ WindowFinder.kt · WindowLabeler.kt
    │           ├── stays/ StayDetector.kt · StoneWave.kt · Returns.kt
    │           ├── metrics/ Parts.kt · Steadiness.kt · DayMetrics.kt · WeekMetrics.kt
    │           ├── baseline/ BaselineService.kt
    │           ├── patterns/ PatternDetector.kt · Rhythm.kt · Trend.kt · Shift.kt · WindowShape.kt · BreakPoint.kt · CrossDay.kt · Clustering.kt · EvidenceGate.kt
    │           ├── suggestions/ SuggestionRule.kt · SuggestionSelector.kt · rules/S01ClearHour.kt … S12Win.kt
    │           ├── judging/ ExperimentJudge.kt
    │           ├── mirror/ MirrorBuilder.kt · TodayBuilder.kt · SentenceBuilder.kt · TeacherLeaves.kt · Lapse.kt · DataStates.kt
    │           ├── lake/ LakeBuilder.kt
    │           ├── note/ WeeklyNote.kt
    │           ├── privacy/ WhatISeeBuilder.kt · ExportBuilder.kt
    │           └── demo/ Personas.kt · EventSynthesizer.kt
    └── test/kotlin/com/kleos/sakshi/
        ├── engine/ (mirrors the engine tree) · fixtures/ (golden event streams, .json)
        ├── data/   (Room in-memory tests)
        └── arch/   DependencyRuleTest.kt · PrivacyBoundaryTest.kt · SayingShelfIntegrityTest.kt
```

```
SHARED SURFACES (collision points, named once, owned in DOC 4):
  pigeons/sakshi_api.dart            written by Track 1 only; read by Tracks 3 and 4
  engine/model/Entities.kt, Views.kt, ports/Ports.kt, tuning/Tuning.kt     Track 2 writes; Track 1 implements
  host/Mappers.kt                    Track 1 writes; breaks if Views.kt or the Pigeon file change
  lib/core/*, lib/host/*             Track 3 owns; Track 4 reads
  assets/sakshi/sayings.json         Track 4 owns; Track 1 loads it
```

**The one-sentence rule behind the tree:** a path says what may change it. `engine/` changes when the definition of focus changes, `host/` and `data/` when Android or the schema changes, `lib/` when the design changes.

**Dependency Rule test (DependencyRuleTest.kt).**
- Walk every `.kt` file under `engine/` and fail if any line matches `^import\s+(android\.|androidx\.|com\.kleos\.sakshi\.(host|data)\.|io\.flutter)`.
- Also fail if the engine reaches the clock or randomness directly: any occurrence of `System.currentTimeMillis`, `Instant.now`, `LocalDateTime.now`, `Random()` or `Math.random` in `engine/` outside `demo/EventSynthesizer.kt`. Time comes from the `Clock` port, randomness from the `Randomness` port.
- Failure message: "engine must not import android/host/data and must not read time or randomness directly".

```
TESTING PLAN (skeleton):
  Unit:        DependencyRuleTest; a build-config test that the release manifest has no INTERNET,
               no ACCESSIBILITY, no SYSTEM_ALERT_WINDOW, no FOREGROUND_SERVICE
  Integration: `flutter build apk --debug` and `./gradlew testDebugUnitTest` both pass on the empty skeleton
               (this is spike S-G; it comes first)
  E2E:         one Pigeon round trip (getSetupState) on a device
```

---

## Event Vocabulary and Tuning

This section is the one data shape collectors and engine must agree on (DOC 2 seam S-3), plus the single file of magic numbers.

### Value types (`engine/model/Values.kt`)

```kotlin
@JvmInline value class Pkg(val value: String)                 // Android package name, never empty
@JvmInline value class EpochMs(val value: Long)               // UTC milliseconds
@JvmInline value class Minutes(val value: Double)
@JvmInline value class Ratio(val value: Double)               // always clamped when used in Steadiness
@JvmInline value class StudyDay(val epochDay: Long)           // the local calendar date whose 04:00 starts it
@JvmInline value class WeekStart(val studyDay: StudyDay)      // Monday study day
```

`StudyDay.of(ts, zone)`: local time minus 4 hours, then the local date. Test: 2026-10-07 01:30 local belongs to study day 2026-10-06; 2026-10-07 04:00 local belongs to 2026-10-07. A week starts Monday 04:00 local.

### Facts (`engine/model/Entities.kt`; written only by the collectors)

```kotlin
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
```

### Tuning (`engine/tuning/Tuning.kt`): the single file of numbers

Every constant carries its DOC 1 origin. Names are frozen (they are a contract); values are tunable by the face-validity check (DOC 2 §2.8.3) without any other edit.

```kotlin
object Tuning {
  // time
  const val STUDY_DAY_START_HOUR = 4                      // confirmed
  const val RAW_RETENTION_DAYS = 14                       // DOC 2 §2.3.4
  const val WINDOW_FINALISE_LAG_MIN = 15                  // DOC 2 §2.3.3
  const val ASSUMED_PLATFORM_RETENTION_DAYS = 3           // ESTIMATE; replaced by spike S-B result
  // glance, stay, return
  const val GLANCE_MAX_SEC = 30                           // DOC 1 §1.2.3 (ESTIMATE)
  const val STAY_MERGE_GAP_SEC = 20                       // ESTIMATE
  const val RETURN_BACK_IN_SET_SEC = 30
  const val RETURN_SCREEN_OFF_MIN = 5
  const val PUT_DOWN_MIN = 3                              // D1
  const val DEPENDS_JOIN_SEC = 90                         // ASSUMPTION
  const val STONE_LOOKBACK_SEC = 30
  const val NO_RIPPLE_SEC = 60
  // windows
  const val RUN_MIN_IN_SET_MIN = 5                        // D9
  const val RUN_MAX_GAP_MIN = 3                           // D9
  const val WINDOW_PAD_MIN = 10
  const val VALID_DAY_WINDOW_MIN = 45
  const val RAMP_UP_MIN_RUN_MIN = 2
  const val FLINCH_FIRST_MIN = 5
  // baseline and steadiness
  const val BASELINE_VALID_DAYS = 8
  const val STAYS_SMOOTH_STAYS = 1.0                      // add-one smoothing: (stays+1)/(hours+1)
  const val STAYS_SMOOTH_HOURS = 1.0
  const val RATIO_CLAMP_LO = 0.5; const val RATIO_CLAMP_HI = 1.5
  const val W_STRETCH = 0.35; const val W_STAYS = 0.30; const val W_RETURN = 0.20; const val W_QUIET = 0.15
  const val WORD_WAVERING_BELOW = 90; const val WORD_STEADIER_ABOVE = 110
  const val UNUSUAL_WEEK_WINDOW_MIN_RATIO = 0.5           // week window-minutes < 50% of normal ⇒ unusual
  const val REANCHOR_MIN_WEEK = 4
  // evidence gate (patterns)
  const val EVIDENCE_MIN_WINDOWS = 4; const val EVIDENCE_MIN_DAYS = 3
  const val EVIDENCE_RATIO_HI = 1.5;  const val EVIDENCE_RATIO_LO = 0.67
  const val RATE_EPS = 0.5                                // stays/hour added to numerator and denominator in ratios
  const val SHAPE_MIN_WINDOWS = 30; const val BREAK_MIN_STAYS = 30
  const val PINGED_STONE_SHARE = 0.60
  const val TREND_FLAT_BAND = 0.05
  const val CUSUM_BASE_WEEKS = 3; const val CUSUM_K = 0.5; const val CUSUM_H = 4.0; const val CUSUM_SCALE_FLOOR = 0.10
  const val CLUSTER_MIN_WINDOWS = 40; const val CLUSTER_MIN_WEEK = 8; const val CLUSTER_MAX_K = 3
  const val CLUSTER_MIN_SILHOUETTE = 0.50; const val CLUSTER_SEED = 20261006L
  const val CLUSTER_CELL_SHARE = 0.70; const val CLUSTER_MIN_SIZE = 5
  const val CROSS_DAY_LATE_FROM_MIN = 30                  // 00:30 local
  const val CROSS_DAY_MIN_NIGHTS = 4; const val CROSS_DAY_SHORTER = 0.25
  // suggestions
  const val SUGGEST_MIN_VALID_DAYS = 8; const val SUGGEST_MIN_WINDOWS = 10
  const val SUGGEST_DISMISS_WEEKS = 4
  const val SUGGEST_EARLY_KINDS = "S1,S2,S4,S7"           // eligible from week 2
  const val SUGGEST_LATER_KINDS = "S3,S5,S6,S8,S9"        // eligible from week 4
  const val SUGGEST_SLOWDOWN_WEEK = 8                     // D7: then one per two weeks
  const val SUGGEST_SLOWDOWN_GAP_WEEKS = 2
  const val S1_SLOT_HOURS = 2; const val S1_MIN_WINDOWS = 3; const val S1_STAY_RATIO = 0.5; const val S1_STRETCH_RATIO = 1.5
  const val S2_SHARE = 0.35; const val S2_MIN_WINDOWS = 8
  const val S3_SHARE = 0.60; const val S4_SHARE = 0.60
  const val S5_RATIO = 1.5; const val S5_MIN_MIN = 8.0; const val S5_WEEKS = 2
  const val S6_SHARE = 0.50
  const val S7_COVERAGE = 0.25; const val S7_MIN_WINDOWS = 10
  const val S9_BAND_MIN = 5; const val S9_SHARE = 0.50; const val S9_MIN_WEEK = 4
  const val S10_MIN_GLANCES = 8; const val S10_GLANCE_PER_STAY = 8.0
  const val S12_IMPROVE = 0.15
  // judging
  const val JUDGE_DAYS = 14; const val JUDGE_MIN_WINDOWS = 8; const val JUDGE_MOVED = 0.20
  const val FOOTPRINT_DROP = 0.50
  // meter, lapse, lake, note
  const val OWN_OPEN_MERGE_SEC = 30
  const val LAPSE_MIN_DAYS = 3
  const val LAKE_CHOPPY_RATIO = 1.5; const val LAKE_MIN_WINDOWS = 3
  const val NOTE_HOUR_LOCAL = 8
  const val WORKSET_CAP = 12
  const val SAYING_OFFER_EVERY_WEEKS = 3
  const val GOAL_TAP_LAST_WEEK = 8                       // fades by about day 60
  const val PING_COVERAGE_PARTIAL = 0.70                 // F17
}
```

`[ASSUMPTION: S10's "many glances, few stays" had no number in DOC 1; I set at least 8 glances in the week and at least 8 glances per stay.]`

```
TESTING PLAN:
  Unit:        StudyDay.of at 03:59 / 04:00 / midnight crossing, in two zones (Asia/Kolkata and UTC);
               a test that Tuning contains no duplicate numeric meaning (a code-review checklist, not a test)
  Integration: PrivacyBoundaryTest builds a NotifEvent via reflection and fails if any field type is String
               other than pkg and category
```

---

## Collectors and Background Ingestion

```
FEATURE: Collectors and Background Ingestion

MODULE STRUCTURE:
  host/
    ├── UsageEventsSource.kt:     reads UsageStatsManager.queryEvents(begin, end) → List<RawEvent>. Maps Android
    │                             constants to RawType by NAME. Ignores every other event type. Keeps ALL packages,
    │                             including Sakshi's own (D6). One responsibility: translate, never judge.
    ├── NotificationCollector.kt: NotificationListenerService. onListenerConnected/Disconnected maintain
    │                             listener_session. onNotificationPosted/Removed append a NotifEvent. onBind guards
    │                             and calls requestRebind via PermissionGateway when asked.
    ├── IngestWorker.kt:          CoroutineWorker. One call: engine.processNewEvents(...). Reschedules nothing itself.
    ├── PermissionGateway.kt:     usage-access check via AppOpsManager (never a returned boolean from a dialog);
    │                             notification-listener enabled check; POST_NOTIFICATIONS check; deep links to settings pages.
    └── AppCatalogImpl.kt:        launcher apps via <queries>; ApplicationInfo.category; neutral-list lookup.
  data/
    ├── RoomEventStore.kt:        implements EventStore
    ├── RoomNotifStore.kt:        implements NotifStore + ListenerCoverage
    └── Retention.kt:             purge raw_event and notif_event older than 14 days; runs at the end of every ingest

SHARED SURFACES: Entities.kt (Track 2 wrote it, Track 1 reads it); the data_gap table (Track 1 writes, engine reads).

FUNCTION & CLASS DESIGN:
  UsageEventsSource.read(from: EpochMs, to: EpochMs): List<RawEvent>
      Single pass over UsageEvents; for each Event, `when (type)` on the six supported constants, else skip.
  IngestOrchestrator (host/IngestWorker.kt, plain function, no Android types):
      fun runIngest(deps, asOf): IngestReport
        1. state = ingestState.read(); if paused → append nothing, return Paused
        2. from = state.cursor ?: (asOf − ASSUMED_PLATFORM_RETENTION_DAYS days − 1 day); to = asOf
        3. events = source.read(from − OVERLAP_MARGIN_MIN(10 min), to); eventStore.append(events) (dedupe by unique key)
        4. gap check: if state.lastRunAt != null and asOf − state.lastRunAt > ASSUMED_PLATFORM_RETENTION_DAYS days
           → dataGaps.add(NOT_SEEN, state.lastRunAt, asOf − retention)
        5. cursor = max(ts of events) − OVERLAP_MARGIN_MIN; lastRunAt = asOf; workerRuns7d updated
        6. engine.processNewEvents(asOf)   (derive, baseline, patterns, lake, note decision)
        7. host: LakeWidget.refresh(); WeeklyNoteNotifier.maybePost(decision)
        8. Retention.purge(asOf)
  The 10-minute overlap margin exists because queryEvents "may omit the last few minutes"; re-reading the
  overlap is safe because of the unique key (ts, type, pkg).

INTERFACES & CONTRACTS:
  Ports consumed (defined in engine/ports/Ports.kt): EventStore, NotifStore, ListenerCoverage, StateStore, Clock.
  Pigeon calls that trigger it: syncNow(); the periodic job; the expedited on-open job.
  Promise to the engine: events arrive sorted by ts ascending within a read, deduped, typed.
  Kept private: Android event constant values, AppOps details, work request names.

ERROR HANDLING STRATEGY:
  SecurityException from queryEvents (usage access revoked): return IngestReport.NoPermission; the engine keeps
  derived data; SetupStateDto.usageAccessGranted goes false; no crash, no zero-fill.
  Any other exception: record `last_error` (a short code, no package names) in ingest_state and return Result.success();
  the next scheduled run is the retry (never Result.retry, which would loop on a persistent failure).
  NotificationCollector never throws out of a callback; it catches, increments a local error counter, drops the event.

EDGE CASES TO HANDLE:
  - App Standby bucket delays the job for hours: catch-up handles it (the engine re-derives from raw events).
  - Process killed after step 3, before step 5: the next run re-reads the overlap; unique key swallows duplicates.
  - Clock set backward: events with ts in the future of asOf are kept but windows containing them are flagged
    unusual by the engine; never deleted.
  - Notification posted for Sakshi's own package or a group-summary/ongoing notification: stored with ongoing=true
    or skipped for own package; the engine ignores ongoing.
  - Listener disconnects during a callback: close session with disconnectedAt = now; a crash leaves it open, and
    the next onListenerConnected closes any stale open session at the last known notification ts.
  - Pause is switched on mid-run: honoured at the next run; the pause starts a data_gap(PAUSED) at the pause tap.

PERFORMANCE CONSIDERATIONS:
  One job reads a bounded range; Room appends in a single transaction. 10,000 raw events/day is the planning
  ceiling (DOC 2 §2.0.3, spike S-B). No coroutine fan-out. Do not optimise the foreground-interval pass.

TESTING PLAN:
  Unit (host-free):  runIngest against FakeUsageSource + in-memory stores: first run (no cursor), repeated run
                     (no duplicates), run after a long gap (NOT_SEEN gap written), paused run (nothing appended).
  Integration:       RoomEventStore unique-key dedupe; Retention purges at exactly 14 days.
  Privacy:           PrivacyBoundaryTest posts a fake StatusBarNotification-shaped object carrying title and text;
                     the stored NotifEvent has neither (there is no field), and a reflection check on the entity.
  Device:            spikes S-A, S-B, S-C, S-D.
```

```
LOCKED CONTRACT detail — Android event name map (host/UsageEventsSource.kt):
  UsageEvents.Event.ACTIVITY_RESUMED → RawType.ACTIVITY_RESUMED     ACTIVITY_PAUSED → ACTIVITY_PAUSED
  SCREEN_INTERACTIVE → SCREEN_INTERACTIVE                           SCREEN_NON_INTERACTIVE → SCREEN_NON_INTERACTIVE
  KEYGUARD_SHOWN → KEYGUARD_SHOWN                                   KEYGUARD_HIDDEN → KEYGUARD_HIDDEN
  Stored by NAME, never by the integer constant (constants differ across versions).
```

---

## Foreground Intervals and App Classification

This is the deepest module and the one most likely to be wrong on a real phone; DOC 2 hid it behind one function so a fix touches one file.

```
FEATURE: Foreground Intervals and App Classification

MODULE STRUCTURE:
  engine/intervals/
    └── ForegroundIntervals.kt:  fun reconstruct(events: List<RawEvent>, asOf: EpochMs): Reconstruction
  engine/classify/
    └── AppClassifier.kt:        class AppClassifier(userClasses: Map<Pkg, UserClass>, catalog: AppCatalog)
                                   fun classify(pkg: Pkg): AppClass            // IN_SET | DEPENDS | NEUTRAL | OFF_SET
  engine/model/
    └── Entities.kt:             ForegroundInterval, ScreenSpan, Reconstruction, AppClass

SHARED SURFACES: None (internal), except `AppCatalog` (port, implemented by host).

FUNCTION & CLASS DESIGN:
  data class ForegroundInterval(val pkg: Pkg, val start: EpochMs, val end: EpochMs)
  data class ScreenSpan(val start: EpochMs, val end: EpochMs?)        // a screen-off (non-interactive) span
  data class Reconstruction(val intervals: List<ForegroundInterval>, val screenOff: List<ScreenSpan>, val openEnded: Boolean)

  reconstruct() rules, in this order (a state machine over events sorted by ts, ties by this type order:
  PAUSED, NON_INTERACTIVE, KEYGUARD_SHOWN, then RESUMED, INTERACTIVE, KEYGUARD_HIDDEN):
    R1 ACTIVITY_RESUMED(p): if another interval is open, close it at ts; open (p, ts).
    R2 ACTIVITY_PAUSED(p): if the open interval is p, close it at ts. If it is a different package, ignore.
    R3 SCREEN_NON_INTERACTIVE: close any open interval at ts; open a ScreenSpan at ts.
    R4 SCREEN_INTERACTIVE: close the open ScreenSpan at ts.
    R5 KEYGUARD_SHOWN: close any open interval at ts (the lock screen is not an app).
    R6 KEYGUARD_HIDDEN: no effect by itself (the next RESUMED opens an interval).
    R7 Intervals shorter than 1 second are dropped.
    R8 At the end of the input an interval or span still open is closed at min(asOf, lastEventTs + 10 min)
       and Reconstruction.openEnded = true (so a caller knows its tail is provisional).
    R9 Two consecutive intervals of the same package with a gap under 2 seconds are joined (activity transitions
       inside one app).
  classify():
    1. user class IN_SET or DEPENDS if the user set it (set at setup, F2)
    2. NEUTRAL if pkg is in the bundled neutral list or is the default dialer or launcher (AppCatalog)
    3. else OFF_SET
  Depends resolution lives in StayDetector (it needs time context), not here.

INTERFACES & CONTRACTS:
  Input is the LC-1 vocabulary; output types above are engine-internal but covered by golden fixtures.
  Promise: intervals never overlap; no interval spans a screen-off span; sorted by start.

ERROR HANDLING STRATEGY:
  Malformed sequences (PAUSED without RESUMED, double RESUMED) are tolerated by R1 and R2; the function never
  throws. A counter `anomalies` is returned in Reconstruction for the What I see page ("odd event pairs: n").

EDGE CASES TO HANDLE (each is a golden fixture, see Testing):
  - Split-screen: two RESUMED without a PAUSED between → R1 closes the first at the second's start. [VERIFY ON DEVICE: S-A]
  - Picture-in-picture: the player package keeps RESUMED while another app is open → R1 closes it when the other app resumes. Accepted loss: the PiP time is attributed to the app in front.
  - Lock during use: NON_INTERACTIVE then KEYGUARD_SHOWN at nearly the same ts → both close; once.
  - Always-on display or glance screen waking: INTERACTIVE for under 5 seconds with no RESUMED → a screen-on blip with no interval; ignored by stays, counted by pickups only if KEYGUARD_HIDDEN follows.
  - Events out of order within the same millisecond: the tie order above.
  - Interval reaching past a day boundary: kept whole; the window logic assigns the day.

PERFORMANCE CONSIDERATIONS:
  O(n) over sorted events. Re-run per recompute for the affected days only (the caller passes the day's events plus
  60 minutes of lead-in so an interval opened before the day boundary is seen).

TESTING PLAN:
  Unit (golden examples, local JUnit; times as HH:MM:SS on 2026-10-06):
    G-I1  10:00:00 RES A; 10:05:00 PAUSED A; 10:05:00 RES B; 10:07:00 NON_INTERACTIVE
          ⇒ intervals [A 10:00:00–10:05:00], [B 10:05:00–10:07:00]; screenOff open at 10:07:00.
    G-I2  10:00:00 RES A; 10:00:00.5 PAUSED A ⇒ no interval (R7).
    G-I3  10:00:00 RES A; 10:03:00 RES B (no PAUSED A) ⇒ [A 10:00–10:03], B open to the end (openEnded = true).
    G-I4  10:00:00 RES A; 10:02:00 KEYGUARD_SHOWN; 10:02:00 NON_INTERACTIVE ⇒ [A 10:00–10:02] once.
    G-I5  RES A 10:00:00; PAUSED A 10:01:00; RES A 10:01:01 ⇒ one joined interval A 10:00:00–(next close) (R9).
  Property: for any shuffled copy of an event list with distinct timestamps (sorted internally), the
            Reconstruction is identical; no two intervals overlap; every interval end ≥ start.
  Integration: S-A device dump of a scripted 5-minute sequence is saved as fixtures/real_s_a.json and replayed.
```

---

## F1. Instant First Look

```
FEATURE: F1 Instant First Look

MODULE STRUCTURE:
  engine/
    ├── SakshiEngine.kt:         fun processNewEvents(asOf): ProcessReport   (shared with every feature)
    └── mirror/MirrorBuilder.kt: builds a MirrorView with provisional=true when no baseline exists; the First Look
                                 is that provisional Mirror over the retained days (no separate screen model)
  host/HostApiImpl.kt:           syncNow(), getMirror(null)
  lib/features/setup/first_look_screen.dart       (Track 3: the screen shown right after usage access is granted)

SHARED SURFACES: MirrorDto and SyncStatusDto (Pigeon); lib/features/mirror/ widgets are reused by first_look_screen
  (Track 3 imports the Mirror parts widget from Track 4: a named dependency, see Flutter App Shell).

FUNCTION & CLASS DESIGN:
  SakshiEngine.processNewEvents(asOf):
     1. EventStore.range(oldest, asOf) → days touched since the last derive
     2. for each touched study day: RecomputeDay (reconstruct → classify → windows → stays → stone/wave → metrics)
     3. FinalizeWindows (a window ends ≥ 15 min before asOf ⇒ finalised=true)
     4. UpdateBaseline → DetectPatterns → UpdateSuggestionState → JudgeExperiments → BuildLake → DecideWeeklyNote
     Return ProcessReport(daysRecomputed, newWindows, baselineFrozen, lakeChanged, noteDecision).
  First Look = MirrorBuilder.build(week = the most recent days, provisional = true). No Steadiness, no suggestion,
  no pattern lines; the four parts with plain words and the in-set versus quiet split; Lake state = learning.

INTERFACES & CONTRACTS:
  getMirror(null) on a fresh install returns MirrorDto{ provisional: true, dataState: learningBaseline, steadiness: null,
  suggestion: null, headline: "Your last few days: …" }.

ERROR HANDLING STRATEGY:
  If usage access is not granted: SetupStateDto says so; there is no First Look and no error. If granted but the
  platform returned no events: dataState = tooLittleData and the sentence says so (F17).

EDGE CASES TO HANDLE:
  - A freshly reset phone (little or no retained history): First Look says "Not much history yet"; the Time Machine is the demo (DOC 1 §1.4.4).
  - Retained days exist but fewer than 45 window-minutes each: invalid days; the four parts are shown only if at least one window exists.
  - Retained days are not counted toward the baseline (D2): First Look shows them; baseline starts at first_read_at's study day.

PERFORMANCE CONSIDERATIONS:
  First read recomputes up to about four days in one pass; target a few seconds on the iQOO Z7. Show a progress state
  in Flutter while syncNow() is pending; never block the UI thread (the host call is async).

TESTING PLAN:
  Unit:        golden fixture "four retained days, no baseline" ⇒ provisional Mirror with parts present, steadiness null
  Integration: Room-backed processNewEvents over fixtures/first_look.json produces the same MirrorView as the in-memory run
  E2E:         S-A/S-B on a real phone: grant usage access, wait for the First Look, compare with Android's own Digital Wellbeing for plausibility (face validity)
```

---

## F2. Work-Set Setup

```
FEATURE: F2 Work-Set Setup

MODULE STRUCTURE:
  engine/
    ├── ports/Ports.kt:           AppCatalog { fun launcherApps(): List<AppInfo>; fun category(pkg): Int?; fun isNeutral(pkg): Boolean }
    ├── classify/WorkSetPolicy.kt: fun validate(entries: List<WorkSetEntry>): ValidationResult   (cap 12; Depends counts toward the cap)
    └── usecases/SaveWorkSet.kt:  persists AppMeta rows, triggers a recompute of retained days
  host/AppCatalogImpl.kt:         PackageManager launcher query via <queries>; category from ApplicationInfo.category (API 26+)
  lib/features/setup/work_set_screen.dart   (Track 3)

SHARED SURFACES: Pigeon: AppDto, WorkSetEntryDto, SaveResultDto.

FUNCTION & CLASS DESIGN:
  suggestPreticks(apps: List<AppInfo>): List<AppSuggestion>   // in engine, pure
     pre-tick IN_SET only when ApplicationInfo.category is PRODUCTIVITY [ASSUMPTION: education-adjacent categories are not
     reliably reported by Android, so they are not pre-ticked];
     pre-mark DEPENDS for a bundled dual-use list: YouTube, Chrome, WhatsApp, Telegram (DOC 1 §1.2.3)
     a convenience, not a classifier; the user changes everything with taps
  WorkSetPolicy.validate: reject count of IN_SET + DEPENDS > 12 with a USER-facing message from a fixed set
  SaveWorkSet: replace all AppMeta rows with user_class != NONE; trigger engine.processNewEvents (history re-derives)

INTERFACES & CONTRACTS:
  listLauncherApps(): List<AppDto>; saveWorkSet(List<WorkSetEntryDto>): SaveResultDto. The cap is enforced in the engine,
  not only the UI (the UI shows a counter but never owns the rule).

ERROR HANDLING STRATEGY:
  Over the cap → SaveResultDto{ok:false, userMessage:"Pick up to 12. Fewer is better."}, nothing saved. Unknown package
  (uninstalled since listing) → dropped silently with a count in the result message, never a crash.

EDGE CASES TO HANDLE:
  - An app in usage events but not in the launcher list (work profile, a hidden system app): package name is the label.
  - Zero apps selected: allowed but windows can only come from study hours; the setup screen says plainly that Sakshi will see very little.
  - The user marks an app both IN_SET and neutral by default (e.g. calculator): the user choice wins.
  - Re-editing the work-set later: history is re-derived under the new set; the baseline is NOT recomputed (it stays frozen); a banner says "your starting normal used your earlier set" [ASSUMPTION].

PERFORMANCE CONSIDERATIONS:
  Launcher query returns a few hundred apps; label lookup is the slow part; do it once on a background thread.

TESTING PLAN:
  Unit:        validate(12 ok, 13 rejected, Depends counted); suggestPreticks on a fixture list
  Integration: SaveWorkSet then processNewEvents changes classification of a fixture day
  E2E:         setup flow on the Nothing Phone 3a: pick 12, save, confirm What I see shows the set size
```

---

## F3. Window Finder

```
FEATURE: F3 Window Finder

MODULE STRUCTURE:
  engine/windows/
    ├── WindowFinder.kt:   fun find(day: StudyDay, intervals, classes, studyHours, settings): List<Window>
    └── WindowLabeler.kt:  labels Held/Pinged/Reached (used by P4, see Pattern Layer)

SHARED SURFACES: Window entity in Entities.kt.

FUNCTION & CLASS DESIGN:
  find():
    1. studyBlocks(day): each StudyHoursDto block (minutes-from-midnight start and end, end < start means crossing
       midnight) placed on the study day's calendar; if learn_study_hours is on and ≥ 14 inferred windows exist, derive
       blocks from the median start and end of inferred windows (a "learned" block) — until then, inferred runs only.
    2. runs(): chain effective-in-set intervals (Depends resolution happens first, see Stays) where each gap ≤ RUN_MAX_GAP_MIN (3 min);
       keep chains with total in-set time ≥ RUN_MIN_IN_SET_MIN (5 min); extend each by WINDOW_PAD_MIN (10 min) on both sides.
    3. union the blocks and the padded runs; merge overlapping or touching; source = STUDY_HOURS | INFERRED | BOTH.
    4. a window is attributed to the study day it STARTED in (even if it crosses 04:00).
    5. finalised = (end + 15 min ≤ asOf). A window that overlaps a data_gap is marked partial = true.
  A day is VALID if its non-partial window minutes ≥ VALID_DAY_WINDOW_MIN (45).

INTERFACES & CONTRACTS:
  data class Window(val id, day, start, end, source, partial, finalised, shape: Shape?) — Shape = HELD | PINGED | REACHED.

ERROR HANDLING STRATEGY: pure function; never throws. A day with no data returns an empty list, not an error.

EDGE CASES TO HANDLE:
  - Study hours crossing midnight (22:00–01:30): one window attributed to the day it began.
  - A run that starts during study hours: BOTH.
  - Midnight reel sessions with no in-set run and outside study hours: no window ⇒ not scored (the point of DOC 1 §1.2.3).
  - Windows overlapping a data gap: partial, excluded from baseline and trends.
  - Two runs separated by exactly 3 minutes: one chain; by 3 minutes 1 second: two.

PERFORMANCE CONSIDERATIONS: linear in intervals; no memoisation.

TESTING PLAN:
  Unit (golden):
    G-W1  study 20:00–23:00; in-set run 22:50–23:30 ⇒ one window 20:00–23:40, source BOTH.
    G-W2  no study hours; in-set runs 21:00–21:04 (4 min) ⇒ no window; 21:00–21:05 ⇒ window 20:50–21:15, INFERRED.
    G-W3  in-set runs 21:00–21:10 and 21:13–21:20 (gap exactly 3 min) ⇒ one chain, window 20:50–21:30.
          in-set runs 21:00–21:10 and 21:50–22:00 (gap 40 min) ⇒ two windows, 20:50–21:20 and 21:40–22:10.
    G-W4  valid-day check: windows totalling 44 min ⇒ invalid; 45 ⇒ valid.
  Property: windows never overlap; every window.start < window.end.
```

---

## F4. Stone and Wave

```
FEATURE: F4 Stone and Wave

MODULE STRUCTURE:
  engine/stays/
    ├── StayDetector.kt:  glances, stays, merges, Depends resolution, return time
    ├── StoneWave.kt:     origin classification against NotifStore and ListenerCoverage
    └── Returns.kt:       return computation (back-in-set or put-down)

SHARED SURFACES: Stay entity; ListenerCoverage port.

FUNCTION & CLASS DESIGN:
  StayDetector.detect(window, intervals, classes, screenOff): StayResult(glances, stays)
    Depends resolution first (D: DEPENDS_JOIN_SEC = 90): a DEPENDS interval is effectively IN_SET when its start is
      within 90 s after the end of an effectively in-set interval (neutral intervals in between are transparent), or is
      contiguous (gap ≤ 90 s) with an effectively in-set DEPENDS interval; otherwise it is OFF_SET.
    Departure runs: maximal runs of OFF_SET time inside the window (neutral time neither starts nor breaks one;
      screen-off time ends a run). Run length < 30 s ⇒ glance; ≥ 30 s ⇒ candidate stay.
    Merge: two candidate/glance runs with a gap < 20 s join into one run BEFORE the 30 s test.
    A stay: start, end, first_pkg (first off-set package of the run), pkg_main (the package with the most time in the run), glances_before.
  Returns.returnMinutes(stay, intervals, screenOff): 
    return = time from stay.start to the first moment the user is effectively in-set for ≥ 30 s, OR to the start of a screen-off
    span lasting ≥ 5 min (a put-down), whichever is first; null if neither happens before window end ("unresolved"; excluded from the median).
  StoneWave.classify(stay, notifs, coverage): Origin
    covered = coverage.coversInterval(stay.start − 30 s, stay.start)
    if !covered ⇒ UNKNOWN
    else ping = a POSTED, non-ongoing NotifEvent with pkg == stay.first_pkg and ts in [stay.start − 30 s, stay.start] ⇒ STONE
         (clicked = a REMOVED/CLICK event for that notification key within 60 s) else SELF_STARTED.
  no-ripple (separate output): for every non-ongoing POSTED notification inside a window from a non-neutral package, whether a stay began
    within 60 s (any app). Rate = notifications without a stay ÷ notifications; needs ≥ 1 covered notification.

INTERFACES & CONTRACTS:
  enum class Origin { STONE, SELF_STARTED, UNKNOWN }
  data class Stay(id, windowId, start, end, firstPkg, pkgMain, origin, stonePkg: Pkg?, notifClicked: Boolean, returnMinutes: Double?, glancesBefore: Int)

ERROR HANDLING STRATEGY: pure. Missing notification data never throws; it yields UNKNOWN.

EDGE CASES TO HANDLE:
  - Ping from app B, then the user opens app C (via Home): SELF_STARTED, not STONE (DOC 1: the app the user then opened).
  - Ping from Sakshi's own package or ongoing (music): ignored.
  - Listener died 2 days ago and restarted: stays in the gap are UNKNOWN; stays after reconnect are classified.
  - Multiple pings within 30 s: the one from first_pkg decides; the stone_pkg is that package.
  - Return unresolved at window end: excluded from Return median, counted in a "never came back inside the window" tally for the Mirror's honest line.

PERFORMANCE CONSIDERATIONS: notification lookup by binary search on a per-package sorted list; do not scan the whole log per stay.

TESTING PLAN:
  Unit (golden):
    G-S1  in-set A throughout; off-set B 10:10:00–10:10:29 ⇒ glance; B 10:10:00–10:10:30 ⇒ stay.
    G-S2  B 10:10:00–10:10:40 then C 10:10:55–10:11:30 (gap 15 s) ⇒ one stay 10:10:00–10:11:30, first_pkg B, pkg_main B (40 s vs 35 s).
    G-S3  same with gap 21 s ⇒ two separate runs: B is a stay (40 s), C is a stay (35 s).
    G-S4  stay starts 10:10:00 (B); POSTED(B) at 10:09:45 ⇒ STONE; at 10:09:20 ⇒ SELF_STARTED; REMOVED/CLICK at 10:09:58 ⇒ clicked.
    G-S5  same as G-S4 but listener coverage has a hole over 10:09:30–10:09:50 ⇒ UNKNOWN.
    G-S6  return, case (a): stay at 10:10:00; back in A at 10:14:00 and holding 40 s ⇒ return 4.0 min.
          return, case (b): stay at 10:10:00; screen off 10:12:00–10:20:00 ⇒ return 2.0 min (put-down at 10:12:00, span ≥ 5 min).
  Property: stays never overlap; a stay lies inside one window; origin is UNKNOWN whenever coverage is false.
```

---

## F5. Steadiness and Its Four Parts

```
FEATURE: F5 Steadiness and Its Four Parts

MODULE STRUCTURE:
  engine/metrics/
    ├── Parts.kt:        data class Parts(stretchMin, staysPerHour, returnMin, quietShare, …extras)
    ├── DayMetrics.kt:   per-day summary from windows, stretches, stays
    ├── WeekMetrics.kt:  pooled week values (the only place pooling is defined)
    └── Steadiness.kt:   pure function steadiness(parts, baseline) → Steadiness(value:Int, word: Word)
  engine/baseline/BaselineService.kt: freeze, re-anchor, unusual-week flag

SHARED SURFACES: Baseline and WeekSummary entities.

FUNCTION & CLASS DESIGN:
  Stretches (inside StayDetector's caller, `StretchBuilder`): inside a window, a stretch runs from window start or the end of the
    previous stay until the next stay start, the window end, or a PUT_DOWN. Quiet time joins the stretch it follows (D1); a quiet
    span of ≥ 3 min ends the stretch as PUT_DOWN and the next in-set use starts a new stretch. Stretch minutes = in-set minutes + quiet minutes
    (glance time and neutral-app time count as holding). A stretch's `endedBy` = STAY | PUT_DOWN | WINDOW_END.
  Pooling for a week (WeekMetrics): all non-partial windows with study day in the week, from valid days only.
    stretchMin  C = length-weighted median: sort stretches by length descending; add lengths until the running total ≥ 50 % of
                 the total stretch minutes; C = the length at which that happens ("half your time came in stretches this long or longer").
    longestStretch = max length.
    staysPerHour P = (stayCount + 1) / (windowHours + 1)     [smoothing; Tuning.STAYS_SMOOTH_*]
    returnMin R = median of returnMinutes over stays with a return.
    quietShare Q = quiet minutes ÷ window minutes.
    inSetShare   = in-set minutes ÷ window minutes (shown beside Stretch).
    extras (shown, not scored): switches per window-hour, ramp-up minutes, flinch rate, no-ripple rate, work-set coverage, endings (put-down vs pull), glance count.
  Steadiness(parts, base):
    s = clamp(C/C0) ^0.35 × clamp(P0/P) ^0.30 × clamp(R0/R) ^0.20 × clamp(Q/Q0) ^0.15, clamp to [0.5, 1.5]; value = round(100 × s).
    word: value < 90 Wavering; 90–110 Steady; > 110 Steadier. (Boundaries: 90 and 110 are Steady.)
    if any of the four parts is missing (e.g. no returns) the number is NOT computed (null); the Mirror says "too little data" for that part.
  BaselineService:
    freeze: the earliest BASELINE_VALID_DAYS (8) valid days with study day ≥ day(first_read_at) (D2); computed with the SAME pooling code;
            written once as Baseline(is_active = true, frozen_at = end of the 8th valid day). Never overwritten.
    reanchor(): allowed once, at week ≥ 4; creates a new Baseline from the 8 most recent valid days, the old one is_active = false.
    unusualWeek(week): week's non-partial window minutes < 50 % of the baseline's mean weekly window minutes ⇒ unusual (excluded from trend and judging).

INTERFACES & CONTRACTS:
  data class Parts(val stretchMin: Double?, val longestStretchMin: Double?, val staysPerHour: Double?, val returnMin: Double?,
                   val quietShare: Double?, val inSetShare: Double?, val extras: Extras)
  data class Baseline(val id, frozenAt, c0, p0, r0, q0, daysUsed, isActive)
  data class Steadiness(val value: Int, val word: Word)

ERROR HANDLING STRATEGY: nullable parts, never zeros. Division by a zero baseline value is impossible (baseline creation requires all four > 0; otherwise the baseline is not frozen and the Mirror says "learning your normal" with a reason).

EDGE CASES TO HANDLE:
  - Baseline from fewer than 8 valid days exists only after 8; before that Steadiness is null.
  - A student with zero returns (never leaves): return part missing ⇒ no number; the Mirror shows the other parts.
  - A week with 1 window: parts exist but the Mirror flags tooLittleData (< 4 windows) [ASSUMPTION].
  - Quiet-only windows (paper-book student): one long stretch, zero stays, return missing ⇒ no number but a clear honest line (F17).
  - Reanchor requested before week 4 or twice: rejected with a typed result.

PERFORMANCE CONSIDERATIONS: week metrics recomputed on demand from stored stretches/stays; trivial.

TESTING PLAN:
  Unit (golden, from DOC 1 §1.2.4):
    G-F1  baseline (C0 10, stays 4.0, R0 6, Q0 0.36); week (12, 3.5, 5, 0.38) ⇒ ratios 1.20, 1.143, 1.20, 1.056 ⇒ value 116, word Steadier.
    G-F2  week (8, 5.5, 9, 0.30) ⇒ ratios 0.80, 0.727, 0.667, 0.833 ⇒ value 75, word Wavering.
    G-F3  clamp: week stretch ratio 3.0 ⇒ treated as 1.5; ratio 0.2 ⇒ 0.5.
    G-F4  boundaries: value 90 ⇒ Steady; 110 ⇒ Steady; 111 ⇒ Steadier; 89 ⇒ Wavering.
    G-F5  weighted median: stretches [30,10,10,10,10] ⇒ C = 10 (30/70 = 43 % < 50 %, adding one 10 gives 57 %).
    G-F6  smoothing: 0 stays over 5 window-hours ⇒ P = 1/6 = 0.167; 6 stays over 5 hours ⇒ 7/6 = 1.167.
    G-F7  determinism: steadiness(parts, base) twice ⇒ identical; baseline row never changes after freeze.
  Property: Steadiness is monotone: improving any single part (others fixed) never lowers the value; value equals 100 when parts equal the baseline.
  Persona replay: Aarav's weeks (see Time Machine) ⇒ ~100 at baseline, ~116 at week 4, ~136 at week 8 (tolerance ±6 each).
  Face validity (manual, DOC 2 §2.8.3): fix by editing Tuning only.
```

---

## Pattern Layer (P1 to P5, Cross-Day Link, Clustering)

This is the chosen Technical Differentiator (DOC 1 §1.2.6). Nothing here reacts to a single event. A pattern needs recurrence, and a suggestion needs a pattern. Every detector is deterministic.

```
FEATURE: Pattern Layer

MODULE STRUCTURE:
  engine/patterns/
    ├── PatternDetector.kt:  interface PatternDetector { val kind: PatternKind; fun detect(ctx: PatternContext): List<Pattern> }
    ├── EvidenceGate.kt:     object EvidenceGate { fun passes(windows: Int, days: Int, ratio: Double): Boolean }
    ├── Rhythm.kt            P1: stay rate by day-part × weekday/weekend (8 cells)
    ├── Trend.kt             P2: direction of each part over 4 weekly values
    ├── Shift.kt             P3: CUSUM change detector on each weekly part series
    ├── WindowShape.kt       P4: Held / Pinged / Reached (also used by WindowLabeler)
    ├── BreakPoint.kt        P5: where stretches end, and what usually came just before a stay
    ├── CrossDay.kt          last screen-off vs next day's first stretch (the S8 source)
    ├── Clustering.kt        optional seeded k-means over window vectors
    └── PatternEngine.kt:    runs every registered detector, upserts Pattern rows, retires stale ones
  engine/model/Entities.kt:  Pattern, PatternKind, PatternContext

SHARED SURFACES: Pattern entity (Entities.kt); the Mirror's pattern lines (via SentenceBuilder).

FUNCTION & CLASS DESIGN:
  PatternContext(windows: List<WindowWithDetail>, weeks: List<WeekSummary>, days: List<DaySummary>, baseline: Baseline?, asOf, random: Randomness)
  data class Pattern(val kind: PatternKind, val key: String, val strength: Double, val evidenceWindows: Int, val evidenceDays: Int,
                     val firstSeen: EpochMs, val lastSeen: EpochMs, val args: Map<String, String>)
  enum class PatternKind { RHYTHM, TREND, SHIFT, SHAPE, BREAK_POINT, CROSS_DAY }
  EvidenceGate.passes(windows, days, ratio) = windows ≥ 4 && days ≥ 3 && (ratio ≥ 1.5 || ratio ≤ 0.67)
  PatternEngine.run(ctx): for each detector, results are upserted by (kind, key); a pattern that no run re-detects for 14 days is
     marked retired and no longer shown. Retired patterns are kept so first_seen survives a pattern coming back.
  Windows used by every detector: non-partial, finalised, in valid days. Weeks used: non-unusual.

  P1 Rhythm (cells)
    dayPart by the window's START local hour: MORNING 04–12, AFTERNOON 12–17, EVENING 17–22, NIGHT 22–04.
    cell = (dayPart, WEEKDAY|WEEKEND) by the study day. Per-window rate = stays ÷ window hours (windows under 20 minutes are skipped).
    overall = median of per-window rates over all windows. cellValue = median over the cell's windows.
    ratio = (cellValue + RATE_EPS) ÷ (overall + RATE_EPS). Pattern exists when EvidenceGate.passes(cellWindows, cellDays, ratio).
    key = "cell:NIGHT-WEEKDAY"; args = { cell, cellRate, overallRate, direction: CHOPPY|CLEAR }.
    Also: the clear hour (for S1): over 2-hour local slots, the slot with stays/hour ≤ 0.5 × overall AND stretch ≥ 1.5 × overall stretch
    across ≥ 3 windows; key = "clear:21-23".
  P2 Trend (per part: stretch, stays, return, quiet)
    values = the last 4 weekly values excluding unusual and partial-data weeks (need 4). run3 = the three week-to-week changes all same sign,
    each at least 5 % relative. direction = compare median(last 2) with median(prior 2): within ±5 % ⇒ FLAT; else BETTER or WORSE using each part's
    better-direction (higher stretch/quiet, lower stays/return). Pattern exists if direction ≠ FLAT. args = { part, values, direction, run3 }.
  P3 Shift (per part; CUSUM)
    series x_0..x_n-1 (n ≥ 4, unusual weeks removed). μ = median of the first 3 values; s = max(MAD of the first 3, 0.10 × μ);
    k = 0.5 s; h = 4 s. Upper S⁺_t = max(0, S⁺_{t−1} + (x_t − μ − k)); lower S⁻_t = max(0, S⁻_{t−1} + (μ − x_t − k)).
    Alarm at the first t > 2 where S⁺ > h or S⁻ > h. Persistent if the mean of x from t onward differs from μ by ≥ 2k. Pattern = SHIFT{part, weekIndex, direction}.
    A shift reported in the Mirror only if the alarm week is within the last 4 weeks.
  P4 Window shape
    Held   : stays/hour ≤ the user's own median AND stretch ≥ the user's own median; or the window has zero stays.
    else Pinged : stone share ≥ 60 % among stays with a KNOWN origin (needs ≥ 2 known-origin stays); else Reached.
    If fewer than 2 known-origin stays and not Held: shape = null (not labelled).
    Pattern exists at ≥ 30 labelled windows (SHAPE_MIN_WINDOWS); key = "mix"; args = { held, pinged, reached, reachedStartsAfter: "22:30" or null }.
    reachedStartsAfter = the earliest local clock time after which ≥ 70 % of Reached windows started, if ≥ 3 Reached windows (rounded to 30 min).
  P5 Break point
    stretchEndings = all stretches ended by STAY or PUT_DOWN. bands = 5-minute bins of stretch length. topBand share = windows' endings in the top bin ÷ all endings.
    Exists when stays ≥ 30 (BREAK_MIN_STAYS) and topBandShare ≥ 0.50; args = { bandStartMin, bandShare, cause }.
    cause: among stays with known origin, STONE share ≥ 0.50 and one package ≥ 50 % of the stones ⇒ "PING:<pkg>"; SELF_STARTED share ≥ 0.50 ⇒ "SELF"; else "MIXED".
  CrossDay
    A night is LATE when the last SCREEN_NON_INTERACTIVE before the next study day's 04:00 is at or after 00:30 local. Qualifying night = LATE and the next day valid
    with a first stretch. Over the last 14 days: if ≥ 4 qualifying nights and their mean first stretch ≤ 0.75 × the mean first stretch of non-late valid days ⇒ pattern.
    args = { lateNights, shorterByMin, lateAfter: "01:30" (the median last screen-off, rounded to 30 min) }.
  Clustering (optional; from week 8 and ≥ 40 windows)
    vector per window = [stretchMin, staysPerHour, firstStayMin, stoneShare (0 if unknown), returnMin (median of that window's returns, 0 if none), quietShare]; z-score per column.
    k ∈ {2, 3}: k-means++ initialisation with Randomness seeded by CLUSTER_SEED; at most 50 iterations; choose k with the best mean silhouette; require ≥ 0.50, else nothing.
    A cluster becomes a Pattern(SHAPE, key="cluster:<cell>") only if: size ≥ 5, ≥ 70 % of its windows share one P1 cell, and its centre's stays/hour ≥ 1.5 × overall.
    The group is NAMED by rule from the centre ("Reached"-like if stone share low; "Pinged"-like if high), never by the user.

INTERFACES & CONTRACTS:
  The detectors consume derived entities (Window, Stay, Stretch, WeekSummary, DaySummary) and return Pattern. No detector reads raw events and none calls a port.
  The Mirror consumes Pattern.args; SentenceBuilder turns them into lines carrying the evidence count ("based on 9 windows").

ERROR HANDLING STRATEGY:
  A detector with too little evidence returns an empty list. Never an exception, never a low-confidence pattern. Division by zero is avoided by RATE_EPS and
  the minimum counts above.

EDGE CASES TO HANDLE:
  - The user changes the work-set: patterns are recomputed from re-derived windows; the pattern's first_seen is kept if the key matches.
  - Exactly 4 windows over 2 days in a cell: gate fails (days). Over 3 days: passes only if the ratio rule holds.
  - Seasonal or exam weeks flagged unusual are excluded from P2 and P3, never from P1 (rhythm is by cell).
  - k-means with two identical clusters (zero variance): silhouette undefined ⇒ treat as 0 ⇒ no cluster pattern.
  - A pattern disappears (the user improved): it is retired after 14 days without redetection, and a suggestion that depended on it stops being eligible.

PERFORMANCE CONSIDERATIONS:
  At most a few hundred windows. k-means is trivial. Run the whole PatternEngine only when a window finalises or a week closes, not on every worker run.

TESTING PLAN:
  Unit (golden):
    G-P1  overall median 3.0 stays/h. Cell NIGHT-WEEKDAY: 4 windows over 3 days, median 6.0 ⇒ ratio (6+0.5)/(3+0.5) = 1.857 ⇒ pattern CHOPPY.
          Same with 3 windows ⇒ none. Same with 4 windows on 2 days ⇒ none. Same with median 3.6 (ratio 1.17) ⇒ none.
    G-P2  return series [7, 6, 5, 4] ⇒ run3 = true, direction BETTER (last-2 median 4.5 vs prior-2 6.5 = −31 %). Series [5, 5.1, 4.9, 5.0] ⇒ FLAT, no pattern.
    G-P3  stays/h series [4, 4, 4, 6, 6, 6]: μ = 4, MAD = 0 ⇒ s = 0.4, k = 0.2, h = 1.6; S⁺ at index 3 = 1.8 > 1.6 ⇒ SHIFT up at index 3, persistent (mean 6 vs 4).
          Series [4, 4, 4, 4.2, 3.9, 4.1] ⇒ S⁺ and S⁻ never exceed h ⇒ none.
    G-P4  user median stays/h 3.0, stretch 10. W-A: 2.0 stays/h, stretch 14 ⇒ HELD. W-B: 5.0, 6 min, 3 of 4 stones (75 %) ⇒ PINGED.
          W-C: 5.0, 6 min, 1 of 4 stones (25 %) ⇒ REACHED. W-D: 0 stays ⇒ HELD. W-E: 2.0, 8 min, 3 of 5 stones (60 %) ⇒ PINGED (≥ 60 %). W-F: 5.0, 6 min, 1 known-origin stay ⇒ null.
    G-P5  35 stays; stretches ending in minutes 20–25: 22 of 40 endings (55 %) ⇒ pattern band 20, share 0.55. 18 of 40 (45 %) ⇒ none.
    G-P6  cross-day: 14 days, 5 qualifying late nights with mean next-day first stretch 8 min vs 14 min on other days (−43 %) ⇒ pattern. With 3 late nights ⇒ none.
    G-P7  clustering: 25 windows near (stretch 15, stays/h 2) on weekday afternoons plus 15 near (stretch 5, stays/h 7, stone 0.1) all weekend evenings ⇒ k = 2, silhouette ≥ 0.5,
          the second cluster ⇒ Pattern key "cluster:EVENING-WEEKEND". Uniform noise ⇒ none. Same seed twice ⇒ identical assignment.
  Property: re-running PatternEngine on identical input yields identical patterns (including keys and args); shuffling window order changes nothing.
  Integration: persona replay (Time Machine) surfaces P1, P4 and P5 for Aarav by his week 4 and the cross-day link by week 8.
```

---

## F11. The Suggestions Engine

```
FEATURE: F11 The Suggestions Engine

MODULE STRUCTURE:
  engine/suggestions/
    ├── SuggestionRule.kt:        interface SuggestionRule { val kind: SuggestionKind; fun evaluate(ctx: SuggestionContext): Candidate? }
    ├── SuggestionSelector.kt:    the silence rules and ranking, in one place
    ├── SuggestionRegistry.kt:    the list of the twelve rules (adding a thirteenth = one new file + one line here)
    └── rules/ S01ClearHour.kt · S02RepeatLeak.kt · S03SelfStarted.kt · S04PingDriven.kt · S05SlowReturn.kt · S06EarlyFlinch.kt
               · S07WorkSetLeak.kt · S08LateNight.kt · S09NaturalRhythm.kt · S10GlanceReassurance.kt · S11AfterLapse.kt · S12Win.kt
  engine/usecases/: TapTryThis.kt · DismissSuggestion.kt
  engine/model/:    SuggestionKind, Candidate, ActionType, SuggestionState

SHARED SURFACES: SuggestionDto in the Pigeon file (kind id, line, action, action type); the sentence lines in SentenceBuilder (F6).

FUNCTION & CLASS DESIGN:
  enum class SuggestionKind { S1, S2, S3, S4, S5, S6, S7, S8, S9, S10, S11, S12 }
  enum class ActionType { OPEN_NOTIFICATION_SETTINGS, MOVE_ICON, PHONE_FACE_DOWN, FIRST_THING_HARDEST, LEAVE_PAGE_OPEN, PHONE_IN_OTHER_ROOM,
                          ADD_TO_WORK_SET, SCREEN_OFF_BY_0030, PLAN_SHORT_PUT_DOWN, NONE }
  data class Candidate(val kind: SuggestionKind, val subject: Pkg?, val impactShare: Double, val target: TargetMetric?, val args: Map<String, String>, val action: ActionType)
  rules return a Candidate (data) or null. They never write words and never touch a store.

  Rule implementations (each uses Tuning; each consumes patterns or weekly aggregates, never a single event):
    S1  Pattern P1 clear slot: slot stays/h ≤ 0.5 × overall, stretch ≥ 1.5 × overall, ≥ 3 windows. impactShare = slot's share of all stretch minutes. target STRETCH_IN_SLOT. action FIRST_THING_HARDEST.
    S2  One OFF_SET pkg is first_pkg of ≥ 35 % of stays over ≥ 8 windows. impactShare = that share. subject = pkg. target STAYS_PER_HOUR_FROM_PKG. action MOVE_ICON.
    S3  ≥ 60 % of KNOWN-origin stays are SELF_STARTED (needs ≥ 10 known-origin stays). impactShare = self-started share. target SELF_STARTED_PER_HOUR. action PHONE_FACE_DOWN.
    S4  ≥ 60 % of KNOWN-origin stays are STONE and one package ≥ 50 % of the stones. impactShare = stone share. subject = pkg. target PINGS_FROM_PKG_IN_WINDOWS. action OPEN_NOTIFICATION_SETTINGS.
    S5  Median return > 1.5 × baseline for 2 consecutive weeks, or ≥ 8 minutes. impactShare = the share of the week's stays whose return exceeded 1.5 × the baseline return. target MEDIAN_RETURN. action LEAVE_PAGE_OPEN.
    S6  First stay inside the first 5 minutes of ≥ 50 % of windows (≥ 10 windows). impactShare = that share. target FLINCH_RATE. action PHONE_IN_OTHER_ROOM.
    S7  ≥ 25 % of window time in apps neither in-set nor neutral over ≥ 10 windows; subject = the top such pkg. impactShare = the coverage. target WORKSET_COVERAGE. action ADD_TO_WORK_SET.
    S8  P-CrossDay pattern. impactShare = lateNights ÷ 14. target NEXT_DAY_FIRST_STRETCH. action SCREEN_OFF_BY_0030.
    S9  P5 break point with topBandShare ≥ 0.50 and week ≥ 4. impactShare = topBandShare. target PUTDOWN_SHARE. action PLAN_SHORT_PUT_DOWN.
    S10 Glances ≥ 8 in the week and glances ÷ stays ≥ 8. Exempt observation, action NONE, no target.
    S11 A lapse of ≥ 3 days (F15). Exempt observation, action NONE.
    S12 A part improved ≥ 15 % two weeks running (P2 run). Exempt observation, action NONE.

  SuggestionSelector.select(ctx): SelectionResult(task: Candidate?, observation: Candidate?, reason: SilenceReason?)
    Observation: S11 > S12 > S10 priority; at most one, shown beside or without a task.
    Task (S1–S9) passes every silence rule in this order, else task = null with the first failing SilenceReason:
      1. validDays ≥ 8 and windows ≥ 10                                    (NOT_ENOUGH_DATA)
      2. not the first Mirror after a lapse                                (AFTER_LAPSE)
      3. no live experiment                                                (EXPERIMENT_LIVE)
      4. kind eligible for the user's week: S1, S2, S4, S7 from week 2; S3, S5, S6, S8, S9 from week 4   (TOO_EARLY)
      5. not dismissed (dismissed_until > asOf, per kind and subject)       (DISMISSED)
      6. not the same kind and subject within 4 weeks of its last showing, and not retired after NO_CHANGE for 8 weeks   (RECENT)
      7. no task already shown in the current week; from week 8 none within the last 2 weeks (D7)   (QUOTA)
    Ranking among the survivors: impactShare descending; ties go to the kind never shown before; from week 8 multiply impactShare by
    1.25 for kinds that were judged MOVED for this user and by 0.5 for kinds judged NO_CHANGE.
    "Nothing to fix this week" is the result when task = null and the reason is not an error. It is displayed (F6), never silent.
  TapTryThis(kind, subject): creates an Experiment(started_at = now, start_reason TAP, target from the Candidate) if none is live.
  DismissSuggestion(kind, subject): suggestion_state.dismissed_until = now + 4 weeks (S7 "never ask again about this app" = dismissed forever for that subject).
  Footprint start (S2 and S4): at each processNewEvents with the suggestion shown and no experiment live, if the last 7 days' rate for the subject is ≤ 0.5 × the
    prior 7 days' rate (≥ 3 windows in each) ⇒ Experiment(start_reason FOOTPRINT, started_at = start of the last 7 days).

INTERFACES & CONTRACTS:
  Suggestion lines and the one action per suggestion are produced by SentenceBuilder from Candidate.args. SuggestionDto carries: kindId, line, actionLabel, actionType,
  opensSettings(bool), subjectLabel(optional). No SuggestionDto carries two actions.

ERROR HANDLING STRATEGY:
  A rule that throws is caught by the selector, logged as a short code, and treated as null (one broken rule never silences the others). The selector itself returns a typed reason, never an exception.

EDGE CASES TO HANDLE:
  - Two rules fire with the same impact: the never-shown kind wins; if still tied, the lower kind number.
  - The user taps "Try this" for a suggestion that is no longer current (stale screen): rejected with a typed result, no experiment created.
  - The subject app is uninstalled mid-experiment: the experiment is judged UNCLEAR.
  - A suggestion that needs notification access (S3, S4) while access is off or listener coverage under 50 % of the last 14 days: rule returns null.
  - The same pattern drives two kinds (S2 and S4 both fire on one app): the higher impact wins, the other counts as RECENT for the cycle.

PERFORMANCE CONSIDERATIONS: twelve cheap rules over already-derived aggregates; run once per week close and once per window finalisation.

TESTING PLAN:
  Unit (golden):
    G-G1  silence: 7 valid days ⇒ task null (NOT_ENOUGH_DATA); 8 valid days and 9 windows ⇒ still null; 8 and 10 ⇒ eligible.
    G-G2  with a live experiment ⇒ task null (EXPERIMENT_LIVE) even if S2 would fire.
    G-G3  first Mirror after a 3-day lapse ⇒ task null, observation S11 present.
    G-G4  ranking: S2 impact 0.40 and S3 impact 0.62 ⇒ S3 wins. Tie at 0.50 each, S2 never shown and S3 shown before ⇒ S2 wins. Tie with both never shown ⇒ the lower kind number.
          Week 9: S2 judged MOVED earlier (0.40 × 1.25 = 0.50) against S3 0.45 ⇒ S2 wins.
    G-G5  dismissal: after DismissSuggestion(S2, pkgX) at t, S2/pkgX not eligible until t + 4 weeks; S2/pkgY unaffected.
    G-G6  week 8: a task shown in week 7 ⇒ none in week 8 (QUOTA); shown in week 6 ⇒ eligible in week 8.
    G-G7  each rule's boundary: S2 with 34 % ⇒ null, 35 % ⇒ fires; S6 with 49 % ⇒ null, 50 % ⇒ fires; S5 median 7.9 min but ratio 1.2 ⇒ null; 8.0 min ⇒ fires.
  Property: never two live experiments; never a task during a lapse's first Mirror; the selector output is identical on identical input; at most one task per Mirror.
  Copy: the sentence builder produces one line and one action for every kind with no forbidden word (see F6).
```

---

## F12. Self-Judging Verdicts

```
FEATURE: F12 Self-Judging Verdicts

MODULE STRUCTURE:
  engine/judging/
    ├── ExperimentJudge.kt:   fun judge(exp: Experiment, ctx: JudgeContext): Verdict
    ├── TargetMetrics.kt:     fun value(target: TargetMetric, subject: Pkg?, windows: List<WindowWithDetail>): Double?
    └── WeekdayMix.kt:        re-weights the before-period to the after-period's weekday/weekend mix
  engine/usecases/JudgeExperiments.kt: finds experiments with window_end ≤ asOf and writes verdicts
  engine/model/Entities.kt:  Experiment, Verdict, TargetMetric

SHARED SURFACES: VerdictDto (Pigeon); Experiment rows (read by F11 and F6).

FUNCTION & CLASS DESIGN:
  enum class TargetMetric { STRETCH_IN_SLOT, STAYS_PER_HOUR_FROM_PKG, SELF_STARTED_PER_HOUR, PINGS_FROM_PKG_IN_WINDOWS, MEDIAN_RETURN,
                            FLINCH_RATE, WORKSET_COVERAGE, NEXT_DAY_FIRST_STRETCH, PUTDOWN_SHARE }
  higherIsBetter: STRETCH_IN_SLOT, NEXT_DAY_FIRST_STRETCH, PUTDOWN_SHARE. Every other target is lower-is-better.
  enum class Verdict { PENDING, MOVED, NO_CHANGE, TOO_LITTLE, UNCLEAR }
  judge():
    1. asOf < started_at + 14 days ⇒ PENDING.
    2. after = windows in [started_at, started_at + 14 d); before = windows in [started_at − 14 d, started_at); both non-partial, valid days.
    3. if any week overlapping either period is unusual, or the subject app was uninstalled ⇒ UNCLEAR.
    4. if after has fewer than 8 windows ⇒ TOO_LITTLE.
    5. beforeValue is re-weighted to the after period's weekday/weekend mix (WeekdayMix): value per class, combined with the after-period class weights
       (window-hours; the example in G-J7 uses equal-length windows). If a class is missing on either side, use pooled values and set approxMix = true [ASSUMPTION].
    6. improvement = (before − after) ÷ before for lower-is-better; (after − before) ÷ before for higher-is-better.
    7. Steadiness fell = steadiness(pooled after parts) < steadiness(pooled before parts), both against the frozen baseline (if no baseline, treated as not fell).
    8. MOVED if improvement ≥ 0.20 and not steadinessFell; otherwise NO_CHANGE.
  Verdict effects (JudgeExperiments): MOVED ⇒ kind weight 1.25 (week ≥ 8 ranking); NO_CHANGE ⇒ kind retired for this user 8 weeks and weight 0.5;
    TOO_LITTLE and UNCLEAR ⇒ no claim, the experiment closes, the kind becomes eligible again after 4 weeks.
  Honest line (SentenceBuilder): every verdict says what it cannot say ("correlation, not cause; it could also have been a lighter fortnight").

INTERFACES & CONTRACTS:
  VerdictDto { verdict, line, beforeValue, afterValue, approxMix } displayed once in the Mirror after it lands, then kept in What I see history as counts only.

ERROR HANDLING STRATEGY: a missing metric (null) on either side ⇒ TOO_LITTLE with a sentence, never a zero comparison.

EDGE CASES TO HANDLE:
  - The user taps Try this and does nothing: the verdict is still computed (it is a measurement, not a grade); line wording stays neutral.
  - The footprint start precedes the tap: started_at = the footprint date.
  - Phone replaced or data cleared during the 14 days: Delete everything wipes experiments (a fresh install).
  - A second suggestion cannot start while one is live, so periods never overlap.

PERFORMANCE CONSIDERATIONS: evaluated once when the experiment's end passes.

TESTING PLAN:
  Unit (golden):
    G-J1  S2 target stays/h from the app: before 4.1, after 3.0 ⇒ improvement 26.8 % ⇒ MOVED (Steadiness not fell).
    G-J2  before 3.1, after 2.7 ⇒ 12.9 % ⇒ NO_CHANGE.
    G-J3  before 4.1, after 3.0, but Steadiness fell ⇒ NO_CHANGE.
    G-J4  7 windows in the after period ⇒ TOO_LITTLE; 8 ⇒ judged.
    G-J5  an unusual week inside the after period ⇒ UNCLEAR (precedence over TOO_LITTLE).
    G-J6  higher-is-better: STRETCH_IN_SLOT 10 ⇒ 12.5 ⇒ +25 % ⇒ MOVED; 10 ⇒ 11.5 ⇒ NO_CHANGE.
    G-J7  weekday mix: before 6 weekday windows at 5.0 and 2 weekend at 3.0; after 2 weekday at 5.0 and 6 weekend at 3.0 (equal real behaviour) ⇒ re-weighted before = 3.5, after = 3.5 ⇒ improvement 0 ⇒ NO_CHANGE (the naive pooled before of 4.5 would have shown a false improvement).
  Property: PENDING until exactly 14 days; deterministic; a verdict never changes after it is written.
```

---

## F6. The Weekly Mirror

```
FEATURE: F6 The Weekly Mirror

MODULE STRUCTURE:
  engine/mirror/
    ├── MirrorBuilder.kt:    fun build(weekStart: WeekStart?, asOf, ctx): MirrorView    (the one assembly point)
    ├── SentenceBuilder.kt:  all user-facing English; pure functions over structured args
    ├── DataStates.kt:       flags and fixed sentences (F17)
    ├── GentlePolicy.kt:     fun apply(view: MirrorView): MirrorView                     (F16)
    └── (TeacherLeaves.kt, Lapse.kt: F14, F15)
  host/Mappers.kt:           MirrorView → MirrorDto, field for field
  lib/features/mirror/       mirror_screen.dart, parts_card.dart, stones_card.dart, suggestion_card.dart, verdict_card.dart, goal_tap.dart, patterns_card.dart   (Track 4)

SHARED SURFACES: MirrorView (Views.kt), MirrorDto (Pigeon), the pattern/suggestion strings in SentenceBuilder (also read by F11/F12).

FUNCTION & CLASS DESIGN:
  data class MirrorView(
    val isDemo: Boolean, val provisional: Boolean, val gentle: Boolean,
    val weekStart: EpochMs, val weekLabel: String,                    // "5–11 Oct"
    val dataState: DataState,                                          // OK | LEARNING_BASELINE | TOO_LITTLE_DATA
    val dataFlags: List<DataFlag>, val dataLines: List<String>,        // F17, one fixed sentence per flag
    val headline: String,
    val parts: PartsView?, val steadiness: SteadinessView?,
    val stones: StonesView?, val clearHour: ClearHourView?,
    val patterns: List<PatternLine>,                                   // at most 4
    val suggestion: SuggestionView?, val observation: ObservationView?, val nothingToFix: Boolean,
    val verdict: VerdictView?, val goalTap: GoalTapView, val teacher: TeacherView?,
    val lapseLine: String?, val saying: SayingView?, val returnLine: String?,
    val reanchorOffered: Boolean, val suggestedStudyBlock: StudyBlockView?)      // v1.1 CA-1
  StudyBlockView(startMinute: Int, endMinute: Int)                      // minutes from local midnight; end < start crosses midnight
  PartsView(stretchMin, longestStretchMin, inSetShare, quietShare, staysPerHour, glances, returnMin: Double?, lines: PartLines, extrasLines: List<String>)
  SteadinessView(value: Int, word: String)                              // word ∈ Wavering | Steady | Steadier
  StonesView(totalStays, stoneCount, selfStartedCount, unknownCount, topStoneLabel: String?, noRippleRate: Double?, line: String)
  ClearHourView(startHour: Int, endHour: Int, stretchMin: Double, line: String)
  PatternLine(kindId: String, line: String, evidenceWindows: Int, evidenceDays: Int)
  SuggestionView(kindId: String, subjectKey: String?, line: String, actionLabel: String, actionType: String, opensSettings: Boolean)
  ObservationView(kindId: String, line: String)
  VerdictView(verdict: String, line: String, beforeValue: Double?, afterValue: Double?, approxMix: Boolean)
  GoalTapView(offered: Boolean, answer: String?)                        // YES | PARTLY | NOT_YET
  TeacherView(opensThisWeek: Int, opensPrevWeek: Int?, minutesThisWeek: Double, line: String)
  SayingView(id, text, source, tierLabel, question: Int)               // the user's current saying

  MirrorBuilder.build, in this order (the DOC 1 §1.3.2 F6 order is the UI order; this is the compute order):
    1. resolve week: null ⇒ the latest completed week with ≥ 3 valid days; if none exists ⇒ provisional First Look over all seen days (F1).
    2. WeekMetrics → Parts; steadiness only if a baseline is active, all four parts present, and the week is not provisional; else null.
    3. stones from the week's stays; clear hour from the P1 clear slot if it exists; patterns = existing Pattern rows seen in the last 14 days, ranked by evidenceWindows, max 4.
    4. selection = SuggestionSelector.select(...) ⇒ suggestion or nothingToFix = true (only when the reason is not NOT_ENOUGH_DATA; with not enough data the
       Mirror shows the "learning" sentence instead of "Nothing to fix").
    5. verdict = the latest written Verdict not yet shown; goalTap offered if a baseline exists, the week count since baseline ≤ GOAL_TAP_LAST_WEEK (8), and no answer for the week.
    6. teacher, lapseLine, saying, dataFlags.
    7. reanchorOffered = a baseline is active && the week is not provisional && week count since baseline ≥ 4 && only one Baseline row exists (never re-anchored);
       suggestedStudyBlock = the learned study block when learn_study_hours is on, ≥ 14 inferred windows exist (F3) and its start or end differs from the saved block
       by ≥ 60 minutes at week ≥ 3; otherwise null. (v1.1 CA-1)
    8. if gentle ⇒ GentlePolicy.apply(view) last, so no feature can bypass it (it also sets reanchorOffered = false and suggestedStudyBlock = null).
    The façade method SakshiEngine.mirror() then records note_state.mirrorViewedWeek = weekStart when the week is completed and not demo (v1.1 CA-3).
  SentenceBuilder rules (the entire copy lives here; Flutter never words anything):
    headline(week): "You held {C}-minute stretches and {R}-minute returns; {rel} your starting normal." rel = "steadier than" | "close to" | "less steady than" (from the word).
                    provisional: "Your last few days: you held about {C} minutes at a stretch. Something pulled you away about {P} times an hour."
    part lines:     "You stayed in your work apps for {C} minutes at a stretch (longest {L})." · "Something pulled you away {n} times." · "It took you about {R} minutes to get back."
                    · "Your phone was quiet for {Q}% of your work time."
    stones:         "{k} of your {n} stays began with a ping." (only when ≥ 1 known origin) · "Most of your stays began with no ping." for self-started majority.
    pattern lines:  one template per kind taken from DOC 1 §1.2.6's example sentences; each ends with "(based on {w} windows over {d} days)".
    suggestion:     one plain line and one action label from DOC 1 §1.3.3 (e.g. S2 "One app is behind {a} of your last {b} stays." / "Move its icon to a second screen.").
    verdict:        DOC 1 §1.3.4 wording; every verdict line carries "This is correlation, not cause." variant per verdict kind.
    lapse:          "You were away {n} days. Your starting normal is still here."
    teacher:        see F14.
  FORBIDDEN (CopyRulesTest): no string produced by SentenceBuilder may match (case-insensitive)
      \\b(focused|distracted|distraction|wasted|waste|failed|failure|streak|lazy|addict\\w*|ruin\\w*|lost (your )?(focus|concentration)|you (should|must|need to))\\b
      or contain "!" . It may not name any app as a cause of a feeling; app names appear only as the subject of a count ("behind 11 of 17 stays").
  Every pattern line carries its evidence count. No sentence compares the user to anyone else.

INTERFACES & CONTRACTS:
  MirrorDto = MirrorView field for field (Pigeon file, LC-4). Flutter renders strings it is given, in the DOC 1 order:
  headline, parts (with in-set/quiet split beside Stretch), top stone, clear hour, patterns, saying, suggestion or "Nothing to fix this week", verdict, goal tap, teacher line.

ERROR HANDLING STRATEGY:
  MirrorBuilder never throws on missing data: absent pieces are null and the corresponding F17 flag is added. An unexpected exception inside one section is caught,
  that section is null, and `dataFlags` gains INTERNAL_PARTIAL (shown as "I could not read part of this week"); the error code goes to last_error.

EDGE CASES TO HANDLE:
  - Provisional (no baseline): no Steadiness, no suggestion, no pattern lines, headline in the First Look form.
  - Week in progress requested: allowed (the user asks via listMirrorWeeks), flagged provisional = true, parts only from finalised windows.
  - An unusual week: shown with the F17 line; excluded from trends.
  - Suggestion subject app no longer installed: the suggestion is dropped for this build (rule returns null next run).
  - A week with no windows: parts null, dataState TOO_LITTLE_DATA, no zeros.

PERFORMANCE CONSIDERATIONS: built on demand from derived rows; typically a few milliseconds; Flutter caches the DTO per week in a provider until syncNow() completes.

TESTING PLAN:
  Unit:        golden MirrorViews for: learning, steady, steadier, wavering, partial ping, nothing to fix, unusual week, demo; each asserts field presence/absence
  Copy:        CopyRulesTest runs SentenceBuilder over every fixture and over a generated matrix of args; fails on any forbidden pattern or "!"
  Contract:    a Dart test decodes a fixture JSON of MirrorDto produced by the Kotlin golden test (same file in test resources and Flutter test assets)
  Unit (v1.1):  reanchorOffered true only at week ≥ 4 with one Baseline row; false after reanchor() and in gentle; suggestedStudyBlock null below 60 min difference; mirror() writes mirrorViewedWeek once for a completed non-demo week and never for demo or in-progress weeks
  Presentation (Flutter widget tests): each fixture renders; "Nothing to fix this week" appears when nothingToFix; no Steadiness widget in gentle fixture
```

---

## F7. The Lake (Home-Screen Widget)

```
FEATURE: F7 The Lake (home-screen widget; required in the demo build)

MODULE STRUCTURE:
  engine/lake/LakeBuilder.kt:  fun build(asOf, ctx): LakeView            (state, phrase, asOf)
  host/LakeWidget.kt:          AppWidgetProvider; refresh(context, view: LakeView?) static helper
  android/app/src/main/res/
    ├── xml/lake_widget_info.xml      updatePeriodMillis = 0, minWidth/minHeight for a 2×1 cell, no configure activity
    ├── layout/lake_widget.xml        ImageView + phrase TextView + "as of" TextView
    └── drawable/lake_still.xml · lake_rippled.xml · lake_choppy.xml    vector drawables (art by Track 4, wired by Track 1)
  lib/features/lake/lake_painter.dart   in-app Lake (same three states; Track 4)

SHARED SURFACES: LakeView / LakeDto (Pigeon), drawable names (a frozen name list), res/ shared by Track 1 and Track 4 (Track 4 only adds drawables).

FUNCTION & CLASS DESIGN:
  enum class LakeState { LEARNING, NO_DATA, STILL, RIPPLED, CHOPPY }
  LakeBuilder.build:
    - no finalised, non-partial window yet ⇒ NO_DATA
    - baseline not frozen ⇒ LEARNING
    - else take the most recently ENDED finalised window (never the live one): HELD ⇒ STILL;
      stays/hour ≥ 1.5 × the user's own median per-window rate ⇒ CHOPPY; otherwise RIPPLED (including shape null).
    asOf label = that window's end time.
  Phrases (fixed set in SentenceBuilder; gentle set differs):
    normal: STILL "Still water." · RIPPLED "A few ripples." · CHOPPY "Choppy water." · LEARNING "Learning your normal." · NO_DATA "Nothing to show yet."
    gentle: STILL "Calm." · RIPPLED "Some ripples." · CHOPPY "Some waves." · LEARNING "Learning your normal." · NO_DATA "Nothing to show yet."
  LakeWidget.refresh: read lake_state; set the drawable by state (LEARNING and NO_DATA use the still drawable), phrase text, "as of 9:42 pm" in local time;
    PendingIntent opens MainActivity on the Mirror (extra route=mirror; MainActivity.getInitialRoute() returns "/mirror" for it: v1.1 CA-4). Called by: IngestWorker (when ProcessReport.lakeChanged), app open (syncNow), widget onUpdate, DemoController.
  No counters, no minutes, nothing live, no animation.

INTERFACES & CONTRACTS:
  getLake(): LakeDto — the same source as the widget (lake_state row), so the in-app Lake and the widget never disagree.

ERROR HANDLING STRATEGY:
  refresh() catches every exception (a widget failure must never take down a worker run) and writes a short code to last_error. If the stored state is missing it draws NO_DATA.

EDGE CASES TO HANDLE:
  - The launcher is changed or the widget removed: onDeleted does nothing; adding it again calls onUpdate which redraws from stored state.
  - The worker is killed for days (iQOO): the widget keeps showing its "as of" time, so staleness is visible, never silent. [VERIFY ON DEVICE: S-D, S-F]
  - Demo mode: DemoController passes a LakeView with the phrase suffixed " (demo)" so the widget cannot be mistaken for real data.
  - Dark mode and different launcher grid sizes: vectors scale; text uses theme-agnostic colours with a translucent backing.
  - Window ends while the screen is on (the widget redraw happens when the user is on the home screen): acceptable; it reflects the completed window only.

PERFORMANCE CONSIDERATIONS: one RemoteViews update per run; no bitmaps drawn in code; vectors only.

TESTING PLAN:
  Unit:        LakeBuilder: HELD window ⇒ STILL; window at 1.5× median ⇒ CHOPPY; 1.49× ⇒ RIPPLED; no baseline ⇒ LEARNING; no windows ⇒ NO_DATA; the live (unfinalised) window is ignored.
  Integration: Room lake_state round trip; getLake() equals the widget's view model
  Device:      S-F on both phones: add the widget, finish a window (or use the demo clock), observe a redraw; reboot; change launcher
```

---

## F8. Ask Now (Today So Far)

```
FEATURE: F8 Ask Now (Today So Far)

MODULE STRUCTURE:
  engine/mirror/TodayBuilder.kt:  fun build(asOf, ctx): TodayView
  lib/features/today/today_screen.dart   (Track 4)

SHARED SURFACES: TodayDto (Pigeon). TodayView has NO suggestion, NO Steadiness, NO baseline comparison fields, so the silence rules cannot be broken by accident (type-level guard).

FUNCTION & CLASS DESIGN:
  data class TodayView(val isDemo: Boolean, val windows: List<TodayWindowView>, val parts: PartsView?, val line: String, val dataFlags: List<DataFlag>, val dataLines: List<String>)
  data class TodayWindowView(val start: EpochMs, val end: EpochMs, val shape: String?, val stretchMin: Double?, val stays: Int, val returnMin: Double?)
  TodayBuilder: only FINALISED windows whose start falls in the current study day; parts pooled over them with the same code as WeekMetrics (no smoothing difference);
  line: "Today so far: {n} finished windows. You held {C}-minute stretches." Never a verdict about the day. An in-progress window is never included (so it is never feedback mid-session).
  Pull-only: nothing in Sakshi links to this screen except a quiet menu entry in the Mirror; no notification, no widget tap target.

INTERFACES & CONTRACTS: getTodaySoFar(): TodayDto.

ERROR HANDLING STRATEGY: none finalised ⇒ line "No finished window yet today." with empty lists, never zeros.

EDGE CASES TO HANDLE:
  - Window crossing 04:00: belongs to the day it began; if requested after 04:00 it appears under "yesterday" (not today).
  - Windows < 15 minutes old are not finalised and are excluded (the 15-minute lag shows as "recent minutes are not visible yet" via the F17 sentence).

PERFORMANCE CONSIDERATIONS: trivial.

TESTING PLAN:
  Unit:        two finalised windows and one live ⇒ exactly two in the view; a view type check that TodayView has no suggestion member (reflection test)
  E2E:         Rohan scenario: open mid-day, see completed windows only
```

---

## F9. The Quiet Note

```
FEATURE: F9 The Quiet Note (optional, off by default)

MODULE STRUCTURE:
  engine/note/WeeklyNote.kt:  fun decide(ctx: NoteContext): NoteDecision
  host/WeeklyNoteNotifier.kt: posts the notification; owns the channel; checks POST_NOTIFICATIONS
  lib/features/settings/weekly_note_tile.dart   (Track 3)

SHARED SURFACES: setWeeklyNote(bool) in the Pigeon file; note_state row.

FUNCTION & CLASS DESIGN:
  data class NoteContext(asOf, enabled, canPost: Boolean, lastCompletedWeek: WeekStart?, validDaysInThatWeek: Int, lastNoteWeek: WeekStart?, inWindowNow: Boolean, localZone)
  decide(): post = enabled && canPost && lastCompletedWeek != null && validDaysInThatWeek ≥ 3 && lastNoteWeek != lastCompletedWeek
            && local time within Monday 08:00 to Wednesday 23:59 of the following week && !inWindowNow
            (otherwise NoteDecision(post = false, reason)). If the Wednesday cut-off passes, that week's note is skipped for good (a stale "ready" is worse than none).
  inWindowNow = a study-hours block is active now OR in-set use occurred within the last 10 minutes.
  WeeklyNoteNotifier.maybePost(decision): on post = true, create the channel "weekly_mirror" (IMPORTANCE_LOW: no sound, no vibration, no badge), title "Sakshi",
    body "Your Mirror is ready", tap opens the Mirror, setTimeoutAfter(3 days), no actions, no content about the score. Then note_state.last_note_week = that week.
  setWeeklyNote(true): requests POST_NOTIFICATIONS on Android 13+ through the Activity; returns the effective state (false if denied). setWeeklyNote(false) cancels any posted note.

INTERFACES & CONTRACTS: setWeeklyNote(enabled): Future<bool> returns the effective state. The notification text is a constant string in one place.

ERROR HANDLING STRATEGY: a denied permission leaves enabled = false and the UI shows one plain line; a failed post logs a short code and the next run retries until the Wednesday cut-off.

EDGE CASES TO HANDLE:
  - The user opens the Mirror before the note posts: no note (skip if the Mirror for that week was already viewed; note_state.mirror_viewed_week).
  - Two worker runs in one minute: the note_state check makes the second a no-op.
  - Phone off over the weekend: the note posts on the first run after Monday 08:00 if before Wednesday end.
  - Gentle mode: same note text; nothing about numbers.

PERFORMANCE CONSIDERATIONS: negligible.

TESTING PLAN:
  Unit:        Monday 07:59 ⇒ no; 08:00 ⇒ yes; Wednesday 23:59 ⇒ yes; Thursday 00:00 ⇒ no; already noted ⇒ no; inWindowNow ⇒ no; 2 valid days ⇒ no; disabled ⇒ no
  Integration: after post, last_note_week is written; a second decide() returns no
  Device:      post once on the Nothing Phone 3a; confirm silent, no badge, taps into the Mirror
```

---

## F10. What I See (Pause, Export, Delete)

```
FEATURE: F10 What I See (Pause, Export, Delete everything)

MODULE STRUCTURE:
  engine/privacy/
    ├── WhatISeeBuilder.kt:  fun build(ctx): WhatISeeView
    └── ExportBuilder.kt:    fun build(ctx, includeRaw): ExportDocument      (pure; JSON is written by data/Exporter.kt)
  engine/usecases/: PauseCollection.kt · DeleteEverything.kt
  data/Exporter.kt:          writes cacheDir/exports/sakshi_export_<yyyyMMdd>.json; the host shares it through a FileProvider URI
  lib/features/what_i_see/   what_i_see_screen.dart (Track 3)

SHARED SURFACES: WhatISeeDto, ExportDto; the data_gap table.

FUNCTION & CLASS DESIGN:
  data class WhatISeeView(isDemo, usageAccessGranted, notificationAccessGranted, rawEventCount, notifEventCount, oldestRawEvent: EpochMs?, derivedDays, listenerCoverage7d: Double?,
                          lastWorkerRun: EpochMs?, workerRuns7d: Int, paused: Boolean, lastError: String?, oddEventPairs: Int, lines: List<String>)
  lines (fixed sentences): "I can see which app is in front and when (package names and times). I never read what is on your screen."
                           "I note when an app sends a notification: its name, time and category. Never the words." "Everything is stored on this phone and nowhere else."
                           "Heard pings for {x}% of the last 7 days." "Last background run {m} minutes ago; {n} runs in the last 7 days."
  PauseCollection(on): on ⇒ ingest_state.paused = true, DataGap(PAUSED, now, null); the NotificationCollector drops events while paused; IngestWorker appends nothing.
                       off ⇒ close the open DataGap at now; paused = false; cursor moves to now (the gap is never back-filled from Android's log).
  ExportBuilder: document { exportVersion: 1, exportedAt, settings, baselines, weeks, days, windows, stretches, stays, patterns, suggestionStates, experiments, goalTaps,
                 sayingPicks, raw: { rawEvents[], notifEvents[] } only when includeRaw }. Derived rows only by default. No app labels, only package names.
  DeleteEverything: transaction wipes every table (facts, derived, settings, gaps, state, picks), deletes cached exports, resets lake_state to NO_DATA, redraws the widget,
                    leaves permissions untouched, leaves the WorkManager schedule in place; the next run behaves like a fresh install (first_read_at is set again).
  Demo gating: Pause, Export and Delete are rejected while a demo is active (typed error DEMO_ACTIVE) so a real database is never wiped by a stray tap on demo data.

INTERFACES & CONTRACTS: pause(bool), exportData(includeRaw): ExportDto{fileName, byteSize}, deleteEverything(): void. The host writes the file and launches the Android share sheet itself (no Flutter share plugin). The Flutter side confirms Delete with a two-step dialog; the host does not.

ERROR HANDLING STRATEGY:
  Export write failure ⇒ HostError EXPORT_FAILED with a user message "I could not write the file."; nothing partial is left (write to a temp file, rename on success).
  Delete failure mid-way is impossible by design (one Room transaction); file deletion failures are ignored and logged.

EDGE CASES TO HANDLE:
  - Delete during an ingest run: the run's transaction is serialised with the delete through a single-threaded dispatcher; the run sees an empty store and no cursor.
  - Pause then reboot: paused is persisted; the job still runs but appends nothing.
  - Export with includeRaw on a heavy phone (about 14 days of events): streamed JSON writer, not an in-memory string. [ASSUMPTION: a few MB at most.]

PERFORMANCE CONSIDERATIONS: export streams; delete is a single transaction.

TESTING PLAN:
  Unit:        pause/resume creates and closes exactly one PAUSED gap; export of a fixture equals a golden JSON (derived only) and includes raw only on request
  Integration: Delete empties every table (a test lists all tables from the Room schema and asserts zero rows); Pause blocks NotificationCollector appends
  Privacy:     the export contains no field named title or text and no app label strings
  E2E:         export to the share sheet on a phone; delete; confirm the app returns to setup
```

---

## F13. The Saying Shelf

```
FEATURE: F13 The Saying Shelf

MODULE STRUCTURE:
  engine/ports/Ports.kt:    interface SayingShelf { fun all(): List<Saying> }                         (new port, D5)
  engine/model/Entities.kt: Saying
  engine/usecases/:         ChooseSayings.kt · PickSaying.kt
  host/SayingShelfImpl.kt:  reads and parses assets/sakshi/sayings.json once, caches
  android/app/src/main/assets/sakshi/sayings.json   LC-5 (27 entries, delivered with this document)
  lib/features/shelf/       saying_picker.dart (three cards), saying_footer.dart (the saying under the Mirror)   (Track 4)
  tools/verify_sayings.py + tools/qindex.json       (development only; not in the APK)

SHARED SURFACES: sayings.json schema (Track 4 owns the file, Track 1 loads it, Track 2 reads via the port).

FUNCTION & CLASS DESIGN:
  data class Saying(val id: String, val q: String, val text: String, val source: String, val tier: Char, val usedFor: String)
  tierLabel(tier): 'A' "his own writing or letter" · 'B' "recorded lecture" · 'C' "reported by others" · 'D' "type not resolved in the Outcome Map".
     Computed in the engine from the tier letter; never stored; no tier D saying can ever be labelled "his own writing".
  ChooseSayings(asOf): offered when (no saying has been picked and the user is in week ≥ 3) or (≥ 3 weeks since the last pick). Returns exactly 3:
     1. exclude the 9 most recently picked ids; 2. relevance score = +1 per active tag the current week matches (tags from usedFor: "Stone and wave" if stone share ≥ 50 %;
     "Return" if the return part is worse than baseline; "Quiet; rhythm" if quiet share improved; "Glance reassurance" if S10 fired; "The Teacher Leaves" if week ≥ 8;
     "Wins" if S12 fired; "After a lapse" if a lapse line is showing); 3. top 3 by score; ties by Randomness seeded with the week number (deterministic).
  PickSaying(id): writes saying_pick; the Mirror footer shows that saying until the next pick. One tap; optional; never a command; never during a window (pull-only).
  Where it shows: under the data in the Mirror (position 5 of F6) with source and tierLabel. Never in the Lake, the widget, the note or any notification.

INTERFACES & CONTRACTS:
  sayings.json schema (LC-5): [{ "id": "sy01", "q": "Q086", "text": "<verbatim excerpt>", "source": "<as the Map gives it>", "tier": "A|B|C|D", "usedFor": "<tag text>" }]
  getSayingChoices(): List<SayingDto> (3 or 0 when not offered); pickSaying(id).

ERROR HANDLING STRATEGY: a missing or corrupt shelf file ⇒ getSayingChoices() returns an empty list and the Mirror hides the saying; never a crash.

EDGE CASES TO HANDLE:
  - Fewer than 3 unpicked sayings (a long-lived install): the 9-exclusion window shrinks until 3 remain.
  - Q077 and Q190 must never appear in the shelf (Q077 is a chain in disguise; Q190 is a caution): enforced by the integrity test.
  - Gentle mode: the Saying still shows (D8).

PERFORMANCE CONSIDERATIONS: the shelf is 27 small records; parse once.

TESTING PLAN:
  Unit:        ChooseSayings: week 2 ⇒ none; week 3 ⇒ three; after a pick, none until 3 weeks later; deterministic for a fixed week
  Integration (SayingShelfIntegrityTest): exactly 27 entries; ids unique; q matches Q\\d{3}; tier ∈ A–D; no Q077 or Q190; each `text` (curly quotes normalised, trailing full stop ignored)
               is a substring of its Outcome Map quote in tools/qindex.json (run from android/app as ../../tools/qindex.json)
  Copy:        tierLabel never returns "his own writing" for tier D
```

---

## F14. The Teacher Leaves

```
FEATURE: F14 The Teacher Leaves

MODULE STRUCTURE:
  engine/mirror/TeacherLeaves.kt:  fun meter(ownEvents: List<RawEvent>, weekStart): OwnUse(opens, minutes)
  engine/ports/Ports.kt:           AppCatalog.ownPackage(): Pkg   (so the engine can recognise Sakshi's own events without hardcoding)
  lib/features/mirror/teacher_line.dart   (Track 4)

SHARED SURFACES: week_summary.app_opens / app_minutes; TeacherView in MirrorDto.

FUNCTION & CLASS DESIGN:
  meter(): reconstruct foreground intervals for pkg == ownPackage only; merge intervals separated by < 30 s (OWN_OPEN_MERGE_SEC); an "open" = one merged interval of ≥ 3 seconds;
           minutes = total merged time. Own events are excluded from every other metric (they are not in-set, not off-set; the classifier returns NEUTRAL for the own package).
  Teacher line (SentenceBuilder), shown from the user's 2nd week of data:
     opens fell:  "You opened me {a} times in week {k} and {b} times this week."
     else:        "You opened me {b} times this week."        (never a claim, never a nudge, never a goal of fewer opens)
  Suggestion volume (D7): from week 8 the selector allows one task every two weeks (F11). The Mirror shows nothing about this; "a quiet Mirror is a good Mirror".
  Gentle mode: no teacher line (D8).
  Widget taps and notification taps count as opens (they bring Sakshi to the foreground).

INTERFACES & CONTRACTS: TeacherView{opensThisWeek, opensPrevWeek?, minutesThisWeek, line}; derived rows week_summary.app_opens/app_minutes written at week close and recomputed for the current week on demand.

ERROR HANDLING STRATEGY: no own events in the week (user never opened it) ⇒ opens = 0 and the line is omitted, never a sad message.

EDGE CASES TO HANDLE:
  - Setup week opens are high: shown as they are; the persona data (5, 3, 2) mirrors reality.
  - Opens during a data gap are not recoverable: the week is flagged and the line omitted.
  - A restart caused by Android (process kill) while the app is open creates a gap < 30 s: merged by the 30-second rule.

PERFORMANCE CONSIDERATIONS: derived from the same raw events already loaded.

TESTING PLAN:
  Unit:        two own intervals 20 s apart ⇒ one open; 31 s apart ⇒ two; a 2 s interval ⇒ not an open; 5 → 3 opens ⇒ falling line; 3 → 3 ⇒ plain line
  Invariant:   inserting own-package events never changes Steadiness or any Part (property test)
  Persona:     Aarav weeks produce 5, 3, 2 opens
```

---

## F15. Lapse and Return

```
FEATURE: F15 Lapse and Return

MODULE STRUCTURE:
  engine/mirror/Lapse.kt:  fun detect(days: List<DaySummary>, gaps: List<DataGap>, asOf): LapseInfo?
  engine/model/Entities.kt: LapseInfo(days: Int, endedAt: StudyDay)

SHARED SURFACES: MirrorView.lapseLine; SuggestionSelector input (AFTER_LAPSE); settings.lapse_acknowledged_through.

FUNCTION & CLASS DESIGN:
  detect(): the most recent run of ≥ 3 consecutive study days that are INACTIVE, where inactive = no ACTIVITY_RESUMED event from any package other than Sakshi's own (D12),
            none of those days is covered (≥ 50 %) by a NOT_SEEN or PAUSED gap, and the run lies after first_read_at.
  Why not "no valid days": a study-hours block makes a day valid even when the phone is untouched, and a paper-book day is a real day for the baseline. A day with
  no phone activity at all, three times in a row, is the user being away.
  The line is shown once: when the user next opens Sakshi after the run (settings.lapse_acknowledged_through = the run's last day afterwards).
  Line: "You were away {n} days. Your starting normal is still here." No count of days kept, no streak, no guilt; n is the days away, said once.
  Effects: the first Mirror after a lapse carries no task suggestion (AFTER_LAPSE); the lapse line is the S11 observation.
  Returns after lapses are counted in week_summary.returns_after_lapse (the discipline signal; never shown as a chain).
  DaySummary gains `externalResumes: Int` (count of non-own ACTIVITY_RESUMED events that study day) for this rule.

INTERFACES & CONTRACTS: MirrorView.lapseLine: String?; no separate Pigeon call.

ERROR HANDLING STRATEGY: if data gaps explain the silence (phone off, Sakshi away longer than Android keeps data), the lapse is NOT declared; the F17 "not seen" line is shown instead (a lapse is something the user did, not something the phone did).

EDGE CASES TO HANDLE:
  - A paper-book day with a few messages: ACTIVE, so not a lapse and a valid day for the baseline.
  - Holiday week flagged unusual and lapse together: both lines may show; the lapse line first.
  - A lapse longer than the platform retention: the NOT_SEEN gap path above applies.

PERFORMANCE CONSIDERATIONS: trivial.

TESTING PLAN:
  Unit:        3 inactive seen days ⇒ lapse n = 3; 2 days ⇒ none; 3 days inside a NOT_SEEN gap ⇒ none; 3 days with only Sakshi's own events ⇒ lapse; shown once then acknowledged; first Mirror after it has no task
  Copy:        the lapse line contains no "streak", "missed" or "days kept"
```

---

## F16. Gentle Mode

```
FEATURE: F16 Gentle Mode

MODULE STRUCTURE:
  engine/mirror/GentlePolicy.kt:  fun apply(view: MirrorView): MirrorView            (the only place the restriction is defined)
  engine/usecases/SetGentle.kt · SetUnder18.kt
  lib/features/settings/gentle_tile.dart · lib/features/setup/age_tap.dart   (Track 3)

SHARED SURFACES: settings.gentle_mode, settings.gentle_explicit, settings.age_under_18; MirrorDto.gentle.

FUNCTION & CLASS DESIGN:
  effectiveGentle = gentle_explicit ? gentle_mode : age_under_18.
  setUnder18(true) at setup sets the default on; setGentleMode(x) records an explicit choice that overrides the age default from then on.
  The age flag is one boolean used for nothing else; it is never exported with a label other than "gentleDefault".
  GentlePolicy.apply(view): returns a copy with
     steadiness = null; patterns = []; suggestion = null; observation = null; nothingToFix = false; goalTap.offered = false; teacher = null; clearHour = null;
     headline = the return line ("After a stay, it took you about {R} minutes to get back." or the learning sentence);
     parts reduced to staysPerHour (stay count in words), returnMin, and their two lines; quiet/stretch lines hidden.
  Also in gentle mode (v1.1 CA-1): reanchorOffered = false, suggestedStudyBlock = null.
  Kept in gentle mode (D8): the return line, the stay count, the Saying, honest data states, the lapse line, the gentle Lake phrases.
  The Lake and widget use the gentle phrase set.

INTERFACES & CONTRACTS: MirrorDto.gentle: bool; Flutter shows no Steadiness widget when steadiness == null and gentle == true.

ERROR HANDLING STRATEGY: if the policy fails (exception), the Mirror falls back to the most restrictive view (headline + return line only); never to the full view.

EDGE CASES TO HANDLE:
  - Toggled mid-week: applies immediately to all Mirrors, including past weeks.
  - Under-18 user turns gentle off explicitly: honoured; the user's choice wins.
  - The baseline still freezes silently in gentle mode (so turning gentle off later shows a number immediately).

PERFORMANCE CONSIDERATIONS: trivial.

TESTING PLAN:
  Unit:        for every golden MirrorView, apply(view) has steadiness == null, no pattern or suggestion, and no string anywhere matches \\b(Wavering|Steady|Steadier|Steadiness)\\b
  Invariant:   GentlePolicy is idempotent; applying it to an already gentle view changes nothing
  Widget:      Flutter gentle fixture shows return line and stay count only
```

---

## F17. Honest Data States

```
FEATURE: F17 Honest Data States

MODULE STRUCTURE:
  engine/mirror/DataStates.kt:  fun flags(ctx, week): List<DataFlag>;  fun sentences(flags, args): List<String>
  engine/model/Entities.kt:     enum class DataFlag { PING_OFF, PARTIAL_PING, NOT_SEEN, PAUSED, TOO_LITTLE_DATA, UNUSUAL_WEEK, INTERNAL_PARTIAL, FIRST_LOOK }
                                enum class DataState { OK, LEARNING_BASELINE, TOO_LITTLE_DATA }

SHARED SURFACES: MirrorDto.dataFlags and dataLines; SyncStatusDto.state.

FUNCTION & CLASS DESIGN:
  PING_OFF       notification access not granted, or no listener session ever recorded.
  PARTIAL_PING   listener coverage of the displayed week's window time < 70 % (ESTIMATE; Tuning.PING_COVERAGE_PARTIAL = 0.70); includes "last heard {n} days ago".
  NOT_SEEN       the week overlaps a NOT_SEEN gap; those days are named and excluded.
  PAUSED         the week overlaps a PAUSED gap.
  TOO_LITTLE_DATA fewer than 4 non-partial windows, or zero valid days, in the displayed week.
  UNUSUAL_WEEK   the week's window minutes < 50 % of the baseline's mean weekly window minutes.
  FIRST_LOOK     the view is provisional.
  Fixed sentences (one per flag; the only wording for these states):
    PING_OFF "Ping awareness is off, so I can't tell a ping from a reach."
    PARTIAL_PING "Ping awareness is partial; I last heard a ping {n} days ago. Stays I could not check are left out of the ping count."
    NOT_SEEN "I did not see {d} days this week (the phone was off, or I was away longer than Android keeps data). They are left out."
    PAUSED "Collection was paused for {d} days this week."
    TOO_LITTLE_DATA "Too little data this week to say much."
    UNUSUAL_WEEK "This looks like an unusual week, so I have left it out of the trends."
    FIRST_LOOK "These are your last few days as Android kept them."
  Rule: a number that cannot be computed is null in the DTO and the UI shows its sentence, never a zero. A silent zero is a lie (DOC 1 F17).
  Rule: on a not-seen day the baseline and trends skip the day; they are never treated as quiet.

INTERFACES & CONTRACTS: SyncStatusDto.state ∈ ok | partialPingAwareness | gapNotSeen | paused | noPermission (derived from the same flags).

ERROR HANDLING STRATEGY: flags are data; there is no exception path. Unknown combinations fall back to TOO_LITTLE_DATA.

EDGE CASES TO HANDLE:
  - Listener alive but the phone was in Do Not Disturb (pings still arrive in the listener): no flag.
  - Window overlaps a gap by a few seconds: the window is partial and excluded; the sentence names days, not windows.
  - Demo data: flags are computed normally on the synthetic data (so the demo can show a partial-ping week deliberately).

PERFORMANCE CONSIDERATIONS: coverage is an interval intersection over a handful of sessions.

TESTING PLAN:
  Unit:        coverage 0.69 ⇒ PARTIAL_PING; 0.70 ⇒ none; gap days produce NOT_SEEN with the right count; 3 windows ⇒ TOO_LITTLE_DATA; 4 ⇒ not
  Invariant:   no Part is 0.0 as a stand-in for "missing" (property test over generated sparse inputs: parts are null or > 0)
  Presentation: each flag's sentence renders in a Flutter widget test
```

---

## Time Machine Demo and Personas

```
FEATURE: Time Machine Demo and Personas

MODULE STRUCTURE:
  engine/demo/
    ├── Personas.kt:          val PERSONAS: Map<String, PersonaSpec>   (aarav, meera, rohan)
    └── EventSynthesizer.kt:  fun synthesize(spec: PersonaSpec, seed: Long): SyntheticHistory   (the ONLY engine file allowed to build timestamps from scratch)
  host/DemoController.kt:     start(personaId), setAsOf(dayIndex), stop(); swaps the stores in AppContainer
  data/SakshiDatabase.kt:     the same Room class built in memory for the demo (a separate instance; the real database file is never touched)
  lib/features/demo/          demo_screen.dart (slider with three presets), demo_banner.dart (permanent "Demo data" label)   (Track 4)

SHARED SURFACES: PersonaSpec and SyntheticHistory (Entities.kt); startDemo/setDemoAsOf/stopDemo in the Pigeon file; every DTO's isDemo field.

FUNCTION & CLASS DESIGN:
  data class WeekTarget(val stretchMin: Double, val staysPerHour: Double, val returnMin: Double, val quietShare: Double, val stoneShare: Double, val opens: Int)
  data class PersonaSpec(val id: String, val name: String, val gentleDefault: Boolean, val studyBlocks: List<StudyBlockSpec>, val workSetPkgs: List<Pkg>, val dependsPkgs: List<Pkg>,
                         val leakPkg: Pkg, val weeks: List<WeekTarget>, val quirks: List<Quirk>, val days: Int = 60, val firstReadDay: Int = 3)
  Quirks (data, not code): REACHED_AFTER_2230 (windows starting after 22:30 are self-started heavy), LATE_NIGHT_SPILLOVER (on 5 of 14 nights the screen runs past 01:30 and the next day's first
     stretch is 6 minutes shorter), PING_APP_LEAK (one app behind most stones), FOOTPRINT_FALL (after day 20 that app's pings fall so S4's experiment resolves MOVED), LISTENER_GAP (a 2-day listener outage
     in week 5), SUNDAY_EVENING_CHOPPY (a cluster cell for P1/clustering).
  EventSynthesizer.synthesize(spec, seed): deterministic. Per study day it places one to two windows (weekday evening block from spec; weekend afternoon), then fills each window with segments so
     the REAL engine, run over the result, lands on the week's targets: number of stays = round(P × window-hours) (deterministic, no random counts); stay durations and returns drawn from a seeded
     ±15 % jitter whose mean is forced to the target by adjusting the last element (so means are exact); stone share by placing a ping 5–25 s before that fraction of stays; quiet share by screen-off
     spans; opens by own-package intervals placed outside windows. Output: RawEvents (including own package), NotifEvents (category "msg"), ListenerSessions, StudyHours.
  Aarav's weeks (from DOC 1 §1.4.3): weeks 1–2 = the starting normal; weeks 3 to 8 interpolate to the week-4 and week-8 targets.
     normal (wk 1–2): stretch 10, stays 4.0, return 6, quiet 0.36, stone 0.75, opens 5
     week 4:          stretch 12, stays 3.5, return 5, quiet 0.38, stone 0.75, opens 3
     week 8:          stretch 14, stays 3.0, return 4, quiet 0.41, stone 0.50, opens 2
     expected Steadiness: 100, 116, 136 (DOC 1 worked example).
  Meera (gentle default): shorter stretches (8 → 10), more quiet (0.50), few stays, panic-checking pattern (many glances); proves gentle mode and the return line.
  Rohan (Depends-heavy, tab-hopper): YouTube and Chrome as DEPENDS; the LISTENER_GAP quirk makes the PARTIAL_PING sentence appear; proves Ask now and the honest data states.
  DemoController:
    start(personaId): build SyntheticHistory, create the in-memory database, write events/sessions/settings/work-set/study hours, set first_read_at to firstReadDay, flag isDemo = true in AppContainer,
        run engine.processNewEvents at dayIndex firstReadDay (Day 1). Real stores are untouched and remain registered.
    setAsOf(dayIndex): DerivedStore.clearDerived(); Clock.now() = start of (study day dayIndex + 1) 04:00 minus 1 minute; engine.processNewEvents(asOf). The engine never reads events after asOf
        (PropertyTest), so moving the slider backwards is correct. Presets: Day 1 = day 3, Week 4 = day 31, Week 8 = day 59.
    stop(): drop the demo stores and database; redraw the real Lake; isDemo = false.
  While a demo is active: the periodic worker skips widget refresh; Pause/Export/Delete are rejected (DEMO_ACTIVE); the widget phrase carries "(demo)".

INTERFACES & CONTRACTS: startDemo(personaId: String), setDemoAsOf(dayIndex: int), stopDemo(); every DTO carries isDemo and the UI shows "Demo data" whenever it is true.
  The demo exercises the same use cases as a real phone; there is no demo-only branch inside the engine except the injected Clock and Randomness.

ERROR HANDLING STRATEGY: an unknown personaId ⇒ HostError BAD_REQUEST. If synthesis fails (a bug), startDemo returns an error and the real database stays untouched.

EDGE CASES TO HANDLE:
  - The user leaves the app mid-demo: the in-memory database is gone on process death; the app restarts in real mode (never a silently persisted demo).
  - setAsOf before firstReadDay: clamps to firstReadDay.
  - The phone's real clock vs the demo clock: the demo uses the injected Clock only; no code in the demo path reads System time (DependencyRuleTest covers engine/).

PERFORMANCE CONSIDERATIONS: synthesis of 60 days × about 1.5 windows is thousands of events; build once at start (a second or two on the iQOO Z7) and cache; each slider step recomputes derived rows for at most 8 weeks.

TESTING PLAN:
  Unit:        synthesize(aarav, seed) twice ⇒ identical lists; event counts within the 10,000/day ceiling
  Persona replay (the DOC 1 §1.2.5 check): processNewEvents at Day 1 ⇒ provisional Mirror, no Steadiness; Week 4 ⇒ Steadiness 116 ± 6, word Steadier, each part within ±15 % of target;
               Week 8 ⇒ 136 ± 6, opens meter 5 → 3 → 2, P1/P4/P5 patterns present, a MOVED verdict from the PING_APP_LEAK experiment, a known-bad hour window labelled REACHED or PINGED (never HELD)
  Property:    events after asOf never change the output; setAsOf(0..59) in any order gives identical results per day
  E2E:         run the three beats of DOC 1 §1.4.4 on the Nothing Phone 3a and the iQOO Z7
```

---

## Pigeon Bridge and Fake Host

```
FEATURE: Pigeon Bridge and Fake Host

MODULE STRUCTURE:
  pigeons/sakshi_api.dart                     the contract (below)
  lib/gen/sakshi_api.g.dart · android/.../host/gen/SakshiApi.g.kt    generated: `dart run pigeon --input pigeons/sakshi_api.dart`
  lib/host/host_client.dart                   abstract class HostClient + class HostException(code, userMessage)
  lib/host/pigeon_host_client.dart            PigeonHostClient implements HostClient (maps PlatformException/FlutterError → HostException)
  lib/host/fake_host.dart                     FakeHost implements HostClient (state machine over fixtures; also used by the web-less dev loop)
  android/.../host/HostApiImpl.kt             implements the generated SakshiHostApi; thin: validate, call the engine or an adapter, map to DTOs
  android/.../host/Mappers.kt                 engine views → DTOs

SHARED SURFACES: the Pigeon file (Track 1 writes; Tracks 3 and 4 read; changing it follows the Contract Change Process); fixtures (split by track).

FUNCTION & CLASS DESIGN:
  HostApiImpl rules: no business logic; each method is at most a few lines: permission/gate check → engine or adapter call → mapper.
  Long operations (syncNow, startDemo, exportData) run on a background dispatcher and return through Pigeon's async callback; the UI thread never blocks.
  FakeHost: holds in-memory state (setup done?, gentle?, demo?), returns the fixture DTOs for each data state; syncNow() waits 400 ms and returns ok.
     Fixtures: setup_fixtures.dart (Track 3) provides SetupState variants and the app list; mirror_fixtures.dart (Track 4) provides MirrorDto variants:
     provisional/firstLook, learning, steady, steadier, wavering, gentle, partialPing, pingOff, notSeen, tooLittle, unusualWeek, nothingToFix, withSuggestion, withVerdict, afterLapse, demoAarav.
  Error codes (FlutterError.code): NO_PERMISSION · DEMO_ACTIVE · BAD_REQUEST · STALE_SUGGESTION · REANCHOR_NOT_ALLOWED · EXPORT_FAILED · INTERNAL.
     Each carries a plain user message from a fixed set (in Kotlin) and a developer detail string (logged locally; never a package name).

INTERFACES & CONTRACTS — THE FILE (LC-4):
```

```dart
// pigeons/sakshi_api.dart — LOCKED CONTRACT LC-4. Pull-only, request/response, no streams.
// [VERIFY: option names and generic-nullability style against the Pigeon version pinned at project start; adapt mechanically.]
import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(PigeonOptions(
  dartOut: 'lib/gen/sakshi_api.g.dart',
  kotlinOut: 'android/app/src/main/kotlin/com/kleos/sakshi/host/gen/SakshiApi.g.kt',
  kotlinOptions: KotlinOptions(package: 'com.kleos.sakshi.host.gen'),
))

// ---------- enums ----------
enum UserClassDto { inSet, depends }
enum SyncStateDto { ok, partialPingAwareness, gapNotSeen, paused, noPermission }
enum DataStateDto { ok, learningBaseline, tooLittleData }
enum DataFlagDto { pingOff, partialPing, notSeen, paused, tooLittleData, unusualWeek, internalPartial, firstLook }
enum LakeStateDto { learning, noData, still, rippled, choppy }
enum GoalAnswerDto { yes, partly, notYet }

// ---------- setup ----------
class CollectionHealthDto {
  CollectionHealthDto({required this.workerRuns7d, required this.paused, this.lastWorkerRunEpochMs, this.listenerCoverage7d, this.lastError});
  int workerRuns7d; bool paused; int? lastWorkerRunEpochMs; double? listenerCoverage7d; String? lastError;
}
class SetupStateDto {
  SetupStateDto({required this.usageAccessGranted, required this.notificationAccessGranted, required this.restrictedSettingsSuspected,
      required this.workSetSaved, required this.studyHoursSaved, required this.batteryHelperShown, required this.weeklyNoteEnabled,
      required this.gentleMode, required this.isDemo, required this.health});
  bool usageAccessGranted; bool notificationAccessGranted; bool restrictedSettingsSuspected;
  bool workSetSaved; bool studyHoursSaved; bool batteryHelperShown; bool weeklyNoteEnabled; bool gentleMode; bool isDemo;
  CollectionHealthDto health;
}
class AppDto {
  AppDto({required this.pkg, required this.label, required this.suggestedInSet, required this.suggestedDepends});
  String pkg; String label; bool suggestedInSet; bool suggestedDepends;
}
class WorkSetEntryDto { WorkSetEntryDto({required this.pkg, required this.userClass}); String pkg; UserClassDto userClass; }
class SaveResultDto { SaveResultDto({required this.ok, required this.savedCount, this.userMessage}); bool ok; int savedCount; String? userMessage; }
class StudyBlockDto { StudyBlockDto({required this.startMinute, required this.endMinute}); int startMinute; int endMinute; } // minutes from local midnight; end < start crosses midnight
class StudyHoursDto { StudyHoursDto({required this.blocks, required this.learnForMe}); List<StudyBlockDto> blocks; bool learnForMe; }

// ---------- read: Mirror and friends ----------
class SyncStatusDto { SyncStatusDto({required this.state, this.lastSyncEpochMs, this.message}); SyncStateDto state; int? lastSyncEpochMs; String? message; }
class PartLinesDto {
  PartLinesDto({this.stretch, this.stays, this.ret, this.quiet});
  String? stretch; String? stays; String? ret; String? quiet;           // `ret` because `return` is reserved
}
class PartsDto {
  PartsDto({this.stretchMin, this.longestStretchMin, this.inSetShare, this.quietShare, this.staysPerHour, required this.glances, this.returnMin,
      required this.lines, required this.extrasLines});
  double? stretchMin; double? longestStretchMin; double? inSetShare; double? quietShare; double? staysPerHour; int glances; double? returnMin;
  PartLinesDto lines; List<String> extrasLines;
}
class SteadinessDto { SteadinessDto({required this.value, required this.word}); int value; String word; }     // Wavering | Steady | Steadier
class StonesDto {
  StonesDto({required this.totalStays, required this.stoneCount, required this.selfStartedCount, required this.unknownCount, this.topStoneLabel, this.noRippleRate, required this.line});
  int totalStays; int stoneCount; int selfStartedCount; int unknownCount; String? topStoneLabel; double? noRippleRate; String line;
}
class ClearHourDto { ClearHourDto({required this.startHour, required this.endHour, required this.stretchMin, required this.line}); int startHour; int endHour; double stretchMin; String line; }
class PatternLineDto { PatternLineDto({required this.kindId, required this.line, required this.evidenceWindows, required this.evidenceDays}); String kindId; String line; int evidenceWindows; int evidenceDays; }
class SuggestionDto {
  SuggestionDto({required this.kindId, required this.line, required this.actionLabel, required this.actionType, required this.opensSettings, this.subjectKey});
  String kindId; String line; String actionLabel; String actionType; bool opensSettings; String? subjectKey;
}
class ObservationDto { ObservationDto({required this.kindId, required this.line}); String kindId; String line; }
class VerdictDto { VerdictDto({required this.verdict, required this.line, required this.approxMix, this.beforeValue, this.afterValue}); String verdict; String line; bool approxMix; double? beforeValue; double? afterValue; }
class GoalTapDto { GoalTapDto({required this.offered, this.answer}); bool offered; GoalAnswerDto? answer; }
class TeacherDto { TeacherDto({required this.opensThisWeek, required this.minutesThisWeek, required this.line, this.opensPrevWeek}); int opensThisWeek; double minutesThisWeek; String line; int? opensPrevWeek; }
class SayingDto { SayingDto({required this.id, required this.text, required this.source, required this.tierLabel, required this.question}); String id; String text; String source; String tierLabel; int question; }

class MirrorDto {
  MirrorDto({required this.isDemo, required this.provisional, required this.gentle, required this.weekStartEpochMs, required this.weekLabel, required this.dataState,
      required this.dataFlags, required this.dataLines, required this.headline, required this.patterns, required this.nothingToFix, required this.goalTap, required this.reanchorOffered,
      this.parts, this.steadiness, this.stones, this.clearHour, this.suggestion, this.observation, this.verdict, this.teacher, this.lapseLine, this.saying, this.returnLine, this.suggestedStudyBlock});
  bool isDemo; bool provisional; bool gentle; int weekStartEpochMs; String weekLabel; DataStateDto dataState;
  List<DataFlagDto> dataFlags; List<String> dataLines; String headline;
  PartsDto? parts; SteadinessDto? steadiness; StonesDto? stones; ClearHourDto? clearHour;
  List<PatternLineDto> patterns; SuggestionDto? suggestion; ObservationDto? observation; bool nothingToFix;
  VerdictDto? verdict; GoalTapDto goalTap; TeacherDto? teacher; String? lapseLine; SayingDto? saying; String? returnLine;
  bool reanchorOffered; StudyBlockDto? suggestedStudyBlock;   // v1.1 CA-1
}
class WeekRefDto { WeekRefDto({required this.weekStartEpochMs, required this.label, required this.completed}); int weekStartEpochMs; String label; bool completed; }
class TodayWindowDto { TodayWindowDto({required this.startEpochMs, required this.endEpochMs, required this.stays, this.shape, this.stretchMin, this.returnMin}); int startEpochMs; int endEpochMs; int stays; String? shape; double? stretchMin; double? returnMin; }
class TodayDto { TodayDto({required this.isDemo, required this.windows, required this.line, required this.dataFlags, required this.dataLines, this.parts}); bool isDemo; List<TodayWindowDto> windows; String line; List<DataFlagDto> dataFlags; List<String> dataLines; PartsDto? parts; }
class WhatISeeDto {
  WhatISeeDto({required this.isDemo, required this.usageAccessGranted, required this.notificationAccessGranted, required this.rawEventCount, required this.notifEventCount,
      required this.derivedDays, required this.workerRuns7d, required this.paused, required this.oddEventPairs, required this.lines, this.oldestRawEpochMs, this.listenerCoverage7d, this.lastWorkerRunEpochMs, this.lastError});
  bool isDemo; bool usageAccessGranted; bool notificationAccessGranted; int rawEventCount; int notifEventCount; int derivedDays; int workerRuns7d; bool paused; int oddEventPairs;
  List<String> lines; int? oldestRawEpochMs; double? listenerCoverage7d; int? lastWorkerRunEpochMs; String? lastError;
}
class LakeDto { LakeDto({required this.state, required this.phrase, required this.isDemo, this.asOfEpochMs}); LakeStateDto state; String phrase; bool isDemo; int? asOfEpochMs; }
class ExportDto { ExportDto({required this.fileName, required this.byteSize}); String fileName; int byteSize; }

// ---------- the host API ----------
@HostApi()
abstract class SakshiHostApi {
  // setup
  @async SetupStateDto getSetupState();
  @async void openUsageAccessSettings();
  @async void openNotificationAccessSettings();
  @async void openAppInfoForRestrictedSettings();
  @async void openBatterySettings();                       // opens Android's own page; never requests the exemption
  @async void markBatteryHelperShown();
  @async List<AppDto> listLauncherApps();
  @async SaveResultDto saveWorkSet(List<WorkSetEntryDto> entries);
  @async void saveStudyHours(StudyHoursDto hours);
  @async void setGentleMode(bool on);
  @async void setUnder18(bool on);
  @async bool setWeeklyNote(bool enabled);                 // returns the effective state (false if the permission was denied)
  // read (all pull)
  @async SyncStatusDto syncNow();
  @async MirrorDto getMirror(int? weekStartEpochMs);       // null ⇒ latest completed week, or the First Look
  @async List<WeekRefDto> listMirrorWeeks();
  @async TodayDto getTodaySoFar();
  @async WhatISeeDto getWhatISee();
  @async List<SayingDto> getSayingChoices();
  @async LakeDto getLake();
  // write (rare, user-initiated, one tap each)
  @async void pickSaying(String sayingId);
  @async void tapTryThis(String kindId, String? subjectKey);
  @async void dismissSuggestion(String kindId, String? subjectKey);
  @async void tapGoal(GoalAnswerDto answer);
  @async void reanchorBaseline();
  @async void pause(bool on);
  @async ExportDto exportData(bool includeRaw);            // writes the file and opens the Android share sheet
  @async void deleteEverything();
  // demo
  @async void startDemo(String personaId);                 // "aarav" | "meera" | "rohan"
  @async void setDemoAsOf(int dayIndex);                   // 0..59
  @async void stopDemo();
}
```

```
  HostClient (lib/host/host_client.dart): one abstract method per Pigeon method, same names, same parameter and return types (DTO classes imported from lib/gen). PigeonHostClient and FakeHost
  both `implements HostClient`; a Dart compile error is therefore the contract test for a missing or renamed method.

ERROR HANDLING STRATEGY:
  Kotlin throws FlutterError(code, userMessage, devDetail). PigeonHostClient converts to HostException(code, userMessage). Screens show userMessage in a snackbar or inline; devDetail is logged only.
  Never swallow: every provider that calls the host returns AsyncValue and each screen renders its error state (a plain sentence and a retry button).
  Not-granted, partial and not-seen are VALUES in DTOs, never exceptions (DOC 2 §2.4.2).

EDGE CASES TO HANDLE:
  - Hot restart or process death mid-call: Pigeon calls are idempotent reads or single-tap writes; the UI re-fetches on resume.
  - The Activity is not available (widget tap, cold start from the worker): setWeeklyNote and the open*Settings calls return BAD_REQUEST with a message; reads never need an Activity.
  - A DTO field added on one side only: Pigeon regeneration fails the build on the other side (the point of the tool).
  - Time: all timestamps are epoch milliseconds; no DateTime across the bridge; week/day labels are strings from the host.

PERFORMANCE CONSIDERATIONS: DTOs are small (a Mirror is a few KB). Pigeon's standard codec is fine; no streaming.

TESTING PLAN:
  Unit (Kotlin):  Mappers: for each golden MirrorView/TodayView/LakeView/WhatISeeView, the DTO equals a checked-in JSON (shared with Flutter tests)
  Contract (Dart): FakeHost fixtures decode from the same JSON files as the Kotlin goldens (test/fixtures/*.json mirrored in android/app/src/test/resources); every Pigeon method is implemented by HostClient
  Integration:    one round trip per method on a device (getSetupState first: spike S-G)
  Fallback:       if S-G shows Pigeon cannot build, replace gen/ with a hand-written MethodChannel behind HostClient (same signatures); nothing above HostClient changes
```

---

## Flutter App Shell

```
FEATURE: Flutter App Shell

MODULE STRUCTURE:
  lib/main.dart · lib/app.dart
  lib/core/
    ├── theme.dart         Material 3 theme; one calm palette; text styles; dark theme; Devanagari-capable system font for "साक्षी"
    ├── router.dart        go_router; `final routes = [...setupRoutes, ...whatISeeRoutes, ...settingsRoutes, ...mirrorRoutes, ...todayRoutes, ...shelfRoutes, ...demoRoutes];`
    ├── providers.dart     hostClientProvider; setupStateProvider; mirrorProvider(weekStart); todayProvider; lakeProvider; whatISeeProvider; syncProvider (calls syncNow, then invalidates the others)
    ├── ui_strings.dart    chrome-only strings: titles, button labels, permission explanations, the four-line first screen, the Demo-data banner text
    └── widgets/           shared small widgets (SectionCard, DataFlagLine, PrimaryButton); DemoBanner lives in lib/features/demo/demo_banner.dart (v1.1 CA-2)
  lib/features/<feature>/  each exports `<feature>Routes` (a List<RouteBase>) and owns its own widgets
  test/                    widget tests, contract tests, copy_rules_test.dart

SHARED SURFACES (the likely merge conflicts, with one owner each; DOC 4 turns this into the ownership map):
  lib/core/*              Track 3 owns. Track 4 may only add a widget file under lib/core/widgets/ with a name prefixed by its feature; edits to theme/router/providers go through Track 3.
  lib/features/mirror/*   Track 4 owns. Track 3's First Look screen imports ONLY `features/mirror/parts_card.dart` and `features/mirror/mirror_content.dart`, both of which Track 4 publishes early.
  lib/host/fixtures/*     split: setup_fixtures.dart (Track 3), mirror_fixtures.dart (Track 4); fake_host.dart (Track 3) calls both.
  pubspec.yaml            Track 3 owns; dependencies are frozen to: flutter_riverpod, go_router (and pigeon, flutter_test as dev). Adding a package is a contract change.

FUNCTION & CLASS DESIGN:
  Rules for every screen: reads a provider (AsyncValue<Dto>), renders strings it is given, calls a HostClient method on a tap. No arithmetic on DTO numbers beyond layout (a bar width).
  UI states are exhaustive: loading, error (sentence + retry), data, and each F17 flag line (always above the data it qualifies).
  The first screen says what Sakshi will and will not do in four lines (DOC 1 §1.4.1); setup flow order: first screen → usage access → First Look → notification access → work-set → study hours → age tap
  (one tap, skippable) → battery helper (optional, once) → done. Setup is resumable from SetupStateDto (each step checks its own flag).
  Navigation: the Mirror is the home route after setup. There is no dashboard, no tab bar of metrics, no daily number, no badge.
  Lake painter (Track 4): CustomPainter with three states; the same art language as the widget drawables.
  Time Machine screen: three preset chips (Day 1, Week 4, Week 8) plus the slider (0..59); the DemoBanner is mounted in app.dart whenever any DTO's isDemo is true.

INTERFACES & CONTRACTS: depends only on HostClient and the DTOs; provides `hostClientProvider` overrideable in tests with FakeHost.

ERROR HANDLING STRATEGY: a global error boundary shows "Something went wrong reading Sakshi's data" with a retry; HostException(userMessage) is shown verbatim when present.

EDGE CASES TO HANDLE:
  - Return from Android settings: re-fetch getSetupState on app resume (WidgetsBindingObserver), never trust an assumed grant.
  - Rotation and small screens: single-column scroll; the Mirror is one scroll.
  - Text scaling up to 1.3×: no clipped data lines.
  - Hindi: UI chrome strings are in one file so they can be translated later; engine sentences are English for the hackathon [ASSUMPTION].
  - Release build must not log DTO contents.

PERFORMANCE CONSIDERATIONS: providers cache per week; invalidate only after syncNow completes or after a write; no polling, no timers.

TESTING PLAN:
  Unit/widget: each fixture renders on each screen; error and loading states render; copy_rules_test.dart scans lib/**/*.dart string literals for the forbidden words in LC-8 (best-effort regex) and fails on a match
  Integration: setup flow against FakeHost end to end; the First Look screen shows the Mirror parts widget
  E2E:         the real flow on the Nothing Phone 3a with the release APK: sideload, restricted settings, grant, First Look, setup, Mirror
```

---

## Locked Shared Contracts

**Frozen the moment you confirm DOC 3.** Each is a real artifact (a file or a signature), not a description. Tracks: T1 = platform backend (you), T2 = analytics engine, T3 = app shell and trust screens, T4 = Mirror, Lake and demo screens (DOC 2 §2.9).

```
LOCKED CONTRACT LC-1: Event vocabulary and value types
  DEFINITION: engine/model/Values.kt and the fact entities in engine/model/Entities.kt exactly as written in "Event Vocabulary and Tuning":
              Pkg, EpochMs, Minutes, Ratio, StudyDay, WeekStart; RawType, RawEvent; NotifKind, RemovalKind, NotifEvent (no text fields); ListenerSession; GapKind, DataGap.
  USED BY:    T1 (writes them), T2 (reads them), Room entities, golden fixtures

LOCKED CONTRACT LC-2: Ports and derived entities
  DEFINITION: engine/ports/Ports.kt:
      interface Clock            { fun now(): EpochMs }
      interface Randomness       { fun nextDouble(): Double; fun nextInt(bound: Int): Int; fun fork(seed: Long): Randomness }
      interface EventStore       { fun append(events: List<RawEvent>); fun range(from: EpochMs, to: EpochMs): List<RawEvent>; fun oldest(): EpochMs?; fun count(): Int; fun purgeBefore(ts: EpochMs): Int }
      interface NotifStore       { fun append(e: NotifEvent); fun range(from: EpochMs, to: EpochMs): List<NotifEvent>; fun count(): Int; fun purgeBefore(ts: EpochMs): Int }
      interface ListenerCoverage { fun sessions(from: EpochMs, to: EpochMs): List<ListenerSession>; fun coversInterval(from: EpochMs, to: EpochMs): Boolean; fun coverageFraction(from: EpochMs, to: EpochMs): Double
                                   fun openSession(at: EpochMs); fun closeSession(at: EpochMs) }
      interface GapStore         { fun add(gap: DataGap); fun closeOpenPause(at: EpochMs); fun overlapping(from: EpochMs, to: EpochMs): List<DataGap> }
      interface DerivedStore     { fun replaceDay(day: StudyDay, d: DayDerivation); fun windows(from: EpochMs, to: EpochMs): List<WindowWithDetail>; fun days(from: StudyDay, to: StudyDay): List<DaySummary>
                                   fun upsertWeek(w: WeekSummary); fun weeks(): List<WeekSummary>; fun upsertPatterns(p: List<Pattern>); fun patterns(): List<Pattern>; fun clearDerived(); fun clearAll() }
      interface StateStore       { fun baseline(): Baseline?; fun saveBaseline(b: Baseline); fun settings(): Settings; fun saveSettings(s: Settings)
                                   fun apps(): List<AppMeta>; fun replaceApps(a: List<AppMeta>); fun suggestionStates(): List<SuggestionState>; fun saveSuggestionState(s: SuggestionState)
                                   fun experiments(): List<Experiment>; fun saveExperiment(e: Experiment); fun sayingPicks(): List<SayingPick>; fun savePick(p: SayingPick)
                                   fun goalTaps(): List<GoalTap>; fun saveGoalTap(g: GoalTap); fun lake(): LakeRow?; fun saveLake(l: LakeRow); fun note(): NoteState; fun saveNote(n: NoteState)
                                   fun ingest(): IngestState; fun saveIngest(i: IngestState) }
      interface AppCatalog       { fun launcherApps(): List<AppInfo>; fun category(pkg: Pkg): Int?; fun isNeutral(pkg: Pkg): Boolean; fun ownPackage(): Pkg }
      interface SayingShelf      { fun all(): List<Saying> }
      data class Ports(val events: EventStore, val notifs: NotifStore, val coverage: ListenerCoverage, val gaps: GapStore, val derived: DerivedStore, val state: StateStore,
                       val catalog: AppCatalog, val shelf: SayingShelf, val clock: Clock, val random: Randomness)
    Derived and state entities (engine/model/Entities.kt; Room entities mirror these field for field):
      Settings(studyBlocks: List<StudyBlock>, learnStudyHours: Boolean, gentleMode: Boolean, gentleExplicit: Boolean, weeklyNoteEnabled: Boolean, ageUnder18: Boolean, batteryHelperShown: Boolean,
               lapseAcknowledgedThrough: StudyDay?, firstReadAt: EpochMs?, createdAt: EpochMs)
      AppMeta(pkg: Pkg, label: String, systemCategory: Int?, userClass: UserClass, addedAt: EpochMs)           // UserClass = IN_SET | DEPENDS | NONE
      DaySummary(day: StudyDay, valid: Boolean, windowMinutes: Double, quietMinutes: Double, inSetMinutes: Double, coverage: Double, pickups: Int, switchesPerHour: Double?, flinch: Boolean?,
                 rampUpMin: Double?, lastScreenOffTs: EpochMs?, firstStretchMin: Double?, externalResumes: Int)
      Window(id: Long, day: StudyDay, start: EpochMs, end: EpochMs, source: WindowSource, partial: Boolean, finalised: Boolean, shape: Shape?)
      Stretch(id: Long, windowId: Long, start: EpochMs, end: EpochMs, minutes: Double, inSetMinutes: Double, quietMinutes: Double, endedBy: EndedBy)       // STAY | PUT_DOWN | WINDOW_END
      Stay(id: Long, windowId: Long, start: EpochMs, end: EpochMs, firstPkg: Pkg, pkgMain: Pkg, origin: Origin, stonePkg: Pkg?, notifClicked: Boolean, returnMinutes: Double?, glancesBefore: Int)
      WindowWithDetail(window: Window, stretches: List<Stretch>, stays: List<Stay>, quietMinutes: Double, glances: Int)
      DayDerivation(windows: List<WindowWithDetail>, summary: DaySummary)
      WeekSummary(weekStart: WeekStart, stretchMedianMin: Double?, longestStretchMin: Double?, staysPerHour: Double?, returnMedianMin: Double?, quietShare: Double?, inSetShare: Double?,
                  steadiness: Int?, word: String?, windowsCount: Int, validDays: Int, unusual: Boolean, appOpens: Int, appMinutes: Double, returnsAfterLapse: Int)
      Baseline(id: Long, frozenAt: EpochMs, c0: Double, p0: Double, r0: Double, q0: Double, daysUsed: Int, isActive: Boolean)
      Pattern (see Pattern Layer) · SuggestionState(kind: SuggestionKind, subject: Pkg?, firstEligibleAt: EpochMs?, lastShownAt: EpochMs?, shownInWeek: WeekStart?, dismissedUntil: EpochMs?, retiredUntil: EpochMs?, status: String)
      Experiment(id: Long, kind: SuggestionKind, subject: Pkg?, startedAt: EpochMs, startReason: StartReason, target: TargetMetric, beforeValue: Double?, afterValue: Double?, windowEnd: EpochMs, verdict: Verdict, approxMix: Boolean, shown: Boolean)
      SayingPick(id: Long, sayingId: String, pickedAt: EpochMs) · GoalTap(weekStart: WeekStart, answer: GoalAnswer) · LakeRow(state: LakeState, phrase: String, asOf: EpochMs?)
      NoteState(lastNoteWeek: WeekStart?, mirrorReadyWeek: WeekStart?, mirrorViewedWeek: WeekStart?)
      IngestState(cursor: EpochMs?, lastRunAt: EpochMs?, paused: Boolean, pausedSince: EpochMs?, firstReadAt: EpochMs?, lastWorkerRunAt: EpochMs?, workerRuns7d: Int, lastError: String?, oddEventPairs: Int)
  USED BY:    T1 (implements every port in Room; owns the schema), T2 (consumes ports; owns the entities' semantics), golden fixtures. Room destructive migration is allowed in development; the schema freezes at the first device install.

LOCKED CONTRACT LC-3: Engine façade and views
  DEFINITION: engine/SakshiEngine.kt
      class SakshiEngine(ports: Ports) {
        fun processNewEvents(asOf: EpochMs): ProcessReport
        fun mirror(week: WeekStart?, asOf: EpochMs): MirrorView        fun listMirrorWeeks(asOf: EpochMs): List<WeekRef>
        fun today(asOf: EpochMs): TodayView                            fun lake(asOf: EpochMs): LakeView
        fun whatISee(asOf: EpochMs, host: HostFacts): WhatISeeView
        fun saveWorkSet(entries: List<WorkSetEntry>): SaveResult       fun saveStudyHours(h: StudyHours)
        fun setGentle(on: Boolean)   fun setUnder18(on: Boolean)   fun setWeeklyNote(on: Boolean)   fun markBatteryHelperShown()
        fun tapTryThis(kind: SuggestionKind, subject: Pkg?, asOf: EpochMs): TapResult     fun dismissSuggestion(kind: SuggestionKind, subject: Pkg?, asOf: EpochMs)
        fun tapGoal(answer: GoalAnswer, asOf: EpochMs)                 fun reanchor(asOf: EpochMs): ReanchorResult
        fun chooseSayings(asOf: EpochMs): List<Saying>                 fun pickSaying(id: String, asOf: EpochMs)
        fun pause(on: Boolean, asOf: EpochMs)                          fun export(includeRaw: Boolean, asOf: EpochMs): ExportDocument
        fun deleteEverything()                                         fun noteDecision(asOf: EpochMs, host: HostFacts): NoteDecision
      }
      data class HostFacts(val usageAccessGranted: Boolean, val notificationAccessGranted: Boolean, val canPostNotifications: Boolean, val lastError: String?, val oddEventPairsUnknown: Boolean = false)
      Views: MirrorView, PartsView, SteadinessView, StonesView, ClearHourView, PatternLine, SuggestionView, ObservationView, VerdictView, GoalTapView, TeacherView, SayingView, StudyBlockView (F6);
             TodayView, TodayWindowView (F8); LakeView(state, phrase, asOf) (F7); WhatISeeView (F10); WeekRef(weekStart, label, completed).
      Every View is a plain immutable data class of primitives, Strings, lists, enums and other Views.
  USED BY:    T1 (HostApiImpl and Mappers call it), T2 (implements it), T3/T4 indirectly through the DTOs

LOCKED CONTRACT LC-4: The Pigeon file
  DEFINITION: pigeons/sakshi_api.dart exactly as written in "Pigeon Bridge and Fake Host", plus the HostClient abstract class and the error codes
              NO_PERMISSION, DEMO_ACTIVE, BAD_REQUEST, STALE_SUGGESTION, REANCHOR_NOT_ALLOWED, EXPORT_FAILED, INTERNAL.
  USED BY:    T1 (implements), T3 and T4 (call), fixtures, golden JSON files

LOCKED CONTRACT LC-5: The Saying shelf file
  DEFINITION: android/app/src/main/assets/sakshi/sayings.json: an array of { id, q, text, source, tier, usedFor } (27 entries, delivered with this document); tier ∈ A|B|C|D;
              the engine's tierLabel mapping (A "his own writing or letter", B "recorded lecture", C "reported by others", D "type not resolved in the Outcome Map").
  USED BY:    T4 (owns the file and the UI), T1 (SayingShelfImpl), T2 (ChooseSayings)

LOCKED CONTRACT LC-6: Persona specification
  DEFINITION: PersonaSpec, WeekTarget, Quirk and the targets for aarav, meera and rohan as written in "Time Machine Demo and Personas"; Presets Day 1 = day 3, Week 4 = day 31, Week 8 = day 59.
  USED BY:    T2 (writes the synthesizer and replay), T1 (DemoController), T4 (the slider and presets)

LOCKED CONTRACT LC-7: Tuning names
  DEFINITION: the constant NAMES in engine/tuning/Tuning.kt. Values are tunable by the face-validity check without a contract change; adding, renaming or removing a name is a contract change.
  USED BY:    T2 only (the engine); documented for T1 (RAW_RETENTION_DAYS, ASSUMED_PLATFORM_RETENTION_DAYS, STUDY_DAY_START_HOUR are read by the host through the engine, not duplicated)

LOCKED CONTRACT LC-8: Copy rules
  DEFINITION: all user-facing sentences about the user's data are produced by SentenceBuilder and nowhere else; the forbidden pattern
              \b(focused|distracted|distraction|wasted|waste|failed|failure|streak|lazy|addict\w*|ruin\w*|lost (your )?(focus|concentration)|you (should|must|need to))\b and the "!" ban;
              Flutter owns chrome strings only (lib/core/ui_strings.dart).
  USED BY:    T2 (SentenceBuilder), T3 and T4 (ui_strings and widgets), CopyRulesTest and copy_rules_test.dart

LOCKED CONTRACT LC-9: Host-side file and class names that other tracks rely on
  DEFINITION: MainActivity registers the Pigeon host; AppContainer is the only wiring point; drawable names lake_still, lake_rippled, lake_choppy; notification channel id "weekly_mirror";
              the widget provider class com.kleos.sakshi.host.LakeWidget.
  USED BY:    T1 (owns), T4 (adds drawables with those exact names)
```

### Contract Change Process

A locked contract will turn out to be wrong mid-build. The point is to make that a deliberate act:

1. **The track that needs the change proposes the new definition** (in the shared channel, as a diff to this document's DEFINITION) and names the affected tracks from `USED BY`.
2. **Every affected track's owner is notified before the change lands**, not after. Silence is not agreement for LC-2, LC-3 and LC-4, which the Integration Owner (you) must approve.
3. **This document's DEFINITION is updated**, and that update is itself a Sync Point in DOC 4: any track whose work depends on the contract waits for it, then rebases.
4. **Pigeon changes** (LC-4) are made by Track 1 only, regenerated in the same commit as the Kotlin implementation, the Dart `HostClient` and both fixture sets. A change that compiles on one side only is not merged.
5. **Agents never change a locked contract on their own.** If an agent says "I'll just add a field", the answer is: stop, and propose it through steps 1 to 3.

---

**⛔ GATE:** Does the module and coding architecture match what you want? Any feature's design, any decision in the table at the top (D1 to D13), or any locked contract to revisit? Once you confirm DOC 3, I will write DOC 4: the four tracks with their steps, sync points, merge strategy, descope order, agentic rules and the deployment checklist, then DOC 5 and AGENTS.md.
