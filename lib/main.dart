import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'core/providers.dart';
import 'host/host_client.dart';

// ---------------------------------------------------------------------------
// Debug-only fake host — lets the Done-when check be run without a real
// Android host. Toggling [_debugIsDemo] to true makes the DemoBanner appear.
// This entire class is tree-shaken in release builds (kDebugMode guard).
// ---------------------------------------------------------------------------

// Toggle this to true and hot-restart to see the DemoBanner.
const bool _debugIsDemo = false;

class _DebugFakeHost implements HostClient {
  const _DebugFakeHost();

  static CollectionHealthDto get _health => CollectionHealthDto(
        workerRuns7d: 0,
        paused: false,
      );

  @override
  Future<SetupStateDto> getSetupState() async => SetupStateDto(
        usageAccessGranted: true,
        notificationAccessGranted: true,
        restrictedSettingsSuspected: false,
        workSetSaved: true,
        studyHoursSaved: true,
        batteryHelperShown: true,
        weeklyNoteEnabled: false,
        gentleMode: false,
        isDemo: _debugIsDemo,
        health: _health,
      );

  @override
  Future<void> openUsageAccessSettings() async {}
  @override
  Future<void> openNotificationAccessSettings() async {}
  @override
  Future<void> openAppInfoForRestrictedSettings() async {}
  @override
  Future<void> openBatterySettings() async {}
  @override
  Future<void> markBatteryHelperShown() async {}
  @override
  Future<List<AppDto>> listLauncherApps() async => const [];
  @override
  Future<SaveResultDto> saveWorkSet(List<WorkSetEntryDto> entries) async =>
      SaveResultDto(ok: true, savedCount: 0);
  @override
  Future<void> saveStudyHours(StudyHoursDto hours) async {}
  @override
  Future<void> setGentleMode(bool on) async {}
  @override
  Future<void> setUnder18(bool on) async {}
  @override
  Future<bool> setWeeklyNote(bool enabled) async => enabled;
  @override
  Future<SyncStatusDto> syncNow() async =>
      SyncStatusDto(state: SyncStateDto.ok);
  @override
  Future<MirrorDto> getMirror(int? weekStartEpochMs) async => MirrorDto(
        isDemo: _debugIsDemo,
        provisional: true,
        gentle: false,
        weekStartEpochMs: 0,
        weekLabel: '',
        dataState: DataStateDto.learningBaseline,
        dataFlags: const [],
        dataLines: const [],
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
        isDemo: _debugIsDemo,
        windows: const [],
        line: '',
        dataFlags: const [],
        dataLines: const [],
      );
  @override
  Future<WhatISeeDto> getWhatISee() async => WhatISeeDto(
        isDemo: _debugIsDemo,
        usageAccessGranted: true,
        notificationAccessGranted: true,
        rawEventCount: 0,
        notifEventCount: 0,
        derivedDays: 0,
        workerRuns7d: 0,
        paused: false,
        oddEventPairs: 0,
        lines: const [],
      );
  @override
  Future<List<SayingDto>> getSayingChoices() async => const [];
  @override
  Future<LakeDto> getLake() async => LakeDto(
        state: LakeStateDto.noData,
        phrase: '',
        isDemo: _debugIsDemo,
      );
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
  Future<void> pause(bool on) async {}
  @override
  Future<ExportDto> exportData(bool includeRaw) async =>
      ExportDto(fileName: 'sakshi_export.json', byteSize: 0);
  @override
  Future<void> deleteEverything() async {}
  @override
  Future<void> startDemo(String personaId) async {}
  @override
  Future<void> setDemoAsOf(int dayIndex) async {}
  @override
  Future<void> stopDemo() async {}
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

void main() {
  runApp(
    ProviderScope(
      overrides: [
        if (kDebugMode)
          hostClientProvider.overrideWithValue(const _DebugFakeHost()),
      ],
      child: const SakshiApp(),
    ),
  );
}
