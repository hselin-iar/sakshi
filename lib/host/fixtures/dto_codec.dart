// lib/host/fixtures/dto_codec.dart
// T4.7 — JSON serializers and deserializers for Sakshi DTOs.
// Used by contract tests and golden fixture synchronization.

import 'package:sakshi/gen/sakshi_api.g.dart';

// ─────────────────────────────────────────────────────────────────────────────
// MirrorDto Codec
// ─────────────────────────────────────────────────────────────────────────────

Map<String, dynamic> mirrorDtoToJson(MirrorDto dto) => {
  'isDemo': dto.isDemo,
  'provisional': dto.provisional,
  'gentle': dto.gentle,
  'weekStartEpochMs': dto.weekStartEpochMs,
  'weekLabel': dto.weekLabel,
  'dataState': dto.dataState.name,
  'dataFlags': dto.dataFlags.map((f) => f.name).toList(),
  'dataLines': dto.dataLines,
  'headline': dto.headline,
  if (dto.parts != null) 'parts': partsDtoToJson(dto.parts!),
  if (dto.steadiness != null) 'steadiness': steadinessDtoToJson(dto.steadiness!),
  if (dto.stones != null) 'stones': stonesDtoToJson(dto.stones!),
  if (dto.clearHour != null) 'clearHour': clearHourDtoToJson(dto.clearHour!),
  'patterns': dto.patterns.map(patternLineDtoToJson).toList(),
  if (dto.suggestion != null) 'suggestion': suggestionDtoToJson(dto.suggestion!),
  if (dto.observation != null) 'observation': observationDtoToJson(dto.observation!),
  'nothingToFix': dto.nothingToFix,
  if (dto.verdict != null) 'verdict': verdictDtoToJson(dto.verdict!),
  'goalTap': goalTapDtoToJson(dto.goalTap),
  if (dto.teacher != null) 'teacher': teacherDtoToJson(dto.teacher!),
  if (dto.lapseLine != null) 'lapseLine': dto.lapseLine,
  if (dto.saying != null) 'saying': sayingDtoToJson(dto.saying!),
  if (dto.returnLine != null) 'returnLine': dto.returnLine,
  'reanchorOffered': dto.reanchorOffered,
  if (dto.suggestedStudyBlock != null) 'suggestedStudyBlock': studyBlockDtoToJson(dto.suggestedStudyBlock!),
};

MirrorDto mirrorDtoFromJson(Map<String, dynamic> json) => MirrorDto(
  isDemo: json['isDemo'] as bool,
  provisional: json['provisional'] as bool,
  gentle: json['gentle'] as bool,
  weekStartEpochMs: json['weekStartEpochMs'] as int,
  weekLabel: json['weekLabel'] as String,
  dataState: DataStateDto.values.byName(json['dataState'] as String),
  dataFlags: (json['dataFlags'] as List).map((f) => DataFlagDto.values.byName(f as String)).toList(),
  dataLines: (json['dataLines'] as List).cast<String>(),
  headline: json['headline'] as String,
  parts: json['parts'] != null ? partsDtoFromJson(json['parts'] as Map<String, dynamic>) : null,
  steadiness: json['steadiness'] != null ? steadinessDtoFromJson(json['steadiness'] as Map<String, dynamic>) : null,
  stones: json['stones'] != null ? stonesDtoFromJson(json['stones'] as Map<String, dynamic>) : null,
  clearHour: json['clearHour'] != null ? clearHourDtoFromJson(json['clearHour'] as Map<String, dynamic>) : null,
  patterns: (json['patterns'] as List).map((p) => patternLineDtoFromJson(p as Map<String, dynamic>)).toList(),
  suggestion: json['suggestion'] != null ? suggestionDtoFromJson(json['suggestion'] as Map<String, dynamic>) : null,
  observation: json['observation'] != null ? observationDtoFromJson(json['observation'] as Map<String, dynamic>) : null,
  nothingToFix: json['nothingToFix'] as bool,
  verdict: json['verdict'] != null ? verdictDtoFromJson(json['verdict'] as Map<String, dynamic>) : null,
  goalTap: goalTapDtoFromJson(json['goalTap'] as Map<String, dynamic>),
  teacher: json['teacher'] != null ? teacherDtoFromJson(json['teacher'] as Map<String, dynamic>) : null,
  lapseLine: json['lapseLine'] as String?,
  saying: json['saying'] != null ? sayingDtoFromJson(json['saying'] as Map<String, dynamic>) : null,
  returnLine: json['returnLine'] as String?,
  reanchorOffered: json['reanchorOffered'] as bool,
  suggestedStudyBlock: json['suggestedStudyBlock'] != null ? studyBlockDtoFromJson(json['suggestedStudyBlock'] as Map<String, dynamic>) : null,
);

