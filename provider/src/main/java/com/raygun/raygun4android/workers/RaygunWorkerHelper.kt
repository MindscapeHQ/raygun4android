package com.raygun.raygun4android.workers

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
}
