// test/fixtures/mirror_fixtures_test.dart
// T4.1 — verifies that every fixture in mirror_fixtures.dart constructs
// without error and that key invariants hold.
// Run with:  flutter test test/fixtures/mirror_fixtures_test.dart

import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';

void main() {
  // ── Mirror fixture construction ──────────────────────────────────────────

  group('allMirrorFixtures', () {
    test('contains exactly sixteen entries', () {
      expect(allMirrorFixtures.length, 16);
    });

    test('all sixteen names are present', () {
      const expected = [
        'firstLook',
        'learning',
        'steady',
        'steadier',
        'wavering',
        'gentle',
        'partialPing',
        'pingOff',
        'notSeen',
        'tooLittle',
        'unusualWeek',
        'nothingToFix',
        'withSuggestion',
        'withVerdict',
        'afterLapse',
        'demoAarav',
      ];
      for (final name in expected) {
        expect(
          allMirrorFixtures.containsKey(name),
          isTrue,
          reason: '$name missing from allMirrorFixtures',
        );
      }
    });

    test('every fixture is a MirrorDto', () {
      for (final entry in allMirrorFixtures.entries) {
        expect(
          entry.value,
          isA<MirrorDto>(),
          reason: '${entry.key} is not a MirrorDto',
        );
      }
    });
  });

  // ── firstLook ─────────────────────────────────────────────────────────────

  group('firstLook', () {
    test('provisional = true', () => expect(firstLook.provisional, isTrue));
    test('isDemo = false', () => expect(firstLook.isDemo, isFalse));
    test('steadiness is null', () => expect(firstLook.steadiness, isNull));
    test('patterns is empty', () => expect(firstLook.patterns, isEmpty));
    test(
      'reanchorOffered = false',
      () => expect(firstLook.reanchorOffered, isFalse),
    );
    test(
      'suggestedStudyBlock is null',
      () => expect(firstLook.suggestedStudyBlock, isNull),
    );
    test(
      'dataFlags contains firstLook flag',
      () => expect(firstLook.dataFlags, contains(DataFlagDto.firstLook)),
    );
  });

  // ── learning ──────────────────────────────────────────────────────────────

  group('learning', () {
    test(
      'dataState = learningBaseline',
      () => expect(learning.dataState, DataStateDto.learningBaseline),
    );
    test('steadiness is null', () => expect(learning.steadiness, isNull));
    test('nothingToFix = false', () => expect(learning.nothingToFix, isFalse));
    test('suggestion is null', () => expect(learning.suggestion, isNull));
  });

  // ── steady ────────────────────────────────────────────────────────────────

  group('steady', () {
    test(
      'reanchorOffered = true',
      () => expect(steady.reanchorOffered, isTrue),
    );
    test(
      'Steadiness word = Steady',
      () => expect(steady.steadiness?.word, 'Steady'),
    );
    test('Steadiness value = 100', () => expect(steady.steadiness?.value, 100));
    test(
      'suggestedStudyBlock is null',
      () => expect(steady.suggestedStudyBlock, isNull),
    );
    test('gentle = false', () => expect(steady.gentle, isFalse));
  });

  // ── steadier ──────────────────────────────────────────────────────────────

  group('steadier', () {
    test(
      'Steadiness word = Steadier',
      () => expect(steadier.steadiness?.word, 'Steadier'),
    );
    test(
      'Steadiness value = 116',
      () => expect(steadier.steadiness?.value, 116),
    );
    test(
      'suggestedStudyBlock is not null',
      () => expect(steadier.suggestedStudyBlock, isNotNull),
    );
    test(
      'suggestedStudyBlock.startMinute = 540',
      () => expect(steadier.suggestedStudyBlock?.startMinute, 540),
    );
    test(
      'reanchorOffered = false',
      () => expect(steadier.reanchorOffered, isFalse),
    );
  });

  // ── wavering ──────────────────────────────────────────────────────────────

  group('wavering', () {
    test(
      'Steadiness word = Wavering',
      () => expect(wavering.steadiness?.word, 'Wavering'),
    );
    test(
      'Steadiness value < 90',
      () => expect(wavering.steadiness!.value, lessThan(90)),
    );
    test('has a suggestion', () => expect(wavering.suggestion, isNotNull));
  });

  // ── gentle ────────────────────────────────────────────────────────────────

  group('gentle', () {
    test('gentle = true', () => expect(gentle.gentle, isTrue));
    test('steadiness is null', () => expect(gentle.steadiness, isNull));
    test('patterns is empty', () => expect(gentle.patterns, isEmpty));
    test('suggestion is null', () => expect(gentle.suggestion, isNull));
    test(
      'goalTap.offered = false',
      () => expect(gentle.goalTap.offered, isFalse),
    );
    test('teacher is null', () => expect(gentle.teacher, isNull));
    test(
      'reanchorOffered = false',
      () => expect(gentle.reanchorOffered, isFalse),
    );
    test(
      'suggestedStudyBlock is null',
      () => expect(gentle.suggestedStudyBlock, isNull),
    );
    test('returnLine is present', () => expect(gentle.returnLine, isNotNull));
    test('saying is present', () => expect(gentle.saying, isNotNull));
  });

  // ── tooLittle ─────────────────────────────────────────────────────────────

  group('tooLittle', () {
    test(
      'dataState = tooLittleData',
      () => expect(tooLittle.dataState, DataStateDto.tooLittleData),
    );
    test('parts is null', () => expect(tooLittle.parts, isNull));
    test('steadiness is null', () => expect(tooLittle.steadiness, isNull));
    test('stones is null', () => expect(tooLittle.stones, isNull));
  });

  // ── nothingToFix ─────────────────────────────────────────────────────────

  group('nothingToFix', () {
    test(
      'nothingToFix = true',
      () => expect(nothingToFix.nothingToFix, isTrue),
    );
    test('suggestion is null', () => expect(nothingToFix.suggestion, isNull));
  });

  // ── withSuggestion ────────────────────────────────────────────────────────

  group('withSuggestion', () {
    test(
      'has a suggestion',
      () => expect(withSuggestion.suggestion, isNotNull),
    );
    test(
      'nothingToFix = false',
      () => expect(withSuggestion.nothingToFix, isFalse),
    );
    test(
      'suggestion kindId is non-empty',
      () => expect(withSuggestion.suggestion!.kindId, isNotEmpty),
    );
  });

  // ── withVerdict ───────────────────────────────────────────────────────────

  group('withVerdict', () {
    test('verdict is not null', () => expect(withVerdict.verdict, isNotNull));
    test(
      'verdict.verdict = MOVED',
      () => expect(withVerdict.verdict!.verdict, 'MOVED'),
    );
    test(
      'verdict.beforeValue is not null',
      () => expect(withVerdict.verdict!.beforeValue, isNotNull),
    );
    test(
      'verdict.afterValue is not null',
      () => expect(withVerdict.verdict!.afterValue, isNotNull),
    );
  });

  // ── afterLapse ────────────────────────────────────────────────────────────

  group('afterLapse', () {
    test(
      'lapseLine is not null',
      () => expect(afterLapse.lapseLine, isNotNull),
    );
    test(
      'observation is S11',
      () => expect(afterLapse.observation?.kindId, 'S11'),
    );
    test(
      'goalTap.offered = false',
      () => expect(afterLapse.goalTap.offered, isFalse),
    );
  });

  // ── demoAarav ─────────────────────────────────────────────────────────────

  group('demoAarav', () {
    test('isDemo = true', () => expect(demoAarav.isDemo, isTrue));
    test('Steadiness = 116 Steadier', () {
      expect(demoAarav.steadiness?.value, 116);
      expect(demoAarav.steadiness?.word, 'Steadier');
    });
    test(
      'weekLabel contains Demo',
      () => expect(demoAarav.weekLabel, contains('Demo')),
    );
    test(
      'reanchorOffered = false',
      () => expect(demoAarav.reanchorOffered, isFalse),
    );
  });

  // ── Lake fixtures ─────────────────────────────────────────────────────────

  group('allLakeFixtures', () {
    test('contains all five states', () {
      expect(allLakeFixtures.length, 5);
      expect(allLakeFixtures.containsKey('still'), isTrue);
      expect(allLakeFixtures.containsKey('rippled'), isTrue);
      expect(allLakeFixtures.containsKey('choppy'), isTrue);
      expect(allLakeFixtures.containsKey('learning'), isTrue);
      expect(allLakeFixtures.containsKey('noData'), isTrue);
    });

    test(
      'still   state = still',
      () => expect(lakeStill.state, LakeStateDto.still),
    );
    test(
      'rippled state = rippled',
      () => expect(lakeRippled.state, LakeStateDto.rippled),
    );
    test(
      'choppy  state = choppy',
      () => expect(lakeChoppy.state, LakeStateDto.choppy),
    );
    test(
      'learning state = learning',
      () => expect(lakeLearning.state, LakeStateDto.learning),
    );
    test(
      'noData  state = noData',
      () => expect(lakeNoData.state, LakeStateDto.noData),
    );

    test('learning and noData have null asOfEpochMs', () {
      expect(lakeLearning.asOfEpochMs, isNull);
      expect(lakeNoData.asOfEpochMs, isNull);
    });

    test('still/rippled/choppy have non-null asOfEpochMs', () {
      expect(lakeStill.asOfEpochMs, isNotNull);
      expect(lakeRippled.asOfEpochMs, isNotNull);
      expect(lakeChoppy.asOfEpochMs, isNotNull);
    });
  });

  // ── Today fixtures ────────────────────────────────────────────────────────

  group('TodayDto fixtures', () {
    test(
      'todayTypical has 2 windows',
      () => expect(todayTypical.windows.length, 2),
    );
    test('todayEmpty has 0 windows', () => expect(todayEmpty.windows, isEmpty));
    test('todayDemo.isDemo = true', () => expect(todayDemo.isDemo, isTrue));
    test('todayPartialPing has partialPing flag', () {
      expect(todayPartialPing.dataFlags, contains(DataFlagDto.partialPing));
    });
  });

  // ── WhatISee fixtures ─────────────────────────────────────────────────────

  group('WhatISeeDto fixtures', () {
    test('whatISeeTypical granted both permissions', () {
      expect(whatISeeTypical.usageAccessGranted, isTrue);
      expect(whatISeeTypical.notificationAccessGranted, isTrue);
    });
    test(
      'whatISeeNoNotif.notificationAccessGranted = false',
      () => expect(whatISeeNoNotif.notificationAccessGranted, isFalse),
    );
    test(
      'whatISeeDemo.isDemo = true',
      () => expect(whatISeeDemo.isDemo, isTrue),
    );
    test(
      'whatISeeTypical.lines is non-empty',
      () => expect(whatISeeTypical.lines, isNotEmpty),
    );
  });

  // ── Saying choices ────────────────────────────────────────────────────────

  group('sayingChoices', () {
    test('has 3 entries', () => expect(sayingChoices.length, 3));
    test('all have non-empty text', () {
      for (final s in sayingChoices) {
        expect(s.text, isNotEmpty);
      }
    });
    test('all have non-empty source', () {
      for (final s in sayingChoices) {
        expect(s.source, isNotEmpty);
      }
    });
    test('all have non-empty tierLabel', () {
      for (final s in sayingChoices) {
        expect(s.tierLabel, isNotEmpty);
      }
    });
  });

  // ── Copy-rule smoke check on all headlines and lines ─────────────────────

  group('copy rules smoke', () {
    final _forbidden = RegExp(
      r'\b(focused|distracted|distraction|wasted|waste|failed|failure|'
      r'streak|lazy|addict\w*|ruin\w*|'
      r'lost (your )?(focus|concentration)|'
      r'you (should|must|need to))\b',
      caseSensitive: false,
    );

    List<String> _allStrings(MirrorDto m) {
      return [
        m.headline,
        ...m.dataLines,
        if (m.returnLine != null) m.returnLine!,
        if (m.lapseLine != null) m.lapseLine!,
        if (m.parts?.lines.stretch != null) m.parts!.lines.stretch!,
        if (m.parts?.lines.stays != null) m.parts!.lines.stays!,
        if (m.parts?.lines.ret != null) m.parts!.lines.ret!,
        if (m.parts?.lines.quiet != null) m.parts!.lines.quiet!,
        ...?m.parts?.extrasLines,
        if (m.stones != null) m.stones!.line,
        if (m.clearHour != null) m.clearHour!.line,
        ...m.patterns.map((p) => p.line),
        if (m.suggestion != null) m.suggestion!.line,
        if (m.observation != null) m.observation!.line,
        if (m.verdict != null) m.verdict!.line,
        if (m.teacher != null) m.teacher!.line,
        if (m.saying != null) m.saying!.text,
      ];
    }

    for (final entry in {
      'firstLook': firstLook,
      'learning': learning,
      'steady': steady,
      'steadier': steadier,
      'wavering': wavering,
      'gentle': gentle,
      'partialPing': partialPing,
      'pingOff': pingOff,
      'notSeen': notSeen,
      'tooLittle': tooLittle,
      'unusualWeek': unusualWeek,
      'nothingToFix': nothingToFix,
      'withSuggestion': withSuggestion,
      'withVerdict': withVerdict,
      'afterLapse': afterLapse,
      'demoAarav': demoAarav,
    }.entries) {
      test('${entry.key}: no forbidden words', () {
        for (final s in _allStrings(entry.value)) {
          expect(
            _forbidden.hasMatch(s),
            isFalse,
            reason: 'Forbidden word in ${entry.key}: "$s"',
          );
          expect(
            s.contains('!'),
            isFalse,
            reason: 'Exclamation mark in ${entry.key}: "$s"',
          );
        }
      });
    }

    for (final entry in {
      'firstLook': firstLook,
      'learning': learning,
      'steady': steady,
      'steadier': steadier,
      'wavering': wavering,
      'gentle': gentle,
      'partialPing': partialPing,
      'pingOff': pingOff,
      'notSeen': notSeen,
      'tooLittle': tooLittle,
      'unusualWeek': unusualWeek,
      'nothingToFix': nothingToFix,
      'withSuggestion': withSuggestion,
      'withVerdict': withVerdict,
      'afterLapse': afterLapse,
      'demoAarav': demoAarav,
    }.entries) {
      final patterns = entry.value.patterns;
      if (patterns.isNotEmpty) {
        test('${entry.key}: pattern lines end with evidence tag', () {
          for (final p in patterns) {
            expect(
              p.line,
              matches(r'\(based on \d+ windows? over \d+ days?\)\.$'),
              reason:
                  '${entry.key} pattern line missing evidence tag: "${p.line}"',
            );
          }
        });
      }
    }
  });
}
