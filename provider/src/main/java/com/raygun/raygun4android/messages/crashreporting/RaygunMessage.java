package com.raygun.raygun4android.messages.crashreporting;

import android.os.Build;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.TimeZone;

public class RaygunMessage {
    private String occurredOn;
    private RaygunMessageDetails details;

    public RaygunMessage() {
        details = new RaygunMessageDetails();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime utcDateTime = LocalDateTime.now(ZoneId.of("UTC"));
            occurredOn = utcDateTime.toString();
        } else {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            occurredOn = df.format(Calendar.getInstance().getTime());
        }
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

    public void setOccurredOn(String occurredOn) {
        this.occurredOn = occurredOn;
    }
}
