package com.kleos.sakshi.engine.intervals

import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.RawEvent
import com.kleos.sakshi.engine.model.RawType
import com.kleos.sakshi.engine.model.Reconstruction
import com.kleos.sakshi.engine.model.ScreenSpan

// Tie order for events sharing a timestamp (DOC 3): PAUSED, NON_INTERACTIVE,
// KEYGUARD_SHOWN, then RESUMED, INTERACTIVE, KEYGUARD_HIDDEN.
private val TIE_ORDER = mapOf(
    RawType.ACTIVITY_PAUSED to 0,
    RawType.SCREEN_NON_INTERACTIVE to 1,
    RawType.KEYGUARD_SHOWN to 2,
    RawType.ACTIVITY_RESUMED to 3,
    RawType.SCREEN_INTERACTIVE to 4,
    RawType.KEYGUARD_HIDDEN to 5,
)

private const val MIN_INTERVAL_MS = 1_000L           // R7
private const val JOIN_GAP_MS = 2_000L               // R9
private const val OPEN_END_LAG_MS = 10 * 60 * 1_000L // R8

fun reconstruct(events: List<RawEvent>, asOf: EpochMs): Reconstruction {
    val sorted = events.sortedWith(compareBy({ it.ts.value }, { TIE_ORDER[it.type] ?: Int.MAX_VALUE }))

    val rawIntervals = mutableListOf<ForegroundInterval>()
    val screenOff = mutableListOf<ScreenSpan>()
    var openApp: Pair<Pkg, EpochMs>? = null
    var openScreenOffStart: EpochMs? = null
    var anomalies = 0

    fun closeApp(at: EpochMs) {
        val (pkg, start) = openApp ?: return
        rawIntervals += ForegroundInterval(pkg, start, at)
        openApp = null
    }

    fun closeScreenOff(at: EpochMs) {
        val start = openScreenOffStart ?: return
        screenOff += ScreenSpan(start, at)
        openScreenOffStart = null
    }

    for (event in sorted) {
        when (event.type) {
            RawType.ACTIVITY_RESUMED -> {
                val pkg = event.pkg ?: continue
                val doubleResumed = openApp?.first == pkg // R1: "another interval is open" closes it regardless
                closeApp(event.ts)
                if (doubleResumed) anomalies++
                openApp = pkg to event.ts
            }
            RawType.ACTIVITY_PAUSED -> {
                val pkg = event.pkg ?: continue
                if (openApp?.first == pkg) {
                    closeApp(event.ts)
                } else {
                    anomalies++ // PAUSED without a matching open RESUMED (R2: ignored)
                }
            }
            RawType.SCREEN_NON_INTERACTIVE -> {
                closeApp(event.ts)
                if (openScreenOffStart == null) openScreenOffStart = event.ts
            }
            RawType.SCREEN_INTERACTIVE -> {
                closeScreenOff(event.ts)
            }
            RawType.KEYGUARD_SHOWN -> {
                closeApp(event.ts) // no-op if already closed by a same-ts, lower-tie-order event
            }
            RawType.KEYGUARD_HIDDEN -> {
                // R6: no effect by itself
            }
        }
    }

    var openEnded = false
    if (sorted.isNotEmpty() && (openApp != null || openScreenOffStart != null)) {
        val lastEventTs = sorted.last().ts
        val closeAt = EpochMs(minOf(asOf.value, lastEventTs.value + OPEN_END_LAG_MS))
        closeApp(closeAt)
        closeScreenOff(closeAt)
        openEnded = true
    }

    val joined = joinAdjacentSamePkg(rawIntervals.sortedBy { it.start.value })
    val filtered = joined.filter { it.end.value - it.start.value >= MIN_INTERVAL_MS }

    return Reconstruction(
        intervals = filtered,
        screenOff = screenOff.sortedBy { it.start.value },
        openEnded = openEnded,
        anomalies = anomalies,
    )
}

// R9: two consecutive intervals of the same package with a gap under 2 seconds are joined.
private fun joinAdjacentSamePkg(intervals: List<ForegroundInterval>): List<ForegroundInterval> {
    if (intervals.isEmpty()) return intervals
    val result = mutableListOf(intervals.first())
    for (next in intervals.drop(1)) {
        val last = result.last()
        if (last.pkg == next.pkg && next.start.value - last.end.value < JOIN_GAP_MS) {
            result[result.lastIndex] = last.copy(end = next.end)
        } else {
            result += next
        }
    }
    return result
}
