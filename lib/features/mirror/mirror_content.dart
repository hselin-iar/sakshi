// lib/features/mirror/mirror_content.dart
// T4.3 (Prompt B) — Single scroll composing the Weekly Mirror in DOC 1 order.
// No dashboard, no daily number, no badges, no streak visuals.
// Gentle mode shows only what the DTO contains.

import 'package:flutter/material.dart';
import 'package:sakshi/features/mirror/goal_tap.dart';
import 'package:sakshi/features/mirror/parts_card.dart';
import 'package:sakshi/features/mirror/patterns_card.dart';
import 'package:sakshi/features/mirror/reanchor_card.dart';
import 'package:sakshi/features/mirror/stones_card.dart';
import 'package:sakshi/features/mirror/study_hours_card.dart';
import 'package:sakshi/features/mirror/suggestion_card.dart';
import 'package:sakshi/features/mirror/teacher_line.dart';
import 'package:sakshi/features/mirror/verdict_card.dart';
import 'package:sakshi/features/shelf/saying_footer.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// The single scroll composing the Mirror in DOC 1 order:
/// 1. General data flags (above the data they qualify)
/// 2. Headline (+ Steadiness when present and not gentle)
/// 3. Parts (stretch, stays, return, quiet, extras)
/// 4. Stones-specific data flag + Stones card
/// 5. Clear hour
/// 6. Patterns (at most 4 with evidence counts)
/// 7. Saying footer
/// 8. Suggestion or "Nothing to fix this week"
/// 9. Verdict
/// 10. Goal tap
/// 11. Teacher line
/// 12. Lapse line
/// 13. Re-anchor card (when offered and not gentle)
/// 14. Study hours card (when suggested and not gentle)
class MirrorContent extends StatelessWidget {
  const MirrorContent({
    super.key,
    required this.mirror,
    this.onTapTryThis,
    this.onDismissSuggestion,
    this.onTapGoal,
    this.onReanchor,
    this.onDismissReanchor,
    this.onUseSuggestedBlock,
    this.onKeepMineBlock,
  });

  final MirrorDto mirror;
  final void Function(String kindId, String? subjectKey, bool opensSettings)? onTapTryThis;
  final void Function(String kindId, String? subjectKey)? onDismissSuggestion;
  final void Function(GoalAnswerDto answer)? onTapGoal;
  final VoidCallback? onReanchor;
  final VoidCallback? onDismissReanchor;
  final void Function(StudyBlockDto block)? onUseSuggestedBlock;
  final VoidCallback? onKeepMineBlock;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final textTheme = theme.textTheme;
    final colorScheme = theme.colorScheme;

    // Distinguish stones-specific flags (partialPing, pingOff) from general week flags
    final stonesFlags = <int>[];
    final generalFlags = <int>[];

    for (var i = 0; i < mirror.dataFlags.length; i++) {
      final flag = mirror.dataFlags[i];
      if (flag == DataFlagDto.partialPing || flag == DataFlagDto.pingOff) {
        stonesFlags.add(i);
      } else {
        generalFlags.add(i);
      }
    }

    return SingleChildScrollView(
      key: const Key('mirror_scroll_view'),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── 1. General Data Flags (above headline) ───────────────────────
          for (final idx in generalFlags)
            if (idx < mirror.dataLines.length) ...[
              _FlagNotice(
                key: Key('flag_general_$idx'),
                text: mirror.dataLines[idx],
              ),
              const SizedBox(height: 12),
            ],

          // ── 2. Headline ──────────────────────────────────────────────────
          Text(
            mirror.headline,
            style: textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w600,
              height: 1.4,
            ),
            key: const Key('mirror_headline'),
          ),

          // Steadiness indicator (omitted in gentle mode or when null)
          if (!mirror.gentle && mirror.steadiness != null) ...[
            const SizedBox(height: 8),
            _SteadinessWidget(
              key: const Key('steadiness_widget'),
              steadiness: mirror.steadiness!,
            ),
          ],

          const SizedBox(height: 20),

          // ── 3. Parts Card ────────────────────────────────────────────────
          if (mirror.parts != null) ...[
            _SectionCard(
              child: PartsCard(parts: mirror.parts),
            ),
            const SizedBox(height: 16),
          ],

          // ── 4. Stones (with qualifying flag directly above) ──────────────
          if (mirror.stones != null) ...[
            for (final idx in stonesFlags)
              if (idx < mirror.dataLines.length) ...[
                _FlagNotice(
                  key: Key('flag_stones_$idx'),
                  text: mirror.dataLines[idx],
                ),
                const SizedBox(height: 8),
              ],
            _SectionCard(
              child: StonesCard(stones: mirror.stones!),
            ),
            const SizedBox(height: 16),
          ],

