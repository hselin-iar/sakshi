// lib/features/mirror/mirror_screen.dart
// T4.3 (Prompt B) — Provider-backed Weekly Mirror screen.
// Week picker through listMirrorWeeks.
// Exhaustive states: loading, error with retry, and data.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:sakshi/features/mirror/mirror_content.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

// ─────────────────────────────────────────────────────────────────────────────
// Providers
// ─────────────────────────────────────────────────────────────────────────────

/// The Pigeon API client instance, overrideable in tests.
final sakshiHostApiProvider = Provider<SakshiHostApi>((ref) => SakshiHostApi());

/// The currently selected week start epoch ms (null = most recent completed week).
final selectedWeekProvider = StateProvider<int?>((ref) => null);

/// Loads the list of available Mirror weeks.
final mirrorWeeksProvider = FutureProvider<List<WeekRefDto>>((ref) async {
  final api = ref.watch(sakshiHostApiProvider);
  return await api.listMirrorWeeks();
});

/// Loads the MirrorDto for the given week start (or latest if null).
final mirrorProvider = FutureProvider.family<MirrorDto, int?>((ref, weekStartEpochMs) async {
  final api = ref.watch(sakshiHostApiProvider);
  return await api.getMirror(weekStartEpochMs);
});

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

class MirrorScreen extends ConsumerWidget {
  const MirrorScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final selectedWeek = ref.watch(selectedWeekProvider);
    final mirrorAsync = ref.watch(mirrorProvider(selectedWeek));
    final weeksAsync = ref.watch(mirrorWeeksProvider);
    final api = ref.watch(sakshiHostApiProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('साक्षी'),
        centerTitle: false,
        actions: [
          weeksAsync.when(
            data: (weeks) {
              if (weeks.isEmpty) return const SizedBox.shrink();
              return Padding(
                padding: const EdgeInsets.only(right: 12),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<int?>(
                    key: const Key('week_picker_dropdown'),
                    value: selectedWeek,
                    hint: Text(
                      weeks.firstWhere(
                        (w) => w.weekStartEpochMs == selectedWeek,
                        orElse: () => weeks.first,
                      ).label,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    items: [
                      const DropdownMenuItem<int?>(
                        value: null,
                        child: Text('Latest'),
                      ),
                      ...weeks.map(
                        (w) => DropdownMenuItem<int?>(
                          value: w.weekStartEpochMs,
                          child: Text(w.label),
                        ),
                      ),
                    ],
                    onChanged: (newWeek) {
                      ref.read(selectedWeekProvider.notifier).state = newWeek;
                    },
                  ),
                ),
              );
            },
            loading: () => const SizedBox.shrink(),
            error: (_, __) => const SizedBox.shrink(),
          ),
          PopupMenuButton<String>(
            key: const Key('mirror_menu_button'),
            icon: const Icon(Icons.more_vert),
            onSelected: (value) {
              if (value == 'today') {
                context.push('/today');
              } else if (value == 'demo') {
                context.push('/demo');
              }
            },
            itemBuilder: (context) => [
              const PopupMenuItem(
                value: 'today',
                key: Key('menu_item_today'),
                child: Text('Today so far'),
              ),
              const PopupMenuItem(
                value: 'demo',
                key: Key('menu_item_demo'),
                child: Text('Time Machine'),
              ),
            ],
          ),
        ],
      ),
      body: mirrorAsync.when(
        loading: () => const Center(
          key: Key('mirror_loading_indicator'),
          child: CircularProgressIndicator(),
        ),
        error: (err, stack) => Center(
          key: const Key('mirror_error_view'),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  "Could not read Sakshi's data",
                  style: Theme.of(context).textTheme.bodyLarge,
                  key: const Key('mirror_error_message'),
                ),
                const SizedBox(height: 16),
                OutlinedButton(
                  key: const Key('mirror_retry_button'),
                  onPressed: () {
                    ref.invalidate(mirrorProvider(selectedWeek));
                    ref.invalidate(mirrorWeeksProvider);
                  },
                  child: const Text('Retry'),
                ),
              ],
            ),
          ),
        ),
        data: (mirror) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(mirrorProvider(selectedWeek));
            ref.invalidate(mirrorWeeksProvider);
          },
          child: MirrorContent(
            mirror: mirror,
            onTapTryThis: (kindId, subjectKey, opensSettings) async {
              await api.tapTryThis(kindId, subjectKey);
              ref.invalidate(mirrorProvider(selectedWeek));
            },
            onDismissSuggestion: (kindId, subjectKey) async {
              await api.dismissSuggestion(kindId, subjectKey);
              ref.invalidate(mirrorProvider(selectedWeek));
            },
            onTapGoal: (answer) async {
              await api.tapGoal(answer);
              ref.invalidate(mirrorProvider(selectedWeek));
            },
            onReanchor: () async {
              await api.reanchorBaseline();
              ref.invalidate(mirrorProvider(selectedWeek));
            },
            onUseSuggestedBlock: (block) async {
              await api.saveStudyHours(
                StudyHoursDto(
                  blocks: [block],
                  learnForMe: true,
                ),
              );
              ref.invalidate(mirrorProvider(selectedWeek));
            },
          ),
        ),
      ),
    );
  }
}
