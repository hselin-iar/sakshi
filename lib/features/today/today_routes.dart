// lib/features/today/today_routes.dart
// T4.4 — Route definition for the Today feature.

import 'package:go_router/go_router.dart';
import 'package:sakshi/features/today/today_screen.dart';

/// The route definition for the Today so far screen.
final todayRoutes = <RouteBase>[
  GoRoute(
    path: '/today',
    name: 'today',
    builder: (context, state) => const TodayScreen(),
  ),
];
