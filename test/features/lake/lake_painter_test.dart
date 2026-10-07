// test/features/lake/lake_painter_test.dart
// T4.4 — Widget tests for LakePainter and in-app Lake widget.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/features/lake/lake_painter.dart';
import 'package:sakshi/features/lake/lake_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';

void main() {
  Widget wrap(Widget child) {
    return MaterialApp(
      home: Scaffold(
        body: Center(child: child),
      ),
    );
  }

  group('LakePainter and LakeWidgetView', () {
    testWidgets('renders all five Lake states and paints correctly', (tester) async {
      for (final entry in allLakeFixtures.entries) {
        final lake = entry.value;

        await tester.pumpWidget(wrap(
          LakeWidgetView(lake: lake),
        ));

        expect(find.byKey(Key('lake_canvas_${lake.state.name}')), findsOneWidget);
        expect(find.byKey(const Key('lake_phrase')), findsOneWidget);
        expect(find.text(lake.phrase), findsOneWidget);
      }
    });

    testWidgets('paints three distinct visual states: still, rippled, choppy', (tester) async {
      // Test still painter directly
      final stillPainter = LakePainter(state: LakeStateDto.still);
      final learningPainter = LakePainter(state: LakeStateDto.learning);
      final noDataPainter = LakePainter(state: LakeStateDto.noData);
      final rippledPainter = LakePainter(state: LakeStateDto.rippled);
      final choppyPainter = LakePainter(state: LakeStateDto.choppy);

      expect(stillPainter.state, LakeStateDto.still);
      expect(learningPainter.state, LakeStateDto.learning);
      expect(noDataPainter.state, LakeStateDto.noData);
      expect(rippledPainter.state, LakeStateDto.rippled);
      expect(choppyPainter.state, LakeStateDto.choppy);
    });

    testWidgets('LakeScreen displays lake data from provider', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            lakeProvider.overrideWith((ref) => Future.value(lakeRippled)),
          ],
          child: const MaterialApp(
            home: LakeScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('The Lake'), findsOneWidget);
      expect(find.text('A few ripples.'), findsOneWidget);
    });
  });
}
