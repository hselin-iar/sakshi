import 'host_client.dart';
import 'fixtures/mirror_fixtures.dart' as mf;
import 'fixtures/setup_fixtures.dart';

// ---------------------------------------------------------------------------
// FakeHost — implements HostClient with in-memory state.
//
// Used in debug builds and widget tests in place of PigeonHostClient.
// State machine: tracks setup progress, gentle mode and demo flag.
// Mirror/Today/Lake/WhatISee/Saying calls serve Track 4's mirror_fixtures.dart,
// picked from the fake state (demo, gentle, setup finished or not).
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
  Future<AskReplyDto> askSakshi(String question, List<AskTurnDto> history) async => AskReplyDto(
    text:
        'Here is how the week looked. You held 14-minute stretches and 4-minute returns; steadier than your starting normal. '
        'This is a preview with made-up numbers.',
    source: 'OFFLINE',
    isDemo: true,
    quote: 'I am watching my mind act.',
    quoteSource: 'Complete Works, Vol. 8',
  );

  @override
  Future<bool> requestLakeWidget() async => true;

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

  /// A typical Mirror once setup is done; the demo, gentle and first-look variants follow the fake state.
  @override
  Future<MirrorDto> getMirror(int? weekStartEpochMs) async {
    if (_state.isDemo) return mf.demoAarav;
    if (_state.gentleMode) return mf.gentle;
    if (!_state.workSetSaved) return mf.firstLook;
    return mf.steadier;
  }

  @override
  Future<List<WeekRefDto>> listMirrorWeeks() async => [
    WeekRefDto(
      weekStartEpochMs: 1759603800000,
      label: '5–11 Oct',
      completed: true,
    ),
    WeekRefDto(
      weekStartEpochMs: 1759603800000 - 7 * 86400000,
      label: '28 Sep–4 Oct',
      completed: true,
    ),
  ];

  @override
  Future<TodayDto> getTodaySoFar() async =>
      _state.isDemo ? mf.todayDemo : mf.todayTypical;

  @override
  Future<WhatISeeDto> getWhatISee() async {
    if (_state.isDemo) return mf.whatISeeDemo;
    return _state.notificationAccessGranted
        ? mf.whatISeeTypical
        : mf.whatISeeNoNotif;
  }

  @override
  Future<List<SayingDto>> getSayingChoices() async => mf.sayingChoices;

  @override
  Future<LakeDto> getLake() async {
    if (_state.isDemo) {
      return LakeDto(
        state: mf.lakeStill.state,
        phrase: '${mf.lakeStill.phrase} (demo)',
        isDemo: true,
        asOfEpochMs: mf.lakeStill.asOfEpochMs,
      );
    }
    return _state.workSetSaved ? mf.lakeRippled : mf.lakeLearning;
  }

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
