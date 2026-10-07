// lib/features/demo/demo_screen.dart
// T4.6 — Time Machine Demo Screen (Track 4 only).
// Controls: persona chips (Aarav default, Meera, Rohan),
// three preset chips (Day 1 = 3, Week 4 = 31, Week 8 = 59),
// slider 0..59 calling setDemoAsOf on release.
// The Mirror is shown beneath via normal Mirror widgets.
// Clear "Stop demo" button.
// DemoBanner is permanent while isDemo is true.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
// riverpod 3 moved StateProvider here; the pinned version is 3.4.3.
import 'package:flutter_riverpod/legacy.dart';
import 'package:sakshi/features/demo/demo_banner.dart';
import 'package:sakshi/features/mirror/mirror_content.dart';
import 'package:sakshi/features/mirror/mirror_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

import '../../core/providers.dart' show noAutoRetry;

// ─────────────────────────────────────────────────────────────────────────────
// Demo State & Providers
// ─────────────────────────────────────────────────────────────────────────────

/// Whether the synthetic demo is currently running.
final isDemoActiveProvider = StateProvider<bool>((ref) => false);

/// Currently selected demo persona ('aarav', 'meera', 'rohan').
final demoPersonaProvider = StateProvider<String>((ref) => 'aarav');

/// Current demo as-of day index (0..59, default 31 for Week 4).
final demoDayIndexProvider = StateProvider<int>((ref) => 31);

/// Loads the MirrorDto for the current demo state.
final demoMirrorProvider = FutureProvider<MirrorDto>((ref) async {
  final api = ref.watch(sakshiHostApiProvider);
  return await api.getMirror(null);
}, retry: noAutoRetry);

// ─────────────────────────────────────────────────────────────────────────────
// DemoScreen
// ─────────────────────────────────────────────────────────────────────────────

class DemoScreen extends ConsumerStatefulWidget {
  const DemoScreen({super.key});

  @override
  ConsumerState<DemoScreen> createState() => _DemoScreenState();
}

class _DemoScreenState extends ConsumerState<DemoScreen> {
  bool _isInitializing = false;
  double _sliderValue = 31.0;

  @override
  void initState() {
    super.initState();
    _initDemo();
  }

  Future<void> _initDemo() async {
    final isAlreadyActive = ref.read(isDemoActiveProvider);
    if (!isAlreadyActive) {
      setState(() => _isInitializing = true);
      try {
        final api = ref.read(sakshiHostApiProvider);
        final persona = ref.read(demoPersonaProvider);
        await api.startDemo(persona);
        await api.setDemoAsOf(31);
        ref.read(isDemoActiveProvider.notifier).state = true;
        ref.read(demoDayIndexProvider.notifier).state = 31;
        _sliderValue = 31.0;
        ref.invalidate(demoMirrorProvider);
      } catch (_) {
        // Handled via error states
      } finally {
        if (mounted) {
          setState(() => _isInitializing = false);
        }
      }
    } else {
      _sliderValue = ref.read(demoDayIndexProvider).toDouble();
    }
  }

  Future<void> _changePersona(String newPersona) async {
    setState(() => _isInitializing = true);
    try {
      final api = ref.read(sakshiHostApiProvider);
      ref.read(demoPersonaProvider.notifier).state = newPersona;
      await api.startDemo(newPersona);
      final currentDay = ref.read(demoDayIndexProvider);
      await api.setDemoAsOf(currentDay);
      ref.invalidate(demoMirrorProvider);
    } catch (_) {
      // Handled via error states
    } finally {
      if (mounted) {
        setState(() => _isInitializing = false);
      }
    }
  }

  Future<void> _setDay(int day) async {
    setState(() => _sliderValue = day.toDouble());
    ref.read(demoDayIndexProvider.notifier).state = day;
    try {
      final api = ref.read(sakshiHostApiProvider);
      await api.setDemoAsOf(day);
    } catch (_) {
      // Handled via error states
    }
    ref.invalidate(demoMirrorProvider);
  }

