// copy_rules_test.dart — LC-8 copy-rule enforcement (T3.7).
//
// Scans every .dart file under lib/ for:
//   1. The forbidden-word pattern (AGENTS.md §7 line 80).
//   2. An exclamation mark (!) in a string literal.
//
// Best-effort regex: catches violations in simple string literals.
// Skips comment lines to avoid false positives on the pattern itself.
//
// Run: flutter test test/copy_rules_test.dart

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  final libDir = Directory('lib');

  final dartFiles = libDir
      .listSync(recursive: true)
      .whereType<File>()
      .where((f) => f.path.endsWith('.dart'))
      .toList();

  // LC-8 forbidden pattern — verbatim from AGENTS.md §7.
  final forbiddenPattern = RegExp(
    r'\b(focused|distracted|distraction|wasted|waste|failed|failure|'
    r'streak|lazy|addict\w*|ruin\w*|lost (your )?(focus|concentration)|'
    r'you (should|must|need to))\b',
    caseSensitive: false,
  );

  // Matches single- or double-quoted string literal content (best-effort).
  // Does not handle raw strings or multiline strings — acceptable per spec.
  final singleQuotePattern = RegExp(r"'([^'\\]*(?:\\.[^'\\]*)*)'");
  final doubleQuotePattern = RegExp(r'"([^"\\]*(?:\\.[^"\\]*)*)"');

  group('LC-8 copy rules', () {
    for (final file in dartFiles) {
      test(file.path, () {
        final lines = file.readAsLinesSync();
        final violations = <String>[];

        for (var i = 0; i < lines.length; i++) {
          final line = lines[i];

          // Skip comment lines — the forbidden words appear in comments
          // referencing AGENTS.md and in the regex pattern definition itself.
          final trimmed = line.trimLeft();
          if (trimmed.startsWith('//') || trimmed.startsWith('*')) continue;

          // Collect string contents from both quote styles.
          final contents = <String>[];
          for (final m in singleQuotePattern.allMatches(line)) {
            final c = m.group(1);
            if (c != null) contents.add(c);
          }
          for (final m in doubleQuotePattern.allMatches(line)) {
            final c = m.group(1);
            if (c != null) contents.add(c);
          }

          for (final content in contents) {
            if (forbiddenPattern.hasMatch(content)) {
              violations.add(
                '${file.path}:${i + 1}: forbidden word in: "$content"',
              );
            }
            if (content.contains('!')) {
              violations.add(
                '${file.path}:${i + 1}: exclamation mark in: "$content"',
              );
            }
          }
        }

        expect(
          violations,
          isEmpty,
          reason: 'LC-8 violations:\n${violations.join('\n')}',
        );
      });
    }
  });
}
