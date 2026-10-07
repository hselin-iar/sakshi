#!/usr/bin/env bash
# Collects what the T1.14 spikes need from one connected phone running the DEBUG build, into docs/spike_runs/<label>-<time>/.
#   tools/spike_collect.sh iqoo-helper-skipped
# Run it again later (for example hours apart) to see how lastWorkerRunAt and the run count moved while the app was idle.
# The output holds package names from your own phone, so docs/spike_runs/ is git-ignored. Do not commit it.
set -u
LABEL="${1:?usage: tools/spike_collect.sh <label>   e.g. iqoo-helper-skipped}"
PKG=com.kleos.sakshi
ADB="${ADB:-adb}"

command -v "$ADB" >/dev/null || { echo "adb not found. Install Android platform-tools or set ADB=/path/to/adb"; exit 1; }
DEVICES=$("$ADB" devices | awk 'NR>1 && $2=="device"{print $1}')
N=$(printf '%s\n' "$DEVICES" | grep -c .)
if [ "$N" -eq 0 ]; then echo "No phone connected. Plug it in, enable USB debugging, accept the prompt, then retry."; exit 1; fi
if [ "$N" -gt 1 ] && [ -z "${ANDROID_SERIAL:-}" ]; then echo "More than one device. Set ANDROID_SERIAL to one of:"; echo "$DEVICES"; exit 1; fi

STAMP=$(date +%Y%m%d-%H%M)
OUT="docs/spike_runs/${LABEL}-${STAMP}"
mkdir -p "$OUT/db"
printf '*\n' > docs/spike_runs/.gitignore

sh() { "$ADB" shell "$@" 2>&1 | tr -d '\r'; }

{
  echo "label:          $LABEL"
  echo "collected at:   $(date '+%Y-%m-%d %H:%M:%S %Z') (this Mac)"
  echo "phone time:     $(sh date '+%Y-%m-%d %H:%M:%S')"
  echo "manufacturer:   $(sh getprop ro.product.manufacturer)"
  echo "model:          $(sh getprop ro.product.model)"
  echo "android:        $(sh getprop ro.build.version.release) (sdk $(sh getprop ro.build.version.sdk))"
  echo "build:          $(sh getprop ro.build.display.id)"
  echo "app version:    $(sh dumpsys package $PKG | grep -m1 versionName)"
  echo "usage access:   $(sh appops get $PKG GET_USAGE_STATS)"
  echo "listener on:    $(sh settings get secure enabled_notification_listeners | grep -c $PKG) (1 = enabled in Settings)"
  echo "standby bucket: $(sh am get-standby-bucket $PKG)"
  echo "battery exempt: $(sh dumpsys deviceidle whitelist | grep -c $PKG) (1 = on the doze whitelist)"
} > "$OUT/phone.txt"

sh dumpsys jobscheduler | grep -A14 "$PKG" | head -120 > "$OUT/jobscheduler.txt"
"$ADB" logcat -d -s SakshiSetup SakshiIngest SakshiDebug SakshiHost 2>&1 | tr -d '\r' > "$OUT/logcat.txt"

for f in sakshi.db sakshi.db-wal sakshi.db-shm; do
  "$ADB" exec-out run-as "$PKG" cat "databases/$f" > "$OUT/db/$f" 2>/dev/null
  [ -s "$OUT/db/$f" ] || rm -f "$OUT/db/$f"
done

if [ -f "$OUT/db/sakshi.db" ] && command -v sqlite3 >/dev/null; then
  sqlite3 -header -column "$OUT/db/sakshi.db" <<'SQL' > "$OUT/db.txt" 2>&1
.print '== ingest_state (times are UTC) =='
select cursor, datetime(lastRunAt/1000,'unixepoch') lastRunAt, datetime(lastWorkerRunAt/1000,'unixepoch') lastWorkerRunAt,
       workerRuns7d, paused, datetime(firstReadAt/1000,'unixepoch') firstReadAt, lastError, oddEventPairs from ingest_state;
.print ''
.print '== raw events =='
select count(*) events, datetime(min(ts)/1000,'unixepoch') oldest, datetime(max(ts)/1000,'unixepoch') newest from raw_event;
select date(ts/1000,'unixepoch') day, count(*) events from raw_event group by day order by day;
.print ''
.print '== listener sessions =='
select id, datetime(connectedAt/1000,'unixepoch') connected, datetime(disconnectedAt/1000,'unixepoch') disconnected from listener_session order by id;
.print ''
.print '== last 20 notifications (package, time, category, kind, removal; never text) =='
select id, datetime(ts/1000,'unixepoch') at, pkg, category, kind, removal, ongoing from notif_event order by id desc limit 20;
.print ''
.print '== data gaps =='
select id, kind, datetime(start/1000,'unixepoch') start, datetime("end"/1000,'unixepoch') "end" from data_gap;
.print ''
.print '== lake_state =='
select state, phrase, datetime(asOf/1000,'unixepoch') asOf from lake_state;
SQL
else
  echo "(no database pulled: this needs the DEBUG build, and sqlite3 on this Mac)" > "$OUT/db.txt"
fi

echo; echo "Saved to $OUT"; echo; cat "$OUT/phone.txt"; echo; sed -n 1,6p "$OUT/db.txt"
echo; echo "Job entries found: $(grep -c "$PKG" "$OUT/jobscheduler.txt")   Log lines: $(wc -l < "$OUT/logcat.txt" | tr -d ' ')"
