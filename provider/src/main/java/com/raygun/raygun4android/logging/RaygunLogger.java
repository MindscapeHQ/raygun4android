package com.raygun.raygun4android.logging;

import com.raygun.raygun4android.RaygunSettings;

import timber.log.Timber;

public class RaygunLogger {

    public static void d(String string) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).d(string);
        }
    }

    public static void i(String string) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).i(string);
        }
    }

    public static void w(String string) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).w(string);
        }
    }

    public static void e(String string) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).e(string);
        }
    }

    public static void v(String string) {
        if (string != null) {
            Timber.tag(RaygunSettings.LOGGING_TAG).v(string);
        }
    }

    public static void responseCode(int responseCode) {
        switch (responseCode) {
            case RaygunSettings.RESPONSE_CODE_ACCEPTED:
                RaygunLogger.d("Request succeeded");
                break;
            case RaygunSettings.RESPONSE_CODE_BAD_MESSAGE:
                RaygunLogger.e("Bad message - could not parse the provided JSON. Check all fields are present, especially both occurredOn (ISO 8601 DateTime) and details { } at the top level");
                break;
            case RaygunSettings.RESPONSE_CODE_INVALID_API_KEY:
                RaygunLogger.e("Invalid API Key - The value specified in the header X-ApiKey did not match with an application in Raygun");
                break;
            case RaygunSettings.RESPONSE_CODE_LARGE_PAYLOAD:
                RaygunLogger.e("Request entity too large - The maximum size of a JSON payload is 128KB");
                break;
            case RaygunSettings.RESPONSE_CODE_RATE_LIMITED:
                RaygunLogger.e("Too Many Requests - Plan limit exceeded for month or plan expired");
                break;
            default:
                RaygunLogger.d("Response status code: " + responseCode);

        }
    }

}