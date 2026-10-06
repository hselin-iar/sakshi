## Table of Contents
- §2.0 — In Plain Words, and Constraints Carried From DOC 1
- §2.1 — Architecture Overview
- §2.2 — Technology Stack Decisions
- §2.3 — Data Architecture
- §2.4 — API Design
- §2.5 — Clean Architecture & Engineering Principles
- §2.6 — Privacy & Permissions Architecture
- §2.7 — Background Execution & Failure Behaviour
- §2.8 — Testing & Verification Architecture
- §2.9 — Track Seams (Input to DOC 3 and DOC 4)
- §2.10 — Device Spikes, Risks & Open Questions

---

# Sakshi (साक्षी): System & Technical Architecture (DOC 2, v3)

**Builds on:** DOC 1 v7.1. This document decides *how* Sakshi is built. It does not cover class-level design (DOC 3), build order or track steps (DOC 4), or the demo script (DOC 5). It contains no hour estimates.
**Tags:** `[ASSUMPTION: …]` is a choice I made without verification and you can overrule. `[VERIFY ON DEVICE: …]` is a platform behaviour I believe but have not tested; §2.10 lists the spikes that settle each one. `[KNOWN RISK: …]` is a risk I accept on purpose.

**What changed from v2, and why.** You set the rule: *functionality over understanding, nothing cut, and the most vibe-coding-friendly option for every stack choice.* So:
- **Kotlin is back, and the engine is in Kotlin again.** The engine runs in the background, computes the Lake and the weekly note with no UI alive, and refreshes the widget on its own. v2's "nothing computes in the background" compromise is gone.
- **Every stack choice is now judged by one extra test: how well can an AI agent write it, and how clear are the errors when it gets it wrong?** Where the first draft picked the *best* tool, this version picks the *easiest for an agent to get right*. Each decision in §2.2 carries a "vibe-fit" line.
- **Nothing is cut.** Every DOC 1 feature, the pattern layer, the clustering step, the widget, the weekly note and the Time Machine remain. One thing is added: an optional, skippable battery-setup helper (§2.7.4), because you asked for background functionality to actually work on hostile phones like the iQOO Z7.

| Your answer | What I did with it |
|---|---|
| Functionality over understanding; add Kotlin and the background functionality | Kotlin engine and background computation restored (§2.1, §2.7) |
| Every teammate is a vibe coder for every stack; cut nothing | No feature, track or stack layer removed. Stack chosen for agent-friendliness (§2.2) |
| Phones: Nothing Phone 3a, iQOO Z7, and others | Nothing Phone 3a becomes the demo phone; **the iQOO Z7 becomes the deliberate hostile-OEM stress phone** (§2.10.3) |
| Earlier: Android 10 floor, Integration Owner is you, Lake widget in the demo, study day at 04:00 | Unchanged. Retention stays at 14 days. |

---

## §2.0 — In Plain Words, and Constraints Carried From DOC 1

### 2.0.1 In plain words

Sakshi is one Android app with four boxes. None of the team has to read the code in them. Each box has a way to check it works without reading it.

1. **The Collectors (Kotlin).** They write down what Android tells them: which app was in front, when the screen went on or off, which app sent a notification (never what it said). They decide nothing.
2. **The Brain (Kotlin, the engine).** It turns those notes into *stretches*, *stays* and *returns*, then Steadiness, patterns and the one suggestion. It is a calculator: the same notes always give the same answer. That is why it can be checked by automatic tests, with no phone. It runs in the background on a schedule, so the home-screen Lake and the weekly note stay fresh without you opening the app.
3. **The Face (Flutter screens).** It only shows what the Brain worked out and never calculates anything.
4. **The Bridge (Pigeon).** A generated contract between the Face and the Brain. If either side disagrees about a field, the build breaks instead of the app misbehaving.

Because nobody reads the code, **trust comes from checks, not from understanding**: automatic tests for the Brain, a fixed contract for the Bridge, and a short list of tap-through checks on real phones (§2.10). Those checks are the architecture's safety net, not an afterthought.

### 2.0.2 Constraints carried from DOC 1

| Constraint from DOC 1 | Decision in DOC 2 | Where |
|---|---|---|
| Phone-only Android APK, Flutter plus Kotlin platform channels | Flutter for screens, Kotlin for everything that touches Android or computes | §2.1, §2.2 |
| Passive-first, no typing in the measurement path | Only two Android readers write measurements; no screen can write one | §2.1, §2.6 |
| A killed service must not lose measurement | Idempotent catch-up from Android's retained event log; a background job also keeps the data fresh | §2.7 |
| Notification awareness is the weak link | Listener coverage stored as intervals; a stay outside coverage is `Unknown`, never "self-started" | §2.3, §2.7 |
| First Look from retained days; Time Machine demo | The engine is a **pure function of (events, as-of clock)**; demo personas run through the real engine | §2.1, §2.8 |
| No control: Sakshi cannot block or lock | The manifest has **no accessibility service, no overlay permission, no device admin, no foreground service** | §2.6 |
| No accounts, no cloud | The release manifest has **no INTERNET permission**, a checkable claim | §2.6 |
| Pattern layer: statistics plus one tiny clustering step | Pure Kotlin, deterministic (seeded), no model file, no ML runtime | §2.2, §2.5 |
| Day-60 survival under OEM battery killers | Idempotent work, no wake locks, plus an optional guided battery setup and a real stress phone | §2.7, §2.10 |
| Neutral-app list, app-category pre-tick, `<queries>` | Settled below | §2.3, §2.6 |

### 2.0.3 Answers to the "Additional Technical Questions" (decided, not asked)

- **Data volumes.** One user per install. Raw usage events: hundreds to a few thousand a day. `[ASSUMPTION: a heavy phone stays under about 10,000 raw events a day; spikes S-A and S-B measure it.]` No scale problem and no scale ambition.
- **Real-time.** None. Screens are pull-only, and Android's event log can lag by minutes anyway.
- **Background processing.** A periodic job (best effort, 15-minute minimum) that ingests, recomputes, refreshes the widget, and decides whether the optional weekly note is due.
- **Authentication.** None.
- **Files and media.** One JSON export, user-initiated, through the share sheet.
- **NFRs.** Privacy (nothing leaves the phone), battery (no persistent process, small jobs), correctness under lateness (running late or twice gives the same answer), determinism (same events, same clock, same output).
- **Open source.** `[ASSUMPTION: the repo may go public after the hackathon; no secrets exist because there are no keys.]`
- **Minimum Android version.** `minSdk 29` (confirmed).

---

## §2.1 — Architecture Overview

```
ARCHITECTURE STYLE: Modular monolith, on-device, one APK, one Android Gradle module.
  Layered with the Dependency Rule enforced by an automatic test:
    a pure-Kotlin ENGINE package that may not import Android,
    a DATA/HOST layer that implements the engine's ports and talks to Android,
    a Flutter UI that holds no logic.

RATIONALE:
  - Monolith-first (reference §1): one team, one APK, thresholds that will be tuned.
  - The domain is the product (Clean Architecture, reference §2). Windows, stays, returns,
    Steadiness, patterns and suggestion rules are pure computations on timestamped events,
    so they live in an inner layer with no framework dependencies.
  - Android access (usage events, notification listener, WorkManager, widget) is
    infrastructure behind ports, so the engine runs on a laptop against fakes.
  - Running the engine in Kotlin lets the background job, the widget updater and the
    weekly note use it with no UI process alive. That is the functionality you asked for.
  - ONE Gradle module, not several. Splitting the engine into its own Gradle module is the
    stronger way to enforce the Dependency Rule, but multi-module setup inside a Flutter
    project is a known source of build errors that an agent finds hard to fix. The rule is
    enforced instead by a test that scans the engine's sources (§2.5, §2.8).
```

