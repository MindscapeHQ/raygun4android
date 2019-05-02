package com.raygun.raygun4android;

import java.util.Arrays;
import java.util.HashSet;

public class RaygunSettings {

    // General
    public static final String APIKEY_MANIFEST_FIELD = "com.raygun.raygun4android.apikey";
    public static final String RAYGUN_CLIENT_VERSION = BuildConfig.VERSION_NAME;

    // HTTP error response codes
    public static final int RESPONSE_CODE_ACCEPTED = 202;
    public static final int RESPONSE_CODE_BAD_MESSAGE = 400;
    public static final int RESPONSE_CODE_INVALID_API_KEY = 403;
    public static final int RESPONSE_CODE_LARGE_PAYLOAD = 413;
    public static final int RESPONSE_CODE_RATE_LIMITED = 429;

    // Crash Reporting
    public static final String DEFAULT_CRASHREPORTING_ENDPOINT = "https://api.raygun.io/entries";
    public static final String DEFAULT_FILE_EXTENSION = "raygun4";
    public static final int DEFAULT_MAX_REPORTS_STORED_ON_DEVICE = 64;
    public static final String CRASH_REPORTING_UNHANDLED_EXCEPTION_TAG = "UnhandledException";

    // RUM
    public static final String RUM_EVENT_SESSION_START = "session_start";
    public static final String RUM_EVENT_SESSION_END = "session_end";
    public static final String RUM_EVENT_TIMING = "mobile_event_timing";
    public static final String DEFAULT_RUM_ENDPOINT = "https://api.raygun.io/events";
    // 30 minutes in milliseconds
    public static final int RUM_SESSION_EXPIRY = 30 * 60 * 1000;

    private static IgnoredURLs ignoredURLs = new IgnoredURLs("api.raygun.io");
    private static HashSet<String> ignoredViews = new HashSet<>();
    private static int maxReportsStoredOnDevice = DEFAULT_MAX_REPORTS_STORED_ON_DEVICE;
    private static String crashReportingEndpoint = DEFAULT_CRASHREPORTING_ENDPOINT;
    private static String rumEndpoint = DEFAULT_RUM_ENDPOINT;

    private RaygunSettings() {
    }

    public static String getCrashReportingEndpoint() {
        return crashReportingEndpoint;
    }

    static void setCrashReportingEndpoint(String crashReportingEndpoint) {
        RaygunSettings.crashReportingEndpoint = crashReportingEndpoint;
    }

    public static String getRUMEndpoint() {
        return rumEndpoint;
    }

    static void setRUMEndpoint(String rumEndpoint) {
        RaygunSettings.rumEndpoint = rumEndpoint;
    }

    public static HashSet<String> getIgnoredURLs() {
        return ignoredURLs;
    }

    static HashSet<String> getIgnoredViews() {
        return ignoredViews;
    }

    public static int getMaxReportsStoredOnDevice() {
        return maxReportsStoredOnDevice;
    }

    static void setMaxReportsStoredOnDevice(int maxReportsStoredOnDevice) {
        if (maxReportsStoredOnDevice <= DEFAULT_MAX_REPORTS_STORED_ON_DEVICE) {
            RaygunSettings.maxReportsStoredOnDevice = maxReportsStoredOnDevice;
        } else {
            RaygunLogger.w("It's not possible to exceed the value " + DEFAULT_MAX_REPORTS_STORED_ON_DEVICE + " for the number of reports stored on the device. The setting has not been applied.");
        }
    }

    public static class IgnoredURLs extends HashSet<String> {
        IgnoredURLs(String... defaultIgnoredUrls) {
            this.addAll(Arrays.asList(defaultIgnoredUrls));
        }
    }

    static void ignoreURLs(String[] urls) {
        if (urls != null) {
            for (String url : urls) {
                if (url != null) {
                    RaygunSettings.ignoredURLs.add(url);
                }
            }
        }
    }

    static void ignoreViews(String[] views) {
        if (views != null) {
            for (String view : views) {
                if (view != null) {
                    RaygunSettings.ignoredViews.add(view);
                }
            }
        }
    }
}
