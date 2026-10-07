// lib/features/mirror/verdict_card.dart
// T4.3 (Prompt A) — Mirror Sub-Cards (Track 4 only).
// Displays the self-judging verdict from an experiment.
// No color judgment, no red/green. Displays text as given by DTO.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Verdict card: displays the outcome of a past experiment.
class VerdictCard extends StatelessWidget {
  const VerdictCard({super.key, this.verdict});

  final VerdictDto? verdict;

  @override
  Widget build(BuildContext context) {
    final v = verdict;
    if (v == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;
    final aside = theme.textTheme.bodySmall?.copyWith(
      color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(v.line, style: body, key: const Key('verdict_line')),
        if (v.beforeValue != null && v.afterValue != null) ...[
          const SizedBox(height: 4),
          Text(
            '${v.beforeValue} → ${v.afterValue}${v.approxMix ? ' (approximate mix)' : ''}',
            style: aside,
            key: const Key('verdict_values'),
          ),
        ],
      ],
    );
  }
}
