import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show PlatformException;

import '../gen/sakshi_api.g.dart';

/// T1.9 debug screen: one button per Pigeon method, so every call can be tried on a real phone.
/// Not product UI. Track 3 replaces main.dart; this file can then be dropped or kept behind a debug flag.
class HostDebugScreen extends StatefulWidget {
  const HostDebugScreen({super.key});

  @override
  State<HostDebugScreen> createState() => _HostDebugScreenState();
}

class _HostDebugScreenState extends State<HostDebugScreen> {
  final SakshiHostApi _api = SakshiHostApi();
  final List<String> _log = [];

  Future<void> _run(String name, Future<Object?> Function() call) async {
    String result;
    try {
      final value = await call();
      result = value == null ? 'ok' : _short(value);
    } on PlatformException catch (e) {
      result = 'ERROR ${e.code}: ${e.message}';
    } catch (e) {
      result = 'ERROR $e';
    }
    setState(() => _log.insert(0, '$name -> $result'));
  }

  String _short(Object v) {
    final s = v.toString();
    return s.length > 160 ? '${s.substring(0, 160)}…' : s;
  }

  @override
  Widget build(BuildContext context) {
    final calls = <String, Future<Object?> Function()>{
      'getSetupState': () => _api.getSetupState(),
      'openUsageAccessSettings': () => _api.openUsageAccessSettings(),
      'openNotificationAccessSettings': () =>
          _api.openNotificationAccessSettings(),
      'openAppInfoForRestrictedSettings': () =>
          _api.openAppInfoForRestrictedSettings(),
      'openBatterySettings': () => _api.openBatterySettings(),
      'markBatteryHelperShown': () => _api.markBatteryHelperShown(),
      'listLauncherApps': () async =>
          (await _api.listLauncherApps()).take(10).map((a) => a.label).toList(),
      'saveWorkSet': () => _api.saveWorkSet([]),
      'saveStudyHours': () =>
          _api.saveStudyHours(StudyHoursDto(blocks: [], learnForMe: false)),
      'setGentleMode(false)': () => _api.setGentleMode(false),
      'setUnder18(false)': () => _api.setUnder18(false),
      'setWeeklyNote(false)': () => _api.setWeeklyNote(false),
      'syncNow': () => _api.syncNow(),
      'getMirror': () => _api.getMirror(null),
      'listMirrorWeeks': () => _api.listMirrorWeeks(),
      'getTodaySoFar': () => _api.getTodaySoFar(),
      'getWhatISee': () => _api.getWhatISee(),
      'getSayingChoices': () => _api.getSayingChoices(),
      'getLake': () => _api.getLake(),
      'pickSaying(sy01)': () => _api.pickSaying('sy01'),
      'tapTryThis(S2)': () => _api.tapTryThis('S2', null),
      'dismissSuggestion(S2)': () => _api.dismissSuggestion('S2', null),
      'tapGoal(yes)': () => _api.tapGoal(GoalAnswerDto.yes),
      'reanchorBaseline': () => _api.reanchorBaseline(),
      'pause(false)': () => _api.pause(false),
      'exportData': () => _api.exportData(false),
      'DELETE EVERYTHING': () => _api.deleteEverything(),
      'startDemo(aarav)': () => _api.startDemo('aarav'),
      'setDemoAsOf(3)': () => _api.setDemoAsOf(3),
      'stopDemo': () => _api.stopDemo(),
    };
    return Scaffold(
      appBar: AppBar(title: const Text('Host debug (T1.9)')),
      body: Column(
        children: [
          Wrap(
            spacing: 6,
            children: [
              for (final e in calls.entries)
                OutlinedButton(
                  onPressed: () => _run(e.key, e.value),
                  child: Text(e.key),
                ),
            ],
          ),
          const Divider(),
          Expanded(
            child: ListView(
              children: [
                for (final line in _log)
                  Padding(padding: const EdgeInsets.all(4), child: Text(line)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
