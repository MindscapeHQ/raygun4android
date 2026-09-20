package com.raygun.raygun4android.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.i
import java.io.File

object CrashReportingWorkerHelper {
    internal const val TEMP_FILE_INPUT = "file"
    internal const val CACHED_FILE_INPUT = "cachedFile"
    internal const val API_KEY_INPUT = "apikey"
    internal const val LEGACY_SERIALIZED_INPUT = "legacySerialized"
    private const val CACHED_WORK_PREFIX = "raygun-cached-crash-"

    fun enqueueCrashReport(
        context: Context,
        message: String,
        apiKey: String?,
    ) {
        val file = CrashReportCache.store(context, message) ?: return
        enqueueCachedCrashReport(context, file, apiKey)
    }

    internal fun enqueueCachedCrashReport(
        context: Context,
        file: File,
        apiKey: String?,
    ): Boolean {
        val workRequest = cachedCrashReportWorkRequest(context, file, apiKey)
        return try {
            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(
                    cachedWorkName(file),
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )
            i("Work for CrashReportingWorker has been put into the queue.")
            true
        } catch (exception: IllegalStateException) {
            e("WorkManager is not initialized; cached crash report will be retried later.")
            false
        }
    }

    internal fun cachedCrashReportWorkRequest(
        context: Context,
        file: File,
        apiKey: String?,
    ): OneTimeWorkRequest {
        val inputData =
            Data
                .Builder()
                .putString(CACHED_FILE_INPUT, file.absolutePath)
                .putString(API_KEY_INPUT, apiKey)
                .putBoolean(LEGACY_SERIALIZED_INPUT, isLegacyCacheFile(context, file))
                .build()
        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        return OneTimeWorkRequest
            .Builder(CrashReportingWorker::class.java)
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
    }

    internal fun cachedWorkName(file: File): String = CACHED_WORK_PREFIX + file.absolutePath

    private fun isLegacyCacheFile(
        context: Context,
        file: File,
    ): Boolean = file.parentFile?.absolutePath == context.cacheDir.absolutePath
}
