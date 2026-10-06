## Table of Contents
- §4.0 — Project Context Snapshot
- Track 1 — Platform Backend (Primary)
  - Step T1.1 — Skeleton and Spike S-G
  - Step T1.2 — Lock the Contracts Into Code
  - Step T1.3 — Manifest, Permissions and PermissionGateway
  - Step T1.4 — Room Database and Port Stores
  - Step T1.5 — UsageEventsSource and the Ingest Orchestrator
  - Step T1.6 — NotificationCollector and Listener Coverage
  - Step T1.7 — IngestWorker and Scheduling
  - Step T1.8 — AppCatalog and SayingShelf Adapters
  - Step T1.9 — HostApiImpl and Mappers
  - Step T1.10 — Lake Widget
  - Step T1.11 — Weekly Note Notifier
  - Step T1.12 — Battery Helper
  - Step T1.13 — Pause, Export, Delete and DemoController
  - Step T1.14 — Hostile-OEM Spike Pass
  - Step T1.15 — Real Engine on the Real Phone
  - Step T1.16 — Release Build and Manifest Audit
- Track 2 — Analytics Engine
  - Step T2.1 — Engine Test Kit and Tuning
  - Step T2.2 — Foreground Intervals
  - Step T2.3 — App Classifier and Depends Resolution
  - Step T2.4 — Window Finder
  - Step T2.5 — Stay Detector, Returns and Stretches
  - Step T2.6 — Stone and Wave
  - Step T2.7 — Parts, Steadiness and Baseline
  - Step T2.8 — Engine Core: processNewEvents
  - Step T2.9 — Data States, Lapse and Teacher Meter
  - Step T2.10 — Patterns P1 to P3
  - Step T2.11 — Patterns P4, P5, Cross-Day and Clustering
  - Step T2.12 — Suggestion Rules and Selector
  - Step T2.13 — Judging, Footprint and Suggestion Taps
  - Step T2.14 — SentenceBuilder and Gentle Policy
  - Step T2.15 — Views, Lake, Today, Note, Export, Sayings and the Façade
  - Step T2.16 — Personas and Persona Replay
  - Step T2.17 — Face-Validity and Tuning Pass
- Track 3 — App Shell and Trust Screens
  - Step T3.1 — Flutter Shell
  - Step T3.2 — HostClient and FakeHost
  - Step T3.3 — Setup Flow Screens
  - Step T3.4 — Work-Set, Study Hours and Age Tap
  - Step T3.5 — Settings, Battery Helper and Weekly Note Tile
  - Step T3.6 — What I See Screen
  - Step T3.7 — First Look Screen and Copy Test
- Track 4 — Mirror, Lake and Demo Screens
  - Step T4.1 — Mirror Fixtures and Shared JSON
  - Step T4.2 — Mirror Content Widgets
  - Step T4.3 — Mirror Screen
  - Step T4.4 — Today Screen, Lake Painter and Lake Art
  - Step T4.5 — Saying Shelf UI
  - Step T4.6 — Time Machine Screen
  - Step T4.7 — Demo Flow and Rehearsal Pass
- §4.1a — Sync Points
- §4.1b — Ownership and Merge Strategy
- §4.1c — Descope Order
- §4.2 — Agentic Coding Rules
- §4.3 — Integration Checkpoints
- §4.4 — Deployment Checklist

---

# Sakshi (साक्षी): Vibecoding Build Guide (DOC 4)

**Builds on:** DOC 1 v7.1, DOC 2 v3, DOC 3 **v1.1** (confirmed, contracts locked; v1.1 adds `reanchorOffered` and `suggestedStudyBlock` to the Mirror, one `DemoBanner` location, the mirror-viewed write and the `getInitialRoute` note: see its amendment table). This is the executable plan. It names tracks, steps, sync points, ownership and the rules every agent session must follow. It does not repeat DOC 3's designs; each step points to the DOC 3 section that holds the design and the golden examples.

**Tags:** `[ASSUMPTION: …]` is a call I made that you can overrule. `[VERIFY ON DEVICE: …]` is settled by a device step.

**Size tags** (S, M, L) are *relative* effort between steps, an ESTIMATE, not hours.

**The team (from you):** four builders, one laptop each, tracks merged at the end.

| Track | Owner | Claude plan | What that means for how steps are written |
|---|---|---|---|
| Track 1: Platform Backend (primary) | You, **Integration Owner** | Pro | Long agent sessions; agentic coding with tests and builds run by the agent. `[ASSUMPTION: Pro includes Claude Code on your laptop, so the agent can run gradlew and flutter commands itself.]` |
| Track 2: Analytics Engine | Teammate B | Pro | Same. Pure Kotlin with local JUnit tests, so the agent can run its own verifier. |
| Track 3: App Shell and Trust Screens | Teammate C | Free | Small steps: one prompt, one to three files, the two excerpts to paste are named. The human runs `flutter run` and pastes errors back. |
| Track 4: Mirror, Lake and Demo Screens | Teammate D | Free | Same as Track 3. |

**Executor tags:** **Coding agent (Pro)** = Claude with code execution on your laptop. **Coding agent (free)** = Claude in chat, no execution; the human is the compiler. **Human** = a person doing what an agent cannot. **Research agent** = paste-ready prompt for Grok or similar; four prompts exist, all inside Track 1, because that track meets the real phones.

**One rule above all:** a step is done when its **Evidence** is on the table and someone other than the maker has looked at it (Maker ≠ Checker). Track 1's owner checks Tracks 3 and 4; Track 2's owner checks Track 1's ingest tests and vice versa; you check the merges.

---

## §4.0 — Project Context Snapshot

Paste this block at the start of every agent session, followed by the one step entry you are working on and nothing else.

```
PROJECT: Sakshi (साक्षी)
TAGLINE: A passive witness for a student's attention: it shows how you stay and how you return, never controls you.

WHAT WE'RE BUILDING:
  An Android-only app (minSdk 29) for students. It reads what Android already records (app foreground events, screen on/off, and the app,
  time and category of notifications, never their text), turns that into windows, stretches, stays and returns against the user's OWN starting
  normal, and shows a weekly Mirror with one plain suggestion at most, judged two weeks later by the user's own data. A home-screen Lake widget,
  a "What I see" privacy page, and a synthetic-data Time Machine demo. No accounts, no cloud, no INTERNET permission, no blocking.

STACK:
  UI:         Flutter (Dart), flutter_riverpod (manual providers), go_router. No other packages.
  Bridge:     Pigeon (generated typed host API). Pull-only. File: pigeons/sakshi_api.dart
  Native:     Kotlin in ONE Android module. Room (SQLite, KSP), WorkManager, native AppWidgetProvider (RemoteViews), NotificationListenerService,
              kotlinx.serialization, manual DI (AppContainer). No Hilt, no kapt.
  Engine:     pure Kotlin package com.kleos.sakshi.engine (NO android imports), JUnit 4 local tests.
  Distribution: signed release APK, sideloaded.

ARCHITECTURE IN ONE LINE:
  Flutter screens (no logic) → Pigeon host API → Kotlin host + Room data layer → pure Kotlin engine, where Mirror = f(raw events, notifications,
  listener coverage, settings, as-of clock).

CORE PRINCIPLES TO FOLLOW:
  - All logic about attention lives in engine/. Collectors, widget, host and Flutter never decide anything about attention.
  - The Dependency Rule is a test: engine/ may not import android.*, androidx.*, host.*, data.*, io.flutter.*, and may not read time or randomness except through the Clock and Randomness ports.
  - Locked contracts (DOC 3: LC-1 to LC-9) are never edited by an agent. Propose a change; do not make it.
  - Every number from DOC 1 lives once, in engine/tuning/Tuning.kt.
  - All user-facing sentences come from SentenceBuilder (Kotlin). Flutter holds chrome strings only.
  - "Unknown" and "not seen" are values, never zeros.
  - A step is done only when its Done-when test output is shown, not when the agent says so.
  - NotifEvent has no text field and none may ever be added.

MVP FEATURES (in build order):
  1. Collectors + background ingestion (usage events, notification listener, WorkManager)
  2. Foreground intervals, app classification, window finder
  3. Stays, returns, stone and wave
  4. Steadiness, four parts, baseline
  5. Instant First Look and the Weekly Mirror (F1, F6)
  6. The Lake widget (F7), required in the demo
  7. Honest data states, lapse, Teacher Leaves meter (F17, F15, F14)
  8. Pattern layer P1–P5, cross-day link, optional clustering
  9. Suggestions engine S1–S12 and self-judging verdicts (F11, F12)
 10. Work-set setup, study hours, gentle mode (F2, F16)
 11. What I see: Pause, Export, Delete (F10)
 12. Saying shelf (F13), Ask now (F8), optional quiet note (F9), battery helper
 13. Time Machine demo with synthetic personas
```

---

## Track 1 — Platform Backend (Primary)

```
TRACK 1: Everything that touches Android, the database, the bridge and the phones; owns integration.
  OWNER:       You (Integration Owner). Executor: Coding agent (Pro), plus Human for device steps.
  DEPENDS ON:  None. Steps T1.1 and T1.2 come first and gate everyone (Sync 1).
  STEPS:       16, mostly sequential; T1.3–T1.8 can overlap once T1.2 is merged.
  PATHS:       android/app/src/main/kotlin/com/kleos/sakshi/host/, .../data/, MainActivity.kt, AndroidManifest.xml, res/, pigeons/, android/app/src/test/.../data and .../arch
```

### Step T1.1 — Skeleton and Spike S-G

- **Reference:** DOC 3 → *Project Skeleton and Folder Structure*; DOC 2 §2.2 (stack, vibe rules), §2.10 (spike S-G).
- **Executor:** Coding agent (Pro), with one Research-agent prompt (below).
- **Size:** M.
- **What to build:** `flutter create sakshi --org com.kleos`. In `android/`, add Room with KSP, WorkManager, kotlinx-coroutines, kotlinx-serialization, JUnit 4 via the version catalog (`libs.versions.toml`); minSdk 29. Add `pigeon` to `pubspec.yaml` dev dependencies, plus `flutter_riverpod` and `go_router`. Create the folder tree from DOC 3 with empty placeholder files. Write `pigeons/sakshi_api.dart` containing only one class and one method (`getSetupState` returning a trivial DTO), generate it, implement it in `HostApiImpl`, register in `MainActivity`, call it from `main.dart` on a button. Write `DependencyRuleTest.kt` (rules in DOC 3) and one trivial engine unit test. Pin every version; record them in `docs/versions.md`.
- **Folder/file targets:** `pubspec.yaml`, `pigeons/`, `lib/main.dart`, `android/app/build.gradle(.kts)`, `android/gradle/libs.versions.toml`, `android/app/src/main/kotlin/com/kleos/sakshi/…`, `android/app/src/test/kotlin/com/kleos/sakshi/arch/DependencyRuleTest.kt`, `docs/versions.md`.
- **Agent prompt hint:** "Create the project skeleton exactly per the DOC 3 folder tree. One Pigeon round trip and one JUnit test only. Do not restructure the Flutter template, do not add libraries beyond the list, do not upgrade anything once it builds."
- **Your job — alongside:** run the Research prompt first and hand the agent the pinned versions. Plug in the Nothing Phone 3a with USB debugging.
- **Done when:** `flutter build apk --debug` succeeds; `./gradlew testDebugUnitTest` (run in `android/`) is green including the DependencyRuleTest; pressing the button on the real phone shows the Pigeon reply.
- **Evidence required:** the two command outputs, a photo or screenshot of the phone reply, `docs/versions.md`, and the diff listing only skeleton files.
- **Common drift:** the agent "fixes" a Gradle error by adding libraries, restructuring modules, or upgrading versions. Say: "Fix in place. No new libraries, no module split. Show me the error and the smallest change."

```
RESEARCH AGENT PROMPT (R1): "As of today, list the latest STABLE versions and the known-compatible combination for an Android + Flutter project:
  Flutter stable, Dart, Android Gradle Plugin, Kotlin, KSP (for that Kotlin), Room, WorkManager, kotlinx-coroutines, kotlinx-serialization, Pigeon (dart pub), flutter_riverpod, go_router.
  For Pigeon give the exact PigeonOptions fields for kotlinOut and kotlinOptions(package), whether generic lists must be written List<T?>, and how @async is expressed for the Kotlin output.
  Give source URLs and the date of each source. Output a table plus a 10-line 'known pitfalls' list for Flutter + Room/KSP + Pigeon in one Android module. Do not guess; mark anything unverified."
OUTPUT LANDS AT: docs/research/versions.md
CONSUMED BY:     Track 1 Step T1.1 (versions to pin), Track 3 Step T3.1 (Dart package versions)
```

### Step T1.2 — Lock the Contracts Into Code

