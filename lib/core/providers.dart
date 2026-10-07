import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../host/host_client.dart';

// ---------------------------------------------------------------------------
// Providers — manual Riverpod only. No code generation (@riverpod / build_runner).
// ---------------------------------------------------------------------------

// --- hostClientProvider ---------------------------------------------------
// Overrideable at app start (real) or in tests (FakeHost).
// There is no default: callers must override in ProviderScope.overrides.
final hostClientProvider = Provider<HostClient>((ref) {
  throw UnimplementedError(
    'hostClientProvider must be overridden with a real or fake HostClient.',
  );
});

// --- setupStateProvider ---------------------------------------------------
// Fetches the current setup state on demand.
final setupStateProvider = FutureProvider<SetupStateDto>((ref) async {
  final client = ref.watch(hostClientProvider);
  return client.getSetupState();
});

// --- syncProvider ---------------------------------------------------------
// Calls syncNow(), then invalidates setupStateProvider (and any others that
// depend on host data) so they re-fetch with fresh values.
// Returns the SyncStatusDto from syncNow.
final syncProvider = FutureProvider.autoDispose<SyncStatusDto>((ref) async {
  final client = ref.watch(hostClientProvider);
  final status = await client.syncNow();
  ref.invalidate(setupStateProvider);
  return status;
});
