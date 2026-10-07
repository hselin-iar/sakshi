import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/ui_strings.dart';
import '../../host/host_client.dart';

// ---------------------------------------------------------------------------
// WhatISeeScreen
//
// Renders WhatISeeDto exactly as the host gives it:
//   - dto.lines are shown verbatim — no extra sentences invented here.
//   - Counts and health lines are shown as plain text.
//   - Pause switch calls pause(bool).
//   - Export button (+ include-raw checkbox, default off).
//   - Delete button with two-step confirmation.
//
// DEMO_ACTIVE errors are shown as their userMessage.
// After delete the user is returned to the setup gate ('/').
// ---------------------------------------------------------------------------

class WhatISeeScreen extends ConsumerStatefulWidget {
  const WhatISeeScreen({super.key});

  @override
  ConsumerState<WhatISeeScreen> createState() => _WhatISeeScreenState();
}

class _WhatISeeScreenState extends ConsumerState<WhatISeeScreen> {
  bool _pausing = false;
  bool _exporting = false;
  bool _includeRaw = false;
  String? _exportResult;

  // Two-step delete state: 0 = idle, 1 = first tap done, 2 = deleting.
  int _deleteStep = 0;

  Future<void> _togglePause(WhatISeeDto dto) async {
    setState(() => _pausing = true);
    try {
      await ref.read(hostClientProvider).pause(!dto.paused);
      ref.invalidate(setupStateProvider);
      // Force a re-fetch of WhatISee after pause change.
      ref.invalidate(_whatISeeProvider);
    } catch (e) {
      if (!mounted) return;
      _showError(e);
    } finally {
      if (mounted) setState(() => _pausing = false);
    }
  }

  Future<void> _export() async {
    setState(() {
      _exporting = true;
      _exportResult = null;
    });
    try {
      final result = await ref.read(hostClientProvider).exportData(_includeRaw);
      if (!mounted) return;
      setState(() => _exportResult = result.fileName);
    } catch (e) {
      if (!mounted) return;
      _showError(e);
    } finally {
      if (mounted) setState(() => _exporting = false);
    }
  }

  Future<void> _deleteTap() async {
    if (_deleteStep == 0) {
      setState(() => _deleteStep = 1);
      return;
    }
    if (_deleteStep == 1) {
      setState(() => _deleteStep = 2);
      try {
        await ref.read(hostClientProvider).deleteEverything();
        if (!mounted) return;
        context.go('/');
      } catch (e) {
        if (!mounted) return;
        setState(() => _deleteStep = 0);
        _showError(e);
      }
    }
  }

