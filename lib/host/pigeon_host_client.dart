import 'host_client.dart';

// ---------------------------------------------------------------------------
// PigeonHostClient — wraps the generated SakshiHostApi (lib/gen/sakshi_api.g.dart).
//
// This file compiles in isolation until Pigeon generation lands (T1.2).
// When lib/gen/sakshi_api.g.dart exists, replace the TODO body with real
// Pigeon calls and remove the UnimplementedError stubs.
//
// Error mapping rule: native FlutterError(code, message, detail) →
//   HostException(code, message). The detail string is logged locally only.
// ---------------------------------------------------------------------------

// ignore: avoid_classes_with_only_static_members — intentional wrapper pattern

class PigeonHostClient implements HostClient {
  const PigeonHostClient();

  // [REFACTOR CANDIDATE: generate the mapping from the Pigeon file when T1.2 lands]

  @override
  Future<SetupStateDto> getSetupState() {
    // TODO(T1.2): return SakshiHostApi().getSetupState();
    throw UnimplementedError('PigeonHostClient: awaiting Pigeon generation (T1.2)');
  }

  @override
  Future<void> openUsageAccessSettings() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> openNotificationAccessSettings() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> openAppInfoForRestrictedSettings() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> openBatterySettings() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> markBatteryHelperShown() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<List<AppDto>> listLauncherApps() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<SaveResultDto> saveWorkSet(List<WorkSetEntryDto> entries) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> saveStudyHours(StudyHoursDto hours) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> setGentleMode(bool on) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> setUnder18(bool on) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<bool> setWeeklyNote(bool enabled) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<SyncStatusDto> syncNow() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<MirrorDto> getMirror(int? weekStartEpochMs) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<List<WeekRefDto>> listMirrorWeeks() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<TodayDto> getTodaySoFar() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<WhatISeeDto> getWhatISee() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<List<SayingDto>> getSayingChoices() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<LakeDto> getLake() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> pickSaying(String sayingId) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> tapTryThis(String kindId, String? subjectKey) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> dismissSuggestion(String kindId, String? subjectKey) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> tapGoal(GoalAnswerDto answer) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> reanchorBaseline() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> pause(bool on) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<ExportDto> exportData(bool includeRaw) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> deleteEverything() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> startDemo(String personaId) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> setDemoAsOf(int dayIndex) {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }

  @override
  Future<void> stopDemo() {
    throw UnimplementedError('PigeonHostClient: awaiting T1.2');
  }
}
