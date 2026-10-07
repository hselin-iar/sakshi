import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../host/host_client.dart';

// ---------------------------------------------------------------------------
// FirstLookScreen — F1. Instant First Look (DOC 3 §F1).
//
// Calls syncNow() then getMirror(null). Shows a progress state during sync.
// Renders headline, data lines and the Parts card from Track 4's widgets.
//
// Stub strategy: until Track 4 publishes parts_card.dart (T4.2), a local
// _PartsCardStub is used with the same constructor signature.
// Delete _PartsCardStub when the real PartsCard is imported.
//
// Import path when T4.2 lands (replace the stub import with):
//   import '../../mirror/parts_card.dart';
// ---------------------------------------------------------------------------

class FirstLookScreen extends ConsumerStatefulWidget {
  const FirstLookScreen({super.key});

  @override
  ConsumerState<FirstLookScreen> createState() => _FirstLookScreenState();
}

class _FirstLookScreenState extends ConsumerState<FirstLookScreen> {
  bool _syncing = true;
  MirrorDto? _mirror;
  String? _error;

  @override
  void initState() {
    super.initState();
    _sync();
  }

  Future<void> _sync() async {
    setState(() {
      _syncing = true;
      _error = null;
    });
    try {
      await ref.read(hostClientProvider).syncNow();
      final mirror = await ref.read(hostClientProvider).getMirror(null);
      if (!mounted) return;
      setState(() {
        _mirror = mirror;
        _syncing = false;
      });
      // Invalidate so setupStateProvider picks up any changes from the sync.
      ref.invalidate(setupStateProvider);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e is HostException ? e.userMessage : 'Something went wrong.';
        _syncing = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('First look'),
        centerTitle: false,
        automaticallyImplyLeading: false,
      ),
      body: SafeArea(
        child: _syncing
            ? _SyncProgress()
            : _error != null
            ? _ErrorBody(message: _error!, onRetry: _sync)
            : _MirrorBody(mirror: _mirror!),
      ),
      bottomNavigationBar: !_syncing && _error == null
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 16),
                child: SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: () => context.go('/setup/notif'),
                    child: const Text('Continue'),
                  ),
                ),
              ),
            )
          : null,
    );
  }
}

// ---------------------------------------------------------------------------
// _SyncProgress
// ---------------------------------------------------------------------------

class _SyncProgress extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: 24),
          Text(
            'Reading your last few days…',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// _MirrorBody — renders headline, data lines and PartsCard stub.
// ---------------------------------------------------------------------------

class _MirrorBody extends StatelessWidget {
  const _MirrorBody({required this.mirror});

  final MirrorDto mirror;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (mirror.headline.isNotEmpty) ...[
            Text(
              mirror.headline,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 12),
          ],
          // Data-state lines — rendered verbatim from the host.
          for (final line in mirror.dataLines) ...[
            Text(line, style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: 6),
          ],
          if (mirror.parts != null) ...[
            const SizedBox(height: 16),
            // [REFACTOR CANDIDATE: replace _PartsCardStub with the real
            // PartsCard from lib/features/mirror/parts_card.dart (T4.2)]
            _PartsCardStub(parts: mirror.parts!),
          ],
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// _PartsCardStub — temporary until Track 4 publishes parts_card.dart (T4.2).
// Constructor signature matches the real PartsCard so the import swap is a
// one-line change: remove this class and update the import.
// ---------------------------------------------------------------------------

class _PartsCardStub extends StatelessWidget {
  const _PartsCardStub({required this.parts});

  // ignore: unused_field
  final PartsDto parts;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        'Parts card — Track 4 (T4.2)',
        style: Theme.of(context).textTheme.bodySmall
            ?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// _ErrorBody
// ---------------------------------------------------------------------------

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