  void _showError(Object e) {
    final msg = e is HostException ? e.userMessage : 'Something went wrong.';
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    final async = ref.watch(_whatISeeProvider);

    return Scaffold(
      appBar: AppBar(title: const Text(titleWhatISee), centerTitle: false),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => _ErrorBody(
          message: e is HostException ? e.userMessage : 'Something went wrong.',
          onRetry: () => ref.invalidate(_whatISeeProvider),
        ),
        data: (dto) => _Body(
          dto: dto,
          pausing: _pausing,
          exporting: _exporting,
          includeRaw: _includeRaw,
          exportResult: _exportResult,
          deleteStep: _deleteStep,
          onPauseToggle: () => _togglePause(dto),
          onIncludeRawChanged: (v) => setState(() {
            _includeRaw = v;
            _exportResult = null;
          }),
          onExport: _export,
          onDeleteTap: _deleteTap,
          onDeleteCancel: () => setState(() => _deleteStep = 0),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Local provider — not shared globally; WhatISee is a pull-only screen.
// ---------------------------------------------------------------------------

final _whatISeeProvider = FutureProvider.autoDispose<WhatISeeDto>(
  (ref) => ref.read(hostClientProvider).getWhatISee(),
  retry: noAutoRetry,
);

// ---------------------------------------------------------------------------
// _Body — purely presentational.
// ---------------------------------------------------------------------------

class _Body extends StatelessWidget {
  const _Body({
    required this.dto,
    required this.pausing,
    required this.exporting,
    required this.includeRaw,
    required this.exportResult,
    required this.deleteStep,
    required this.onPauseToggle,
    required this.onIncludeRawChanged,
    required this.onExport,
    required this.onDeleteTap,
    required this.onDeleteCancel,
  });

  final WhatISeeDto dto;
  final bool pausing;
  final bool exporting;
  final bool includeRaw;
  final String? exportResult;
  final int deleteStep;
  final VoidCallback onPauseToggle;
  final ValueChanged<bool> onIncludeRawChanged;
  final VoidCallback onExport;
  final VoidCallback onDeleteTap;
  final VoidCallback onDeleteCancel;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      children: [
        // Host-provided lines — shown verbatim.
        for (final line in dto.lines) ...[
          Text(line, style: Theme.of(context).textTheme.bodyMedium),
          const SizedBox(height: 8),
        ],

        const SizedBox(height: 8),
        const Divider(),
        const SizedBox(height: 8),

        // Counts.
        _CountRow('Usage events', dto.rawEventCount),
        _CountRow('Notification events', dto.notifEventCount),
        _CountRow('Derived days', dto.derivedDays),
        _CountRow('Worker runs (7 d)', dto.workerRuns7d),
        _CountRow('Odd event pairs', dto.oddEventPairs),
        if (dto.listenerCoverage7d != null)
          Builder(
            builder: (context) {
              final coveragePct = (dto.listenerCoverage7d! * 100)
                  .toStringAsFixed(0);
              return _TextRow('Listener coverage (7 d)', '$coveragePct%');
            },
          ),
        if (dto.lastError != null) _TextRow('Last error', dto.lastError ?? ''),

        const SizedBox(height: 16),
        const Divider(),

        // Pause switch.
        SwitchListTile(
          title: const Text(whatISeePauseLabel),
          value: dto.paused,
          onChanged: pausing ? null : (_) => onPauseToggle(),
          contentPadding: EdgeInsets.zero,
        ),

        const Divider(),
        const SizedBox(height: 8),

        // Export section.
        CheckboxListTile(
          title: const Text(whatISeeExportIncludeRaw),
          value: includeRaw,
          onChanged: exporting ? null : (v) => onIncludeRawChanged(v ?? false),
          contentPadding: EdgeInsets.zero,
          controlAffinity: ListTileControlAffinity.leading,
        ),
        const SizedBox(height: 4),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton(
            onPressed: exporting ? null : onExport,
            child: exporting
                ? const SizedBox(
                    height: 18,
                    width: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text(whatISeeExportLabel),
          ),
        ),
        if (exportResult != null) ...[
          const SizedBox(height: 6),
          Text(
            '$whatISeeExportDone$exportResult',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],

        const SizedBox(height: 24),
        const Divider(),
        const SizedBox(height: 8),

        // Delete section — two-step confirmation.
        if (deleteStep == 0)
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: onDeleteTap,
              style: OutlinedButton.styleFrom(
                foregroundColor: Theme.of(context).colorScheme.error,
                side: BorderSide(color: Theme.of(context).colorScheme.error),
              ),
              child: const Text(whatISeeDeleteLabel),
            ),
          ),
        if (deleteStep == 1) ...[
          Text(
            whatISeeDeleteConfirm1,
            style: Theme.of(context).textTheme.bodyMedium
                ?.copyWith(color: Theme.of(context).colorScheme.error),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: onDeleteCancel,
                  child: const Text('Cancel'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: onDeleteTap,
                  style: FilledButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.error,
                  ),
                  child: const Text(whatISeeDeleteConfirm2),
                ),
              ),
            ],
          ),
        ],
        if (deleteStep == 2) const Center(child: CircularProgressIndicator()),

        const SizedBox(height: 32),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Small shared display widgets.
// ---------------------------------------------------------------------------

class _CountRow extends StatelessWidget {
  const _CountRow(this.label, this.count);

  final String label;
  final int count;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          Text(
            count.toString(),
            style: Theme.of(context).textTheme.bodySmall
                ?.copyWith(fontFeatures: const [FontFeature.tabularFigures()]),
          ),
        ],
      ),
    );
  }
}

class _TextRow extends StatelessWidget {
  const _TextRow(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          Text(value, style: Theme.of(context).textTheme.bodySmall),
        ],
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
