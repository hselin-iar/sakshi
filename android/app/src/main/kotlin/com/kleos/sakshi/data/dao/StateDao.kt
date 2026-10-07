package com.kleos.sakshi.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kleos.sakshi.data.entities.AppMetaEntity
import com.kleos.sakshi.data.entities.BaselineEntity
import com.kleos.sakshi.data.entities.ExperimentEntity
import com.kleos.sakshi.data.entities.GoalTapEntity
import com.kleos.sakshi.data.entities.IngestStateEntity
import com.kleos.sakshi.data.entities.LakeRowEntity
import com.kleos.sakshi.data.entities.NoteStateEntity
import com.kleos.sakshi.data.entities.SayingPickEntity
import com.kleos.sakshi.data.entities.SettingsEntity
import com.kleos.sakshi.data.entities.SuggestionStateEntity

@Dao
interface StateDao {
    @Query("SELECT * FROM baseline WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun activeBaseline(): BaselineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveBaseline(baseline: BaselineEntity)

    @Query("SELECT * FROM settings WHERE id = 0")
    fun settings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSettings(settings: SettingsEntity)

    @Query("SELECT * FROM app_meta ORDER BY pkg")
    fun apps(): List<AppMetaEntity>

    @Query("DELETE FROM app_meta")
    fun clearApps()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertApps(apps: List<AppMetaEntity>)

    @Transaction
    fun replaceApps(apps: List<AppMetaEntity>) {
        clearApps()
        insertApps(apps)
    }

    @Query("SELECT * FROM suggestion_state ORDER BY kind, subject")
    fun suggestionStates(): List<SuggestionStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSuggestionState(state: SuggestionStateEntity)

    @Query("SELECT * FROM experiment ORDER BY id")
    fun experiments(): List<ExperimentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveExperiment(experiment: ExperimentEntity)

    @Query("SELECT * FROM saying_pick ORDER BY id")
    fun sayingPicks(): List<SayingPickEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePick(pick: SayingPickEntity)

    @Query("SELECT * FROM goal_tap ORDER BY weekStart")
    fun goalTaps(): List<GoalTapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveGoalTap(tap: GoalTapEntity)

    @Query("SELECT * FROM lake_state WHERE id = 0")
    fun lake(): LakeRowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLake(lake: LakeRowEntity)

    @Query("SELECT * FROM note_state WHERE id = 0")
    fun note(): NoteStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveNote(note: NoteStateEntity)

    @Query("SELECT * FROM ingest_state WHERE id = 0")
    fun ingest(): IngestStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveIngest(ingest: IngestStateEntity)
}
