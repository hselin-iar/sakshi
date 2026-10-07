// lib/host/fixtures/mirror_fixtures.dart
// T4.1 — Mirror Fixtures and Shared JSON (Track 4 only).
// Placeholders — real golden values arrive from Track 2 at T4.7.
// Sentences are observations with evidence counts, never advice.
// No forbidden words. No '!'. Pattern lines end with
// "(based on N windows over D days)".
// Steadiness words: Wavering | Steady | Steadier (exactly).

// ignore_for_file: prefer_const_constructors, prefer_const_literals_to_create_immutables

import 'package:sakshi/gen/sakshi_api.g.dart';
export 'package:sakshi/host/fixtures/dto_codec.dart';

// ─────────────────────────────────────────────────────────────────────────────
// Shared sub-objects reused across fixtures
// ─────────────────────────────────────────────────────────────────────────────

// Sayings (from sayings.json; question = numeric Q-index).
final _sayingNeutral = SayingDto(
  id: 'sy03',
  text: 'Until you know what the mind is doing you cannot control it.',
  source: 'Complete Works, Volume 1, 1.3.6 Pratyahara And Dharana',
  tierLabel: 'Type not resolved in the Outcome Map',
  question: 7,
);

final _sayingReturn = SayingDto(
  id: 'sy05',
  text:
      "the faculty of detaching ourselves at a moment's notice from anything.",
  source: 'Complete Works, Volume 6, 6.4.157 Margot',
  tierLabel: 'His own writing or letter',
  question: 66,
);

final _sayingLapse = SayingDto(
  id: 'sy07',
  text: 'No Hurry, No Worry.',
  source: 'Complete Works, Volume 9, 9.1.214 Christine',
  tierLabel: 'His own writing or letter',
  question: 212,
);

// Epoch placeholder — Mon 5 Oct 2026 04:00 IST.
const _weekMs = 1759603800000;
const _weekLabel = '5–11 Oct';

// Parts — typical healthy week.
final _partsTypical = PartsDto(
  stretchMin: 14.5,
  longestStretchMin: 28.0,
  inSetShare: 0.68,
  quietShare: 0.12,
  staysPerHour: 3.2,
  glances: 4,
  returnMin: 4.8,
  lines: PartLinesDto(
    stretch: 'You stayed in your work apps for about 15 minutes at a stretch (longest 28).',
    stays: 'Something pulled you away about 3 times an hour.',
    ret: 'It took you about 5 minutes to come back.',
    quiet: 'Your phone was quiet for 12% of your work time.',
  ),
  extrasLines: [],
);

// Parts — provisional (no baseline).
final _partsProvisional = PartsDto(
  stretchMin: 12.0,
  longestStretchMin: 22.0,
  inSetShare: 0.60,
  quietShare: null,
  staysPerHour: 4.1,
  glances: 6,
  returnMin: null,
  lines: PartLinesDto(
    stretch: 'You stayed in your work apps for about 12 minutes at a stretch (longest 22).',
    stays: 'Something pulled you away about 4 times an hour.',
    ret: null,
    quiet: null,
  ),
  extrasLines: [],
);

// Goal taps.
final _goalOffered = GoalTapDto(offered: true, answer: null);
final _goalNotOffered = GoalTapDto(offered: false, answer: null);
final _goalAnsweredPartly = GoalTapDto(
  offered: true,
  answer: GoalAnswerDto.partly,
);

// Stones.
final _stonesTypical = StonesDto(
  totalStays: 17,
  stoneCount: 9,
  selfStartedCount: 6,
  unknownCount: 2,
  topStoneLabel: null,
  noRippleRate: 0.41,
  line: '9 of your 17 stays began with a ping.',
);

final _stonesWithTop = StonesDto(
  totalStays: 21,
  stoneCount: 12,
  selfStartedCount: 7,
  unknownCount: 2,
  topStoneLabel: 'Messaging app',
  noRippleRate: 0.38,
  line: '12 of your 21 stays began with a ping.',
);

final _stonesSelfStarted = StonesDto(
  totalStays: 14,
  stoneCount: 4,
  selfStartedCount: 9,
  unknownCount: 1,
  topStoneLabel: null,
  noRippleRate: null,
  line: 'Most of your stays began with no ping.',
);

