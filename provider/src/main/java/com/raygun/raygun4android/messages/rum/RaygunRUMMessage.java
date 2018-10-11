package com.raygun.raygun4android.messages.rum;

public class RaygunRUMMessage {
    private RaygunRUMDataMessage[] eventData;

    public RaygunRUMDataMessage[] getEventData() {
        return eventData;
    }

    public void setEventData(RaygunRUMDataMessage[] eventData) {
        this.eventData = eventData;
    }
}
