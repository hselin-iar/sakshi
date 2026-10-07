// lib/features/mirror/mirror_routes.dart
// T4.3 (Prompt B) — Route definition for the Mirror feature.
// Exported as mirrorRoutes (List<RouteBase>) for the app router.

import 'package:go_router/go_router.dart';
import 'package:sakshi/features/mirror/mirror_screen.dart';

/// The route definition for the Weekly Mirror screen.
final mirrorRoutes = <RouteBase>[
  GoRoute(
    path: '/mirror',
    name: 'mirror',
    builder: (context, state) => const MirrorScreen(),
  ),
];
