import 'package:flutter/services.dart' show PlatformException;

import 'host_client.dart';

// ---------------------------------------------------------------------------
// PigeonHostClient: the real HostClient. It wraps the generated SakshiHostApi (lib/gen/sakshi_api.g.dart) and nothing else.
//
// Error mapping: a native FlutterError(code, message, details) arrives as a PlatformException and becomes
//   HostException(code, message). The details string is a developer note; it is never shown or logged here.
// Not-granted, partial and not-seen are values inside the DTOs, never exceptions (DOC 2 §2.4.2).
//
// [REFACTOR CANDIDATE: each method below is the same one-line delegation; generate it from the Pigeon file if it ever grows.]
// ---------------------------------------------------------------------------

class PigeonHostClient implements HostClient {
  PigeonHostClient([SakshiHostApi? api]) : _api = api ?? SakshiHostApi();

  final SakshiHostApi _api;

  Future<T> _call<T>(Future<T> Function() call) async {
    try {
      return await call();
    } on PlatformException catch (e) {
      throw HostException(e.code, e.message ?? '');
    }
  }

  @override
  Future<SetupStateDto> getSetupState() => _call(() => _api.getSetupState());

  @override
  Future<void> openUsageAccessSettings() =>
      _call(() => _api.openUsageAccessSettings());

  @override
  Future<void> openNotificationAccessSettings() =>
      _call(() => _api.openNotificationAccessSettings());

  @override
  Future<void> openAppInfoForRestrictedSettings() =>
      _call(() => _api.openAppInfoForRestrictedSettings());

  @override
  Future<void> openBatterySettings() => _call(() => _api.openBatterySettings());

  @override
  Future<AskReplyDto> askSakshi(String question, List<AskTurnDto> history) => _call(() => _api.askSakshi(question, history));

  @override
  Future<bool> requestLakeWidget() => _call(() => _api.requestLakeWidget());

  @override
  Future<void> markBatteryHelperShown() =>
      _call(() => _api.markBatteryHelperShown());

  @override
  Future<List<AppDto>> listLauncherApps() =>
      _call(() => _api.listLauncherApps());

  @override
  Future<SaveResultDto> saveWorkSet(List<WorkSetEntryDto> entries) =>
      _call(() => _api.saveWorkSet(entries));

  @override
  Future<void> saveStudyHours(StudyHoursDto hours) =>
      _call(() => _api.saveStudyHours(hours));

  @override
  Future<void> setGentleMode(bool on) => _call(() => _api.setGentleMode(on));

  @override
  Future<void> setUnder18(bool on) => _call(() => _api.setUnder18(on));

  @override
  Future<bool> setWeeklyNote(bool enabled) =>
      _call(() => _api.setWeeklyNote(enabled));

  @override
  Future<SyncStatusDto> syncNow() => _call(() => _api.syncNow());

  @override
  Future<MirrorDto> getMirror(int? weekStartEpochMs) =>
      _call(() => _api.getMirror(weekStartEpochMs));

  @override
  Future<List<WeekRefDto>> listMirrorWeeks() =>
      _call(() => _api.listMirrorWeeks());

  @override
  Future<TodayDto> getTodaySoFar() => _call(() => _api.getTodaySoFar());

  @override
  Future<WhatISeeDto> getWhatISee() => _call(() => _api.getWhatISee());

  @override
  Future<List<SayingDto>> getSayingChoices() =>
      _call(() => _api.getSayingChoices());

  @override
  Future<LakeDto> getLake() => _call(() => _api.getLake());

  @override
  Future<void> pickSaying(String sayingId) =>
      _call(() => _api.pickSaying(sayingId));

  @override
  Future<void> tapTryThis(String kindId, String? subjectKey) =>
      _call(() => _api.tapTryThis(kindId, subjectKey));

  @override
  Future<void> dismissSuggestion(String kindId, String? subjectKey) =>
      _call(() => _api.dismissSuggestion(kindId, subjectKey));

  @override
  Future<void> tapGoal(GoalAnswerDto answer) =>
      _call(() => _api.tapGoal(answer));

  @override
  Future<void> reanchorBaseline() => _call(() => _api.reanchorBaseline());

  @override
  Future<void> pause(bool on) => _call(() => _api.pause(on));

  @override
  Future<ExportDto> exportData(bool includeRaw) =>
      _call(() => _api.exportData(includeRaw));

  @override
  Future<void> deleteEverything() => _call(() => _api.deleteEverything());

  @override
  Future<void> startDemo(String personaId) =>
      _call(() => _api.startDemo(personaId));

  @override
  Future<void> setDemoAsOf(int dayIndex) =>
      _call(() => _api.setDemoAsOf(dayIndex));

  @override
  Future<void> stopDemo() => _call(() => _api.stopDemo());
}
