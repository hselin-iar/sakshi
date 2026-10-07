import 'package:flutter/material.dart';

import 'debug/host_debug_screen.dart';

// Phone-only entry point: one button per host method, against the real Android host.
//   flutter run -t lib/main_debug.dart
void main() => runApp(const MaterialApp(home: HostDebugScreen()));
