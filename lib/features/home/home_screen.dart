import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/ui_strings.dart';
import '../../host/host_client.dart';
import '../demo/demo_launch.dart';
import '../lake/lake_screen.dart';
import '../mirror/mirror_screen.dart';
import '../today/today_screen.dart';

// ---------------------------------------------------------------------------
// HomeScreen — the dashboard every launch lands on once usage access is
// granted. It greets, reads what Android has recorded since the last visit,
// and shows where things stand. Nothing here is forced: the work set,
// study hours, notification access and the battery note are cards the user
// can act on or leave. No logic about attention lives here; every sentence
// about the user's data comes from the host.
// ---------------------------------------------------------------------------

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen>
    with WidgetsBindingObserver {
  bool _syncing = false;
  String? _syncMessage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    // Read once on arrival so the first visit already has something to show.
    WidgetsBinding.instance.addPostFrameCallback((_) => _sync());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  // Coming back from Android's settings (permissions, battery): re-read the state.
  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refreshAll();
  }

  void _refreshAll() {
    ref.invalidate(setupStateProvider);
    ref.invalidate(lakeProvider);
    ref.invalidate(todayProvider);
    ref.invalidate(mirrorProvider);
  }

  Future<void> _sync() async {
    if (_syncing) return;
    setState(() {
      _syncing = true;
      _syncMessage = null;
    });
    try {
      final status = await ref.read(hostClientProvider).syncNow();
      if (!mounted) return;
      _syncMessage = status.message;
    } catch (e) {
      if (!mounted) return;
      _syncMessage = e is HostException ? e.userMessage : null;
    }
    if (!mounted) return;
    _refreshAll();
    setState(() => _syncing = false);
  }

  String _greeting() {
    final hour = DateTime.now().hour;
    if (hour < 12) return homeGreetingMorning;
    if (hour < 17) return homeGreetingAfternoon;
    return homeGreetingEvening;
  }

  @override
  Widget build(BuildContext context) {
    final setupAsync = ref.watch(setupStateProvider);
    final textTheme = Theme.of(context).textTheme;
    final colors = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: const Text(titleHome),
        centerTitle: false,
        actions: [
          IconButton(
            tooltip: homeSettingsTitle,
            icon: const Icon(Icons.settings_outlined),
            onPressed: () => context.push('/settings'),
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _sync,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(4, 8, 4, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(_greeting(), style: textTheme.headlineMedium),
                    const SizedBox(height: 4),
                    Text(
                      _syncing ? homeSyncing : (_syncMessage ?? homeSubtitle),
                      style: textTheme.bodyMedium?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                    if (_syncing) ...[
                      const SizedBox(height: 12),
                      const LinearProgressIndicator(),
                    ],
                  ],
                ),
              ),
              setupAsync.when(
                loading: () => const Padding(
                  padding: EdgeInsets.all(32),
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (e, _) => _ErrorCard(
                  message: e is HostException
                      ? e.userMessage
                      : 'Something went wrong.',
                  onRetry: () => ref.invalidate(setupStateProvider),
                ),
                data: (s) => _Body(setup: s, onSync: _sync, syncing: _syncing),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Body extends ConsumerWidget {
  const _Body({
    required this.setup,
    required this.onSync,
    required this.syncing,
  });

  final SetupStateDto setup;
  final VoidCallback onSync;
  final bool syncing;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lake = ref.watch(lakeProvider);
    final today = ref.watch(todayProvider);
    final mirror = ref.watch(mirrorProvider(null));

    String? lakeText = lake.maybeWhen(
      data: (l) => l.phrase.isEmpty ? null : l.phrase,
      orElse: () => null,
    );
    String? todayText = today.maybeWhen(
      data: (t) => t.line.isEmpty ? null : t.line,
      orElse: () => null,
    );
    String? mirrorText = mirror.maybeWhen(
      data: (m) => m.headline.isEmpty ? null : m.headline,
      orElse: () => null,
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (setup.isDemo)
          _DemoBar(
            onMove: () => context.push('/demo'),
            onExit: () => exitDemo(ref),
          )
        else ...[
          if (!setup.usageAccessGranted)
            _PromptCard(
              icon: Icons.lock_open_outlined,
              title: homeUsageNeededTitle,
              body: homeUsageNeededBody,
              buttonLabel: homeUsageNeededButton,
              onPressed: () => context.push('/setup/usage'),
            )
          else if (!setup.workSetSaved)
            _PromptCard(
              icon: Icons.apps_outlined,
              title: homeWorkSetNeededTitle,
              body: homeWorkSetNeededBody,
              buttonLabel: homeWorkSetNeededButton,
              onPressed: () => context.push('/setup/work-set'),
            ),
          _TryDemoCard(onTry: () => enterDemo(ref)),
        ],
        _InfoCard(
          icon: Icons.bar_chart_outlined,
          title: homeMirrorTitle,
          text: mirrorText,
          loading: mirror.isLoading,
          onTap: () => context.push('/mirror'),
        ),
        _InfoCard(
          icon: Icons.today_outlined,
          title: homeTodayTitle,
          text: todayText,
          loading: today.isLoading,
          onTap: () => context.push('/today'),
        ),
        _InfoCard(
          icon: Icons.water_outlined,
          title: homeLakeTitle,
          text: lakeText,
          loading: lake.isLoading,
          onTap: () => context.push('/lake'),
        ),
        const SizedBox(height: 8),
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton.icon(
            onPressed: syncing ? null : onSync,
            icon: const Icon(Icons.refresh),
            label: const Text(homeSyncNow),
          ),
        ),
        if (!setup.isDemo) ...[
          const _Heading(homeSetupHeading),
          if (setup.workSetSaved)
            _LinkTile(
              icon: Icons.apps_outlined,
              title: homeWorkSetTitle,
              body: homeWorkSetBody,
              onTap: () => context.push('/setup/work-set'),
            ),
          _LinkTile(
            icon: Icons.schedule_outlined,
            title: homeStudyHoursTitle,
            body: homeStudyHoursBody,
            onTap: () => context.push('/setup/study-hours'),
          ),
          if (!setup.notificationAccessGranted)
            _LinkTile(
              icon: Icons.notifications_none_outlined,
              title: homeNotifTitle,
              body: homeNotifBody,
              onTap: () => context.push('/setup/notif'),
            ),
          if (!setup.batteryHelperShown)
            _LinkTile(
              icon: Icons.battery_charging_full_outlined,
              title: homeBatteryTitle,
              body: homeBatteryBody,
              onTap: () => context.push('/setup/battery'),
            ),
        ],
        const _Heading(homeExploreHeading),
        _LinkTile(
          icon: Icons.visibility_outlined,
          title: homeWhatISeeTitle,
          body: homeWhatISeeBody,
          onTap: () => context.push('/what-i-see'),
        ),
      ],
    );
  }
}

/// Shown above everything while a demo runs: what this is, how to move through it, how to leave.
class _DemoBar extends StatelessWidget {
  const _DemoBar({required this.onMove, required this.onExit});

  final VoidCallback onMove;
  final VoidCallback onExit;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      key: const Key('home_demo_bar'),
      color: colors.tertiaryContainer,
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              demoActiveTitle,
              style: Theme.of(context).textTheme.titleMedium
                  ?.copyWith(color: colors.onTertiaryContainer),
            ),
            const SizedBox(height: 6),
            Text(
              demoActiveBody,
              style: Theme.of(context).textTheme.bodyMedium
                  ?.copyWith(color: colors.onTertiaryContainer),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: [
                FilledButton(
                  onPressed: onMove,
                  child: const Text(demoMoveButton),
                ),
                OutlinedButton(
                  key: const Key('home_exit_demo'),
                  onPressed: onExit,
                  child: const Text(demoExitButton),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// One tap into a populated demo. Needs no permission, so it is the thing to show before anything is set up.
class _TryDemoCard extends StatefulWidget {
  const _TryDemoCard({required this.onTry});

  final Future<void> Function() onTry;

  @override
  State<_TryDemoCard> createState() => _TryDemoCardState();
}

class _TryDemoCardState extends State<_TryDemoCard> {
  bool _starting = false;

  Future<void> _go() async {
    setState(() => _starting = true);
    try {
      await widget.onTry();
    } finally {
      if (mounted) setState(() => _starting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      key: const Key('home_try_demo'),
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(demoTryTitle, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 6),
            Text(demoTryBody, style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: 12),
            FilledButton.tonal(
              key: const Key('home_try_demo_button'),
              onPressed: _starting ? null : _go,
              child: Text(_starting ? demoStarting : demoTryButton),
            ),
          ],
        ),
      ),
    );
  }
}

class _Heading extends StatelessWidget {
  const _Heading(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.fromLTRB(4, 20, 4, 8),
    child: Text(text, style: Theme.of(context).textTheme.titleSmall),
  );
}

class _PromptCard extends StatelessWidget {
  const _PromptCard({
    required this.icon,
    required this.title,
    required this.body,
    required this.buttonLabel,
    required this.onPressed,
  });

  final IconData icon;
  final String title;
  final String body;
  final String buttonLabel;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      color: colors.primaryContainer,
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, color: colors.onPrimaryContainer),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    title,
                    style: Theme.of(context).textTheme.titleMedium
                        ?.copyWith(color: colors.onPrimaryContainer),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              body,
              style: Theme.of(context).textTheme.bodyMedium
                  ?.copyWith(color: colors.onPrimaryContainer),
            ),
            const SizedBox(height: 12),
            FilledButton(onPressed: onPressed, child: Text(buttonLabel)),
          ],
        ),
      ),
    );
  }
}