### 2.1.1 Layer diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│ PRESENTATION   Flutter (Dart)   lib/                                 │
│   Setup flow · Weekly Mirror · Today so far · What I see ·           │
│   Saying shelf · Time Machine · gentle mode · the Lake drawing       │
│   Holds NO logic, NO database, NO event access. Renders DTOs.        │
└───────────────▲──────────────────────────────────────────────────────┘
                │ Pigeon-generated typed host API (pull-only, §2.4)
┌───────────────┴──────────────────────────────────────────────────────┐
│ HOST / INTERFACE ADAPTERS   Kotlin   android/app/.../host            │
│   HostApiImpl (Pigeon) · UsageEventsSource · NotificationCollector   │
│   (NotificationListenerService) · IngestWorker (WorkManager) ·       │
│   LakeWidget · WeeklyNoteNotifier · PermissionGateway ·              │
│   AppCatalogImpl · BatterySetupHelper                                │
└───────────────▲──────────────────────────────────────────────────────┘
                │ implements ports defined by the engine
┌───────────────┴──────────────────────────────────────────────────────┐
│ DATA / INFRASTRUCTURE   Kotlin   android/app/.../data               │
│   Room (SQLite) repositories implementing engine ports ·             │
│   JSON exporter · retention purger                                   │
└───────────────▲──────────────────────────────────────────────────────┘
                │ depends inward only
┌───────────────┴──────────────────────────────────────────────────────┐
│ APPLICATION + DOMAIN   pure Kotlin   android/app/.../engine          │
│   Use cases · entities · metric math · pattern layer · suggestions · │
│   judging · synthetic personas · ports.                              │
│   NO `import android.*` and no Flutter. Enforced by a test.          │
└──────────────────────────────────────────────────────────────────────┘

THE ONLY THINGS THAT WRITE MEASUREMENT:
  Android UsageStatsManager ─► UsageEventsSource ──┐
  Android NotificationListenerService ─► NotificationCollector ─┴─► raw_event / notif_event
```

### 2.1.2 The one idea that shapes everything: the engine is a pure function

```
Mirror = f( raw events , notification events , listener coverage , user settings , as-of clock )
```

- **No hidden state.** Everything the engine reads comes through a port; everything it writes goes out through a port. A `Clock` port supplies "now".
- **Consequence 1: testability without understanding.** Feed it a hand-written event stream and assert on windows, stays and returns with local unit tests (`./gradlew testDebugUnitTest`, no device). For vibe coding this is the agent's verifier: it writes, runs, sees red or green.
- **Consequence 2: lateness is harmless.** If the job runs an hour late or twice, the output is identical, because the engine recomputes a day from raw events.
- **Consequence 3: the demo is honest.** The Time Machine does not show mocked screens. Synthetic personas generate **raw events**, and those go through the same engine as a real phone's. Moving the slider changes only the `Clock`.

### 2.1.3 The chosen Technical Differentiator (from DOC 1 §1.2.6)

- **Where it lives:** the engine package, `patterns` beside `metrics`. Five detectors (rhythm, trend, shift, window shape, break point), one cross-day link, and the optional clustering step, all deterministic.
- **What it needs:** derived storage of windows, stays and stretches, a seeded random source, and an as-of clock.
- **Fallback (planned):** if clustering is not ready, everything else ships. Rules alone produce Held, Pinged and Reached windows. No other module depends on clustering.
- **`LITERATURE-BACKED: no`.** None of these detectors is an open research question; the thresholds are DOC 1 estimates to tune, so no research prompt is produced.

### 2.1.4 Process model on the phone

```
One app process, started three different ways:
  - The user opens the app   → Flutter UI + HostApiImpl + engine calls (short-lived)
  - Android binds the listener → NotificationCollector appends one row per notification
  - Android runs the job      → IngestWorker: ingest, recompute, refresh the Lake widget, maybe
                                post the weekly note. No Flutter engine is started.
  - Android broadcasts to the widget → LakeWidget redraws from the stored lake_state
There is no foreground service and no wake lock. WorkManager reschedules itself after a reboot.
```

---

## §2.2 — Technology Stack Decisions

**The new selection rule.** The reference's usual criteria apply (maturity, documentation, lock-in, operational cost). The deciding criterion is now **vibe-fit**: (1) how much of this has an AI agent seen in thousands of examples, (2) how clear the error messages are when it goes wrong, (3) how few configuration files it adds, and (4) whether a mistake becomes a *build error* (loud, fixable by the agent) rather than a *runtime surprise* (silent, found on a phone). Team familiarity is irrelevant: everyone is a vibe coder for every layer.

**Stack-wide vibe rules** (each guards a failure that vibe coding is known for):
- Start from the **default Flutter project template** and the **default Android project files it generates**. Do not restructure them.
- **One version catalog** for Android dependencies (`libs.versions.toml`, the template default). Versions are whatever is current when the project starts, then pinned. `[VERIFY: pin at project start; do not let an agent upgrade versions mid-build.]`
- **No exotic libraries.** Every dependency below is the most widely used option in its category.
- **No annotation-processor stacking.** One code generator on the Android side (KSP, for Room). No Hilt, no kapt.
- **No code generation on the Dart side except Pigeon.** No riverpod_generator, no freezed, no build_runner chains.

```
Decision: Where the logic runs
  Chosen: Kotlin, in a pure package `engine` inside the single Android module
  Reason: Measurement and the Lake must run with no UI alive (periodic job, widget
          refresh, weekly note). In Kotlin this is an ordinary Android job. The Android
          APIs we need are Kotlin-native. The engine is testable with local JVM unit
          tests, so an agent can verify its own work with no device.
  Vibe-fit: Very high. Kotlin plus Android jobs is among the most common things agents
          write; errors are compile errors with line numbers.
  Alternatives evaluated:
    - Dart engine (v2): rejected because it cannot run in the background without a second
      Flutter engine started by a plugin, which OEM killers break and which is very hard
      to debug by an agent.
    - Engine as its own Gradle module: stronger boundary, rejected for build-fragility
      inside a Flutter project; replaced by a scan test (§2.5).
  Trade-offs accepted: Nobody on the team can review the engine by reading. It is verified
    by behaviour (§2.8). `[KNOWN RISK: a wrong rule that tests do not cover will ship
    unnoticed. Mitigation: the golden fixtures, persona replay and the face-validity check
    in §2.8 are the net, so DOC 3 must specify each rule as a testable example.]`
  Future migration risk: Low. Plain Kotlin behind ports.
```

```
Decision: UI toolkit
  Chosen: Flutter (Dart), stable channel at build time
  Reason: Required by the brief; fast for the screens, the Lake drawing and the Time
          Machine slider.
  Vibe-fit: High. Flutter is among the best-known frameworks to agents; hot reload gives
          instant feedback.
  Alternatives: Jetpack Compose (single-language native; a strong alternative, rejected
          only because Flutter is a stated requirement); React Native (rejected, a heavier
          native bridge).
  Trade-offs accepted: Two languages and one typed bridge.
