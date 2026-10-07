// lib/features/lake/lake_painter.dart
// T4.4 — Lake Painter and in-app Lake widget (Track 4 only).
// Renders still, rippled, or choppy water using vector paths only.
// learning and noData draw still water.
// Calm, no counters, nothing live, static only.

import 'package:flutter/material.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

/// CustomPainter rendering water surface for the Lake:
/// - still (also for learning, noData): calm horizontal lines
/// - rippled: gentle curved ripple arcs
/// - choppy: sharper undulating wave crests
class LakePainter extends CustomPainter {
  const LakePainter({
    required this.state,
    this.primaryColor,
    this.secondaryColor,
  });

  final LakeStateDto state;
  final Color? primaryColor;
  final Color? secondaryColor;

  @override
  void paint(Canvas canvas, Size size) {
    final w = size.width;
    final h = size.height;

    final primary = primaryColor ?? const Color(0xFF00796B);
    final secondary = secondaryColor ?? const Color(0xFF26A69A);

    final bgPaint = Paint()
      ..color = primary.withValues(alpha: 0.08)
      ..style = PaintingStyle.fill;

    // Base background water area (bottom half)
    final bgRect = RRect.fromRectAndRadius(
      Rect.fromLTWH(0, h * 0.45, w, h * 0.55),
      const Radius.circular(8),
    );
    canvas.drawRRect(bgRect, bgPaint);

    switch (state) {
      case LakeStateDto.still:
      case LakeStateDto.learning:
      case LakeStateDto.noData:
        _drawStill(canvas, size, primary, secondary);
        break;
      case LakeStateDto.rippled:
        _drawRippled(canvas, size, primary, secondary);
        break;
      case LakeStateDto.choppy:
        _drawChoppy(canvas, size, primary, secondary);
        break;
    }
  }

  void _drawStill(Canvas canvas, Size size, Color primary, Color secondary) {
    final w = size.width;
    final h = size.height;

    final line1 = Paint()
      ..color = primary
      ..strokeWidth = 1.6
      ..style = PaintingStyle.stroke;

    final line2 = Paint()
      ..color = secondary
      ..strokeWidth = 1.2
      ..style = PaintingStyle.stroke;

    final line3 = Paint()
      ..color = secondary.withValues(alpha: 0.7)
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;

    final line4 = Paint()
      ..color = secondary.withValues(alpha: 0.4)
      ..strokeWidth = 0.8
      ..style = PaintingStyle.stroke;

    // Calm horizontal parallel lines
    canvas.drawLine(Offset(w * 0.05, h * 0.48), Offset(w * 0.95, h * 0.48), line1);
    canvas.drawLine(Offset(w * 0.12, h * 0.58), Offset(w * 0.88, h * 0.58), line2);
    canvas.drawLine(Offset(w * 0.20, h * 0.68), Offset(w * 0.80, h * 0.68), line3);
    canvas.drawLine(Offset(w * 0.32, h * 0.78), Offset(w * 0.68, h * 0.78), line4);
    canvas.drawLine(Offset(w * 0.40, h * 0.88), Offset(w * 0.60, h * 0.88), line4);
  }

  void _drawRippled(Canvas canvas, Size size, Color primary, Color secondary) {
    final w = size.width;
    final h = size.height;

    final stroke1 = Paint()
      ..color = primary
      ..strokeWidth = 1.6
      ..style = PaintingStyle.stroke;

    final stroke2 = Paint()
      ..color = secondary
      ..strokeWidth = 1.3
      ..style = PaintingStyle.stroke;

    final stroke3 = Paint()
      ..color = secondary.withValues(alpha: 0.7)
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;

    final stroke4 = Paint()
      ..color = secondary.withValues(alpha: 0.4)
      ..strokeWidth = 0.8
      ..style = PaintingStyle.stroke;

    // Gentle curved ripple paths
    final p1 = Path()
      ..moveTo(w * 0.05, h * 0.48)
      ..quadraticBezierTo(w * 0.18, h * 0.44, w * 0.30, h * 0.48)
      ..quadraticBezierTo(w * 0.42, h * 0.52, w * 0.55, h * 0.48)
      ..quadraticBezierTo(w * 0.68, h * 0.44, w * 0.80, h * 0.48)
      ..quadraticBezierTo(w * 0.88, h * 0.52, w * 0.95, h * 0.48);
    canvas.drawPath(p1, stroke1);

    final p2 = Path()
      ..moveTo(w * 0.10, h * 0.58)
      ..quadraticBezierTo(w * 0.25, h * 0.54, w * 0.40, h * 0.58)
      ..quadraticBezierTo(w * 0.55, h * 0.62, w * 0.70, h * 0.58)
      ..quadraticBezierTo(w * 0.80, h * 0.54, w * 0.90, h * 0.58);
    canvas.drawPath(p2, stroke2);

    final p3 = Path()
      ..moveTo(w * 0.18, h * 0.68)
      ..quadraticBezierTo(w * 0.35, h * 0.64, w * 0.52, h * 0.68)
      ..quadraticBezierTo(w * 0.68, h * 0.72, w * 0.82, h * 0.68);
    canvas.drawPath(p3, stroke3);

    final p4 = Path()
      ..moveTo(w * 0.28, h * 0.78)
      ..quadraticBezierTo(w * 0.45, h * 0.75, w * 0.62, h * 0.78)
      ..quadraticBezierTo(w * 0.70, h * 0.81, w * 0.78, h * 0.78);
    canvas.drawPath(p4, stroke4);
  }

