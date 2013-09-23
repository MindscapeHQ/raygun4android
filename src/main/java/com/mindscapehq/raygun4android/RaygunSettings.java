package main.java.com.mindscapehq.raygun4android;

public class RaygunSettings {

  private RaygunSettings() { }

  public static RaygunSettings getSettings() { return new RaygunSettings(); }

  private final String defaultApiEndpoint = "https://api.raygun.io/entries";

  public String getApiEndpoint() {
    return defaultApiEndpoint;
  }
}
