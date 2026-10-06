package com.kleos.sakshi.engine.ports

import com.kleos.sakshi.engine.model.*

interface Clock            { fun now(): EpochMs }
interface Randomness       { fun nextDouble(): Double; fun nextInt(bound: Int): Int; fun fork(seed: Long): Randomness }
interface EventStore       { fun append(events: List<RawEvent>); fun range(from: EpochMs, to: EpochMs): List<RawEvent>; fun oldest(): EpochMs?; fun count(): Int; fun purgeBefore(ts: EpochMs): Int }
interface NotifStore       { fun append(e: NotifEvent); fun range(from: EpochMs, to: EpochMs): List<NotifEvent>; fun count(): Int; fun purgeBefore(ts: EpochMs): Int }
interface ListenerCoverage { fun sessions(from: EpochMs, to: EpochMs): List<ListenerSession>; fun coversInterval(from: EpochMs, to: EpochMs): Boolean; fun coverageFraction(from: EpochMs, to: EpochMs): Double
fun openSession(at: EpochMs); fun closeSession(at: EpochMs) }
interface GapStore         { fun add(gap: DataGap); fun closeOpenPause(at: EpochMs); fun overlapping(from: EpochMs, to: EpochMs): List<DataGap> }
interface DerivedStore     { fun replaceDay(day: StudyDay, d: DayDerivation); fun windows(from: EpochMs, to: EpochMs): List<WindowWithDetail>; fun days(from: StudyDay, to: StudyDay): List<DaySummary>
fun upsertWeek(w: WeekSummary); fun weeks(): List<WeekSummary>; fun upsertPatterns(p: List<Pattern>); fun patterns(): List<Pattern>; fun clearDerived(); fun clearAll() }
interface StateStore       { fun baseline(): Baseline?; fun saveBaseline(b: Baseline); fun settings(): Settings; fun saveSettings(s: Settings)
fun apps(): List<AppMeta>; fun replaceApps(a: List<AppMeta>); fun suggestionStates(): List<SuggestionState>; fun saveSuggestionState(s: SuggestionState)
fun experiments(): List<Experiment>; fun saveExperiment(e: Experiment); fun sayingPicks(): List<SayingPick>; fun savePick(p: SayingPick)
fun goalTaps(): List<GoalTap>; fun saveGoalTap(g: GoalTap); fun lake(): LakeRow?; fun saveLake(l: LakeRow); fun note(): NoteState; fun saveNote(n: NoteState)
fun ingest(): IngestState; fun saveIngest(i: IngestState) }
interface AppCatalog       { fun launcherApps(): List<AppInfo>; fun category(pkg: Pkg): Int?; fun isNeutral(pkg: Pkg): Boolean; fun ownPackage(): Pkg }
interface SayingShelf      { fun all(): List<Saying> }
data class Ports(val events: EventStore, val notifs: NotifStore, val coverage: ListenerCoverage, val gaps: GapStore, val derived: DerivedStore, val state: StateStore,
val catalog: AppCatalog, val shelf: SayingShelf, val clock: Clock, val random: Randomness)
