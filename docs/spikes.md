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
