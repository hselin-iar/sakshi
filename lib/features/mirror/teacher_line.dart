// lib/features/mirror/teacher_line.dart
// T4.3 (Prompt A) — Mirror Sub-Cards (Track 4 only).
// Displays the "Teacher Leaves" own-app usage line.
// Subdued, calm observation. Omitted in gentle mode or when teacher is null.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// Teacher line: quietly displays Sakshi's own open count.
class TeacherLine extends StatelessWidget {
  const TeacherLine({super.key, this.teacher});

  final TeacherDto? teacher;

  @override
  Widget build(BuildContext context) {
    final t = teacher;
    if (t == null) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final textStyle = theme.textTheme.bodySmall?.copyWith(
      color: theme.colorScheme.onSurface.withValues(alpha: 0.65),
    );

    return Text(t.line, style: textStyle, key: const Key('teacher_line'));
  }
}
