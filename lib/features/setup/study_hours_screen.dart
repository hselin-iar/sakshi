import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/nav.dart';
import '../../core/providers.dart';
import '../../core/ui_strings.dart';
import '../../host/host_client.dart';

// ---------------------------------------------------------------------------
// StudyHoursScreen
//
// Two time-range pickers (start and end) as minute-of-day sliders.
// End < start is valid — it means the block crosses midnight (22:00–01:30).
// A "learn it for me" switch. Skip is allowed.
// No text fields.
// ---------------------------------------------------------------------------

class StudyHoursScreen extends ConsumerStatefulWidget {
  const StudyHoursScreen({super.key});

  @override
  ConsumerState<StudyHoursScreen> createState() => _StudyHoursScreenState();
}

class _StudyHoursScreenState extends ConsumerState<StudyHoursScreen> {
  // Default: 20:00–23:00 (1200–1380 minutes from midnight).
  int _startMinute = 1200;
  int _endMinute = 1380;
  bool _learnForMe = false;
  bool _saving = false;

  String _minuteLabel(int m) {
    final h = m ~/ 60;
    final min = m % 60;
    final period = h < 12 ? 'am' : 'pm';
    final displayH = h == 0 ? 12 : (h > 12 ? h - 12 : h);
    return '$displayH:${min.toString().padLeft(2, '0')} $period';
  }

  bool get _crossesMidnight => _endMinute < _startMinute;

  Future<void> _save() async {
    setState(() => _saving = true);
    final block = StudyBlockDto(
      startMinute: _startMinute,
      endMinute: _endMinute,
    );
    final dto = StudyHoursDto(blocks: [block], learnForMe: _learnForMe);
    try {
      await ref.read(hostClientProvider).saveStudyHours(dto);
      if (!mounted) return;
      ref.invalidate(setupStateProvider);
      leaveToWhereYouCameFrom(context);
    } catch (e) {
      if (!mounted) return;
      setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text(titleStudyHours), centerTitle: false),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'When do you usually study? Sakshi uses this as a hint '
                'to find your study sessions. You can change it later.',
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const SizedBox(height: 32),

              // Start time slider.
              _SliderSection(
                label: 'Start time',
                value: _startMinute,
                timeLabel: _minuteLabel(_startMinute),
                onChanged: (v) => setState(() => _startMinute = v),
              ),
              const SizedBox(height: 24),

              // End time slider.
              _SliderSection(
                label: 'End time',
                value: _endMinute,
                timeLabel: _minuteLabel(_endMinute),
                onChanged: (v) => setState(() => _endMinute = v),
              ),
              const SizedBox(height: 12),

              // Midnight-crossing indicator.
              if (_crossesMidnight)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    'Block crosses midnight '
                    '(${_minuteLabel(_startMinute)} – ${_minuteLabel(_endMinute)})',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ),

              const SizedBox(height: 32),

              // Learn-for-me switch.
              SwitchListTile(
                value: _learnForMe,
                onChanged: (v) => setState(() => _learnForMe = v),
                title: const Text('Learn it for me'),
                subtitle: const Text(
                  'After a few weeks Sakshi will infer your study '
                  'times from usage patterns.',
                ),
                contentPadding: EdgeInsets.zero,
              ),

              const SizedBox(height: 32),

              SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: _saving ? null : _save,
                  child: _saving
                      ? const SizedBox(
                          height: 18,
                          width: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Save and continue'),
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: TextButton(
                  onPressed: () => leaveToWhereYouCameFrom(context),
                  child: const Text('Skip for now'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SliderSection extends StatelessWidget {
  const _SliderSection({
    required this.label,
    required this.value,
    required this.timeLabel,
    required this.onChanged,
  });

  final String label;
  final int value;
  final String timeLabel;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(label, style: Theme.of(context).textTheme.titleSmall),
            Text(
              timeLabel,
              style: Theme.of(context).textTheme.titleSmall
                  ?.copyWith(color: Theme.of(context).colorScheme.primary),
            ),
          ],
        ),
        Slider(
          // 0 to 1439 minutes (23:59).
          min: 0,
          max: 1439,
          divisions: 288, // 5-minute steps.
          value: value.toDouble(),
          label: timeLabel,
          onChanged: (v) => onChanged(v.round()),
        ),
      ],
    );
  }
}
