package com.kleos.sakshi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kleos.sakshi.data.dao.DataGapDao
import com.kleos.sakshi.data.dao.DerivedDao
import com.kleos.sakshi.data.dao.ListenerSessionDao
import com.kleos.sakshi.data.dao.NotifEventDao
import com.kleos.sakshi.data.dao.RawEventDao
import com.kleos.sakshi.data.dao.StateDao
import com.kleos.sakshi.data.entities.AppMetaEntity
import com.kleos.sakshi.data.entities.BaselineEntity
import com.kleos.sakshi.data.entities.DataGapEntity
import com.kleos.sakshi.data.entities.DaySummaryEntity
import com.kleos.sakshi.data.entities.ExperimentEntity
import com.kleos.sakshi.data.entities.GoalTapEntity
import com.kleos.sakshi.data.entities.IngestStateEntity
import com.kleos.sakshi.data.entities.LakeRowEntity
import com.kleos.sakshi.data.entities.ListenerSessionEntity
import com.kleos.sakshi.data.entities.NoteStateEntity
import com.kleos.sakshi.data.entities.NotifEventEntity
import com.kleos.sakshi.data.entities.PatternEntity
import com.kleos.sakshi.data.entities.RawEventEntity
import com.kleos.sakshi.data.entities.SayingPickEntity
import com.kleos.sakshi.data.entities.SettingsEntity
import com.kleos.sakshi.data.entities.StayEntity
import com.kleos.sakshi.data.entities.StretchEntity
import com.kleos.sakshi.data.entities.SuggestionStateEntity
import com.kleos.sakshi.data.entities.WeekSummaryEntity
import com.kleos.sakshi.data.entities.WindowEntity

/** One table per LC-2 entity. The schema freezes at the first device install; until then it may be rebuilt. */
@Database(
    entities = [
        RawEventEntity::class, NotifEventEntity::class, ListenerSessionEntity::class, DataGapEntity::class,
        IngestStateEntity::class, AppMetaEntity::class, SettingsEntity::class,
        DaySummaryEntity::class, WindowEntity::class, StretchEntity::class, StayEntity::class, WeekSummaryEntity::class,
        BaselineEntity::class, PatternEntity::class, SuggestionStateEntity::class, ExperimentEntity::class,
        SayingPickEntity::class, GoalTapEntity::class, LakeRowEntity::class, NoteStateEntity::class],
    version = 1,
    exportSchema = true)
abstract class SakshiDatabase : RoomDatabase() {
    abstract fun rawEvents(): RawEventDao
    abstract fun notifEvents(): NotifEventDao
    abstract fun listenerSessions(): ListenerSessionDao
    abstract fun gaps(): DataGapDao
    abstract fun derived(): DerivedDao
    abstract fun state(): StateDao

    companion object {
        const val FILE_NAME = "sakshi.db"

        fun create(context: Context): SakshiDatabase =
            Room.databaseBuilder(context.applicationContext, SakshiDatabase::class.java, FILE_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true) // allowed in development (LC-2)
                .build()

        /** For tests and the demo: nothing touches disk. Main-thread queries are allowed because the ports are blocking. */
        fun inMemory(context: Context): SakshiDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, SakshiDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
