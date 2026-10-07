import 'package:go_router/go_router.dart';

import 'battery_helper_screen.dart';
import 'settings_screen.dart';

// ---------------------------------------------------------------------------
// Settings routes — exported to router.dart.
// ---------------------------------------------------------------------------

final settingsRoutes = <RouteBase>[
  GoRoute(
    path: '/settings',
    builder: (context, state) => const SettingsScreen(),
  ),
  GoRoute(
    path: '/setup/battery',
    builder: (context, state) {
      // The extra field carries the HostClient so BatteryHelperScreen can call
      // markBatteryHelperShown on leave without a provider reference.
      // In practice it is always available via ref.read; the extra is unused.
      return const BatteryHelperScreen();
    },
  ),
];
