// lib/features/shelf/saying_footer.dart
// T4.5 — Saying Footer (Track 4 only).
// Displays the chosen saying verbatim under the Mirror data
// with its source and tier label.
// Never presents the text as an instruction or command.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Saying footer: renders the user's active saying verbatim
/// with its source and tier label.
class SayingFooter extends StatelessWidget {
  const SayingFooter({super.key, this.saying});

  final SayingDto? saying;

  @override
  Widget build(BuildContext context) {
    final s = saying;
    if (s == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final textTheme = theme.textTheme;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: theme.colorScheme.outlineVariant.withValues(alpha: 0.4),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '“${s.text}”',
            style: textTheme.bodyMedium?.copyWith(
              fontStyle: FontStyle.italic,
              height: 1.4,
            ),
            key: const Key('saying_footer_text'),
          ),
          const SizedBox(height: 8),
          Text(
            '${s.source} · ${s.tierLabel}',
            style: textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurface.withValues(alpha: 0.6),
            ),
            key: const Key('saying_footer_source'),
          ),
        ],
      ),
    );
  }
}
