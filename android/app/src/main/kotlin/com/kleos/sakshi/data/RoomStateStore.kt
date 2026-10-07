package com.kleos.sakshi.data

import com.kleos.sakshi.data.entities.toEntity
import com.kleos.sakshi.engine.model.AppMeta
import com.kleos.sakshi.engine.model.Baseline
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Experiment
import com.kleos.sakshi.engine.model.GoalTap
import com.kleos.sakshi.engine.model.IngestState
import com.kleos.sakshi.engine.model.LakeRow
import com.kleos.sakshi.engine.model.NoteState
import com.kleos.sakshi.engine.model.SayingPick
import com.kleos.sakshi.engine.model.Settings
import com.kleos.sakshi.engine.model.SuggestionState
import com.kleos.sakshi.engine.ports.StateStore

/**
 * Single-row state (settings, ingest, note) reads back a plain default until first saved. `createdAt` in that default is
 * epoch 0, not a guess at "now"; the host saves real settings at first launch.
 */
class RoomStateStore(private val db: SakshiDatabase) : StateStore {
    private val dao get() = db.state()

    /** The active baseline. Re-anchoring saves a new active row and the old one with isActive = false. */
    override fun baseline(): Baseline? = dao.activeBaseline()?.toModel()
    override fun saveBaseline(b: Baseline) = dao.saveBaseline(b.toEntity())

    override fun settings(): Settings = dao.settings()?.toModel() ?: DEFAULT_SETTINGS
    override fun saveSettings(s: Settings) = dao.saveSettings(s.toEntity())

    override fun apps(): List<AppMeta> = dao.apps().map { it.toModel() }
    override fun replaceApps(a: List<AppMeta>) = dao.replaceApps(a.map { it.toEntity() })

    override fun suggestionStates(): List<SuggestionState> = dao.suggestionStates().map { it.toModel() }
    override fun saveSuggestionState(s: SuggestionState) = dao.saveSuggestionState(s.toEntity())

    override fun experiments(): List<Experiment> = dao.experiments().map { it.toModel() }
    override fun saveExperiment(e: Experiment) = dao.saveExperiment(e.toEntity())

    override fun sayingPicks(): List<SayingPick> = dao.sayingPicks().map { it.toModel() }
    override fun savePick(p: SayingPick) = dao.savePick(p.toEntity())

    override fun goalTaps(): List<GoalTap> = dao.goalTaps().map { it.toModel() }
    override fun saveGoalTap(g: GoalTap) = dao.saveGoalTap(g.toEntity())

    override fun lake(): LakeRow? = dao.lake()?.toModel()
    override fun saveLake(l: LakeRow) = dao.saveLake(l.toEntity())

    override fun note(): NoteState = dao.note()?.toModel() ?: NoteState(null, null, null)
    override fun saveNote(n: NoteState) = dao.saveNote(n.toEntity())

    override fun ingest(): IngestState = dao.ingest()?.toModel() ?: DEFAULT_INGEST
    override fun saveIngest(i: IngestState) = dao.saveIngest(i.toEntity())

    private companion object {
        val DEFAULT_SETTINGS = Settings(
            studyBlocks = emptyList(), learnStudyHours = false, gentleMode = false, gentleExplicit = false,
            weeklyNoteEnabled = false, ageUnder18 = false, batteryHelperShown = false, lapseAcknowledgedThrough = null,
            firstReadAt = null, createdAt = EpochMs(0))
        val DEFAULT_INGEST = IngestState(
            cursor = null, lastRunAt = null, paused = false, pausedSince = null, firstReadAt = null, lastWorkerRunAt = null,
            workerRuns7d = 0, lastError = null, oddEventPairs = 0)
    }
}
