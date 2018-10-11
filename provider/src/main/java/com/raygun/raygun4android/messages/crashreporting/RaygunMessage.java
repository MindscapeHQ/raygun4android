package com.raygun.raygun4android.messages.crashreporting;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class RaygunMessage {
    private String occurredOn;
    private RaygunMessageDetails details;

    public RaygunMessage() {
        details = new RaygunMessageDetails();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        occurredOn = df.format(Calendar.getInstance().getTime());
    }

    public RaygunMessageDetails getDetails() {
        return details;
    }

    public void setDetails(RaygunMessageDetails details) {
        this.details = details;
    }

    public String getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(String _occurredOn) {
        this.occurredOn = _occurredOn;
    }
}