- **Reference:** DOC 3 → *Locked Shared Contracts* (LC-1 to LC-9), *Event Vocabulary and Tuning*, *Pigeon Bridge and Fake Host*.
- **Executor:** Coding agent (Pro). **Reason:** mechanical transcription of confirmed contracts into compiling code; this is the pivot that unblocks three tracks.
- **Size:** M.
- **What to build:** Transcribe verbatim: `engine/model/Values.kt`, the fact entities and all derived/state entities in `Entities.kt` (LC-1, LC-2), `engine/ports/Ports.kt` (LC-2), `engine/model/Views.kt` and the façade `SakshiEngine.kt` (LC-3) with **minimal canned implementations** (each method returns an empty-but-valid view with `provisional = true` or a no-op) so everything compiles and Track 1 can proceed. Replace the Pigeon file with the full `pigeons/sakshi_api.dart` (LC-4); regenerate Dart and Kotlin; make `HostApiImpl` compile with each method returning canned DTOs. Add `Tuning.kt` with every constant name and value from DOC 3 (LC-7). Copy `sayings.json` to `android/app/src/main/assets/sakshi/` and `tools/qindex.json`. Add `SayingShelfIntegrityTest` and `PrivacyBoundaryTest`. Put `lib/host/host_client.dart` (abstract class, 1:1 with Pigeon) in place. Commit to `main` and tag `contracts-v1`.
- **Folder/file targets:** `android/app/src/main/kotlin/com/kleos/sakshi/engine/{model,ports,tuning}/…`, `engine/SakshiEngine.kt`, `pigeons/sakshi_api.dart`, `lib/gen/…`, `host/gen/…`, `host/HostApiImpl.kt`, `lib/host/host_client.dart`, `assets/sakshi/sayings.json`, `tools/qindex.json`, tests under `…/arch/`.
- **Agent prompt hint:** "Transcribe the locked contracts from DOC 3 into code exactly as written. Do not rename, add or remove any field, port method or enum value. Stubs return canned values; do not implement any engine logic."
- **Your job — alongside:** read the diff against DOC 3 line by line for the Pigeon file and Ports (this is the one place a silent mismatch costs every track). Resolve any Pigeon option-name mismatch against the pinned version mechanically, and note it in `docs/versions.md`.
- **Done when:** `flutter build apk --debug` and `./gradlew testDebugUnitTest` are green with the full contract in place; `PrivacyBoundaryTest` and `SayingShelfIntegrityTest` pass; tag `contracts-v1` exists; the other three teammates have cloned it.
- **Evidence required:** green output of both commands; `git diff --stat` for the tag; a list of the Pigeon methods (count = 30 host methods) and ports (count = 10 interfaces, plus the `Ports` holder).
- **Common drift:** "improving" a contract while transcribing (renaming `ret` back to `return`, merging DTOs). Pull back: "Contracts are locked. Fix the generator option, not the contract."

### Step T1.3 — Manifest, Permissions and PermissionGateway

- **Reference:** DOC 3 → *Collectors and Background Ingestion* (PermissionGateway); DOC 2 §2.6 (manifest, permission flow).
- **Executor:** Coding agent (Pro), one Research prompt.
- **Size:** M.
- **What to build:** `AndroidManifest.xml` per DOC 2 §2.6 (usage stats permission, listener service declaration with `BIND_NOTIFICATION_LISTENER_SERVICE`, `<queries>` with MAIN/LAUNCHER filter, `allowBackup=false`, data-extraction rules excluding everything); a `src/debug` manifest overlay may add what Flutter tooling needs, the main manifest must never declare INTERNET, accessibility, overlay, device admin, foreground service or the battery-exemption permission. `PermissionGateway`: usage access via `AppOpsManager`, listener-enabled check via the enabled-listeners setting, POST_NOTIFICATIONS check, intents opening the usage-access, notification-listener and app-info settings pages. Implement `getSetupState`, `openUsageAccessSettings`, `openNotificationAccessSettings`, `openAppInfoForRestrictedSettings`. "Restricted settings suspected" = notification access not granted after the user returned from the settings page.
- **Folder/file targets:** `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`, `host/PermissionGateway.kt`, `host/HostApiImpl.kt` (those four methods).
- **Agent prompt hint:** "Implement PermissionGateway and the four setup methods. Never trust a boolean returned by a settings page; re-check state. Do not declare any permission not listed in DOC 2 §2.6."
- **Your job — alongside:** run research prompt R2 on both phones' OS versions; test the real grant flow by hand on the Nothing Phone 3a.
- **Done when:** `getSetupState()` on the phone reflects reality before and after granting usage access; the release-manifest test (merged manifest scan) finds none of the forbidden permissions.
- **Evidence required:** screenshots of before/after SetupStateDto values (log lines), the merged-manifest permission list, the passing manifest test.
- **Common drift:** adding a "just in case" permission (QUERY_ALL_PACKAGES, FOREGROUND_SERVICE). Pull back: "The manifest is the promise. Remove it."

```
RESEARCH AGENT PROMPT (R2): "For Android 13, 14 and 15 and for Nothing OS (Nothing Phone 3a) and iQOO/Vivo Funtouch OS or OriginOS (iQOO Z7), document exactly how a SIDELOADED APK
  (installed from a file or adb, not a store) gets 'Notification access' and 'Usage access'. Specifically: is the notification-listener toggle greyed out ('restricted setting'), and what are the exact
  menu steps to 'Allow restricted settings' in App info on each OS? Is Usage access also affected? Does installing via adb change this? Give exact tap paths, Android/OS versions, and source URLs
  with dates. Mark anything not directly verified. Keep the answer under 400 words."
OUTPUT LANDS AT: docs/research/restricted_settings.md
CONSUMED BY:     Track 1 Step T1.3 (restricted-settings detection), Track 3 Step T3.3 (help text), Track 1 Step T1.14 (checks against real phones)
```

### Step T1.4 — Room Database and Port Stores

- **Reference:** DOC 3 → *Locked Shared Contracts* (LC-2); DOC 2 §2.3 (entities, retention, unique key).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** Room entities mirroring LC-2 field for field (`raw_event` unique on `(ts,type,pkg)`, `notif_event`, `listener_session`, `data_gap`, `ingest_state`, `app_meta`, `settings`, all derived and state tables); DAOs; `RoomEventStore`, `RoomNotifStore` (+ `ListenerCoverage`), `RoomGapStore`, `RoomDerivedStore`, `RoomStateStore` implementing the ports; `Retention.kt` (14 days for raw and notification events); `SakshiDatabase.kt` with an in-memory factory (used by tests and the demo); destructive migration allowed in development.
- **Folder/file targets:** `data/SakshiDatabase.kt`, `data/entities/*.kt`, `data/dao/*.kt`, `data/Room*Store.kt`, `data/Retention.kt`, `AppContainer.kt` (data wiring only), tests in `src/test/.../data/`.
- **Agent prompt hint:** "Implement Room storage that satisfies the ports in Ports.kt exactly. In-memory Room tests must pass the same behaviours the engine's fakes will have. No business logic in DAOs."
- **Your job — alongside:** none until the end: review the table list against LC-2 once.
- **Stub/mock strategy:** none needed; depends only on Sync 1.
- **Done when:** in-memory Room tests pass for: append dedupe via unique key; range queries inclusive/exclusive as the port says; retention purges at exactly 14 days; `clearAll()` empties every table (the test enumerates tables from the Room schema); coverage fraction math on sample sessions.
- **Evidence required:** the test list with pass status; the schema file; a count of tables equal to LC-2's entity list.
- **Common drift:** adding columns not in LC-2, or logic in DAOs. Say: "LC-2 is the schema. Extra columns go through the Contract Change Process."

### Step T1.5 — UsageEventsSource and the Ingest Orchestrator

- **Reference:** DOC 3 → *Collectors and Background Ingestion*; DOC 2 §2.7.
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `UsageEventsSource.read(from,to)` mapping the six `UsageEvents.Event` constants **by name** to `RawType`, keeping ALL packages including Sakshi's own (D6). `runIngest(deps, asOf)` as a plain function over ports (no Android types) with the eight steps in DOC 3: paused check, cursor, 10-minute overlap, append, NOT_SEEN gap rule, cursor update, engine call, retention. A debug-only "Dump events" action (writes a JSON of the last N hours of `RawEvent`s to the app's files dir) for spikes S-A and S-B and for Track 2's real fixtures.
- **Folder/file targets:** `host/UsageEventsSource.kt`, `host/IngestOrchestrator.kt` (new file, plain Kotlin), debug-only dump action in `host/DebugTools.kt` (excluded from release), tests in `src/test/.../data/` or `.../host/` using fakes.
- **Agent prompt hint:** "Implement UsageEventsSource and runIngest exactly as DOC 3 describes. runIngest must be unit-testable with fake stores and a fake source. No Android types in runIngest."
- **Your job — alongside:** run spikes S-A and S-B on the Nothing Phone 3a: scripted 5-minute sequence (switch apps, split-screen, picture-in-picture, lock/unlock); note the oldest event returned and events per day. Save the dump as `fixtures/real_s_a.json` for Track 2.
- **Done when:** unit tests cover first run (no cursor), repeated run (no duplicates), long gap (NOT_SEEN gap written), paused run (nothing appended), and permission-revoked run (typed NoPermission result); on the phone the dump contains events for the scripted sequence.
- **Evidence required:** test output; the dump file; a three-line note of S-B findings (oldest timestamp, events/day) added to `docs/spikes.md`.
- **Common drift:** classifying or filtering in the collector (dropping "boring" apps, skipping own package). Say: "The collector translates, never judges. Keep every package."

### Step T1.6 — NotificationCollector and Listener Coverage

- **Reference:** DOC 3 → *Collectors and Background Ingestion*; DOC 2 §2.6.1 (known risk on notification text).
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `NotificationCollector : NotificationListenerService`: on connect open a `listener_session`, on disconnect close it (stale open session closed at last known ts on next connect); `onNotificationPosted` and `onNotificationRemoved(sbn, rankingMap, reason)` append a `NotifEvent` with package, post time, category, ongoing flag and removal reason `CLICK`/`OTHER`; **never read title, text, extras or anything else**; drop events while paused; ignore Sakshi's own package; swallow and count exceptions. `requestRebind(ComponentName)` through `PermissionGateway`, called on app open when access is granted but the listener is not bound.
- **Folder/file targets:** `host/NotificationCollector.kt`, manifest service entry, tests.
- **Agent prompt hint:** "Implement the listener exactly per DOC 3. Store only the fields of NotifEvent. If you are tempted to read notification text for any reason, stop."
- **Your job — alongside:** post test notifications from three apps on the phone, tap one, dismiss one; check the rows. Begin spike S-C notes (force-stop and wait).
- **Done when:** a unit test over a fake notification object carrying title and text produces a stored row containing neither (and a reflection check that `NotifEvent` has no String field except `pkg` and `category`); on the phone, tapped notifications store `removal = CLICK`; sessions open and close across a force-stop.
- **Evidence required:** test output; a DB export snippet showing 5 rows; the S-C note.
- **Common drift:** logging notification content "for debugging". Say: "No content in logs either."

### Step T1.7 — IngestWorker and Scheduling

- **Reference:** DOC 3 → *Collectors and Background Ingestion*; DOC 2 §2.2 (background execution), §2.7.
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `IngestWorker : CoroutineWorker` calling `runIngest` with the real ports; always returns `Result.success()`; unique periodic work (15-minute interval, keep policy) enqueued on app start and after boot; an expedited one-time run for `syncNow()`; worker-run bookkeeping in `ingest_state` (`last_worker_run_at`, `worker_runs_7d`). No foreground service, no wake locks, no exact alarms.
- **Folder/file targets:** `host/IngestWorker.kt`, `host/Scheduler.kt`, `AppContainer.kt`.
- **Agent prompt hint:** "Wire runIngest into a CoroutineWorker with unique periodic work and an expedited on-open run. Return success always. Do not add a foreground service."
- **Your job — alongside:** leave the Nothing Phone 3a idle with the app closed for a stretch and check run timestamps (spike S-D).
- **Done when:** after installing, the periodic job appears in `adb shell dumpsys jobscheduler` for the package (or WorkManager inspector); a debug "Run now" produces an `ingest_state` update; run timestamps accrue while the app is closed.
- **Evidence required:** the dumpsys excerpt or inspector screenshot; two consecutive run timestamps from the idle phone.
- **Common drift:** switching to a foreground service "to be reliable". Pull back: "Idempotent catch-up is the reliability. No foreground service."

### Step T1.8 — AppCatalog and SayingShelf Adapters

- **Reference:** DOC 3 → *F2. Work-Set Setup*, *F13. The Saying Shelf*; DOC 2 §2.6.3.
- **Executor:** Coding agent (Pro), one Research prompt.
- **Size:** S.
- **What to build:** `AppCatalogImpl`: launcher apps through `<queries>`, label lookup on a background thread, `ApplicationInfo.category` (API 26+) mapped to the engine's category int, `isNeutral` from a bundled neutral list (dialer, maps, camera, calculator, clock, system UI, plus default dialer/launcher role lookups), `ownPackage()`. Neutral list lives in `assets/sakshi/neutral_packages.json` (names only). `SayingShelfImpl` parses `sayings.json` once.
- **Folder/file targets:** `host/AppCatalogImpl.kt`, `host/SayingShelfImpl.kt`, `assets/sakshi/neutral_packages.json`.
- **Agent prompt hint:** "Implement AppCatalog and SayingShelf per the ports. Data comes from the neutral_packages.json asset produced from research prompt R3."
- **Your job — alongside:** run R3; spike S-E (compare the launcher list with packages seen in a day of usage events).
- **Done when:** `listLauncherApps()` returns the phone's launcher apps with labels; an app seen in usage events but not in the list falls back to its package name as the label; `SayingShelfImpl.all().size == 27`.
- **Evidence required:** a log of the first 10 apps with categories; the S-E note.
- **Common drift:** requesting `QUERY_ALL_PACKAGES` when the queries block is slightly wrong. Say: "Fix the queries block."