// Teacher lines.
final _teacherWeek4 = TeacherDto(
  opensThisWeek: 5,
  opensPrevWeek: 7,
  minutesThisWeek: 18.0,
  line: 'You opened Sakshi 5 times this week.',
);

final _teacherWeek8 = TeacherDto(
  opensThisWeek: 2,
  opensPrevWeek: 3,
  minutesThisWeek: 6.0,
  line: 'You opened Sakshi 2 times this week.',
);

// Clear-hour slot.
final _clearHour = ClearHourDto(
  startHour: 9,
  endHour: 11,
  stretchMin: 22.0,
  line: 'Between 09:00 and 11:00 your stretches average 22 minutes.',
);

// Pattern lines.
final _patternRhythm = PatternLineDto(
  kindId: 'RHYTHM',
  line: 'Your work-hour windows tend to run longer in the morning than in the afternoon (based on 11 windows over 5 days).',
  evidenceWindows: 11,
  evidenceDays: 5,
);

final _patternTrend = PatternLineDto(
  kindId: 'TREND',
  line: 'Your return time has been getting shorter over the last four weeks (based on 14 windows over 7 days).',
  evidenceWindows: 14,
  evidenceDays: 7,
);

final _patternShape = PatternLineDto(
  kindId: 'SHAPE',
  line: 'About half your evening windows ended shortly after a ping arrived (based on 9 windows over 6 days).',
  evidenceWindows: 9,
  evidenceDays: 6,
);

final _patternBreak = PatternLineDto(
  kindId: 'BREAK_POINT',
  line: 'Most stretches end around the 20-to-25-minute mark (based on 10 windows over 8 days).',
  evidenceWindows: 10,
  evidenceDays: 8,
);

// Suggestions.
final _suggestionS4 = SuggestionDto(
  kindId: 'S4',
  line: 'One app accounts for 8 of your last 12 ping-driven stays.',
  actionLabel: 'Review its notifications',
  actionType: 'OPEN_NOTIFICATION_SETTINGS',
  opensSettings: true,
  subjectKey: 'com.example.messaging',
);

final _suggestionS1 = SuggestionDto(
  kindId: 'S1',
  line: 'Your 09:00-to-11:00 block has fewer pulls and longer stretches than the rest of your day.',
  actionLabel: 'Put the hardest thing first',
  actionType: 'FIRST_THING_HARDEST',
  opensSettings: false,
  subjectKey: null,
);

// Observations.
final _observationS11 = ObservationDto(
  kindId: 'S11',
  line: 'You were away for 4 days. Your starting normal is still here.',
);

final _observationS12 = ObservationDto(
  kindId: 'S12',
  line: 'Your return time improved by 20% over two weeks (based on 14 windows over 7 days).',
);

// Verdicts.
final _verdictMoved = VerdictDto(
  verdict: 'MOVED',
  line: 'Your stays per hour from that app dropped from 4.1 to 3.0 in the two weeks after you tried the change. This is correlation, not cause.',
  approxMix: false,
  beforeValue: 4.1,
  afterValue: 3.0,
);

// ─────────────────────────────────────────────────────────────────────────────
// The sixteen Mirror fixtures
// ─────────────────────────────────────────────────────────────────────────────

/// 1. firstLook — provisional over retained days; no baseline, no steadiness.
final firstLook = MirrorDto(
  isDemo: false,
  provisional: true,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [DataFlagDto.firstLook],
  dataLines: ['This is based on the last few days your phone has kept.'],
  headline: 'Your last few days: you held about 12 minutes at a stretch. Something pulled you away about 4 times an hour.',
  parts: _partsProvisional,
  steadiness: null,
  stones: _stonesTypical,
  clearHour: null,
  patterns: [],
  suggestion: null,
  observation: null,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalNotOffered,
  teacher: null,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: null,
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 2. learning — baseline building; no Steadiness, no suggestion.
final learning = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.learningBaseline,
  dataFlags: [],
  dataLines: [
    'Building your starting normal — a few more days and comparisons will appear.',
  ],
  headline: 'Your last few days: you held about 12 minutes at a stretch. Something pulled you away about 4 times an hour.',
  parts: _partsProvisional,
  steadiness: null,
  stones: _stonesTypical,
  clearHour: null,
  patterns: [],
  suggestion: null,
  observation: null,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalNotOffered,
  teacher: null,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: null,
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 3. steady — week 5, Steadiness 100, reanchorOffered = true.
final steady = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 15-minute stretches and 5-minute returns; close to your starting normal.',
  parts: _partsTypical,
  steadiness: SteadinessDto(value: 100, word: 'Steady'),
  stones: _stonesTypical,
  clearHour: _clearHour,
  patterns: [_patternRhythm],
  suggestion: null,
  observation: null,
  nothingToFix: true,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 5 minutes on average.',
  reanchorOffered: true, // week 5, first and only baseline row
  suggestedStudyBlock: null,
);