// ─────────────────────────────────────────────────────────────────────────────
// Sub-DTO Codecs
// ─────────────────────────────────────────────────────────────────────────────

Map<String, dynamic> partsDtoToJson(PartsDto dto) => {
  if (dto.stretchMin != null) 'stretchMin': dto.stretchMin,
  if (dto.longestStretchMin != null) 'longestStretchMin': dto.longestStretchMin,
  if (dto.inSetShare != null) 'inSetShare': dto.inSetShare,
  if (dto.quietShare != null) 'quietShare': dto.quietShare,
  if (dto.staysPerHour != null) 'staysPerHour': dto.staysPerHour,
  'glances': dto.glances,
  if (dto.returnMin != null) 'returnMin': dto.returnMin,
  'lines': {
    if (dto.lines.stretch != null) 'stretch': dto.lines.stretch,
    if (dto.lines.stays != null) 'stays': dto.lines.stays,
    if (dto.lines.ret != null) 'ret': dto.lines.ret,
    if (dto.lines.quiet != null) 'quiet': dto.lines.quiet,
  },
  'extrasLines': dto.extrasLines,
};

PartsDto partsDtoFromJson(Map<String, dynamic> json) => PartsDto(
  stretchMin: (json['stretchMin'] as num?)?.toDouble(),
  longestStretchMin: (json['longestStretchMin'] as num?)?.toDouble(),
  inSetShare: (json['inSetShare'] as num?)?.toDouble(),
  quietShare: (json['quietShare'] as num?)?.toDouble(),
  staysPerHour: (json['staysPerHour'] as num?)?.toDouble(),
  glances: json['glances'] as int,
  returnMin: (json['returnMin'] as num?)?.toDouble(),
  lines: PartLinesDto(
    stretch: (json['lines'] as Map<String, dynamic>?)?['stretch'] as String?,
    stays: (json['lines'] as Map<String, dynamic>?)?['stays'] as String?,
    ret: (json['lines'] as Map<String, dynamic>?)?['ret'] as String?,
    quiet: (json['lines'] as Map<String, dynamic>?)?['quiet'] as String?,
  ),
  extrasLines: (json['extrasLines'] as List).cast<String>(),
);

Map<String, dynamic> steadinessDtoToJson(SteadinessDto dto) => {
  'value': dto.value,
  'word': dto.word,
};

SteadinessDto steadinessDtoFromJson(Map<String, dynamic> json) => SteadinessDto(
  value: json['value'] as int,
  word: json['word'] as String,
);

Map<String, dynamic> stonesDtoToJson(StonesDto dto) => {
  'totalStays': dto.totalStays,
  'stoneCount': dto.stoneCount,
  'selfStartedCount': dto.selfStartedCount,
  'unknownCount': dto.unknownCount,
  if (dto.topStoneLabel != null) 'topStoneLabel': dto.topStoneLabel,
  if (dto.noRippleRate != null) 'noRippleRate': dto.noRippleRate,
  'line': dto.line,
};

StonesDto stonesDtoFromJson(Map<String, dynamic> json) => StonesDto(
  totalStays: json['totalStays'] as int,
  stoneCount: json['stoneCount'] as int,
  selfStartedCount: json['selfStartedCount'] as int,
  unknownCount: json['unknownCount'] as int,
  topStoneLabel: json['topStoneLabel'] as String?,
  noRippleRate: (json['noRippleRate'] as num?)?.toDouble(),
  line: json['line'] as String,
);

Map<String, dynamic> clearHourDtoToJson(ClearHourDto dto) => {
  'startHour': dto.startHour,
  'endHour': dto.endHour,
  'stretchMin': dto.stretchMin,
  'line': dto.line,
};

ClearHourDto clearHourDtoFromJson(Map<String, dynamic> json) => ClearHourDto(
  startHour: json['startHour'] as int,
  endHour: json['endHour'] as int,
  stretchMin: (json['stretchMin'] as num).toDouble(),
  line: json['line'] as String,
);

