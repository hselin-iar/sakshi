// Chrome-only strings for the Sakshi Flutter shell.
//
// RULES (LC-8):
//   - User-facing sentences about the user's data come from SentenceBuilder
//     (Kotlin). This file holds only UI chrome: titles, button labels,
//     permission explanations and the first-screen text.
//   - No exclamation marks. No forbidden words.
//   - More strings are added in later steps (T3.2 – T3.7, T4.*).

// ---------------------------------------------------------------------------
// First screen (four lines — DOC 1 §1.4.1)
// ---------------------------------------------------------------------------

const firstScreenLine1 =
    'Sakshi reads what is already on your phone — which apps are open and when.';

const firstScreenLine2 =
    'It never reads messages, titles, contents or what you type.';

const firstScreenLine3 =
    'Nothing leaves your device. No account, no internet, no cloud.';

const firstScreenLine4 = 'It shows you patterns in your own data, once a week.';

// ---------------------------------------------------------------------------
// Screen titles
// ---------------------------------------------------------------------------

const titleSetup = 'Getting started';
const titleUsageAccess = 'Usage access';
const titleNotificationAccess = 'Notification access';
const titleWorkSet = 'Your apps';
const titleStudyHours = 'Study hours';
const titleAgeTap = 'One question';
const titleSettings = 'Settings';
const titleWhatISee = 'What Sakshi sees';
const titleMirror = 'Weekly Mirror';
const titleToday = 'Today so far';
const titleShelf = 'Sayings';
const titleDemo = 'Demo';

// ---------------------------------------------------------------------------
// Demo banner
// ---------------------------------------------------------------------------

const demoBannerText = 'Demo data';

// ---------------------------------------------------------------------------
// Settings screen (T3.5)
// ---------------------------------------------------------------------------

const settingsWeeklyNoteLabel = 'Weekly note';
const settingsWeeklyNoteSubtitle =
    'A quiet notification when your Mirror is ready.';
const settingsWeeklyNoteOffLine =
    'Notification permission was not granted. The weekly note is off.';
const settingsGentleModeLabel = 'Gentle mode';
const settingsGentleModeSubtitle =
    'Hides patterns and suggestions. Shows stay count and return time only.';
const settingsRedoWorkSet = 'Change your apps';
const settingsRedoStudyHours = 'Change study hours';
const settingsWhatISeeLink = 'What Sakshi sees';

// ---------------------------------------------------------------------------
// Battery helper (T3.5) — OEM-specific steps; iQOO/Vivo and Nothing first.
// Source: DOC 2 §2.7.4 and R4 (verified on iQOO Z7 in T1.12).
// Track 1 owns factual accuracy; Track 3 owns file position only.
// ---------------------------------------------------------------------------

const batteryHelperIntro =
    'Sakshi runs a short background task every few hours to process '
    'your usage. On some phones the system stops background tasks '
    'aggressively. The steps below take about one minute and are needed '
    'only once.';

const batteryOemIqooTitle = 'iQOO or Vivo';
const batteryOemIqooSteps =
    '1. Open Phone Manager → App management → Sakshi.\n'
    '2. Tap "Battery" and set to "No restrictions".\n'
    '3. Tap "Open battery settings" below to confirm.';

const batteryOemNothingTitle = 'Nothing Phone';
const batteryOemNothingSteps =
    '1. Long-press Sakshi → App info → Battery.\n'
    '2. Choose "Unrestricted".\n'
    '3. Tap "Open battery settings" below to confirm.';

const batteryOemOneplusTitle = 'OnePlus or Oppo';
const batteryOemOneplusSteps =
    '1. Settings → Battery → Battery optimisation → Sakshi.\n'
    '2. Set to "Don\'t optimise".\n'
    '3. Tap "Open battery settings" below to confirm.';

const batteryOemXiaomiTitle = 'Xiaomi, Redmi or Poco';
const batteryOemXiaomiSteps =
    '1. Settings → Apps → Manage apps → Sakshi → Battery saver.\n'
    '2. Choose "No restrictions".\n'
    '3. Tap "Open battery settings" below to confirm.';

