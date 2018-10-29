package main.java.com.mindscapehq.android.raygun4android;

import java.util.HashSet;

public class RaygunSettings {
  // General
  public static final String RAYGUN_CLIENT_VERSION = "3.0.6";
  public static final String APIKEY_MANIFEST_FIELD = "com.mindscapehq.android.raygun4android.apikey";

  // Crash Reporting
  private static final String DEFAULT_CRASHREPORTING_ENDPOINT = "https://api.raygun.io/entries";
  public static final String DEFAULT_FILE_EXTENSION = "raygun";
  private static final int DEFAULT_MAX_REPORTS_STORED_ON_DEVICE = 64;

  // RUM
  public  static final String RUM_EVENT_SESSION_START = "session_start";
  public  static final String RUM_EVENT_SESSION_END   = "session_end";
  public  static final String RUM_EVENT_TIMING        = "mobile_event_timing";
  private static final String DEFAULT_RUM_ENDPOINT    = "https://api.raygun.io/events";

  private static IgnoredURLs ignoredURLs = new IgnoredURLs("api.raygun.io");
  private static HashSet<String> ignoredViews = new HashSet<String>();
  private static int maxReportsStoredOnDevice = DEFAULT_MAX_REPORTS_STORED_ON_DEVICE;
  private static String crashReportingEndpoint = DEFAULT_CRASHREPORTING_ENDPOINT;
  private static String rumEndpoint = DEFAULT_RUM_ENDPOINT;

  private RaygunSettings() {
  }

  public static String getCrashReportingEndpoint() {
    return crashReportingEndpoint;
  }

  public static void setCrashReportingEndpoint(String crashReportingEndpoint) {
    RaygunSettings.crashReportingEndpoint = crashReportingEndpoint;
  }

  public static String getRUMEndpoint() {
    return rumEndpoint;
  }

  public static void setRUMEndpoint(String rumEndpoint) {
    RaygunSettings.rumEndpoint = rumEndpoint;
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
