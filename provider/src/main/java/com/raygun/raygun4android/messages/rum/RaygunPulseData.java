package com.raygun.raygun4android.messages.rum;

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
