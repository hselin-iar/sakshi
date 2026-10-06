package com.kleos.sakshi.host

import android.content.Context
import com.kleos.sakshi.data.Retention
import com.kleos.sakshi.data.RoomDerivedStore
import com.kleos.sakshi.data.RoomEventStore
import com.kleos.sakshi.data.RoomGapStore
import com.kleos.sakshi.data.RoomNotifStore
import com.kleos.sakshi.data.RoomStateStore
import com.kleos.sakshi.data.SakshiDatabase
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.SakshiEngine
import com.kleos.sakshi.engine.ports.Ports

/** The one wiring point (LC-9). Data wiring only for now; T1.5 onward adds the adapters, T1.9 the engine. */
class AppContainer(private val context: Context) {
    val database: SakshiDatabase by lazy { SakshiDatabase.create(context) }

    val events by lazy { RoomEventStore(database) }
    val notifs by lazy { RoomNotifStore(database) }
    val gaps by lazy { RoomGapStore(database) }
    val derived by lazy { RoomDerivedStore(database) }
    val state by lazy { RoomStateStore(database) }
    val retention by lazy { Retention(events, notifs) }

    val catalog by lazy { AppCatalogImpl.create(context) }
    val shelf by lazy { SayingShelfImpl.create(context) }

    val clock by lazy { SystemClock() }

    /** Every port the engine needs, wired to the real adapters. */
    val ports by lazy {
        Ports(
            events = events, notifs = notifs, coverage = notifs, gaps = gaps, derived = derived, state = state,
            catalog = catalog, shelf = shelf, clock = clock, random = SeededRandomness())
    }

    /** T1.2's canned façade until Track 2 lands (Sync 4); nothing in the host depends on which it is. */
    val engine by lazy { SakshiEngine(ports) }

    /** Set by DemoController (T1.13). Pause, Export and Delete are refused while true. */
    @Volatile var demoActive: Boolean = false

    private val ingestLock = Any()

    private fun ingestDeps() = IngestDeps(
        source = UsageEventsSource(context),
        hasUsageAccess = PermissionGateway(context)::usageAccessGranted,
        events = events, gaps = gaps, state = state, retention = retention,
        process = engine::processNewEvents,
        afterProcess = { report -> if (report.lakeChanged) LakeWidget.refresh(context) })

    /** The periodic job and syncNow() share one lock, so two runs never move the cursor at the same time. */
    fun ingest(asOf: EpochMs): IngestReport = synchronized(ingestLock) { runIngest(ingestDeps(), asOf) }

    companion object {
        @Volatile private var instance: AppContainer? = null

        /** For tests: drop the shared container (closing its database) so the next caller builds a fresh one. */
        fun reset() = synchronized(this) { instance?.let { runCatching { it.database.close() } }; instance = null }

        /** One container per process; the listener service and the Pigeon host share it. */
        fun from(context: Context): AppContainer =
            instance ?: synchronized(this) { instance ?: AppContainer(context.applicationContext).also { instance = it } }
    }
}
