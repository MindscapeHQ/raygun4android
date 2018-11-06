package main.java.com.mindscapehq.android.raygun4android.messages;

public class RaygunPulseData {
  private String name;
  private RaygunPulseTimingMessage timing;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public RaygunPulseTimingMessage getTiming() {
    return timing;
  }

  public void setTiming(RaygunPulseTimingMessage timing) {
    this.timing = timing;
  }
}
