package main.java.com.mindscapehq.android.raygun4android;

public class RaygunSettings {

  private RaygunSettings() { }

  public synchronized static RaygunSettings getSettings()
  {
    if (settings == null) {
      settings = new RaygunSettings();
    }
    return settings;
  }

  private static RaygunSettings settings = null;

  private final String defaultApiEndpoint = "https://api.raygun.io/entries";
  private final String defaultPulseEndpoint = "https://api.raygun.io/events";

  private final IgnoredUrls ignoredUrls = new IgnoredUrls(defaultApiEndpoint);

  public String getApiEndpoint() {
    return defaultApiEndpoint;
  }

  public String getPulseEndpoint() {
    return defaultPulseEndpoint;
  }

 public HashSet<String> getIgnoredUrls() {
    return ignoredUrls;
  }

  public class IgnoredUrls
      extends HashSet<String>
  {
    public IgnoredUrls(String... defaultIgnoredUrls) {
      for (String url : defaultIgnoredUrls) {
        add(url);
      }
    }
  }
}
