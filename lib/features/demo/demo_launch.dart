import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/providers.dart';
import '../lake/lake_screen.dart';
import '../mirror/mirror_screen.dart';
import '../today/today_screen.dart';
import 'demo_screen.dart';

// One tap into a populated demo, and one tap out. Shared by Home, the intro screen and the Time Machine screen so every screen that
// reads the host refreshes the same way.

/// The end of week 8: the Mirror has Steadiness, patterns, a verdict and a saying, and Today and the Lake have windows to show.
const demoShowcaseDay = 59;

void _refreshEverything(WidgetRef ref) {
  ref.invalidate(setupStateProvider);
  ref.invalidate(lakeProvider);
  ref.invalidate(todayProvider);
  ref.invalidate(mirrorProvider);
  ref.invalidate(mirrorWeeksProvider);
  ref.invalidate(demoMirrorProvider);
}

Future<void> enterDemo(
  WidgetRef ref, {
  String persona = 'aarav',
  int day = demoShowcaseDay,
}) async {
  final host = ref.read(sakshiHostApiProvider);
  await host.startDemo(persona);
  await host.setDemoAsOf(day);
  ref.read(demoPersonaProvider.notifier).state = persona;
  ref.read(demoDayIndexProvider.notifier).state = day;
  ref.read(isDemoActiveProvider.notifier).state = true;
  _refreshEverything(ref);
}

Future<void> exitDemo(WidgetRef ref) async {
  try {
    await ref.read(sakshiHostApiProvider).stopDemo();
  } catch (_) {
    // The demo set is gone either way; the screens re-read below.
  }
  ref.read(isDemoActiveProvider.notifier).state = false;
  _refreshEverything(ref);
}
