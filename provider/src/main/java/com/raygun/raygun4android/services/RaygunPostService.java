package com.raygun.raygun4android.services;

import androidx.core.app.JobIntentService;

import com.raygun.raygun4android.logging.RaygunLogger;
import com.raygun.raygun4android.logging.TimberRaygunLoggerImplementation;

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
    public void onCreate() {
        super.onCreate();

        TimberRaygunLoggerImplementation.init();

        RaygunLogger.i("onCreate() in RaygunPostService executed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        RaygunLogger.i("onDestroy() in RaygunPostService executed");
    }
}
