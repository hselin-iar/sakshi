// lib/features/demo/demo_banner.dart
// T4.6 — Permanent Demo Data banner (Track 4 only).
// Mounted whenever isDemo is true.
// Permanent — it is the honesty of the demo.

import 'package:flutter/material.dart';

/// Permanent banner indicating synthetic demo data is in use.
class DemoBanner extends StatelessWidget {
  const DemoBanner({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    return Container(
      key: const Key('demo_banner'),
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 16),
      color: cs.tertiaryContainer,
      child: Center(
        child: Text(
          'Demo data',
          style: theme.textTheme.labelSmall?.copyWith(
            color: cs.onTertiaryContainer,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.6,
          ),
        ),
      ),
    );
  }
}
