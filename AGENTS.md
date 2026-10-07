# AGENTS.md — Sakshi (साक्षी)

Android-only passive attention app (Flutter UI, Kotlin host, pure-Kotlin engine). Four people build four tracks on four laptops; one Integration Owner merges. Read this whole file at the start of every session. If your tool reads `CLAUDE.md`, that file contains one line: `@AGENTS.md`.

## 1. What this is

Sakshi reads what Android already records (app foreground events, screen on/off, and the **package, time and category** of notifications, never their text). It turns that into windows, stretches, stays and returns against the user's **own** starting normal and shows a weekly Mirror with at most one plain suggestion. It also has a Lake home-screen widget, a "What I see" privacy page (Pause, Export, Delete) and a synthetic-data Time Machine demo.

It never blocks, delays, locks or hides anything. No accounts, no cloud, no `INTERNET` permission, no typing anywhere.

## 2. Session start (every session)

1. You will be told your **track** (T1 Platform, T2 Engine, T3 Shell, T4 Mirror) and your **step** (for example `T2.7`). Work only that step.
2. You will be given the step entry and the DOC 3 section it cites. If you were not given them, say so and stop. Do not guess the design.
3. Edit only the paths your track owns (section 4). Need something elsewhere? Ask; do not edit.
4. One step per session. Do not start the next step until the Evidence for this one has been shown and checked by someone other than you.

## 3. Stack and commands

- UI: Flutter (Dart), `flutter_riverpod` (manual providers, no code generation), `go_router`. **No other packages.**
- Bridge: Pigeon, pull-only. Single file `pigeons/sakshi_api.dart` (31 host methods; the 31st, requestLakeWidget, is a Contract Change).
- Native: Kotlin, **one** Android module. Room (KSP), WorkManager, native `AppWidgetProvider`/RemoteViews, `NotificationListenerService`, kotlinx.serialization, manual DI (`AppContainer`). No Hilt, no kapt.
- Engine: package `com.kleos.sakshi.engine`, pure Kotlin, JUnit 4 local tests.
- minSdk 29. Study day starts at 04:00 local. Raw events kept 14 days.
- Versions are pinned in `docs/versions.md`. Never upgrade one.

```
flutter analyze                    # Dart static check (must be clean)
flutter test                       # Dart tests, incl. copy_rules_test and contract_test
flutter build apk --debug          # whole-app build
cd android && ./gradlew testDebugUnitTest   # Kotlin tests, incl. DependencyRuleTest, PrivacyBoundaryTest, SayingShelfIntegrityTest
tools/audit_apk.sh                 # release permission audit (Track 1)
```

## 4. Layers and ownership

```
Flutter screens (no logic) → Pigeon host API → Kotlin host + Room data → pure Kotlin engine
Mirror = f(raw events, notifications, listener coverage, settings, as-of clock)
```

| Track | Owner paths (edit only these) |
|---|---|
| T1 Platform | `android/app/src/main/kotlin/com/kleos/sakshi/{host,data}/**`, `MainActivity.kt`, `AndroidManifest.xml`, `res/xml/**`, `res/layout/**`, `pigeons/sakshi_api.dart`, `tools/**`, `docs/**`, `fixtures/real_*.json`, `src/test/.../{data,arch}/**` |
| T2 Engine | `.../engine/**` (after T2.8 including `SakshiEngine.kt`), `src/test/.../engine/**`, `src/test/resources/{fixtures,golden}/**` |
| T3 Shell | `pubspec.yaml`, `lib/main.dart`, `lib/app.dart`, `lib/core/**`, `lib/host/{host_client,pigeon_host_client,fake_host}.dart`, `lib/host/fixtures/setup_fixtures.dart`, `lib/features/{setup,what_i_see,settings}/**`, `test/copy_rules_test.dart` |
| T4 Mirror | `lib/features/{mirror,today,lake,shelf,demo}/**`, `lib/host/fixtures/mirror_fixtures.dart`, `test/fixtures/*.json`, `test/contract_test.dart`, `res/drawable/lake_*.xml` (art only), `assets/sakshi/sayings.json` |

Shared surfaces (coordinate before touching): the Pigeon file, `Entities.kt`, `Views.kt`, `Ports.kt`, `Tuning.kt` (values only), `host/Mappers.kt`, `lib/host/host_client.dart`, `lib/host/fixtures/*`, drawable names `lake_still`, `lake_rippled`, `lake_choppy`.

Branches: `feat/t1-platform`, `feat/t2-engine`, `feat/t3-shell`, `feat/t4-mirror`. Commit format: `feat(t2): step T2.7 — parts, steadiness and baseline`.

