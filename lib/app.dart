import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/providers.dart';
import 'core/router.dart';
import 'core/theme.dart';
import 'features/demo/demo_banner.dart';

// ---------------------------------------------------------------------------
// SakshiApp — root widget.
//
// DemoBanner is rendered inside MaterialApp (via the router's builder) so
// that Directionality, MediaQuery and Overlay are all available above it.
// The banner is mounted whenever setupState.isDemo is true (DOC 3 v1.1 CA-2).
// ---------------------------------------------------------------------------

class SakshiApp extends ConsumerWidget {
  const SakshiApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final setupAsync = ref.watch(setupStateProvider);

    // isDemo defaults to false while loading or on error — no banner flash.
    final isDemo = setupAsync.maybeWhen(
      data: (s) => s.isDemo,
      orElse: () => false,
    );

    return MaterialApp.router(
      title: 'Sakshi',
      theme: sakshiLight(),
      darkTheme: sakshiDark(),
      themeMode: ThemeMode.system,
      routerConfig: appRouter,
      // The permanent "Demo data" banner sits above every page whenever isDemo is true.
      builder: isDemo
          ? (context, child) => Column(
              children: [
                const DemoBanner(),
                Expanded(child: child ?? const SizedBox()),
              ],
            )
          : null,
    );
  }
}
