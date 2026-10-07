// lib/features/mirror/goal_tap.dart
// T4.3 (Prompt A) — Mirror Sub-Cards (Track 4 only).
// Offers the weekly goal check: three chips (Yes, Partly, Not yet).
// When offered is false, renders SizedBox.shrink().

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Goal tap: three chips for the weekly goal self-check.
class GoalTap extends StatelessWidget {
  const GoalTap({super.key, required this.goalTap, this.onTapGoal});

  final GoalTapDto goalTap;
  final void Function(GoalAnswerDto answer)? onTapGoal;

  @override
  Widget build(BuildContext context) {
    if (!goalTap.offered) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final labelStyle = theme.textTheme.bodyMedium;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Did you meet your own goal this week?',
          style: labelStyle,
          key: const Key('goal_prompt'),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: [
            ChoiceChip(
              key: const Key('goal_chip_yes'),
              label: const Text('Yes'),
              selected: goalTap.answer == GoalAnswerDto.yes,
              onSelected: (_) => onTapGoal?.call(GoalAnswerDto.yes),
            ),
            ChoiceChip(
              key: const Key('goal_chip_partly'),
              label: const Text('Partly'),
              selected: goalTap.answer == GoalAnswerDto.partly,
              onSelected: (_) => onTapGoal?.call(GoalAnswerDto.partly),
            ),
            ChoiceChip(
              key: const Key('goal_chip_not_yet'),
              label: const Text('Not yet'),
              selected: goalTap.answer == GoalAnswerDto.notYet,
              onSelected: (_) => onTapGoal?.call(GoalAnswerDto.notYet),
            ),
          ],
        ),
      ],
    );
  }
}
