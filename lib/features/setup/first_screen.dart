import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/palette.dart';
import '../../core/ui_strings.dart';
import '../../core/widgets/wordmark.dart';
import '../demo/demo_launch.dart';

// ---------------------------------------------------------------------------
// FirstScreen — what Sakshi will and will not do (DOC 1 §1.4.1).
//
// Four lines from ui_strings.dart. One button. No data, no logic.
// ---------------------------------------------------------------------------

class FirstScreen extends ConsumerStatefulWidget {
  const FirstScreen({super.key});

  @override
  ConsumerState<FirstScreen> createState() => _FirstScreenState();
}

class _FirstScreenState extends ConsumerState<FirstScreen> {
  bool _starting = false;

  Future<void> _tryDemo() async {
    setState(() => _starting = true);
    try {
      await enterDemo(ref);
      if (mounted) context.go('/home');
    } catch (_) {
      if (mounted) setState(() => _starting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: SakshiColors.brown,
      body: Stack(
        children: [
          // the poster's gold circle, in the top-right corner, clear of the text below it
          Positioned(
            right: -120,
            top: -130,
            child: Container(
              width: 260,
              height: 260,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: SakshiColors.gold.withValues(alpha: 0.92),
              ),
            ),
          ),
          SafeArea(
            child: Column(
              children: [
                // the text scrolls on a short screen; the buttons below never do
                const Expanded(
                  child: SingleChildScrollView(
                    padding: EdgeInsets.fromLTRB(28, 48, 28, 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Wordmark(size: 72, latin: true),
                        SizedBox(height: 14),
                        Text(
                          firstScreenTagline,
                          style: TextStyle(
                            fontFamily: 'serif',
                            fontSize: 20,
                            height: 1.35,
                            color: SakshiColors.paleGold,
                          ),
                        ),
                        SizedBox(height: 40),
                        _Line(firstScreenLine1),
                        SizedBox(height: 16),
                        _Line(firstScreenLine2),
                        SizedBox(height: 16),
                        _Line(firstScreenLine3),
                        SizedBox(height: 16),
                        _Line(firstScreenLine4),
                      ],
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.fromLTRB(28, 8, 28, 20),
                  child: Column(
                    children: [
                      SizedBox(
                        width: double.infinity,
                        child: FilledButton(
                          style: FilledButton.styleFrom(
                            backgroundColor: SakshiColors.cream,
                            foregroundColor: SakshiColors.deepBrown,
                          ),
                          onPressed: () => context.go('/setup/usage'),
                          child: const Text('Continue'),
                        ),
                      ),
                      const SizedBox(height: 4),
                      SizedBox(
                        width: double.infinity,
                        child: TextButton(
                          key: const Key('first_try_demo'),
                          style: TextButton.styleFrom(
                            foregroundColor: SakshiColors.cream,
                          ),
                          onPressed: _starting ? null : _tryDemo,
                          child: Text(
                            _starting ? demoStarting : demoTryFirstButton,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Line extends StatelessWidget {
  const _Line(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(
        fontSize: 16,
        height: 1.4,
        color: SakshiColors.cream,
      ),
    );
  }
}
