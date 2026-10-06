# Real-phone findings

## S-A / S-B (T1.5): event reading on the Nothing Phone 3a — NOT YET RUN

Pending a real phone. How to run, with the debug build installed and usage access granted:

1. Do the scripted 5-minute sequence: switch between three apps, enter split-screen, start picture-in-picture, lock and unlock.
2. `adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.DUMP_EVENTS --ei hours 72`
3. `adb logcat -s SakshiDebug` prints the count and the oldest timestamp.
4. `adb exec-out run-as com.kleos.sakshi cat files/dumps/events.json > fixtures/real_s_a.json`

Record here, three lines:
- Oldest event returned (how far back Android keeps events):
- Events per day (roughly):
- Anything odd in the sequence (split-screen pairs, PiP, lock ordering):

## S-C (T1.6): listener survival — NOT YET RUN

With notification access granted on the Nothing Phone 3a:
1. Post test notifications from three apps, tap one, dismiss one.
2. Force-stop Sakshi (App info > Force stop), wait a few minutes, post another notification, open Sakshi.
3. Expected: tapped notification stored with `removal = CLICK`; the session open before the force-stop is closed at the last notification it heard; a new session opens on rebind (the app open calls requestRebind if the listener is granted but not bound).
4. Export a few rows: `adb exec-out run-as com.kleos.sakshi sqlite3 databases/sakshi.db "select * from notif_event"` (if sqlite3 is missing on the phone, use Android Studio's App Inspection).

Record here: did the listener rebind by itself after force-stop, or only after opening Sakshi? How long until it did?

## S-D (T1.7): background runs while the app is closed — NOT YET RUN

1. Install the debug build, open Sakshi once (this schedules the 15-minute job), then close it and leave the phone idle for a few hours.
2. `adb shell dumpsys jobscheduler | grep -A6 com.kleos.sakshi` shows the periodic job.
3. `adb shell am broadcast -n com.kleos.sakshi/.host.DebugTools -a com.kleos.sakshi.RUN_INGEST_NOW` forces one run; `adb logcat -s SakshiIngest` prints `run: Ran`.
4. Read two consecutive `lastWorkerRunAt` values from `ingest_state` (App Inspection, or `run-as com.kleos.sakshi sqlite3 databases/sakshi.db "select lastWorkerRunAt, workerRuns7d from ingest_state"`).

Record here: the dumpsys excerpt, the two timestamps, and on the iQOO Z7 whether runs stopped while idle (and whether the battery helper changed that).
