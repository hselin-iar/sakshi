// test/features/shelf/saying_shelf_test.dart
// T4.5 — Widget tests for Saying Shelf (SayingPicker, SayingFooter, SayingPickerScreen).
// Verifies:
// 1. Three tier labels display correctly (A: 'His own writing or letter', C: 'Reported by others', D: 'Type not resolved in the Outcome Map').
// 2. A tap calls pickSaying with the chosen saying's id.
// 3. "Not now" action is present.
// 4. An empty list renders nothing.
// 5. SayingFooter displays verbatim text, source, and tier label; null shows nothing.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/features/shelf/saying_footer.dart';
import 'package:sakshi/features/shelf/saying_picker.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';

void main() {
  Widget wrap(Widget child) {
    return MaterialApp(
      home: Scaffold(
        body: child,
      ),
    );
  }

  group('SayingPicker', () {
    testWidgets('renders three saying cards with verbatim text, source, and tier labels A, C, D', (tester) async {
      String? pickedId;
      var dismissed = false;

      await tester.pumpWidget(wrap(
        SayingPicker(
          choices: sayingChoices,
          onPickSaying: (id) => pickedId = id,
          onDismiss: () => dismissed = true,
        ),
      ));

      // Title & three choice cards
      expect(find.byKey(const Key('saying_picker_title')), findsOneWidget);
      expect(find.byKey(const Key('saying_choice_0')), findsOneWidget);
      expect(find.byKey(const Key('saying_choice_1')), findsOneWidget);
      expect(find.byKey(const Key('saying_choice_2')), findsOneWidget);

      // Verify tier labels A, C, D display correctly
      expect(find.textContaining('Type not resolved in the Outcome Map'), findsOneWidget); // Tier D
      expect(find.textContaining('His own writing or letter'), findsOneWidget); // Tier A
      expect(find.textContaining('Reported by others'), findsOneWidget); // Tier C

      // Verify verbatim sayings texts
      expect(find.textContaining('Until you know what the mind is doing you cannot control it.'), findsOneWidget);
      expect(find.textContaining("the faculty of detaching ourselves at a moment's notice"), findsOneWidget);
      expect(find.textContaining('I am watching my mind act'), findsOneWidget);

      // Tap on second choice (sy05)
      await tester.tap(find.byKey(const Key('saying_choice_1')));
      expect(pickedId, 'sy05');

      // Dismiss button
      expect(find.byKey(const Key('saying_picker_dismiss_button')), findsOneWidget);
      await tester.tap(find.byKey(const Key('saying_picker_dismiss_button')));
      expect(dismissed, isTrue);
    });

    testWidgets('renders SizedBox.shrink when choices list is empty', (tester) async {
      await tester.pumpWidget(wrap(
        const SayingPicker(choices: []),
      ));

      expect(find.byKey(const Key('saying_picker_title')), findsNothing);
      expect(find.byKey(const Key('saying_picker_dismiss_button')), findsNothing);
    });
  });

  group('SayingFooter', () {
    testWidgets('renders chosen saying with verbatim text, source, and tier label', (tester) async {
      final saying = sayingChoices[0];

      await tester.pumpWidget(wrap(
        SayingFooter(saying: saying),
      ));

      expect(find.byKey(const Key('saying_footer_text')), findsOneWidget);
      expect(find.text('“${saying.text}”'), findsOneWidget);
      expect(find.byKey(const Key('saying_footer_source')), findsOneWidget);
      expect(find.text('${saying.source} · ${saying.tierLabel}'), findsOneWidget);
    });

    testWidgets('renders SizedBox.shrink when saying is null', (tester) async {
      await tester.pumpWidget(wrap(
        const SayingFooter(saying: null),
      ));

      expect(find.byKey(const Key('saying_footer_text')), findsNothing);
      expect(find.byKey(const Key('saying_footer_source')), findsNothing);
    });
  });

  group('SayingPickerScreen', () {
    testWidgets('renders choices from provider', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sayingChoicesProvider.overrideWith((ref) => Future.value(sayingChoices)),
          ],
          child: const MaterialApp(
            home: SayingPickerScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Saying Shelf'), findsOneWidget);
      expect(find.byKey(const Key('saying_picker_title')), findsOneWidget);
      expect(find.byKey(const Key('saying_choice_0')), findsOneWidget);
    });

    testWidgets('renders empty message when no choices available', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sayingChoicesProvider.overrideWith((ref) => Future.value([])),
          ],
          child: const MaterialApp(
            home: SayingPickerScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('saying_picker_empty')), findsOneWidget);
      expect(find.text('No sayings to pick right now.'), findsOneWidget);
    });
  });
}
