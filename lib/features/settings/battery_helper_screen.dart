import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/ui_strings.dart';

// ---------------------------------------------------------------------------
// BatteryHelperScreen
//
// Shown once after setup (when SetupStateDto.batteryHelperShown is false).
// Shows fixed OEM-specific steps as a short list. One button opens Android's
// battery settings page. A "Skip" button exits without penalising the user.
// Calls markBatteryHelperShown() on leave (both paths).
// Never nags: batteryHelperShown = true after the first leave.
// ---------------------------------------------------------------------------

class BatteryHelperScreen extends ConsumerStatefulWidget {
  const BatteryHelperScreen({super.key});

  @override
  ConsumerState<BatteryHelperScreen> createState() =>
      _BatteryHelperScreenState();
}

class _BatteryHelperScreenState extends ConsumerState<BatteryHelperScreen> {
  bool _leaving = false;

  Future<void> _leave(BuildContext context) async {
    if (_leaving) return;
    setState(() => _leaving = true);
    try {
      await ref.read(hostClientProvider).markBatteryHelperShown();
      ref.invalidate(setupStateProvider);
    } catch (_) {
      // Silent — the screen is gone regardless.
    }
    if (context.mounted) context.go('/');
  }

  Future<void> _openAndLeave(BuildContext context) async {
    // Open the OS battery settings first, then mark shown.
    await ref.read(hostClientProvider).openBatterySettings();
    if (context.mounted) _leave(context);
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      // Intercept back button — still call markBatteryHelperShown.
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _leave(context);
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Battery settings'),
          centerTitle: false,
          automaticallyImplyLeading: false,
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  batteryHelperIntro,
                  style: Theme.of(context).textTheme.bodyLarge,
                ),
                const SizedBox(height: 24),

                // OEM sections — iQOO/Vivo and Nothing first (spec).
                const _OemSection(
                  title: batteryOemIqooTitle,
                  steps: batteryOemIqooSteps,
                ),
                const _OemSection(
                  title: batteryOemNothingTitle,
                  steps: batteryOemNothingSteps,
                ),
                const _OemSection(
                  title: batteryOemOneplusTitle,
                  steps: batteryOemOneplusSteps,
                ),
                const _OemSection(
                  title: batteryOemXiaomiTitle,
                  steps: batteryOemXiaomiSteps,
                ),
                const _OemSection(
                  title: batteryOemSamsungTitle,
                  steps: batteryOemSamsungSteps,
                ),
                const _OemSection(
                  title: batteryOemOtherTitle,
                  steps: batteryOemOtherSteps,
                ),

                const SizedBox(height: 32),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: _leaving ? null : () => _openAndLeave(context),
                    child: const Text('Open battery settings'),
                  ),
                ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: TextButton(
                    onPressed: _leaving ? null : () => _leave(context),
                    child: const Text('Skip'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _OemSection extends StatelessWidget {
  const _OemSection({required this.title, required this.steps});

  final String title;
  final String steps;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title,
            style: Theme.of(context).textTheme.titleSmall
                ?.copyWith(color: Theme.of(context).colorScheme.primary),
          ),
          const SizedBox(height: 6),
          Text(steps, style: Theme.of(context).textTheme.bodyMedium),
        ],
      ),
    );
  }
}
