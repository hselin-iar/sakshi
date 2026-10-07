import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/core/providers.dart';
import 'package:sakshi/core/theme.dart';
import 'package:sakshi/features/ask/ask_screen.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fake_host.dart';
import 'package:sakshi/host/fixtures/setup_fixtures.dart';

class _FailingHost extends FakeHost {
  _FailingHost() : super(initial: SetupFixtures.demoActive);
  @override
  Future<AskReplyDto> askSakshi(
    String question,
    List<AskTurnDto> history,
  ) async => throw StateError('no');
}

class _RecordingHost extends FakeHost {
  _RecordingHost() : super(initial: SetupFixtures.demoActive);
  final questions = <String>[];
  final histories = <List<AskTurnDto>>[];
  @override
  Future<AskReplyDto> askSakshi(
    String question,
    List<AskTurnDto> history,
  ) async {
    questions.add(question);
    histories.add(history);
    return AskReplyDto(
      text: 'Answer to $question',
      source: 'LLM',
      isDemo: true,
      quote: 'A quote.',
      quoteSource: 'Complete Works',
    );
  }
}

Future<void> _open(WidgetTester tester, FakeHost host) async {
  await tester.binding.setSurfaceSize(const Size(400, 900));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(
    ProviderScope(
      overrides: [hostClientProvider.overrideWithValue(host)],
      child: MaterialApp(theme: sakshiLight(), home: const AskScreen()),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets(
    'in real mode nothing can be asked until the person has seen what is sent',
    (tester) async {
      await _open(tester, FakeHost(initial: SetupFixtures.usageGranted));
      expect(find.byKey(const Key('ask_disclosure')), findsOneWidget);
      expect(find.textContaining('no app names'), findsWidgets);
      expect(
        tester.widget<TextField>(find.byKey(const Key('ask_input'))).enabled,
        isFalse,
      );
      expect(find.byType(ActionChip), findsNothing);

      await tester.tap(find.byKey(const Key('ask_accept')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('ask_disclosure')), findsNothing);
      expect(
        tester.widget<TextField>(find.byKey(const Key('ask_input'))).enabled,
        isTrue,
      );
      expect(find.byType(ActionChip), findsWidgets);
    },
  );

  testWidgets(
    'in demo mode it is ready at once and says only made-up numbers are used',
    (tester) async {
      await _open(tester, FakeHost(initial: SetupFixtures.demoActive));
      expect(
        find.text(
          'Demo data: only made-up numbers and your question are used.',
        ),
        findsOneWidget,
      );
      expect(find.byKey(const Key('ask_accept')), findsNothing);
      expect(
        tester.widget<TextField>(find.byKey(const Key('ask_input'))).enabled,
        isTrue,
      );
    },
  );

  testWidgets(
    'a chip asks, the answer shows with a Vivekananda quote and where it was written',
    (tester) async {
      await _open(tester, FakeHost(initial: SetupFixtures.demoActive));
      expect(find.textContaining('Namaste. I am Sakshi'), findsOneWidget);
      await tester.tap(find.widgetWithText(ActionChip, 'How was my week?'));
      await tester.pumpAndSettle();
      expect(
        find.text('How was my week?'),
        findsOneWidget,
      ); // the person's own bubble (the chip row has gone)
      expect(
        find.textContaining('steadier than your starting normal'),
        findsOneWidget,
      );
      expect(find.byKey(const Key('ask_quote')), findsOneWidget);
      expect(find.textContaining('Swami Vivekananda'), findsOneWidget);
      expect(
        find.text('Written on your phone from your numbers. No network used.'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'a typed question is sent with the conversation so far, and the service source is stated',
    (tester) async {
      final host = _RecordingHost();
      await _open(tester, host);
      await tester.enterText(
        find.byKey(const Key('ask_input')),
        'Why do I get pulled away?',
      );
      await tester.tap(find.byKey(const Key('ask_send')));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('ask_input')), 'And today?');
      await tester.tap(find.byKey(const Key('ask_send')));
      await tester.pumpAndSettle();

      expect(host.questions, ['Why do I get pulled away?', 'And today?']);
      expect(host.histories.first, isEmpty); // the greeting is never sent back
      expect(host.histories.last.map((t) => t.role), ['user', 'sakshi']);
      expect(
        find.text(
          'Written by a language service from a summary of your numbers.',
        ),
        findsNWidgets(2),
      );
    },
  );

  testWidgets('an empty question does nothing', (tester) async {
    final host = _RecordingHost();
    await _open(tester, host);
    await tester.tap(find.byKey(const Key('ask_send')));
    await tester.pumpAndSettle();
    expect(host.questions, isEmpty);
  });

  testWidgets(
    'if the host fails the person sees a plain sentence, not an error',
    (tester) async {
      await _open(tester, _FailingHost());
      await tester.enterText(find.byKey(const Key('ask_input')), 'hello');
      await tester.tap(find.byKey(const Key('ask_send')));
      await tester.pumpAndSettle();
      expect(
        find.text('I could not answer just now. Please try again.'),
        findsOneWidget,
      );
    },
  );
}
