package main.java.com.mindscapehq.android.raygun4android.messages;

public class RaygunPulseTimingMessage {
    private String type;
    private double duration;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }
}
