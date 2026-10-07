package com.kleos.sakshi.host

import android.content.Context
import com.kleos.sakshi.data.SakshiDatabase
import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.ports.Clock
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.LocalDate
import java.time.ZoneId

/** A clock the demo moves by hand. The demo path reads no other time. */
class DemoClock(@Volatile var nowMs: Long) : Clock {
    override fun now() = EpochMs(nowMs)
}

/**
 * The Time Machine demo (DOC 3). `start` builds a second, in-memory set of stores with its own injected clock and registers it in
 * the container; the real database is never opened through it, so its file stays byte-identical. `setAsOf` moves the demo clock and
 * recomputes; `stop` drops the demo set. A process death also drops it: the app always restarts in real mode.
 */
class DemoController(
    private val context: Context,
    private val container: AppContainer,
    private val zone: ZoneId = ZoneId.systemDefault()) {

    private var clock: DemoClock? = null
    private var anchor: LocalDate? = null

    /** Study day `index` (0..59) begins at 04:00 local; day 59 is today's study day, so the demo ends at the present. */
    internal fun dayStartMs(index: Int): Long =
        anchor!!.minusDays((LAST_DAY - index).toLong()).atTime(Tuning.STUDY_DAY_START_HOUR, 0).atZone(zone).toInstant().toEpochMilli()

    @Synchronized fun start(personaId: String) {
        require(personaId in PERSONAS) { "unknown persona" }
        if (container.demoActive) stop()
        val database = SakshiDatabase.inMemory(context)
        try {
            val realNow = container.real.clock.now().value
            anchor = java.time.Instant.ofEpochMilli(realNow).atZone(zone).minusHours(Tuning.STUDY_DAY_START_HOUR.toLong()).toLocalDate()
            val demoClock = DemoClock(realNow).also { clock = it }
            val stores = Stores(database, container.catalog, container.shelf, demoClock, SeededRandomness(personaId.hashCode().toLong()))
            seed(stores, DemoHistory.stub(context.packageName, ::dayStartMs, FIRST_READ_DAY))
            container.beginDemo(stores)
            setAsOf(FIRST_READ_DAY)
        } catch (e: Exception) {
            // Synthesis failed: leave the app in real mode with the real database untouched.
            container.endDemo()
            runCatching { database.close() }
            clock = null
            throw e
        }
    }

    /** Before the first read day it clamps up; after the last it clamps down. Derived rows are rebuilt from the events up to that moment. */
    @Synchronized fun setAsOf(dayIndex: Int) {
        val demo = container.stores.takeIf { container.demoActive } ?: throw IllegalStateException("no demo is running")
        val day = dayIndex.coerceIn(FIRST_READ_DAY, LAST_DAY)
        val demoClock = checkNotNull(clock)
        demoClock.nowMs = dayStartMs(day + 1) - ONE_MINUTE_MS
        demo.derived.clearDerived()
        demo.engine.processNewEvents(demoClock.now())
        LakeWidget.refresh(context)
    }

    @Synchronized fun stop() {
        val demo = container.stores.takeIf { container.demoActive }
        container.endDemo()
        runCatching { demo?.database?.close() }
        clock = null
        LakeWidget.refresh(context)          // redraw the real Lake
    }

    private fun seed(stores: Stores, seed: DemoHistory.Seed) {
        stores.events.append(seed.events)
        stores.state.saveSettings(seed.settings)
        stores.state.replaceApps(seed.apps)
        stores.notifs.openSession(EpochMs(dayStartMs(0)))
        stores.state.saveIngest(stores.state.ingest().copy(firstReadAt = seed.settings.firstReadAt))
    }

    companion object {
        val PERSONAS = setOf("aarav", "meera", "rohan")
        const val FIRST_READ_DAY = 3        // Day 1 preset
        const val LAST_DAY = 59             // Week 8 preset
        private const val ONE_MINUTE_MS = 60_000L
    }
}

/**
 * A handful of hard-coded events so the swap mechanics can be exercised. Not the persona history.
 * [REFACTOR CANDIDATE: replaced by engine/demo/EventSynthesizer (T2.16) once Track 2 delivers it.]
 */
object DemoHistory {
    class Seed(val events: List<RawEvent>, val apps: List<AppMeta>, val settings: Settings)

    fun stub(ownPackage: String, dayStart: (Int) -> Long, firstReadDay: Int): Seed {
        val hour = 3_600_000L
        val minute = 60_000L
        val events = (0..firstReadDay).flatMap { d ->
            val s = dayStart(d)
            fun at(offset: Long, type: RawType, pkg: String?) = RawEvent(EpochMs(s + offset), type, pkg?.let(::Pkg))
            listOf(
                at(15 * hour + 50 * minute, RawType.ACTIVITY_RESUMED, ownPackage), at(15 * hour + 51 * minute, RawType.ACTIVITY_PAUSED, ownPackage),
                at(16 * hour, RawType.ACTIVITY_RESUMED, "demo.notes"), at(16 * hour + 20 * minute, RawType.ACTIVITY_PAUSED, "demo.notes"),
                at(16 * hour + 20 * minute, RawType.ACTIVITY_RESUMED, "demo.video"), at(16 * hour + 25 * minute, RawType.ACTIVITY_PAUSED, "demo.video"),
                at(16 * hour + 25 * minute, RawType.ACTIVITY_RESUMED, "demo.notes"), at(16 * hour + 50 * minute, RawType.ACTIVITY_PAUSED, "demo.notes"),
                at(16 * hour + 51 * minute, RawType.SCREEN_NON_INTERACTIVE, null))
        }
        val firstRead = EpochMs(dayStart(firstReadDay))
        return Seed(
            events = events,
            apps = listOf(AppMeta(Pkg("demo.notes"), "Notes", null, UserClass.IN_SET, EpochMs(dayStart(0)))),
            settings = Settings(
                studyBlocks = listOf(StudyBlock(1200, 1380)), learnStudyHours = false, gentleMode = false, gentleExplicit = false,
                weeklyNoteEnabled = false, ageUnder18 = false, batteryHelperShown = true, lapseAcknowledgedThrough = null,
                firstReadAt = firstRead, createdAt = EpochMs(dayStart(0))))
    }
}
