# Phone test day: every outstanding device check, in one pass

Covers T1.1 to T1.13 and the T1.14 spikes. Nothing here has been run. Do it on the **iQOO Z7** and on the **Nothing Phone 3a**, and write the results into the table at the end (and into `docs/spikes.md` where a section asks for it).

Builds are in the T1 worktree (`/Users/hselin/sakshi-t1`):
- debug: `build/app/outputs/flutter-apk/app-debug.apk` (everything except S-H)
- release: `build/app/outputs/flutter-apk/app-release.apk` (S-H only; no INTERNET, no debug receivers)

Rebuild first if the code changed: `flutter build apk --debug` and `flutter build apk --release`.

## 0. Before you start the clock

1. Turn on Developer options, then USB debugging (on Vivo/iQOO also allow "USB debugging (security settings)" or "Install via USB" if it asks). `adb devices` must list the phone.
2. **Start S-D first, because it needs hours of idle time.** Install the debug build, open Sakshi once so it schedules the 15-minute job, then run `tools/spike_collect.sh <phone>-helper-skipped` (this is reading number 1). Close the app, lock the phone, and carry on with the other checks that do not need Sakshi in the foreground for long; come back to collect again after 3 to 6 hours.
3. Keep a one-line log per test: phone, Android/OS version, helper on or off, result.

Install: `adb install -r build/app/outputs/flutter-apk/app-debug.apk`

## 1. Debug screen basics (T1.1, T1.3, T1.9)

Open Sakshi. The screen is the host debug screen, one button per method.
- Press `getSetupState`. Expect `usageAccessGranted: false`. Screenshot (T1.1).
- Press `openUsageAccessSettings`, grant Sakshi, come back, press `getSetupState` again: now `true` (T1.3). Note both lines, or `adb logcat -s SakshiSetup`.
- Press every other button once. Anything that shows `ERROR` is a finding; note which. (`tapTryThis` and `reanchorBaseline` are expected to answer `STALE_SUGGESTION` / `REANCHOR_NOT_ALLOWED` until the engine is real. Skip `DELETE EVERYTHING` for now; it is in step 8.)

## 2. S-H: the sideload and restricted-settings path (T1.3, T1.6)

This one uses the **release** APK. Uninstall the debug build first (`adb uninstall com.kleos.sakshi`).
1. Install the release APK **two ways** and test each: `adb install` and, separately, copy the file to the phone and tap it in the Files app. The point of S-H is whether the way you install changes the next step.
2. Open Sakshi (the release build has no debug screen content beyond the same button list; use `openNotificationAccessSettings`).
3. In Notification access, is the Sakshi toggle greyed out, with a "restricted setting" message? Write down the exact words.
4. If greyed: App info, the three-dot menu, "Allow restricted settings", confirm, then try the toggle again. Write down every tap and menu name exactly as shown, and the Android/OS version.
5. After returning, `getSetupState` should show `restrictedSettingsSuspected: true` while the toggle was unreachable and `notificationAccessGranted: true` once it works.
Then reinstall the debug build for the remaining steps.

## 3. S-A and S-B: events (T1.5)

1. Do the scripted 5-minute sequence: switch between three apps, enter split-screen, start picture-in-picture, lock then unlock.
2. `adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.DUMP_EVENTS --ei hours 72`
3. `adb logcat -d -s SakshiDebug` prints the count and the oldest timestamp. Then `adb exec-out run-as com.kleos.sakshi cat files/dumps/events.json > fixtures/real_s_a.json`.
4. Write the three S-B lines in `docs/spikes.md`: oldest event returned, events per day, anything odd (split-screen pairs, PiP, lock ordering).

## 4. S-C and notifications (T1.6)

1. With notification access granted, post test notifications from three apps, tap one, dismiss one.
2. `tools/spike_collect.sh <phone>-notifs`: `db.txt` shows the last 20 rows. You want five rows, one with `removal = CLICK`. Never any text.
3. S-C: Settings, App info, Force stop. Wait a few minutes, post another notification, then open Sakshi. In `db.txt` the `listener_session` table should show the old session closed at the last notification it heard and a new one open. Record whether the listener rebound by itself or only when you opened Sakshi, and how long it took.
4. **On the iQOO Z7 repeat step 3 twice:** once with the battery helper skipped and once after applying it (step 9).