const batteryOemSamsungTitle = 'Samsung';
const batteryOemSamsungSteps =
    '1. Settings → Apps → Sakshi → Battery → Battery usage.\n'
    '2. Choose "Unrestricted".\n'
    '3. Tap "Open battery settings" below to confirm.';

const batteryOemOtherTitle = 'Other phones';
const batteryOemOtherSteps =
    '1. Settings → Apps → Sakshi → Battery.\n'
    '2. Choose "Unrestricted" or "Don\'t optimise".\n'
    '3. Tap "Open battery settings" below.';

// ---------------------------------------------------------------------------
// What I See screen (T3.6)
// ---------------------------------------------------------------------------

const whatISeeExportLabel = 'Export data';
const whatISeeExportIncludeRaw = 'Include raw events';
const whatISeeExportDone = 'Exported: ';
const whatISeePauseLabel = 'Pause collection';
const whatISeeDeleteLabel = 'Delete everything';
const whatISeeDeleteConfirm1 =
    'This will delete all your data on this device. Are you sure?';
const whatISeeDeleteConfirm2 = 'This cannot be undone. Tap again to confirm.';

// ---------------------------------------------------------------------------
// Home (the dashboard every launch lands on)
// ---------------------------------------------------------------------------

const titleHome = 'Sakshi';
const homeGreetingMorning = 'Good morning';
const homeGreetingAfternoon = 'Good afternoon';
const homeGreetingEvening = 'Good evening';
const homeSubtitle = 'Here is how things stand.';
const homeSyncing = 'Reading what Android has recorded…';
const homeSyncNow = 'Read now';
const homeSetupHeading = 'Your setup';
const homeExploreHeading = 'Look around';
const homeWorkSetNeededTitle = 'Choose the apps you work in';
const homeWorkSetNeededBody =
    'Sakshi finds your study windows from the apps you pick. Pick up to 12; fewer is better.';
const homeWorkSetNeededButton = 'Choose apps';
const homeWorkSetTitle = 'Work set';
const homeWorkSetBody = 'Change the apps that count as work.';
const homeStudyHoursTitle = 'Study hours';
const homeStudyHoursBody = 'Optional. Tell Sakshi when you usually study.';
const homeNotifTitle = 'Notification access';
const homeNotifBody = 'Optional. Lets Sakshi note when an app pings, never the words.';
const homeBatteryTitle = 'Keep Sakshi reading';
const homeBatteryBody = 'A quick look at your phone\'s battery settings.';
const homeMirrorTitle = 'Your Mirror';
const homeTodayTitle = 'Today so far';
const homeLakeTitle = 'The Lake';
const homeWhatISeeTitle = 'What Sakshi sees';
const homeWhatISeeBody = 'Exactly what is read, with pause, export and delete.';
const homeDemoTitle = 'Time Machine';
const homeDemoBody = 'Try Sakshi on made-up history.';
const homeSettingsTitle = 'Settings';
const homeWidgetTitle = 'Add the Lake to your home screen';
const homeWidgetBody = 'Still water, ripples or waves: how your last study window went, in one glance.';
const homeWidgetByHand =
    'Your launcher cannot add it from here. Touch and hold an empty spot on your home screen, tap Widgets, and find Sakshi: The Lake.';
const homeErrorRetry = 'Try again';

// ---------------------------------------------------------------------------
// Demo entry points
// ---------------------------------------------------------------------------

const demoTryTitle = 'See it with sample data';
const demoTryBody =
    'Eight weeks of a made-up student, so every screen is filled in. Nothing is read from your phone.';
const demoTryButton = 'Try the demo';
const demoTryFirstButton = 'Try the demo first';
const demoActiveTitle = 'You are looking at demo data';
const demoActiveBody =
    'Aarav, a made-up student, at the end of week 8. Nothing here comes from your phone.';
const demoMoveButton = 'Move through time';
const demoExitButton = 'Exit demo';
const demoStarting = 'Setting up the demo…';
const homeUsageNeededTitle = 'Allow usage access';
const homeUsageNeededBody =
    'Sakshi reads which app is open and when, from Android\'s own record. It never reads what is inside an app.';
const homeUsageNeededButton = 'Continue setup';
