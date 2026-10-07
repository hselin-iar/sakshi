// lib/features/shelf/saying_picker.dart
// T4.5 — Saying Shelf Picker (Track 4 only).
// Offers three saying cards, one tap to pick.
// Shows verbatim text, source line, and tier label.
// Includes a "Not now" action.
// An empty list shows nothing. Never uses command language.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sakshi/features/mirror/mirror_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Provider for available saying choices.
final sayingChoicesProvider = FutureProvider<List<SayingDto>>((ref) async {
  final api = ref.watch(sakshiHostApiProvider);
  return await api.getSayingChoices();
});

/// Embeddable Saying picker widget.
class SayingPicker extends StatelessWidget {
  const SayingPicker({
    super.key,
    required this.choices,
    this.onPickSaying,
    this.onDismiss,
  });

  final List<SayingDto> choices;
  final void Function(String sayingId)? onPickSaying;
  final VoidCallback? onDismiss;

  @override
  Widget build(BuildContext context) {
    if (choices.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final textTheme = theme.textTheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'Choose a saying for the Mirror',
          style: textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w600,
          ),
          key: const Key('saying_picker_title'),
        ),
        const SizedBox(height: 12),
        for (var i = 0; i < choices.length; i++) ...[
          _SayingChoiceCard(
            key: Key('saying_choice_$i'),
            saying: choices[i],
            onTap: () => onPickSaying?.call(choices[i].id),
          ),
          if (i < choices.length - 1) const SizedBox(height: 10),
        ],
        const SizedBox(height: 12),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton(
            key: const Key('saying_picker_dismiss_button'),
            onPressed: onDismiss,
            child: const Text('Not now'),
          ),
        ),
      ],
    );
  }
}

class _SayingChoiceCard extends StatelessWidget {
  const _SayingChoiceCard({
    super.key,
    required this.saying,
    required this.onTap,
  });

  final SayingDto saying;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
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
              '“${saying.text}”',
              style: textTheme.bodyMedium?.copyWith(
                fontStyle: FontStyle.italic,
                height: 1.4,
              ),
              key: Key('saying_choice_text_${saying.id}'),
            ),
            const SizedBox(height: 8),
            Text(
              '${saying.source} · ${saying.tierLabel}',
              style: textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurface.withValues(alpha: 0.6),
              ),
              key: Key('saying_choice_meta_${saying.id}'),
            ),
          ],
        ),
      ),
    );
  }
}

/// Screen hosting the saying picker via Riverpod.
class SayingPickerScreen extends ConsumerWidget {
  const SayingPickerScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final choicesAsync = ref.watch(sayingChoicesProvider);
    final api = ref.watch(sakshiHostApiProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Saying Shelf'),
      ),
      body: choicesAsync.when(
        loading: () => const Center(
          key: Key('saying_picker_loading'),
          child: CircularProgressIndicator(),
        ),
        error: (err, _) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text("Could not read sayings", key: Key('saying_picker_error')),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => ref.invalidate(sayingChoicesProvider),
                  child: const Text('Retry'),
                ),
              ],
            ),
          ),
        ),
        data: (choices) {
          if (choices.isEmpty) {
            return const Center(
              key: Key('saying_picker_empty'),
              child: Text('No sayings to pick right now.'),
            );
          }
          return SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            child: SayingPicker(
              choices: choices,
              onPickSaying: (id) async {
                await api.pickSaying(id);
                if (context.mounted) {
                  Navigator.of(context).maybePop();
                }
              },
              onDismiss: () {
                Navigator.of(context).maybePop();
              },
            ),
          );
        },
      ),
    );
  }
}
