package com.raygun.raygun4android;

import java.util.HashSet;

public class RaygunSettings {

    public static final int MAX_REPORTS_STORED_ON_DEVICE_DEFAULT = 64;
    private static final String defaultApiEndpoint = "https://api.raygun.io/entries";
    private static final String defaultPulseEndpoint = "https://api.raygun.io/events";

    private static IgnoredURLs ignoredURLs = new IgnoredURLs("api.raygun.io");
    private static HashSet<String> ignoredViews = new HashSet<String>();
    private static int maxReportsStoredOnDevice = MAX_REPORTS_STORED_ON_DEVICE_DEFAULT;

    public static int getMaxReportsStoredOnDevice() {
        return maxReportsStoredOnDevice;
    }

    public static void setMaxReportsStoredOnDevice(int maxReportsStoredOnDevice) {
        RaygunSettings.maxReportsStoredOnDevice = maxReportsStoredOnDevice;
    }

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

    public static class IgnoredURLs extends HashSet<String> {
        public IgnoredURLs(String... defaultIgnoredUrls) {
            for (String url : defaultIgnoredUrls) {
                add(url);
            }
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
