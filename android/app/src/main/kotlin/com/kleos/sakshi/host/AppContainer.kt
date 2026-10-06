package com.kleos.sakshi.host

import android.content.Context
import com.kleos.sakshi.data.Retention
import com.kleos.sakshi.data.RoomDerivedStore
import com.kleos.sakshi.data.RoomEventStore
import com.kleos.sakshi.data.RoomGapStore
import com.kleos.sakshi.data.RoomNotifStore
import com.kleos.sakshi.data.RoomStateStore
import com.kleos.sakshi.data.SakshiDatabase

/** The one wiring point (LC-9). Data wiring only for now; T1.5 onward adds the adapters, T1.9 the engine. */
class AppContainer(context: Context) {
    val database: SakshiDatabase by lazy { SakshiDatabase.create(context) }

    val events by lazy { RoomEventStore(database) }
    val notifs by lazy { RoomNotifStore(database) }
    val gaps by lazy { RoomGapStore(database) }
    val derived by lazy { RoomDerivedStore(database) }
    val state by lazy { RoomStateStore(database) }
    val retention by lazy { Retention(events, notifs) }

    companion object {
        @Volatile private var instance: AppContainer? = null

        /** One container per process; the listener service and the Pigeon host share it. */
        fun from(context: Context): AppContainer =
            instance ?: synchronized(this) { instance ?: AppContainer(context.applicationContext).also { instance = it } }
    }
}