/// 4. steadier — week 8, Steadiness 116, suggestedStudyBlock set.
final steadier = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 18-minute stretches and 4-minute returns; steadier than your starting normal.',
  parts: PartsDto(
    stretchMin: 18.0,
    longestStretchMin: 34.0,
    inSetShare: 0.74,
    quietShare: 0.15,
    staysPerHour: 2.5,
    glances: 3,
    returnMin: 3.8,
    lines: PartLinesDto(
      stretch: 'You stayed in your work apps for about 18 minutes at a stretch (longest 34).',
      stays: 'Something pulled you away about 2 to 3 times an hour.',
      ret: 'It took you about 4 minutes to come back.',
      quiet: 'Your phone was quiet for 15% of your work time.',
    ),
    extrasLines: [],
  ),
  steadiness: SteadinessDto(value: 116, word: 'Steadier'),
  stones: _stonesSelfStarted,
  clearHour: _clearHour,
  patterns: [_patternRhythm, _patternTrend],
  suggestion: null,
  observation: _observationS12,
  nothingToFix: true,
  verdict: null,
  goalTap: _goalAnsweredPartly,
  teacher: _teacherWeek8,
  lapseLine: null,
  saying: _sayingReturn,
  returnLine: 'You came back in about 4 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: StudyBlockDto(
    startMinute: 540,
    endMinute: 720,
  ), // 09:00–12:00 learned
);

/// 5. wavering — Steadiness 82, Wavering, active S4 suggestion.
final wavering = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 10-minute stretches and 7-minute returns; less steady than your starting normal.',
  parts: PartsDto(
    stretchMin: 10.0,
    longestStretchMin: 18.0,
    inSetShare: 0.55,
    quietShare: 0.08,
    staysPerHour: 5.1,
    glances: 8,
    returnMin: 7.2,
    lines: PartLinesDto(
      stretch: 'You stayed in your work apps for about 10 minutes at a stretch (longest 18).',
      stays: 'Something pulled you away about 5 times an hour.',
      ret: 'It took you about 7 minutes to come back.',
      quiet: 'Your phone was quiet for 8% of your work time.',
    ),
    extrasLines: [],
  ),
  steadiness: SteadinessDto(value: 82, word: 'Wavering'),
  stones: _stonesWithTop,
  clearHour: null,
  patterns: [_patternBreak, _patternShape],
  suggestion: _suggestionS4,
  observation: null,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 7 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 6. gentle — gentle mode: return line and stay count only.
///    No Steadiness, no patterns, no suggestion, no goalTap offered,
///    no teacher, reanchorOffered = false, suggestedStudyBlock = null.
final gentle = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: true,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'Your week, gently.',
  parts: PartsDto(
    stretchMin: null,
    longestStretchMin: null,
    inSetShare: null,
    quietShare: null,
    staysPerHour: null,
    glances: 0,
    returnMin: null,
    lines: PartLinesDto(stretch: null, stays: null, ret: null, quiet: null),
    extrasLines: [],
  ),
  steadiness: null,
  stones: StonesDto(
    totalStays: 14,
    stoneCount: 0,
    selfStartedCount: 0,
    unknownCount: 14,
    topStoneLabel: null,
    noRippleRate: null,
    line: 'You had 14 stays this week.',
  ),
  clearHour: null,
  patterns: [],
  suggestion: null,
  observation: null,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalNotOffered,
  teacher: null,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 5 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 7. partialPing — listener alive for only part of the week.
