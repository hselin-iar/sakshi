// lib/features/mirror/reanchor_card.dart
// T4.3 (Prompt A) — Mirror Sub-Cards (Track 4 only).
// The week-4 offer when reanchorOffered is true: one plain line,
// one button calling reanchorBaseline(), and one "Not now".

import 'package:flutter/material.dart';

/// Reanchor card: offers the user the one-time option to re-anchor baseline.
class ReanchorCard extends StatelessWidget {
  const ReanchorCard({
    super.key,
    required this.reanchorOffered,
    this.onReanchor,
    this.onDismiss,
  });

  final bool reanchorOffered;
  final VoidCallback? onReanchor;
  final VoidCallback? onDismiss;

  @override
  Widget build(BuildContext context) {
    if (!reanchorOffered) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Four weeks in. Would you like to re-anchor your starting normal?',
          style: body,
          key: const Key('reanchor_line'),
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            OutlinedButton(
              key: const Key('reanchor_button'),
              onPressed: onReanchor,
              child: const Text('Re-anchor'),
            ),
            TextButton(
              key: const Key('reanchor_dismiss_button'),
              onPressed: onDismiss,
              child: const Text('Not now'),
            ),
          ],
        ),
      ],
    );
  }
}
