// The app must never loop through onboarding: with usage access granted every launch lands on Home, whatever else is or is not done.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:sakshi/core/providers.dart';
import 'package:sakshi/features/home/home_routes.dart';
import 'package:sakshi/features/setup/setup_routes.dart';
import 'package:sakshi/host/fake_host.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

SetupStateDto _state({
  bool usage = true,
  bool notif = false,
  bool workSet = false,
  bool studyHours = false,
  bool battery = false,
}) => SetupStateDto(
  usageAccessGranted: usage,
  notificationAccessGranted: notif,
  restrictedSettingsSuspected: false,
  workSetSaved: workSet,
  studyHoursSaved: studyHours,
  batteryHelperShown: battery,
  weeklyNoteEnabled: false,
  gentleMode: false,
  isDemo: false,
  health: CollectionHealthDto(workerRuns7d: 0, paused: false),
);

Future<GoRouter> _launch(WidgetTester tester, SetupStateDto state) async {
  await tester.binding.setSurfaceSize(const Size(400, 1000));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final router = GoRouter(
    initialLocation: '/',
    routes: [
      ...setupRoutes,
      ...homeRoutes,
      // Stand-ins so a tap has somewhere to go.
      GoRoute(
        path: '/settings',
        builder: (c, s) => const Scaffold(body: Text('settings page')),
      ),
      GoRoute(
        path: '/mirror',
        builder: (c, s) => const Scaffold(body: Text('mirror page')),
      ),
      GoRoute(
        path: '/today',
        builder: (c, s) => const Scaffold(body: Text('today page')),
      ),
      GoRoute(
        path: '/lake',
        builder: (c, s) => const Scaffold(body: Text('lake page')),
      ),
      GoRoute(
        path: '/what-i-see',
        builder: (c, s) => const Scaffold(body: Text('what page')),
      ),
      GoRoute(
        path: '/demo',
        builder: (c, s) => const Scaffold(body: Text('demo page')),
      ),
    ],
  );
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        hostClientProvider.overrideWithValue(FakeHost(initial: state)),
      ],
      child: MaterialApp.router(routerConfig: router),
    ),
  );
  await tester.pumpAndSettle();
  return router;
}

String _location(GoRouter r) =>
    r.routerDelegate.currentConfiguration.uri.toString();

void main() {
  group('the gate', () {
    testWidgets('no usage access starts the introduction', (tester) async {
      final r = await _launch(tester, _state(usage: false));
      expect(_location(r), '/setup/first');
    });

    for (final c in <String, SetupStateDto>{
      'nothing else done': _state(),
      'notification access granted, no work set': _state(notif: true),
      'work set saved': _state(workSet: true),
      'everything done': _state(
        notif: true,
        workSet: true,
        studyHours: true,
        battery: true,
      ),
    }.entries) {
      testWidgets('usage access granted lands on Home (${c.key})', (
        tester,
      ) async {
        final r = await _launch(tester, c.value);
        expect(_location(r), '/home');
        expect(find.text('साक्षी'), findsWidgets);
      });
    }
  });

  group('Home', () {
    testWidgets('greets, and offers a work set when there is none', (
      tester,
    ) async {
      await _launch(tester, _state());
      expect(
        find.byWidgetPredicate(
          (w) => w is Text && (w.data ?? '').startsWith('Good '),
        ),
        findsOneWidget,
      );
      expect(find.text('Choose the apps you work in'), findsOneWidget);
      expect(find.text('Choose apps'), findsOneWidget);
    });

    testWidgets('with a work set it offers to change it instead', (
      tester,
    ) async {
      await _launch(tester, _state(workSet: true));
      expect(find.text('Choose the apps you work in'), findsNothing);
      expect(find.text('Work set'), findsOneWidget);
    });

    testWidgets(
      'notification access and the battery note are offered, not forced',
      (tester) async {
        await _launch(tester, _state());
        await tester.scrollUntilVisible(
          find.text('Notification access'),
          200,
          scrollable: find.byType(Scrollable).first,
        );
        expect(find.text('Notification access'), findsOneWidget);
        expect(find.text('Keep Sakshi reading'), findsOneWidget);
      },
    );

    testWidgets('the Mirror card opens the Mirror', (tester) async {
      await _launch(tester, _state());
      await tester.tap(find.text('Your Mirror'));
      await tester.pumpAndSettle();
      expect(find.text('mirror page'), findsOneWidget);
    });
  });

  group('the demo', () {
    testWidgets(
      'one tap on Home starts the demo at week 8 and shows the demo bar',
      (tester) async {
        await _launch(tester, _state());
        expect(find.byKey(const Key('home_try_demo')), findsOneWidget);
        await tester.tap(find.byKey(const Key('home_try_demo_button')));
        await tester.pumpAndSettle();
        expect(find.byKey(const Key('home_demo_bar')), findsOneWidget);
        expect(find.text('You are looking at demo data'), findsOneWidget);
        // the setup rows are not shown over made-up data
        expect(find.text('Your setup'), findsNothing);
        expect(find.byKey(const Key('home_try_demo')), findsNothing);
      },
    );

    testWidgets('Exit demo goes back to the normal Home', (tester) async {
      await _launch(tester, _state());
      await tester.tap(find.byKey(const Key('home_try_demo_button')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('home_exit_demo')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('home_demo_bar')), findsNothing);
      expect(find.byKey(const Key('home_try_demo')), findsOneWidget);
    });

    testWidgets(
      'the introduction offers the demo before any permission is asked for',
      (tester) async {
        final r = await _launch(tester, _state(usage: false));
        expect(_location(r), '/setup/first');
        await tester.tap(find.byKey(const Key('first_try_demo')));
        await tester.pumpAndSettle();
        expect(_location(r), '/home');
        expect(find.byKey(const Key('home_demo_bar')), findsOneWidget);
      },
    );

    testWidgets(
      'without usage access Home asks for it instead of the work set',
      (tester) async {
        await tester.binding.setSurfaceSize(const Size(400, 900));
        final r = await _launch(tester, _state(usage: false));
        r.go('/home');
        await tester.pumpAndSettle();
        expect(find.text('Allow usage access'), findsOneWidget);
        expect(find.text('Choose the apps you work in'), findsNothing);
      },
    );
  });
}
