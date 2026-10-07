import 'package:flutter/material.dart';

// ---------------------------------------------------------------------------
// Sakshi colour palette — calm, low-saturation tones.
// No harsh reds, no bright blues. Uses Material 3 colour scheme.
// ---------------------------------------------------------------------------

const _seed = Color(0xFF5B7FA6); // muted slate-blue — calming, neutral

ColorScheme _lightScheme() =>
    ColorScheme.fromSeed(seedColor: _seed, brightness: Brightness.light);

ColorScheme _darkScheme() =>
    ColorScheme.fromSeed(seedColor: _seed, brightness: Brightness.dark);

ThemeData sakshiLight() => ThemeData(
  useMaterial3: true,
  colorScheme: _lightScheme(),
  // System font used; Devanagari glyphs for "साक्षी" are in the OS font.
  typography: Typography.material2021(),
);

ThemeData sakshiDark() => ThemeData(
  useMaterial3: true,
  colorScheme: _darkScheme(),
  typography: Typography.material2021(),
);