  void _drawChoppy(Canvas canvas, Size size, Color primary, Color secondary) {
    final w = size.width;
    final h = size.height;

    final stroke1 = Paint()
      ..color = primary
      ..strokeWidth = 1.8
      ..style = PaintingStyle.stroke;

    final stroke2 = Paint()
      ..color = secondary
      ..strokeWidth = 1.4
      ..style = PaintingStyle.stroke;

    final stroke3 = Paint()
      ..color = secondary.withValues(alpha: 0.7)
      ..strokeWidth = 1.1
      ..style = PaintingStyle.stroke;

    final stroke4 = Paint()
      ..color = secondary.withValues(alpha: 0.4)
      ..strokeWidth = 0.9
      ..style = PaintingStyle.stroke;

    // Sharper, undulating choppy wave paths
    final p1 = Path()..moveTo(w * 0.05, h * 0.48);
    for (var i = 0; i < 7; i++) {
      final x1 = w * (0.05 + i * 0.13 + 0.06);
      final y1 = (i % 2 == 0) ? h * 0.41 : h * 0.52;
      final x2 = w * (0.05 + (i + 1) * 0.13);
      final y2 = h * 0.48;
      p1.quadraticBezierTo(x1, y1, x2, y2);
    }
    canvas.drawPath(p1, stroke1);

    final p2 = Path()..moveTo(w * 0.08, h * 0.58);
    for (var i = 0; i < 6; i++) {
      final x1 = w * (0.08 + i * 0.14 + 0.07);
      final y1 = (i % 2 == 0) ? h * 0.52 : h * 0.63;
      final x2 = w * (0.08 + (i + 1) * 0.14);
      final y2 = h * 0.58;
      p2.quadraticBezierTo(x1, y1, x2, y2);
    }
    canvas.drawPath(p2, stroke2);

    final p3 = Path()..moveTo(w * 0.15, h * 0.69);
    for (var i = 0; i < 5; i++) {
      final x1 = w * (0.15 + i * 0.14 + 0.07);
      final y1 = (i % 2 == 0) ? h * 0.63 : h * 0.73;
      final x2 = w * (0.15 + (i + 1) * 0.14);
      final y2 = h * 0.69;
      p3.quadraticBezierTo(x1, y1, x2, y2);
    }
    canvas.drawPath(p3, stroke3);

    final p4 = Path()..moveTo(w * 0.25, h * 0.79);
    for (var i = 0; i < 4; i++) {
      final x1 = w * (0.25 + i * 0.13 + 0.06);
      final y1 = (i % 2 == 0) ? h * 0.74 : h * 0.83;
      final x2 = w * (0.25 + (i + 1) * 0.13);
      final y2 = h * 0.79;
      p4.quadraticBezierTo(x1, y1, x2, y2);
    }
    canvas.drawPath(p4, stroke4);
  }

  @override
  bool shouldRepaint(covariant LakePainter oldDelegate) {
    return oldDelegate.state != state ||
        oldDelegate.primaryColor != primaryColor ||
        oldDelegate.secondaryColor != secondaryColor;
  }
}

/// In-app Lake widget presenting the canvas art and lake phrase.
class LakeWidgetView extends StatelessWidget {
  const LakeWidgetView({
    super.key,
    required this.lake,
    this.width = 240,
    this.height = 120,
  });

  final LakeDto lake;
  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: width,
          height: height,
          child: CustomPaint(
            key: Key('lake_canvas_${lake.state.name}'),
            painter: LakePainter(
              state: lake.state,
              primaryColor: cs.primary,
              secondaryColor: cs.secondary,
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text(
          lake.phrase,
          style: theme.textTheme.bodyMedium?.copyWith(
            fontWeight: FontWeight.w500,
          ),
          key: const Key('lake_phrase'),
        ),
      ],
    );
  }
}
