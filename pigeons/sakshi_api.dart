// pigeons/sakshi_api.dart — LOCKED CONTRACT LC-4. Pull-only, request/response, no streams.
// [VERIFY: option names and generic-nullability style against the Pigeon version pinned at project start; adapt mechanically.]
import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(PigeonOptions(
  dartOut: 'lib/gen/sakshi_api.g.dart',
  kotlinOut: 'android/app/src/main/kotlin/com/kleos/sakshi/host/gen/SakshiApi.g.kt',
  kotlinOptions: KotlinOptions(package: 'com.kleos.sakshi.host.gen'),
))

// ---------- enums ----------
enum UserClassDto { inSet, depends }
enum SyncStateDto { ok, partialPingAwareness, gapNotSeen, paused, noPermission }
enum DataStateDto { ok, learningBaseline, tooLittleData }
enum DataFlagDto { pingOff, partialPing, notSeen, paused, tooLittleData, unusualWeek, internalPartial, firstLook }
enum LakeStateDto { learning, noData, still, rippled, choppy }
enum GoalAnswerDto { yes, partly, notYet }

// ---------- setup ----------
class CollectionHealthDto {
  CollectionHealthDto({required this.workerRuns7d, required this.paused, this.lastWorkerRunEpochMs, this.listenerCoverage7d, this.lastError});
  int workerRuns7d; bool paused; int? lastWorkerRunEpochMs; double? listenerCoverage7d; String? lastError;
}
class SetupStateDto {
  SetupStateDto({required this.usageAccessGranted, required this.notificationAccessGranted, required this.restrictedSettingsSuspected,
      required this.workSetSaved, required this.studyHoursSaved, required this.batteryHelperShown, required this.weeklyNoteEnabled,
      required this.gentleMode, required this.isDemo, required this.health});
  bool usageAccessGranted; bool notificationAccessGranted; bool restrictedSettingsSuspected;
  bool workSetSaved; bool studyHoursSaved; bool batteryHelperShown; bool weeklyNoteEnabled; bool gentleMode; bool isDemo;
  CollectionHealthDto health;
}
class AppDto {
  AppDto({required this.pkg, required this.label, required this.suggestedInSet, required this.suggestedDepends});
  String pkg; String label; bool suggestedInSet; bool suggestedDepends;
}
class WorkSetEntryDto { WorkSetEntryDto({required this.pkg, required this.userClass}); String pkg; UserClassDto userClass; }
class SaveResultDto { SaveResultDto({required this.ok, required this.savedCount, this.userMessage}); bool ok; int savedCount; String? userMessage; }
class StudyBlockDto { StudyBlockDto({required this.startMinute, required this.endMinute}); int startMinute; int endMinute; } // minutes from local midnight; end < start crosses midnight
class StudyHoursDto { StudyHoursDto({required this.blocks, required this.learnForMe}); List<StudyBlockDto> blocks; bool learnForMe; }

// ---------- read: Mirror and friends ----------
class SyncStatusDto { SyncStatusDto({required this.state, this.lastSyncEpochMs, this.message}); SyncStateDto state; int? lastSyncEpochMs; String? message; }
class PartLinesDto {
  PartLinesDto({this.stretch, this.stays, this.ret, this.quiet});
  String? stretch; String? stays; String? ret; String? quiet;           // `ret` because `return` is reserved
}
class PartsDto {
  PartsDto({this.stretchMin, this.longestStretchMin, this.inSetShare, this.quietShare, this.staysPerHour, required this.glances, this.returnMin,
      required this.lines, required this.extrasLines});
  double? stretchMin; double? longestStretchMin; double? inSetShare; double? quietShare; double? staysPerHour; int glances; double? returnMin;
  PartLinesDto lines; List<String> extrasLines;
}
class SteadinessDto { SteadinessDto({required this.value, required this.word}); int value; String word; }     // Wavering | Steady | Steadier
class StonesDto {
  StonesDto({required this.totalStays, required this.stoneCount, required this.selfStartedCount, required this.unknownCount, this.topStoneLabel, this.noRippleRate, required this.line});
  int totalStays; int stoneCount; int selfStartedCount; int unknownCount; String? topStoneLabel; double? noRippleRate; String line;
}
class ClearHourDto { ClearHourDto({required this.startHour, required this.endHour, required this.stretchMin, required this.line}); int startHour; int endHour; double stretchMin; String line; }
class PatternLineDto { PatternLineDto({required this.kindId, required this.line, required this.evidenceWindows, required this.evidenceDays}); String kindId; String line; int evidenceWindows; int evidenceDays; }
class SuggestionDto {
  SuggestionDto({required this.kindId, required this.line, required this.actionLabel, required this.actionType, required this.opensSettings, this.subjectKey});
  String kindId; String line; String actionLabel; String actionType; bool opensSettings; String? subjectKey;
}
class ObservationDto { ObservationDto({required this.kindId, required this.line}); String kindId; String line; }
class VerdictDto { VerdictDto({required this.verdict, required this.line, required this.approxMix, this.beforeValue, this.afterValue}); String verdict; String line; bool approxMix; double? beforeValue; double? afterValue; }
class GoalTapDto { GoalTapDto({required this.offered, this.answer}); bool offered; GoalAnswerDto? answer; }
class TeacherDto { TeacherDto({required this.opensThisWeek, required this.minutesThisWeek, required this.line, this.opensPrevWeek}); int opensThisWeek; double minutesThisWeek; String line; int? opensPrevWeek; }
class SayingDto { SayingDto({required this.id, required this.text, required this.source, required this.tierLabel, required this.question}); String id; String text; String source; String tierLabel; int question; }

