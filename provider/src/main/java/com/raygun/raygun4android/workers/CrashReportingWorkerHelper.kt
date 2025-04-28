package com.raygun.raygun4android.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.d
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.i
import com.raygun.raygun4android.logging.RaygunLogger.v
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

object CrashReportingWorkerHelper {
    private const val MAX_DATA_SIZE = 10000

    fun enqueueCrashReport(
        context: Context,
        message: String,
        apiKey: String?,
    ) {
        val inputData: Data
        val encoded = message.toByteArray(StandardCharsets.UTF_8)
        val length = encoded.size
        v("Message length: $length")
        if (length > MAX_DATA_SIZE) {
            d(
                (
                    "Message length (" +
                        length +
                        ") greater than " +
                        MAX_DATA_SIZE +
                        ", storing as file."
                ),
            )
            // Store the message in a file to circumvent the WorkManager's 10240 bytes limit
            val fileName = storeMessageInTempFile(context, encoded)
            i("Stored temp file: $fileName")
            inputData =
                Data
                    .Builder()
                    .putString("file", fileName)
                    .putString("apikey", apiKey)
                    .build()
        } else {
            inputData =
                Data
                    .Builder()
                    .putByteArray("msg", encoded)
                    .putString("apikey", apiKey)
                    .build()
        }

        val workRequest =
            OneTimeWorkRequest
                .Builder(CrashReportingWorker::class.java)
                .setInputData(inputData)
                .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        i("Work for CrashReportingWorker has been put into the queue.")
    }

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
