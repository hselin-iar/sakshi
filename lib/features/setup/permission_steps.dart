import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/ui_strings.dart';
import '../../host/host_client.dart';

// ---------------------------------------------------------------------------
// UsageAccessStep
//
// Shows a status line (granted / not granted) and an "Open settings" button.
// Re-fetches SetupStateDto on app resume via WidgetsBindingObserver — never
// trusts an assumed grant.
// Advances to '/setup/notif' when usageAccessGranted becomes true.
// ---------------------------------------------------------------------------

class UsageAccessStep extends ConsumerStatefulWidget {
  const UsageAccessStep({super.key});

  @override
  ConsumerState<UsageAccessStep> createState() => _UsageAccessStepState();
}

class _UsageAccessStepState extends ConsumerState<UsageAccessStep>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  // Re-read setup state whenever the user returns from Android settings.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      ref.invalidate(setupStateProvider);
    }
  }

  @override
  Widget build(BuildContext context) {
    final setupAsync = ref.watch(setupStateProvider);

    return setupAsync.when(
      loading: () => const _PermissionScaffold(
        title: titleUsageAccess,
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => _PermissionScaffold(
        title: titleUsageAccess,
        child: _ErrorRetry(
          message: e is HostException ? e.userMessage : 'Something went wrong.',
          onRetry: () => ref.invalidate(setupStateProvider),
        ),
      ),
      data: (s) {
        // Advance as soon as the grant is confirmed.
        if (s.usageAccessGranted) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (context.mounted) context.go('/setup/notif');
          });
        }

        return _PermissionScaffold(
          title: titleUsageAccess,
          child: _UsageContent(granted: s.usageAccessGranted),
        );
      },
    );
  }
}

class _UsageContent extends ConsumerWidget {
  const _UsageContent({required this.granted});

  final bool granted;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Sakshi reads which apps are open and when, using Android\'s '
            'Usage Access API. It never reads what you do inside an app.',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 24),
          _StatusLine(granted: granted, label: 'Usage access'),
          const SizedBox(height: 32),
          if (!granted)
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () =>
                    ref.read(hostClientProvider).openUsageAccessSettings(),
                child: const Text('Open settings'),
              ),
            ),
          if (granted)
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => context.go('/setup/notif'),
                child: const Text('Continue'),
              ),
            ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// NotificationAccessStep
//
// Optional step. Shows a one-line "why" (Sakshi reads app and category, never
// content). When restrictedSettingsSuspected, shows the three-step App Info
// path once. The user can skip this step entirely.
// ---------------------------------------------------------------------------

class NotificationAccessStep extends ConsumerStatefulWidget {
  const NotificationAccessStep({super.key});

  @override
  ConsumerState<NotificationAccessStep> createState() =>
      _NotificationAccessStepState();
}

class _NotificationAccessStepState extends ConsumerState<NotificationAccessStep>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      ref.invalidate(setupStateProvider);
    }
  }

  @override
  Widget build(BuildContext context) {
    final setupAsync = ref.watch(setupStateProvider);

    return setupAsync.when(
      loading: () => const _PermissionScaffold(
        title: titleNotificationAccess,
        child: Center(child: CircularProgressIndicator()),
      ),
      error: (e, _) => _PermissionScaffold(
        title: titleNotificationAccess,
        child: _ErrorRetry(
          message: e is HostException ? e.userMessage : 'Something went wrong.',
          onRetry: () => ref.invalidate(setupStateProvider),
        ),
      ),
      data: (s) => _PermissionScaffold(
        title: titleNotificationAccess,
        child: _NotifContent(
          granted: s.notificationAccessGranted,
          restrictedSuspected: s.restrictedSettingsSuspected,
        ),
      ),
    );
  }
}

class _NotifContent extends ConsumerWidget {
  const _NotifContent({
    required this.granted,
    required this.restrictedSuspected,
  });

  final bool granted;
  final bool restrictedSuspected;

  // Next step after notification: work-set (T3.4).
  static const _nextPath = '/setup/work-set';

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Sakshi can see which apps send notifications and when. '
            'It never reads notification titles, text or content.',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 24),
          _StatusLine(granted: granted, label: 'Notification access'),
          const SizedBox(height: 24),

          // Show the restricted-settings help when suspected — shown once
          // (the screen is never re-shown after the user returns granted).
          if (restrictedSuspected && !granted) ...[
            const _RestrictedSettingsHelp(),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => ref
                    .read(hostClientProvider)
                    .openAppInfoForRestrictedSettings(),
                child: const Text('Open App Info'),
              ),
            ),
            const SizedBox(height: 12),
          ],

          if (!restrictedSuspected && !granted) ...[
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => ref
                    .read(hostClientProvider)
                    .openNotificationAccessSettings(),
                child: const Text('Open settings'),
              ),
            ),
            const SizedBox(height: 12),
          ],

          // Skip is always available — notification access is optional.
          SizedBox(
            width: double.infinity,
            child: TextButton(
              onPressed: () => context.go(_nextPath),
              child: const Text('Skip for now'),
            ),
          ),

          if (granted) ...[
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () => context.go(_nextPath),
                child: const Text('Continue'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Restricted-settings help (shown when restrictedSettingsSuspected is true).
// Three steps the user must follow on Android 13+ sideloaded APKs.
// Content is chrome (not data), so it lives here per LC-8.
// ---------------------------------------------------------------------------

class _RestrictedSettingsHelp extends StatelessWidget {
  const _RestrictedSettingsHelp();

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
      ),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Notification access is restricted on this device because the app '
            'was installed from a file. Follow these steps:',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 12),
          const _Step(
            number: '1',
            text: 'Tap "Open App Info" below to open Sakshi\'s app page.',
          ),
          const SizedBox(height: 8),
          const _Step(
            number: '2',
            text: 'Tap the three-dot menu and choose "Allow restricted settings".',
          ),
          const SizedBox(height: 8),
          const _Step(
            number: '3',
            text: 'Go back and tap "Open settings" to enable notification access.',
          ),
        ],
      ),
    );
  }
}

class _Step extends StatelessWidget {
  const _Step({required this.number, required this.text});

  final String number;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        CircleAvatar(
          radius: 12,
          backgroundColor: Theme.of(context).colorScheme.primary,
          foregroundColor: Theme.of(context).colorScheme.onPrimary,
          child: Text(
            number,
            style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(text, style: Theme.of(context).textTheme.bodyMedium),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Shared small widgets used only within the setup feature.
// ---------------------------------------------------------------------------

class _PermissionScaffold extends StatelessWidget {
  const _PermissionScaffold({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title), centerTitle: false),
      body: SingleChildScrollView(child: child),
    );
  }
}

class _StatusLine extends StatelessWidget {
  const _StatusLine({required this.granted, required this.label});

  final bool granted;
  final String label;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    return Row(
      children: [
        Icon(
          granted ? Icons.check_circle_outline : Icons.radio_button_unchecked,
          color: granted ? colorScheme.primary : colorScheme.outline,
          size: 20,
        ),
        const SizedBox(width: 8),
        Text(
          granted ? '$label: granted' : '$label: not yet granted',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: granted ? colorScheme.primary : colorScheme.onSurface,
          ),
        ),
      ],
    );
  }
}

class _ErrorRetry extends StatelessWidget {
  const _ErrorRetry({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 24),
          FilledButton(onPressed: onRetry, child: const Text('Try again')),
        ],
      ),
    );
  }
}
