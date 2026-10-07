# Building and checking the release APK (T1.16)

Sakshi is sideloaded: there is no store listing, so the APK itself carries the promise (DOC 2 §2.6). `tools/audit_apk.sh` is the gate.

## Build

```
flutter build apk --release        # -> build/app/outputs/flutter-apk/app-release.apk
tools/audit_apk.sh                 # prints the permission list, exits 0 only if the APK keeps the promise
```

R8 code shrinking and resource shrinking stay on at Flutter's defaults. The debug-only tools (`host/DebugTools`) live in `src/debug` and are not compiled into release; the audit checks both the manifest and the code for them.

## What the audit refuses

An exit code other than 0 means do not ship. It fails on:
- any of: `INTERNET`, `SYSTEM_ALERT_WINDOW`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `QUERY_ALL_PACKAGES`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `BIND_ACCESSIBILITY_SERVICE`, `BIND_DEVICE_ADMIN`, any `FOREGROUND_SERVICE*`, and contacts, location, camera, microphone and SMS permissions
- an accessibility service, a device admin receiver, a foreground service or its type, or WorkManager's foreground service in the manifest
- the `DebugTools` receiver in the manifest or its code in the dex
- a debuggable APK, `allowBackup` not set to false, or the notification listener service missing
- a signature that does not verify, or one made with the Android **debug** key

What it lets through today (all added by WorkManager's own library manifest, none by Sakshi): `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`. `PACKAGE_USAGE_STATS` and `POST_NOTIFICATIONS` are the two Sakshi declares.

`ALLOW_DEBUG_SIGNED=1 tools/audit_apk.sh` tolerates the debug key, for development only. Never use it for an APK you hand to anyone.

## The signing key (keep it out of git, and back it up)

The release key is **not** in the repository. This Mac holds:
- `~/.sakshi/sakshi-release.jks` (the keystore, `chmod 600`)
- `android/key.properties` (its path, alias and passwords; git-ignored)

**Back both up somewhere safe** (a password manager or an encrypted drive). Without the keystore you cannot publish an update that installs over an existing install; a different key means uninstalling first, which deletes the user's local data.

On another machine, create `android/key.properties`:
```
storeFile=/absolute/path/to/sakshi-release.jks
storePassword=...
keyAlias=sakshi
keyPassword=...
```
Without that file the build falls back to the debug key, prints a warning, and `tools/audit_apk.sh` refuses the result.

To make a new key (this invalidates updates for existing installs):
`keytool -genkeypair -keystore ~/.sakshi/sakshi-release.jks -storetype PKCS12 -alias sakshi -keyalg RSA -keysize 2048 -validity 10000`

## Install and the no-network demonstration (on the phones)

1. `adb install -r build/app/outputs/flutter-apk/app-release.apk`, **and** separately copy the file to the phone and install it from the Files app. The two routes can differ for the notification-access "restricted setting" (spike S-H, see `docs/phone_test_day.md`).
2. Turn airplane mode on and complete the setup flow (usage access, optionally notification access). Sakshi works with no network because it has no `INTERNET` permission at all; the audit output above is the proof.
3. Screenshot the install and the finished setup, on the Nothing Phone 3a and the iQOO Z7.

The setup flow itself is Track 3's screens. Until they land, the debug-screen buttons (`openUsageAccessSettings`, `getSetupState`) stand in for it.
