import 'package:go_router/go_router.dart';

import 'what_i_see_screen.dart';

// ---------------------------------------------------------------------------
// What I See routes — exported to router.dart.
// ---------------------------------------------------------------------------

final whatISeeRoutes = <RouteBase>[
  GoRoute(
    path: '/what-i-see',
    builder: (context, state) => const WhatISeeScreen(),
  ),
];
