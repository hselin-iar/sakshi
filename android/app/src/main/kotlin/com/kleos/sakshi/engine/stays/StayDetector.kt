package com.kleos.sakshi.engine.stays

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.Origin
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.model.Stay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.tuning.Tuning

/** Top-level glance count for the window, alongside the stays detected within it. */
data class StayResult(val glances: Int, val stays: List<Stay>)

/**
 * F4's departure-run algorithm. `classes` must already be Depends-resolved
 * (DependsResolver output, aligned by index with `intervals`) — this never
 * sees the literal DEPENDS value. Stays are returned with origin=UNKNOWN,
 * stonePkg=null, notifClicked=false and returnMinutes=null: StoneWave (T2.6)
 * and Returns fill those in as a separate pass.
 */
object StayDetector {
    fun detect(
        window: Window,
        intervals: List<ForegroundInterval>,
        classes: List<AppClass>,
        screenOff: List<ScreenSpan>,
    ): StayResult {
        val windowStart = window.start.value
        val windowEnd = window.end.value

        data class TypedSpan(val start: Long, val end: Long, val pkg: Pkg?, val cls: AppClass?, val isScreenOff: Boolean)

        val appSpans = intervals.indices.mapNotNull { i ->
            val interval = intervals[i]
            val start = maxOf(interval.start.value, windowStart)
            val end = minOf(interval.end.value, windowEnd)
            if (end > start) TypedSpan(start, end, interval.pkg, classes[i], false) else null
        }
        val offSpans = screenOff.mapNotNull { span ->
            val start = maxOf(span.start.value, windowStart)
            val end = minOf(span.end?.value ?: windowEnd, windowEnd)
            if (end > start) TypedSpan(start, end, null, null, true) else null
        }
        val timeline = (appSpans + offSpans).sortedBy { it.start }

        data class RawRun(val start: Long, val end: Long, val firstPkg: Pkg, val pkgTime: Map<Pkg, Long>)

        val rawRuns = mutableListOf<RawRun>()
        var runStart: Long? = null
        var runEnd = 0L
        var runFirstPkg: Pkg? = null
        val runPkgTime = mutableMapOf<Pkg, Long>()

        fun closeRun() {
            val s = runStart ?: return
            rawRuns += RawRun(s, runEnd, runFirstPkg!!, runPkgTime.toMap())
            runStart = null
            runPkgTime.clear()
        }

        var cursorEnd = windowStart
        for (span in timeline) {
            // A gap in the recorded timeline itself ends any active run, the same as an
            // explicit IN_SET or screen-off interruption would — the <20s merge below is
            // what re-joins nearby runs, regardless of why they were split.
            if (span.start > cursorEnd) closeRun()

            when {
                span.isScreenOff -> closeRun()
                span.cls == AppClass.IN_SET -> closeRun()
                span.cls == AppClass.NEUTRAL -> if (runStart != null) runEnd = span.end
                span.cls == AppClass.OFF_SET -> {
                    if (runStart == null) {
                        runStart = span.start
                        runFirstPkg = span.pkg
                    }
                    runEnd = span.end
                    val pkg = span.pkg!!
                    runPkgTime[pkg] = (runPkgTime[pkg] ?: 0L) + (span.end - span.start)
                }
            }
            cursorEnd = maxOf(cursorEnd, span.end)
        }
        closeRun()

        // Merge: two runs with a gap < STAY_MERGE_GAP_SEC join into one BEFORE the glance/stay test.
        val mergeGapMs = Tuning.STAY_MERGE_GAP_SEC * 1_000L
        val mergedGroups = mutableListOf<MutableList<RawRun>>()
        for (run in rawRuns) {
            val lastGroup = mergedGroups.lastOrNull()
            if (lastGroup != null && run.start - lastGroup.last().end < mergeGapMs) {
                lastGroup += run
            } else {
                mergedGroups += mutableListOf(run)
            }
        }

        val glanceMs = Tuning.GLANCE_MAX_SEC * 1_000L
        var totalGlances = 0
        var glancesSinceLastStay = 0
        val stays = mutableListOf<Stay>()

        for (group in mergedGroups) {
            val start = group.first().start
            val end = group.last().end
            if (end - start < glanceMs) {
                totalGlances++
                glancesSinceLastStay++
                continue
            }

            val combinedPkgTime = mutableMapOf<Pkg, Long>()
            for (run in group) {
                for ((pkg, time) in run.pkgTime) {
                    combinedPkgTime[pkg] = (combinedPkgTime[pkg] ?: 0L) + time
                }
            }
            val pkgMain = combinedPkgTime.entries.maxByOrNull { it.value }!!.key

            stays += Stay(
                id = 0L,
                windowId = window.id,
                start = EpochMs(start),
                end = EpochMs(end),
                firstPkg = group.first().firstPkg,
                pkgMain = pkgMain,
                origin = Origin.UNKNOWN,
                stonePkg = null,
                notifClicked = false,
                returnMinutes = null,
                glancesBefore = glancesSinceLastStay,
            )
            glancesSinceLastStay = 0
        }

        return StayResult(totalGlances, stays)
    }
}
