import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/palette.dart';
import '../../core/providers.dart';
import '../../core/ui_strings.dart';
import '../../gen/sakshi_api.g.dart';
import '../../host/host_client.dart';

// ---------------------------------------------------------------------------
// AskScreen — a chat about the person's own numbers. The words come from the host (a language service, or the engine's offline answer);
// this screen only shows them. Each answer says where it was written, and carries a saying chosen from the shelf by the host.
// In real mode the person first sees what is sent and taps once to go on; in demo mode only made-up numbers are used.
// ---------------------------------------------------------------------------

class _Message {
  _Message.user(this.text)
    : fromSakshi = false,
      quote = null,
      quoteSource = null,
      source = null;
  _Message.sakshi(this.text, {this.quote, this.quoteSource, this.source})
    : fromSakshi = true;

  final bool fromSakshi;
  final String text;
  final String? quote;
  final String? quoteSource;
  final String? source; // 'LLM' or 'OFFLINE'
}

class AskScreen extends ConsumerStatefulWidget {
  const AskScreen({super.key});

  @override
  ConsumerState<AskScreen> createState() => _AskScreenState();
}

class _AskScreenState extends ConsumerState<AskScreen> {
  final _messages = <_Message>[_Message.sakshi(askGreeting)];
  final _controller = TextEditingController();
  final _scroll = ScrollController();
  bool _thinking = false;
  bool _acknowledged = false;

