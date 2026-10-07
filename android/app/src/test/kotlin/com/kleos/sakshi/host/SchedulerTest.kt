package com.kleos.sakshi.host

import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SchedulerTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = WorkManager.getInstance(context)

    @Before fun startWorkManager() {
        // Workers never run (parked executor): these tests look at what was scheduled, not at a run.
        val parked = Executor { }
        try {
            WorkManager.initialize(context, Configuration.Builder().setExecutor(parked).build())
        } catch (_: IllegalStateException) {
            // already initialised by an earlier test in this JVM; start from an empty queue instead
        }
        manager.cancelAllWork().result.get()
        manager.pruneWork().result.get()
    }

    private fun infos(name: String) = manager.getWorkInfosForUniqueWork(name).get()

    @Test fun periodicJobIsOneUniqueFifteenMinuteRequest() {
        Scheduler.ensurePeriodic(context)
        val info = infos(Scheduler.PERIODIC_NAME).single()
        assertEquals(WorkInfo.State.ENQUEUED, info.state)
        assertEquals(TimeUnit.MINUTES.toMillis(15), info.periodicityInfo!!.repeatIntervalMillis)
    }

    @Test fun startingTheAppAgainKeepsTheExistingScheduleInsteadOfDuplicatingIt() {
        Scheduler.ensurePeriodic(context)
        val first = infos(Scheduler.PERIODIC_NAME).single().id
        Scheduler.ensurePeriodic(context); Scheduler.ensurePeriodic(context)
        assertEquals(first, infos(Scheduler.PERIODIC_NAME).single().id)
    }

    @Test fun runNowQueuesOneRequestAndASecondTapReplacesItInsteadOfPilingUp() {
        Scheduler.runNow(context)
        val first = infos(Scheduler.NOW_NAME).single { it.state == WorkInfo.State.ENQUEUED }.id
        Scheduler.runNow(context)
        val live = infos(Scheduler.NOW_NAME).filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, live.size)
        assertNotEquals(first, live.single().id)
    }

    @Test fun expeditedOnlyFromAndroid12SoNoForegroundServiceIsEverNeeded() {
        assertFalse(Scheduler.shouldExpedite(Build.VERSION_CODES.Q))
        assertFalse(Scheduler.shouldExpedite(Build.VERSION_CODES.R))
        assertTrue(Scheduler.shouldExpedite(Build.VERSION_CODES.S))
        assertTrue(Scheduler.shouldExpedite(Build.VERSION_CODES.VANILLA_ICE_CREAM))
    }

    @Test fun aThrowInsideTheWorkerBodyIsSwallowedSoTheJobStaysScheduled() {
        var reached = false
        IngestWorker.safely { throw IllegalStateException("container failed") }
        IngestWorker.safely { reached = true }
        assertTrue(reached)
    }
}
