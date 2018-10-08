package com.raygun.raygun4android;

import java.util.Arrays;
import java.util.HashSet;

public class RaygunSettings {

    private static final int DEFAULT_MAX_REPORTS_STORED_ON_DEVICE = 64;
    private static final String DEFAULT_API_ENDPOINT = "https://api.raygun.io/entries";
    private static final String DEFAULT_PULSE_ENDPOINT = "https://api.raygun.io/events";

    private static IgnoredURLs ignoredURLs = new IgnoredURLs("api.raygun.io");
    private static HashSet<String> ignoredViews = new HashSet<>();
    private static int maxReportsStoredOnDevice = DEFAULT_MAX_REPORTS_STORED_ON_DEVICE;
    private static String apiEndpoint = DEFAULT_API_ENDPOINT;
    private static String pulseEndpoint = DEFAULT_PULSE_ENDPOINT;

    private RaygunSettings() {
    }

    public static String getApiEndpoint() {
        return apiEndpoint;
    }

    public static void setApiEndpoint(String apiEndpoint) {
        RaygunSettings.apiEndpoint = apiEndpoint;
    }

    public static String getPulseEndpoint() {
        return pulseEndpoint;
    }

    public static void setPulseEndpoint(String pulseEndpoint) {
        RaygunSettings.pulseEndpoint = pulseEndpoint;
    }

    public static HashSet<String> getIgnoredURLs() {
        return ignoredURLs;
    }

    public static HashSet<String> getIgnoredViews() {
        return ignoredViews;
    }

    public static int getMaxReportsStoredOnDevice() {
        return maxReportsStoredOnDevice;
    }

    public static void setMaxReportsStoredOnDevice(int maxReportsStoredOnDevice) {
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

    public static void ignoreURLs(String[] urls) {
        if (urls != null) {
            for (String url : urls) {
                if (url != null) {
                    RaygunSettings.ignoredURLs.add(url);
                }
            }
        }
    }

    public static void ignoreViews(String[] views) {
        if (views != null) {
            for (String view : views) {
                if (view != null) {
                    RaygunSettings.ignoredViews.add(view);
                }
            }
        }
    }
}
