import '../host_client.dart';

// ---------------------------------------------------------------------------
// SetupFixtures — canonical SetupStateDto variants for FakeHost and tests.
//
// Variants (from the T3.2 spec):
//   nothingGranted          — fresh install, no permissions
//   usageGranted            — usage access ticked; notification not yet
//   bothGranted             — both granted, setup not yet complete
//   restrictedSuspected     — notification access not granted after settings
//   allDone                 — fully set up, no demo
//   demoActive              — isDemo = true, all permissions granted
//
// appList — about 20 realistic launcher apps for the work-set screen.
// ---------------------------------------------------------------------------

abstract final class SetupFixtures {
  static CollectionHealthDto get _healthOk => CollectionHealthDto(
        workerRuns7d: 7,
        paused: false,
        listenerCoverage7d: 0.95,
      );

  static CollectionHealthDto get _healthFresh => CollectionHealthDto(
        workerRuns7d: 0,
        paused: false,
      );

  // Nothing granted — fresh install.
  static SetupStateDto get nothingGranted => SetupStateDto(
        usageAccessGranted: false,
        notificationAccessGranted: false,
        restrictedSettingsSuspected: false,
        workSetSaved: false,
        studyHoursSaved: false,
        batteryHelperShown: false,
        weeklyNoteEnabled: false,
        gentleMode: false,
        isDemo: false,
        health: _healthFresh,
      );

  // Usage access granted; notification not yet.
  static SetupStateDto get usageGranted => SetupStateDto(
        usageAccessGranted: true,
        notificationAccessGranted: false,
        restrictedSettingsSuspected: false,
        workSetSaved: false,
        studyHoursSaved: false,
        batteryHelperShown: false,
        weeklyNoteEnabled: false,
        gentleMode: false,
        isDemo: false,
        health: _healthFresh,
      );

  // Both permissions granted; setup steps not yet completed.
  static SetupStateDto get bothGranted => SetupStateDto(
        usageAccessGranted: true,
        notificationAccessGranted: true,
        restrictedSettingsSuspected: false,
        workSetSaved: false,
        studyHoursSaved: false,
        batteryHelperShown: false,
        weeklyNoteEnabled: false,
        gentleMode: false,
        isDemo: false,
        health: _healthFresh,
      );

  // Restricted settings suspected — user returned from settings but
  // notification access was not granted (sideloaded APK on Android 13+).
  static SetupStateDto get restrictedSuspected => SetupStateDto(
        usageAccessGranted: true,
        notificationAccessGranted: false,
        restrictedSettingsSuspected: true,
        workSetSaved: false,
        studyHoursSaved: false,
        batteryHelperShown: false,
        weeklyNoteEnabled: false,
        gentleMode: false,
        isDemo: false,
        health: _healthFresh,
      );

  // Fully set up — normal use, no demo.
  static SetupStateDto get allDone => SetupStateDto(
        usageAccessGranted: true,
        notificationAccessGranted: true,
        restrictedSettingsSuspected: false,
        workSetSaved: true,
        studyHoursSaved: true,
        batteryHelperShown: true,
        weeklyNoteEnabled: true,
        gentleMode: false,
        isDemo: false,
        health: _healthOk,
      );

  // Demo active — permanent banner shown.
  static SetupStateDto get demoActive => SetupStateDto(
        usageAccessGranted: true,
        notificationAccessGranted: true,
        restrictedSettingsSuspected: false,
        workSetSaved: true,
        studyHoursSaved: true,
        batteryHelperShown: true,
        weeklyNoteEnabled: false,
        gentleMode: false,
        isDemo: true,
        health: _healthOk,
      );

  // ---------------------------------------------------------------------------
  // App list — ~20 representative launcher apps for the work-set screen.
  // pkg values are real package names; labels are what a user would see.
  // suggestedInSet = true means the engine pre-ticks this app in the work set.
  // suggestedDepends = true means the engine suggests it as a Depends app.
  // ---------------------------------------------------------------------------

  static List<AppDto> get appList => [
        AppDto(pkg: 'com.google.android.youtube',       label: 'YouTube',        suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.instagram.android',            label: 'Instagram',      suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.whatsapp',                     label: 'WhatsApp',       suggestedInSet: false, suggestedDepends: true),
        AppDto(pkg: 'org.telegram.messenger',           label: 'Telegram',       suggestedInSet: false, suggestedDepends: true),
        AppDto(pkg: 'com.twitter.android',              label: 'X (Twitter)',    suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.snapchat.android',             label: 'Snapchat',       suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.zhiliaoapp.musically',         label: 'TikTok',         suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.reddit.frontpage',             label: 'Reddit',         suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.linkedin.android',             label: 'LinkedIn',       suggestedInSet: false, suggestedDepends: false),
        AppDto(pkg: 'com.google.android.apps.maps',     label: 'Maps',           suggestedInSet: false, suggestedDepends: false),
        AppDto(pkg: 'com.amazon.mShop.android.shopping',label: 'Amazon',         suggestedInSet: false, suggestedDepends: false),
        AppDto(pkg: 'com.flipkart.android',             label: 'Flipkart',       suggestedInSet: false, suggestedDepends: false),
        AppDto(pkg: 'com.netflix.mediaclient',          label: 'Netflix',        suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.spotify.music',                label: 'Spotify',        suggestedInSet: false, suggestedDepends: true),
        AppDto(pkg: 'com.discord',                      label: 'Discord',        suggestedInSet: true,  suggestedDepends: false),
        AppDto(pkg: 'com.google.android.gm',            label: 'Gmail',          suggestedInSet: false, suggestedDepends: true),
        AppDto(pkg: 'com.microsoft.launcher',           label: 'Edge',           suggestedInSet: false, suggestedDepends: false),
        AppDto(pkg: 'com.android.chrome',               label: 'Chrome',         suggestedInSet: false, suggestedDepends: false),
        AppDto(pkg: 'com.duolingo',                     label: 'Duolingo',       suggestedInSet: false, suggestedDepends: false),
        AppDto(pkg: 'com.byju.s',                       label: 'BYJU\'S',        suggestedInSet: false, suggestedDepends: false),
      ];
}