Map<String, dynamic> patternLineDtoToJson(PatternLineDto dto) => {
  'kindId': dto.kindId,
  'line': dto.line,
  'evidenceWindows': dto.evidenceWindows,
  'evidenceDays': dto.evidenceDays,
};

PatternLineDto patternLineDtoFromJson(Map<String, dynamic> json) => PatternLineDto(
  kindId: json['kindId'] as String,
  line: json['line'] as String,
  evidenceWindows: json['evidenceWindows'] as int,
  evidenceDays: json['evidenceDays'] as int,
);

Map<String, dynamic> suggestionDtoToJson(SuggestionDto dto) => {
  'kindId': dto.kindId,
  'line': dto.line,
  'actionLabel': dto.actionLabel,
  'actionType': dto.actionType,
  'opensSettings': dto.opensSettings,
  if (dto.subjectKey != null) 'subjectKey': dto.subjectKey,
};

SuggestionDto suggestionDtoFromJson(Map<String, dynamic> json) => SuggestionDto(
  kindId: json['kindId'] as String,
  line: json['line'] as String,
  actionLabel: json['actionLabel'] as String,
  actionType: json['actionType'] as String,
  opensSettings: json['opensSettings'] as bool,
  subjectKey: json['subjectKey'] as String?,
);

ObservationDto observationDtoFromJson(Map<String, dynamic> json) => ObservationDto(
  kindId: json['kindId'] as String,
  line: json['line'] as String,
);

Map<String, dynamic> observationDtoToJson(ObservationDto dto) => {
  'kindId': dto.kindId,
  'line': dto.line,
};

Map<String, dynamic> verdictDtoToJson(VerdictDto dto) => {
  'verdict': dto.verdict,
  'line': dto.line,
  'approxMix': dto.approxMix,
  if (dto.beforeValue != null) 'beforeValue': dto.beforeValue,
  if (dto.afterValue != null) 'afterValue': dto.afterValue,
};

VerdictDto verdictDtoFromJson(Map<String, dynamic> json) => VerdictDto(
  verdict: json['verdict'] as String,
  line: json['line'] as String,
  approxMix: json['approxMix'] as bool,
  beforeValue: (json['beforeValue'] as num?)?.toDouble(),
  afterValue: (json['afterValue'] as num?)?.toDouble(),
);

Map<String, dynamic> goalTapDtoToJson(GoalTapDto dto) => {
  'offered': dto.offered,
  if (dto.answer != null) 'answer': dto.answer!.name,
};

GoalTapDto goalTapDtoFromJson(Map<String, dynamic> json) => GoalTapDto(
  offered: json['offered'] as bool,
  answer: json['answer'] != null ? GoalAnswerDto.values.byName(json['answer'] as String) : null,
);

TeacherDto teacherDtoFromJson(Map<String, dynamic> json) => TeacherDto(
  opensThisWeek: json['opensThisWeek'] as int,
  opensPrevWeek: json['opensPrevWeek'] as int,
  minutesThisWeek: (json['minutesThisWeek'] as num).toDouble(),
  line: json['line'] as String,
);

Map<String, dynamic> teacherDtoToJson(TeacherDto dto) => {
  'opensThisWeek': dto.opensThisWeek,
  'opensPrevWeek': dto.opensPrevWeek,
  'minutesThisWeek': dto.minutesThisWeek,
  'line': dto.line,
};

StudyBlockDto studyBlockDtoFromJson(Map<String, dynamic> json) => StudyBlockDto(
  startMinute: json['startMinute'] as int,
  endMinute: json['endMinute'] as int,
);

Map<String, dynamic> studyBlockDtoToJson(StudyBlockDto dto) => {
  'startMinute': dto.startMinute,
  'endMinute': dto.endMinute,
};

SayingDto sayingDtoFromJson(Map<String, dynamic> json) => SayingDto(
  id: json['id'] as String,
  text: json['text'] as String,
  source: json['source'] as String,
  tierLabel: json['tierLabel'] as String,
  question: json['question'] as int,
);

Map<String, dynamic> sayingDtoToJson(SayingDto dto) => {
  'id': dto.id,
  'text': dto.text,
  'source': dto.source,
  'tierLabel': dto.tierLabel,
  'question': dto.question,
};

// ─────────────────────────────────────────────────────────────────────────────
// TodayDto Codec
// ─────────────────────────────────────────────────────────────────────────────

