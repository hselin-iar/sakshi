package com.kleos.sakshi.engine.testkit

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
 * Dumb in-memory store. settings()/note()/ingest() are non-nullable in the
 * port, so this starts from a neutral default a test can override by calling
 * saveSettings/saveNote/saveIngest before exercising the thing under test.
 */
class FakeStateStore(
    initialSettings: Settings = defaultSettings(),
    initialNote: NoteState = defaultNote(),
    initialIngest: IngestState = defaultIngest(),
) : StateStore {
    private var settingsValue: Settings = initialSettings
    private var noteValue: NoteState = initialNote
    private var ingestValue: IngestState = initialIngest
    private var baselineValue: Baseline? = null
    private var lakeValue: LakeRow? = null

    private val appList = mutableListOf<AppMeta>()
    private val suggestionList = mutableListOf<SuggestionState>()
    private val experimentList = mutableListOf<Experiment>()
    private val sayingPickList = mutableListOf<SayingPick>()
    private val goalTapList = mutableListOf<GoalTap>()

    override fun baseline(): Baseline? = baselineValue
    override fun saveBaseline(b: Baseline) {
        baselineValue = b
    }

    override fun settings(): Settings = settingsValue
    override fun saveSettings(s: Settings) {
        settingsValue = s
    }

    override fun apps(): List<AppMeta> = appList.toList()
    override fun replaceApps(a: List<AppMeta>) {
        appList.clear()
        appList += a
    }

    override fun suggestionStates(): List<SuggestionState> = suggestionList.toList()
    override fun saveSuggestionState(s: SuggestionState) {
        val index = suggestionList.indexOfFirst { it.kind == s.kind && it.subject == s.subject }
        if (index >= 0) suggestionList[index] = s else suggestionList += s
    }

    override fun experiments(): List<Experiment> = experimentList.toList()
    override fun saveExperiment(e: Experiment) {
        val index = experimentList.indexOfFirst { it.id == e.id }
        if (index >= 0) experimentList[index] = e else experimentList += e
    }

    override fun sayingPicks(): List<SayingPick> = sayingPickList.toList()
    override fun savePick(p: SayingPick) {
        sayingPickList += p
    }

    override fun goalTaps(): List<GoalTap> = goalTapList.toList()
    override fun saveGoalTap(g: GoalTap) {
        val index = goalTapList.indexOfFirst { it.weekStart == g.weekStart }
        if (index >= 0) goalTapList[index] = g else goalTapList += g
    }

    override fun lake(): LakeRow? = lakeValue
    override fun saveLake(l: LakeRow) {
        lakeValue = l
    }

    override fun note(): NoteState = noteValue
    override fun saveNote(n: NoteState) {
        noteValue = n
    }

    override fun ingest(): IngestState = ingestValue
    override fun saveIngest(i: IngestState) {
        ingestValue = i
    }

    companion object {
        fun defaultSettings(): Settings = Settings(
            studyBlocks = emptyList(),
            learnStudyHours = false,
            gentleMode = false,
            gentleExplicit = false,
            weeklyNoteEnabled = false,
            ageUnder18 = false,
            batteryHelperShown = false,
            lapseAcknowledgedThrough = null,
            firstReadAt = null,
            createdAt = EpochMs(0),
        )

        fun defaultNote(): NoteState = NoteState(
            lastNoteWeek = null,
            mirrorReadyWeek = null,
            mirrorViewedWeek = null,
        )

        fun defaultIngest(): IngestState = IngestState(
            cursor = null,
            lastRunAt = null,
            paused = false,
            pausedSince = null,
            firstReadAt = null,
            lastWorkerRunAt = null,
            workerRuns7d = 0,
            lastError = null,
            oddEventPairs = 0,
        )
    }
}
