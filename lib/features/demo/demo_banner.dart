import 'package:flutter/material.dart';

import '../../core/ui_strings.dart';

// DemoBanner is created here (T3.1 stub) and owned by Track 4 afterwards
// (DOC 3 v1.1 CA-2). Track 4 will replace the body with richer content.
// DO NOT move this file; its path is referenced by app.dart.

/// A persistent banner shown at the top of the app whenever [SetupStateDto.isDemo]
/// is true. Mounted via [MaterialApp.router]'s builder in app.dart.
class DemoBanner extends StatelessWidget {
  const DemoBanner({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final banner = Material(
      color: Theme.of(context).colorScheme.tertiaryContainer,
      child: SafeArea(
        bottom: false,
        child: SizedBox(
          width: double.infinity,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 16),
            child: Text(
              demoBannerText,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    color:
                        Theme.of(context).colorScheme.onTertiaryContainer,
                  ),
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
    );

    return Stack(
      children: [
        child,
        Positioned(top: 0, left: 0, right: 0, child: banner),
      ],
    );
  }
}
