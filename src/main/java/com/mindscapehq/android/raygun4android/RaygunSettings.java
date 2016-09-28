package main.java.com.mindscapehq.android.raygun4android;

import java.util.HashSet;

public class RaygunSettings {
  private RaygunSettings() {
  }

  private static final String defaultApiEndpoint = "https://api.raygun.io/entries";
  private static final String defaultPulseEndpoint = "https://api.raygun.io/events";
  private static IgnoredUrls ignoredUrls = new IgnoredUrls(defaultApiEndpoint, defaultPulseEndpoint);

  public static String getApiEndpoint() {
    return defaultApiEndpoint;
  }

  public static String getPulseEndpoint() {
    return defaultPulseEndpoint;
  }

  public static HashSet<String> getIgnoredUrls() {
    return ignoredUrls;
  }

  public static class IgnoredUrls extends HashSet<String> {
    public IgnoredUrls(String... defaultIgnoredUrls) {
      for (String url : defaultIgnoredUrls) {
        add(url);
      }
    }
  }
}
