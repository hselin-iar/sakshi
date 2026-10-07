// test/features/mirror/mirror_subcards_test.dart
// T4.3 (Prompt A) — Widget tests for Mirror sub-cards.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/features/mirror/goal_tap.dart';
import 'package:sakshi/features/mirror/reanchor_card.dart';
import 'package:sakshi/features/mirror/study_hours_card.dart';
import 'package:sakshi/features/mirror/suggestion_card.dart';
import 'package:sakshi/features/mirror/teacher_line.dart';
import 'package:sakshi/features/mirror/verdict_card.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

void main() {
  Widget wrap(Widget child) {
    return MaterialApp(
      home: Scaffold(
        body: child,
      ),
    );
  }

  group('SuggestionCard', () {
    testWidgets('renders suggestion line and buttons with tapTryThis and dismiss callbacks', (tester) async {
      String? tappedKind;
      String? tappedKey;
      bool? tappedSettings;
      String? dismissedKind;
      String? dismissedKey;

      final s = SuggestionDto(
        kindId: 'S4',
        line: 'One app accounts for 8 of your last 12 ping-driven stays.',
        actionLabel: 'Review its notifications',
        actionType: 'OPEN_NOTIFICATION_SETTINGS',
        opensSettings: true,
        subjectKey: 'com.example.messaging',
      );

      await tester.pumpWidget(wrap(
        SuggestionCard(
          suggestion: s,
          onTapTryThis: (kind, key, settings) {
            tappedKind = kind;
            tappedKey = key;
            tappedSettings = settings;
          },
          onDismiss: (kind, key) {
            dismissedKind = kind;
            dismissedKey = key;
          },
        ),
      ));

      expect(find.byKey(const Key('suggestion_line')), findsOneWidget);
      expect(find.text('One app accounts for 8 of your last 12 ping-driven stays.'), findsOneWidget);
      expect(find.byKey(const Key('suggestion_action_button')), findsOneWidget);
      expect(find.text('Review its notifications'), findsOneWidget);
      expect(find.byKey(const Key('suggestion_dismiss_button')), findsOneWidget);
      expect(find.text('Not now'), findsOneWidget);

      await tester.tap(find.byKey(const Key('suggestion_action_button')));
      expect(tappedKind, 'S4');
      expect(tappedKey, 'com.example.messaging');
      expect(tappedSettings, isTrue);

      await tester.tap(find.byKey(const Key('suggestion_dismiss_button')));
      expect(dismissedKind, 'S4');
      expect(dismissedKey, 'com.example.messaging');
    });

    testWidgets('renders observation line without action buttons when only observation is set', (tester) async {
      final o = ObservationDto(
        kindId: 'S11',
        line: 'You were away for 4 days. Your starting normal is still here.',
      );

      await tester.pumpWidget(wrap(
        SuggestionCard(observation: o),
      ));

      expect(find.byKey(const Key('observation_line')), findsOneWidget);
      expect(find.text('You were away for 4 days. Your starting normal is still here.'), findsOneWidget);
      expect(find.byKey(const Key('suggestion_action_button')), findsNothing);
      expect(find.byKey(const Key('suggestion_dismiss_button')), findsNothing);
    });

    testWidgets('renders SizedBox.shrink when both suggestion and observation are null', (tester) async {
      await tester.pumpWidget(wrap(
        const SuggestionCard(),
      ));

      expect(find.byKey(const Key('suggestion_line')), findsNothing);
      expect(find.byKey(const Key('observation_line')), findsNothing);
      expect(find.byType(OutlinedButton), findsNothing);
      expect(find.byType(TextButton), findsNothing);
    });
  });

  group('VerdictCard', () {
    testWidgets('renders verdict line and values when present', (tester) async {
      final v = VerdictDto(
        verdict: 'MOVED',
        line: 'Your stays per hour from that app dropped from 4.1 to 3.0 in the two weeks after you tried the change. This is correlation, not cause.',
        approxMix: false,
        beforeValue: 4.1,
        afterValue: 3.0,
      );

      await tester.pumpWidget(wrap(
        VerdictCard(verdict: v),
      ));

      expect(find.byKey(const Key('verdict_line')), findsOneWidget);
      expect(find.textContaining('This is correlation, not cause.'), findsOneWidget);
      expect(find.byKey(const Key('verdict_values')), findsOneWidget);
      expect(find.text('4.1 → 3.0'), findsOneWidget);
    });

    testWidgets('renders SizedBox.shrink when verdict is null', (tester) async {
      await tester.pumpWidget(wrap(
        const VerdictCard(verdict: null),
      ));

      expect(find.byKey(const Key('verdict_line')), findsNothing);
      expect(find.byKey(const Key('verdict_values')), findsNothing);
    });
  });

  group('GoalTap', () {
    testWidgets('renders three chips when offered is true and handles tapGoal', (tester) async {
      GoalAnswerDto? picked;

      final goalDto = GoalTapDto(offered: true, answer: null);

      await tester.pumpWidget(wrap(
        GoalTap(
          goalTap: goalDto,
          onTapGoal: (answer) => picked = answer,
        ),
      ));

      expect(find.byKey(const Key('goal_prompt')), findsOneWidget);
      expect(find.byKey(const Key('goal_chip_yes')), findsOneWidget);
      expect(find.byKey(const Key('goal_chip_partly')), findsOneWidget);
      expect(find.byKey(const Key('goal_chip_not_yet')), findsOneWidget);

      await tester.tap(find.byKey(const Key('goal_chip_partly')));
      expect(picked, GoalAnswerDto.partly);
    });

    testWidgets('marks selected chip according to answer', (tester) async {
      final goalDto = GoalTapDto(offered: true, answer: GoalAnswerDto.yes);

      await tester.pumpWidget(wrap(
        GoalTap(goalTap: goalDto),
      ));

      final yesChip = tester.widget<ChoiceChip>(find.byKey(const Key('goal_chip_yes')));
      final partlyChip = tester.widget<ChoiceChip>(find.byKey(const Key('goal_chip_partly')));
      expect(yesChip.selected, isTrue);
      expect(partlyChip.selected, isFalse);
    });

    testWidgets('renders SizedBox.shrink when offered is false', (tester) async {
      final goalDto = GoalTapDto(offered: false, answer: null);

      await tester.pumpWidget(wrap(
        GoalTap(goalTap: goalDto),
      ));

      expect(find.byKey(const Key('goal_prompt')), findsNothing);
      expect(find.byKey(const Key('goal_chip_yes')), findsNothing);
    });
  });

  group('TeacherLine', () {
    testWidgets('renders teacher line when present', (tester) async {
      final t = TeacherDto(
        opensThisWeek: 5,
        opensPrevWeek: 7,
        minutesThisWeek: 18.0,
        line: 'You opened Sakshi 5 times this week.',
      );

      await tester.pumpWidget(wrap(
        TeacherLine(teacher: t),
      ));

      expect(find.byKey(const Key('teacher_line')), findsOneWidget);
      expect(find.text('You opened Sakshi 5 times this week.'), findsOneWidget);
    });

    testWidgets('renders SizedBox.shrink when teacher is null', (tester) async {
      await tester.pumpWidget(wrap(
        const TeacherLine(teacher: null),
      ));

      expect(find.byKey(const Key('teacher_line')), findsNothing);
    });
  });

  group('ReanchorCard', () {
    testWidgets('renders re-anchor offer line and buttons when offered is true', (tester) async {
      var reanchored = false;
      var dismissed = false;

      await tester.pumpWidget(wrap(
        ReanchorCard(
          reanchorOffered: true,
          onReanchor: () => reanchored = true,
          onDismiss: () => dismissed = true,
        ),
      ));

      expect(find.byKey(const Key('reanchor_line')), findsOneWidget);
      expect(find.text('Four weeks in. Would you like to re-anchor your starting normal?'), findsOneWidget);
      expect(find.byKey(const Key('reanchor_button')), findsOneWidget);
      expect(find.byKey(const Key('reanchor_dismiss_button')), findsOneWidget);

      await tester.tap(find.byKey(const Key('reanchor_button')));
      expect(reanchored, isTrue);

      await tester.tap(find.byKey(const Key('reanchor_dismiss_button')));
      expect(dismissed, isTrue);
    });

    testWidgets('renders SizedBox.shrink when reanchorOffered is false', (tester) async {
      await tester.pumpWidget(wrap(
        const ReanchorCard(reanchorOffered: false),
      ));

      expect(find.byKey(const Key('reanchor_line')), findsNothing);
      expect(find.byKey(const Key('reanchor_button')), findsNothing);
    });
  });

  group('StudyHoursCard', () {
    testWidgets('renders learned block line and action buttons', (tester) async {
      StudyBlockDto? usedBlock;
      var keptMine = false;

      final block = StudyBlockDto(startMinute: 540, endMinute: 720); // 09:00–12:00

      await tester.pumpWidget(wrap(
        StudyHoursCard(
          suggestedStudyBlock: block,
          onUseSuggestedBlock: (b) => usedBlock = b,
          onKeepMine: () => keptMine = true,
        ),
      ));

      expect(find.byKey(const Key('study_hours_line')), findsOneWidget);
      expect(find.text('Your learned study block is 09:00–12:00.'), findsOneWidget);
      expect(find.byKey(const Key('study_hours_use_button')), findsOneWidget);
      expect(find.byKey(const Key('study_hours_keep_button')), findsOneWidget);

      await tester.tap(find.byKey(const Key('study_hours_use_button')));
      expect(usedBlock, equals(block));

      await tester.tap(find.byKey(const Key('study_hours_keep_button')));
      expect(keptMine, isTrue);
    });

    testWidgets('renders SizedBox.shrink when suggestedStudyBlock is null', (tester) async {
      await tester.pumpWidget(wrap(
        const StudyHoursCard(suggestedStudyBlock: null),
      ));

      expect(find.byKey(const Key('study_hours_line')), findsNothing);
      expect(find.byKey(const Key('study_hours_use_button')), findsNothing);
    });
  });
}