```

```
Decision: Flutter to Kotlin bridge
  Chosen: Pigeon (generated typed host API), pull-only, no streams
  Reason: One Dart definition generates both sides, so a field renamed on one side fails
          to compile on the other.
  Vibe-fit: Medium to high. Fewer examples than hand-written channels, but it turns the
          riskiest boundary into build errors an agent can read. The generator is one
          command.
  Fallback: if Pigeon codegen blocks the build (spike S-G), use a hand-written
          MethodChannel behind a typed Dart wrapper and a contract test. The API surface
          in §2.4 stays identical.
  Alternatives: Hand-written MethodChannel with maps (the most familiar to agents, but
          silent drift); EventChannel (nothing is live); FFI (too much tooling).
```

```
Decision: Local database
  Chosen: Room (SQLite) with KSP, in the single Android module, accessed only through
          engine-defined ports
  Reason: Boring, Android-native, compile-time-checked queries, works from every Android
          entry point (worker, collector, widget) with no Flutter involved.
  Vibe-fit: Very high. Room is among the most widely documented Android components; query
          and schema mistakes surface at build time.
  Alternatives: Drift/sqflite (Dart; rejected, the writers are Kotlin services and jobs);
          Isar/Hive/SharedPreferences (rejected, the data is relational and queried by
          time range); raw SQLite helpers (rejected, untyped).
  Trade-offs accepted: Schema migrations are our job. `[KNOWN RISK: the schema will change
          during the build. Destructive migration is allowed in development; the schema is
          frozen in DOC 3.]`
```

```
Decision: Background execution
  Chosen: WorkManager `CoroutineWorker`: one periodic job (15-minute minimum), plus an
          expedited one-time run when the app opens. No foreground service. No wake locks.
  Reason: Idempotent catch-up means the weakest guarantee a scheduler gives is enough.
          The job ingests, recomputes, writes the Lake state, refreshes the widget and
          decides whether the weekly note is due.
  Vibe-fit: Very high. WorkManager is the standard answer and agents write it reliably.
  Alternatives evaluated:
    - Foreground service with a persistent notification: rejected, it is an always-on
      interruption (against DOC 1) and the first thing OEMs kill.
    - AlarmManager exact alarms: rejected, special permission and no benefit.
    - Accessibility service for live tracking: rejected, a control-capable permission that
      violates "Sakshi cannot block".
  Trade-offs accepted: The system throttles jobs for rarely-opened apps (standby buckets)
    and OEMs may delay them. `[VERIFY ON DEVICE: real cadence on the Nothing Phone 3a and
    the iQOO Z7, spike S-D.]` This costs freshness, never correctness.
```

```
Decision: Home-screen widget (the Lake), required in the demo build
  Chosen: Native AppWidgetProvider with RemoteViews, XML layouts, and three prebuilt
          vector states (still, rippled, choppy) plus a phrase and an "as of" label.
          Redrawn by the worker and on app open. `updatePeriodMillis = 0`.
  Vibe-fit: Very high. RemoteViews widgets are heavily documented; Glance is newer and has
          fewer examples.
  Alternatives: Jetpack Glance (newer, fewer examples); the `home_widget` Flutter plugin
          (rejected, adds a plugin that must work with no Flutter engine alive);
          live wallpaper / dynamic icon / quick-settings tile (cut in DOC 1).
  Trade-offs accepted: The Lake is drawn twice, in Flutter and as native vector art.
```

```
Decision: Dependency injection
  Chosen: Manual constructor injection through one `AppContainer` object
  Vibe-fit: Very high. No annotation processing, no generated graph, no cryptic errors.
  Alternatives: Hilt (heavy, kapt-era errors are hard for agents to repair), Koin.
```

```
Decision: State management (Flutter)
  Chosen: flutter_riverpod, manual providers only (no code generation)
  Vibe-fit: High. Widely documented; the manual style avoids the generator-version mismatch
          that breaks agent-written projects.
  Alternatives: Bloc (more ceremony), setState (ad hoc across the Mirror), GetX (poor
          testability).
```

```
Decision: Serialization (export, demo fixtures)
  Chosen: kotlinx.serialization (JSON)
  Vibe-fit: High. Standard Kotlin plugin; compile-time errors.
  Alternatives: Moshi, Gson. Rejected, no benefit here.
```

```
Decision: Learning component (the only "ML")
  Chosen: Hand-written k-means (k at most 3, seeded, standardised features) in the engine
  Reason: DOC 1 §1.2.6 limits learning to one tiny unsupervised step. It is about a page of
          code, which an agent writes well and a test can check on known points.
  Alternatives: TFLite / ML Kit / ONNX. Rejected (no labels, no data, no one-sentence
          explanation; they add a runtime and model file to a small, auditable APK).
```

```
Decision: Testing
  Chosen: JUnit 4 local unit tests with plain assertions for the engine and data layer
          (the Android default, no extra setup); Flutter widget tests for presentational
          screens; a manual device checklist for platform behaviour
  Vibe-fit: Very high. Default setup, no extra plugins. JUnit 5 is rejected because it
          needs extra configuration that agents often get wrong on Android.
```

```
Decision: Monitoring and crash reporting
  Chosen: None over the network. A local "last error" counter shown in What I see, plus Logcat.
  Reason: No INTERNET permission by design.
```

```
Decision: Build, CI and distribution
  Chosen: Standard Flutter build; signed release APK shared as a file and sideloaded.
          Optional GitHub Actions job running the Android local unit tests.
  Reason: A Play Store release is out of scope (DOC 1 §1.7.3 limit 13). Sideloading has one
          consequence that shapes the demo: Android 13+ restricts notification access for
          APKs not installed from a store until the user chooses "Allow restricted settings"
          (DOC 1 §1.2.1). The setup flow must tell the user.
```

**Platform facts this stack relies on** (Android documentation and the verification pass for DOC 1; spikes in §2.10 settle each VERIFY):
- `PeriodicWorkRequest` has a 15-minute minimum interval.
- `UsageStatsManager.queryEvents()` retains events only "for a few days" and may omit the last few minutes.
- `NotificationListenerService.onNotificationRemoved(sbn, rankingMap, reason)` has a removal reason from API 26; `requestRebind(ComponentName)` asks the system to reconnect a disconnected listener.
- `ApplicationInfo.category` exists from API 26; package visibility filtering applies from Android 11, and a `<queries>` block with a launcher intent filter exposes launcher apps without the all-packages permission.
- `[VERIFY ON DEVICE: event semantics around split-screen, picture-in-picture and lock; true retention length; listener rebind behaviour; widget refresh from a worker.]`

---

## §2.3 — Data Architecture

### 2.3.1 Core entities

Two kinds. **Facts** are observed and immutable. **Derived** rows are recomputed by the engine and are disposable: delete them and the engine rebuilds them.

**Facts (written only by the collectors):**

```
Entity: raw_event            (from UsageStatsManager; one row per relevant event)
  Fields:   id (integer, autoincrement)
            ts (epoch millis, UTC)
            type (text enum, stored by symbolic name: ACTIVITY_RESUMED, ACTIVITY_PAUSED,
                  SCREEN_INTERACTIVE, SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN, KEYGUARD_HIDDEN)
            pkg (text, nullable; null for screen and keyguard events)
  Unique:   (ts, type, pkg), so re-reading an overlapping range cannot create duplicates
  Index:    ts (every query is a time range)
  Retention: 14 days rolling

