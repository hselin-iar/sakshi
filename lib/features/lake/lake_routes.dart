// lib/features/lake/lake_routes.dart
// T4.4 — Route definition for the Lake feature.

import 'package:go_router/go_router.dart';
import 'package:sakshi/features/lake/lake_screen.dart';

/// The route definition for the Lake screen.
final lakeRoutes = <RouteBase>[
  GoRoute(
    path: '/lake',
    name: 'lake',
    builder: (context, state) => const LakeScreen(),
  ),
];