final partialPing = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [DataFlagDto.partialPing],
  dataLines: [
    'Ping tracking was active for part of this week — some stays are listed as unknown origin.',
  ],
  headline: 'You held 13-minute stretches and 5-minute returns; close to your starting normal.',
  parts: _partsTypical,
  steadiness: SteadinessDto(value: 98, word: 'Steady'),
  stones: StonesDto(
    totalStays: 19,
    stoneCount: 5,
    selfStartedCount: 4,
    unknownCount: 10,
    topStoneLabel: null,
    noRippleRate: null,
    line: '5 of your 9 known-origin stays began with a ping.',
  ),
  clearHour: null,
  patterns: [_patternRhythm],
  suggestion: null,
  observation: null,
  nothingToFix: true,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 5 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 8. pingOff — notification listener never granted.
final pingOff = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [DataFlagDto.pingOff],
  dataLines: ['Ping tracking is off — all stays show as unknown origin.'],
  headline: 'You held 13-minute stretches; close to your starting normal.',
  parts: PartsDto(
    stretchMin: 13.0,
    longestStretchMin: 25.0,
    inSetShare: 0.65,
    quietShare: 0.11,
    staysPerHour: 3.5,
    glances: 5,
    returnMin: 5.5,
    lines: PartLinesDto(
      stretch: 'You stayed in your work apps for about 13 minutes at a stretch (longest 25).',
      stays: 'Something pulled you away about 3 to 4 times an hour.',
      ret: 'It took you about 6 minutes to come back.',
      quiet: 'Your phone was quiet for 11% of your work time.',
    ),
    extrasLines: [],
  ),
  steadiness: SteadinessDto(value: 97, word: 'Steady'),
  stones: StonesDto(
    totalStays: 16,
    stoneCount: 0,
    selfStartedCount: 0,
    unknownCount: 16,
    topStoneLabel: null,
    noRippleRate: null,
    line: 'All stays are unknown origin — ping tracking is off.',
  ),
  clearHour: null,
  patterns: [],
  suggestion: null,
  observation: null,
  nothingToFix: true,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 6 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 9. notSeen — gap in collection; those days excluded from numbers.
final notSeen = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [DataFlagDto.notSeen],
  dataLines: [
    'Some days this week were not seen — those days are not included in these numbers.',
  ],
  headline: 'You held 14-minute stretches and 5-minute returns; close to your starting normal.',
  parts: _partsTypical,
  steadiness: SteadinessDto(value: 101, word: 'Steady'),
  stones: _stonesTypical,
  clearHour: null,
  patterns: [_patternRhythm],
  suggestion: null,
  observation: null,
  nothingToFix: true,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 5 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 10. tooLittle — not enough windows to compute anything.
final tooLittle = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.tooLittleData,
  dataFlags: [DataFlagDto.tooLittleData],
  dataLines: ['Not enough windows this week to show comparisons.'],
  headline: 'Not enough data this week.',
  parts: null,
  steadiness: null,
  stones: null,
  clearHour: null,
  patterns: [],
  suggestion: null,
  observation: null,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalNotOffered,
  teacher: null,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: null,
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 11. unusualWeek — outlier week excluded from trends.
final unusualWeek = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [DataFlagDto.unusualWeek],
  dataLines: [
    'This week looks different from your normal pattern and is not included in your trends.',
  ],
  headline: 'You held 9-minute stretches and 9-minute returns; less steady than your starting normal.',
  parts: PartsDto(
    stretchMin: 9.0,
    longestStretchMin: 16.0,
    inSetShare: 0.48,
    quietShare: 0.06,
    staysPerHour: 6.0,
    glances: 10,
    returnMin: 9.0,
    lines: PartLinesDto(
      stretch: 'You stayed in your work apps for about 9 minutes at a stretch (longest 16).',
      stays: 'Something pulled you away about 6 times an hour.',
      ret: 'It took you about 9 minutes to come back.',
      quiet: 'Your phone was quiet for 6% of your work time.',
    ),
    extrasLines: [],
  ),
  steadiness: SteadinessDto(value: 74, word: 'Wavering'),
  stones: _stonesWithTop,
  clearHour: null,
  patterns: [],
  suggestion: null,
  observation: null,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 9 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 12. nothingToFix — all silence rules passed; nothingToFix = true.
