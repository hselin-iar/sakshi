// lib/features/shelf/shelf_routes.dart
// T4.5 — Route definition for the Saying Shelf feature.

import 'package:go_router/go_router.dart';
import 'package:sakshi/features/shelf/saying_picker.dart';

/// The route definition for the Saying Shelf picker screen.
final shelfRoutes = <RouteBase>[
  GoRoute(
    path: '/shelf',
    name: 'shelf',
    builder: (context, state) => const SayingPickerScreen(),
  ),
];
