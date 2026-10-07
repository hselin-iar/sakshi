import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/host/host_client.dart';
import 'package:sakshi/host/pigeon_host_client.dart';

// Fakes the native side of the real generated channels, so these tests cover the whole Dart path:
// PigeonHostClient -> generated SakshiHostApi -> channel -> reply (success, or a FlutterError as [code, message, details]).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  const codec = SakshiHostApi.pigeonChannelCodec;
  String channel(String method) =>
      'dev.flutter.pigeon.sakshi.SakshiHostApi.$method';

  void reply(String method, Object? Function(List<Object?> args) handler) {
    messenger.setMockMessageHandler(channel(method), (message) async {
      // Methods with no arguments send a null message rather than an empty list.
      final args =
          (codec.decodeMessage(message) as List<Object?>?) ?? <Object?>[];
      return codec.encodeMessage(handler(args));
    });
  }

  tearDown(() {
    for (final m in [
      'setWeeklyNote',
      'pause',
      'tapTryThis',
      'getLake',
      'startDemo',
    ]) {
      messenger.setMockMessageHandler(channel(m), null);
    }
  });

  test('a successful call returns the native value', () async {
    reply('setWeeklyNote', (args) => <Object?>[args.first == true]);
    expect(await PigeonHostClient().setWeeklyNote(true), isTrue);
    expect(await PigeonHostClient().setWeeklyNote(false), isFalse);
  });

  test('a DTO comes back field for field, with null staying null', () async {
    reply(
      'getLake',
      (_) => <Object?>[
        LakeDto(state: LakeStateDto.noData, phrase: '', isDemo: false),
      ],
    );
    final lake = await PigeonHostClient().getLake();
    expect(lake.state, LakeStateDto.noData);
    expect(lake.asOfEpochMs, isNull);
    expect(lake.isDemo, isFalse);
  });

  test('every error code from the native side becomes a HostException with its message', () async {
    for (final code in hostErrorCodes) {
      reply(
        'pause',
        (_) => <Object?>[code, 'message for $code', 'developer detail'],
      );
      try {
        await PigeonHostClient().pause(true);
        fail('expected a HostException for $code');
      } on HostException catch (e) {
        expect(e.code, code);
        expect(e.userMessage, 'message for $code');
        expect(
          e.toString(),
          isNot(contains('developer detail')),
          reason: 'the dev detail is never surfaced',
        );
      }
    }
  });

  test('arguments reach the native side unchanged', () async {
    List<Object?>? seen;
    reply('tapTryThis', (args) {
      seen = args;
      return <Object?>[null];
    });
    await PigeonHostClient().tapTryThis('S2', 'com.example.chat');
    expect(seen, ['S2', 'com.example.chat']);
  });

  test('a void reply with a null payload completes normally', () async {
    reply('startDemo', (_) => <Object?>[null]);
    await PigeonHostClient().startDemo('aarav');
  });
}
