package com.raygun.raygun4android.workers

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.SerializedMessage
import com.raygun.raygun4android.logging.RaygunLogger.d
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.responseCode
import com.raygun.raygun4android.logging.RaygunLogger.w
import com.raygun.raygun4android.network.RaygunNetworkUtils.hasInternetConnection
import com.raygun.raygun4android.utils.RaygunFileFilter
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class CrashReportingWorker(
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Retrieve data from WorkManager
        val encoded = inputData.getByteArray("msg")
        val file = inputData.getString("file")
        val apiKey = inputData.getString("apikey")

        var message: String? = null
        if (encoded == null && file != null) {
            message = readMessageFromTempFileAndDelete(file)
        } else if (encoded != null) {
            message = String(encoded, StandardCharsets.UTF_8)
        }

        if (message != null && apiKey != null) {
            if (hasInternetConnection(applicationContext)) {
                val responseCode = postCrashReporting(apiKey, message)
                responseCode(responseCode)

                if (responseCode == RaygunSettings.RESPONSE_CODE_RATE_LIMITED) {
                    saveMessage(message)
                }
            } else {
                saveMessage(message)
            }
            return Result.success()
        }

        e("No message or API key was provided.")
        return Result.failure()
    }

    private fun saveMessage(message: String) {
        synchronized(this) {
            val cachedFiles =
                arrayListOf(
                    applicationContext.cacheDir.listFiles(RaygunFileFilter()) ?: emptyList<File>(),
                )
            if (cachedFiles.size < RaygunSettings.getMaxReportsStoredOnDevice()) {
                @SuppressLint("SimpleDateFormat")
                val timestamp =
                    SimpleDateFormat("yyyyMMddHHmmss").format(Date(System.currentTimeMillis()))
                val uuid = UUID.randomUUID().toString().replace("-", "")
                val file =
                    File(
                        applicationContext.cacheDir,
                        (timestamp + "-" + uuid + "." + RaygunSettings.DEFAULT_FILE_EXTENSION),
                    )

                try {
                    ObjectOutputStream(FileOutputStream(file)).use { out ->
                        val serializedMessage = SerializedMessage(message)
                        out.writeObject(serializedMessage)
                        out.close()
                    }
                } catch (e: FileNotFoundException) {
                    e("Error creating file when caching message to filesystem: " + e.message)
                } catch (e: IOException) {
                    e("Error writing message to filesystem: " + e.message)
                }
            } else {
                w("Maximum stored reports reached. Discarding message.")
            }
        }
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
                val endpoint = RaygunSettings.getCrashReportingEndpoint()
                val mediaType: MediaType? = "application/json; charset=utf-8".toMediaTypeOrNull()
                val client = RaygunSettings.getHttpClient()
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
                    if (response != null) {
                        if (response.body != null) {
                            response.body!!.close()
                        }
                    }
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
                        reader.forEachLine { line ->
                            message.append(line).append("\n")
                        }
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
}
