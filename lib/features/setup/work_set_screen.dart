import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/ui_strings.dart';
import '../../host/host_client.dart';

// ---------------------------------------------------------------------------
// WorkSetScreen
//
// Shows the list of launcher apps. User taps to tick/untick (IN_SET).
// Ticked apps show a Depends toggle. Counter "n of 12" updates live.
// Save is disabled above 12 but the host also enforces the cap.
// No text fields anywhere — DOC 1: no typing.
// ---------------------------------------------------------------------------

class WorkSetScreen extends ConsumerStatefulWidget {
  const WorkSetScreen({super.key});

  @override
  ConsumerState<WorkSetScreen> createState() => _WorkSetScreenState();
}

class _WorkSetScreenState extends ConsumerState<WorkSetScreen> {
  List<AppDto>? _apps;
  // pkg → userClass (null = not selected)
  final Map<String, UserClassDto?> _selection = {};
  bool _loading = true;
  bool _saving = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadApps();
  }

  Future<void> _loadApps() async {
    try {
      final apps = await ref.read(hostClientProvider).listLauncherApps();
      if (!mounted) return;
      setState(() {
        _apps = apps;
        // Pre-tick apps suggested by the host.
        for (final a in apps) {
          if (a.suggestedInSet) {
            _selection[a.pkg] = UserClassDto.inSet;
          } else if (a.suggestedDepends) {
            _selection[a.pkg] = UserClassDto.depends;
          }
        }
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = e is HostException
            ? e.userMessage
            : 'Could not load your apps.';
        _loading = false;
      });
    }
  }

  int get _selectedCount => _selection.values.where((v) => v != null).length;

  bool get _overCap => _selectedCount > 12;

  Future<void> _save() async {
    setState(() => _saving = true);
    final entries = _selection.entries
        .where((e) => e.value != null)
        .map((e) => WorkSetEntryDto(pkg: e.key, userClass: e.value!))
        .toList();
    try {
      final result = await ref.read(hostClientProvider).saveWorkSet(entries);
      if (!mounted) return;
      if (result.ok) {
        ref.invalidate(setupStateProvider);
        context.go('/setup/study-hours');
      } else {
        setState(() {
          _errorMessage = result.userMessage ?? 'Could not save.';
          _saving = false;
        });
      }
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = e is HostException ? e.userMessage : 'Could not save.';
        _saving = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(titleWorkSet), centerTitle: false),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _errorMessage != null && _apps == null
          ? _ErrorBody(
              message: _errorMessage!,
              onRetry: () {
                setState(() {
                  _errorMessage = null;
                  _loading = true;
                });
                _loadApps();
              },
            )
          : _buildList(),
      bottomNavigationBar: _loading || _apps == null
          ? null
          : _BottomBar(
              count: _selectedCount,
              overCap: _overCap,
              saving: _saving,
              errorMessage: _overCap ? null : _errorMessage,
              onSave: _overCap || _saving ? null : _save,
              onSkip: () => context.go('/setup/study-hours'),
            ),
    );
  }

  Widget _buildList() {
    final apps = _apps!;
    return ListView.builder(
      itemCount: apps.length,
      itemBuilder: (context, i) {
        final app = apps[i];
        final cls = _selection[app.pkg];
        final inSet = cls == UserClassDto.inSet;
        final depends = cls == UserClassDto.depends;

        return Column(
          children: [
            CheckboxListTile(
              value: inSet || depends,
              onChanged: (checked) {
                setState(() {
                  if (checked == true) {
                    _selection[app.pkg] = UserClassDto.inSet;
                  } else {
                    _selection.remove(app.pkg);
                  }
                  _errorMessage = null;
                });
              },
              title: Text(app.label),
              subtitle: Text(
                app.pkg,
                style: Theme.of(context).textTheme.bodySmall,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              secondary: (inSet || depends)
                  ? _DependsChip(
                      isDepends: depends,
                      onToggle: () {
                        setState(() {
                          _selection[app.pkg] = depends
                              ? UserClassDto.inSet
                              : UserClassDto.depends;
                        });
                      },
                    )
                  : null,
            ),
          ],
        );
      },
    );
  }
}

class _DependsChip extends StatelessWidget {
  const _DependsChip({required this.isDepends, required this.onToggle});

  final bool isDepends;
  final VoidCallback onToggle;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onToggle,
      child: Chip(
        label: Text(isDepends ? 'Depends' : 'In set'),
        backgroundColor: isDepends
            ? Theme.of(context).colorScheme.secondaryContainer
            : Theme.of(context).colorScheme.primaryContainer,
        labelStyle: TextStyle(
          fontSize: 11,
          color: isDepends
              ? Theme.of(context).colorScheme.onSecondaryContainer
              : Theme.of(context).colorScheme.onPrimaryContainer,
        ),
        padding: EdgeInsets.zero,
        visualDensity: VisualDensity.compact,
      ),
    );
  }
}

class _BottomBar extends StatelessWidget {
  const _BottomBar({
    required this.count,
    required this.overCap,
    required this.saving,
    required this.onSave,
    required this.onSkip,
    this.errorMessage,
  });

  final int count;
  final bool overCap;
  final bool saving;
  final VoidCallback? onSave;
  final VoidCallback onSkip;
  final String? errorMessage;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 8, 24, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '$count of 12',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: overCap
                        ? Theme.of(context).colorScheme.error
                        : Theme.of(context).colorScheme.onSurface,
                  ),
                ),
                if (overCap)
                  Text(
                    'Pick up to 12. Fewer is better.',
                    style: Theme.of(context).textTheme.bodySmall
                        ?.copyWith(color: Theme.of(context).colorScheme.error),
                  ),
              ],
            ),
            if (errorMessage != null) ...[
              const SizedBox(height: 4),
              Text(
                errorMessage!,
                style: Theme.of(context).textTheme.bodySmall
                    ?.copyWith(color: Theme.of(context).colorScheme.error),
              ),
            ],
            const SizedBox(height: 8),
            FilledButton(
              onPressed: onSave,
              child: saving
                  ? const SizedBox(
                      height: 18,
                      width: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Save and continue'),
            ),
            TextButton(onPressed: onSkip, child: const Text('Skip for now')),
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
