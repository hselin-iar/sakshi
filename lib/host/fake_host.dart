import 'host_client.dart';
import 'fixtures/setup_fixtures.dart';

// ---------------------------------------------------------------------------
// FakeHost — implements HostClient with in-memory state.
//
// Used in debug builds and widget tests in place of PigeonHostClient.
// State machine: tracks setup progress, gentle mode and demo flag.
// Mirror/Today/Lake/WhatISee/Saying calls return placeholder DTOs until
// Track 4 provides mirror_fixtures.dart (T4.1).
//
// Rules:
//   - syncNow() waits 400 ms (simulates a real sync round trip).
//   - Never throw; return ok/placeholder DTOs for every method.
//   - Do NOT change HostClient or any DTO field to make this easier.
// ---------------------------------------------------------------------------

class FakeHost implements HostClient {
  FakeHost({SetupStateDto? initial}) {
    _state = initial ?? SetupFixtures.nothingGranted;
  }

  late SetupStateDto _state;

  // ---- setup ---------------------------------------------------------------

  @override
  Future<SetupStateDto> getSetupState() async => _state;

  @override
  Future<void> openUsageAccessSettings() async {
    // In a real device this opens Android settings. Fake does nothing.
  }

  @override
  Future<void> openNotificationAccessSettings() async {}

  @override
  Future<void> openAppInfoForRestrictedSettings() async {}

  @override
  Future<void> openBatterySettings() async {}

  @override
  Future<void> markBatteryHelperShown() async {
    _state = SetupStateDto(
      usageAccessGranted: _state.usageAccessGranted,
      notificationAccessGranted: _state.notificationAccessGranted,
      restrictedSettingsSuspected: _state.restrictedSettingsSuspected,
      workSetSaved: _state.workSetSaved,
      studyHoursSaved: _state.studyHoursSaved,
      batteryHelperShown: true,
      weeklyNoteEnabled: _state.weeklyNoteEnabled,
      gentleMode: _state.gentleMode,
      isDemo: _state.isDemo,
      health: _state.health,
    );
  }

  @override
  Future<List<AppDto>> listLauncherApps() async => SetupFixtures.appList;

  @override
  Future<SaveResultDto> saveWorkSet(List<WorkSetEntryDto> entries) async {
    if (entries.length > 12) {
      return SaveResultDto(
        ok: false,
        savedCount: 0,
        userMessage: 'Your work set is limited to 12 apps.',
      );
    }
    _state = SetupStateDto(
      usageAccessGranted: _state.usageAccessGranted,
      notificationAccessGranted: _state.notificationAccessGranted,
      restrictedSettingsSuspected: _state.restrictedSettingsSuspected,
      workSetSaved: true,
      studyHoursSaved: _state.studyHoursSaved,
      batteryHelperShown: _state.batteryHelperShown,
      weeklyNoteEnabled: _state.weeklyNoteEnabled,
      gentleMode: _state.gentleMode,
      isDemo: _state.isDemo,
      health: _state.health,
    );
    return SaveResultDto(ok: true, savedCount: entries.length);
  }

  @override
  Future<void> saveStudyHours(StudyHoursDto hours) async {
    _state = SetupStateDto(
      usageAccessGranted: _state.usageAccessGranted,
      notificationAccessGranted: _state.notificationAccessGranted,
      restrictedSettingsSuspected: _state.restrictedSettingsSuspected,
      workSetSaved: _state.workSetSaved,
      studyHoursSaved: true,
      batteryHelperShown: _state.batteryHelperShown,
      weeklyNoteEnabled: _state.weeklyNoteEnabled,
      gentleMode: _state.gentleMode,
      isDemo: _state.isDemo,
      health: _state.health,
    );
  }

  @override
  Future<void> setGentleMode(bool on) async {
    _state = SetupStateDto(
      usageAccessGranted: _state.usageAccessGranted,
      notificationAccessGranted: _state.notificationAccessGranted,
      restrictedSettingsSuspected: _state.restrictedSettingsSuspected,
      workSetSaved: _state.workSetSaved,
      studyHoursSaved: _state.studyHoursSaved,
      batteryHelperShown: _state.batteryHelperShown,
      weeklyNoteEnabled: _state.weeklyNoteEnabled,
      gentleMode: on,
      isDemo: _state.isDemo,
      health: _state.health,
    );
  }

  @override
  Future<void> setUnder18(bool on) async {}

