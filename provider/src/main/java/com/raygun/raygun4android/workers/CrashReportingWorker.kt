package com.raygun.raygun4android.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.SerializedMessage
import com.raygun.raygun4android.logging.RaygunLogger.d
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.responseCode
import com.raygun.raygun4android.network.ConnectivityUtils
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

        if (temporaryFile.isNullOrEmpty() && cachedFile.isNullOrEmpty()) {
            e("No file provided in input data.")
            return Result.failure()
        }

        val message =
            if (cachedFile != null) {
                readMessageFromCacheAndDelete(cachedFile)
            } else {
                readMessageFromTempFileAndDelete(temporaryFile!!)
            }

        if (apiKey != null) {
            if (ConnectivityUtils.isNetworkAvailable(applicationContext)) {
                val responseCode = postCrashReporting(apiKey, message)
                responseCode(responseCode)

                return when {
                    responseCode in 200..299 -> {
                        Result.success()
                    }

                    responseCode == RaygunSettings.RESPONSE_CODE_BAD_MESSAGE ||
                        responseCode == RaygunSettings.RESPONSE_CODE_INVALID_API_KEY ||
                        responseCode == RaygunSettings.RESPONSE_CODE_LARGE_PAYLOAD -> {
                        Result.failure()
                    }

                    else -> {
                        CrashReportCache.store(applicationContext, message)
                        Result.success()
                    }
                }
            } else {
                CrashReportCache.store(applicationContext, message)
                return Result.success()
            }
        }

        e("No message or API key was provided.")
        return Result.failure()
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

    private fun readMessageFromTempFileAndDelete(fileName: String): String {
        val file = File(applicationContext.filesDir, fileName)
        val message = StringBuilder()

        try {
            file.inputStream().use { fis ->
                InputStreamReader(fis, StandardCharsets.UTF_8).use { isr ->
                    isr.buffered().use { reader ->
                        reader.forEachLine { line -> message.append(line).append("\n") }
                    }
                }
            }
            if (!file.delete()) {
                e("Failed to delete the file: $fileName")
            }
        } catch (e: IOException) {
            e("Failed to read message from file: " + e.message)
        }

        return message.toString().trimEnd()
    }

    private fun readMessageFromCacheAndDelete(fileName: String): String {
        val file = File(applicationContext.cacheDir, fileName)

        try {
            val message =
                ObjectInputStream(FileInputStream(file)).use { input ->
                    (input.readObject() as SerializedMessage).message
                }
            if (!file.delete()) {
                e("Failed to delete the file: $fileName")
            }
            return message
        } catch (exception: IOException) {
            e("Failed to read cached message: " + exception.message)
        } catch (exception: ClassNotFoundException) {
            e("Failed to deserialize cached message: " + exception.message)
        }

        return ""
    }
}
