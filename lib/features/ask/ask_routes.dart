import 'package:go_router/go_router.dart';

import 'ask_screen.dart';

final askRoutes = <RouteBase>[
  GoRoute(path: '/ask', builder: (context, state) => const AskScreen()),
];
