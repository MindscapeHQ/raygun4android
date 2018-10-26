package main.java.com.mindscapehq.android.raygun4android.messages;

public class RaygunPulseMessage {
  private RaygunPulseDataMessage[] eventData;

  public RaygunPulseDataMessage[] getEventData() {
    return eventData;
  }

  public void setEventData(RaygunPulseDataMessage[] eventData) {
    this.eventData = eventData;
  }
}
