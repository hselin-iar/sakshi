import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/ui_strings.dart';

// ---------------------------------------------------------------------------
// FirstScreen — what Sakshi will and will not do (DOC 1 §1.4.1).
//
// Four lines from ui_strings.dart. One button. No data, no logic.
// ---------------------------------------------------------------------------

class FirstScreen extends StatelessWidget {
  const FirstScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              Text(
                'साक्षी',
                style: textTheme.displaySmall?.copyWith(
                  color: colorScheme.primary,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                'Sakshi',
                style: textTheme.titleMedium?.copyWith(
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 40),
              const _Line(firstScreenLine1),
              const SizedBox(height: 20),
              const _Line(firstScreenLine2),
              const SizedBox(height: 20),
              const _Line(firstScreenLine3),
              const SizedBox(height: 20),
              const _Line(firstScreenLine4),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => context.go('/setup/usage'),
                  child: const Text('Continue'),
                ),
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ),
    );
  }
}

class _Line extends StatelessWidget {
  const _Line(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: Theme.of(context).textTheme.bodyLarge,
    );
  }
}
