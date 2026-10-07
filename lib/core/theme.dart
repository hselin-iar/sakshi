import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'palette.dart';

// ---------------------------------------------------------------------------
// Sakshi theme — the poster's palette made usable.
//
// Light: parchment ground, deep-brown text, maroon for actions, gold for accents; the top bar is the poster's brown.
// Dark: the poster itself: deep-brown ground, cream text, gold for actions, maroon for emphasis.
// Every pair of text and ground below is dark-on-light or light-on-dark by a wide margin; nothing relies on colour alone.
// Headings use the system serif (as the poster does); body text stays in the system sans for reading.
// ---------------------------------------------------------------------------

const _serif = 'serif';

ColorScheme _lightScheme() => const ColorScheme(
  brightness: Brightness.light,
  primary: SakshiColors.maroon,
  onPrimary: SakshiColors.parchment,
  primaryContainer: Color(0xFFF7DFA6),
  onPrimaryContainer: Color(0xFF4A1416),
  secondary: Color(0xFF9A5A12),
  onSecondary: SakshiColors.parchment,
  secondaryContainer: Color(0xFFFBE3B0),
  onSecondaryContainer: Color(0xFF4A2A05),
  tertiary: SakshiColors.brown,
  onTertiary: SakshiColors.parchment,
  tertiaryContainer: Color(0xFFF0D9B6),
  onTertiaryContainer: SakshiColors.deepBrown,
  error: Color(0xFFB3261E),
  onError: Color(0xFFFFFFFF),
  errorContainer: Color(0xFFF9DEDC),
  onErrorContainer: Color(0xFF410E0B),
  surface: SakshiColors.parchment,
  onSurface: Color(0xFF2B1609),
  onSurfaceVariant: Color(0xFF5C4631),
  outline: Color(0xFF8C6A4A),
  outlineVariant: Color(0xFFE2CBA3),
  surfaceDim: Color(0xFFEFDDB8),
  surfaceBright: SakshiColors.parchment,
  surfaceContainerLowest: Color(0xFFFFFDF6),
  surfaceContainerLow: Color(0xFFFFF4DD),
  surfaceContainer: Color(0xFFFCEBCB),
  surfaceContainerHigh: Color(0xFFF6E1B9),
  surfaceContainerHighest: Color(0xFFF0D7AA),
  inverseSurface: SakshiColors.deepBrown,
  onInverseSurface: SakshiColors.cream,
  inversePrimary: SakshiColors.paleGold,
  shadow: Color(0xFF000000),
  scrim: Color(0xFF000000),
  surfaceTint: SakshiColors.maroon,
);

ColorScheme _darkScheme() => const ColorScheme(
  brightness: Brightness.dark,
  primary: SakshiColors.paleGold,
  onPrimary: SakshiColors.deepBrown,
  primaryContainer: SakshiColors.maroon,
  onPrimaryContainer: Color(0xFFFFDDB5),
  secondary: Color(0xFFF0955A),
  onSecondary: Color(0xFF3A1500),
  secondaryContainer: Color(0xFF6B3A16),
  onSecondaryContainer: Color(0xFFFFDCC2),
  tertiary: SakshiColors.gold,
  onTertiary: SakshiColors.deepBrown,
  tertiaryContainer: Color(0xFF5A3A1E),
  onTertiaryContainer: SakshiColors.cream,
  error: Color(0xFFFFB4AB),
  onError: Color(0xFF690005),
  errorContainer: Color(0xFF93000A),
  onErrorContainer: Color(0xFFFFDAD6),
  surface: Color(0xFF2A150A),
  onSurface: SakshiColors.cream,
  onSurfaceVariant: Color(0xFFD9C2A3),
  outline: Color(0xFFA38A6C),
  outlineVariant: Color(0xFF5A4330),
  surfaceDim: Color(0xFF22110A),
  surfaceBright: Color(0xFF4A2C1B),
  surfaceContainerLowest: Color(0xFF1E0F07),
  surfaceContainerLow: Color(0xFF33190C),
  surfaceContainer: Color(0xFF3A1E10),
  surfaceContainerHigh: Color(0xFF45281A),
  surfaceContainerHighest: Color(0xFF513121),
  inverseSurface: SakshiColors.cream,
  onInverseSurface: SakshiColors.deepBrown,
  inversePrimary: SakshiColors.maroon,
  shadow: Color(0xFF000000),
  scrim: Color(0xFF000000),
  surfaceTint: SakshiColors.paleGold,
);

ThemeData _build(ColorScheme s) {
  final base = ThemeData(
    useMaterial3: true,
    colorScheme: s,
    typography: Typography.material2021(),
  );
  final t = base.textTheme;
  TextStyle? serif(TextStyle? x, [FontWeight? weight]) =>
      x?.copyWith(fontFamily: _serif, fontWeight: weight);

  final dark = s.brightness == Brightness.dark;
  final barColor = dark ? SakshiColors.deepBrown : SakshiColors.brown;
  const shape = StadiumBorder();

  return base.copyWith(
    scaffoldBackgroundColor: s.surface,
    textTheme: t.copyWith(
      displayLarge: serif(t.displayLarge),
      displayMedium: serif(t.displayMedium),
      displaySmall: serif(t.displaySmall),
      headlineLarge: serif(t.headlineLarge),
      headlineMedium: serif(t.headlineMedium),
      headlineSmall: serif(t.headlineSmall),
      titleLarge: serif(t.titleLarge, FontWeight.w600),
    ),
    appBarTheme: AppBarTheme(
      backgroundColor: barColor,
      foregroundColor: SakshiColors.cream,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
      iconTheme: const IconThemeData(color: SakshiColors.cream),
      actionsIconTheme: const IconThemeData(color: SakshiColors.cream),
      titleTextStyle: const TextStyle(
        fontFamily: _serif,
        fontSize: 22,
        fontWeight: FontWeight.w600,
        color: SakshiColors.cream,
      ),
      systemOverlayStyle: SystemUiOverlayStyle.light,
    ),
    cardTheme: CardThemeData(
      color: dark ? s.surfaceContainer : s.surfaceContainerLowest,
      surfaceTintColor: Colors.transparent,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(color: s.outlineVariant),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(64, 52),
        shape: shape,
        textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(64, 52),
        shape: shape,
        side: BorderSide(color: s.primary),
        textStyle: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        textStyle: const TextStyle(fontWeight: FontWeight.w600),
      ),
    ),
    listTileTheme: ListTileThemeData(
      iconColor: s.primary,
      textColor: s.onSurface,
    ),
    dividerTheme: DividerThemeData(color: s.outlineVariant, space: 1),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: s.inverseSurface,
      contentTextStyle: TextStyle(color: s.onInverseSurface),
    ),
    chipTheme: ChipThemeData(
      selectedColor: s.primaryContainer,
      side: BorderSide(color: s.outlineVariant),
    ),
  );
}

ThemeData sakshiLight() => _build(_lightScheme());

ThemeData sakshiDark() => _build(_darkScheme());
