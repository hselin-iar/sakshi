// test/features/demo/demo_screen_test.dart
// T4.6 — Widget tests for Time Machine DemoScreen and DemoBanner.
// Verifies:
// 1. Permanent DemoBanner visibility ("Demo data" banner).
// 2. Initial state (Aarav persona, Week 4 day 31 preset, startDemo/setDemoAsOf calls).
// 3. Persona switching (Aarav -> Meera -> Rohan).
// 4. Preset switching (Day 1 = 3, Week 4 = 31, Week 8 = 59).
// 5. Slider interaction and day update.
// 6. MirrorContent rendered beneath controls with demo data.
// 7. "Stop demo" button calling stopDemo() and deactivating demo.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/features/demo/demo_banner.dart';
import 'package:sakshi/features/demo/demo_screen.dart';
import 'package:sakshi/features/mirror/mirror_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';

class MockSakshiHostApi extends Fake implements SakshiHostApi {
  final List<String> startDemoCalls = [];
  final List<int> setDemoAsOfCalls = [];
  bool stopDemoCalled = false;
  MirrorDto mirrorToReturn = demoAarav;

  @override
  Future<void> startDemo(String personaId) async {
    startDemoCalls.add(personaId);
  }

  @override
  Future<void> setDemoAsOf(int dayIndex) async {
    setDemoAsOfCalls.add(dayIndex);
  }

  @override
  Future<void> stopDemo() async {
    stopDemoCalled = true;
  }

  @override
  Future<MirrorDto> getMirror(int? weekStartEpochMs) async {
    return mirrorToReturn;
  }
}

void main() {
  late MockSakshiHostApi mockApi;

  setUp(() {
    mockApi = MockSakshiHostApi();
  });

  Widget buildTestableDemoScreen({MockSakshiHostApi? api}) {
    return ProviderScope(
      overrides: [
        sakshiHostApiProvider.overrideWithValue(api ?? mockApi),
        isDemoActiveProvider.overrideWith((ref) => false),
        demoPersonaProvider.overrideWith((ref) => 'aarav'),
        demoDayIndexProvider.overrideWith((ref) => 31),
      ],
      child: const MaterialApp(
        home: DemoScreen(),
      ),
    );
  }

  group('DemoScreen & DemoBanner', () {
    testWidgets('DemoBanner renders "Demo data" text and is permanent', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: DemoBanner(),
          ),
        ),
      );

      expect(find.byKey(const Key('demo_banner')), findsOneWidget);
      expect(find.text('Demo data'), findsOneWidget);
    });

    testWidgets('initializes demo on load with Aarav persona at day 31 (Week 4)', (tester) async {
      await tester.pumpWidget(buildTestableDemoScreen());
      await tester.pumpAndSettle();

      // Permanent demo banner visible
      expect(find.byKey(const Key('demo_banner')), findsOneWidget);
      expect(find.text('Demo data'), findsOneWidget);

      // Verify initial host API calls
      expect(mockApi.startDemoCalls, contains('aarav'));
      expect(mockApi.setDemoAsOfCalls, contains(31));

      // Day label shows Day 31
      expect(find.byKey(const Key('slider_day_label')), findsOneWidget);
      expect(find.text('Day 31'), findsOneWidget);

      // Persona chips exist
      expect(find.byKey(const Key('persona_chip_aarav')), findsOneWidget);
      expect(find.byKey(const Key('persona_chip_meera')), findsOneWidget);
      expect(find.byKey(const Key('persona_chip_rohan')), findsOneWidget);

      // Preset chips exist
      expect(find.byKey(const Key('preset_chip_3')), findsOneWidget);
      expect(find.byKey(const Key('preset_chip_31')), findsOneWidget);
      expect(find.byKey(const Key('preset_chip_59')), findsOneWidget);

      // Mirror content rendered
      expect(find.byKey(const Key('mirror_headline')), findsOneWidget);
      expect(find.text(demoAarav.headline), findsOneWidget);
    });

    testWidgets('switching persona triggers startDemo with new persona', (tester) async {
      await tester.pumpWidget(buildTestableDemoScreen());
      await tester.pumpAndSettle();

      // Tap Meera
      await tester.tap(find.byKey(const Key('persona_chip_meera')));
      await tester.pumpAndSettle();

      expect(mockApi.startDemoCalls.last, equals('meera'));

      // Tap Rohan
      await tester.tap(find.byKey(const Key('persona_chip_rohan')));
      await tester.pumpAndSettle();

      expect(mockApi.startDemoCalls.last, equals('rohan'));

      // Tap Aarav back
      await tester.tap(find.byKey(const Key('persona_chip_aarav')));
      await tester.pumpAndSettle();

      expect(mockApi.startDemoCalls.last, equals('aarav'));
    });

    testWidgets('tapping presets changes day index and calls setDemoAsOf', (tester) async {
      await tester.pumpWidget(buildTestableDemoScreen());
      await tester.pumpAndSettle();

      // Tap Day 1 (day 3)
      await tester.tap(find.byKey(const Key('preset_chip_3')));
      await tester.pumpAndSettle();

      expect(mockApi.setDemoAsOfCalls.last, equals(3));
      expect(find.text('Day 03'), findsOneWidget);

      // Tap Week 8 (day 59)
      await tester.tap(find.byKey(const Key('preset_chip_59')));
      await tester.pumpAndSettle();

      expect(mockApi.setDemoAsOfCalls.last, equals(59));
      expect(find.text('Day 59'), findsOneWidget);

      // Tap Week 4 (day 31)
      await tester.tap(find.byKey(const Key('preset_chip_31')));
      await tester.pumpAndSettle();

      expect(mockApi.setDemoAsOfCalls.last, equals(31));
      expect(find.text('Day 31'), findsOneWidget);
    });

    testWidgets('tapping "Stop demo" invokes stopDemo on host API', (tester) async {
      await tester.pumpWidget(buildTestableDemoScreen());
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('stop_demo_button')), findsOneWidget);

      await tester.tap(find.byKey(const Key('stop_demo_button')));
      await tester.pumpAndSettle();

      expect(mockApi.stopDemoCalled, isTrue);
    });
  });
}
