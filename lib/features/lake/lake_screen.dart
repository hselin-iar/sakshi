// lib/features/lake/lake_screen.dart
// T4.4 — Lake screen displaying the Lake widget state in-app.

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sakshi/features/lake/lake_painter.dart';
import 'package:sakshi/features/mirror/mirror_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';

import '../../core/providers.dart' show noAutoRetry;

/// Provider for the Lake state.
final lakeProvider = FutureProvider<LakeDto>((ref) async {
  final api = ref.watch(sakshiHostApiProvider);
  return await api.getLake();
}, retry: noAutoRetry);

class LakeScreen extends ConsumerWidget {
  const LakeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final lakeAsync = ref.watch(lakeProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('The Lake')),
      body: Center(
        child: lakeAsync.when(
          loading: () =>
              const CircularProgressIndicator(key: Key('lake_loading')),
          error: (err, _) => Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text("Could not read Lake data", key: Key('lake_error')),
                const SizedBox(height: 12),
                OutlinedButton(
                  onPressed: () => ref.invalidate(lakeProvider),
                  child: const Text('Retry'),
                ),
              ],
            ),
          ),
          data: (lake) => Padding(
            padding: const EdgeInsets.all(24),
            child: LakeWidgetView(lake: lake),
          ),
        ),
      ),
    );
  }
}
