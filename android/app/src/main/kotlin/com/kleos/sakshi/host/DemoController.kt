package com.kleos.sakshi.host

import android.content.Context
import com.kleos.sakshi.data.SakshiDatabase
import com.kleos.sakshi.engine.demo.DemoReplay
import com.kleos.sakshi.engine.demo.EventSynthesizer
import com.kleos.sakshi.engine.demo.PersonaSpec
import com.kleos.sakshi.engine.demo.SyntheticHistory
import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.ports.AppCatalog
import com.kleos.sakshi.engine.ports.Clock
import com.kleos.sakshi.engine.tuning.Tuning
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** A clock the demo moves by hand. The demo path reads no other time. */
class DemoClock(@Volatile var nowMs: Long) : Clock {
    override fun now() = EpochMs(nowMs)
}

/**
 * The real phone's catalog plus the persona's own packages, so the engine sees the demo apps as installed. The engine has no demo branch;
 * only what it is given differs.
 */
class DemoCatalog(private val real: AppCatalog, private val extra: List<AppInfo>) : AppCatalog {
    override fun launcherApps(): List<AppInfo> = real.launcherApps() + extra
    override fun category(pkg: Pkg): Int? = real.category(pkg)
    override fun isNeutral(pkg: Pkg): Boolean = real.isNeutral(pkg)
    override fun ownPackage(): Pkg = real.ownPackage()
}

/**
 * The Time Machine demo (DOC 3). `start` builds a second, in-memory set of stores with its own injected clock and registers it in
 * the container; the real database is never opened through it, so its file stays byte-identical. `setAsOf` moves the demo clock and
 * rebuilds what the engine would have made by then; `stop` drops the demo set. A process death also drops it: the app always restarts
 * in real mode.
 */
class DemoController(
    private val context: Context,
    private val container: AppContainer,
    private val zone: ZoneId = ZoneId.systemDefault()) {

    private var clock: DemoClock? = null
    private var anchor: LocalDate? = null
    private var spec: PersonaSpec? = null
    private var history: SyntheticHistory? = null

    /**
     * Study day `index` (0..59) begins at 04:00 local. Day 0 is a Monday, so a persona's weeks are calendar weeks, and day 59 is on or
     * before today's study day: the demo never reaches into the future.
     */
    internal fun dayStartMs(index: Int): Long =
        anchor!!.plusDays(index.toLong()).atTime(Tuning.STUDY_DAY_START_HOUR, 0).atZone(zone).toInstant().toEpochMilli()

    @Synchronized fun start(personaId: String) {
        val persona = requireNotNull(com.kleos.sakshi.engine.demo.PERSONAS[personaId]) { "unknown persona" }
        if (container.demoActive) stop()
        val database = SakshiDatabase.inMemory(context)
        try {
            val realNow = container.real.clock.now().value
            val today = java.time.Instant.ofEpochMilli(realNow).atZone(zone).minusHours(Tuning.STUDY_DAY_START_HOUR.toLong()).toLocalDate()
            anchor = today.minusDays(LAST_DAY.toLong()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val demoClock = DemoClock(realNow).also { clock = it }
            val seed = personaId.hashCode().toLong()
            val made = EventSynthesizer.synthesize(persona, seed, ::dayStartMs, container.catalog.ownPackage())
            spec = persona; history = made
            val stores = Stores(
                database, DemoCatalog(container.catalog, made.launcherApps), container.shelf, demoClock, SeededRandomness(seed), isDemo = true)
            container.beginDemo(stores)
            setAsOf(FIRST_READ_DAY)
        } catch (e: Exception) {
            // Synthesis failed: leave the app in real mode with the real database untouched.
            container.endDemo()
            runCatching { database.close() }
            clock = null; spec = null; history = null
            throw e
        }
    }

    /**
     * Before the first read day it clamps up; after the last it clamps down. Everything the engine made is thrown away and rebuilt by
     * living through the history up to that day, so moving backwards is as correct as moving forwards: no suggestion shown, verdict read
     * or experiment started "in the future" survives.
     */
    @Synchronized fun setAsOf(dayIndex: Int) {
        val demo = container.stores.takeIf { container.demoActive } ?: throw IllegalStateException("no demo is running")
        val day = dayIndex.coerceIn(FIRST_READ_DAY, LAST_DAY)
        val demoClock = checkNotNull(clock)
        demoClock.nowMs = dayStartMs(day + 1) - ONE_MINUTE_MS
        demo.derived.clearAll()
        seed(demo, checkNotNull(history), checkNotNull(spec), untilMs = demoClock.nowMs)
        DemoReplay.run(demo.engine, ::dayStartMs, day)
        LakeWidget.refresh(context)
    }

    @Synchronized fun stop() {
        val demo = container.stores.takeIf { container.demoActive }
        container.endDemo()
        runCatching { demo?.database?.close() }
        clock = null; spec = null; history = null
        LakeWidget.refresh(context)          // redraw the real Lake
    }

    /** Only what had happened by `untilMs` is written, so nothing after the slider can matter. */
    private fun seed(stores: Stores, made: SyntheticHistory, persona: PersonaSpec, untilMs: Long) {
        stores.events.append(made.events.filter { it.ts.value <= untilMs })
        made.notifs.filter { it.ts.value <= untilMs }.forEach { stores.notifs.append(it) }
        made.sessions.forEach { s ->
            if (s.connectedAt.value <= untilMs) {
                stores.notifs.openSession(s.connectedAt)
                s.disconnectedAt?.takeIf { it.value <= untilMs }?.let { stores.notifs.closeSession(it) }
            }
        }
        val firstRead = EpochMs(dayStartMs(made.firstReadDay))
        stores.state.replaceApps(made.apps)
        stores.state.saveSettings(
            Settings(
                studyBlocks = made.studyBlocks, learnStudyHours = false, gentleMode = persona.gentleDefault, gentleExplicit = persona.gentleDefault,
                weeklyNoteEnabled = false, ageUnder18 = false, batteryHelperShown = true, lapseAcknowledgedThrough = null,
                firstReadAt = firstRead, createdAt = EpochMs(dayStartMs(0))))
        stores.state.saveIngest(stores.state.ingest().copy(firstReadAt = firstRead))
    }

    companion object {
        val PERSONAS: Set<String> = com.kleos.sakshi.engine.demo.PERSONAS.keys
        const val FIRST_READ_DAY = 3        // Day 1 preset
        const val LAST_DAY = 59             // Week 8 preset
        private const val ONE_MINUTE_MS = 60_000L
    }
}
