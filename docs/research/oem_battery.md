# OEM battery paths (T1.12)

Where a user goes to let Sakshi keep reading in the background. The helper opens Android's own battery page, shown once and skippable (DOC 2 §2.7.4); this file is the plain wording for the short tips under it. Track 3 copies the verified steps into `ui_strings.dart` in T3.5. The same steps live in `host/BatteryTips.kt`, and a test fails if the two differ.

## How far to trust each path

| Status | Meaning |
|---|---|
| VERIFIED_ON_DEVICE | someone followed it on the phone and it matched |
| PUBLIC_SOURCE | taken from dontkillmyapp.com, fetched 2026-10-07. Those pages show no date and say they are incomplete, so treat them as a starting point |
| UNVERIFIED | written from the general shape of the OS, with no source found |

**Nothing is VERIFIED_ON_DEVICE yet.** Research prompt R4 was not run. The Nothing OS page on dontkillmyapp.com returned 404, and its Oppo page only documents old models (Oppo F1S, ColorOS 5 and 6), so those two are UNVERIFIED. Menu names change between versions: on the iQOO Z7 and the Nothing Phone 3a, follow each path by hand and correct the wording below, then change the status.

## Vivo and iQOO (Funtouch OS / OriginOS) — PUBLIC_SOURCE
1. Press and hold the Sakshi icon, then tap App info.
2. Tap Battery, then choose Unrestricted (on some versions: Battery optimization, then Not optimized).
3. Open Settings, then Battery, then Background power consumption management, and allow Sakshi.
4. Open Settings, then More settings, then Applications, then Autostart, and turn Sakshi on.

## Nothing OS — UNVERIFIED
1. Open Settings, then Apps, then Sakshi.
2. Tap App battery usage (or Battery) and choose Unrestricted.

## Xiaomi, Redmi and POCO (HyperOS / MIUI) — PUBLIC_SOURCE
1. Open Settings, then Apps, then Sakshi, then App permissions, and turn on Autostart.
2. Open Security, then Battery, then App battery saver, find Sakshi and choose No restrictions.

## Oppo, Realme and OnePlus (ColorOS) — UNVERIFIED
1. Open Settings, then Apps, then App management, then Sakshi.
2. Turn on Allow auto-launch (older versions: Allow auto start-up).
3. Tap Battery usage and choose Allow background activity.

## Samsung (One UI) — PUBLIC_SOURCE
1. Open Settings, then Apps, then Sakshi, then Battery, and choose Unrestricted.
2. Open Settings, then Battery, then Background usage limits, and turn off Put unused apps to sleep, or remove Sakshi from the sleeping apps list.

## Any other phone
No steps. The button opens Android's own battery page and the tips line stays out of the way.