```
RESEARCH AGENT PROMPT (R3): "List the package names of the default dialer, contacts, messages, camera, calculator, clock, maps, Settings, and system launcher apps on: Nothing OS (Nothing Phone 3a),
  Vivo/iQOO (Funtouch OS / OriginOS), Xiaomi (HyperOS/MIUI), Samsung One UI, Oppo/Realme (ColorOS), and stock Pixel Android. Output a JSON array of package-name strings only, grouped in comments by OEM
  (comments outside the array). Mark any package you could not verify. Sources with dates."
OUTPUT LANDS AT: docs/research/neutral_packages.json
CONSUMED BY:     Track 1 Step T1.8 (assets/sakshi/neutral_packages.json)
```

### Step T1.9 — HostApiImpl and Mappers

- **Reference:** DOC 3 → *Pigeon Bridge and Fake Host*, *Locked Shared Contracts* (LC-3, LC-4).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** Every Pigeon method implemented as a thin call: validate/gate → engine façade or adapter → `Mappers.kt` (MirrorView, TodayView, LakeView, WhatISeeView, SuggestionView etc. → DTOs, field for field). Error codes `NO_PERMISSION`, `DEMO_ACTIVE`, `BAD_REQUEST`, `STALE_SUGGESTION`, `REANCHOR_NOT_ALLOWED`, `EXPORT_FAILED`, `INTERNAL` as `FlutterError`. Long calls on a background dispatcher. `syncNow` = expedited ingest then return `SyncStatusDto`. Golden JSON tests: each canned View maps to a checked-in JSON (shared with Track 4's fixtures: same file).
- **Folder/file targets:** `host/HostApiImpl.kt`, `host/Mappers.kt`, `AppContainer.kt`, `src/test/resources/golden/*.json`.
- **Agent prompt hint:** "Implement HostApiImpl as a thin layer: no business logic, each method a few lines, mapping in Mappers.kt only. Add golden JSON tests per View."
- **Your job — alongside:** None — pure codegen, watch-and-verify (watch that no method contains an `if` about attention).
- **Stub/mock strategy:** the engine façade is still T1.2's canned stub until Sync 4; mappers are tested against canned Views, so this step does not wait for Track 2.
- **Done when:** all 30 methods respond on the real phone through a debug screen; golden mapper tests are green; a mapper test fails when a View field is added without a DTO field (a reflection test comparing field names).
- **Evidence required:** test output; a table of method → "implemented" with line counts (any method over ~15 lines is explained).
- **Common drift:** logic creeping into HostApiImpl (e.g., deciding "Nothing to fix"). Say: "That belongs in the engine. Move it or leave it undone."

### Step T1.10 — Lake Widget

- **Reference:** DOC 3 → *F7. The Lake (Home-Screen Widget)*; DOC 2 §2.2 (widget decision).
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `LakeWidget : AppWidgetProvider`, `res/xml/lake_widget_info.xml` (`updatePeriodMillis=0`, 2×1 cell), `res/layout/lake_widget.xml`, a `refresh(context, view?)` helper that reads `lake_state` and sets drawable, phrase and "as of" text, PendingIntent to the Mirror (MainActivity overrides `getInitialRoute()` to return `/mirror` when launched from the widget or the note, so no host-to-Flutter call is needed); called from the worker (when the engine reports `lakeChanged`), app open and `onUpdate`; catches all exceptions. Use placeholder vector drawables named `lake_still`, `lake_rippled`, `lake_choppy` until Track 4 delivers art (same names; Track 4 only replaces files).
- **Folder/file targets:** `host/LakeWidget.kt`, `res/xml/lake_widget_info.xml`, `res/layout/lake_widget.xml`, `res/drawable/lake_*.xml` (placeholders), manifest receiver.
- **Agent prompt hint:** "Native RemoteViews widget, no Glance, no bitmaps drawn in code. Reads stored lake_state only. Never computes anything."
- **Your job — alongside:** add the widget on the Nothing Phone 3a home screen and on the iQOO Z7; test with the debug "set lake state" action and later with the demo clock (spike S-F).
- **Done when:** the widget shows each of the three states plus the learning phrase; it redraws when the debug action changes `lake_state`; survives a reboot and a launcher change; shows its "as of" time.
- **Evidence required:** screenshots of the three states on the phone; S-F note (launcher change, reboot).
- **Common drift:** adding a live-updating or animated widget. Say: "Last completed window only. No timers."

### Step T1.11 — Weekly Note Notifier

- **Reference:** DOC 3 → *F9. The Quiet Note*.
- **Executor:** Coding agent (Pro).
- **Size:** S.
- **What to build:** `WeeklyNoteNotifier.maybePost(decision)`: channel `weekly_mirror` (IMPORTANCE_LOW, no sound/vibration/badge), title "Sakshi", body "Your Mirror is ready", tap opens the Mirror, 3-day timeout, no actions; `setWeeklyNote(true)` requests POST_NOTIFICATIONS on Android 13+ through the Activity and returns the effective state; `setWeeklyNote(false)` cancels any posted note; the periodic worker calls `engine.noteDecision` then `maybePost`.
- **Folder/file targets:** `host/WeeklyNoteNotifier.kt`, `host/HostApiImpl.kt` (the method), manifest (`POST_NOTIFICATIONS` declared; requested only when enabled).
- **Agent prompt hint:** "Post a silent, content-free note only when engine.noteDecision says so. The only text is the constant 'Your Mirror is ready'."
- **Your job — alongside:** trigger once by a debug action on the phone, confirm silence, no badge, tap opens the Mirror.
- **Done when:** a debug-forced decision posts the note once; a second forced decision for the same week does not; denial leaves `weeklyNoteEnabled=false`.
- **Evidence required:** screenshot of the note and the channel settings; log lines for the two decisions.
- **Common drift:** adding score or app names to the text. Say: "The text is a constant. Nothing else."

### Step T1.12 — Battery Helper

- **Reference:** DOC 2 §2.7.4; DOC 3 → *Pigeon Bridge and Fake Host* (`openBatterySettings`, `markBatteryHelperShown`).
- **Executor:** Coding agent (Pro), one Research prompt.
- **Size:** S.
- **What to build:** `BatterySetup.openBatterySettings()` opens Android's own battery-optimisation settings page (never requests the exemption through the system dialog; no new permission); `markBatteryHelperShown` writes the setting; fixed OEM text table in a Kotlin object (`BatteryTips`) keyed by manufacturer (`Build.MANUFACTURER`) with the research output; the per-OEM wording is NOT sent over the bridge: this step produces the verified text in `docs/research/oem_battery.md`, and Track 3 copies it into `ui_strings.dart` in T3.5 (chrome text, per LC-8).
- **Folder/file targets:** `host/BatterySetup.kt`, `host/HostApiImpl.kt` (two methods), `docs/research/oem_battery.md`.
- **Agent prompt hint:** "Implement only the two host methods. The helper opens Android's own page. It never requests REQUEST_IGNORE_BATTERY_OPTIMIZATIONS."
- **Your job — alongside:** run R4; then verify each tap path on the iQOO Z7 by hand and correct the wording.
- **Done when:** on the iQOO Z7 the button opens the real battery/background settings page and the verified tap path text is written to `oem_battery.md`.
- **Evidence required:** screenshots of the page opened on both phones; the verified wording file.
- **Common drift:** requesting the exemption "to make it work". Say: "DOC 2 §2.7.4: the user decides, once."

```
RESEARCH AGENT PROMPT (R4): "For Vivo/iQOO (Funtouch OS / OriginOS), Nothing OS, Xiaomi HyperOS, Oppo/Realme ColorOS and Samsung One UI, give the exact settings paths a user follows to allow an app
  to keep running in the background, allow auto-start, and set battery usage to unrestricted (names of menus and toggles as they appear, with OS version). One short numbered list per OEM, maximum 5
  steps each. Mark paths you could not verify. Sources with dates. Plain wording a student could follow."
OUTPUT LANDS AT: docs/research/oem_battery.md
CONSUMED BY:     Track 1 Step T1.12 (verification on the iQOO Z7), Track 3 Step T3.5 (the helper screen text)
```

### Step T1.13 — Pause, Export, Delete and DemoController

- **Reference:** DOC 3 → *F10. What I See (Pause, Export, Delete)*, *Time Machine Demo and Personas*.
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `pause(on)` through the engine use case plus the collector/worker honouring the flag; `Exporter` (streamed JSON to `cacheDir/exports`, rename on success, share sheet launched from the host through a FileProvider, `ExportDto{fileName, byteSize}`); `deleteEverything()` (one transaction, cached exports removed, lake reset, widget redrawn, WorkManager schedule kept); `DemoController` (in-memory database swap in `AppContainer`, `isDemo` flag, injected Clock, widget phrase suffix " (demo)", worker skips widget refresh, `DEMO_ACTIVE` rejection of Pause/Export/Delete).
- **Folder/file targets:** `data/Exporter.kt`, `host/DemoController.kt`, `AppContainer.kt`, `res/xml/file_paths.xml`, manifest provider entry.
- **Agent prompt hint:** "Implement pause, export, delete and the demo store swap exactly per DOC 3. Delete must be one Room transaction. Export streams to a temp file and renames."
- **Your job — alongside:** run export and delete on the phone once; open the exported JSON and confirm no text fields.
- **Stub/mock strategy:** until Track 2 delivers `EventSynthesizer`, `startDemo` loads a hardcoded handful of events so the swap mechanics can be tested.
- **Done when:** delete empties every table (test enumerates tables); export of a fixture equals the golden JSON; pause creates exactly one PAUSED gap and closes it on resume; starting a demo leaves the real database file byte-identical (hash before and after).
- **Evidence required:** test output; a hash comparison line; a screenshot of the Android share sheet.
- **Common drift:** letting Delete touch the demo or Pause apply during demo. Say: "DEMO_ACTIVE rejects all three."

### Step T1.14 — Hostile-OEM Spike Pass

- **Reference:** DOC 2 §2.10 (spikes S-C, S-D, S-F, S-H) and §2.10.3.
- **Executor:** Human (you), with a Coding agent (Pro) for small fixes.
- **Size:** M.
- **What to build:** nothing new unless a fix is needed. Run, on the **iQOO Z7**: S-C (listener kill and reconnect) and S-D (periodic run cadence) **with the battery helper skipped, then applied**; S-F (widget refresh); S-H (sideload restricted-settings path). Repeat S-C/S-D/S-F/S-H on the Nothing Phone 3a. Record results in `docs/spikes.md`. Any other teammate with a Xiaomi/Oppo/Vivo/Realme phone runs S-C and S-D too.
- **Folder/file targets:** `docs/spikes.md`; fixes land only in `host/` files named by the failing spike.
- **Agent prompt hint:** "Here is the spike result (paste). Propose the smallest change in host/ that fixes it, or say it cannot be fixed and what honest state the app should show."
- **Your job — alongside:** this step is the human job. Keep a one-line log per test: phone, OS version, helper on/off, result.
- **Done when:** `docs/spikes.md` records, for each phone: whether the listener reconnects, run cadence over an idle stretch, whether the widget redraws, and the exact sideload path; the demo phone is named (Nothing Phone 3a unless S-C/S-D/S-F say otherwise).
- **Evidence required:** the filled spikes table with timestamps and OS versions.
- **Common drift:** "fixing" a hostile-OEM kill with a foreground service or exemption request. Say: "Report it. The honest state is the fix."

### Step T1.15 — Real Engine on the Real Phone

- **Reference:** DOC 3 → *F1. Instant First Look*; DOC 2 §2.8.3 (face validity).
- **Executor:** Coding agent (Pro), with Human.
- **Size:** M.
- **What to build:** merge `feat/t2-engine` into the Track 1 branch after Sync 4; delete T1.2's canned stub logic; run the full path on the real phone: grant, First Look, setup, a few hours of use, Mirror. Fix integration mismatches **in host/ and data/ only**; any engine bug goes to Track 2 as a failing fixture. Export the real events (raw dump from T1.5 plus notifications) to `fixtures/real_*.json` for Track 2's tuning (Sync 5).
- **Folder/file targets:** `host/`, `data/`, `fixtures/real_*.json`, `docs/integration.md`.
- **Agent prompt hint:** "The real engine is now in. Make the host and data layers satisfy it. Do not edit engine/. For any engine bug, write a failing fixture and report it."
- **Your job — alongside:** compare the First Look with Android's own Digital Wellbeing for plausibility; each teammate uses the build for a couple of ordinary days (face-validity check from DOC 2 §2.8.3).
- **Done when:** on the Nothing Phone 3a, granting usage access shows a First Look of the phone's real recent days within seconds; the periodic job later updates the Lake; real exported events exist for Track 2.
- **Evidence required:** screenshots of the First Look and the Lake; the exported fixtures; a list of any engine bugs filed as failing fixtures.
- **Common drift:** fixing an engine bug inside host/ by special-casing. Say: "No. File the fixture; Track 2 fixes it."

### Step T1.16 — Release Build and Manifest Audit

- **Reference:** DOC 2 §2.6 (manifest is the promise), §2.2 (distribution).
- **Executor:** Coding agent (Pro) and Human.
- **Size:** S.
- **What to build:** signed release APK (a keystore kept out of git), R8/ProGuard defaults, release build type with no debug tools (the `DebugTools` dump excluded); an automated audit script `tools/audit_apk.sh` that extracts permissions from the built APK and fails on INTERNET, accessibility, overlay, device admin, foreground service or battery exemption.
- **Folder/file targets:** `android/app/build.gradle(.kts)` (signing), `tools/audit_apk.sh`, `docs/release.md`.
- **Agent prompt hint:** "Produce the signed release APK and the audit script. The script must fail the build on any forbidden permission."
- **Your job — alongside:** install via file on the Nothing Phone 3a and walk the sideload path; run with airplane mode on to demonstrate that it works with no network.
- **Done when:** the audit script prints the permission list and exits 0; the release APK installs on both phones and completes the setup flow.
- **Evidence required:** the audit output; the install screenshots.
- **Common drift:** shipping a debug-signed or debug-permission APK. Say: "The audit script is the gate."

---

## Track 2 — Analytics Engine

```
TRACK 2: Everything in com.kleos.sakshi.engine and its tests: the definition of focus as code.
  OWNER:       Teammate B. Executor: Coding agent (Pro). Human for the final tuning step.
  DEPENDS ON:  Track 1 Step T1.2 (Sync 1).
  STEPS:       17. Each step ships its golden examples (DOC 3) as unit tests BEFORE it counts as done.
  PATHS:       android/app/src/main/kotlin/com/kleos/sakshi/engine/ (all of it except the files transcribed in T1.2: see ownership map), android/app/src/test/kotlin/com/kleos/sakshi/engine/, fixtures/
```

Every Track 2 step uses this prompt frame: *"Implement only [module] per DOC 3 → [section]. Write the golden tests first and make them pass. Do not import android, host or data. Take time from the Clock port and randomness from the Randomness port. Do not edit any file outside engine/ and its tests."*

### Step T2.1 — Engine Test Kit and Tuning

- **Reference:** DOC 3 → *Event Vocabulary and Tuning*, *Project Skeleton and Folder Structure*.
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `src/test/.../engine/testkit/`: in-memory fakes for every port (`FakeEventStore`, `FakeNotifStore`, `FakeListenerCoverage`, `FakeGapStore`, `FakeDerivedStore`, `FakeStateStore`, `FakeAppCatalog`, `FakeSayingShelf`, `FakeClock`, `SeededRandomness`); an event-stream builder DSL (`events { at("10:00:00") resume("A"); … }`) and a JSON golden-fixture loader/asserter; time helpers in the Asia/Kolkata zone and UTC. Verify `Tuning.kt` (written in T1.2) matches DOC 3 name for name; fix values only.
- **Folder/file targets:** `src/test/kotlin/com/kleos/sakshi/engine/testkit/*.kt`, `engine/tuning/Tuning.kt` (values only).
- **Agent prompt hint:** "Build the test kit: fakes honouring the port contracts and a tiny DSL for hand-written event streams. No production logic."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** a test using the DSL builds the G-I1 stream and the fakes round-trip it; `StudyDay.of` tests (03:59, 04:00, midnight crossing, two zones) pass.
- **Evidence required:** test output; the DSL usage example.
- **Common drift:** putting logic in fakes. Say: "Fakes are dumb stores."

### Step T2.2 — Foreground Intervals

- **Reference:** DOC 3 → *Foreground Intervals and App Classification* (rules R1–R9, G-I1–G-I5).
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `engine/intervals/ForegroundIntervals.kt` with `reconstruct()` implementing R1–R9 with the tie order; `Reconstruction`, `ForegroundInterval`, `ScreenSpan`; the anomaly counter.
- **Folder/file targets:** `engine/intervals/ForegroundIntervals.kt`, `engine/model/Entities.kt` (only the engine-internal types it needs, appended in a clearly marked block), tests.
- **Agent prompt hint:** (frame) + "Rules R1–R9 verbatim. The five golden examples are the spec."
- **Your job — alongside:** when Track 1 delivers `fixtures/real_s_a.json` (T1.5), replay it and check by eye that the intervals match the scripted sequence.
- **Stub/mock strategy:** uses hand-built streams; the real fixture is added when available.
- **Done when:** G-I1 to G-I5 pass; property tests pass (shuffle-invariant, no overlap, end ≥ start).
- **Evidence required:** test output; the property-test seeds used.
- **Common drift:** inventing handling for events not in R1–R9. Say: "Unknown event types are skipped. Do not add rules."

### Step T2.3 — App Classifier and Depends Resolution

- **Reference:** DOC 3 → *Foreground Intervals and App Classification* (classify), *F4. Stone and Wave* (Depends resolution).
- **Executor:** Coding agent (Pro).
- **Size:** S.
- **What to build:** `AppClassifier` (user class, neutral list via `AppCatalog`, own package → NEUTRAL), and `DependsResolver` (the 90-second chain rule with transparent neutral intervals) producing effective classes per interval.
- **Folder/file targets:** `engine/classify/AppClassifier.kt`, `engine/classify/DependsResolver.kt`, tests.
- **Agent prompt hint:** (frame) + "Depends rules exactly as DOC 3 F4. Own package is always NEUTRAL."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** tests: a Depends interval starting 89 s after an in-set interval is in-set, 91 s is off-set; a chain of Depends intervals each within 90 s stays in-set; a neutral interval between two in-set intervals is transparent; own package events never change a class outcome.
- **Evidence required:** test output.
- **Common drift:** adding a fourth "unknown" class. Say: "Four classes only."

### Step T2.4 — Window Finder

- **Reference:** DOC 3 → *F3. Window Finder* (G-W1–G-W4).
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `WindowFinder.find` (study blocks incl. midnight crossing, runs per D9, padding, union/merge, source tag, day attribution, finalised/partial flags, valid-day rule); learn-study-hours derivation (median start/end of ≥ 14 inferred windows).
- **Folder/file targets:** `engine/windows/WindowFinder.kt`, tests.
- **Agent prompt hint:** (frame) + "Implement the window rules and D9's run definition exactly. G-W1 to G-W4 are the spec."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** G-W1 to G-W4 pass; property: windows never overlap and start < end; a window overlapping a `data_gap` is `partial`.
- **Evidence required:** test output.
- **Common drift:** scoring midnight reels as windows. Say: "No in-set run and outside study hours means no window."

### Step T2.5 — Stay Detector, Returns and Stretches

- **Reference:** DOC 3 → *F4. Stone and Wave* (StayDetector, Returns), *F5* (stretch rules D1).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `StayDetector.detect` (glances, merge before the 30 s test, first_pkg, pkg_main, glances_before), `Returns.returnMinutes` (back in-set 30 s or put-down), `StretchBuilder` (D1: quiet joins the stretch it follows, ≥ 3 min quiet ends it as PUT_DOWN, neutral and glance time hold).
- **Folder/file targets:** `engine/stays/StayDetector.kt`, `engine/stays/Returns.kt`, `engine/metrics/StretchBuilder.kt`, tests.
- **Agent prompt hint:** (frame) + "G-S1 to G-S3, G-S6 and the stretch rules D1 are the spec. Merge before testing the 30 s threshold."
- **Your job — alongside:** read D1 once; confirm the stretch example (25 min study + 5 min screen-off = one 30-minute PUT_DOWN stretch) matches your intuition.
- **Done when:** G-S1, G-S2, G-S3, G-S6a, G-S6b pass; stretch tests: the D1 example; a 2-minute screen-off does not end a stretch; a window of only quiet is one stretch ended by WINDOW_END; properties: stays never overlap and lie inside one window.
- **Evidence required:** test output.
- **Common drift:** double-counting glance time. Say: "Glances hold; they are counted, not scored."

### Step T2.6 — Stone and Wave

- **Reference:** DOC 3 → *F4. Stone and Wave* (StoneWave, no-ripple, G-S4, G-S5).
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `StoneWave.classify` (30-second look-back, coverage check, first_pkg match, click detection), the no-ripple rate computation.
- **Folder/file targets:** `engine/stays/StoneWave.kt`, tests.
- **Agent prompt hint:** (frame) + "Origin is UNKNOWN whenever listener coverage does not cover the look-back. Ongoing and own-package notifications are ignored."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** G-S4 and G-S5 pass; ping from app B then stay in app C is SELF_STARTED; property: origin is UNKNOWN whenever coverage is false.
- **Evidence required:** test output.
- **Common drift:** treating no data as "self-started". Say: "No coverage means UNKNOWN."

### Step T2.7 — Parts, Steadiness and Baseline

- **Reference:** DOC 3 → *F5. Steadiness and Its Four Parts* (G-F1–G-F7).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `DayMetrics`, `WeekMetrics` (pooling, length-weighted median, smoothing, extras: switches per hour, ramp-up, flinch, no-ripple, work-set coverage, endings), `Steadiness.steadiness`, `BaselineService` (freeze after 8 valid days from first-read study day per D2, single re-anchor at week ≥ 4, unusual-week flag).
- **Folder/file targets:** `engine/metrics/*.kt`, `engine/baseline/BaselineService.kt`, tests.
- **Agent prompt hint:** (frame) + "Implement the DOC 3 formulas and the seven golden examples. Parts are nullable, never zero. Steadiness is null if any part is missing."
- **Your job — alongside:** this is the formula you will defend to a judge: recompute G-F1 and G-F2 by hand once.
- **Done when:** G-F1 (116, Steadier), G-F2 (75, Wavering), G-F3 clamps, G-F4 boundaries, G-F5 weighted median (10), G-F6 smoothing, G-F7 determinism pass; property: monotone in each part and 100 at baseline; baseline never changes after freeze.
- **Evidence required:** test output; the hand calculation in a comment.
- **Common drift:** "improving" the formula (different weights, recomputed baseline). Say: "The weights are Tuning values. Do not change the formula."

### Step T2.8 — Engine Core: processNewEvents

- **Reference:** DOC 3 → *F1. Instant First Look* (processNewEvents), *Foreground Intervals*, *F3*, *F4*, *F5*.
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `SakshiEngine.processNewEvents(asOf)` replacing T1.2's stub for this method: for each touched study day, 60 minutes of lead-in events → reconstruct → classify → windows → stays → stone/wave → metrics → `DerivedStore.replaceDay`; `FinalizeWindows`; `UpdateBaseline`; `ProcessReport`. It must never read events after `asOf`. Orchestration in `engine/usecases/`.
- **Folder/file targets:** `engine/SakshiEngine.kt` (this method), `engine/usecases/RecomputeDay.kt`, `FinalizeWindows.kt`, `UpdateBaseline.kt`, golden fixtures in `src/test/resources/fixtures/*.json`.
- **Agent prompt hint:** (frame) + "Wire the pure modules into processNewEvents. Run over golden fixtures: a clean evening, a ping-broken evening, a midnight crossing, split-screen and PiP, and a listener-gap day."
- **Your job — alongside:** write the expected derived rows for the five fixtures by hand (or review the agent's) before accepting them as golden.
- **Done when:** the five golden fixtures produce the expected `Window`, `Stretch`, `Stay` and `DaySummary` rows; recomputing a day twice gives identical rows; duplicating or reordering raw events changes nothing; events after `asOf` change nothing (properties).
- **Evidence required:** test output; the fixture files; the property-test results.
- **Common drift:** reading the system clock. Say: "Clock port only; the Dependency Rule test will fail you."

### Step T2.9 — Data States, Lapse and Teacher Meter

- **Reference:** DOC 3 → *F17. Honest Data States*, *F15. Lapse and Return*, *F14. The Teacher Leaves*.
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `DataStates.flags/sentences` (PING_OFF, PARTIAL_PING, NOT_SEEN, PAUSED, TOO_LITTLE_DATA, UNUSUAL_WEEK, FIRST_LOOK) with listener coverage maths; `Lapse.detect` (D12: 3+ consecutive inactive days, gap exclusion, shown once); `TeacherLeaves.meter` (30 s merge, ≥ 3 s opens, falling vs plain line data). Sentences here are placeholders in a `Copy` object consumed later by `SentenceBuilder` (T2.14); this step returns data and fixed sentences for the F17 flags only.
- **Folder/file targets:** `engine/mirror/DataStates.kt`, `Lapse.kt`, `TeacherLeaves.kt`, tests.
- **Agent prompt hint:** (frame) + "Implement the three modules with the DOC 3 tests. A number that cannot be computed is null, never zero."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** coverage 0.69 ⇒ PARTIAL_PING, 0.70 ⇒ none; 3 windows ⇒ TOO_LITTLE_DATA, 4 ⇒ not; 3 inactive seen days ⇒ lapse n=3; 2 days or 3 days inside a NOT_SEEN gap ⇒ none; two own intervals 20 s apart ⇒ one open, 31 s ⇒ two; inserting own-package events never changes any Part (property).
- **Evidence required:** test output.
- **Common drift:** zero-filling missing parts. Say: "Null or a sentence. Never zero."

### Step T2.10 — Patterns P1 to P3

- **Reference:** DOC 3 → *Pattern Layer (P1 to P5, Cross-Day Link, Clustering)* (P1, P2, P3; G-P1–G-P3).
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `PatternDetector` interface, `EvidenceGate`, `Rhythm.kt` (cells, clear slot), `Trend.kt`, `Shift.kt` (CUSUM), the `Pattern` upsert plumbing in `PatternEngine.kt` (retire stale after 14 days).
- **Folder/file targets:** `engine/patterns/{PatternDetector,EvidenceGate,Rhythm,Trend,Shift,PatternEngine}.kt`, tests.
- **Agent prompt hint:** (frame) + "Detectors return empty lists when evidence is short. G-P1, G-P2, G-P3 are the spec; the CUSUM parameters are in Tuning."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** G-P1 (ratio 1.857 pattern; 3 windows none; 2 days none; ratio 1.17 none), G-P2 ([7,6,5,4] BETTER, run3; flat none), G-P3 (shift at index 3; no alarm on the noisy series) pass; re-running yields identical patterns.
- **Evidence required:** test output.
- **Common drift:** adding a "low confidence" pattern. Say: "No evidence, no pattern."

### Step T2.11 — Patterns P4, P5, Cross-Day and Clustering

- **Reference:** DOC 3 → *Pattern Layer (P1 to P5, Cross-Day Link, Clustering)* (G-P4–G-P7).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `WindowShape.kt` with `WindowLabeler`, `BreakPoint.kt`, `CrossDay.kt`, `Clustering.kt` (seeded k-means++, k ∈ {2,3}, silhouette, rule-named groups); register all detectors in `PatternEngine`.
- **Folder/file targets:** `engine/patterns/{WindowShape,BreakPoint,CrossDay,Clustering}.kt`, `engine/windows/WindowLabeler.kt`, tests.
- **Agent prompt hint:** (frame) + "G-P4 to G-P7 are the spec. Clustering is optional and must return nothing when silhouette < 0.50. No ML libraries: about a page of code."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** G-P4 labels (A Held, B Pinged, C Reached, D Held, E Pinged, F null), G-P5 (55 % pattern, 45 % none), G-P6 (5 late nights pattern, 3 none), G-P7 (two blobs cluster, noise none, same seed identical) pass.
- **Evidence required:** test output.
- **Common drift:** pulling in a clustering library. Say: "Hand-written k-means only."

### Step T2.12 — Suggestion Rules and Selector

- **Reference:** DOC 3 → *F11. The Suggestions Engine* (G-G1–G-G7).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `SuggestionRule` interface, twelve rule files, `SuggestionRegistry`, `SuggestionSelector` with the ordered silence rules and ranking.
- **Folder/file targets:** `engine/suggestions/**`, tests.
- **Agent prompt hint:** (frame) + "Rules return Candidate data or null and never produce words. The selector applies the seven silence rules in DOC 3's order. G-G1 to G-G7 are the spec."
- **Your job — alongside:** read the seven silence rules once: they are DOC 1's promises.
- **Done when:** G-G1 to G-G7 pass; properties: never two live experiments, never a task in the first Mirror after a lapse, at most one task per Mirror, identical output on identical input.
- **Evidence required:** test output.
- **Common drift:** writing suggestion lines inside rules. Say: "Words live in SentenceBuilder."

### Step T2.13 — Judging, Footprint and Suggestion Taps

- **Reference:** DOC 3 → *F12. Self-Judging Verdicts* (G-J1–G-J7), *F11* (TapTryThis, Dismiss, footprint).
- **Executor:** Coding agent (Pro).
- **Size:** M.
- **What to build:** `ExperimentJudge`, `TargetMetrics`, `WeekdayMix`, `JudgeExperiments`, `TapTryThis`, `DismissSuggestion`, footprint start for S2/S4.
- **Folder/file targets:** `engine/judging/*.kt`, `engine/usecases/{TapTryThis,DismissSuggestion,JudgeExperiments}.kt`, tests.
- **Agent prompt hint:** (frame) + "Verdict precedence: PENDING, UNCLEAR, TOO_LITTLE, then MOVED or NO_CHANGE. G-J1 to G-J7 are the spec."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** G-J1 to G-J7 pass (including the weekday-mix example that must not show a false improvement); a verdict never changes once written; a stale tap returns `STALE`.
- **Evidence required:** test output.
- **Common drift:** judging without re-weighting the weekday mix. Say: "G-J7 exists to catch exactly that."

### Step T2.14 — SentenceBuilder and Gentle Policy

- **Reference:** DOC 3 → *F6. The Weekly Mirror* (SentenceBuilder, forbidden words), *F16. Gentle Mode*, *F7* (phrases).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `SentenceBuilder` with every template (headline, part lines, stones, clear hour, 5 pattern kinds, 12 suggestion lines and action labels, verdicts, lapse, teacher, Lake phrases normal and gentle, data-state sentences), `GentlePolicy.apply`, `CopyRulesTest` (regex deny list and "!" ban over fixtures and a generated args matrix). Sentences must be built from DOC 1 §1.2.2, §1.2.6, §1.3.3 and §1.3.4 examples.
- **Folder/file targets:** `engine/mirror/SentenceBuilder.kt`, `engine/mirror/GentlePolicy.kt`, tests.
- **Agent prompt hint:** (frame) + "All user-facing sentences live in SentenceBuilder. Use the DOC 1 example sentences as templates. No sentence may match the forbidden pattern or contain '!'. Every pattern line ends with its evidence count."
- **Your job — alongside:** read every template aloud once and rewrite anything that sounds like a lecture; this is the voice of the product.
- **Done when:** `CopyRulesTest` passes over all templates; gentle fixtures contain none of Steadiness/Steady/Steadier/Wavering; `apply` is idempotent.
- **Evidence required:** test output; a printed list of all templates for your review.
- **Common drift:** motivational phrasing, exclamation marks, "you should". Say: "Observation, one action, no judgement."

### Step T2.15 — Views, Lake, Today, Note, Export, Sayings and the Façade

- **Reference:** DOC 3 → *F6*, *F7*, *F8*, *F9*, *F10*, *F13*, *Locked Shared Contracts* (LC-3).
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `MirrorBuilder` (the eight-step compute order, including `reanchorOffered` and `suggestedStudyBlock`, and the `mirrorViewedWeek` write in `mirror()`, provisional First Look, gentle applied last), `TodayBuilder`, `LakeBuilder`, `WhatISeeBuilder`, `WeeklyNote.decide`, `ExportBuilder`, `ChooseSayings`/`PickSaying`, the goal-tap and re-anchor use cases, `SetGentle`/`SetUnder18`, `PauseCollection`, `DeleteEverything`; complete `SakshiEngine.kt` so every LC-3 method is real. Golden Mirror/Today/Lake/WhatISee views serialised to `src/test/resources/golden/*.json` **in the exact JSON shape Track 1's mappers and Track 4's fixtures use**.
- **Folder/file targets:** `engine/mirror/{MirrorBuilder,TodayBuilder}.kt`, `engine/lake/LakeBuilder.kt`, `engine/privacy/*.kt`, `engine/note/WeeklyNote.kt`, `engine/usecases/*.kt`, `engine/SakshiEngine.kt`, golden JSON.
- **Agent prompt hint:** (frame) + "Complete the façade. TodayView has no suggestion field. The Mirror compute order is DOC 3's. Provide golden JSON for every view state in the F6 test list."
- **Your job — alongside:** review the golden Mirror JSON for the 'steady', 'wavering', 'gentle' and 'nothing to fix' states: these are what the UI will show.
- **Done when:** all façade methods are real; every golden view test passes; the Lake rules (HELD ⇒ STILL, 1.5× ⇒ CHOPPY, 1.49× ⇒ RIPPLED, no baseline ⇒ LEARNING, none ⇒ NO_DATA, live window ignored) and the note rules (Monday 07:59 no, 08:00 yes, Wednesday 23:59 yes, Thursday no, 2 valid days no, duplicate no) pass.
- **Evidence required:** test output; the list of golden JSON files and the list of façade methods with "real" status.
- **Common drift:** adding fields to Views. Say: "Views are LC-3. Propose changes."

### Step T2.16 — Personas and Persona Replay

- **Reference:** DOC 3 → *Time Machine Demo and Personas* (LC-6); DOC 1 §1.4.3.
- **Executor:** Coding agent (Pro).
- **Size:** L.
- **What to build:** `PersonaSpec`, `PERSONAS` (aarav, meera, rohan), `EventSynthesizer.synthesize` (deterministic counts, mean-forced jitter, quirks), and `PersonaReplayTest` for the three presets and the demo beats; calibrate the synthesizer (never the engine) until Steadiness lands at 100 / 116 ± 6 / 136 ± 6 and each part within ±15 % of target.
- **Folder/file targets:** `engine/demo/{Personas,EventSynthesizer}.kt`, tests.
- **Agent prompt hint:** (frame) + "Make the synthetic history so that the real engine produces DOC 1's Aarav numbers. If a number is off, fix the synthesizer, not the engine."
- **Your job — alongside:** None — pure codegen, watch-and-verify (then look at the Aarav Mirror JSON at each preset).
- **Done when:** persona replay passes (Day 1 provisional, Week 4 ≈ 116 Steadier, Week 8 ≈ 136 Steadier, opens 5 → 3 → 2, patterns present, a MOVED verdict, a known-bad window REACHED or PINGED); `synthesize` twice gives identical output; events after `asOf` never matter.
- **Evidence required:** the replay output table (preset, parts, Steadiness) and the golden Aarav JSON at the three presets.
- **Common drift:** tuning the engine to fit the persona. Say: "The persona serves the engine, not the other way."

### Step T2.17 — Face-Validity and Tuning Pass

- **Reference:** DOC 2 §2.8.3; DOC 3 → *Event Vocabulary and Tuning*; DOC 1 §1.2.5.
- **Executor:** Human (Teammate B, all four builders participate), with Coding agent (Pro).
- **Size:** M.
- **What to build:** replay the real exported events from Track 1 (`fixtures/real_*.json`) through the engine; each teammate checks whether the day Sakshi calls steadier was the day they felt steadier; adjust `Tuning` values only. Add the real fixtures as regression tests (anonymised: package names replaced by letters).
- **Folder/file targets:** `engine/tuning/Tuning.kt` (values only), `src/test/resources/fixtures/real_*.json`.
- **Agent prompt hint:** "Here are the real windows and stays for four people (paste summaries). Propose Tuning value changes only; show the effect on each person's Steadiness; do not touch formulas."
- **Your job — alongside:** the judgment itself: for each person, steadier day or not.
- **Done when:** the verdict of each teammate is recorded; if three of five fail face validity, the weights are changed and the persona replay is re-run to stay within its tolerance; `Tuning` changes are listed.
- **Evidence required:** the table of persons × days with the felt/called match; the Tuning diff.
- **Common drift:** changing a formula instead of a number. Say: "Numbers only."

---

## Track 3 — App Shell and Trust Screens

```
TRACK 3: The Flutter shell and every screen about setup, permissions, settings and privacy.
  OWNER:       Teammate C. Executor: Coding agent (free, chat) + Human (runs flutter, pastes errors back).
  DEPENDS ON:  Track 1 Step T1.2 (Sync 1): generated Dart API and HostClient exist.
  STEPS:       7, each one prompt, one to three files.
  PATHS:       pubspec.yaml, lib/main.dart, lib/app.dart, lib/core/**, lib/host/**, lib/features/{setup,what_i_see,settings}/**, lib/features/demo/demo_banner.dart (created here, owned by Track 4 afterwards), test/ for these paths
```

**How every Track 3 and Track 4 step is run (free plan).** One chat per step. Paste, in this order: (1) the §4.0 snapshot, (2) the step entry, (3) the **named excerpts only** (the DTO classes the step uses, never the whole Pigeon file or any DOC). Ask for the files in one reply. You run `flutter analyze` and `flutter run` against `FakeHost`, paste any error back, and when it works you commit. If a chat gets long and starts forgetting rules, start a fresh chat with the same three pastes. The agent never needs the Kotlin side: everything runs on the fake host.

### Step T3.1 — Flutter Shell

- **Reference:** DOC 3 → *Flutter App Shell*.
- **Executor:** Coding agent (free).
- **Size:** S.
- **What to build:** `lib/core/theme.dart` (Material 3, calm palette, light and dark), `lib/core/router.dart` (go_router; `final routes = [...setupRoutes, ...whatISeeRoutes, ...settingsRoutes, ...mirrorRoutes, ...todayRoutes, ...shelfRoutes, ...demoRoutes]`), `lib/core/providers.dart` (`hostClientProvider` overridable; `setupStateProvider`; `syncProvider` that calls `syncNow()` then invalidates the others), `lib/core/ui_strings.dart` (the four-line first-screen text and titles only; more is added by later steps), `lib/app.dart` mounting `DemoBanner` whenever `setupState.isDemo`. Create **stub** route files with an empty list for every feature (`lib/features/<f>/<f>_routes.dart`) and a minimal `lib/features/demo/demo_banner.dart` (class `DemoBanner`).
- **Folder/file targets:** `lib/core/*`, `lib/app.dart`, `lib/main.dart`, `lib/features/*/…_routes.dart` stubs, `lib/features/demo/demo_banner.dart`.
- **Agent prompt hint:** "Create the app shell: theme, go_router with each feature exporting a routes list, Riverpod providers with manual providers only (no code generation), chrome-only strings, a DemoBanner stub. No screens yet."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** `flutter analyze` is clean; `flutter run` shows a placeholder home and the banner when the fake `isDemo` is toggled in a debug flag.
- **Evidence required:** `flutter analyze` output; a screenshot.
- **Common drift:** adding packages (a state-management or UI library). Say: "Only flutter_riverpod and go_router. No code generation."

### Step T3.2 — HostClient and FakeHost

- **Reference:** DOC 3 → *Pigeon Bridge and Fake Host*.
- **Executor:** Coding agent (free). **Paste:** the abstract `HostClient` file from T1.2 and the list of DTO classes for setup (`SetupStateDto`, `AppDto`, `WorkSetEntryDto`, `SaveResultDto`, `StudyHoursDto`, `StudyBlockDto`, `CollectionHealthDto`).
- **Size:** M.
- **What to build:** `lib/host/pigeon_host_client.dart` (wraps the generated class; converts `PlatformException` to `HostException(code, userMessage)`), `lib/host/fake_host.dart` (implements `HostClient`; in-memory state for setup progress, gentle, demo; `syncNow()` waits 400 ms and returns ok; for Mirror/Today/Lake/WhatISee/Saying calls it delegates to Track 4's `mirror_fixtures.dart` through a thin interface and returns a **placeholder DTO** until that file exists), `lib/host/fixtures/setup_fixtures.dart` (SetupState variants: nothing granted, usage granted, both granted, restricted settings suspected, all done; an app list of about 20 apps).
- **Folder/file targets:** `lib/host/pigeon_host_client.dart`, `lib/host/fake_host.dart`, `lib/host/fixtures/setup_fixtures.dart`.
- **Agent prompt hint:** "Implement PigeonHostClient and FakeHost against the abstract HostClient. FakeHost must compile with every method. Do not change HostClient or any DTO."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** `flutter analyze` is clean (a missing or renamed method fails compilation: that is the contract test); a debug screen calling `FakeHost.getSetupState()` shows each fixture variant.
- **Evidence required:** analyze output; a screenshot of the fixture switch.
- **Common drift:** changing a DTO "to make the fake easier". Say: "DTOs are locked. The fake adapts."

### Step T3.3 — Setup Flow Screens

- **Reference:** DOC 3 → *Flutter App Shell* (setup order, resume rules); DOC 1 §1.4.1.
- **Executor:** Coding agent (free). **Paste:** `SetupStateDto`, `HostClient`'s setup methods, the four-line first-screen text from `ui_strings.dart`, and `docs/research/restricted_settings.md` (R2 output).
- **Size:** M.
- **What to build:** `setup_routes.dart`, `first_screen.dart` (what Sakshi will and will not do, four lines, one button), `permission_steps.dart` (usage access step with an "Open settings" button and a status line; notification access step, optional, with the one-line "why" and, when `restrictedSettingsSuspected`, the three-step App info path from R2 with a button to `openAppInfoForRestrictedSettings()`); setup resumes from `SetupStateDto` and re-fetches on app resume (`WidgetsBindingObserver`); order: first screen → usage access → (First Look is T3.7) → notification access → work-set → study hours → age tap → battery helper → done.
- **Folder/file targets:** `lib/features/setup/{setup_routes,first_screen,permission_steps}.dart`.
- **Agent prompt hint:** "Build the first screen and the two permission steps. Never trust an assumed grant: re-read setup state on resume. No loops: if still not granted, show the restricted-settings help once."
- **Your job — alongside:** read the notification step copy aloud: it must say content is never read.
- **Stub/mock strategy:** runs against `FakeHost` setup fixtures until Track 1's real host is merged.
- **Done when:** with FakeHost, flipping `usageAccessGranted` advances the flow; the restricted-settings variant shows the three steps once; killing and restarting resumes at the right step.
- **Evidence required:** screen recording or four screenshots; analyze output.
- **Common drift:** a custom permission dialog pretending to grant. Say: "Open the Android page. Never fake a grant."

### Step T3.4 — Work-Set, Study Hours and Age Tap

- **Reference:** DOC 3 → *F2. Work-Set Setup*, *F3. Window Finder* (study hours), *F16. Gentle Mode* (age tap); DOC 1 §1.3.1.
- **Executor:** Coding agent (free). **Paste:** `AppDto`, `WorkSetEntryDto`, `SaveResultDto`, `StudyHoursDto`, `StudyBlockDto` and the related `HostClient` methods.
- **Size:** M.
- **What to build:** `work_set_screen.dart` (list with pre-ticked apps from `suggestedInSet`, a Depends toggle per ticked app, a live counter "n of 12", save disabled above 12 but the **host also enforces**, shows `SaveResultDto.userMessage`), `study_hours_screen.dart` (two sliders as minutes-from-midnight with midnight-crossing support, a "learn it for me" switch, skip allowed), `age_tap.dart` (one question, "Are you under 18?", skippable; calls `setUnder18`).
- **Folder/file targets:** `lib/features/setup/{work_set_screen,study_hours_screen,age_tap}.dart`.
- **Agent prompt hint:** "Three setup screens against HostClient. The UI shows the 12 cap but never owns it. No text fields anywhere (DOC 1: no typing)."
- **Your job — alongside:** None — pure codegen, watch-and-verify (check there is no `TextField` in these files).
- **Done when:** with FakeHost: 13th tick shows the cap message; Depends toggles persist in the saved list; a study block ending before it starts (22:00–01:30) is accepted; "learn it for me" saves `learnForMe=true`.
- **Evidence required:** analyze output; `grep -n "TextField" lib/features/setup` returns nothing; screenshots.
- **Common drift:** adding a search box. Say: "No typing. The list is short."

### Step T3.5 — Settings, Battery Helper and Weekly Note Tile

- **Reference:** DOC 3 → *F9. The Quiet Note*, *F16. Gentle Mode*; DOC 2 §2.7.4.
- **Executor:** Coding agent (free). **Paste:** `SetupStateDto`, the settings-related `HostClient` methods, and `docs/research/oem_battery.md` (R4, verified on the iQOO Z7 in T1.12).
- **Size:** M.
- **What to build:** `settings_routes.dart`, `settings_screen.dart` (gentle-mode switch, weekly-note switch (shows the one plain line when the host returns false), a link to What I see, and a "Re-do work-set / study hours" entry), `battery_helper_screen.dart` (shown once after setup; one button to `openBatterySettings()`, fixed text for all OEMs shown as a short list the user reads (no device detection), with the iQOO/Vivo and Nothing sections first; a "Skip" button; calls `markBatteryHelperShown()` on leave). Add the OEM text to `ui_strings.dart`.
- **Folder/file targets:** `lib/features/settings/{settings_routes,settings_screen,battery_helper_screen}.dart`, `lib/core/ui_strings.dart` (append only).
- **Agent prompt hint:** "Settings screen and the optional battery helper screen. The helper only opens Android's own page and shows fixed text. It never nags: shown once."
- **Your job — alongside:** read the OEM text against what Track 1 verified on the phones; any mismatch is fixed in the text, not the code.
- **Done when:** with FakeHost: toggles update fake state; the helper screen appears once and is skipped cleanly; `setWeeklyNote` returning false shows the message and leaves the switch off.
- **Evidence required:** analyze output; screenshots; the diff to `ui_strings.dart`.
- **Common drift:** asking for the battery exemption in-app. Say: "Open the page. That is all."

### Step T3.6 — What I See Screen

- **Reference:** DOC 3 → *F10. What I See (Pause, Export, Delete)*.
- **Executor:** Coding agent (free). **Paste:** `WhatISeeDto`, `ExportDto`, `HostException`, the pause/export/delete `HostClient` methods.
- **Size:** M.
- **What to build:** `what_i_see_routes.dart`, `what_i_see_screen.dart`: renders `lines` as given, shows counts, listener coverage and job health lines, a Pause switch, an Export button with an "include raw events" checkbox (default off), and a Delete everything button with a **two-step confirmation**; shows `DEMO_ACTIVE` errors as the user message; after delete navigates to the first setup step.
- **Folder/file targets:** `lib/features/what_i_see/{what_i_see_routes,what_i_see_screen}.dart`.
- **Agent prompt hint:** "Render WhatISeeDto exactly as given; write no sentences of your own about data. Two-step confirm for Delete."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Done when:** with FakeHost: Pause toggles; Export shows the returned file name; Delete requires two taps and then returns to setup; a `DEMO_ACTIVE` error shows its message.
- **Evidence required:** analyze output; screenshots including the two confirmation steps.
- **Common drift:** inventing explanatory text about what is stored. Say: "Strings come from the host. Chrome strings only from ui_strings.dart."

### Step T3.7 — First Look Screen and Copy Test

- **Reference:** DOC 3 → *F1. Instant First Look*, *Flutter App Shell* (copy_rules_test).
- **Executor:** Coding agent (free). **Paste:** `MirrorDto` and `PartsDto` (the classes), the Track 4 widget signatures once published (T4.2).
- **Size:** S.
- **What to build:** `first_look_screen.dart` (calls `syncNow()` then `getMirror(null)`, shows a progress state, then `PartsCard` and the `headline` and data lines from Track 4's widgets); `test/copy_rules_test.dart` scanning `lib/**/*.dart` string literals for the LC-8 forbidden pattern and "!"; wires the First Look into the setup order.
- **Folder/file targets:** `lib/features/setup/first_look_screen.dart`, `test/copy_rules_test.dart`.
- **Agent prompt hint:** "First Look is the provisional Mirror: reuse Track 4's parts card. Add the copy-rule test over lib/. Do not write new data sentences."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Stub/mock strategy:** until Track 4 publishes `parts_card.dart` (T4.2), use a local placeholder widget named `PartsCard` with the same constructor `PartsCard({required PartsDto parts})`; delete it when the real one lands.
- **Done when:** `flutter test` passes including the copy test; with FakeHost the First Look shows the provisional Mirror fixture.
- **Evidence required:** `flutter test` output; screenshot.
- **Common drift:** duplicating Track 4's widget. Say: "Import it. If it's missing, use the stub signature."

---

## Track 4 — Mirror, Lake and Demo Screens

```
TRACK 4: The weekly Mirror and everything the user looks at: Today, the Lake, the Saying shelf and the Time Machine.
  OWNER:       Teammate D. Executor: Coding agent (free, chat) + Human (runs flutter, makes the Lake art look right).
  DEPENDS ON:  T1.2 (Sync 1) for the DTOs. Steps T4.3 onward need Track 3's shell and FakeHost (Sync 2).
  STEPS:       7, each one to three files (T4.3 is run as two prompts).
  PATHS:       lib/features/{mirror,today,lake,shelf,demo}/**, lib/host/fixtures/mirror_fixtures.dart, test/fixtures/*.json (mirrored with Kotlin goldens), android/app/src/main/res/drawable/lake_*.xml (art only)
```

### Step T4.1 — Mirror Fixtures and Shared JSON

- **Reference:** DOC 3 → *F6. The Weekly Mirror* (test list), *Pigeon Bridge and Fake Host* (fixture names).
- **Executor:** Coding agent (free). **Paste:** all the Mirror-related DTO classes (`MirrorDto`, `PartsDto`, `PartLinesDto`, `SteadinessDto`, `StonesDto`, `ClearHourDto`, `PatternLineDto`, `SuggestionDto`, `ObservationDto`, `VerdictDto`, `GoalTapDto`, `TeacherDto`, `SayingDto`, `StudyBlockDto`, the enums). Include `reanchorOffered` (true in `steady` at week 5) and `suggestedStudyBlock` (set in `steadier`).
- **Size:** M.
- **What to build:** `lib/host/fixtures/mirror_fixtures.dart` with the sixteen named variants: `firstLook`, `learning`, `steady`, `steadier`, `wavering`, `gentle`, `partialPing`, `pingOff`, `notSeen`, `tooLittle`, `unusualWeek`, `nothingToFix`, `withSuggestion`, `withVerdict`, `afterLapse`, `demoAarav`; plus matching `TodayDto`, `LakeDto` (all five states), `WhatISeeDto` and `SayingDto` lists. Write sentences by hand in the DOC 1 voice (these are **placeholders** that the real golden JSON from Track 2 replaces at T4.7). A `test/fixtures/` folder for the JSON copies.
- **Folder/file targets:** `lib/host/fixtures/mirror_fixtures.dart`, `test/fixtures/` (later).
- **Agent prompt hint:** "Create the sixteen Mirror fixtures as const DTOs. Sentences follow DOC 1's defensible-sentence rules: no 'focused', 'distracted', 'wasted', no exclamation marks. Gentle fixture has no Steadiness."
- **Your job — alongside:** read every fixture sentence against the forbidden list.
- **Done when:** `flutter analyze` clean; a test builds each fixture; the copy-rule test (T3.7, when merged) passes on them.
- **Evidence required:** analyze/test output; the list of fixture names.
- **Common drift:** inventing DTO fields to make a fixture easier. Say: "Use only the locked fields."

### Step T4.2 — Mirror Content Widgets

- **Reference:** DOC 3 → *F6. The Weekly Mirror*; DOC 1 §1.3.2 F6 (display order).
- **Executor:** Coding agent (free). **Paste:** `PartsDto`, `PartLinesDto`, `StonesDto`, `PatternLineDto`, `ClearHourDto`.
- **Size:** M.
- **What to build:** `parts_card.dart` (`PartsCard({required PartsDto parts})`: four parts with the in-set versus quiet split beside Stretch, shows `lines` as given, a quiet "more" area for `extrasLines`, null parts show nothing but never "0"), `stones_card.dart` (the line, stone/self/unknown counts as a simple three-segment bar, top stone label), `patterns_card.dart` (the `line` of each pattern with its evidence count visible; at most 4).
- **Folder/file targets:** `lib/features/mirror/{parts_card,stones_card,patterns_card}.dart`.
- **Agent prompt hint:** "Display-only widgets for DTOs. No arithmetic beyond bar widths. A null value renders nothing, never zero."
- **Your job — alongside:** None — pure codegen, watch-and-verify.
- **Stub/mock strategy:** develop with a throwaway harness `lib/dev/t4_harness.dart` (delete in T4.7) that renders the cards from the fixtures; this does not need Track 3's router.
- **Done when:** the three widgets render all relevant fixtures including `tooLittle` (nothing shown for null parts) and `partialPing`; **publish `PartsCard` to the shared branch immediately** (Track 3's T3.7 depends on it).
- **Evidence required:** screenshots from the harness for four fixtures; analyze output.
- **Common drift:** computing percentages or adding verdict colours. Say: "Strings and numbers as given. No red/green judgement."

### Step T4.3 — Mirror Screen

- **Reference:** DOC 3 → *F6. The Weekly Mirror*, *F14*, *F15*, *F16*, *F17*; DOC 1 §1.3.2 (order 1 to 8).
- **Executor:** Coding agent (free), run as **two prompts**. **Paste (prompt A):** `SuggestionDto`, `ObservationDto`, `VerdictDto`, `GoalTapDto`, `TeacherDto`, `StudyBlockDto`, `StudyHoursDto`. **Paste (prompt B):** `MirrorDto`, `WeekRefDto`, the Mirror-related providers from `lib/core/providers.dart`, the existing widget file names.
- **Size:** L.
- **What to build:** *Prompt A:* `suggestion_card.dart` (one line, one action button: `tapTryThis`, and "Not now" calling `dismissSuggestion`; when `opensSettings` the action opens the settings first), `verdict_card.dart`, `goal_tap.dart` (three chips, one tap, `tapGoal`), `teacher_line.dart`, `reanchor_card.dart` (the week-4 offer when `reanchorOffered`: one plain line, one button calling `reanchorBaseline()`, one "Not now"), `study_hours_card.dart` (when `suggestedStudyBlock` is non-null: shows the learned block with "Use it" calling `saveStudyHours` and "Keep mine"). *Prompt B:* `mirror_content.dart` (the single scroll in DOC 1 order: data flags above the data they qualify, headline, parts, top stone, clear hour, patterns, saying footer, suggestion or "Nothing to fix this week", verdict, goal tap, teacher line, lapse line), `mirror_screen.dart` (provider-backed, week picker through `listMirrorWeeks`, loading and error states with retry), `mirror_routes.dart` replacing Track 3's stub.
- **Folder/file targets:** `lib/features/mirror/*.dart`.
- **Agent prompt hint:** "Compose the Mirror in the DOC 1 order from MirrorDto. No dashboard, no daily number, no badges. Gentle mode shows only what the DTO contains."
- **Your job — alongside:** compare each fixture screen with the DOC 1 §1.3.2 order; reject any decorative element that adds a number or colour judgement.
- **Done when:** with FakeHost every fixture variant renders; `nothingToFix` shows "Nothing to fix this week" as a normal state; `gentle` shows no Steadiness widget, no re-anchor card and no study-hours card; `partialPing` shows its flag line above the stones card; error/loading states exist.
- **Evidence required:** screenshots of six fixtures (steady, wavering, gentle, partialPing, nothingToFix, withVerdict); analyze output; widget tests for the three structural rules above.
- **Common drift:** adding a streak-style or score-ring visual. Say: "No chains, no rings, no badges. One scroll."

### Step T4.4 — Today Screen, Lake Painter and Lake Art

- **Reference:** DOC 3 → *F8. Ask Now (Today So Far)*, *F7. The Lake (Home-Screen Widget)*.
- **Executor:** Coding agent (free) + Human (art review). **Paste:** `TodayDto`, `TodayWindowDto`, `LakeDto`, `LakeStateDto`.
- **Size:** M.
- **What to build:** `today_screen.dart` and `today_routes.dart` (completed windows only, shown as simple rows; **no suggestion, no Steadiness**; reached only from a quiet menu entry in the Mirror, never pushed), `lake_painter.dart` (CustomPainter for still, rippled, choppy; learning and noData draw still), and the **three vector drawables** `lake_still.xml`, `lake_rippled.xml`, `lake_choppy.xml` in the same art language, to replace Track 1's placeholders **under exactly those names** in `android/app/src/main/res/drawable/`.
- **Folder/file targets:** `lib/features/today/*`, `lib/features/lake/{lake_painter,lake_routes}.dart`, `android/app/src/main/res/drawable/lake_*.xml`.
- **Agent prompt hint:** "Render the Lake in three states, in Flutter and as Android vector drawables with the same look. Calm, no counters, nothing live. Vector paths only."
- **Your job — alongside:** judge the three lakes on a real phone at widget size; the art decides whether the demo's second beat feels alive.
- **Done when:** the painter and the drawables show three clearly different states; Today renders `firstLook`-style empty state ("No finished window yet today.") and a two-window state.
- **Evidence required:** screenshots of the three lakes in-app and as widget placeholders replaced on the phone (via Track 1's debug action).
- **Common drift:** adding animation or minutes to the widget art. Say: "Static. Last window only."

### Step T4.5 — Saying Shelf UI

- **Reference:** DOC 3 → *F13. The Saying Shelf*.
- **Executor:** Coding agent (free). **Paste:** `SayingDto`, `HostClient.getSayingChoices` and `pickSaying`.
- **Size:** S.
- **What to build:** `saying_picker.dart` (three cards, one tap, shows text, source line and tier label; a "Not now"), `saying_footer.dart` (the chosen saying under the Mirror data with source and tier label), `shelf_routes.dart`. No command language around the text.
- **Folder/file targets:** `lib/features/shelf/*.dart`.
- **Agent prompt hint:** "Show the saying exactly as given, with its source and tier label. Never present it as an instruction."
- **Your job — alongside:** check three tier labels display correctly (A, C, D).
- **Done when:** with FakeHost the picker shows three, a tap stores the pick and the footer shows it; an empty list shows nothing.
- **Evidence required:** screenshots; analyze output.
- **Common drift:** paraphrasing or truncating the saying. Say: "Verbatim. Show source and tier."

### Step T4.6 — Time Machine Screen

- **Reference:** DOC 3 → *Time Machine Demo and Personas*; DOC 1 §1.4.4.
- **Executor:** Coding agent (free). **Paste:** `HostClient` demo methods and the `DemoBanner` file.
- **Size:** M.
- **What to build:** `demo_screen.dart` and `demo_routes.dart`: persona chips (Aarav default, Meera, Rohan), three preset chips (Day 1 = 3, Week 4 = 31, Week 8 = 59), a slider 0..59 calling `setDemoAsOf` on release (debounced), the Mirror shown beneath via the normal Mirror widgets, a clear "Stop demo" button; the `DemoBanner` is permanent while `isDemo`. Starting the demo calls `startDemo` and shows a progress state.
- **Folder/file targets:** `lib/features/demo/{demo_screen,demo_routes,demo_banner}.dart`.
- **Agent prompt hint:** "Time Machine: persona, three presets and a slider. Always show the Demo data banner. Never present demo numbers as results."
- **Your job — alongside:** None — pure codegen, watch-and-verify (check the banner on every demo screen).
- **Done when:** with FakeHost the presets change the Mirror fixture shown; the banner is visible on every screen while demo is on; Stop demo returns to normal.
- **Evidence required:** a screen recording of the three presets.
- **Common drift:** hiding the banner "for polish". Say: "Permanent. It is the honesty of the demo."

### Step T4.7 — Demo Flow and Rehearsal Pass

- **Reference:** DOC 1 §1.4.4 (three beats); DOC 5 (when written); DOC 3 → *Pigeon Bridge and Fake Host* (contract test).
- **Executor:** Human (Teammate D) + Coding agent (free).
- **Size:** M.
- **What to build:** replace hand-written fixtures with the golden JSON from Track 2 (T2.15, T2.16) **and** make the Dart contract test decode the same files; delete `lib/dev/`; rehearse the three beats on the Nothing Phone 3a: (1) real phone, First Look; (2) Time Machine Day 1, Week 4, Week 8 on Aarav; (3) the Teacher Leaves meter falling over the eight weeks; fix visual issues found only in `lib/features/**` and the Lake art.
- **Folder/file targets:** `test/fixtures/*.json`, `test/contract_test.dart`, `lib/host/fixtures/mirror_fixtures.dart`, UI polish files.
- **Agent prompt hint:** "Swap the fixtures to decode the shared golden JSON files and keep all screens rendering. Fix only layout problems found in rehearsal; do not add features."
- **Your job — alongside:** the rehearsal itself, with a stopwatch; note the weakest moment of each beat for DOC 5.
- **Done when:** the contract test decodes every golden JSON into DTOs; the three beats run end to end on the real phone without a visual glitch; notes for DOC 5 exist.
- **Evidence required:** contract-test output; a screen recording of the three beats; the notes file.
- **Common drift:** a late "just one more screen". Say: "Rehearse what exists. Cuts are in §4.1c."

---

## §4.1a — Sync Points

```
SYNC 1: After Track 1, Step T1.2 → before Track 2 Step T2.1, Track 3 Step T3.1, Track 4 Step T4.1 may start
  What must be confirmed:  tag contracts-v1 builds (flutter build apk --debug; ./gradlew testDebugUnitTest), the Pigeon-generated files exist on both sides, HostClient exists,
                           and each teammate has cloned it and run the build once.
  Who confirms it:         each consuming track's owner (B, C, D), by posting the green build.
  While waiting:           Track 2: read DOC 3's Foreground Intervals and F5 and draft the event DSL in a scratch file. Track 3: draft ui_strings.dart text.
                           Track 4: sketch the three lakes. (No code in repo paths.)

SYNC 2: After Track 3, Step T3.2 → before Track 4 Step T4.3 may start
  What must be confirmed:  `FakeHost`, `hostClientProvider`, `setupStateProvider`, router with stub route files, and the DemoBanner stub are on main and build.
  Who confirms it:         Track 4's owner (D).
  Stub meanwhile:          Track 4 runs T4.1, T4.2 and T4.4 painter work in `lib/dev/t4_harness.dart`.

SYNC 3: After Track 2, Step T2.8 and Track 1, Step T1.5 → before Track 1 begins merging engine work on the phone
  What must be confirmed:  `processNewEvents` passes the five golden fixtures; the real dump `fixtures/real_s_a.json` replays through intervals to the expected stays.
  Who confirms it:         Track 1's owner (you).
  Effect:                  you may now run ingest → derived rows on the real phone (the Mirror is still canned).

SYNC 4: After Track 2, Step T2.15 and Track 1, Step T1.9 → before Track 1 Step T1.15 may start
  What must be confirmed:  every façade method is real; golden JSON for all views exists in both test resource folders; the mapper tests decode them; `./gradlew testDebugUnitTest` is green on the merge.
  Who confirms it:         Track 1's owner (you), reviewing Track 2's Evidence.

SYNC 5: After Track 1, Step T1.15 (first real exported events) → before Track 2 Step T2.17 may start
  What must be confirmed:  `fixtures/real_*.json` exists for at least two phones' worth of days; names anonymised.
  Who confirms it:         Track 2's owner (B).

SYNC 6: After Track 2, Step T2.15/T2.16 golden JSON → before Track 4 Step T4.7 may start
  What must be confirmed:  golden JSON for all sixteen Mirror states, Today, Lake (five states), WhatISee, and Aarav at the three presets are on main.
  Who confirms it:         Track 4's owner (D).

SYNC 7: After Track 4, Step T4.2 (parts_card published) → before Track 3 Step T3.7 may finish
  What must be confirmed:  `PartsCard({required PartsDto parts})` is on main with that exact signature.
  Who confirms it:         Track 3's owner (C). Stub meanwhile: the placeholder widget in T3.7.

SYNC 8: After Track 3 Step T3.7 and Track 4 Step T4.3 and Track 1 Step T1.15 → before the UI-on-real-host merge (Checkpoint 6)
  What must be confirmed:  FakeHost is replaced by PigeonHostClient in `hostClientProvider` on a release build and every screen renders real data without a crash.
  Who confirms it:         Integration Owner (you).

SYNC C (any time): a Contract Change (DOC 3 process)
  What must be confirmed:  DOC 3's DEFINITION is updated, the affected owners (USED BY) have acknowledged, Pigeon is regenerated in the same commit as the Kotlin and Dart sides.
  Who confirms it:         Integration Owner (you) approves LC-2, LC-3, LC-4.
```

---

## §4.1b — Ownership and Merge Strategy

```
OWNERSHIP MAP:
  Track 1 (you)      owns: android/app/src/main/kotlin/com/kleos/sakshi/host/**, .../data/**, MainActivity.kt, AndroidManifest.xml, res/xml/**, res/layout/**,
                           pigeons/sakshi_api.dart, android/app/src/test/.../data/**, .../arch/**, tools/audit_apk.sh, docs/spikes.md, docs/versions.md, docs/research/**, fixtures/real_*.json
  Track 2 (B)        owns: android/app/src/main/kotlin/com/kleos/sakshi/engine/** (including SakshiEngine.kt after T2.8), android/app/src/test/.../engine/**, src/test/resources/{fixtures,golden}/**
                           (T1.2 transcribes the contract files in engine/{model,ports,tuning}/ once; after contracts-v1, Track 2 edits them only for Tuning VALUES and engine-internal types in a marked block;
                            any other change is a Contract Change)
  Track 3 (C)        owns: pubspec.yaml, lib/main.dart, lib/app.dart, lib/core/**, lib/host/{host_client,pigeon_host_client,fake_host}.dart (host_client.dart is edited only through a Contract Change),
                           lib/host/fixtures/setup_fixtures.dart, lib/features/{setup,what_i_see,settings}/**, test/copy_rules_test.dart
  Track 4 (D)        owns: lib/features/{mirror,today,lake,shelf,demo}/** (demo_banner.dart created by Track 3 in T3.1, owned by Track 4 afterwards), lib/host/fixtures/mirror_fixtures.dart,
                           test/fixtures/*.json, test/contract_test.dart, android/app/src/main/res/drawable/lake_*.xml (art files only), android/app/src/main/assets/sakshi/sayings.json
  SHARED (coordinate before touching; from DOC 3 Shared Surfaces):
    pigeons/sakshi_api.dart, engine/model/{Entities,Views}.kt, engine/ports/Ports.kt, engine/tuning/Tuning.kt, host/Mappers.kt (Track 1 writes, breaks if Views or the Pigeon file change),
    lib/core/*, lib/host/host_client.dart, lib/host/fixtures/*, res/drawable/lake_*.xml (names frozen), lib/features/mirror/{parts_card,mirror_content}.dart (published by Track 4 for Track 3's First Look),
    pubspec.yaml (dependencies frozen: flutter_riverpod, go_router; pigeon and flutter_test as dev)

RULE: an agent or person working a track must not edit a path outside that track's Ownership Map entry without checking Shared Surfaces / a Sync Point first. A teammate who needs a change in
      someone else's path asks the owner (or files a failing test or fixture) instead of editing. The agent's session prompt carries the track's path list (§4.2).

INTEGRATION OWNER: You. Responsible for performing every merge below, keeping `main` green (both build commands pass), approving Contract Changes, and reviewing the Evidence Package of each track at its merge point (Maker ≠ Checker).

BRANCHING: One branch per track: feat/t1-platform, feat/t2-engine, feat/t3-shell, feat/t4-mirror. Commit messages and PR titles name the track and step, e.g. `feat(t2): step T2.7 — parts, steadiness and baseline`. Each teammate rebases on `main` after every merge point.
           Because each builder uses their own laptop, each has their own clone; no shared working directory exists.

ISOLATION: you run two agent sessions at once (a Track 1 step and an integration check): use `git worktree add ../sakshi-integration main` for the integration session so uncommitted Track 1 changes never collide with it.

MERGE POINTS:
  MP1: contracts-v1 → main after T1.1 and T1.2 (everyone branches from it).
  MP2: Track 3 into main after T3.1 and T3.2 (shell and fake host), before T4.3.
  MP3: Track 2 into main after T2.8 (engine core) and Track 1 into main after T1.5; both before Sync 3 verification.
  MP4: Track 2 into main after T2.15 and Track 1 after T1.9; both before T1.15.
  MP5: Track 3 after T3.7 and Track 4 after T4.3; both before Checkpoint 5.
  MP6: Track 1 T1.15/T1.16, Track 2 T2.16/T2.17, Track 4 T4.7 into main = the release candidate, before the deployment checklist.
DEFINITION OF DONE (per merge point): builds clean (`flutter build apk --debug` and `./gradlew testDebugUnitTest` both pass on the merged result); the pushing track's Evidence Package is attached to the PR;
  no edits outside the pushing track's Ownership Map; no locked contract changed outside a Contract Change commit; DependencyRuleTest, PrivacyBoundaryTest, SayingShelfIntegrityTest and copy tests green;
  the track's checkpoint (§4.3) for this point has been run.
```

---

## §4.1c — Descope Order

Tied to milestone state, not the clock. Decided now, while calm.

```
IF Track 1 has not reached Step T1.15 (real engine on the real phone) by the time Track 2 finishes Step T2.12 (suggestion rules and selector):
  THEN cut, in this order:
    1. The clustering step (Step T2.11, Clustering.kt): optional by design; no other module depends on it.
    2. Personas Meera and Rohan (keep Aarav) in Step T2.16 and Step T4.6.
    3. The Quiet Note (F9): Step T1.11, the Track 3 weekly-note tile in T3.5, and the T2.15 note rules (keep `WeeklyNote.decide` returning "no").
    4. Pattern P3 Shift (no suggestion depends on it).
    5. Ask now (F8): the Today screen in Step T4.4 (keep the Lake painter and drawables).
    6. The Saying picker (keep the read-only footer showing one fixed saying): Step T4.5 picker; ChooseSayings stays.
    7. The cross-day link and the break-point pattern, with suggestions S8 and S9 (Step T2.11 detectors, Step T2.12 rules).
    8. The battery helper screen (Step T3.5 screen; keep the one-line link to Android's page and the verified text in docs).

IF Track 3 or Track 4 has not reached Step T3.6 / Step T4.3 by the time Step T1.15 completes:
  THEN switch the Mirror to "plain list mode": render MirrorDto as a scrolling list of its strings in DOC 1 order with no cards; drop, in order: the three-segment stones bar,
    the patterns card styling, the Today screen, the re-anchor card. The strings and data are unchanged.

IF the iQOO Z7 shows (Step T1.14) that the listener or the periodic job cannot be kept alive even with the helper:
  THEN cut nothing. Ship the honest states (F17), demo on the Nothing Phone 3a, and say "tested on a Vivo-family phone: partial ping awareness" in the pitch.

Never cut: ingest, foreground intervals, windows, stays, returns, stone and wave, Steadiness against the frozen baseline, the First Look and the Weekly Mirror, the Lake widget (required in the demo),
  What I see with Pause/Export/Delete, the no-INTERNET release manifest, the Time Machine with Aarav and its permanent "Demo data" banner, the Teacher Leaves meter (demo beat 3), the honest data states, gentle mode.
```

---

## §4.2 — Agentic Coding Rules

Derived from DOC 2 §2.5 and DOC 3. These go into AGENTS.md. Each guards a named drift (from the vibe-coding anti-pattern catalogue).

```
SESSION START (every session, every track):
  1. Paste the §4.0 snapshot, then ONLY the one step entry you are working on, then ONLY the DOC 3 section that step cites. Never load DOC 1 in full; never load another step.
  2. State the track's path list from §4.1b. Edits outside it are forbidden.
  3. One step per session. Do not start the next step until the Evidence for this one has been shown and checked.

ALWAYS:
  [ ] Write the golden tests named in the step first, then make them pass (Track 2 and Track 1 data/host steps).
  [ ] Show the Evidence Package before saying "done": the command output for the Done-when check, the diff limited to the step's targets, one line saying why it satisfies the check.
  [ ] Use the ports: engine code takes time from Clock and randomness from Randomness.
  [ ] Put every DOC 1 number in engine/tuning/Tuning.kt and reference it by name.
  [ ] Put every user-facing sentence about the user's data in SentenceBuilder; Flutter shows strings it is given.
  [ ] Treat "unknown", "partial" and "not seen" as values (null or a sentence), never zero.
  [ ] Run `flutter analyze` (Flutter) or `./gradlew testDebugUnitTest` (Kotlin) before reporting.
  [ ] When the build breaks, fix it in place with the smallest change, and show the error you fixed.
  [ ] Note repetition worth abstracting as `[REFACTOR CANDIDATE: …]` in a comment and continue.

NEVER:
  [ ] Never import android.*, androidx.*, host.*, data.* or io.flutter.* inside engine/; never call System.currentTimeMillis, Instant.now, LocalDateTime.now or Random() there.
  [ ] Never add a field, method, enum value or port to a locked contract (LC-1 to LC-9); propose it through the Contract Change Process.
  [ ] Never add a text field to NotifEvent, never read notification title or text, never log package names in release.
  [ ] Never add a dependency, library or Gradle plugin not listed in §4.0; never upgrade a version; never restructure the Flutter template or split the Android module.
  [ ] Never add INTERNET, accessibility, overlay, device-admin, foreground-service, battery-exemption or QUERY_ALL_PACKAGES to the release manifest.
  [ ] Never put logic about attention in collectors, the widget, HostApiImpl, Mappers or any Flutter file.
  [ ] Never refactor code outside the current step; report what needs changing and wait.
  [ ] Never gold-plate: no loading spinners, animations, logging infrastructure or error UI the step did not specify.
  [ ] Never merge two build steps; never start the next step early.
  [ ] Never add a text field to the setup or measurement path; there is no typing in Sakshi.
  [ ] Never use a streak, badge, score ring, daily number, leaderboard, share card, or any sentence containing the forbidden words (focused, distracted, wasted, failed, streak, "you should", "!").
  [ ] Never make the app block, delay, lock or hide anything.

FREE-PLAN SESSIONS (Tracks 3 and 4):
  - One chat per step; the three pastes only. Ask for all files of the step in a single reply.
  - If the chat starts ignoring rules or inventing fields, start a fresh chat with the same pastes. Do not argue with a degraded session.
  - The human runs the compiler and pastes the exact error back; the agent never guesses about a build it cannot see.

IF THE AGENT GOES OFF-TRACK:
  Scope drift:     "Stop. We are only doing [step]. Finish its Done-when before anything else."
  Contract drift:  "That field is in a locked contract. Revert it, and write the change as a proposal with the affected tracks."
  Layer drift:     "That decision belongs in engine/. Move the rule there with a golden test, or leave it undone."
  Build-fix drift: "Fix the error in place. Show me the error and the smallest change. No new libraries, no restructuring."
  Self-grading:    "Show the command output, not a summary. Done means the Evidence is on the table."
  Copy drift:      "Rewrite as an observation with its evidence count. No advice words, no exclamation marks."
```

---

## §4.3 — Integration Checkpoints

```
CHECKPOINT 1: After T1.1 and T1.2
  What to verify:   the skeleton and the full contract build and test on every teammate's laptop.
  How to test it:   `flutter build apk --debug`; in android/: `./gradlew testDebugUnitTest`; run the app on a phone and press the Pigeon test button.
  If it breaks:     version mismatch (Pigeon, KSP or AGP; check docs/versions.md) or a Pigeon option name; fix the generator option, not the contract.

CHECKPOINT 2: After T2.8 and T1.5 (Sync 3)
  What to verify:   real events become derived rows on the real phone.
  How to test it:   debug "Dump events" on the Nothing Phone 3a, replay through the engine in a local test; then run ingest on the phone and read the derived windows/stays via a debug screen.
  If it breaks:     intervals first (OEM event semantics, spike S-A): add the failing real fixture to Track 2's G-I tests.

CHECKPOINT 3: After T1.9, T2.15 (Sync 4)
  What to verify:   all 30 host methods return real data; mapper goldens match.
  How to test it:   the debug screen calls each method; `./gradlew testDebugUnitTest` green with golden JSON in place.
  If it breaks:     Mappers.kt against Views.kt (field name drift) before blaming the engine.

CHECKPOINT 4: After T1.15
  What to verify:   the First Look on the real phone matches reality, and the Lake updates when a window ends.
  How to test it:   grant usage access on the Nothing Phone 3a; compare the four parts with Android's Digital Wellbeing for plausibility; finish a window; check the widget's "as of" time.
  If it breaks:     the 15-minute finalisation lag (a recent window looks short); then Window finder rules D9; then the worker cadence (spike S-D).

CHECKPOINT 5: After T3.7 and T4.3 (MP5)
  What to verify:   the full UI runs on FakeHost end to end.
  How to test it:   `flutter test`; run the setup flow and open every fixture state of the Mirror; copy test passes.
  If it breaks:     a DTO nullability mismatch between a fixture and a widget; then router stubs not replaced.

CHECKPOINT 6: After Sync 8 (UI on the real host)
  What to verify:   the real app on both phones: setup, First Look, Mirror, What I see (Pause, Export, Delete), widget, Time Machine.
  How to test it:   run the three beats of DOC 1 §1.4.4 on the Nothing Phone 3a; run setup, pause and background test on the iQOO Z7.
  If it breaks:     host call errors (look at the HostException code first), then provider invalidation after syncNow.

CHECKPOINT 7: After T2.16 and T4.7
  What to verify:   the persona replay in the app matches the Aarav figures (about 100, 116, 136) and the Demo data banner is on every demo screen.
  How to test it:   Time Machine presets Day 1, Week 4, Week 8; read Steadiness and the opens meter.
  If it breaks:     the synthesizer calibration (T2.16), never the engine formula.

CHECKPOINT 8: After T1.16
  What to verify:   the signed release APK works with no network and has no forbidden permission.
  How to test it:   `tools/audit_apk.sh`; install by file on both phones; airplane mode on; complete setup and open the Mirror.
  If it breaks:     a debug-only dependency leaking into release (permissions list) or the restricted-settings step not followed.
```

---

## §4.4 — Deployment Checklist

Distribution is a sideloaded APK (DOC 2 §2.2), so deployment is about the file and the phones.

```
[ ] Release keystore created and kept OUT of git; its password stored by you only
[ ] `flutter build apk --release` passes with no analyzer errors and signs with the release keystore
[ ] `tools/audit_apk.sh` prints the permission list and exits 0 (no INTERNET, accessibility, overlay, device admin, foreground service, battery exemption, query-all-packages)
[ ] `allowBackup=false` and data-extraction rules present in the merged manifest
[ ] `./gradlew testDebugUnitTest` and `flutter test` green on the final merge of main
[ ] Install by file on the Nothing Phone 3a (demo phone) and the iQOO Z7 (stress phone); a spare copy of the APK on a second device in case of a corrupted transfer
[ ] On each phone: follow the sideload path from docs/research/restricted_settings.md BEFORE the demo (App info → ⋮ → Allow restricted settings) so notification access can be granted
[ ] Grant usage access; confirm First Look appears; grant notification access (or confirm "ping awareness off" text appears if the toggle won't move)
[ ] Add the Lake widget to the demo phone's home screen; confirm it redraws (use the demo clock if no window has ended)
[ ] Run one full Time Machine pass on the demo phone (Aarav, Day 1 → Week 4 → Week 8); confirm the Demo data banner and the opens meter
[ ] Airplane-mode run: setup, First Look, Mirror all work with no network (proves the no-INTERNET claim)
[ ] Smoke test: What I see shows both permissions, counts and job health; Pause, Export (share sheet opens) and Delete (returns to setup) each work once; then re-grant and re-run the First Look
[ ] Battery helper: skipped-versus-applied result recorded for the iQOO Z7 in docs/spikes.md (the answer to a judge's "does it survive the OEM killer?")
```

---

**⛔ GATE:** Does this build guide match how you want to work? Confirm DOC 4 before anyone starts coding: its build sequence is the contract, and deviations should be conscious decisions. After that I will write DOC 5 (the demo and pitch narrative) and AGENTS.md.
