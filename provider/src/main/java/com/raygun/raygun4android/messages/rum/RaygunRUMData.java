package com.raygun.raygun4android.messages.rum;

public class RaygunRUMData {
    private String name;
    private RaygunRUMTimingMessage timing;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RaygunRUMTimingMessage getTiming() {
        return timing;
    }

    public void setTiming(RaygunRUMTimingMessage timing) {
        this.timing = timing;
    }
}
