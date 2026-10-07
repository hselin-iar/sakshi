// lib/features/demo/demo_routes.dart
// T4.6 — Route definition for the Time Machine demo.

import 'package:go_router/go_router.dart';
import 'package:sakshi/features/demo/demo_screen.dart';

/// The route definition for the Time Machine demo screen.
final demoRoutes = <RouteBase>[
  GoRoute(
    path: '/demo',
    name: 'demo',
    builder: (context, state) => const DemoScreen(),
  ),
];
