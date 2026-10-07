// lib/features/mirror/stones_card.dart
// T4.2 — Mirror Content Widgets (Track 4 only).
// Renders the stones (stay-origin) line and a simple three-segment bar.
// Display-only: no arithmetic beyond bar widths. Null segments → nothing shown.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Stones card: a three-segment bar (ping | self | unknown) plus the
/// top-stone label (if present) and the summary line from the DTO.
///
/// The three segments are proportional by count. When totalStays is zero
/// the bar is not rendered, only the line.
class StonesCard extends StatelessWidget {
  const StonesCard({super.key, required this.stones});

  final StonesDto stones;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;
    final label = theme.textTheme.bodySmall?.copyWith(
      color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
    );
    final cs = theme.colorScheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // ── Summary line ──────────────────────────────────────────────────
        Text(stones.line, style: body, key: const Key('stones_line')),

        // ── Top-stone label ───────────────────────────────────────────────
        if (stones.topStoneLabel != null) ...[
          const SizedBox(height: 2),
          Text(stones.topStoneLabel!, style: label, key: const Key('stones_top_label')),
        ],

        // ── Three-segment bar ─────────────────────────────────────────────
        if (stones.totalStays > 0) ...[
          const SizedBox(height: 8),
          _ThreeSegmentBar(
            key: const Key('stones_bar'),
            pingCount: stones.stoneCount,
            selfCount: stones.selfStartedCount,
            unknownCount: stones.unknownCount,
            total: stones.totalStays,
            pingColor: cs.primary.withValues(alpha: 0.75),
            selfColor: cs.secondary.withValues(alpha: 0.60),
            unknownColor: cs.onSurface.withValues(alpha: 0.18),
          ),
          const SizedBox(height: 4),
          // Legend
          _BarLegend(
            key: const Key('stones_legend'),
            pingCount: stones.stoneCount,
            selfCount: stones.selfStartedCount,
            unknownCount: stones.unknownCount,
            labelStyle: label,
          ),
        ],
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

class _ThreeSegmentBar extends StatelessWidget {
  const _ThreeSegmentBar({
    super.key,
    required this.pingCount,
    required this.selfCount,
    required this.unknownCount,
    required this.total,
    required this.pingColor,
    required this.selfColor,
    required this.unknownColor,
  });

  final int pingCount;
  final int selfCount;
  final int unknownCount;
  final int total;
  final Color pingColor;
  final Color selfColor;
  final Color unknownColor;

  @override
  Widget build(BuildContext context) {
    // Proportional flex values; at least 0, never negative.
    final pFlex = pingCount.clamp(0, total);
    final sFlex = selfCount.clamp(0, total);
    final uFlex = unknownCount.clamp(0, total);

    // Nothing to show if all zero (guard: should not happen when totalStays > 0).
    if (pFlex + sFlex + uFlex == 0) return const SizedBox.shrink();

    return SizedBox(
      height: 10,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(5),
        child: Row(
          children: [
            if (pFlex > 0)
              Expanded(flex: pFlex, child: ColoredBox(color: pingColor, child: const SizedBox.expand())),
            if (sFlex > 0)
              Expanded(flex: sFlex, child: ColoredBox(color: selfColor, child: const SizedBox.expand())),
            if (uFlex > 0)
              Expanded(flex: uFlex, child: ColoredBox(color: unknownColor, child: const SizedBox.expand())),
          ],
        ),
      ),
    );
  }
}

class _BarLegend extends StatelessWidget {
  const _BarLegend({
    super.key,
    required this.pingCount,
    required this.selfCount,
    required this.unknownCount,
    this.labelStyle,
  });

  final int pingCount;
  final int selfCount;
  final int unknownCount;
  final TextStyle? labelStyle;

  @override
  Widget build(BuildContext context) {
    final parts = <String>[];
    if (pingCount > 0) parts.add('$pingCount ping');
    if (selfCount > 0) parts.add('$selfCount self');
    if (unknownCount > 0) parts.add('$unknownCount unknown');
    if (parts.isEmpty) return const SizedBox.shrink();
    return Text(parts.join(' · '), style: labelStyle);
  }
}
