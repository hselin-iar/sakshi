package com.kleos.sakshi.host

import android.app.usage.UsageEvents
import com.kleos.sakshi.data.*
import com.kleos.sakshi.engine.model.*
import com.kleos.sakshi.engine.tuning.Tuning
import org.junit.Assert.*
import org.junit.Test

class IngestOrchestratorTest : DbTest() {
    private val asOf = 100L * DAY
    private val retentionMs = Tuning.ASSUMED_PLATFORM_RETENTION_DAYS * DAY

    private class FakeSource(var events: List<RawEvent> = emptyList(), var failWith: Exception? = null) : EventSource {
        val calls = mutableListOf<Pair<Long, Long>>()
        override fun read(from: EpochMs, to: EpochMs): List<RawEvent> {
            calls += from.value to to.value
            failWith?.let { throw it }
            return events.filter { it.ts.value >= from.value && it.ts.value < to.value }
        }
    }

    private var processed = mutableListOf<Long>()
    private var eventCountWhenProcessed = -1
    private var hasAccess = true

    private fun deps(source: EventSource): IngestDeps {
        val events = RoomEventStore(db)
        return IngestDeps(
            source = source, hasUsageAccess = { hasAccess }, events = events, gaps = RoomGapStore(db),
            state = RoomStateStore(db), retention = Retention(events, RoomNotifStore(db)),
            process = { at -> processed += at.value; eventCountWhenProcessed = events.count(); ProcessReport(lakeChanged = true) })
    }

    @Test fun firstRunWithNoCursorReadsBackAsFarAsThePlatformKeepsAndAppends() {
        val source = FakeSource(listOf(raw(asOf - 2 * DAY), raw(asOf - 1 * DAY), raw(asOf - 10 * MIN)))
        val result = runIngest(deps(source), t(asOf))

        assertEquals(asOf - retentionMs - DAY - 10 * MIN, source.calls.single().first)
        assertEquals(asOf, source.calls.single().second)
        assertEquals(3, (result as IngestReport.Ran).newEvents)
        val state = RoomStateStore(db).ingest()
        assertEquals(asOf - 10 * MIN - 10 * MIN, state.cursor!!.value)   // newest event minus the overlap margin
        assertEquals(asOf, state.lastRunAt!!.value)
        assertEquals(asOf, state.firstReadAt!!.value)
        assertEquals(1, state.workerRuns7d)
        assertNull(state.lastError)
    }

    @Test fun theEngineRunsOnceAfterTheAppendWithTheSameClock() {
        runIngest(deps(FakeSource(listOf(raw(asOf - MIN)))), t(asOf))
        assertEquals(listOf(asOf), processed)
        assertEquals(1, eventCountWhenProcessed)
    }

    @Test fun repeatedRunReadsFromTheCursorMinusTheOverlapAndAddsNoDuplicates() {
        val source = FakeSource(listOf(raw(asOf - 30 * MIN), raw(asOf - 20 * MIN), raw(asOf - 5 * MIN)))
        runIngest(deps(source), t(asOf))
        val cursor = RoomStateStore(db).ingest().cursor!!.value

        val second = runIngest(deps(source), t(asOf + 15 * MIN))
        assertEquals(cursor - 10 * MIN, source.calls.last().first)        // re-reads the overlap
        assertEquals(0, (second as IngestReport.Ran).newEvents)           // the unique key swallowed it
        assertEquals(3, RoomEventStore(db).count())
        assertEquals(asOf - 5 * MIN - 10 * MIN, RoomStateStore(db).ingest().cursor!!.value)
        assertEquals(2, RoomStateStore(db).ingest().workerRuns7d)
    }

    @Test fun anEmptyReadKeepsTheExistingCursor() {
        val source = FakeSource(listOf(raw(asOf - 5 * MIN)))
        runIngest(deps(source), t(asOf))
        val cursor = RoomStateStore(db).ingest().cursor
        source.events = emptyList()
        runIngest(deps(source), t(asOf + 15 * MIN))
        assertEquals(cursor, RoomStateStore(db).ingest().cursor)
    }

    @Test fun awayLongerThanThePlatformKeepsWritesANotSeenGap() {
        RoomStateStore(db).let { it.saveIngest(it.ingest().copy(lastRunAt = t(asOf - 5 * DAY), cursor = t(asOf - 5 * DAY))) }
        runIngest(deps(FakeSource()), t(asOf))
        val gap = RoomGapStore(db).overlapping(t(0), t(asOf * 2)).single()
        assertEquals(GapKind.NOT_SEEN, gap.kind)
        assertEquals(asOf - 5 * DAY, gap.start.value)
        assertEquals(asOf - retentionMs, gap.end!!.value)
    }

