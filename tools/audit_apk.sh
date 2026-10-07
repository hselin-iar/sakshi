#!/usr/bin/env bash
# The gate for "the manifest is the promise" (DOC 2 §2.6). Reads a built APK, prints its permissions and exits non-zero on
# anything Sakshi promised never to ship. It also refuses a debug-signed, debuggable or debug-tooled APK.
#
#   tools/audit_apk.sh [path/to/app-release.apk]        default: build/app/outputs/flutter-apk/app-release.apk
#   ALLOW_DEBUG_SIGNED=1 tools/audit_apk.sh             development only: tolerate the debug signing key
set -u

APK="${1:-build/app/outputs/flutter-apk/app-release.apk}"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
AAPT2="$(command -v aapt2 || echo "$BT/aapt2")"
APKSIGNER="$(command -v apksigner || echo "$BT/apksigner")"

[ -f "$APK" ] || { echo "AUDIT FAILED: no APK at $APK (build it with: flutter build apk --release)"; exit 2; }
[ -x "$AAPT2" ] || { echo "AUDIT FAILED: aapt2 not found. Set ANDROID_HOME to an SDK with build-tools."; exit 2; }

FAIL=0
fail() { echo "  FAIL: $1"; FAIL=1; }
ok()   { echo "  ok:   $1"; }

echo "Auditing $APK"
echo
echo "Permissions in the built APK:"
PERMS=$("$AAPT2" dump permissions "$APK" 2>&1 | sed -n "s/^uses-permission[^:]*: name='\([^']*\)'.*/\1/p" | sort -u)
if [ -z "$PERMS" ]; then echo "  (none)"; else printf '%s\n' "$PERMS" | sed 's/^/  /'; fi
echo

# --- permissions Sakshi promised never to ship ---
# INTERNET is allowed since 2026-10-07 for "Ask Sakshi" (docs/ask.md); it is checked separately below, with cleartext traffic off.
FORBIDDEN_EXACT="android.permission.SYSTEM_ALERT_WINDOW
android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
android.permission.QUERY_ALL_PACKAGES
android.permission.BIND_ACCESSIBILITY_SERVICE
android.permission.BIND_DEVICE_ADMIN
android.permission.SCHEDULE_EXACT_ALARM
android.permission.USE_EXACT_ALARM
android.permission.READ_CONTACTS
android.permission.WRITE_CONTACTS
android.permission.GET_ACCOUNTS
android.permission.ACCESS_FINE_LOCATION
android.permission.ACCESS_COARSE_LOCATION
android.permission.ACCESS_BACKGROUND_LOCATION
android.permission.CAMERA
android.permission.RECORD_AUDIO
android.permission.READ_SMS
android.permission.SEND_SMS
android.permission.RECEIVE_SMS"
echo "Checks:"
for p in $FORBIDDEN_EXACT; do printf '%s\n' "$PERMS" | grep -qx "$p" && fail "forbidden permission $p"; done
printf '%s\n' "$PERMS" | grep -q '^android.permission.FOREGROUND_SERVICE' && fail "a foreground-service permission is declared"
[ "$FAIL" -eq 0 ] && ok "none of the forbidden permissions are declared"
if printf '%s\n' "$PERMS" | grep -qx 'android.permission.INTERNET'; then
  echo "  note: INTERNET is declared (allowed for Ask Sakshi, decision 2026-10-07: HTTPS only, summary numbers only)"
fi

# --- manifest contents: services, receivers, flags ---
MANIFEST=$("$AAPT2" dump xmltree --file AndroidManifest.xml "$APK" 2>&1)
echo "$MANIFEST" | grep -q 'android.accessibilityservice.AccessibilityService' && fail "an accessibility service is declared"
echo "$MANIFEST" | grep -q 'android.app.action.DEVICE_ADMIN_ENABLED' && fail "a device admin receiver is declared"
echo "$MANIFEST" | grep -q 'foregroundServiceType' && fail "a foreground service type is declared"
echo "$MANIFEST" | grep -q 'SystemForegroundService' && fail "WorkManager's foreground service is declared"
echo "$MANIFEST" | grep -q 'host.DebugTools' && fail "the debug tools receiver is in this APK"
echo "$MANIFEST" | grep -q 'debuggable(0x0101000f)=true' && fail "the APK is debuggable"
echo "$MANIFEST" | grep -q 'allowBackup(0x01010280)=false' || fail "allowBackup is not set to false"
echo "$MANIFEST" | grep -q 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE' || fail "the notification listener service is missing"
echo "$MANIFEST" | grep -q 'usesCleartextTraffic(0x010104ec)=false' || fail "cleartext (non-HTTPS) traffic is not switched off"
[ "$FAIL" -eq 0 ] && ok "no accessibility, device admin, foreground service or debug tools; backup off; listener present"

# --- debug tooling in the code itself ---
if unzip -p "$APK" 'classes*.dex' 2>/dev/null | grep -aq 'DebugTools'; then fail "DebugTools is compiled into this APK"; else ok "DebugTools is not in the code"; fi

# --- signing ---
if [ -x "$APKSIGNER" ]; then
  CERTS=$("$APKSIGNER" verify --print-certs "$APK" 2>&1)
  if [ $? -eq 0 ]; then ok "signature verifies"; else fail "the signature does not verify"; fi
  if echo "$CERTS" | grep -q 'CN=Android Debug'; then
    if [ "${ALLOW_DEBUG_SIGNED:-0}" = "1" ]; then echo "  warn: signed with the DEBUG key (allowed by ALLOW_DEBUG_SIGNED=1)"; else fail "signed with the Android debug key; create android/key.properties (docs/release.md)"; fi
  else
    ok "signed with a non-debug key: $(echo "$CERTS" | sed -n 's/.*certificate DN: //p' | head -1)"
  fi
else
  fail "apksigner not found, so the signature could not be checked"
fi

echo
if [ "$FAIL" -eq 0 ]; then echo "AUDIT PASSED"; exit 0; else echo "AUDIT FAILED"; exit 1; fi
