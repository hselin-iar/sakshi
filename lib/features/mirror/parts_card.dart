// lib/features/mirror/parts_card.dart
// T4.2 — Mirror Content Widgets (Track 4 only).
// Display-only. No arithmetic. A null value renders nothing, never zero.
// Published immediately after T4.2 so Track 3 (T3.7) can import PartsCard.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Four-part widget: stretch (with in-set / quiet aside), stays, return, extras.
/// Null parts → empty box; never renders "0" for absent data.
class PartsCard extends StatelessWidget {
  const PartsCard({super.key, required this.parts});

  final PartsDto? parts;

  @override
  Widget build(BuildContext context) {
    final p = parts;
    if (p == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;
    final aside = theme.textTheme.bodySmall?.copyWith(
      color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // ── Stretch line + in-set/quiet split aside ────────────────────────
        if (p.lines.stretch != null)
          _PartRow(
            key: const Key('part_stretch'),
            line: p.lines.stretch!,
            aside: _buildInSetQuietAside(p, aside),
            textStyle: body,
          ),

        // ── Stays line ────────────────────────────────────────────────────
        if (p.lines.stays != null)
          _PartRow(
            key: const Key('part_stays'),
            line: p.lines.stays!,
            textStyle: body,
          ),

        // ── Return line ───────────────────────────────────────────────────
        if (p.lines.ret != null)
          _PartRow(
            key: const Key('part_return'),
            line: p.lines.ret!,
            textStyle: body,
          ),

        // ── Quiet line ────────────────────────────────────────────────────
        if (p.lines.quiet != null)
          _PartRow(
            key: const Key('part_quiet'),
            line: p.lines.quiet!,
            textStyle: body,
          ),

        // ── Extras (collapsible "more" area) ──────────────────────────────
        if (p.extrasLines.isNotEmpty)
          _ExtrasArea(lines: p.extrasLines, aside: aside),
      ],
    );
  }

  /// Builds the subdued in-set / quiet percentages shown beside the stretch line,
  /// only when both values are available.
  Widget? _buildInSetQuietAside(PartsDto p, TextStyle? style) {
    final inSet = p.inSetShare;
    final quiet = p.quietShare;
    if (inSet == null && quiet == null) return null;
    final parts = <String>[];
    if (inSet != null) parts.add('${(inSet * 100).round()}% in work apps');
    if (quiet != null) parts.add('${(quiet * 100).round()}% quiet');
    if (parts.isEmpty) return null;
    return Text(
      parts.join(' · '),
      style: style,
      key: const Key('part_inset_quiet'),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

class _PartRow extends StatelessWidget {
  const _PartRow({super.key, required this.line, this.aside, this.textStyle});

  final String line;
  final Widget? aside;
  final TextStyle? textStyle;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(line, style: textStyle),
          if (aside != null) ...[const SizedBox(height: 2), aside!],
        ],
      ),
    );
  }
}

class _ExtrasArea extends StatefulWidget {
  const _ExtrasArea({required this.lines, this.aside});

  final List<String> lines;
  final TextStyle? aside;

  @override
  State<_ExtrasArea> createState() => _ExtrasAreaState();
}

class _ExtrasAreaState extends State<_ExtrasArea> {
  bool _expanded = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        GestureDetector(
          key: const Key('part_extras_toggle'),
          onTap: () => setState(() => _expanded = !_expanded),
          child: Text(
            _expanded ? 'Less' : 'More',
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurface.withValues(alpha: 0.55),
              decoration: TextDecoration.underline,
            ),
          ),
        ),
        if (_expanded)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                for (final line in widget.lines)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Text(line, style: widget.aside),
                  ),
              ],
            ),
          ),
      ],
    );
  }
}
