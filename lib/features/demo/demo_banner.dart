// lib/features/demo/demo_banner.dart
// T4.6 — Permanent Demo Data banner (Track 4 only).
// Mounted whenever isDemo is true.
// Permanent — it is the honesty of the demo.

import 'package:flutter/material.dart';

import '../../core/palette.dart';

/// Permanent banner indicating synthetic demo data is in use.
class DemoBanner extends StatelessWidget {
  const DemoBanner({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      key: const Key('demo_banner'),
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 16),
      // Solid maroon with cream text: the status bar's white icons sit on it, and it reads as a label in light and dark alike.
      color: SakshiColors.maroon,
      child: Center(
        child: Text(
          'Demo data',
          style: theme.textTheme.labelSmall?.copyWith(
            color: SakshiColors.cream,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.6,
          ),
        ),
      ),
    );
  }
}
