// lib/features/today/today_screen.dart
// T4.4 — Ask Now (Today So Far).
// Shows completed windows only, as simple rows.
// Strictly NO suggestion, NO Steadiness, NO baseline comparisons.
// Pull-only: reached only from a quiet menu entry in the Mirror.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sakshi/features/mirror/mirror_screen.dart';
import 'package:sakshi/features/mirror/parts_card.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

import '../../core/providers.dart' show noAutoRetry;

/// Provider for the TodayDto data.
final todayProvider = FutureProvider<TodayDto>((ref) async {
  final api = ref.watch(sakshiHostApiProvider);
  return await api.getTodaySoFar();
}, retry: noAutoRetry);

class TodayScreen extends ConsumerWidget {
  const TodayScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final todayAsync = ref.watch(todayProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Today so far')),
      body: todayAsync.when(
        loading: () => const Center(
          key: Key('today_loading'),
          child: CircularProgressIndicator(),
        ),
        error: (err, _) => Center(
          key: const Key('today_error'),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  "Could not read today's data",
                  key: Key('today_error_message'),
                ),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => ref.invalidate(todayProvider),
                  child: const Text('Retry'),
                ),
              ],
            ),
          ),
        ),
        data: (today) => RefreshIndicator(
          onRefresh: () async => ref.invalidate(todayProvider),
          child: ListView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            children: [
              // ── Data Flags (if any) ──────────────────────────────────────
              for (final line in today.dataLines) ...[
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 10,
                  ),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerHighest
                        .withValues(alpha: 0.5),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    line,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),
                const SizedBox(height: 12),
              ],

              // ── Summary Line ─────────────────────────────────────────────
              Text(
                today.line,
                style: Theme.of(context).textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.w600, height: 1.4),
                key: const Key('today_summary_line'),
              ),
              const SizedBox(height: 20),

              // ── Completed Windows ────────────────────────────────────────
              if (today.windows.isEmpty) ...[
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: Theme.of(context).colorScheme.outlineVariant
                          .withValues(alpha: 0.4),
                    ),
                  ),
                  child: Text(
                    'No finished window yet today.',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Theme.of(context).colorScheme.onSurface
                          .withValues(alpha: 0.7),
                    ),
                    key: const Key('today_empty_state'),
                  ),
                ),
              ] else ...[
                Column(
                  key: const Key('today_windows_list'),
                  children: [
                    for (var i = 0; i < today.windows.length; i++) ...[
                      _WindowRow(
                        key: Key('today_window_$i'),
                        window: today.windows[i],
                      ),
                      if (i < today.windows.length - 1)
                        const SizedBox(height: 10),
                    ],
                  ],
                ),
              ],

              // ── Pooled Parts (if present) ────────────────────────────────
              if (today.parts != null) ...[
                const SizedBox(height: 20),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerLow,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: Theme.of(context).colorScheme.outlineVariant
                          .withValues(alpha: 0.4),
                    ),
                  ),
                  child: PartsCard(parts: today.parts),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _WindowRow extends StatelessWidget {
  const _WindowRow({super.key, required this.window});

  final TodayWindowDto window;

  String _formatEpoch(int epochMs) {
    final dt = DateTime.fromMillisecondsSinceEpoch(epochMs);
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '$h:$m';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;
    final aside = theme.textTheme.bodySmall?.copyWith(
      color: theme.colorScheme.onSurface.withValues(alpha: 0.6),
    );

    final timeRange =
        '${_formatEpoch(window.startEpochMs)}–${_formatEpoch(window.endEpochMs)}';

    final details = <String>[
      '${window.stays} stays',
      if (window.stretchMin case final stretch?) '${stretch.round()}m stretch',
      if (window.returnMin case final back?) '${back.round()}m return',
      if (window.shape != null) window.shape!,
    ];

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: theme.colorScheme.outlineVariant.withValues(alpha: 0.4),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(timeRange, style: body?.copyWith(fontWeight: FontWeight.w500)),
          Text(details.join(' · '), style: aside),
        ],
      ),
    );
  }
}
