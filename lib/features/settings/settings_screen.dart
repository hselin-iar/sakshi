import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/ui_strings.dart';
import '../../host/host_client.dart';

// ---------------------------------------------------------------------------
// SettingsScreen
//
// Reads SetupStateDto for initial state of toggles. Never caches state locally
// beyond the async call; every toggle calls the host and invalidates the
// provider on success.
//
// Contents:
//   - Gentle mode switch
//   - Weekly note switch (shows one plain line if setWeeklyNote returns false)
//   - "What Sakshi sees" link
//   - "Change your apps" link → /setup/work-set
//   - "Change study hours" link → /setup/study-hours
// ---------------------------------------------------------------------------

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool _togglingGentle = false;
  bool _togglingNote = false;
  String? _noteOffLine;

  Future<void> _setGentle(bool on) async {
    setState(() => _togglingGentle = true);
    try {
      await ref.read(hostClientProvider).setGentleMode(on);
      ref.invalidate(setupStateProvider);
    } catch (_) {
      // Silent: the switch snaps back on next build from provider.
    } finally {
      if (mounted) setState(() => _togglingGentle = false);
    }
  }

  Future<void> _setWeeklyNote(bool on) async {
    setState(() {
      _togglingNote = true;
      _noteOffLine = null;
    });
    try {
      final effective = await ref.read(hostClientProvider).setWeeklyNote(on);
      if (!mounted) return;
      if (!effective && on) {
        // The host denied POST_NOTIFICATIONS — show the one plain line.
        setState(() => _noteOffLine = settingsWeeklyNoteOffLine);
      } else {
        setState(() => _noteOffLine = null);
      }
      ref.invalidate(setupStateProvider);
    } catch (_) {
      // Silent.
    } finally {
      if (mounted) setState(() => _togglingNote = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final setupAsync = ref.watch(setupStateProvider);

    return Scaffold(
      appBar: AppBar(title: const Text(titleSettings), centerTitle: false),
      body: setupAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => _ErrorBody(
          message:
              e is HostException ? e.userMessage : 'Something went wrong.',
          onRetry: () => ref.invalidate(setupStateProvider),
        ),
        data: (s) => ListView(
          children: [
            // Gentle mode.
            SwitchListTile(
              title: const Text(settingsGentleModeLabel),
              subtitle: const Text(settingsGentleModeSubtitle),
              value: s.gentleMode,
              onChanged: _togglingGentle ? null : _setGentle,
            ),
            const Divider(height: 1),

            // Weekly note.
            SwitchListTile(
              title: const Text(settingsWeeklyNoteLabel),
              subtitle: Text(
                _noteOffLine ?? settingsWeeklyNoteSubtitle,
              ),
              value: s.weeklyNoteEnabled,
              onChanged: _togglingNote ? null : _setWeeklyNote,
            ),
            const Divider(height: 1),

            // What I see link.
            ListTile(
              title: const Text(settingsWhatISeeLink),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => context.push('/what-i-see'),
            ),
            const Divider(height: 1),

            // Re-do work-set.
            ListTile(
              title: const Text(settingsRedoWorkSet),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => context.push('/setup/work-set'),
            ),
            const Divider(height: 1),

            // Re-do study hours.
            ListTile(
              title: const Text(settingsRedoStudyHours),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => context.push('/setup/study-hours'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ErrorBody extends StatelessWidget {
  const _ErrorBody({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
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
    );
  }
}