final nothingToFix = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 16-minute stretches and 4-minute returns; steadier than your starting normal.',
  parts: PartsDto(
    stretchMin: 16.0,
    longestStretchMin: 30.0,
    inSetShare: 0.72,
    quietShare: 0.14,
    staysPerHour: 2.8,
    glances: 2,
    returnMin: 4.0,
    lines: PartLinesDto(
      stretch: 'You stayed in your work apps for about 16 minutes at a stretch (longest 30).',
      stays: 'Something pulled you away about 3 times an hour.',
      ret: 'It took you about 4 minutes to come back.',
      quiet: 'Your phone was quiet for 14% of your work time.',
    ),
    extrasLines: [],
  ),
  steadiness: SteadinessDto(value: 112, word: 'Steadier'),
  stones: _stonesSelfStarted,
  clearHour: _clearHour,
  patterns: [_patternRhythm, _patternTrend],
  suggestion: null,
  observation: null,
  nothingToFix: true,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek8,
  lapseLine: null,
  saying: _sayingReturn,
  returnLine: 'You came back in about 4 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 13. withSuggestion — active S4 task suggestion.
final withSuggestion = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 11-minute stretches and 7-minute returns; less steady than your starting normal.',
  parts: PartsDto(
    stretchMin: 11.0,
    longestStretchMin: 19.0,
    inSetShare: 0.57,
    quietShare: 0.09,
    staysPerHour: 4.8,
    glances: 7,
    returnMin: 7.0,
    lines: PartLinesDto(
      stretch: 'You stayed in your work apps for about 11 minutes at a stretch (longest 19).',
      stays: 'Something pulled you away about 5 times an hour.',
      ret: 'It took you about 7 minutes to come back.',
      quiet: 'Your phone was quiet for 9% of your work time.',
    ),
    extrasLines: [],
  ),
  steadiness: SteadinessDto(value: 85, word: 'Wavering'),
  stones: _stonesWithTop,
  clearHour: null,
  patterns: [_patternBreak],
  suggestion: _suggestionS4,
  observation: null,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalOffered,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingNeutral,
  returnLine: 'You came back in about 7 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 14. withVerdict — a MOVED verdict from a completed experiment.
final withVerdict = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 15-minute stretches and 4-minute returns; steadier than your starting normal.',
  parts: _partsTypical,
  steadiness: SteadinessDto(value: 108, word: 'Steady'),
  stones: _stonesTypical,
  clearHour: _clearHour,
  patterns: [_patternRhythm],
  suggestion: _suggestionS1,
  observation: null,
  nothingToFix: false,
  verdict: _verdictMoved,
  goalTap: _goalAnsweredPartly,
  teacher: _teacherWeek4,
  lapseLine: null,
  saying: _sayingReturn,
  returnLine: 'You came back in about 4 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 15. afterLapse — returning after 4 days away; S11 observation shown.
