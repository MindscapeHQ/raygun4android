package com.raygun.raygun4android;

import java.util.Arrays;
import java.util.HashSet;

public class RaygunSettings {

    private static final int MAX_REPORTS_STORED_ON_DEVICE_DEFAULT = 64;
    private static final String defaultApiEndpoint = "https://api.raygun.io/entries";
    private static final String defaultPulseEndpoint = "https://api.raygun.io/events";

    private static IgnoredURLs ignoredURLs = new IgnoredURLs("api.raygun.io");
    private static HashSet<String> ignoredViews = new HashSet<>();
    private static int maxReportsStoredOnDevice = MAX_REPORTS_STORED_ON_DEVICE_DEFAULT;

    private RaygunSettings() {
    }

    public static String getApiEndpoint() {
        return defaultApiEndpoint;
    }

    public static String getPulseEndpoint() {
        return defaultPulseEndpoint;
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
        RaygunSettings.maxReportsStoredOnDevice = maxReportsStoredOnDevice;
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
