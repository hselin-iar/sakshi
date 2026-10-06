import 'package:flutter/material.dart';

import 'debug/host_debug_screen.dart';

void main() => runApp(const SakshiSkeleton());

class SakshiSkeleton extends StatelessWidget {
  const SakshiSkeleton({super.key});

  @override
  Widget build(BuildContext context) =>
      const MaterialApp(home: HostDebugScreen());
}