Map<String, dynamic> todayDtoToJson(TodayDto dto) => {
  'isDemo': dto.isDemo,
  'windows': dto.windows.map((w) => {
    'startEpochMs': w.startEpochMs,
    'endEpochMs': w.endEpochMs,
    'stays': w.stays,
    if (w.shape != null) 'shape': w.shape,
    if (w.stretchMin != null) 'stretchMin': w.stretchMin,
    if (w.returnMin != null) 'returnMin': w.returnMin,
  }).toList(),
  'line': dto.line,
  'dataFlags': dto.dataFlags.map((f) => f.name).toList(),
  'dataLines': dto.dataLines,
  if (dto.parts != null) 'parts': partsDtoToJson(dto.parts!),
};

TodayDto todayDtoFromJson(Map<String, dynamic> json) => TodayDto(
  isDemo: json['isDemo'] as bool,
  windows: (json['windows'] as List).map((w) => TodayWindowDto(
    startEpochMs: (w as Map<String, dynamic>)['startEpochMs'] as int,
    endEpochMs: w['endEpochMs'] as int,
    stays: w['stays'] as int,
    shape: w['shape'] as String?,
    stretchMin: (w['stretchMin'] as num?)?.toDouble(),
    returnMin: (w['returnMin'] as num?)?.toDouble(),
  )).toList(),
  line: json['line'] as String,
  dataFlags: (json['dataFlags'] as List).map((f) => DataFlagDto.values.byName(f as String)).toList(),
  dataLines: (json['dataLines'] as List).cast<String>(),
  parts: json['parts'] != null ? partsDtoFromJson(json['parts'] as Map<String, dynamic>) : null,
);

// ─────────────────────────────────────────────────────────────────────────────
// LakeDto Codec
// ─────────────────────────────────────────────────────────────────────────────

Map<String, dynamic> lakeDtoToJson(LakeDto dto) => {
  'state': dto.state.name,
  'phrase': dto.phrase,
  'isDemo': dto.isDemo,
  if (dto.asOfEpochMs != null) 'asOfEpochMs': dto.asOfEpochMs,
};

LakeDto lakeDtoFromJson(Map<String, dynamic> json) => LakeDto(
  state: LakeStateDto.values.byName(json['state'] as String),
  phrase: json['phrase'] as String,
  isDemo: json['isDemo'] as bool,
  asOfEpochMs: json['asOfEpochMs'] as int?,
);

// ─────────────────────────────────────────────────────────────────────────────
// WhatISeeDto Codec
// ─────────────────────────────────────────────────────────────────────────────

Map<String, dynamic> whatISeeDtoToJson(WhatISeeDto dto) => {
  'isDemo': dto.isDemo,
  'usageAccessGranted': dto.usageAccessGranted,
  'notificationAccessGranted': dto.notificationAccessGranted,
  'rawEventCount': dto.rawEventCount,
  'notifEventCount': dto.notifEventCount,
  'derivedDays': dto.derivedDays,
  'workerRuns7d': dto.workerRuns7d,
  'paused': dto.paused,
  'oddEventPairs': dto.oddEventPairs,
  'lines': dto.lines,
  if (dto.oldestRawEpochMs != null) 'oldestRawEpochMs': dto.oldestRawEpochMs,
  if (dto.listenerCoverage7d != null) 'listenerCoverage7d': dto.listenerCoverage7d,
  if (dto.lastWorkerRunEpochMs != null) 'lastWorkerRunEpochMs': dto.lastWorkerRunEpochMs,
  if (dto.lastError != null) 'lastError': dto.lastError,
};

WhatISeeDto whatISeeDtoFromJson(Map<String, dynamic> json) => WhatISeeDto(
  isDemo: json['isDemo'] as bool,
  usageAccessGranted: json['usageAccessGranted'] as bool,
  notificationAccessGranted: json['notificationAccessGranted'] as bool,
  rawEventCount: json['rawEventCount'] as int,
  notifEventCount: json['notifEventCount'] as int,
  derivedDays: json['derivedDays'] as int,
  workerRuns7d: json['workerRuns7d'] as int,
  paused: json['paused'] as bool,
  oddEventPairs: json['oddEventPairs'] as int,
  lines: (json['lines'] as List).cast<String>(),
  oldestRawEpochMs: json['oldestRawEpochMs'] as int?,
  listenerCoverage7d: (json['listenerCoverage7d'] as num?)?.toDouble(),
  lastWorkerRunEpochMs: json['lastWorkerRunEpochMs'] as int?,
  lastError: json['lastError'] as String?,
);