class MirrorDto {
  MirrorDto({required this.isDemo, required this.provisional, required this.gentle, required this.weekStartEpochMs, required this.weekLabel, required this.dataState,
      required this.dataFlags, required this.dataLines, required this.headline, required this.patterns, required this.nothingToFix, required this.goalTap, required this.reanchorOffered,
      this.parts, this.steadiness, this.stones, this.clearHour, this.suggestion, this.observation, this.verdict, this.teacher, this.lapseLine, this.saying, this.returnLine, this.suggestedStudyBlock});
  bool isDemo; bool provisional; bool gentle; int weekStartEpochMs; String weekLabel; DataStateDto dataState;
  List<DataFlagDto> dataFlags; List<String> dataLines; String headline;
  PartsDto? parts; SteadinessDto? steadiness; StonesDto? stones; ClearHourDto? clearHour;
  List<PatternLineDto> patterns; SuggestionDto? suggestion; ObservationDto? observation; bool nothingToFix;
  VerdictDto? verdict; GoalTapDto goalTap; TeacherDto? teacher; String? lapseLine; SayingDto? saying; String? returnLine;
  bool reanchorOffered; StudyBlockDto? suggestedStudyBlock;   // v1.1 CA-1
}
class WeekRefDto { WeekRefDto({required this.weekStartEpochMs, required this.label, required this.completed}); int weekStartEpochMs; String label; bool completed; }
class TodayWindowDto { TodayWindowDto({required this.startEpochMs, required this.endEpochMs, required this.stays, this.shape, this.stretchMin, this.returnMin}); int startEpochMs; int endEpochMs; int stays; String? shape; double? stretchMin; double? returnMin; }
class TodayDto { TodayDto({required this.isDemo, required this.windows, required this.line, required this.dataFlags, required this.dataLines, this.parts}); bool isDemo; List<TodayWindowDto> windows; String line; List<DataFlagDto> dataFlags; List<String> dataLines; PartsDto? parts; }
class WhatISeeDto {
  WhatISeeDto({required this.isDemo, required this.usageAccessGranted, required this.notificationAccessGranted, required this.rawEventCount, required this.notifEventCount,
      required this.derivedDays, required this.workerRuns7d, required this.paused, required this.oddEventPairs, required this.lines, this.oldestRawEpochMs, this.listenerCoverage7d, this.lastWorkerRunEpochMs, this.lastError});
  bool isDemo; bool usageAccessGranted; bool notificationAccessGranted; int rawEventCount; int notifEventCount; int derivedDays; int workerRuns7d; bool paused; int oddEventPairs;
  List<String> lines; int? oldestRawEpochMs; double? listenerCoverage7d; int? lastWorkerRunEpochMs; String? lastError;
}
class LakeDto { LakeDto({required this.state, required this.phrase, required this.isDemo, this.asOfEpochMs}); LakeStateDto state; String phrase; bool isDemo; int? asOfEpochMs; }
class ExportDto { ExportDto({required this.fileName, required this.byteSize}); String fileName; int byteSize; }

// ---------- the host API ----------
@HostApi()
abstract class SakshiHostApi {
  // setup
  @async SetupStateDto getSetupState();
  @async void openUsageAccessSettings();
  @async void openNotificationAccessSettings();
  @async void openAppInfoForRestrictedSettings();
  @async void openBatterySettings();                       // opens Android's own page; never requests the exemption
  @async bool requestLakeWidget();                         // Contract Change (31st method): asks the launcher to pin the Lake widget; false when it cannot
  @async void markBatteryHelperShown();
  @async List<AppDto> listLauncherApps();
  @async SaveResultDto saveWorkSet(List<WorkSetEntryDto> entries);
  @async void saveStudyHours(StudyHoursDto hours);
  @async void setGentleMode(bool on);
  @async void setUnder18(bool on);
  @async bool setWeeklyNote(bool enabled);                 // returns the effective state (false if the permission was denied)
  // read (all pull)
  @async SyncStatusDto syncNow();
  @async MirrorDto getMirror(int? weekStartEpochMs);       // null ⇒ latest completed week, or the First Look
  @async List<WeekRefDto> listMirrorWeeks();
  @async TodayDto getTodaySoFar();
  @async WhatISeeDto getWhatISee();
  @async List<SayingDto> getSayingChoices();
  @async LakeDto getLake();
  // write (rare, user-initiated, one tap each)
  @async void pickSaying(String sayingId);
  @async void tapTryThis(String kindId, String? subjectKey);
  @async void dismissSuggestion(String kindId, String? subjectKey);
  @async void tapGoal(GoalAnswerDto answer);
  @async void reanchorBaseline();
  @async void pause(bool on);
  @async ExportDto exportData(bool includeRaw);            // writes the file and opens the Android share sheet
  @async void deleteEverything();
  // demo
  @async void startDemo(String personaId);                 // "aarav" | "meera" | "rohan"
  @async void setDemoAsOf(int dayIndex);                   // 0..59
  @async void stopDemo();
}
