package com.raygun.raygun4android.workers;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.raygun.raygun4android.logging.RaygunLogger;

public class RaygunWorkerHelper {

    /**
     * Validation to check if an API key has been supplied to the worker
     *
     * @param apiKey The API key of the app to deliver to
     * @return true or false
     */
    protected static Boolean validateApiKey(String apiKey) {
        if (apiKey.isEmpty()) {
            RaygunLogger.e("API key is empty, nothing will be logged or reported.");
            return false;
        } else {
            return true;
        }
    }
}
