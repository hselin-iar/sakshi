package com.kleos.sakshi.host

import com.kleos.sakshi.data.Retention
import com.kleos.sakshi.engine.model.DataGap
import com.kleos.sakshi.engine.model.EpochMs
import com.kleos.sakshi.engine.model.GapKind
import com.kleos.sakshi.engine.model.ProcessReport
import com.kleos.sakshi.engine.ports.EventStore
import com.kleos.sakshi.engine.ports.GapStore
import com.kleos.sakshi.engine.ports.StateStore
import com.kleos.sakshi.engine.tuning.Tuning

/** Plain Kotlin: no Android types, so it runs against fakes. */
class IngestDeps(
    val source: EventSource,
    val hasUsageAccess: () -> Boolean,
    val events: EventStore,
    val gaps: GapStore,
    val state: StateStore,
    val retention: Retention,
    /** The engine's processNewEvents. */
    val process: (EpochMs) -> ProcessReport,
    /** Step 7: refresh the widget, post the weekly note. Wired in T1.7 and later. */
    val afterProcess: (ProcessReport) -> Unit = {})

sealed interface IngestReport {
    data class Ran(val newEvents: Int, val report: ProcessReport) : IngestReport
    data object Paused : IngestReport
    data object NoPermission : IngestReport
    /** A demo is running: nothing is read from the phone. */
    data object DemoActive : IngestReport
    /** `code` is a short code with no package names; the next scheduled run is the retry. */
    data class Failed(val code: String) : IngestReport
}

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val OVERLAP_MARGIN_MS = 10L * 60 * 1000          // queryEvents may omit the last few minutes
private const val RUN_COUNT_WINDOW_MS = 7 * DAY_MS

fun runIngest(deps: IngestDeps, asOf: EpochMs): IngestReport {
    // 1. paused: append nothing
    val before = deps.state.ingest()
    if (before.paused) return IngestReport.Paused
    if (!deps.hasUsageAccess()) return IngestReport.NoPermission

    val retentionMs = Tuning.ASSUMED_PLATFORM_RETENTION_DAYS * DAY_MS
    return try {
        // 2. from the cursor, or as far back as the platform is assumed to keep events
        val from = before.cursor?.value ?: (asOf.value - retentionMs - DAY_MS)

        // 3. re-read the overlap; the unique key swallows what is already stored
        val read = deps.source.read(EpochMs(from - OVERLAP_MARGIN_MS), asOf)
        val countBefore = deps.events.count()
        deps.events.append(read)
        val newEvents = deps.events.count() - countBefore

        // 4. away longer than the platform keeps events: the stretch in between was not seen
        before.lastRunAt?.let { last ->
            if (asOf.value - last.value > retentionMs) deps.gaps.add(DataGap(GapKind.NOT_SEEN, last, EpochMs(asOf.value - retentionMs)))
        }

        // 5. cursor, last run, run count
        val newest = read.maxOfOrNull { it.ts.value }
        deps.state.saveIngest(
            before.copy(
                cursor = newest?.let { EpochMs(it - OVERLAP_MARGIN_MS) } ?: before.cursor,
                lastRunAt = asOf,
                firstReadAt = before.firstReadAt ?: asOf,
                lastWorkerRunAt = asOf,
                workerRuns7d = runsInLast7Days(before.workerRuns7d, before.lastWorkerRunAt, asOf),
                lastError = null))

        // 6. derive, 7. host side effects
        val report = deps.process(asOf)
        deps.afterProcess(report)

        // 8. retention
        deps.retention.purge(asOf)
        IngestReport.Ran(newEvents, report)
    } catch (_: SecurityException) {
        IngestReport.NoPermission
    } catch (e: Exception) {
        val code = "INGEST_" + (e::class.simpleName ?: "ERROR")
        deps.state.saveIngest(deps.state.ingest().copy(lastError = code))
        IngestReport.Failed(code)
    }
}

/**
 * No run log is stored, so the 7-day count is a decaying estimate: the old count fades over the time since the last run
 * and this run adds one. Runs every 15 minutes settle near 672 a week.
 */
internal fun runsInLast7Days(previous: Int, lastRun: EpochMs?, asOf: EpochMs): Int {
    if (lastRun == null) return 1
    val elapsed = (asOf.value - lastRun.value).coerceAtLeast(0)
    val kept = previous * (1.0 - (elapsed.toDouble() / RUN_COUNT_WINDOW_MS)).coerceIn(0.0, 1.0)
    return Math.round(kept).toInt() + 1
}
