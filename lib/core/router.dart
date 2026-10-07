import 'package:go_router/go_router.dart';

import '../features/setup/setup_routes.dart';
import '../features/what_i_see/what_i_see_routes.dart';
import '../features/settings/settings_routes.dart';
import '../features/mirror/mirror_routes.dart';
import '../features/today/today_routes.dart';
import '../features/shelf/shelf_routes.dart';
import '../features/demo/demo_routes.dart';
import '../features/lake/lake_routes.dart';
import '../features/home/home_routes.dart';
import '../features/ask/ask_routes.dart';

// ---------------------------------------------------------------------------
// Application router.
// Route lists are declared in each feature folder; this file only assembles
// them. No route logic lives here — each feature owns its own paths.
// ---------------------------------------------------------------------------

// [REFACTOR CANDIDATE: extract initialLocation constant when setup flow lands]
const _initialLocation = '/';

final appRouter = GoRouter(
  initialLocation: _initialLocation,
  routes: [
    ...setupRoutes,
    ...homeRoutes,
    ...askRoutes,
    ...whatISeeRoutes,
    ...settingsRoutes,
    ...mirrorRoutes,
    ...lakeRoutes,
    ...todayRoutes,
    ...shelfRoutes,
    ...demoRoutes,
  ],
);
