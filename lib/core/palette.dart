import 'package:flutter/painting.dart';

/// The colours of the Sakshi poster: the brown ground, the maroon robe, the gold circle, the saffron turban, the cream lettering.
/// Screens take their colours from the theme (lib/core/theme.dart); the hero banners and the introduction use these directly, because
/// they are the poster itself and look the same in light and dark.
abstract final class SakshiColors {
  static const deepBrown = Color(0xFF3A1D0D);
  static const brown = Color(0xFF653618);
  static const maroon = Color(0xFF7A2B2E);
  static const saffron = Color(0xFFD9622B);
  static const gold = Color(0xFFD98F3A);
  static const paleGold = Color(0xFFE9B873);
  static const cream = Color(0xFFFDF3E1);
  static const parchment = Color(0xFFFFF8EA);
}
