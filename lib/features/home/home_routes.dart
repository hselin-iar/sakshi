import 'package:go_router/go_router.dart';

import 'home_screen.dart';

final homeRoutes = <RouteBase>[
  GoRoute(path: '/home', builder: (context, state) => const HomeScreen()),
];
