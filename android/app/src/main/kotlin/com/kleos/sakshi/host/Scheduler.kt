package com.kleos.sakshi.host

import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Background catch-up. No foreground service, no wake lock, no exact alarm: a missed run is made good by the next one,
 * because every run re-reads from the cursor and the unique key makes that safe (DOC 2 §2.7.3).
 */
object Scheduler {
    const val PERIODIC_NAME = "sakshi_ingest"
    const val NOW_NAME = "sakshi_ingest_now"
    const val PERIODIC_MINUTES = 15L

    /** Called on every app start. KEEP leaves an existing schedule alone; WorkManager restores it after a reboot by itself. */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<IngestWorker>(PERIODIC_MINUTES, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** One catch-up now. A second request while one is waiting replaces it, so taps never pile up. */
    fun runNow(context: Context) {
        val builder = OneTimeWorkRequestBuilder<IngestWorker>()
        if (shouldExpedite(Build.VERSION.SDK_INT)) builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        WorkManager.getInstance(context).enqueueUniqueWork(NOW_NAME, ExistingWorkPolicy.REPLACE, builder.build())
    }

    /** Before Android 12, expedited work runs as a foreground service, which this app never declares. */
    fun shouldExpedite(sdk: Int): Boolean = sdk >= Build.VERSION_CODES.S
}