  Future<void> _stopDemo() async {
    try {
      final api = ref.read(sakshiHostApiProvider);
      await api.stopDemo();
    } catch (_) {}
    ref.read(isDemoActiveProvider.notifier).state = false;
    ref.invalidate(mirrorProvider(null));
    if (mounted) {
      Navigator.of(context).maybePop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final persona = ref.watch(demoPersonaProvider);
    final currentDay = ref.watch(demoDayIndexProvider);
    final mirrorAsync = ref.watch(demoMirrorProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Time Machine'),
        actions: [
          TextButton(
            key: const Key('stop_demo_button'),
            onPressed: _stopDemo,
            child: const Text('Stop demo'),
          ),
        ],
      ),
      body: Column(
        children: [
          // ── Permanent Demo Banner ────────────────────────────────────────
          const DemoBanner(),

          // ── Controls Section ─────────────────────────────────────────────
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            color: Theme.of(context).colorScheme.surfaceContainerLow,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Persona chips
                Row(
                  children: [
                    Text(
                      'Persona: ',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(width: 8),
                    _PersonaChip(
                      label: 'Aarav',
                      id: 'aarav',
                      selected: persona == 'aarav',
                      onSelected: () => _changePersona('aarav'),
                    ),
                    const SizedBox(width: 6),
                    _PersonaChip(
                      label: 'Meera',
                      id: 'meera',
                      selected: persona == 'meera',
                      onSelected: () => _changePersona('meera'),
                    ),
                    const SizedBox(width: 6),
                    _PersonaChip(
                      label: 'Rohan',
                      id: 'rohan',
                      selected: persona == 'rohan',
                      onSelected: () => _changePersona('rohan'),
                    ),
                  ],
                ),
                const SizedBox(height: 10),

                // Presets
                Row(
                  children: [
                    Text(
                      'Preset: ',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const SizedBox(width: 8),
                    _PresetChip(
                      label: 'Day 1',
                      day: 3,
                      selected: currentDay == 3,
                      onSelected: () => _setDay(3),
                    ),
                    const SizedBox(width: 6),
                    _PresetChip(
                      label: 'Week 4',
                      day: 31,
                      selected: currentDay == 31,
                      onSelected: () => _setDay(31),
                    ),
                    const SizedBox(width: 6),
                    _PresetChip(
                      label: 'Week 8',
                      day: 59,
                      selected: currentDay == 59,
                      onSelected: () => _setDay(59),
                    ),
                  ],
                ),
                const SizedBox(height: 6),

                // Slider
                Row(
                  children: [
                    Text(
                      'Day ${_sliderValue.round().toString().padLeft(2, '0')}',
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        fontFamily: 'monospace',
                        fontWeight: FontWeight.w600,
                      ),
                      key: const Key('slider_day_label'),
                    ),
                    Expanded(
                      child: Slider(
                        key: const Key('time_machine_slider'),
                        min: 0,
                        max: 59,
                        divisions: 59,
                        value: _sliderValue,
                        onChanged: (val) {
                          setState(() => _sliderValue = val);
                        },
                        onChangeEnd: (val) {
                          _setDay(val.round());
                        },
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          // ── Mirror Display or Progress ───────────────────────────────────
          Expanded(
            child: _isInitializing
                ? const Center(
                    key: Key('demo_initializing_indicator'),
                    child: CircularProgressIndicator(),
                  )
                : mirrorAsync.when(
                    loading: () => const Center(
                      key: Key('demo_mirror_loading'),
                      child: CircularProgressIndicator(),
                    ),
                    error: (err, _) => Center(
                      child: Padding(
                        padding: const EdgeInsets.all(24),
                        child: Text("Could not compute demo Mirror: $err"),
                      ),
                    ),
                    data: (mirror) => MirrorContent(mirror: mirror),
                  ),
          ),
        ],
      ),
    );
  }
}

class _PersonaChip extends StatelessWidget {
  const _PersonaChip({
    required this.label,
    required this.id,
    required this.selected,
    required this.onSelected,
  });

  final String label;
  final String id;
  final bool selected;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      key: Key('persona_chip_$id'),
      label: Text(label),
      selected: selected,
      onSelected: (_) => onSelected(),
    );
  }
}

class _PresetChip extends StatelessWidget {
  const _PresetChip({
    required this.label,
    required this.day,
    required this.selected,
    required this.onSelected,
  });

  final String label;
  final int day;
  final bool selected;
  final VoidCallback onSelected;

  @override
  Widget build(BuildContext context) {
    return ChoiceChip(
      key: Key('preset_chip_$day'),
      label: Text(label),
      selected: selected,
      onSelected: (_) => onSelected(),
    );
  }
}
