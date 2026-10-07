import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/providers.dart';
import '../../core/ui_strings.dart';

// ---------------------------------------------------------------------------
// AgeTap
//
// One question: "Are you under 18?" Two buttons: Yes / No. Skippable.
// Calls setUnder18(true/false) then advances to the next setup step.
// No text fields.
// ---------------------------------------------------------------------------

class AgeTap extends ConsumerStatefulWidget {
  const AgeTap({super.key});

  @override
  ConsumerState<AgeTap> createState() => _AgeTapState();
}

class _AgeTapState extends ConsumerState<AgeTap> {
  bool _saving = false;

  // Next step after age: battery helper (T3.5).
  static const _nextPath = '/setup/battery';

  Future<void> _answer(bool under18) async {
    setState(() => _saving = true);
    try {
      await ref.read(hostClientProvider).setUnder18(under18);
      if (!mounted) return;
      ref.invalidate(setupStateProvider);
      context.go(_nextPath);
    } catch (_) {
      if (!mounted) return;
      setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(titleAgeTap), centerTitle: false),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              Text(
                'Are you under 18?',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 12),
              Text(
                'Sakshi uses this to set a calmer default view. '
                'You can change it in Settings at any time.',
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              const SizedBox(height: 40),
              _saving
                  ? const Center(child: CircularProgressIndicator())
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        FilledButton(
                          onPressed: () => _answer(true),
                          child: const Text('Yes'),
                        ),
                        const SizedBox(height: 12),
                        OutlinedButton(
                          onPressed: () => _answer(false),
                          child: const Text('No'),
                        ),
                      ],
                    ),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: TextButton(
                  onPressed: _saving ? null : () => context.go(_nextPath),
                  child: const Text('Skip'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
