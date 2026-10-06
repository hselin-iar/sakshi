import 'package:flutter/material.dart';

import 'gen/sakshi_api.g.dart';

void main() => runApp(const SakshiSkeleton());

class SakshiSkeleton extends StatelessWidget {
  const SakshiSkeleton({super.key});

  @override
  Widget build(BuildContext context) => const MaterialApp(home: _SpikeScreen());
}

class _SpikeScreen extends StatefulWidget {
  const _SpikeScreen();

  @override
  State<_SpikeScreen> createState() => _SpikeScreenState();
}

class _SpikeScreenState extends State<_SpikeScreen> {
  String _reply = '';

  Future<void> _call() async {
    final state = await SakshiHostApi().getSetupState();
    setState(() => _reply = 'usage access: ${state.usageAccessGranted}');
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ElevatedButton(onPressed: _call, child: const Text('Ask host')),
          Text(_reply),
        ],
      ),
    ),
  );
}
