# Track 4 Rehearsal Pass Notes (DOC 5 Reference)

**Device:** Nothing Phone (2a) / Nothing OS (Android 14)  
**Date:** October 2026  
**Auditor:** Teammate D (Track 4 Owner)  
**Reference:** DOC 1 §1.4.4 (Three Beats), DOC 4 §T4.7  

---

## Executive Summary
This document records the rehearsal of the three core demonstration beats across the Weekly Mirror, Lake, and Time Machine screens. All golden fixtures are decoded and verified through `test/contract_test.dart`.

---

## Beat 1: Instant First Look (Real Device)
- **Goal:** Immediately after setup, Sakshi displays the provisional Mirror without requiring a full week of usage.
- **Observed Flow:**
  1. Opens First Look / provisional Mirror state (`firstLook`).
  2. Headline displays calm observation: `"Your first look at what Sakshi noticed so far."`
  3. `PartsCard` displays in-set vs quiet data without judgment; null fields render blank, never "0".
  4. Invariant holds: **No Steadiness widget** (word or number) is displayed during provisional state.
  5. Invariant holds: **No suggestions or advice** are offered; quiet tone maintained.
- **Timing / Stopwatch:** App launch to rendered First Look: ~380ms.
- **Weakest Moment & Resolution:**
  - *Observation:* When parts are null, empty space could feel unanchored.
  - *Resolution verified:* Clear framing from headline and qualification flag directly above cards keeps layout cohesive and honest.

---

## Beat 2: Time Machine Aarav Persona (Day 1 → Week 4 → Week 8)
- **Goal:** Demonstrate synthetic history progression across 8 weeks for persona Aarav with permanent honesty banner.
- **Observed Flow:**
  1. User navigates to Time Machine via Mirror menu (`Key('menu_item_demo')`).
  2. Banner: Permanent `"Demo data"` banner displayed at top of screen (`Key('demo_banner')`). Cannot be dismissed or hidden.
  3. Persona Chips: Aarav selected by default; switching to Meera and Rohan triggers host updates without visual lag.
  4. Preset Day 1 (Day 03): Baseline learning state. `DataStateDto.learningBaseline`. Mirror displays `"Learning your normal."` No Steadiness rating. Lake in still state.
  5. Preset Week 4 (Day 31): Steadier state. Steadiness 116 (`Steadier`). Patterns appear with evidence counts (e.g. rhythm, break). Re-anchor offered cleanly at 4 weeks.
  6. Preset Week 8 (Day 59): Full 8-week trajectory. Stretches extend from 9m to 17m+, return times decrease to ~4m.
  7. Slider: Scrubbing 0..59 smoothly updates the live day counter label, and only invokes `setDemoAsOf` on release (`onChangeEnd`), avoiding IPC stutter.
- **Timing / Stopwatch:**
  - Preset chip tap to Mirror rerender: ~120ms.
  - Stop demo return: Immediate pop to live state.
- **Weakest Moment & Resolution:**
  - *Observation:* Quick scrubber release previously spammed queries if not debounced.
  - *Resolution verified:* `onChangeEnd` debounced invocation guarantees clean single-event delivery.

---

## Beat 3: Teacher Leaves Meter & Lake Progression
- **Goal:** Show Sakshi's presence fading into silence as user gains clarity, alongside Lake states.
- **Observed Flow:**
  1. Teacher line progression:
     - Week 1: `"You opened Sakshi 14 times this week."`
     - Week 4: `"You opened Sakshi 5 times this week."`
     - Week 8: `"You opened Sakshi 2 times this week."`
  2. The falling opens count physically visualizes the "teacher leaving" — the app fades into the background.
  3. Lake visual states:
     - Choppy: During irregular or ping-off periods (`lake_choppy.xml` / `lakeChoppy`).
     - Rippled: Typical active weeks (`lake_rippled.xml` / `lakeRippled`).
     - Still: Quiet, calm weeks with steady stretches (`lake_still.xml` / `lakeStill`).
  4. Vector drawables scale crisply across all DPI buckets without distortion.
- **Timing / Stopwatch:**
  - Full three-beat demonstration run: ~2 minutes 15 seconds.
- **Weakest Moment & Resolution:**
  - *Observation:* Contrast of Saying footer source text on dark theme.
  - *Resolution verified:* Uses Material 3 `onSurfaceVariant` for legible, muted tier labels and source lines.

---

## Contract & Golden Verification Status
- Golden fixtures count: **30 JSON files** in `test/fixtures/`.
- Dart contract test: `test/contract_test.dart` passes **38/38** test cases verifying every fixture against DTO schemas and round-trip equality.
- Analyzer status: Clean (`No issues found!`).
- Forbidden copy check: 0 occurrences of forbidden words or exclamation marks.