    @Test fun awayExactlyAsLongAsThePlatformKeepsIsNotAGap() {
        RoomStateStore(db).let { it.saveIngest(it.ingest().copy(lastRunAt = t(asOf - retentionMs), cursor = t(asOf - retentionMs))) }
        runIngest(deps(FakeSource()), t(asOf))
        assertEquals(0, RoomGapStore(db).overlapping(t(0), t(asOf * 2)).size)
    }

    @Test fun aPausedRunAppendsNothingAndTouchesNothing() {
        val source = FakeSource(listOf(raw(asOf - MIN)))
        RoomStateStore(db).let { it.saveIngest(it.ingest().copy(paused = true, pausedSince = t(asOf - DAY))) }
        assertEquals(IngestReport.Paused, runIngest(deps(source), t(asOf)))
        assertTrue(source.calls.isEmpty())
        assertEquals(0, RoomEventStore(db).count())
        assertTrue(processed.isEmpty())
        assertNull(RoomStateStore(db).ingest().lastRunAt)
    }

    @Test fun revokedUsageAccessGivesATypedNoPermissionAndKeepsStoredData() {
        RoomEventStore(db).append(listOf(raw(asOf - DAY)))
        hasAccess = false
        assertEquals(IngestReport.NoPermission, runIngest(deps(FakeSource()), t(asOf)))
        assertEquals(1, RoomEventStore(db).count())
        assertTrue(processed.isEmpty())
    }

    @Test fun aSecurityExceptionFromTheSourceIsAlsoNoPermission() {
        val source = FakeSource(failWith = SecurityException("revoked"))
        assertEquals(IngestReport.NoPermission, runIngest(deps(source), t(asOf)))
        assertTrue(processed.isEmpty())
    }

    @Test fun anyOtherFailureIsRecordedAsAShortCodeAndReturnedNotThrown() {
        val result = runIngest(deps(FakeSource(failWith = IllegalStateException("boom with com.example.pkg"))), t(asOf))
        assertEquals(IngestReport.Failed("INGEST_IllegalStateException"), result)
        assertEquals("INGEST_IllegalStateException", RoomStateStore(db).ingest().lastError)
        assertNull(RoomStateStore(db).ingest().lastRunAt)   // the run did not complete
    }

    @Test fun retentionRunsAtTheEndOfEveryIngest() {
        RoomEventStore(db).append(listOf(raw(asOf - 15 * DAY, pkg = "old")))
        runIngest(deps(FakeSource(listOf(raw(asOf - MIN)))), t(asOf))
        assertEquals(listOf(asOf - MIN), RoomEventStore(db).range(t(0), t(asOf + 1)).map { it.ts.value })
    }

    @Test fun everyPackageIsKeptIncludingSakshisOwn() {
        runIngest(deps(FakeSource(listOf(raw(asOf - 3 * MIN, pkg = "com.kleos.sakshi"), raw(asOf - 2 * MIN, pkg = "com.android.systemui")))), t(asOf))
        assertEquals(setOf("com.kleos.sakshi", "com.android.systemui"), RoomEventStore(db).range(t(0), t(asOf)).map { it.pkg!!.value }.toSet())
    }

    @Test fun runCountFadesOverTheWindowAndNeverGoesNegative() {
        assertEquals(1, runsInLast7Days(0, null, t(asOf)))
        assertEquals(101, runsInLast7Days(100, t(asOf), t(asOf)))          // same instant: nothing fades
        assertEquals(1, runsInLast7Days(500, t(asOf - 8 * DAY), t(asOf)))  // older than the window: only this run
        assertTrue(runsInLast7Days(672, t(asOf - 15 * MIN), t(asOf)) in 672..673)
    }

    @Test fun androidEventConstantsMapToTheSixVocabularyNamesAndNothingElse() {
        assertEquals(RawType.ACTIVITY_RESUMED, UsageEventsSource.typeFor(UsageEvents.Event.ACTIVITY_RESUMED))
        assertEquals(RawType.ACTIVITY_PAUSED, UsageEventsSource.typeFor(UsageEvents.Event.ACTIVITY_PAUSED))
        assertEquals(RawType.SCREEN_INTERACTIVE, UsageEventsSource.typeFor(UsageEvents.Event.SCREEN_INTERACTIVE))
        assertEquals(RawType.SCREEN_NON_INTERACTIVE, UsageEventsSource.typeFor(UsageEvents.Event.SCREEN_NON_INTERACTIVE))
        assertEquals(RawType.KEYGUARD_SHOWN, UsageEventsSource.typeFor(UsageEvents.Event.KEYGUARD_SHOWN))
        assertEquals(RawType.KEYGUARD_HIDDEN, UsageEventsSource.typeFor(UsageEvents.Event.KEYGUARD_HIDDEN))
        assertNull(UsageEventsSource.typeFor(UsageEvents.Event.USER_INTERACTION))
        assertNull(UsageEventsSource.typeFor(UsageEvents.Event.STANDBY_BUCKET_CHANGED))
        assertNull(UsageEventsSource.typeFor(-1))
    }
}
