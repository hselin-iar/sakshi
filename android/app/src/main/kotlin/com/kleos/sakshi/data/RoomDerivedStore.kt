package com.kleos.sakshi.data

import com.kleos.sakshi.data.entities.toEntity
import com.kleos.sakshi.engine.model.DayDerivation
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.Pattern
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.WeekSummary
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.ports.DerivedStore

/**
 * `windows` returns windows that START in [from, to). `days` is inclusive at both ends because study days are whole units.
 * The engine gives each window a unique non-zero id; stretches and stays point at it through windowId.
 */
class RoomDerivedStore(private val db: SakshiDatabase) : DerivedStore {
    private val dao get() = db.derived()

    override fun replaceDay(day: StudyDay, d: DayDerivation) = db.runInTransaction {
        dao.deleteStretchesOfDay(day.epochDay)
        dao.deleteStaysOfDay(day.epochDay)
        dao.deleteWindowsOfDay(day.epochDay)
        dao.deleteSummaryOfDay(day.epochDay)
        dao.insertWindows(d.windows.map { it.window.toEntity(it.quietMinutes, it.glances) })
        dao.insertStretches(d.windows.flatMap { w -> w.stretches.map { it.toEntity() } })
        dao.insertStays(d.windows.flatMap { w -> w.stays.map { it.toEntity() } })
        dao.insertSummary(d.summary.toEntity())
    }

    override fun windows(from: EpochMs, to: EpochMs): List<WindowWithDetail> {
        val rows = dao.windowsStartingIn(from.value, to.value)
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it.id }
        val stretches = dao.stretchesOf(ids).groupBy { it.windowId }
        val stays = dao.staysOf(ids).groupBy { it.windowId }
        return rows.map { w ->
            WindowWithDetail(
                window = w.toModel(),
                stretches = stretches[w.id].orEmpty().map { it.toModel() },
                stays = stays[w.id].orEmpty().map { it.toModel() },
                quietMinutes = w.quietMinutes,
                glances = w.glances)
        }
    }

    override fun days(from: StudyDay, to: StudyDay): List<DaySummary> = dao.days(from.epochDay, to.epochDay).map { it.toModel() }

    override fun upsertWeek(w: WeekSummary) = dao.upsertWeek(w.toEntity())
    override fun weeks(): List<WeekSummary> = dao.weeks().map { it.toModel() }
    override fun upsertPatterns(p: List<Pattern>) = dao.upsertPatterns(p.map { it.toEntity() })
    override fun patterns(): List<Pattern> = dao.patterns().map { it.toModel() }

    /** Derived rows only. The frozen baseline and the settings are state, not derived, so they stay. */
    override fun clearDerived() = db.runInTransaction {
        dao.clearStretches(); dao.clearStays(); dao.clearWindows()
        dao.clearDaySummaries(); dao.clearWeekSummaries(); dao.clearPatterns()
    }

    /** Delete everything (F10): every table, in one transaction. */
    override fun clearAll() = db.runInTransaction { db.clearAllTables() }
}