## 5. Launcher apps (T1.8, S-E)

Press `listLauncherApps` on the debug screen: the first 10 labels show. Check the list looks like your phone's apps. After a day of normal use, run the DUMP_EVENTS command again and compare the package names in the dump with the launcher list: record those that are in events but not the list (they fall back to package names), and any that belong in `assets/sakshi/neutral_packages.json`.

## 6. Widget (T1.10, S-F)

1. Long-press the home screen, add the "sakshi" widget (2x1).
2. For each of `still`, `rippled`, `choppy`, `learning`, `no_data`: `adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.SET_LAKE_STATE --es state <state>`. Screenshot each. It must redraw and show its "as of" time.
3. Reboot. Does it still show the last state? Change the launcher and add the widget again: does it redraw from stored state? Record both.

## 7. Weekly note (T1.11)

1. `adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.FORCE_WEEKLY_NOTE` posts the note. It must be silent, show no badge, read "Sakshi" / "Your Mirror is ready", and have no buttons. Screenshot it and its channel settings (long-press the note).
2. Run the command again: `adb logcat -d -s SakshiDebug` must say `ALREADY_SENT` and nothing new appears.
3. Deny notification permission, press `setWeeklyNote(false)` then try enabling it from the debug screen: it must answer false.

## 8. Pause, export, delete, demo (T1.13)

1. `exportData`: the Android share sheet opens. Screenshot it. Send the file to yourself and open it: no title, text or app label, only package names. Try again with raw events if you want to see those.
2. Press `pause(true)`, wait a minute, then `pause(false)`. Run `tools/spike_collect.sh <phone>-pause`: `db.txt` must show exactly one PAUSED gap, closed on resume.
3. `startDemo(aarav)`, `setDemoAsOf(31)`: the Lake phrase ends with "(demo)"; `pause`, `exportData` and `deleteEverything` must each answer `DEMO_ACTIVE`; `stopDemo` returns the real Lake.
4. Hash the real database around a demo: `adb exec-out run-as com.kleos.sakshi sh -c 'cat databases/sakshi.db databases/sakshi.db-wal' | shasum -a 256` before `startDemo` and after `stopDemo`. The two lines must match.
5. Last: `DELETE EVERYTHING`. The Lake reads "Nothing to show yet." and `db.txt` shows empty tables.

## 9. Battery helper (T1.12) and S-D readings

1. `openBatterySettings`: screenshot the page that opens. It must be Android's own list. If the system "allow Sakshi to ignore battery optimisations" dialog ever appears, that is a bug: report it.
2. Follow each path in `docs/research/oem_battery.md` for that phone by hand. Correct the wording, set the status to VERIFIED_ON_DEVICE in the doc **and** in `host/BatteryTips.kt`.
3. **iQOO Z7:** apply the helper steps, then leave the phone idle again and collect a second series (`<phone>-helper-applied`).
4. Collect the S-D readings: run `tools/spike_collect.sh` again at 3 to 6 hours after reading number 1. `lastWorkerRunAt` should have advanced; the gap between readings divided by how far it moved is your cadence. Note that the app only stores the latest run and a decaying count, so the cadence is read from successive collections, with the logcat `run:` lines as a bonus.

## 10. Results table (fill in; this is the T1.14 Done-when)

| Phone | OS version | Helper | Listener reconnects after force-stop? | Idle cadence (runs over N hours) | Widget redraws / survives reboot + launcher change? | Exact sideload path (S-H) | Notes |
|---|---|---|---|---|---|---|---|
| iQOO Z7 | | skipped | | | | | |
| iQOO Z7 | | applied | | | | | |
| Nothing Phone 3a | | n/a | | | | | |

Demo phone: **Nothing Phone 3a** unless S-C, S-D and S-F above say otherwise. Write the decision here: ______

Any other teammate with a Xiaomi, Oppo, Vivo or Realme phone should run sections 4 (S-C) and 9 (S-D) and add a row.

## If something fails

Report it; do not fix it with a foreground service or an exemption request (DOC 4 T1.14). The honest state is the fix: tell me the result and I will propose the smallest change in `host/` or say what the app should show instead.
