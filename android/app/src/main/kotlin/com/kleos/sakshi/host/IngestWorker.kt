package com.kleos.sakshi.host

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kleos.sakshi.engine.model.EpochMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One call: runIngest. It always reports success. A failure is recorded in ingest_state by runIngest and the next
 * scheduled run is the retry; Result.retry() would loop on a persistent failure.
 */
class IngestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        safely {
            val report = AppContainer.from(applicationContext).ingest(EpochMs(System.currentTimeMillis()))
            Log.i("SakshiIngest", "run: ${report::class.simpleName}")   // outcome name only, never content
        }
        Result.success()
    }

    companion object {
        /** Anything thrown, including while building the container, is swallowed so the job stays scheduled. */
        internal inline fun safely(block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                Log.w("SakshiIngest", "run failed: ${e::class.simpleName}")
            }
        }
    }
}
