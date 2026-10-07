// lib/features/demo/demo_banner.dart
// Permanent Demo Data banner (Track 4).
// Can be used standalone or wrapping a child widget.

import 'package:flutter/material.dart';
import 'package:sakshi/core/ui_strings.dart';

/// Permanent banner indicating synthetic demo data is in use.
/// Can be mounted standalone or wrap a [child] widget.
class DemoBanner extends StatelessWidget {
  const DemoBanner({super.key, this.child});

  final Widget? child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    final banner = Container(
      key: const Key('demo_banner'),
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 16),
      color: cs.tertiaryContainer,
      child: SafeArea(
        bottom: false,
        child: Center(
          child: Text(
            demoBannerText,
            style: theme.textTheme.labelSmall?.copyWith(
              color: cs.onTertiaryContainer,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.6,
            ),
          ),
        ),
      ),
    );

    if (child == null) {
      return banner;
    }

    return Stack(
      children: [
        child!,
        Positioned(top: 0, left: 0, right: 0, child: banner),
      ],
    );
  }
}