  @override
  void dispose() {
    _controller.dispose();
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _ask(String raw) async {
    final question = raw.trim();
    if (question.isEmpty || _thinking) return;
    // Only real turns are sent back as history, never the greeting.
    final history = _messages
        .skip(1)
        .map(
          (m) =>
              AskTurnDto(role: m.fromSakshi ? 'sakshi' : 'user', text: m.text),
        )
        .toList();
    setState(() {
      _messages.add(_Message.user(question));
      _thinking = true;
      _controller.clear();
    });
    _scrollToEnd();
    try {
      final reply = await ref
          .read(hostClientProvider)
          .askSakshi(question, history);
      if (!mounted) return;
      setState(() {
        _messages.add(
          _Message.sakshi(
            reply.text,
            quote: reply.quote,
            quoteSource: reply.quoteSource,
            source: reply.source,
          ),
        );
        _thinking = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _messages.add(_Message.sakshi(askFailed));
        _thinking = false;
      });
    }
    _scrollToEnd();
  }

  void _scrollToEnd() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scroll.hasClients) {
        _scroll.animateTo(
          _scroll.position.maxScrollExtent + 200,
          duration: const Duration(milliseconds: 250),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final isDemo = ref
        .watch(setupStateProvider)
        .maybeWhen(data: (s) => s.isDemo, orElse: () => false);
    final ready = isDemo || _acknowledged;
    final colors = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text(titleAsk)),
      body: SafeArea(
        child: Column(
          children: [
            _Disclosure(
              isDemo: isDemo,
              acknowledged: _acknowledged,
              onAccept: () => setState(() => _acknowledged = true),
            ),
            Expanded(
              child: ListView.builder(
                key: const Key('ask_messages'),
                controller: _scroll,
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
                itemCount: _messages.length + (_thinking ? 1 : 0),
                itemBuilder: (context, i) {
                  if (i == _messages.length) return const _Bubble.thinking();
                  return _Bubble(message: _messages[i]);
                },
              ),
            ),
            if (_messages.length <= 2 && ready)
              SizedBox(
                height: 48,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  children: [
                    for (final q in askChips)
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: ActionChip(
                          label: Text(q),
                          onPressed: _thinking ? null : () => _ask(q),
                        ),
                      ),
                  ],
                ),
              ),
            Container(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
              decoration: BoxDecoration(
                color: colors.surfaceContainerLow,
                border: Border(top: BorderSide(color: colors.outlineVariant)),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      key: const Key('ask_input'),
                      controller: _controller,
                      enabled: ready,
                      maxLength: 300,
                      minLines: 1,
                      maxLines: 3,
                      textInputAction: TextInputAction.send,
                      onSubmitted: _ask,
                      decoration: InputDecoration(
                        hintText: askInputHint,
                        counterText: '',
                        filled: true,
                        fillColor: colors.surface,
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 18,
                          vertical: 12,
                        ),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(28),
                          borderSide: BorderSide(color: colors.outlineVariant),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(28),
                          borderSide: BorderSide(color: colors.outlineVariant),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(
                    key: const Key('ask_send'),
                    tooltip: askSend,
                    onPressed: ready && !_thinking
                        ? () => _ask(_controller.text)
                        : null,
                    icon: const Icon(Icons.arrow_upward),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Disclosure extends StatelessWidget {
  const _Disclosure({
    required this.isDemo,
    required this.acknowledged,
    required this.onAccept,
  });

  final bool isDemo;
  final bool acknowledged;
  final VoidCallback onAccept;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    if (!isDemo && acknowledged) return const SizedBox.shrink();
    return Container(
      key: const Key('ask_disclosure'),
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.secondaryContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            isDemo ? askDisclosureDemo : askDisclosureReal,
            style: Theme.of(context).textTheme.bodySmall
                ?.copyWith(color: colors.onSecondaryContainer),
          ),
          if (!isDemo) ...[
            const SizedBox(height: 8),
            FilledButton.tonal(
              key: const Key('ask_accept'),
              style: FilledButton.styleFrom(minimumSize: const Size(64, 40)),
              onPressed: onAccept,
              child: const Text(askDisclosureAccept),
            ),
          ],
        ],
      ),
    );
  }
}

class _Bubble extends StatelessWidget {
  const _Bubble({required this.message}) : thinking = false;
  const _Bubble.thinking() : message = null, thinking = true;

  final _Message? message;
  final bool thinking;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final fromSakshi = thinking || message!.fromSakshi;
    final bg = fromSakshi ? colors.surfaceContainerLowest : colors.primary;
    final fg = fromSakshi ? colors.onSurface : colors.onPrimary;
    final maxWidth = MediaQuery.sizeOf(context).width * 0.84;

    return Align(
      alignment: fromSakshi ? Alignment.centerLeft : Alignment.centerRight,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 6),
        constraints: BoxConstraints(maxWidth: maxWidth),
        padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
        decoration: BoxDecoration(
          color: bg,
          border: fromSakshi ? Border.all(color: colors.outlineVariant) : null,
          borderRadius: BorderRadius.only(
            topLeft: const Radius.circular(18),
            topRight: const Radius.circular(18),
            bottomLeft: Radius.circular(fromSakshi ? 4 : 18),
            bottomRight: Radius.circular(fromSakshi ? 18 : 4),
          ),
        ),
        child: thinking
            ? Text(
                askThinking,
                style: text.bodyMedium?.copyWith(
                  color: colors.onSurfaceVariant,
                ),
              )
            : Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (fromSakshi) ...[
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          width: 18,
                          height: 18,
                          decoration: const BoxDecoration(
                            shape: BoxShape.circle,
                            color: SakshiColors.gold,
                          ),
                          child: const Center(
                            child: Icon(
                              Icons.circle,
                              size: 7,
                              color: SakshiColors.brown,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Text(
                          'साक्षी',
                          style: TextStyle(
                            fontFamily: 'serif',
                            fontWeight: FontWeight.w700,
                            fontSize: 14,
                            color: colors.primary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                  ],
                  Text(
                    message!.text,
                    style: text.bodyLarge?.copyWith(color: fg, height: 1.4),
                  ),
                  if (message!.quote != null && message!.quote!.isNotEmpty)
                    _Quote(text: message!.quote!, source: message!.quoteSource),
                  if (message!.source != null) ...[
                    const SizedBox(height: 8),
                    Text(
                      message!.source == 'LLM'
                          ? askSourceService
                          : askSourceOffline,
                      style: text.labelSmall?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ],
              ),
      ),
    );
  }
}

class _Quote extends StatelessWidget {
  const _Quote({required this.text, this.source});

  final String text;
  final String? source;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      key: const Key('ask_quote'),
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.fromLTRB(12, 4, 4, 4),
      decoration: const BoxDecoration(
        border: Border(left: BorderSide(color: SakshiColors.gold, width: 3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '“$text”',
            style: TextStyle(
              fontFamily: 'serif',
              fontStyle: FontStyle.italic,
              fontSize: 16,
              height: 1.4,
              color: colors.onSurface,
            ),
          ),
          if (source != null && source!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              'Swami Vivekananda · $source',
              style: Theme.of(context).textTheme.labelSmall
                  ?.copyWith(color: colors.onSurfaceVariant),
            ),
          ],
        ],
      ),
    );
  }
}
