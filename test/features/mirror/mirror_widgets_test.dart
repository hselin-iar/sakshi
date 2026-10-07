// test/features/mirror/mirror_widgets_test.dart
// T4.2 — Widget tests for PartsCard, StonesCard, and PatternsCard.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/features/mirror/parts_card.dart';
import 'package:sakshi/features/mirror/patterns_card.dart';
import 'package:sakshi/features/mirror/stones_card.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';

void main() {
  Widget wrap(Widget child) {
    return MaterialApp(
      home: Scaffold(
        body: child,
      ),
    );
  }

  group('PartsCard', () {
    testWidgets('renders all four part rows and inset/quiet aside for typical parts', (tester) async {
      await tester.pumpWidget(wrap(PartsCard(parts: steady.parts)));

      expect(find.byKey(const Key('part_stretch')), findsOneWidget);
      expect(find.byKey(const Key('part_stays')), findsOneWidget);
      expect(find.byKey(const Key('part_return')), findsOneWidget);
      expect(find.byKey(const Key('part_quiet')), findsOneWidget);
      expect(find.byKey(const Key('part_inset_quiet')), findsOneWidget);
      expect(find.textContaining('in work apps'), findsOneWidget);
      expect(find.textContaining('quiet'), findsWidgets);
    });

    testWidgets('renders nothing when parts is null (tooLittle fixture)', (tester) async {
      await tester.pumpWidget(wrap(PartsCard(parts: tooLittle.parts)));

      expect(find.byKey(const Key('part_stretch')), findsNothing);
      expect(find.byKey(const Key('part_stays')), findsNothing);
      expect(find.byKey(const Key('part_return')), findsNothing);
      expect(find.byKey(const Key('part_quiet')), findsNothing);
      // Ensures "0" is never rendered for absent data
      expect(find.text('0'), findsNothing);
    });

    testWidgets('provisional parts renders only stretch and stays, null lines omitted', (tester) async {
      await tester.pumpWidget(wrap(PartsCard(parts: firstLook.parts)));

      expect(find.byKey(const Key('part_stretch')), findsOneWidget);
      expect(find.byKey(const Key('part_stays')), findsOneWidget);
      expect(find.byKey(const Key('part_return')), findsNothing);
      expect(find.byKey(const Key('part_quiet')), findsNothing);
    });

    testWidgets('extras lines toggle expands and collapses', (tester) async {
      final partsWithExtras = PartsDto(
        glances: 5,
        lines: PartLinesDto(
          stretch: 'Stretch line.',
          stays: 'Stays line.',
        ),
        extrasLines: ['Extra line 1', 'Extra line 2'],
      );

      await tester.pumpWidget(wrap(PartsCard(parts: partsWithExtras)));

      expect(find.byKey(const Key('part_extras_toggle')), findsOneWidget);
      expect(find.text('More'), findsOneWidget);
      expect(find.text('Extra line 1'), findsNothing);

      await tester.tap(find.byKey(const Key('part_extras_toggle')));
      await tester.pumpAndSettle();

      expect(find.text('Less'), findsOneWidget);
      expect(find.text('Extra line 1'), findsOneWidget);
      expect(find.text('Extra line 2'), findsOneWidget);
    });
  });

  group('StonesCard', () {
    testWidgets('renders summary line, bar, and legend', (tester) async {
      await tester.pumpWidget(wrap(StonesCard(stones: steady.stones!)));

      expect(find.byKey(const Key('stones_line')), findsOneWidget);
      expect(find.byKey(const Key('stones_bar')), findsOneWidget);
      expect(find.byKey(const Key('stones_legend')), findsOneWidget);
      expect(find.textContaining('ping'), findsWidgets);
      expect(find.textContaining('self'), findsWidgets);
    });

    testWidgets('renders top stone label when present', (tester) async {
      await tester.pumpWidget(wrap(StonesCard(stones: wavering.stones!)));

      expect(find.byKey(const Key('stones_top_label')), findsOneWidget);
      expect(find.text(wavering.stones!.topStoneLabel!), findsOneWidget);
    });

    testWidgets('does not render top stone label when null', (tester) async {
      await tester.pumpWidget(wrap(StonesCard(stones: steady.stones!)));

      expect(find.byKey(const Key('stones_top_label')), findsNothing);
    });

    testWidgets('does not render bar when totalStays is 0', (tester) async {
      final zeroStays = StonesDto(
        totalStays: 0,
        stoneCount: 0,
        selfStartedCount: 0,
        unknownCount: 0,
        line: 'No stays observed.',
      );

      await tester.pumpWidget(wrap(StonesCard(stones: zeroStays)));

      expect(find.byKey(const Key('stones_line')), findsOneWidget);
      expect(find.byKey(const Key('stones_bar')), findsNothing);
      expect(find.byKey(const Key('stones_legend')), findsNothing);
    });
  });

  group('PatternsCard', () {
    testWidgets('renders up to 4 patterns with evidence counts', (tester) async {
      await tester.pumpWidget(wrap(PatternsCard(patterns: steadier.patterns)));

      expect(find.byType(PatternsCard), findsOneWidget);
      expect(find.byKey(const Key('pattern_row_0')), findsOneWidget);
      expect(find.textContaining('windows ·'), findsWidgets);
    });

    testWidgets('renders SizedBox.shrink when patterns list is empty', (tester) async {
      await tester.pumpWidget(wrap(const PatternsCard(patterns: [])));

      expect(find.byKey(const Key('pattern_row_0')), findsNothing);
    });

    testWidgets('caps displayed patterns at 4 even if more are given', (tester) async {
      final manyPatterns = List.generate(
        6,
        (i) => PatternLineDto(
          kindId: 'P$i',
          line: 'Pattern line $i (based on ${i + 1} windows over 3 days).',
          evidenceWindows: i + 1,
          evidenceDays: 3,
        ),
      );

      await tester.pumpWidget(wrap(PatternsCard(patterns: manyPatterns)));

      expect(find.byKey(const Key('pattern_row_0')), findsOneWidget);
      expect(find.byKey(const Key('pattern_row_1')), findsOneWidget);
      expect(find.byKey(const Key('pattern_row_2')), findsOneWidget);
      expect(find.byKey(const Key('pattern_row_3')), findsOneWidget);
      expect(find.byKey(const Key('pattern_row_4')), findsNothing);
      expect(find.byKey(const Key('pattern_row_5')), findsNothing);
    });
  });
}