/// A card that shows one sentence from the host, or a quiet placeholder while it loads or when there is nothing to say yet.
class _InfoCard extends StatelessWidget {
  const _InfoCard({
    required this.icon,
    required this.title,
    required this.text,
    required this.loading,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String? text;
  final bool loading;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final textTheme = Theme.of(context).textTheme;
    final colors = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(icon, color: colors.primary),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: textTheme.titleMedium),
                    const SizedBox(height: 4),
                    if (text != null)
                      Text(text!, style: textTheme.bodyMedium)
                    else if (loading)
                      const Padding(
                        padding: EdgeInsets.only(top: 6),
                        child: SizedBox(
                          height: 2,
                          child: LinearProgressIndicator(),
                        ),
                      ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right, color: colors.outline),
            ],
          ),
        ),
      ),
    );
  }
}

class _LinkTile extends StatelessWidget {
  const _LinkTile({
    required this.icon,
    required this.title,
    required this.body,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String body;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: const EdgeInsets.symmetric(horizontal: 4),
    leading: Icon(icon),
    title: Text(title),
    subtitle: Text(body),
    trailing: const Icon(Icons.chevron_right),
    onTap: onTap,
  );
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton(onPressed: onRetry, child: const Text(homeErrorRetry)),
        ],
      ),
    ),
  );
}
