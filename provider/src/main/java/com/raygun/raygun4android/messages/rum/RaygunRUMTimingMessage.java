package com.raygun.raygun4android.messages.rum;

public class RaygunRUMTimingMessage {
    private String type;
    private long duration;

    public static class Builder {

        private String type;
        private long duration;

        public Builder(String type) {
            this.type = type;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public RaygunRUMTimingMessage build() {
            return new RaygunRUMTimingMessage(this);
        }
    }

    private RaygunRUMTimingMessage(Builder builder) {
        type = builder.type;
        duration = builder.duration;
    }
}