## 5. Locked contracts (LC-1 to LC-9)

Event vocabulary, `Ports.kt` and derived entities, the `SakshiEngine` façade and Views, the Pigeon file, `sayings.json` schema, `PersonaSpec`, Tuning names, copy rules and host class/file/drawable/channel names are **locked** (DOC 3, v1.1).

You never add, rename or remove a field, method, enum value or port in them. If you think one needs to change: stop, write the proposal (what, why, which tracks are affected) and hand it to the Integration Owner. A change lands only as a Contract Change commit with Pigeon regenerated on both sides in the same commit.

## 6. ALWAYS

- Write the golden tests named in the step first, then make them pass (T1 data/host steps and all T2 steps).
- Take time from the `Clock` port and randomness from the `Randomness` port inside the engine.
- Put every number in `engine/tuning/Tuning.kt` and reference it by name.
- Put every user-facing sentence about the user's data in `SentenceBuilder`. Flutter shows strings it is given and holds chrome strings only (`lib/core/ui_strings.dart`).
- Treat unknown, partial and not-seen as values (null or a sentence), never zero.
- Run the check for your side (`flutter analyze` or `./gradlew testDebugUnitTest`) before reporting.
- When a build breaks, fix it in place with the smallest change and show the error you fixed.
- Note repetition worth abstracting as `[REFACTOR CANDIDATE: …]` in a comment and continue.

## 7. NEVER

- Import `android.*`, `androidx.*`, `host.*`, `data.*` or `io.flutter.*` inside `engine/`; call `System.currentTimeMillis`, `Instant.now`, `LocalDateTime.now` or `Random()` there.
- Add a text field to `NotifEvent`; read notification title or text; log package names in a release build.
- Add a dependency, library or Gradle plugin; upgrade a version; restructure the Flutter template; split the Android module.
- Add `INTERNET`, accessibility, overlay, device-admin, foreground-service, battery-exemption or `QUERY_ALL_PACKAGES` to the release manifest.
- Put logic about attention in collectors, the widget, `HostApiImpl`, `Mappers` or any Flutter file.
- Refactor outside the current step, gold-plate (spinners, animations, logging frameworks, error UI the step did not ask for), merge two steps, or start the next step early.
- Add a text field to setup or measurement. No typing in Sakshi.
- Use a streak, badge, score ring, daily number, leaderboard or share card.
- Write a sentence that matches `\b(focused|distracted|distraction|wasted|waste|failed|failure|streak|lazy|addict\w*|ruin\w*|lost (your )?(focus|concentration)|you (should|must|need to))\b`, contains `!`, names an app as the cause of a feeling, or compares the user to anyone else.
- Block, delay, lock or hide anything on the user's phone.

## 8. Copy voice

Observations with their evidence count, never advice. Example shape: "One app is behind 11 of your last 17 stays." Every pattern line ends with "(based on {w} windows over {d} days)". Steadiness words are exactly Wavering (<90), Steady (90–110), Steadier (>110). In gentle mode the Mirror shows only the return line, stay count, Saying, lapse line and honest data states.

## 9. Definition of done: the Evidence Package

You may say "done" only after showing all three:
1. The exact command output for the step's Done-when check.
2. The diff, limited to the step's target files.
3. One sentence saying why that output satisfies the Done-when line.

A summary is not evidence. Fabricated or paraphrased output is a failed step.

## 10. Free-plan sessions (Tracks 3 and 4)

- One chat per step. Paste, in order: the project snapshot, the step entry, the named excerpts only.
- Ask for all files of the step in one reply (at most three files).
- You cannot run code. The human runs the command and pastes the exact error back. Never guess about a build you cannot see.
- If you notice yourself forgetting rules or inventing fields, say so and ask for a fresh chat with the same pastes.

## 11. If you are told you drifted

- Scope: "Stop. We are only doing [step]." → finish its Done-when first.
- Contract: revert the field and write it as a proposal.
- Layer: move the decision into `engine/` with a golden test, or leave it undone.
- Build fix: smallest change, no new libraries, no restructuring.
- Self-grading: show command output, not a summary.
- Copy: rewrite as an observation with its evidence count.

## 12. Where things live

- Design for each step: DOC 3 section named in the step (`Reference:` line). Do not load other sections or DOC 1 in full.
- Build order, sync points, descope order: DOC 4. The Integration Owner decides any cut; agents never cut features.
- Real-phone findings: `docs/spikes.md`. Research outputs: `docs/research/`.
- Demo phone: Nothing Phone 3a. Stress phone: iQOO Z7. Demo data always shows the permanent "Demo data" banner.
