package com.raygun.raygun4android.messages.rum;

public class RaygunPulseMessage {
  private RaygunPulseDataMessage[] eventData;

  public RaygunPulseDataMessage[] getEventData() {
    return eventData;
  }

  public void setEventData(RaygunPulseDataMessage[] eventData) {
    this.eventData = eventData;
  }
}