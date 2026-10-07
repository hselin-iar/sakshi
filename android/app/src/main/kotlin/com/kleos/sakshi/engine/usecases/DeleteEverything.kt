package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.ports.Ports

/** F10: wipes every table in one transaction. The next run behaves like a fresh install. Files, the widget and permissions are the host's. */
object DeleteEverything {
    fun run(ports: Ports) = ports.derived.clearAll()
}
