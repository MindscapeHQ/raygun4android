package main.java.com.mindscapehq.android.raygun4android;

import java.util.HashSet;

public class RaygunSettings {
  private RaygunSettings() {
  }

  private static final String defaultApiEndpoint = "https://api.raygun.io/entries";
  private static final String defaultPulseEndpoint = "https://api.raygun.io/events";
  private static IgnoredURLs ignoredURLs = new IgnoredURLs("api.raygun.io");
  private static HashSet<String> ignoredActivities = new HashSet<String>();

  public static String getApiEndpoint() {
    return defaultApiEndpoint;
  }

  public static String getPulseEndpoint() {
    return defaultPulseEndpoint;
  }

  public static HashSet<String> getIgnoredURLs() {
    return ignoredURLs;
  }

  public static HashSet<String> getIgnoredActivities() {
    return ignoredActivities;
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

  public static void ignoreActivities(String[] activities) {
    if (activities != null) {
      for (String activity : activities) {
        if (activity != null) {
          RaygunSettings.ignoredActivities.add(activity);
        }
      }
    }
  }
}