Entity: notif_event          (from NotificationListenerService; never contains text)
  Fields:   id, ts, pkg, category (nullable), kind (POSTED | REMOVED),
            removal (CLICK | OTHER | null), ongoing (boolean; ongoing ones are ignored by the engine)
  NEVER STORED: title, text, extras, icons, intents, people, channel names
  Index:    (pkg, ts)
  Retention: 14 days rolling

Entity: listener_session     (when notification awareness was actually alive)
  Fields:   id, connected_at, disconnected_at (nullable while connected)
  Purpose:  the engine labels a stay Stone or SelfStarted only if its 30-second look-back
            lies inside a connected interval. Otherwise the stay is Unknown.
            (An un-heard ping is not a reach. This is a correctness rule.)

Entity: ingest_state         (one row)
  Fields:   last_event_ts (cursor), last_run_at, paused, paused_since, first_read_at,
            last_worker_run_at, worker_runs_7d (for the health line in What I see)
```

**User configuration (written only through use cases):**

```
Entity: app_meta
  Fields:   pkg (PK), label, system_category (nullable), user_class (IN_SET | DEPENDS | NONE), added_at
  Rule:     at most 12 rows with IN_SET or DEPENDS (DOC 1 cap), enforced in the use case, not only the UI

Entity: settings             (one row)
  Fields:   study_hours (list of start/end minute pairs), learn_study_hours, gentle_mode,
            weekly_note_enabled, age_under_18 (boolean), baseline_anchor_id,
            battery_helper_shown (boolean), created_at
```

**Derived (written only by the engine):**

```
day_summary   day (study day), valid (window minutes ≥ 45), window_minutes, quiet_minutes,
              in_set_minutes, coverage, pickups, switches_per_hour, flinch, ramp_up_minutes,
              last_screen_off_ts, first_stretch_minutes
window        id, day, start_ts, end_ts, source (STUDY_HOURS | INFERRED | BOTH),
              shape (HELD | PINGED | REACHED | null), finalised
stretch       id, window_id, start_ts, end_ts, minutes, in_set_minutes, quiet_minutes,
              ended_by (STAY | PUT_DOWN | WINDOW_END)
stay          id, window_id, start_ts, end_ts, pkg_main, origin (STONE | SELF_STARTED | UNKNOWN),
              stone_pkg, notif_clicked, return_minutes, glances_before
