import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../host/host_client.dart';
import 'age_tap.dart';
import '../settings/battery_helper_screen.dart';
import 'first_look_screen.dart';
import 'first_screen.dart';
import 'permission_steps.dart';
import 'study_hours_screen.dart';
import 'work_set_screen.dart';

// ---------------------------------------------------------------------------
// Setup routes.
//
// Routing logic:
//   '/'                  → SetupGate (decides where the user is in the flow)
//   '/setup/first'       → FirstScreen
//   '/setup/usage'       → UsageAccessStep
//   '/setup/notif'       → NotificationAccessStep
//   '/setup/work-set'    → WorkSetScreen
//   '/setup/study-hours' → StudyHoursScreen
//   '/setup/age'         → AgeTap
//
// Onboarding is first -> usage -> notification (skippable) -> one question -> '/home'.
// '/home' is the dashboard every later launch lands on. The work set, study hours and
// the battery note are cards on Home, not steps in a flow.
// ---------------------------------------------------------------------------

const _pathRoot = '/';
const _pathFirst = '/setup/first';
const _pathUsage = '/setup/usage';
const _pathNotif = '/setup/notif';
const _pathWorkSet = '/setup/work-set';
const _pathStudyHours = '/setup/study-hours';
const _pathAge = '/setup/age';
const _pathBattery = '/setup/battery';
const _pathFirstLook = '/setup/first-look';
const _pathHome = '/home';

/// Routes exported to router.dart.
final setupRoutes = <RouteBase>[
  GoRoute(path: _pathRoot, builder: (context, state) => const SetupGate()),
  GoRoute(path: _pathFirst, builder: (context, state) => const FirstScreen()),
  GoRoute(
    path: _pathUsage,
    builder: (context, state) => const UsageAccessStep(),
  ),
  GoRoute(
    path: _pathNotif,
    builder: (context, state) => const NotificationAccessStep(),
  ),
  GoRoute(
    path: _pathWorkSet,
    builder: (context, state) => const WorkSetScreen(),
  ),
  GoRoute(
    path: _pathStudyHours,
    builder: (context, state) => const StudyHoursScreen(),
  ),
  GoRoute(path: _pathAge, builder: (context, state) => const AgeTap()),
  GoRoute(
    path: _pathBattery,
    builder: (context, state) => const BatteryHelperScreen(),
  ),
  GoRoute(
    path: _pathFirstLook,
    builder: (context, state) => const FirstLookScreen(),
  ),
];

// ---------------------------------------------------------------------------
// SetupGate — reads SetupStateDto and redirects to the right step.
// Shows a loading spinner while the provider is resolving.
// ---------------------------------------------------------------------------

class SetupGate extends ConsumerWidget {
  const SetupGate({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final setupAsync = ref.watch(setupStateProvider);

    return setupAsync.when(
      loading: () =>
          const Scaffold(body: Center(child: CircularProgressIndicator())),
      error: (e, _) => _ErrorView(
        message: e is HostException ? e.userMessage : 'Something went wrong.',
        onRetry: () => ref.invalidate(setupStateProvider),
      ),
      data: (s) {
        // Determine the next required step and redirect.
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (!context.mounted) return;
          _redirect(context, s);
        });
        return const Scaffold(body: Center(child: CircularProgressIndicator()));
      },
    );
  }

  /// Usage access is the only thing the app cannot do without. Everything else (notification access, the work set, study hours,
  /// the battery note) is offered on Home and never forced, so there is nothing for the gate to loop on.
  static void _redirect(BuildContext context, SetupStateDto s) {
    context.go(s.usageAccessGranted ? _pathHome : _pathFirst);
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: 24),
              FilledButton(onPressed: onRetry, child: const Text('Try again')),
            ],
          ),
        ),
      ),
    );
  }
}
