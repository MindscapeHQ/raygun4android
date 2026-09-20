package com.raygun.raygun4android.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.i
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

object CrashReportingWorkerHelper {
    private const val MAX_DATA_SIZE = 10000
    internal const val TEMP_FILE_INPUT = "file"
    internal const val CACHED_FILE_INPUT = "cachedFile"
    internal const val API_KEY_INPUT = "apikey"
    private const val CACHED_WORK_PREFIX = "raygun-cached-crash-"

    fun enqueueCrashReport(
        context: Context,
        message: String,
        apiKey: String?,
    ) {
        val inputData: Data
        val encoded = message.toByteArray(StandardCharsets.UTF_8)

        // Store the message in a file to circumvent the WorkManager's 10240 bytes limit
        val fileName = storeMessageInTempFile(context, encoded)
        i("Stored temp file: $fileName")
        inputData =
            Data
                .Builder()
                .putString(TEMP_FILE_INPUT, fileName)
                .putString(API_KEY_INPUT, apiKey)
                .build()

        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val workRequest =
            OneTimeWorkRequest
                .Builder(CrashReportingWorker::class.java)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        i("Work for CrashReportingWorker has been put into the queue.")
    }

    internal fun enqueueCachedCrashReport(
        context: Context,
        file: File,
        apiKey: String?,
    ) {
        val workRequest = cachedCrashReportWorkRequest(file, apiKey)
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                cachedWorkName(file),
                ExistingWorkPolicy.KEEP,
                workRequest,
            )
    }

    internal fun cachedCrashReportWorkRequest(
        file: File,
        apiKey: String?,
    ): OneTimeWorkRequest {
        val inputData =
            Data
                .Builder()
                .putString(CACHED_FILE_INPUT, file.absolutePath)
                .putString(API_KEY_INPUT, apiKey)
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

    private fun storeMessageInTempFile(
        context: Context,
        message: ByteArray,
    ): String {
        @SuppressLint("SimpleDateFormat")
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date(System.currentTimeMillis()))
        val uuid = UUID.randomUUID().toString().replace("-", "")

        val file =
            File(
                context.filesDir,
                timestamp + "-" + uuid + "." + RaygunSettings.DEFAULT_FILE_EXTENSION,
            )

        try {
            FileOutputStream(file).use { fos ->
                fos.write(message)
                i("Crash report message has been written to file.")
            }
        } catch (e: IOException) {
            e("Failed to write crash report message to file: " + e.message)
        }

        return file.name
    }
}
