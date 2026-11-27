package com.skyplus.billdesk.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skyplus.billdesk.data.database.BillDeskDatabase
import java.io.File
import java.util.concurrent.TimeUnit

/** Offline-only periodic backup of the append-only event history. */
class DailyEventBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = BillDeskDatabase.open(applicationContext)
        return try {
            val events = database.posDao().allEvents()
            val date = java.time.LocalDate.now().toString()
            val directory = File(applicationContext.filesDir, "backups").apply { mkdirs() }
            val destination = File(directory, "daily-events-$date.json")
            val temporary = File(directory, "${destination.name}.tmp")
            temporary.outputStream().buffered().use { EventBackupSerializer.writeTo(it, date, events) }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        } finally {
            database.close()
        }
    }
}

object DailyEventBackupScheduler {
    private const val UNIQUE_NAME = "skyplus-daily-event-backup"

    /** No network constraint: this job runs and saves locally when offline. */
    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyEventBackupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