          // ── 5. Clear Hour ────────────────────────────────────────────────
          if (mirror.clearHour != null) ...[
            _SectionCard(
              child: Text(
                mirror.clearHour!.line,
                style: textTheme.bodyMedium,
                key: const Key('clear_hour_line'),
              ),
            ),
            const SizedBox(height: 16),
          ],

          // ── 6. Patterns (at most 4 with evidence counts) ─────────────────
          if (mirror.patterns.isNotEmpty) ...[
            _SectionCard(
              child: PatternsCard(patterns: mirror.patterns),
            ),
            const SizedBox(height: 16),
          ],

          // ── 7. Saying Footer ─────────────────────────────────────────────
          if (mirror.saying != null) ...[
            SayingFooter(
              key: const Key('saying_footer'),
              saying: mirror.saying,
            ),
            const SizedBox(height: 16),
          ],

          // ── 8. Suggestion or "Nothing to fix this week" ───────────────────
          if (mirror.suggestion != null || mirror.observation != null) ...[
            _SectionCard(
              child: SuggestionCard(
                suggestion: mirror.suggestion,
                observation: mirror.observation,
                onTapTryThis: onTapTryThis,
                onDismiss: onDismissSuggestion,
              ),
            ),
            const SizedBox(height: 16),
          ] else if (mirror.nothingToFix) ...[
            _SectionCard(
              child: Text(
                'Nothing to fix this week',
                style: textTheme.bodyMedium?.copyWith(
                  color: colorScheme.onSurface.withValues(alpha: 0.8),
                ),
                key: const Key('nothing_to_fix_line'),
              ),
            ),
            const SizedBox(height: 16),
          ],

          // ── 9. Verdict ───────────────────────────────────────────────────
          if (mirror.verdict != null) ...[
            _SectionCard(
              child: VerdictCard(verdict: mirror.verdict),
            ),
            const SizedBox(height: 16),
          ],

          // ── 10. Goal Tap ─────────────────────────────────────────────────
          if (mirror.goalTap.offered) ...[
            _SectionCard(
              child: GoalTap(
                goalTap: mirror.goalTap,
                onTapGoal: onTapGoal,
              ),
            ),
            const SizedBox(height: 16),
          ],

          // ── 11. Teacher Line ─────────────────────────────────────────────
          if (mirror.teacher != null) ...[
            _SectionCard(
              child: TeacherLine(teacher: mirror.teacher),
            ),
            const SizedBox(height: 16),
          ],

          // ── 12. Lapse Line ───────────────────────────────────────────────
          if (mirror.lapseLine != null) ...[
            _SectionCard(
              child: Text(
                mirror.lapseLine!,
                style: textTheme.bodyMedium,
                key: const Key('lapse_line'),
              ),
            ),
            const SizedBox(height: 16),
          ],

          // ── 13. Re-anchor Card (week-4 offer, omitted in gentle) ──────────
          if (!mirror.gentle && mirror.reanchorOffered) ...[
            _SectionCard(
              child: ReanchorCard(
                reanchorOffered: mirror.reanchorOffered,
                onReanchor: onReanchor,
                onDismiss: onDismissReanchor,
              ),
            ),
            const SizedBox(height: 16),
          ],

          // ── 14. Study Hours Card (learned block, omitted in gentle) ───────
          if (!mirror.gentle && mirror.suggestedStudyBlock != null) ...[
            _SectionCard(
              child: StudyHoursCard(
                suggestedStudyBlock: mirror.suggestedStudyBlock,
                onUseSuggestedBlock: onUseSuggestedBlock,
                onKeepMine: onKeepMineBlock,
              ),
            ),
            const SizedBox(height: 16),
          ],
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Supporting Sub-Widgets
// ─────────────────────────────────────────────────────────────────────────────

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
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
      child: child,
    );
  }
}

class _FlagNotice extends StatelessWidget {
  const _FlagNotice({super.key, required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurface.withValues(alpha: 0.75),
        ),
      ),
    );
  }
}

class _SteadinessWidget extends StatelessWidget {
  const _SteadinessWidget({super.key, required this.steadiness});

  final SteadinessDto steadiness;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Text(
      'Steadiness: ${steadiness.value} (${steadiness.word})',
      style: theme.textTheme.bodySmall?.copyWith(
        color: theme.colorScheme.onSurface.withValues(alpha: 0.65),
      ),
    );
  }
}
