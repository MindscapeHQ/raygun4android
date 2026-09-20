package com.raygun.raygun4android.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.SerializedMessage
import com.raygun.raygun4android.logging.RaygunLogger.d
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.responseCode
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.nio.charset.StandardCharsets

class CrashReportingWorker(
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Retrieve data from WorkManager
        val temporaryFile = inputData.getString(CrashReportingWorkerHelper.TEMP_FILE_INPUT)
        val cachedFile = inputData.getString(CrashReportingWorkerHelper.CACHED_FILE_INPUT)
        val apiKey = inputData.getString(CrashReportingWorkerHelper.API_KEY_INPUT)
        val legacySerialized =
            inputData.getBoolean(CrashReportingWorkerHelper.LEGACY_SERIALIZED_INPUT, false)

        if (temporaryFile.isNullOrEmpty() && cachedFile.isNullOrEmpty()) {
            e("No file provided in input data.")
            return Result.failure()
        }

        val file =
            if (cachedFile != null) {
                File(cachedFile)
            } else {
                File(applicationContext.filesDir, temporaryFile!!)
            }

        return processCrashReport(
            file = file,
            isLegacySerialized = legacySerialized,
            apiKey = apiKey,
            postCrashReport = ::postCrashReporting,
        )
    }

    /**
     * Raw post method that delivers a pre-built Crash Reporting payload to the Raygun API.
     *
     * @param apiKey The API key of the app to deliver to
     * @param jsonPayload The JSON representation of a RaygunMessage to be delivered over HTTPS.
     * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message
     *   (invalid properties), 429 if rate limited
     */
    private fun postCrashReporting(
        apiKey: String,
        jsonPayload: String,
    ): Int {
        try {
            if (RaygunWorkerHelper.validateApiKey(apiKey)) {
                val endpoint = RaygunSettings.crashReportingEndpoint
                val mediaType: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
                val client = RaygunSettings.httpClient
                val body = jsonPayload.toRequestBody(mediaType)
                val request =
                    Request
                        .Builder()
                        .url(endpoint)
                        .header("X-ApiKey", apiKey)
                        .post(body)
                        .build()
                var response: Response? = null
                try {
                    response = client.newCall(request).execute()
                    d("Crash Reporting HTTP POST result: " + response.code)
                    return response.code
                } catch (ioe: IOException) {
                    e("OkHttp POST to Raygun Crash Reporting backend failed: " + ioe.message)
                    ioe.printStackTrace()
                } finally {
                    response?.body?.close()
                }
            }
        } catch (e: Exception) {
            e("Error posting to Crash Reporting: " + e.message)
            e.printStackTrace()
        }
        return -1
    }

    private fun readMessageFromTempFile(file: File): String? {
        val message = StringBuilder()

        try {
            file.inputStream().use { fis ->
                InputStreamReader(fis, StandardCharsets.UTF_8).use { isr ->
                    isr.buffered().use { reader ->
                        reader.forEachLine { line -> message.append(line).append("\n") }
                    }
                }
            }
        } catch (e: IOException) {
            e("Failed to read message from file: " + e.message)
            return null
        }

        return message.toString().trimEnd()
    }

    private fun readMessageFromCache(file: File): String? =
        try {
            ObjectInputStream(FileInputStream(file)).use { input ->
                (input.readObject() as SerializedMessage).message
            }
        } catch (exception: Exception) {
            e("Failed to read cached message: " + exception.message)
            null
        }

    internal fun processCrashReport(
        file: File,
        isLegacySerialized: Boolean,
        apiKey: String?,
        postCrashReport: (String, String) -> Int,
    ): Result {
        val message =
            if (isLegacySerialized) {
                readMessageFromCache(file)
            } else if (file.parentFile == applicationContext.filesDir) {
                readMessageFromTempFile(file)
            } else {
                CrashReportCache.readPersistent(file)
            }

        if (message == null) {
            e("No message was provided.")
            CrashReportCache.markProcessed(applicationContext, file)
            return Result.failure()
        }

        if (apiKey.isNullOrBlank()) {
            e("No API key was provided; retaining cached crash report.")
            return Result.failure()
        }

        val responseCode = postCrashReport(apiKey, message)
        responseCode(responseCode)
        val result = RaygunWorkerHelper.toWorkerResult(responseCode)

        if (
            responseCode in 200..299 ||
            responseCode == RaygunSettings.RESPONSE_CODE_BAD_MESSAGE ||
            responseCode == RaygunSettings.RESPONSE_CODE_INVALID_API_KEY ||
            responseCode == RaygunSettings.RESPONSE_CODE_LARGE_PAYLOAD
        ) {
            CrashReportCache.markProcessed(applicationContext, file)
        }

        return result
    }
}
