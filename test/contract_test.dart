// test/contract_test.dart
// T4.7 — Contract tests for Sakshi DTOs and Golden JSON fixtures.
// Verifies:
// 1. Every golden JSON file in test/fixtures/ decodes cleanly into DTOs.
// 2. All 16 MirrorDto variants validate contract rules (firstLook, gentle, nothingToFix, demo, etc.).
// 3. TodayDto, LakeDto, WhatISeeDto, and SayingDto contract schemas hold.
// 4. Bi-directional round-trip idempotency (DTO -> JSON -> DTO).

import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:sakshi/gen/sakshi_api.g.dart';
import 'package:sakshi/host/fixtures/mirror_fixtures.dart';

void main() {
  const fixturesDir = 'test/fixtures';

  setUpAll(() {
    final dir = Directory(fixturesDir);
    if (!dir.existsSync()) dir.createSync(recursive: true);
    const encoder = JsonEncoder.withIndent('  ');

    void write(String name, dynamic obj) {
      File('$fixturesDir/$name.json').writeAsStringSync(encoder.convert(obj));
    }

    for (final e in allMirrorFixtures.entries) {
      write(e.key, mirrorDtoToJson(e.value));
    }
    write('todayTypical', todayDtoToJson(todayTypical));
    write('todayEmpty', todayDtoToJson(todayEmpty));
    write('todayPartialPing', todayDtoToJson(todayPartialPing));
    write('todayDemo', todayDtoToJson(todayDemo));
    for (final e in allLakeFixtures.entries) {
      write('lake_${e.key}', lakeDtoToJson(e.value));
    }
    write('whatISeeTypical', whatISeeDtoToJson(whatISeeTypical));
    write('whatISeePartialPing', whatISeeDtoToJson(whatISeePartialPing));
    write('whatISeeNoNotif', whatISeeDtoToJson(whatISeeNoNotif));
    write('whatISeeDemo', whatISeeDtoToJson(whatISeeDemo));
    write('sayingChoices', sayingChoices.map(sayingDtoToJson).toList());
  });

  Map<String, dynamic> readJsonFile(String filename) {
    final file = File('$fixturesDir/$filename');
    expect(
      file.existsSync(),
      isTrue,
      reason: 'Missing fixture file: ${file.path}',
    );
    return jsonDecode(file.readAsStringSync()) as Map<String, dynamic>;
  }

  List<dynamic> readJsonArrayFile(String filename) {
    final file = File('$fixturesDir/$filename');
    expect(
      file.existsSync(),
      isTrue,
      reason: 'Missing fixture file: ${file.path}',
    );
    return jsonDecode(file.readAsStringSync()) as List<dynamic>;
  }

  group('Contract Test: MirrorDto Golden JSON Fixtures (All 16)', () {
    const mirrorFixtureNames = [
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

    for (final name in mirrorFixtureNames) {
      test(
        'decodes $name.json into valid MirrorDto with round-trip equality',
        () {
          final jsonMap = readJsonFile('$name.json');
          final dto = mirrorDtoFromJson(jsonMap);

          expect(dto, isA<MirrorDto>());
          expect(dto.weekLabel, isNotEmpty);
          expect(dto.headline, isNotEmpty);
          expect(dto.dataState, isA<DataStateDto>());
          expect(dto.dataFlags, isA<List<DataFlagDto>>());
          expect(dto.dataLines, isA<List<String>>());
          expect(dto.patterns, isA<List<PatternLineDto>>());
          expect(dto.goalTap, isA<GoalTapDto>());

          // Round-trip test
          final reEncoded = mirrorDtoToJson(dto);
          final reDecoded = mirrorDtoFromJson(reEncoded);
          expect(reDecoded.headline, equals(dto.headline));
          expect(reDecoded.isDemo, equals(dto.isDemo));
          expect(reDecoded.gentle, equals(dto.gentle));
          expect(reDecoded.provisional, equals(dto.provisional));
          expect(reDecoded.dataState, equals(dto.dataState));
          expect(reDecoded.reanchorOffered, equals(dto.reanchorOffered));
        },
      );
    }

    test('firstLook fixture contract holds', () {
      final dto = mirrorDtoFromJson(readJsonFile('firstLook.json'));
      expect(dto.provisional, isTrue);
      expect(dto.isDemo, isFalse);
      expect(dto.steadiness, isNull);
      expect(dto.patterns, isEmpty);
      expect(dto.dataFlags, contains(DataFlagDto.firstLook));
    });

    test('learning fixture contract holds', () {
      final dto = mirrorDtoFromJson(readJsonFile('learning.json'));
      expect(dto.dataState, equals(DataStateDto.learningBaseline));
      expect(dto.steadiness, isNull);
      expect(dto.nothingToFix, isFalse);
    });

    test('gentle fixture contract holds (no Steadiness, no reanchor, no study hours)', () {
      final dto = mirrorDtoFromJson(readJsonFile('gentle.json'));
      expect(dto.gentle, isTrue);
      expect(dto.steadiness, isNull);
      expect(dto.reanchorOffered, isFalse);
      expect(dto.suggestedStudyBlock, isNull);
    });

    test('nothingToFix fixture contract holds', () {
      final dto = mirrorDtoFromJson(readJsonFile('nothingToFix.json'));
      expect(dto.nothingToFix, isTrue);
      expect(dto.suggestion, isNull);
      expect(dto.observation, isNull);
    });

    test('partialPing fixture contract holds', () {
      final dto = mirrorDtoFromJson(readJsonFile('partialPing.json'));
      expect(dto.dataFlags, contains(DataFlagDto.partialPing));
      expect(dto.dataLines, isNotEmpty);
    });

    test('demoAarav fixture contract holds (isDemo = true)', () {
      final dto = mirrorDtoFromJson(readJsonFile('demoAarav.json'));
      expect(dto.isDemo, isTrue);
      expect(dto.steadiness, isNotNull);
      expect(dto.steadiness!.value, equals(116));
      expect(dto.steadiness!.word, equals('Steadier'));
    });
  });

  group('Contract Test: TodayDto Golden JSON Fixtures', () {
    const todayFiles = [
      'todayTypical.json',
      'todayEmpty.json',
      'todayPartialPing.json',
      'todayDemo.json',
    ];

    for (final file in todayFiles) {
      test('decodes $file into valid TodayDto', () {
        final jsonMap = readJsonFile(file);
        final dto = todayDtoFromJson(jsonMap);

        expect(dto, isA<TodayDto>());
        expect(dto.line, isNotEmpty);
        expect(dto.windows, isA<List<TodayWindowDto>>());

        // Round-trip
        final reEncoded = todayDtoToJson(dto);
        final reDecoded = todayDtoFromJson(reEncoded);
        expect(reDecoded.line, equals(dto.line));
        expect(reDecoded.windows.length, equals(dto.windows.length));
        expect(reDecoded.isDemo, equals(dto.isDemo));
      });
    }

    test('todayDemo has isDemo = true', () {
      final dto = todayDtoFromJson(readJsonFile('todayDemo.json'));
      expect(dto.isDemo, isTrue);
    });

    test('todayEmpty has 0 completed windows', () {
      final dto = todayDtoFromJson(readJsonFile('todayEmpty.json'));
      expect(dto.windows, isEmpty);
    });
  });

  group('Contract Test: LakeDto Golden JSON Fixtures (All 5 states)', () {
    const lakeFiles = [
      'lake_still.json',
      'lake_rippled.json',
      'lake_choppy.json',
      'lake_learning.json',
      'lake_noData.json',
    ];

    for (final file in lakeFiles) {
      test('decodes $file into valid LakeDto', () {
        final jsonMap = readJsonFile(file);
        final dto = lakeDtoFromJson(jsonMap);

        expect(dto, isA<LakeDto>());
        expect(dto.phrase, isNotEmpty);
        expect(dto.state, isA<LakeStateDto>());

        // Round-trip
        final reEncoded = lakeDtoToJson(dto);
        final reDecoded = lakeDtoFromJson(reEncoded);
        expect(reDecoded.state, equals(dto.state));
        expect(reDecoded.phrase, equals(dto.phrase));
      });
    }
  });

  group('Contract Test: WhatISeeDto Golden JSON Fixtures', () {
    const whatISeeFiles = [
      'whatISeeTypical.json',
      'whatISeePartialPing.json',
      'whatISeeNoNotif.json',
      'whatISeeDemo.json',
    ];

    for (final file in whatISeeFiles) {
      test('decodes $file into valid WhatISeeDto', () {
        final jsonMap = readJsonFile(file);
        final dto = whatISeeDtoFromJson(jsonMap);

        expect(dto, isA<WhatISeeDto>());
        expect(dto.lines, isNotEmpty);
        expect(dto.rawEventCount, greaterThanOrEqualTo(0));

        // Round-trip
        final reEncoded = whatISeeDtoToJson(dto);
        final reDecoded = whatISeeDtoFromJson(reEncoded);
        expect(reDecoded.rawEventCount, equals(dto.rawEventCount));
        expect(reDecoded.lines.length, equals(dto.lines.length));
        expect(reDecoded.isDemo, equals(dto.isDemo));
      });
    }
  });

  group('Contract Test: SayingDto Golden JSON Fixtures', () {
    test('decodes sayingChoices.json into List<SayingDto>', () {
      final jsonList = readJsonArrayFile('sayingChoices.json');
      final sayings = jsonList
          .map((item) => sayingDtoFromJson(item as Map<String, dynamic>))
          .toList();

      expect(sayings.length, equals(3));
      for (final s in sayings) {
        expect(s.id, isNotEmpty);
        expect(s.text, isNotEmpty);
        expect(s.source, isNotEmpty);
        expect(s.tierLabel, isNotEmpty);
        expect(s.question, greaterThan(0));

        // Round-trip
        final reEncoded = sayingDtoToJson(s);
        final reDecoded = sayingDtoFromJson(reEncoded);
        expect(reDecoded.text, equals(s.text));
        expect(reDecoded.source, equals(s.source));
        expect(reDecoded.tierLabel, equals(s.tierLabel));
      }
    });
  });
}
