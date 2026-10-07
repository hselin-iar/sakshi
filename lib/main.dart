import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'app.dart';
import 'core/providers.dart';
import 'host/fake_host.dart';
import 'host/fixtures/setup_fixtures.dart';
import 'host/host_client.dart';
import 'host/pigeon_host_client.dart';

// ---------------------------------------------------------------------------
// Which host runs the app (the Sync 8 swap).
//
//  - On a phone, in every build mode (debug, profile, release): the real Android host, through Pigeon.
//  - In a browser, where no Android host exists, or when asked with --dart-define=SAKSHI_FAKE_HOST=true:
//    the in-memory FakeHost over the fixtures, so every screen can be looked at without a phone.
//    Start it in a given state with --dart-define=SAKSHI_FAKE_STATE=fresh|usage|both|restricted|done|demo (default: fresh).
// ---------------------------------------------------------------------------

const bool _forceFake = bool.fromEnvironment('SAKSHI_FAKE_HOST');
const String _fakeState = String.fromEnvironment(
  'SAKSHI_FAKE_STATE',
  defaultValue: 'fresh',
);

HostClient _chooseHost() {
  if (!kIsWeb && !_forceFake) return PigeonHostClient();
  return FakeHost(
    initial: switch (_fakeState) {
      'usage' => SetupFixtures.usageGranted,
      'both' => SetupFixtures.bothGranted,
      'restricted' => SetupFixtures.restrictedSuspected,
      'done' => SetupFixtures.allDone,
      'demo' => SetupFixtures.demoActive,
      _ => SetupFixtures.nothingGranted,
    },
  );
}

void main() {
  runApp(
    ProviderScope(
      overrides: [hostClientProvider.overrideWithValue(_chooseHost())],
      child: const SakshiApp(),
    ),
  );
}
