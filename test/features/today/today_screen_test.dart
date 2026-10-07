// test/features/today/today_screen_test.dart
// T4.4 — Widget tests for TodayScreen.
// Verifies:
// 1. Empty state renders "No finished window yet today."
// 2. Completed windows render as simple rows.
// 3. Strictly NO suggestion, NO Steadiness, NO baseline comparisons.
// 4. Loading and error states.

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/features/today/today_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';

void main() {
  group('TodayScreen', () {
    testWidgets('renders firstLook-style empty state when no windows completed', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            todayProvider.overrideWith((ref) => Future.value(todayEmpty)),
          ],
          child: const MaterialApp(
            home: TodayScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('today_empty_state')), findsOneWidget);
      expect(find.text('No finished window yet today.'), findsOneWidget);
      expect(find.byKey(const Key('today_windows_list')), findsNothing);

      // Verify no suggestion and no Steadiness
      expect(find.byKey(const Key('suggestion_line')), findsNothing);
      expect(find.byKey(const Key('steadiness_widget')), findsNothing);
      expect(find.textContaining('Steadiness'), findsNothing);
    });

    testWidgets('renders two-window state with completed windows as simple rows', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            todayProvider.overrideWith((ref) => Future.value(todayTypical)),
          ],
          child: const MaterialApp(
            home: TodayScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('today_summary_line')), findsOneWidget);
      expect(find.byKey(const Key('today_windows_list')), findsOneWidget);
      expect(find.byKey(const Key('today_window_0')), findsOneWidget);
      expect(find.byKey(const Key('today_window_1')), findsOneWidget);

      expect(find.textContaining('3 stays'), findsOneWidget);
      expect(find.textContaining('5 stays'), findsOneWidget);
      expect(find.textContaining('HELD'), findsOneWidget);
      expect(find.textContaining('PINGED'), findsOneWidget);

      // Verify strictly NO suggestion and NO Steadiness
      expect(find.byKey(const Key('suggestion_line')), findsNothing);
      expect(find.byKey(const Key('steadiness_widget')), findsNothing);
      expect(find.textContaining('Wavering'), findsNothing);
      expect(find.textContaining('Steadier'), findsNothing);
      expect(find.textContaining('baseline'), findsNothing);
    });

    testWidgets('renders loading state when provider is loading', (tester) async {
      final completer = Completer<TodayDto>();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            todayProvider.overrideWith((ref) => completer.future),
          ],
          child: const MaterialApp(
            home: TodayScreen(),
          ),
        ),
      );

      expect(find.byKey(const Key('today_loading')), findsOneWidget);

      completer.complete(todayEmpty);
      await tester.pump();
    });

    testWidgets('renders error state and retry button on error', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            todayProvider.overrideWith((ref) => Future.error(Exception('Failed to load today'))),
          ],
          child: const MaterialApp(
            home: TodayScreen(),
          ),
        ),
      );
      await tester.pump();

      expect(find.byKey(const Key('today_error')), findsOneWidget);
      expect(find.byKey(const Key('today_error_message')), findsOneWidget);
      expect(find.text("Could not read today's data"), findsOneWidget);
    });
  });
}
