import '../gen/sakshi_api.g.dart';

// The DTOs and enums are the Pigeon-generated ones (T1.2). Re-exported so screens and fixtures keep importing host_client.dart.
export '../gen/sakshi_api.g.dart';

/// Error codes carried by [HostException.code] (LC-4).
const hostErrorCodes = <String>[
  'NO_PERMISSION',
  'DEMO_ACTIVE',
  'BAD_REQUEST',
  'STALE_SUGGESTION',
  'REANCHOR_NOT_ALLOWED',
  'EXPORT_FAILED',
  'INTERNAL',
];

class HostException implements Exception {
  const HostException(this.code, this.userMessage);
  final String code;
  final String userMessage;

  @override
  String toString() => 'HostException($code): $userMessage';
}

/// One abstract method per Pigeon method, same names, parameter and return types (LC-4).
/// PigeonHostClient and FakeHost both `implements HostClient`.
abstract class HostClient {
  Future<SetupStateDto> getSetupState();
  Future<void> openUsageAccessSettings();
  Future<void> openNotificationAccessSettings();
  Future<void> openAppInfoForRestrictedSettings();
  Future<void> openBatterySettings();

  /// Asks the launcher to pin the Lake widget to the home screen. False when the launcher cannot.
  Future<bool> requestLakeWidget();
  Future<void> markBatteryHelperShown();
  Future<List<AppDto>> listLauncherApps();
  Future<SaveResultDto> saveWorkSet(List<WorkSetEntryDto> entries);
  Future<void> saveStudyHours(StudyHoursDto hours);
  Future<void> setGentleMode(bool on);
  Future<void> setUnder18(bool on);
  Future<bool> setWeeklyNote(bool enabled);
  Future<SyncStatusDto> syncNow();
  Future<MirrorDto> getMirror(int? weekStartEpochMs);
  Future<List<WeekRefDto>> listMirrorWeeks();
  Future<TodayDto> getTodaySoFar();
  Future<WhatISeeDto> getWhatISee();
  Future<List<SayingDto>> getSayingChoices();
  Future<LakeDto> getLake();
  Future<void> pickSaying(String sayingId);
  Future<void> tapTryThis(String kindId, String? subjectKey);
  Future<void> dismissSuggestion(String kindId, String? subjectKey);
  Future<void> tapGoal(GoalAnswerDto answer);
  Future<void> reanchorBaseline();
  Future<void> pause(bool on);
  Future<ExportDto> exportData(bool includeRaw);
  Future<void> deleteEverything();
  Future<void> startDemo(String personaId);
  Future<void> setDemoAsOf(int dayIndex);
  Future<void> stopDemo();
}
