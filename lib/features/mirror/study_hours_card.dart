// lib/features/mirror/study_hours_card.dart
// T4.3 (Prompt A) — Mirror Sub-Cards (Track 4 only).
// Offers the learned study block when suggestedStudyBlock is non-null.
// "Use it" calls saveStudyHours with the learned block.
// "Keep mine" dismisses the offer.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Study hours card: presents an inferred / learned study block to the user.
class StudyHoursCard extends StatelessWidget {
  const StudyHoursCard({
    super.key,
    this.suggestedStudyBlock,
    this.onUseSuggestedBlock,
    this.onKeepMine,
  });

  final StudyBlockDto? suggestedStudyBlock;
  final void Function(StudyBlockDto block)? onUseSuggestedBlock;
  final VoidCallback? onKeepMine;

  static String formatBlock(StudyBlockDto block) {
    final startH = (block.startMinute ~/ 60).toString().padLeft(2, '0');
    final startM = (block.startMinute % 60).toString().padLeft(2, '0');
    final endH = (block.endMinute ~/ 60).toString().padLeft(2, '0');
    final endM = (block.endMinute % 60).toString().padLeft(2, '0');
    return '$startH:$startM–$endH:$endM';
  }

  @override
  Widget build(BuildContext context) {
    final block = suggestedStudyBlock;
    if (block == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;

    final formatted = formatBlock(block);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Your learned study block is $formatted.',
          style: body,
          key: const Key('study_hours_line'),
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton(
              key: const Key('study_hours_use_button'),
              onPressed: () => onUseSuggestedBlock?.call(block),
              child: const Text('Use it'),
            ),
            TextButton(
              key: const Key('study_hours_keep_button'),
              onPressed: onKeepMine,
              child: const Text('Keep mine'),
            ),
          ],
        ),
      ],
    );
  }
}
