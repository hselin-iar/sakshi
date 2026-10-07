package com.kleos.sakshi.host

import android.content.Context
import com.kleos.sakshi.data.Exporter
import com.kleos.sakshi.data.Retention
import com.kleos.sakshi.data.RoomDerivedStore
import com.kleos.sakshi.data.RoomEventStore
import com.kleos.sakshi.data.RoomGapStore
import com.kleos.sakshi.data.RoomNotifStore
import com.kleos.sakshi.data.RoomStateStore
import com.kleos.sakshi.data.SakshiDatabase
import com.kleos.sakshi.engine.SakshiEngine
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.HostFacts
import com.kleos.sakshi.engine.ports.AppCatalog
import com.kleos.sakshi.engine.ports.Clock
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.ports.Randomness
import com.kleos.sakshi.engine.ports.SayingShelf

/**
 * One database, the stores over it, and an engine on top. A phone has one real set. A demo has a second, in memory, with its own
 * injected clock; the real database is never opened through it (DOC 3, Time Machine Demo).
 */
class Stores(val database: SakshiDatabase, catalog: AppCatalog, shelf: SayingShelf, val clock: Clock, random: Randomness) {
    val events = RoomEventStore(database)
    val notifs = RoomNotifStore(database)
    val gaps = RoomGapStore(database)
    val derived = RoomDerivedStore(database)
    val state = RoomStateStore(database)
    val retention = Retention(events, notifs)

    /** T1.2's canned façade until Track 2 lands (Sync 4); nothing in the host depends on which it is. */
    val engine = SakshiEngine(Ports(events, notifs, notifs, gaps, derived, state, catalog, shelf, clock, random))
}

/** The one wiring point (LC-9). */
class AppContainer(private val context: Context) {
    val catalog by lazy { AppCatalogImpl.create(context) }
    val shelf by lazy { SayingShelfImpl.create(context) }
    val permissions by lazy { PermissionGateway(context) }

    /** The phone's real data. The listener, the ingest, Pause, Export and Delete work on this and nothing else. */
    val real: Stores by lazy { Stores(SakshiDatabase.create(context), catalog, shelf, SystemClock(), SeededRandomness()) }
    val database: SakshiDatabase get() = real.database

    @Volatile private var demo: Stores? = null
    val demoActive: Boolean get() = demo != null

    /** What the app screens read: the demo set while a demo runs, the real set otherwise. */
    val stores: Stores get() = demo ?: real
    val events get() = stores.events
    val notifs get() = stores.notifs
    val derived get() = stores.derived
    val state get() = stores.state
    val engine get() = stores.engine
    val clock: Clock get() = stores.clock

    val noteNotifier by lazy { WeeklyNoteNotifier(context, real.state, permissions) }
    val pauseControl by lazy { PauseControl(real.state, real.gaps) }
    val exporter by lazy { Exporter(context.cacheDir, real.events, real.notifs, real.derived, real.state) }
    val demoController by lazy { DemoController(context, this) }

    internal fun beginDemo(stores: Stores) { demo = stores }
    internal fun endDemo() { demo = null }

    fun hostFacts() = HostFacts(
        usageAccessGranted = permissions.usageAccessGranted(), notificationAccessGranted = permissions.notificationListenerEnabled(),
        canPostNotifications = permissions.canPostNotifications(), lastError = state.ingest().lastError)

    private val ingestLock = Any()

    private fun ingestDeps() = IngestDeps(
        source = UsageEventsSource(context),
        hasUsageAccess = permissions::usageAccessGranted,
        events = real.events, gaps = real.gaps, state = real.state, retention = real.retention,
        process = real.engine::processNewEvents,
        afterProcess = { report ->
            if (report.lakeChanged) LakeWidget.refresh(context)
            // The engine decides whether a note is due; the notifier only posts it.
            noteNotifier.maybePost(real.engine.noteDecision(real.clock.now(), hostFacts()))
        })

    /**
     * The periodic job and syncNow() share one lock, so two runs never move the cursor at the same time.
     * While a demo runs nothing is read from the phone, so synthetic and real data can never mix.
     */
    fun ingest(asOf: EpochMs): IngestReport =
        if (demoActive) IngestReport.DemoActive else synchronized(ingestLock) { runIngest(ingestDeps(), asOf) }

    /** Delete everything (F10): every table in one transaction, cached exports removed, Lake reset, widget redrawn. The schedule stays. */
    fun deleteEverything() = synchronized(ingestLock) {
        real.derived.clearAll()
        exporter.clearExports()
        LakeWidget.refresh(context)
    }

    companion object {
        @Volatile private var instance: AppContainer? = null

        /** For tests: drop the shared container (closing its databases) so the next caller builds a fresh one. */
        fun reset() = synchronized(this) {
            instance?.let { c -> runCatching { c.demo?.database?.close() }; runCatching { c.database.close() } }
            instance = null
        }

        /** One container per process; the listener service and the Pigeon host share it. */
        fun from(context: Context): AppContainer =
            instance ?: synchronized(this) { instance ?: AppContainer(context.applicationContext).also { instance = it } }
    }
}
