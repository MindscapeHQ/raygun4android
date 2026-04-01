package com.raygun.raygun4android.workers

import androidx.work.ListenableWorker.Result
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.e

object RaygunWorkerHelper {
    /**
     * Validation to check if an API key has been supplied to the worker
     *
     * @param apiKey The API key of the app to deliver to
     * @return true or false
     */
    @JvmStatic
    fun validateApiKey(apiKey: String): Boolean {
        if (apiKey.isEmpty()) {
            e("API key is empty, nothing will be logged or reported.")
            return false
        } else {
            return true
        }
    }

    /**
     * Maps an HTTP response code to a WorkManager [Result].
     *
     * - 2xx → [Result.success]
     * - 400, 403 → [Result.failure] (permanent client errors, retrying won't help)
     * - 429, 5xx, -1 (network/exception) → [Result.retry]
     */
    @JvmStatic
    fun toWorkerResult(responseCode: Int): Result =
        when {
            responseCode in 200..299 -> Result.success()
            responseCode == RaygunSettings.RESPONSE_CODE_BAD_MESSAGE ||
                responseCode == RaygunSettings.RESPONSE_CODE_INVALID_API_KEY ||
                responseCode == RaygunSettings.RESPONSE_CODE_LARGE_PAYLOAD -> Result.failure()
            else -> Result.retry()
        }
}
