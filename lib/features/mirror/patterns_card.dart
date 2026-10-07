// lib/features/mirror/patterns_card.dart
// T4.2 — Mirror Content Widgets (Track 4 only).
// Shows at most 4 pattern lines. Each line carries its evidence count
// as plain text already embedded in the line string (from SentenceBuilder).
// Display-only. No arithmetic.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Renders up to four PatternLineDto rows, each as a single text line.
/// The evidence count is already embedded in [PatternLineDto.line] by
/// SentenceBuilder — this widget just displays strings it is given.
///
/// An empty list renders nothing (SizedBox.shrink).
class PatternsCard extends StatelessWidget {
  const PatternsCard({super.key, required this.patterns});

  final List<PatternLineDto> patterns;

  // Contract: at most 4 patterns (DOC 3 F6).
  static const _maxPatterns = 4;

  @override
  Widget build(BuildContext context) {
    if (patterns.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;
    final evidenceStyle = theme.textTheme.bodySmall?.copyWith(
      color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
    );

    final shown = patterns.take(_maxPatterns).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (int i = 0; i < shown.length; i++)
          Padding(
            key: Key('pattern_row_$i'),
            padding: EdgeInsets.only(top: i == 0 ? 0 : 8),
            child: _PatternRow(
              pattern: shown[i],
              bodyStyle: body,
              evidenceStyle: evidenceStyle,
            ),
          ),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

class _PatternRow extends StatelessWidget {
  const _PatternRow({
    required this.pattern,
    this.bodyStyle,
    this.evidenceStyle,
  });

  final PatternLineDto pattern;
  final TextStyle? bodyStyle;
  final TextStyle? evidenceStyle;

  @override
  Widget build(BuildContext context) {
    // The line from SentenceBuilder already ends with "(based on N windows over D days)".
    // We display it verbatim. The evidence counts are shown only via the embedded text.
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          pattern.line,
          style: bodyStyle,
          key: Key('pattern_line_${pattern.kindId}'),
        ),
        // Explicit evidence pill so the count is always visible even if the
        // sentence format changes — matches DOC 3 "evidence count visible".
        const SizedBox(height: 2),
        Text(
          '${pattern.evidenceWindows} windows · ${pattern.evidenceDays} days',
          style: evidenceStyle,
          key: Key('pattern_evidence_${pattern.kindId}'),
        ),
      ],
    );
  }
}
