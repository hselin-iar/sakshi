import 'package:flutter/material.dart';

import '../palette.dart';

/// "साक्षी" in Devanagari, the way the poster sets it. The Latin name sits beneath when `latin` is true.
class Wordmark extends StatelessWidget {
  const Wordmark({
    super.key,
    this.size = 44,
    this.color = SakshiColors.cream,
    this.latin = false,
  });

  final double size;
  final Color color;
  final bool latin;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'साक्षी',
          style: TextStyle(
            fontFamily: 'serif',
            fontSize: size,
            height: 1.15,
            fontWeight: FontWeight.w700,
            color: color,
          ),
        ),
        if (latin)
          Text(
            'Sakshi · The Witness',
            style: TextStyle(
              fontFamily: 'serif',
              fontSize: size * 0.34,
              letterSpacing: 0.4,
              color: SakshiColors.paleGold,
            ),
          ),
      ],
    );
  }
}

/// The poster's ground: brown, with the gold circle bleeding off one corner. Used behind the Home header and the introduction.
class PosterGround extends StatelessWidget {
  const PosterGround({
    super.key,
    required this.child,
    this.bottomRadius = 28,
    this.circle = 240,
  });

  final Widget child;
  final double bottomRadius;
  final double circle;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.vertical(
        bottom: Radius.circular(bottomRadius),
      ),
      child: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [SakshiColors.brown, SakshiColors.deepBrown],
          ),
        ),
        child: Stack(
          children: [
            Positioned(
              right: -circle * 0.26,
              top: -circle * 0.32,
              child: Container(
                width: circle,
                height: circle,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: SakshiColors.gold.withValues(alpha: 0.92),
                ),
              ),
            ),
            Positioned(
              right: circle * 0.10,
              top: circle * 0.06,
              child: Container(
                width: circle * 0.30,
                height: circle * 0.30,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: SakshiColors.brown.withValues(alpha: 0.55),
                    width: 2,
                  ),
                ),
              ),
            ),
            child,
          ],
        ),
      ),
    );
  }
}
