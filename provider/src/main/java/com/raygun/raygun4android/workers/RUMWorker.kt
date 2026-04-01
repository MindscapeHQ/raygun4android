package com.raygun.raygun4android.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.d
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.responseCode
import com.raygun.raygun4android.logging.RaygunLogger.v
import com.raygun.raygun4android.network.ConnectivityUtils
import com.raygun.raygun4android.workers.RaygunWorkerHelper.toWorkerResult
import com.raygun.raygun4android.workers.RaygunWorkerHelper.validateApiKey
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class RUMWorker(
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        // Retrieve data from WorkManager
        val message = inputData.getString("msg")
        val apiKey = inputData.getString("apikey")

        v(message)

        // Moved the check for internet connection as close as possible to the calls because the
        // condition can change quite rapidly
        if (message != null && apiKey != null) {
            if (ConnectivityUtils.isNetworkAvailable(applicationContext)) {
                val responseCode = postRUM(apiKey, message)
                responseCode(responseCode)
                return toWorkerResult(responseCode)
            }
            return Result.retry()
        }
        e("No message or API key was provided.")
        return Result.failure()
    }

    companion object {
        /**
         * Raw post method that delivers a pre-built RUM payload to the Raygun API.
         *
         * @param apiKey The API key of the app to deliver to
         * @param jsonPayload The JSON representation of a RUM event to be delivered over HTTPS. The
         *   payload should be a JSON object with the following structure: { "eventName": "string",
         *   // The name of the event (e.g., "pageView", "click"). "timestamp": "string", // The ISO
         *   8601 timestamp of the event. "properties": { // Additional properties related to the
         *   event. "key1": "value1", "key2": "value2" } }
         * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message
         *   (invalid properties)
         */
        private fun postRUM(
            apiKey: String,
            jsonPayload: String,
        ): Int {
            try {
                if (validateApiKey(apiKey)) {
                    val endpoint = RaygunSettings.rumEndpoint
                    val mediaType: MediaType? =
                        "application/json; charset=utf-8".toMediaTypeOrNull()
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
                        d("RUM HTTP POST result: " + response.code)
                        return response.code
                    } catch (ioe: IOException) {
                        e("OkHttp POST to Raygun RUM backend failed: " + ioe.message)
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
                e("Can't post to RUM. Exception - " + e.message)
                e.printStackTrace()
            }
            return -1
        }
    }
}