final afterLapse = MirrorDto(
  isDemo: false,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: _weekLabel,
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 13-minute stretches and 6-minute returns; close to your starting normal.',
  parts: _partsTypical,
  steadiness: SteadinessDto(value: 95, word: 'Steady'),
  stones: _stonesTypical,
  clearHour: null,
  patterns: [],
  suggestion: null,
  observation: _observationS11,
  nothingToFix: false,
  verdict: null,
  goalTap: _goalNotOffered,
  teacher: null,
  lapseLine: 'You were away 4 days. Your starting normal is still here.',
  saying: _sayingLapse,
  returnLine: 'You came back in about 6 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

/// 16. demoAarav — Aarav persona, Week 4 (day 31). isDemo = true.
///     Steadiness ≈ 116 per DOC 3 persona spec (placeholder; calibrated at T4.7).
final demoAarav = MirrorDto(
  isDemo: true,
  provisional: false,
  gentle: false,
  weekStartEpochMs: _weekMs,
  weekLabel: 'Week 4 (Demo)',
  dataState: DataStateDto.ok,
  dataFlags: [],
  dataLines: [],
  headline: 'You held 17-minute stretches and 4-minute returns; steadier than your starting normal.',
  parts: PartsDto(
    stretchMin: 17.0,
    longestStretchMin: 32.0,
    inSetShare: 0.73,
    quietShare: 0.14,
    staysPerHour: 2.6,
    glances: 3,
    returnMin: 3.9,
    lines: PartLinesDto(
      stretch: 'You stayed in your work apps for about 17 minutes at a stretch (longest 32).',
      stays: 'Something pulled you away about 3 times an hour.',
      ret: 'It took you about 4 minutes to come back.',
      quiet: 'Your phone was quiet for 14% of your work time.',
    ),
    extrasLines: [],
  ),
  steadiness: SteadinessDto(value: 116, word: 'Steadier'),
  stones: _stonesSelfStarted,
  clearHour: _clearHour,
  patterns: [_patternRhythm, _patternBreak],
  suggestion: null,
  observation: null,
  nothingToFix: true,
  verdict: null,
  goalTap: _goalOffered,
  teacher: TeacherDto(
    opensThisWeek: 3,
    opensPrevWeek: 5,
    minutesThisWeek: 10.0,
    line: 'You opened Sakshi 3 times this week.',
  ),
  lapseLine: null,
  saying: _sayingReturn,
  returnLine: 'You came back in about 4 minutes on average.',
  reanchorOffered: false,
  suggestedStudyBlock: null,
);

// ─────────────────────────────────────────────────────────────────────────────
// Lookup map (used by the fixture test)
// ─────────────────────────────────────────────────────────────────────────────

final Map<String, MirrorDto> allMirrorFixtures = {
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
};

// ─────────────────────────────────────────────────────────────────────────────
// TodayDto fixtures
// ─────────────────────────────────────────────────────────────────────────────

final todayTypical = TodayDto(
  isDemo: false,
  windows: [
    TodayWindowDto(
      startEpochMs: 1759632600000,
      endEpochMs: 1759636200000,
      stays: 3,
      shape: 'HELD',
      stretchMin: 22.0,
      returnMin: 4.5,
    ),
    TodayWindowDto(
      startEpochMs: 1759640400000,
      endEpochMs: 1759645800000,
      stays: 5,
      shape: 'PINGED',
      stretchMin: 14.0,
      returnMin: 6.0,
    ),
  ],
  line: 'Two windows today. You came back in about 5 minutes on average.',
  dataFlags: [],
  dataLines: [],
  parts: _partsTypical,
);

final todayEmpty = TodayDto(
  isDemo: false,
  windows: [],
  line: 'No windows yet today.',
  dataFlags: [],
  dataLines: [],
  parts: null,
);

final todayPartialPing = TodayDto(
  isDemo: false,
  windows: [
    TodayWindowDto(
      startEpochMs: 1759632600000,
      endEpochMs: 1759636200000,
      stays: 4,
      shape: null,
      stretchMin: 12.0,
      returnMin: null,
    ),
  ],
  line: 'One window today.',
  dataFlags: [DataFlagDto.partialPing],
  dataLines: [
    'Ping tracking was active for part of today — some stays are listed as unknown origin.',
  ],
  parts: null,
);

final todayDemo = TodayDto(
  isDemo: true,
  windows: [
    TodayWindowDto(
      startEpochMs: 1759632600000,
      endEpochMs: 1759640200000,
      stays: 2,
      shape: 'HELD',
      stretchMin: 25.0,
      returnMin: 3.5,
    ),
  ],
  line: 'One window today. You came back in about 4 minutes on average.',
  dataFlags: [],
  dataLines: [],
  parts: null,
);

// ─────────────────────────────────────────────────────────────────────────────
// LakeDto fixtures — one per LakeStateDto value (all five)
// ─────────────────────────────────────────────────────────────────────────────

final lakeStill = LakeDto(
  state: LakeStateDto.still,
  phrase: 'Still water.',
  isDemo: false,
  asOfEpochMs: 1759636200000,
);

final lakeRippled = LakeDto(
  state: LakeStateDto.rippled,
  phrase: 'A few ripples.',
  isDemo: false,
  asOfEpochMs: 1759636200000,
);

final lakeChoppy = LakeDto(
  state: LakeStateDto.choppy,
  phrase: 'Choppy water.',
  isDemo: false,
  asOfEpochMs: 1759636200000,
);

final lakeLearning = LakeDto(
  state: LakeStateDto.learning,
  phrase: 'Learning your normal.',
  isDemo: false,
  asOfEpochMs: null,
);

final lakeNoData = LakeDto(
  state: LakeStateDto.noData,
  phrase: 'Nothing to show yet.',
  isDemo: false,
  asOfEpochMs: null,
);

final Map<String, LakeDto> allLakeFixtures = {
  'still': lakeStill,
  'rippled': lakeRippled,
  'choppy': lakeChoppy,
  'learning': lakeLearning,
  'noData': lakeNoData,
};

// ─────────────────────────────────────────────────────────────────────────────
// WhatISeeDto fixtures
// ─────────────────────────────────────────────────────────────────────────────

final whatISeeTypical = WhatISeeDto(
  isDemo: false,
  usageAccessGranted: true,
  notificationAccessGranted: true,
  rawEventCount: 4821,
  notifEventCount: 312,
  derivedDays: 14,
  workerRuns7d: 62,
  paused: false,
  oddEventPairs: 0,
  lines: [
    'Usage access: on.',
    'Ping tracking: on.',
    'Raw events kept: 4 821 (14 days).',
    'Notification events kept: 312 (14 days).',
    'Background runs in the last 7 days: 62.',
    'Last background run: 18 minutes ago.',
    'Ping tracking active for 89% of the last 7 days.',
  ],
  oldestRawEpochMs: 1758398400000,
  listenerCoverage7d: 0.89,
  lastWorkerRunEpochMs: 1759699200000,
  lastError: null,
);

final whatISeePartialPing = WhatISeeDto(
  isDemo: false,
  usageAccessGranted: true,
  notificationAccessGranted: true,
  rawEventCount: 3100,
  notifEventCount: 88,
  derivedDays: 10,
  workerRuns7d: 41,
  paused: false,
  oddEventPairs: 2,
  lines: [
    'Usage access: on.',
    'Ping tracking: on.',
    'Raw events kept: 3 100 (10 days).',
    'Notification events kept: 88 (10 days).',
    'Background runs in the last 7 days: 41.',
    'Last background run: 2 hours ago.',
    'Ping tracking active for 61% of the last 7 days.',
  ],
  oldestRawEpochMs: 1758830400000,
  listenerCoverage7d: 0.61,
  lastWorkerRunEpochMs: 1759692000000,
  lastError: null,
);

final whatISeeNoNotif = WhatISeeDto(
  isDemo: false,
  usageAccessGranted: true,
  notificationAccessGranted: false,
  rawEventCount: 2200,
  notifEventCount: 0,
  derivedDays: 8,
  workerRuns7d: 55,
  paused: false,
  oddEventPairs: 0,
  lines: [
    'Usage access: on.',
    'Ping tracking: off.',
    'Raw events kept: 2 200 (8 days).',
    'Background runs in the last 7 days: 55.',
    'Last background run: 5 minutes ago.',
  ],
  oldestRawEpochMs: 1759003200000,
  listenerCoverage7d: null,
  lastWorkerRunEpochMs: 1759699500000,
  lastError: null,
);

final whatISeeDemo = WhatISeeDto(
  isDemo: true,
  usageAccessGranted: true,
  notificationAccessGranted: true,
  rawEventCount: 8640,
  notifEventCount: 720,
  derivedDays: 60,
  workerRuns7d: 0,
  paused: false,
  oddEventPairs: 0,
  lines: [
    'Demo data — Aarav persona, day 31.',
    'Raw events kept: 8 640 (synthetic).',
    'Notification events kept: 720 (synthetic).',
  ],
  oldestRawEpochMs: null,
  listenerCoverage7d: null,
  lastWorkerRunEpochMs: null,
  lastError: null,
);

// ─────────────────────────────────────────────────────────────────────────────
// SayingDto list — three choices for the picker
// ─────────────────────────────────────────────────────────────────────────────

final List<SayingDto> sayingChoices = [
  _sayingNeutral,
  _sayingReturn,
  SayingDto(
    id: 'sy01',
    text: 'I am watching my mind act',
    source: 'Complete Works, Volume 8, 8.1.2 Fourth Lesson',
    tierLabel: 'Reported by others',
    question: 86,
  ),
];
