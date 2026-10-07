package com.kleos.sakshi.engine.usecases

import com.kleos.sakshi.engine.classify.AppClassifier
import com.kleos.sakshi.engine.classify.DependsResolver
import com.kleos.sakshi.engine.intervals.reconstruct
import com.kleos.sakshi.engine.metrics.StretchBuilder
import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.DaySummary
import com.kleos.sakshi.engine.model.DayDerivation
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.ForegroundInterval
import com.kleos.sakshi.engine.model.NotifEvent
import com.kleos.sakshi.engine.model.RawType
import com.kleos.sakshi.engine.model.ScreenSpan
import com.kleos.sakshi.engine.model.StudyDay
import com.kleos.sakshi.engine.model.Window
import com.kleos.sakshi.engine.model.WindowWithDetail
import com.kleos.sakshi.engine.ports.Ports
import com.kleos.sakshi.engine.stays.Returns
import com.kleos.sakshi.engine.stays.StayDetector
import com.kleos.sakshi.engine.stays.StoneWave
import com.kleos.sakshi.engine.windows.WindowFinder
import java.time.ZoneId

private const val LEAD_MS = 60L * 60 * 1_000 // 60 minutes, so an interval opened across the boundary is seen
private const val NOTIF_LOOKBACK_MS = 30L * 1_000 // StoneWave's own look-back is 30s; a little extra margin for ties

/**
 * F1: reconstruct -> classify -> windows -> stays -> stone/wave -> metrics, for one study day.
 *
 * Day attribution: an interval/screen-off span counts toward this day only if its own START
 * falls within [day.start, day.end) — "a window is attributed to the study day it started in"
 * (DOC 3, F3). The 60-minute lead-in/lead-out is only so reconstruct() can see the RESUMED event
 * that opened an interval spanning the boundary; it does not widen which intervals this day claims.
 */
object RecomputeDay {
    fun recompute(day: StudyDay, asOf: EpochMs, ports: Ports, zone: ZoneId): DayDerivation {
        val dayStart = day.startEpochMs(zone)
        val dayEnd = day.endEpochMs(zone)

        // Never read events after asOf (F1): the store's range is a plain time window, so filter
        // explicitly rather than relying on its upper bound being exclusive.
        val rawEvents = ports.events.range(EpochMs(dayStart.value - LEAD_MS), EpochMs(dayEnd.value + LEAD_MS))
            .filter { it.ts.value <= asOf.value }
        val reconstruction = reconstruct(rawEvents, asOf)

        val dayIntervals = reconstruction.intervals.filter { it.start.value in dayStart.value until dayEnd.value }
        val dayScreenOff = reconstruction.screenOff.filter { it.start.value in dayStart.value until dayEnd.value }

        val userClasses = ports.state.apps().associate { it.pkg to it.userClass }
        val classifier = AppClassifier(userClasses, ports.catalog)
        val baseClasses = dayIntervals.map { classifier.classify(it.pkg) }
        val effectiveClasses = DependsResolver.resolve(dayIntervals, baseClasses)
        val dayIntervalsWithClass = dayIntervals.zip(effectiveClasses)

        val studyBlocks = ports.state.settings().studyBlocks
        val gaps = ports.gaps.overlapping(dayStart, dayEnd)
        // WindowFinder leaves id = 0. The stores attach stretches and stays to a window by id, so every window needs its own.
        // Its start time is unique (windows never overlap) and stable across recomputes of the same day.
        val windows = WindowFinder.find(day, dayIntervals, effectiveClasses, studyBlocks, gaps, asOf, zone).map { it.copy(id = it.start.value) }

        val notifs = ports.notifs.range(EpochMs(dayStart.value - NOTIF_LOOKBACK_MS), dayEnd)
            .filter { it.ts.value <= asOf.value }

        val windowDetails = windows.map { window -> buildWindowDetail(window, dayIntervalsWithClass, dayScreenOff, dayEnd, notifs, ports) }

        val coverage = ports.coverage.coverageFraction(dayStart, dayEnd)
        val pickups = rawEvents.count { it.type == RawType.KEYGUARD_HIDDEN && it.ts.value in dayStart.value until dayEnd.value }
        val valid = WindowFinder.isValidDay(windowDetails.map { it.window })
        val own = ports.catalog.ownPackage()
        val externalResumes = rawEvents.count {
            it.type == RawType.ACTIVITY_RESUMED && it.pkg != null && it.pkg != own && it.ts.value in dayStart.value until dayEnd.value
        }

        val summary = DaySummary(
            day = day,
            valid = valid,
            windowMinutes = windowDetails.sumOf { (it.window.end.value - it.window.start.value) / 60_000.0 },
            quietMinutes = windowDetails.sumOf { it.quietMinutes },
            inSetMinutes = windowDetails.sumOf { wd -> wd.stretches.sumOf { it.inSetMinutes } },
            coverage = coverage,
            pickups = pickups,
            // Not computed -- no DOC 3 formula or golden example for either (see T2.7's scope note).
            switchesPerHour = null,
            flinch = null,
            rampUpMin = null,
            // T2.8 left these null for lack of a spec; T2.11's CrossDay (F-cross-day) finally defines
            // both: lastScreenOffTs is the last screen-off within the day's own span (which already
            // runs up to the next study day's 04:00, so no separate "before next 04:00" check is
            // needed), firstStretchMin is the day's chronologically first stretch.
            lastScreenOffTs = dayScreenOff.maxByOrNull { it.start.value }?.start,
            firstStretchMin = windowDetails.flatMap { it.stretches }.minByOrNull { it.start.value }?.minutes,
            externalResumes = externalResumes,
        )

        return DayDerivation(windowDetails, summary)
    }

    private fun buildWindowDetail(
        window: Window,
        dayIntervalsWithClass: List<Pair<ForegroundInterval, AppClass>>,
        dayScreenOff: List<ScreenSpan>,
        dayEnd: EpochMs,
        notifs: List<NotifEvent>,
        ports: Ports,
    ): WindowWithDetail {
        val windowPairs = dayIntervalsWithClass.filter { (interval, _) ->
            interval.start.value < window.end.value && interval.end.value > window.start.value
        }
        val windowIntervals = windowPairs.map { it.first }
        val windowClasses = windowPairs.map { it.second }
        val windowScreenOff = dayScreenOff.filter { span ->
            span.start.value < window.end.value && (span.end?.value ?: dayEnd.value) > window.start.value
        }

        val stayResult = StayDetector.detect(window, windowIntervals, windowClasses, windowScreenOff)
        val finishedStays = stayResult.stays.map { stay ->
            val stone = StoneWave.classify(stay, notifs, ports.coverage)
            val returnMinutes = Returns.returnMinutes(stay, windowIntervals, windowClasses, windowScreenOff, window.end)
            stay.copy(
                origin = stone.origin,
                stonePkg = stone.stonePkg,
                notifClicked = stone.notifClicked,
                returnMinutes = returnMinutes,
            )
        }

        val stretches = StretchBuilder.build(window, finishedStays, windowScreenOff)
        val quietMinutes = stretches.sumOf { it.quietMinutes }

        return WindowWithDetail(window, stretches, finishedStays, quietMinutes, stayResult.glances)
    }
}
