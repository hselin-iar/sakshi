// test/features/mirror/mirror_screen_test.dart
// T4.3 (Prompt B) — Widget tests for MirrorContent and MirrorScreen.
// Tests the three structural rules:
// 1. nothingToFix shows "Nothing to fix this week" as a normal state.
// 2. gentle shows no Steadiness widget, no re-anchor card and no study-hours card.
// 3. partialPing shows its flag line above the stones card.
// Also tests that all 16 fixtures render without error, and tests loading and error states.

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/features/mirror/mirror_content.dart';
import 'package:sakshi/features/mirror/mirror_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';
import 'package:sakshi/core/providers.dart' show hostClientProvider;
import 'package:sakshi/host/fake_host.dart';

void main() {
  Widget wrapContent(MirrorDto mirror) {
    return MaterialApp(
      home: Scaffold(body: MirrorContent(mirror: mirror)),
    );
  }

  group('Structural Rules in MirrorContent', () {
    testWidgets(
      'Rule 1: nothingToFix shows "Nothing to fix this week" as a normal state',
      (tester) async {
        await tester.pumpWidget(wrapContent(nothingToFix));

        expect(find.byKey(const Key('nothing_to_fix_line')), findsOneWidget);
        expect(find.text('Nothing to fix this week'), findsOneWidget);
        // Ensures no suggestions or observations are shown
        expect(find.byKey(const Key('suggestion_line')), findsNothing);
      },
    );

    testWidgets(
      'Rule 2: gentle shows no Steadiness widget, no re-anchor card, and no study-hours card',
      (tester) async {
        await tester.pumpWidget(wrapContent(gentle));

        // 1. No Steadiness widget
        expect(find.byKey(const Key('steadiness_widget')), findsNothing);
        expect(find.textContaining('Steadiness:'), findsNothing);

        // 2. No re-anchor card
        expect(find.byKey(const Key('reanchor_line')), findsNothing);
        expect(find.byKey(const Key('reanchor_button')), findsNothing);

        // 3. No study-hours card
        expect(find.byKey(const Key('study_hours_line')), findsNothing);
        expect(find.byKey(const Key('study_hours_use_button')), findsNothing);

        // Gentle headline and return/stay data rendered cleanly
        expect(find.text('Your week, gently.'), findsOneWidget);
        expect(find.text('You had 14 stays this week.'), findsOneWidget);
      },
    );

    testWidgets(
      'Rule 3: partialPing shows its flag line above the stones card',
      (tester) async {
        await tester.pumpWidget(wrapContent(partialPing));

        final flagFinder = find.byKey(const Key('flag_stones_0'));
        final stonesLineFinder = find.byKey(const Key('stones_line'));

        expect(flagFinder, findsOneWidget);
        expect(stonesLineFinder, findsOneWidget);

        // Verify flag is placed above stones line visually (y coordinate of flag < y coordinate of stones)
        final flagY = tester.getTopLeft(flagFinder).dy;
        final stonesY = tester.getTopLeft(stonesLineFinder).dy;
        expect(flagY, lessThan(stonesY));

        expect(
          find.text(
            'Ping tracking was active for part of this week — some stays are listed as unknown origin.',
          ),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'steady fixture shows reanchor offer when reanchorOffered is true and not gentle',
      (tester) async {
        await tester.pumpWidget(wrapContent(steady));

        expect(find.byKey(const Key('reanchor_line')), findsOneWidget);
        expect(find.byKey(const Key('steadiness_widget')), findsOneWidget);
        expect(find.textContaining('Steady'), findsWidgets);
      },
    );

    testWidgets('steadier fixture shows suggested study hours block', (
      tester,
    ) async {
      await tester.pumpWidget(wrapContent(steadier));

      expect(find.byKey(const Key('study_hours_line')), findsOneWidget);
      expect(find.textContaining('09:00–12:00'), findsOneWidget);
    });

    testWidgets(
      'withVerdict fixture renders verdict card and comparison values',
      (tester) async {
        await tester.pumpWidget(wrapContent(withVerdict));

        expect(find.byKey(const Key('verdict_line')), findsOneWidget);
        expect(
          find.textContaining('This is correlation, not cause.'),
          findsOneWidget,
        );
      },
    );

    testWidgets(
      'wavering fixture renders suggestion card with action and dismiss buttons',
      (tester) async {
        await tester.pumpWidget(wrapContent(wavering));

        expect(find.byKey(const Key('suggestion_line')), findsOneWidget);
        expect(
          find.byKey(const Key('suggestion_action_button')),
          findsOneWidget,
        );
        expect(
          find.byKey(const Key('suggestion_dismiss_button')),
          findsOneWidget,
        );
      },
    );
  });

  group('All 16 Mirror Fixtures Render without Error', () {
    final fixtures = <String, MirrorDto>{
      'firstLook': firstLook,
      'learning': learning,
      'steady': steady,
      'steadier': steadier,
      'wavering': wavering,
      'gentle': gentle,
      'partialPing': partialPing,
      'pingOff': pingOff,
      'notSeen': notSeen,
      'tooLittle': tooLittle,
      'unusualWeek': unusualWeek,
      'nothingToFix': nothingToFix,
      'withSuggestion': withSuggestion,
      'withVerdict': withVerdict,
      'afterLapse': afterLapse,
      'demoAarav': demoAarav,
    };

    for (final entry in fixtures.entries) {
      testWidgets('renders fixture ${entry.key}', (tester) async {
        await tester.pumpWidget(wrapContent(entry.value));
        expect(tester.takeException(), isNull);
        expect(find.byKey(const Key('mirror_headline')), findsOneWidget);
      });
    }
  });

  group('MirrorScreen States', () {
    testWidgets('renders loading state when provider is loading', (
      tester,
    ) async {
      final completer = Completer<MirrorDto>();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            hostClientProvider.overrideWithValue(FakeHost()),
            mirrorProvider(null).overrideWith((ref) => completer.future),
            mirrorWeeksProvider.overrideWith(
              (ref) => Future<List<WeekRefDto>>.value([]),
            ),
          ],
          child: const MaterialApp(home: MirrorScreen()),
        ),
      );

      expect(find.byKey(const Key('mirror_loading_indicator')), findsOneWidget);

      completer.complete(steady);
      await tester.pump();
    });

    testWidgets('renders error state and retry button when provider errors', (
      tester,
    ) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            hostClientProvider.overrideWithValue(FakeHost()),
            mirrorProvider(null).overrideWith(
              (ref) => Future<MirrorDto>.error(Exception('Connection failure')),
            ),
            mirrorWeeksProvider.overrideWith(
              (ref) => Future<List<WeekRefDto>>.value([]),
            ),
          ],
          child: const MaterialApp(home: MirrorScreen()),
        ),
      );
      await tester.pump();

      expect(find.byKey(const Key('mirror_error_view')), findsOneWidget);
      expect(find.byKey(const Key('mirror_error_message')), findsOneWidget);
      expect(find.text("Could not read Sakshi's data"), findsOneWidget);
      expect(find.byKey(const Key('mirror_retry_button')), findsOneWidget);
    });

    testWidgets('renders data state with week picker dropdown', (tester) async {
      final weekRefs = [
        WeekRefDto(weekStartEpochMs: 1000, label: '5–11 Oct', completed: true),
        WeekRefDto(weekStartEpochMs: 2000, label: '12–18 Oct', completed: true),
      ];

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            hostClientProvider.overrideWithValue(FakeHost()),
            mirrorProvider(null)
                .overrideWith((ref) => Future<MirrorDto>.value(steady)),
            mirrorWeeksProvider.overrideWith(
              (ref) => Future<List<WeekRefDto>>.value(weekRefs),
            ),
          ],
          child: const MaterialApp(home: MirrorScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('mirror_headline')), findsOneWidget);
      expect(find.byKey(const Key('week_picker_dropdown')), findsOneWidget);
    });
  });
}
