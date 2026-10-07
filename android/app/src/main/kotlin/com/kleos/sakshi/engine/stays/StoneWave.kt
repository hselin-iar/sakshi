package com.kleos.sakshi.engine.stays

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.model.NotifKind
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RemovalKind
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.ports.ListenerCoverage
import com.kleos.sakshi.engine.tuning.Tuning

/** classify()'s extra outputs (stonePkg, notifClicked) alongside Origin, for the caller to copy into the Stay. */
data class StoneWaveResult(val origin: Origin, val stonePkg: Pkg?, val notifClicked: Boolean)

/**
 * F4: 30-second look-back from stay.start. No coverage -> UNKNOWN. A POSTED,
 * non-ongoing notification from stay.firstPkg in that look-back -> STONE
 * (clicked if a REMOVED/CLICK from the same pkg follows within
 * NO_RIPPLE_SEC); otherwise SELF_STARTED. Ongoing and own-package
 * notifications are expected to already be excluded upstream (collectors).
 */
object StoneWave {
    fun classify(stay: Stay, notifs: List<NotifEvent>, coverage: ListenerCoverage): StoneWaveResult {
        val lookbackStart = EpochMs(stay.start.value - Tuning.STONE_LOOKBACK_SEC * 1_000L)
        if (!coverage.coversInterval(lookbackStart, stay.start)) {
            return StoneWaveResult(Origin.UNKNOWN, null, false)
        }

        val ping = notifs.firstOrNull { n ->
            n.kind == NotifKind.POSTED && !n.ongoing && n.pkg == stay.firstPkg &&
                n.ts.value in lookbackStart.value..stay.start.value
        } ?: return StoneWaveResult(Origin.SELF_STARTED, null, false)

        val clickWindowMs = Tuning.NO_RIPPLE_SEC * 1_000L
        val clicked = notifs.any { n ->
            n.kind == NotifKind.REMOVED && n.removal == RemovalKind.CLICK && n.pkg == ping.pkg &&
                n.ts.value in ping.ts.value..(ping.ts.value + clickWindowMs)
        }
        return StoneWaveResult(Origin.STONE, stay.firstPkg, clicked)
    }

    /**
     * For every non-ongoing POSTED notification inside the window from a
     * non-neutral, covered package, whether any stay (any app) began within
     * NO_RIPPLE_SEC. Rate = notifications without a stay / notifications;
     * null when there is no covered qualifying notification.
     */
    fun noRippleRate(
        window: Window,
        notifs: List<NotifEvent>,
        coverage: ListenerCoverage,
        stays: List<Stay>,
        classify: (Pkg) -> AppClass,
    ): Double? {
        val windowMs = Tuning.NO_RIPPLE_SEC * 1_000L
        val qualifying = notifs.filter { n ->
            n.kind == NotifKind.POSTED && !n.ongoing &&
                n.ts.value >= window.start.value && n.ts.value < window.end.value &&
                classify(n.pkg) != AppClass.NEUTRAL &&
                coverage.coversInterval(n.ts, EpochMs(n.ts.value + windowMs))
        }
        if (qualifying.isEmpty()) return null

        val withoutStay = qualifying.count { n ->
            stays.none { stay -> stay.start.value in n.ts.value..(n.ts.value + windowMs) }
        }
        return withoutStay.toDouble() / qualifying.size
    }
}
