// lib/features/mirror/suggestion_card.dart
// T4.3 (Prompt A) — Mirror Sub-Cards (Track 4 only).
// Displays at most one suggestion or observation.
// When opensSettings is true, the primary action handles settings opening.
// "Not now" calls dismissSuggestion.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Suggestion card: displays a single suggestion line with an action button
/// and "Not now" dismiss option, or an observation line when only observation is set.
class SuggestionCard extends StatelessWidget {
  const SuggestionCard({
    super.key,
    this.suggestion,
    this.observation,
    this.onTapTryThis,
    this.onDismiss,
  });

  final SuggestionDto? suggestion;
  final ObservationDto? observation;
  final void Function(String kindId, String? subjectKey, bool opensSettings)? onTapTryThis;
  final void Function(String kindId, String? subjectKey)? onDismiss;

  @override
  Widget build(BuildContext context) {
    final s = suggestion;
    final o = observation;

    if (s == null && o == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final body = theme.textTheme.bodyMedium;

    if (s != null) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            s.line,
            style: body,
            key: const Key('suggestion_line'),
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              OutlinedButton(
                key: const Key('suggestion_action_button'),
                onPressed: () {
                  onTapTryThis?.call(s.kindId, s.subjectKey, s.opensSettings);
                },
                child: Text(s.actionLabel),
              ),
              TextButton(
                key: const Key('suggestion_dismiss_button'),
                onPressed: () {
                  onDismiss?.call(s.kindId, s.subjectKey);
                },
                child: const Text('Not now'),
              ),
            ],
          ),
        ],
      );
    }

    // Observation only (no action buttons).
    return Text(
      o!.line,
      style: body,
      key: const Key('observation_line'),
    );
  }
}
