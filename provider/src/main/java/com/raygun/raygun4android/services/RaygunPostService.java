package com.raygun.raygun4android.services;

import android.support.v4.app.JobIntentService;

import com.raygun.raygun4android.RaygunLogger;

/**
 * A JobIntentService that can validate Raygun API keys
 */
public abstract class RaygunPostService extends JobIntentService {
    /**
     * Validation to check if an API key has been supplied to the service
     *
     * @param apiKey      The API key of the app to deliver to
     * @return true or false
     */
    static Boolean validateApiKey(String apiKey) {
        if (apiKey.length() == 0) {
            RaygunLogger.e("API key is empty, nothing will be logged or reported");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