  @override
  Future<bool> setWeeklyNote(bool enabled) async {
    _state = SetupStateDto(
      usageAccessGranted: _state.usageAccessGranted,
      notificationAccessGranted: _state.notificationAccessGranted,
      restrictedSettingsSuspected: _state.restrictedSettingsSuspected,
      workSetSaved: _state.workSetSaved,
      studyHoursSaved: _state.studyHoursSaved,
      batteryHelperShown: _state.batteryHelperShown,
      weeklyNoteEnabled: enabled,
      gentleMode: _state.gentleMode,
      isDemo: _state.isDemo,
      health: _state.health,
    );
    return enabled;
  }

  // ---- read ----------------------------------------------------------------

  @override
  Future<SyncStatusDto> syncNow() async {
    await Future<void>.delayed(const Duration(milliseconds: 400));
    return SyncStatusDto(state: SyncStateDto.ok);
  }

  /// Returns a placeholder MirrorDto until Track 4 provides mirror_fixtures.
  @override
  Future<MirrorDto> getMirror(int? weekStartEpochMs) async => MirrorDto(
    isDemo: _state.isDemo,
    provisional: true,
    gentle: _state.gentleMode,
    weekStartEpochMs: weekStartEpochMs ?? 0,
    weekLabel: 'Placeholder week',
    dataState: DataStateDto.learningBaseline,
    dataFlags: const [],
    dataLines: const ['Waiting for Track 4 mirror fixtures'],
    headline: '',
    patterns: const [],
    nothingToFix: true,
    goalTap: GoalTapDto(offered: false),
    reanchorOffered: false,
  );

  @override
  Future<List<WeekRefDto>> listMirrorWeeks() async => const [];

  @override
  Future<TodayDto> getTodaySoFar() async => TodayDto(
    isDemo: _state.isDemo,
    windows: const [],
    line: '',
    dataFlags: const [],
    dataLines: const [],
  );

  @override
  Future<WhatISeeDto> getWhatISee() async => WhatISeeDto(
    isDemo: _state.isDemo,
    usageAccessGranted: _state.usageAccessGranted,
    notificationAccessGranted: _state.notificationAccessGranted,
    rawEventCount: 0,
    notifEventCount: 0,
    derivedDays: 0,
    workerRuns7d: _state.health.workerRuns7d,
    paused: _state.health.paused,
    oddEventPairs: 0,
    lines: const [],
  );

  @override
  Future<List<SayingDto>> getSayingChoices() async => const [];

  @override
  Future<LakeDto> getLake() async =>
      LakeDto(state: LakeStateDto.noData, phrase: '', isDemo: _state.isDemo);

  // ---- write ---------------------------------------------------------------

  @override
  Future<void> pickSaying(String sayingId) async {}

  @override
  Future<void> tapTryThis(String kindId, String? subjectKey) async {}

  @override
  Future<void> dismissSuggestion(String kindId, String? subjectKey) async {}

  @override
  Future<void> tapGoal(GoalAnswerDto answer) async {}

  @override
  Future<void> reanchorBaseline() async {}

  @override
  Future<void> pause(bool on) async {
    _state = SetupStateDto(
      usageAccessGranted: _state.usageAccessGranted,
      notificationAccessGranted: _state.notificationAccessGranted,
      restrictedSettingsSuspected: _state.restrictedSettingsSuspected,
      workSetSaved: _state.workSetSaved,
      studyHoursSaved: _state.studyHoursSaved,
      batteryHelperShown: _state.batteryHelperShown,
      weeklyNoteEnabled: _state.weeklyNoteEnabled,
      gentleMode: _state.gentleMode,
      isDemo: _state.isDemo,
      health: CollectionHealthDto(
        workerRuns7d: _state.health.workerRuns7d,
        paused: on,
        lastWorkerRunEpochMs: _state.health.lastWorkerRunEpochMs,
        listenerCoverage7d: _state.health.listenerCoverage7d,
        lastError: _state.health.lastError,
      ),
    );
  }

  @override
  Future<ExportDto> exportData(bool includeRaw) async =>
      ExportDto(fileName: 'sakshi_export.json', byteSize: 0);

  @override
  Future<void> deleteEverything() async {
    _state = SetupFixtures.nothingGranted;
  }

  // ---- demo ----------------------------------------------------------------

  @override
  Future<void> startDemo(String personaId) async {
    _state = SetupFixtures.demoActive;
  }

  @override
  Future<void> setDemoAsOf(int dayIndex) async {}

  @override
  Future<void> stopDemo() async {
    _state = SetupFixtures.allDone;
  }
}