week_summary  week_start, stretch_median_min, stays_per_hour, return_median_min, quiet_share,
              steadiness (null until baseline), word, windows_count, unusual,
              app_opens, app_minutes  (Sakshi's own use, derived passively from the usage
              events of Sakshi's own package, so no extra instrumentation exists)
baseline      id, frozen_at, c0, p0, r0, q0, days_used, is_active   (never overwritten; one re-anchor creates a new row)
pattern       id, kind (RHYTHM | TREND | SHIFT | SHAPE | BREAK_POINT | CROSS_DAY), key, strength,
              evidence_windows, evidence_days, first_seen, last_seen,
              sentence_args (JSON data; the engine emits data, the presenter words it)
suggestion_state  id, kind (S1..S12), first_eligible_at, shown_in_week, dismissed_until, status
experiment    id, suggestion_kind, started_at, start_reason (TAP | FOOTPRINT), target_metric,
              before_value, after_value, window_end, verdict (MOVED | NO_CHANGE | TOO_LITTLE | UNCLEAR | PENDING)
saying_pick   id, saying_id, picked_at
goal_tap      week_start, answer (YES | PARTLY | NOT_YET)
lake_state    (one row) state (STILL | RIPPLED | CHOPPY), phrase, as_of   (what the widget shows)
note_state    (one row) last_note_week, mirror_ready_week
```

The **Saying shelf** (DOC 1 §1.6.2) is a bundled read-only asset with a stable id, the exact excerpt, its Q-number, source and tier. It is never in the database. An asset-integrity test (§2.8) re-checks each excerpt against the Outcome Map text.

### 2.3.2 Data flow for the primary use case (a real phone, first install)

```
1. User grants Usage access (an Android settings page, not an in-app dialog).
2. PermissionGateway reports "granted" → HostApi.syncNow().
3. IngestWorker (expedited) runs IngestEvents:
     a. read ingest_state; no cursor → ask UsageEventsSource for everything Android retains
        (a wide lower bound; Android returns what it has)
     b. append raw_event rows (the unique constraint swallows duplicates)
     c. set cursor = latest event ts minus an overlap margin
4. Engine use cases run over the days now present:
     FinalizeWindows → RecomputeDay (per day) → stay classification (stone / self / unknown)
       → DetectPatterns (whatever has evidence) → write derived rows → write lake_state.
5. First Look: Flutter calls getTodaySoFar() / getMirror() → BuildMirror reads derived rows
   → provisional parts, no Steadiness number, no suggestion (baseline not yet frozen).
6. Every later run (job or app open) repeats 3–4 from the cursor, then asks LakeWidget to
   redraw. After 8 valid days UpdateBaseline freezes the starting normal and the next
   Mirror shows Steadiness.
7. Weekly: BuildWeekSummary → SelectSuggestion (silence rules) → JudgeExperiment → Mirror.
   If weekly_note_enabled and the Mirror for the week is ready and no note was sent for it,
   WeeklyNoteNotifier posts the content-free note ("Your Mirror is ready").
```

### 2.3.3 Time and day boundaries (a source of real bugs)

- **Epoch milliseconds for storage; the device's local zone for humans.**
- **The study day starts at 04:00 local** (confirmed). A midnight session belongs to the day it started. A named constant in `Tuning`.
- **A window that crosses the boundary** is attributed to the day it began. Weeks start Monday 04:00 local. Weekday and weekend patterns (DOC 1 P1) use the study day.
- **A finalised window** is one whose last event is at least 15 minutes old `[ESTIMATE: the last few minutes of the event log may be missing, so a recent window could be wrongly short; the engine may revise it on the next run until then]`.

### 2.3.4 Retention and deletion

- **Raw and notification events: 14 days rolling** (decided by me, as you left it to me): 14 days lets the engine re-derive both comparison windows of the two-week judging loop after a threshold is tuned; Android keeps only a few days anyway; and package names with exact times are the most revealing data the app holds.
- **Derived rows and the baseline: kept until the user deletes everything.**
- **Pause** stops collection and records a gap. It deletes nothing.
- **Delete everything** wipes all tables, the cursor and the settings. Permissions stay (Android's to revoke). The next run is a fresh install.
- **Export** writes derived rows plus settings to JSON through the share sheet. Raw events are exported only if the user separately chooses it. `[ASSUMPTION: raw events are the most revealing data and a user rarely needs them.]`

### 2.3.5 Sensitive data and how each is protected

| Data | Why sensitive | Protection |
|---|---|---|
| Package names with exact times | Reveal a student's life | App-private storage; backup disabled; no INTERNET; 14-day raw retention; never in release logs |
| Notification app plus category plus time | Reveals who contacts them and when | Content never read; category only; same storage rules |
| Study hours, gentle mode, age flag | Behavioural profile; the under-18 flag is sensitive | Device only; the flag is a boolean used only for the gentle-mode default |
| The Mirror and Steadiness | Could shame | Never leaves the phone; no share card |
| Exported file | Leaves the app on user action | Explicit action; derived rows by default; user chooses where it goes |

---

## §2.4 — API Design

There is no network API. The "API" is the typed boundary between Flutter and the host, plus the engine's ports. Both are contracts DOC 3 will lock.

```
API STYLE: Pigeon-generated host API (Dart → Kotlin), request/response, pull-only.
RATIONALE: Every screen is something the user opens on purpose, so no streams or push
           events. A request/response API is the smallest surface that works, the easiest
           to stub (§2.9), and a generated, typed contract is the best protection against
           AI drift on a boundary nobody reviews.
```

### 2.4.1 Core host endpoints (Flutter calls Kotlin)

```
setup
  getSetupState()                 → SetupStateDto   (usage access, notification access,
                                                     restricted-settings suspected, work-set exists,
                                                     collection health)
  openUsageAccessSettings() / openNotificationAccessSettings() → void
      Opens the Android system page. Sakshi never fakes a grant.
  openAppInfoForRestrictedSettings() → void   (sideload case: "Allow restricted settings")
  openBatterySettings()           → void   (optional guided setup; §2.7.4; opens Android's own page)
  listLauncherApps()              → List<AppDto>   (label, pkg, suggestedInSet, suggestedDepends)
  saveWorkSet(List<WorkSetEntryDto>) → SaveResultDto   (rejects more than 12)
  saveStudyHours(StudyHoursDto)   → void
  setGentleMode(bool) / setUnder18(bool) / setWeeklyNote(bool) → void

read (all pull)
  syncNow()                       → SyncStatusDto   ("ok", "partial ping awareness", "gap not seen", "paused")
  getMirror(weekStart?)           → MirrorDto
  getTodaySoFar()                 → TodayDto        (completed windows only; no suggestion)
  getWhatISee()                   → WhatISeeDto     (permissions held, what is stored, counts, last error,
                                                     listener coverage, job health)
  getSayingChoices()              → List<SayingDto> (three, from the bundled shelf)
  getLake()                       → LakeDto         (still | rippled | choppy plus one phrase; same source as the widget)

write (rare, user-initiated, each a single tap)
  pickSaying(sayingId) · tapTryThis(suggestionKind) · tapGoal(answer) · reanchorBaseline()
  pause(bool) · exportData(includeRaw) → ExportDto (a file URI for the share sheet) · deleteEverything()

demo
  startDemo(personaId)            → void   (switches to a separate demo database)
  setDemoAsOf(dayIndex)           → void   (moves the injected clock)
  stopDemo()                      → void
  Key rule: every DTO carries isDemo; the UI must show "Demo data" whenever it is true.
```

**What `MirrorDto` carries (the engine emits data, the presenter words it).** The week's four parts and the in-set versus quiet split; Steadiness and word (absent in gentle mode and before baseline); stone and self-started counts; top stone; clear hours; patterns that exist, each with its evidence count; one suggestion or "nothing to fix"; the experiment verdict if due; goal-tap state; Sakshi's own opens and minutes with their weekly trend; data-state flags (`partial`, `notSeen`, `tooLittleData`). Sentence copy (DOC 1 §1.2.2) is built from structured fields in one place, so the forbidden-sentence rules can be tested.

### 2.4.2 Error philosophy (the API never lies by omission)

- "Not granted", "partial" and "not seen" are **values**, not exceptions; the UI renders them.
- A failure to compute is `tooLittleData`, never a zero.
- Anything unexpected in the host returns a typed `HostError` with a developer message (logged locally, no package names) and a plain user message from a fixed set.

### 2.4.3 Internal ports (the engine's own contracts)

```
Defined in the engine package, implemented in the data layer (and by in-memory fakes in tests):
  EventStore        append raw events; read a time range; purge before a time
  NotifStore        append notification events; read a range
  ListenerCoverage  intervals during which notification awareness was alive
  DerivedStore      read/write day_summary, window, stretch, stay, week_summary, pattern
  StateStore        baseline, suggestion_state, experiment, saying_pick, goal_tap, settings,
                    lake_state, note_state
  AppCatalog        classify(pkg), system_category(pkg), launcher_apps()
  Clock             now()
  Randomness        seeded random source (for the clustering step)
```

The engine's **use cases** are the other contract: `IngestEvents`, `FinalizeWindows`, `RecomputeDay`, `UpdateBaseline`, `BuildWeekSummary`, `DetectPatterns`, `SelectSuggestion`, `JudgeExperiment`, `BuildMirror`, `BuildTodaySoFar`, `BuildLake`, `DecideWeeklyNote`, `BuildWhatISee`, `ExportData`, `DeleteEverything`. One use case is one user-visible action or one scheduled step.

---

## §2.5 — Clean Architecture & Engineering Principles

```
MODULARITY & COHESION:
  Three artifacts with one reason to change each:
    engine package   changes when the PRODUCT's definition of focus changes
    host + data      changes when ANDROID or the schema changes
    lib/ (Flutter)   changes when the DESIGN changes
  Inside the engine, packages are cohesive by concept and say what the system does:
    classification · windows · stays · metrics · baseline · patterns · suggestions ·
    judging · mirror · demo · ports
  Modules talk only through the ports in §2.4.3 and the DTOs in §2.4.1.

SINGLE RESPONSIBILITY PRINCIPLE:
  One use case = one action. RecomputeDay turns events into derived rows and nothing else;
  SelectSuggestion chooses and never computes a metric. Tempting violation: a convenient
  "just calculate it in the presenter" for a sentence. Guard: the presenter receives
  fields, not formulas.

SEPARATION OF CONCERNS:
  Business logic lives only in the engine. UI code never imports a database class and
  never holds a threshold. The collectors only append facts; they never classify, judge or
  decide a stay. Classification happens later in the engine, so changing "30 seconds"
  re-derives history instead of rewriting collection code.

DEPENDENCY INVERSION:
  Dependencies point inward only: Flutter → Pigeon API → host → data → engine. The engine
  declares the ports it needs; the data layer implements them. Enforced by an automatic
  test (below), not by good intentions.

OPEN/CLOSED PRINCIPLE:
  New suggestions and pattern detectors are added by implementing an interface and
  registering it; nothing existing is edited:
    interface PatternDetector { fun detect(ctx): List<Pattern> }
    interface SuggestionRule  { fun evaluate(ctx): Candidate? }
  The twelve suggestions are twelve small rules sharing one selection and silence-rules
  pipeline, so "max one a week, never mid-window, one live experiment" lives in one place
  and cannot be forgotten by a thirteenth rule.

LISKOV / INTERFACE SEGREGATION:
  Fakes and real adapters honour the same port contract, so FakeEventStore in tests and
  RoomEventStore in production behave identically. Ports are narrow.

INFORMATION HIDING (Ousterhout):
  Deep modules, simple interfaces. The Android foreground-interval reconstruction (resume
  and pause pairs, screen-off truncation, lock) is hidden behind ONE function:
      events → List<ForegroundInterval>
  Nothing above it knows what an ACTIVITY_PAUSED is. The same for the stone/wave look-back
  and the baseline freeze rule. If OEMs differ, only that module changes.

TESTABILITY:
  The engine is deterministic: same events, settings, clock and seed give the same rows.
  Hand-built event streams assert on windows, stays and returns. Persona replay asserts on
  the Steadiness band. No mocking framework is needed.

NAMING & READABILITY (Clean Code):
  The DOC 1 vocabulary is the ubiquitous language (DDD): Window, Stretch, Glance, Stay,
  Return, Quiet, Stone, Wave, WorkSet, StartingNormal, Mirror. The same words in code, UI
  copy and docs. No synonyms: not "session", not "distraction", not "streak".
  Magic numbers (30 s, 20 s, 5 min, 0.35, 1.5×, 14 days, 04:00) live in one `Tuning`
  object with a comment tying each to its DOC 1 estimate tag, so tuning is one file.
  Primitive obsession is avoided: Package, StudyDay, Minutes and Ratio are value types.
  Comments explain why (the DOC 1 reference), not what.

DOMAIN MODELLING (DDD, where it applies):
  One bounded context: "attention evidence". Value objects: Window, Stretch, Stay, Ratio.
  Day is the one aggregate. Ports are by access pattern (time-range reads), not one
  repository per table.

12-FACTOR COMPLIANCE (adapted; there is no server):
  Config:        build flavours and one Tuning object; no secrets exist.
  Backing svc:   the SQLite file is an attached resource behind ports.
  Processes:     disposable; state lives in the database. Kill the app at any moment and
                 the next run catches up.
  Disposability: the job is idempotent; collectors append and exit.
  Logs:          Logcat with levels; release logs never contain package names.
  Admin tasks:   Delete and Export are ordinary use cases, not scripts.
  Build/run:     separate debug and release manifests (release has no INTERNET).
  N/A:           port binding, concurrency by process model.
```

**Rules that exist because the whole team vibe-codes** (each guards a named drift; DOC 4 and AGENTS.md will turn them into triggers):
- **The Dependency Rule is a test.** A local unit test scans every source file under the engine package and fails if any contains an `import android.` or an import from the host or data packages. An agent that "just needs a Context" cannot slip it past the build.
- **Contracts are code, not prose.** The Pigeon file and the engine's `ports` and `entities` are the contracts. Changing one follows DOC 3's contract-change process.
- **Tests are the agent's verifier.** Every engine use case ships with a test before it counts as done. "It compiled" is not evidence.
- **No logic in the collectors, the widget or the host.** If an agent adds an `if` that decides anything about attention outside the engine, it is in the wrong file.
- **One `Tuning` object.** No number from DOC 1 appears twice in code.
- **Do not let an agent upgrade versions, restructure the template, or "fix" the build by adding libraries.** Build errors are fixed in place.

---

## §2.6 — Privacy & Permissions Architecture

### 2.6.1 The manifest is the promise

```
DECLARED:
  PACKAGE_USAGE_STATS       special access, granted by the user in Settings > Usage access
  The notification listener service declaration (user grants access in Settings)
  <queries> with a MAIN/LAUNCHER intent filter   (see 2.6.3)
  POST_NOTIFICATIONS        requested only if the user turns the optional weekly note on
                            (off by default)

NOT DECLARED (each absence is a feature):
  INTERNET                  release build. Flutter's debug/profile builds add it for tooling;
                            the release manifest does not, so the shipped APK cannot send data.
                            Judge-checkable by inspecting the APK's permissions.
  ACCESSIBILITY service     would allow reading screens and blocking input
  SYSTEM_ALERT_WINDOW       would allow overlays and blockers
  Device admin / lock-task  would allow locking the device
  FOREGROUND_SERVICE        no persistent process
  REQUEST_IGNORE_BATTERY_OPTIMIZATIONS   not declared; the optional battery helper only
                            opens Android's own settings page, it never requests the exemption
                            itself (§2.7.4)
  QUERY_ALL_PACKAGES        replaced by <queries>
  Contacts, location, camera, microphone, SMS: never

APPLICATION FLAGS:
  allowBackup = false and data-extraction rules excluding everything, so usage data does
  not enter cloud or device-transfer backups.
```

`[KNOWN RISK: judges may ask "can't it still scrape notification text?" The listener API can technically read it. The architectural answer is the code boundary: the NotificationCollector reads only package, post time, category, ongoing flag and removal reason, enforced by a test over a fake notification carrying text that asserts no text reaches storage (§2.8).]`

### 2.6.2 The permission flow

```
1. Usage access:       open the system page → user toggles → on return, re-check with the
                       AppOps check (never trust a returned boolean from the page)
2. Notification access (optional): open the system page → user toggles
     Sideload case: if the toggle is greyed ("this setting is currently unavailable"),
     Sakshi detects "not granted after the user returned" and shows the three-step path:
     App info → ⋮ menu → Allow restricted settings → try again. It never loops.
3. Battery helper (optional, skippable, shown once): §2.7.4
4. POST_NOTIFICATIONS: only when the user chooses the weekly note.
Every state is a value in SetupStateDto, so the UI can always say exactly what Sakshi can and
cannot see. Nothing is silently skipped.
```

### 2.6.3 Package visibility and the neutral list

- A `<queries>` block with the MAIN/LAUNCHER intent filter lets Sakshi list the apps a user can open, without the broad all-packages permission. `[VERIFY ON DEVICE: apps appearing in usage events are visible through this block (spike S-E); an app seen in events but not resolvable falls back to its package name as its label.]`
- **Pre-ticking the work-set:** apps whose `ApplicationInfo.category` is productivity (or education-adjacent where Android reports it) are pre-ticked. The user changes it with taps. A convenience, not a classifier.
- **Neutral list (DOC 1 default):** dialer, maps, camera, calculator, clock, system UI, as a bundled list of package names plus Android's role lookups for the default dialer and launcher. `[ASSUMPTION: OEM variants differ; the list is a start and extendable in one file.]`

### 2.6.4 What the user can inspect

The What I see page (DOC 1 F10) is built from the database: the permissions held, row counts per table, the oldest raw event date, listener coverage ("heard pings for 61% of the last 7 days"), job health ("last background run 38 minutes ago; 61 runs in the last 7 days"), and the buttons Pause, Export and Delete. It is generated from what is actually stored, so it cannot drift from reality. The job-health line is also the user-visible evidence for the OEM-killer question.

---

## §2.7 — Background Execution & Failure Behaviour

**The design principle: the part that can be killed must be the part that matters least, and every run must be safe to repeat.**

### 2.7.1 What runs when

| Trigger | What runs | Notes |
|---|---|---|
| App comes to the foreground | `syncNow()`: catch-up ingestion, recompute affected days, rebuild the Mirror, refresh the widget | Always correct, because it recomputes from the raw log |
| Periodic job (15-minute minimum, best effort) | The same catch-up, then Lake state, widget redraw, and the weekly-note decision | Android may run it late or rarely; the design allows it |
| Notification posted or removed | `NotificationCollector` appends one row | The only thing that cannot be recovered if missed |
| Listener connects or disconnects | Open or close a `listener_session` row | Gives the engine the coverage map |
| Weekly (optional) | A content-free "Your Mirror is ready" notification | Off by default; needs POST_NOTIFICATIONS only if turned on |

### 2.7.2 Failure behaviour (what the user sees, what is lost)

| Failure | Effect on data | What Sakshi says |
|---|---|---|
| Periodic job delayed for hours | None; catch-up reads the retained log | Widget shows its "as of" label |
| App not opened and job killed for longer than Android retains events | Events in the gap are gone | "Not seen" for those days; no zeros; excluded from the baseline and from trends |
| Listener killed by an OEM | Notifications in the gap are lost | "Ping awareness partial"; stays in the gap are `Unknown`; stone-or-reach and no-ripple degrade to partial; on open Sakshi asks the system to reconnect the listener |
| Phone rebooted | None | WorkManager reschedules itself; the listener reconnects when the system binds it |
| Time zone or clock changed | Events keep UTC times; day assignment may shift | Recompute fixes it; windows that day are flagged unusual |
| Process killed mid-ingest | Partial append | The unique constraint plus the overlap margin make the retry safe |
| User pauses collection | A pause interval | "Paused"; the gap is excluded, never counted as quiet |
| Notification access never granted | No stone/wave | "Ping awareness off"; everything else works |
| Android version quirk in event pairs | Mis-derived intervals | One module (foreground-interval reconstruction); fixture-tested; spike S-A |

### 2.7.3 What is explicitly not done

No foreground service. No wake locks. No restart loops. No exact alarms. Sakshi never requests the battery exemption on the user's behalf (the optional helper below only opens the system's own page). The product's stance (DOC 1 F14: the teacher leaves) and its survivability point the same way: the less it asks of the system, the less the system kills.

### 2.7.4 The optional battery helper (new: because you want the background work to survive on phones like the iQOO Z7)

DOC 1 deliberately did not fight the OS. You asked for the background functionality to actually work, and some phones (Vivo family, Xiaomi, Oppo, Realme) will stop a periodic job and a listener unless the user changes a setting. So:

- **What it is.** One optional, skippable screen, shown **once** after setup (never again, never a nag), that says in plain words: "To keep Sakshi's reading going while the app is closed, allow it to run in the background."
- **What it does.** One button that opens **Android's own battery-optimisation settings page** (`openBatterySettings()`), plus short static text for common phone makers ("On Vivo/iQOO: allow background activity and auto-start for Sakshi; set battery to unrestricted"). The text is fixed copy, not detection.
- **What it does not do.** It does not request the exemption through the system dialog, hold a permission for it, change any setting itself, or reappear. The decision stays the user's. `[ASSUMPTION: the exact menu names differ by OEM and Android version; the static text is a starting point to correct against the iQOO Z7 and the Nothing Phone 3a.]`
- **How we know it works.** The What I see page shows job health. The iQOO Z7 is the test: compare the job run counts and listener coverage with the helper skipped versus applied (spike S-D, S-C).
- **Deviation from DOC 1, recorded.** DOC 1's stance was "do not fight the OS". This helper is not a fight: it hands the choice to the user once. It stays optional, and DOC 1 §1.7 gets an update later if you sign off.

---

## §2.8 — Testing & Verification Architecture

Since nobody reads the code, **tests and device checks are the product's proof of correctness.** They are specified in DOC 3 as concrete examples, so each rule can be pasted into an agent as "make these pass".

### 2.8.1 Pyramid

| Level | What | Where | Examples |
|---|---|---|---|
| **Unit (many, fast)** | Pure functions and rules | engine, local JUnit 4 | foreground-interval reconstruction; glance versus stay at 29 s, 30 s, 31 s; merge of off-set runs 19 s and 21 s apart; return when the screen goes off for 5 minutes; Steadiness arithmetic against the DOC 1 worked example (116 and 75); clamping at 0.5 and 1.5; add-one smoothing; baseline freeze after 8 valid days; re-anchor once only |
| **Golden fixtures** | Hand-written event streams with expected derived rows | engine | a clean evening; a ping-broken evening; a midnight window crossing the 04:00 boundary; split-screen and picture-in-picture; a day with a listener gap (stays must be `Unknown`) |
| **Property / invariant** | Rules that must hold for any input | engine | recomputing a day twice gives identical rows; duplicating or reordering raw reads never changes output; no suggestion with fewer than 8 valid days; at most one non-exempt suggestion a week; never two live experiments |
| **Persona replay** | The Time Machine personas through the real engine | engine | Aarav's synthetic weeks produce Steadiness near 100 at baseline and roughly the DOC 1 §1.4.3 figures at weeks 4 and 8 (within tolerance); a known-bad hour comes out Wavering |
| **Copy tests** | The forbidden-sentence rules | engine (sentence builder) | no output contains "focused", "distracted", "wasted"; every pattern sentence carries its evidence count |
| **Asset integrity** | The Saying shelf | build step | every excerpt is an exact substring of its Outcome Map Q-text; no tier-D quote is labelled his own writing; Q077 and Q190 absent |
| **Architecture test** | The Dependency Rule | local JUnit | fails if any engine source imports `android.` or the host or data packages |
| **Data layer** | Repositories against ports | local JUnit with in-memory Room | same behaviour as the engine's fakes; unique-constraint dedupe; retention purge |
| **Privacy test** | The collector boundary | local JUnit with fakes | a notification carrying title and body text produces a stored row with neither; a manifest check confirms no INTERNET permission in release |
| **Contract test** | Pigeon DTOs | Dart and Kotlin | a fake host (§2.9) and the real host return DTOs of the same shape |
| **Presentation (few)** | Screens render each data state | Flutter widget tests | baseline-learning, steady, gentle mode, partial ping awareness, "nothing to fix this week", demo label present |
| **Device checklist (manual)** | What no unit test can know | the Nothing Phone 3a and the iQOO Z7 | §2.10 spikes |

### 2.8.2 Deliberately not tested

Third-party behaviour (WorkManager's scheduler, Room). The exact wording of every suggestion line (it is copy; only the forbidden-sentence rules are tested). Pixel layout of the Lake.

### 2.8.3 The face-validity test from DOC 1 §1.2.5

Each teammate uses the real app for two ordinary days and says whether the day it called steadier was the day they felt steadier. No test replaces it. The architecture's job is to make `Tuning` a single file, so a failed check is fixed by changing numbers, not code.

---

## §2.9 — Track Seams (Input to DOC 3 and DOC 4)

This section does **not** divide the work. It names where the architecture can be cut so DOC 4 can assign tracks cleanly. Team setup: four builders, four laptops, tracks merged at the end; **you** (Claude Pro) the major backend track and Integration Owner, one more Claude Pro teammate on a second major track, two free-plan teammates on smaller tracks. Total tracks: **four**. No research track and no filler track; research prompts live inside the track they serve. **All four are vibe coders in every language**, so a track is defined by how easily its output can be verified by behaviour, not by who "knows" the language.

### 2.9.1 The seams

| Seam | Boundary | Why it is a clean cut |
|---|---|---|
| **S-1: Engine ports and entities** | the engine's `ports` and `entities` | Platform work and engine work meet only here. The engine consumes values; the platform produces them. |
| **S-2: Pigeon host API and DTOs** | the Pigeon definition | The UI and everything beneath it meet only here. A fake host lets the UI be built with no Kotlin and no phone. |
| **S-3: Event vocabulary** | `RawEvent`, `NotifEvent`, `ListenerCoverage` | The one data shape that collectors and the engine must agree on exactly. |
| **S-4: Saying shelf and demo personas** | bundled assets | Content, not code; validated by script. |

### 2.9.2 Proposed split by seam (a proposal for DOC 4, with the reasoning)

| Track | Owner and plan | Work it covers | Why it fits |
|---|---|---|---|
| **Track 1: Platform backend (primary)** | You, Claude Pro | The Pigeon definition and host implementation; UsageEventsSource, NotificationCollector, listener coverage; Room schema and repositories implementing the engine ports; IngestWorker; PermissionGateway; AppCatalog; LakeWidget; weekly note; battery helper; retention, export, delete, pause; manifest hardening; the device spikes on the Nothing 3a and the iQOO Z7 | The most Android-specific and failure-prone work, and the one that touches the phones. Many steps, mostly sequential. |
| **Track 2: Analytics engine** | Teammate, Claude Pro | The engine: foreground-interval reconstruction, classification, windows, stays, metrics, baseline and Steadiness, patterns, suggestions and silence rules, judging, the synthetic personas and replay, `Tuning`, the sentence builder | Pure Kotlin, verified by local tests with no phone; long dependency chain. Many steps. |
| **Track 3: App shell and trust screens** | Teammate, free Claude | Flutter: the fake host, setup and permission flow (including restricted-settings help and the battery helper screen), What I see, Pause/Export/Delete screens, settings, gentle mode | Screens against a fake; small units a free plan can finish one at a time. |
| **Track 4: Mirror, Lake and demo screens** | Teammate, free Claude | Flutter: Weekly Mirror, Today so far, the Lake drawing, Saying shelf pick, the Time Machine slider and Demo label; widget art assets | Pure presentation of DTOs; each screen is independent and testable with fixtures. |

**What the architecture implies about merging:** S-1, S-2 and S-3 are frozen first (DOC 3). Then Tracks 2, 3 and 4 run against fakes while Track 1 builds the real adapters; the join is "swap the fake host and the in-memory fakes for the real ones". DOC 3 locks the contracts and DOC 4 sets steps, sync points and merge points.

`[ASSUMPTION: Integration Owner is you (confirmed).]`
`[KNOWN RISK: Tracks 3 and 4 both write Flutter and will touch shared files (theme, routing, shared DTO imports). DOC 3 should name those shared surfaces and DOC 4 should give one track ownership of each.]`
`[KNOWN RISK: the engine's quality depends on thresholds tuned on real data. Track 2 needs real exported events from Track 1's phones early, which is why the raw-event export in §2.3.4 matters even though users rarely need it.]`
`[KNOWN RISK: Tracks 1 and 2 are both Kotlin and neither owner can review it by reading. The mitigations are structural: the Dependency Rule test, golden fixtures, the contract tests and the device spikes.]`

---

## §2.10 — Device Spikes, Risks & Open Questions

### 2.10.1 Device spikes (small, specific verifications; each settles one `[VERIFY ON DEVICE]` tag)

| # | Question | What to do | Decision it settles |
|---|---|---|---|
| **S-A** | How do resume and pause events actually look around app switches, split-screen, picture-in-picture, lock and unlock? | Run a scripted sequence of about 5 minutes on both phones; dump the raw events; compare with the foreground-interval fixtures | The interval algorithm and its fixtures |
| **S-B** | How many days does `queryEvents` actually return, and how many events a day? | Query a wide range; read the oldest returned timestamp and the count | First Look depth; the "not seen" gap threshold; the 10,000-events assumption |
| **S-C** | Does the notification listener reconnect after a kill, and how often does the OEM stop it? | Grant access, force-stop, wait, post a notification, check `listener_session`. **Repeat on the iQOO Z7 with and without the battery helper.** | The wording of "ping awareness partial" and the on-open rebind; whether the helper is worth keeping |
| **S-D** | How often does the periodic job really run when the app is not opened for hours? | Log run timestamps over an idle stretch on both phones; **on the iQOO Z7 compare helper applied versus skipped** | How stale the Lake can get before the label appears; the helper's real value |
| **S-E** | Does `<queries>` list the apps seen in usage events? | Compare the launcher list with packages in a day of events | Whether the package-name fallback is needed |
| **S-F** | Can the worker redraw the widget reliably, and does it survive a launcher change? | Update and observe on both phones | The Lake in the demo (required, so this is a must-pass) |
| **S-G** | Does Flutter's generated Android project build with Pigeon, Room via KSP, WorkManager and the engine package, and do the local unit tests run? | Create the skeleton; one Pigeon round trip; one engine test; one Room test | The whole project layout. If Pigeon blocks, the MethodChannel fallback applies. |
| **S-H** | Is the sideload flow exactly "greyed toggle → App info → Allow restricted settings" on both phones? | Install the release APK and follow the path | The restricted-settings help text |

### 2.10.2 Risks

| Risk | Likelihood | Effect | Mitigation | Owner |
|---|---|---|---|---|
| Event semantics differ by OEM | Medium | Wrong stays | One module; fixtures; S-A on both phones | Track 2 with Track 1 |
| Listener or job killed on the iQOO Z7 | **High** (Vivo-family skin) | Stone/wave partial; stale Lake | Honest states; battery helper; S-C and S-D; the Nothing Phone 3a as demo phone | Track 1 |
| A rule is wrong and tests do not cover it | Medium | The metric looks arbitrary | Golden fixtures; persona replay; face-validity check; single `Tuning` | Track 2 |
| Gradle or Pigeon build fails and an agent loops | Medium | Blocks everyone | S-G first; the MethodChannel fallback; the "fix in place, no new libraries" rule | Track 1 |
| Agent adds logic outside the engine | Medium | Unverifiable behaviour | The Dependency Rule test; the "no logic in collectors, widget or host" rule | Integration Owner |
| Emulator cannot reproduce real usage history | Certain | Cannot validate First Look | Use the physical phones | Track 1 |
| Two Flutter tracks collide | Medium | Merge pain | Name shared surfaces in DOC 3; one owner each in DOC 4 | Integration Owner |
| Judges ask "where is the ML?" | Medium | Pitch gap | Prepared answer (DOC 1 v7.1 hand-off) | Pitch owner |

### 2.10.3 The phones (decided)

Your teammates have a Nothing Phone 3a, an iQOO Z7 and others. Both named phones are the right pair, for opposite reasons:
- **Nothing Phone 3a: the demo phone.** Nothing OS is close to stock Android, so it is the least likely to kill the listener or the job. `[ASSUMPTION: confirm with S-C, S-D and S-F before locking it as the demo phone.]` The restricted-settings sideload step still applies on it if it runs Android 13 or later.
- **iQOO Z7: the hostile-OEM stress phone.** iQOO is Vivo's sub-brand and runs a Vivo-style skin, one of the aggressive background managers DOC 1 named. `[VERIFY: confirm the skin and Android version on the phone.]` If Sakshi's background work holds up there, with or without the battery helper, that is a real, honest claim for the pitch ("tested on a Vivo-family phone"). If it does not, we know exactly how bad it is before a judge does.
- **The other teammates' phones:** if any is a Xiaomi, Oppo, Vivo or Realme skin, treat it as an extra stress phone for S-C and S-D.
- **The Time Machine works on any phone**, so the demo never depends on one phone's history.

### 2.10.4 Open questions for you

1. **What are the other teammates' phone models?** Only to flag any extra stress phones; nothing blocks on it.
2. **Is a one-time optional battery helper acceptable?** It is a recorded deviation from DOC 1's "do not fight the OS". I included it because you want the background work to survive; if you want it out, S-D will show whether it was needed.
3. **Weekly note in the demo build?** It is built, off by default. It needs POST_NOTIFICATIONS only if turned on. Tell me if you want it demoed.

---

**⛔ GATE:** Does the architecture align with your vision? Any layer, technology choice or principle to revisit? Once you confirm DOC 2 v3, I will move to DOC 3 (module and coding architecture), where the contracts in §2.9 get written down as concrete examples an agent can be told to make pass.
