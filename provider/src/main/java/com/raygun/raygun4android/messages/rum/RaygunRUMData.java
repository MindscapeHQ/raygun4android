package com.raygun.raygun4android.messages.rum;

public class RaygunRUMData {
    @SuppressWarnings("FieldCanBeLocal")
    private final String name;

    @SuppressWarnings("FieldCanBeLocal")
    private final RaygunRUMTimingMessage timing;

    public static class Builder {

        private final String name;
        private RaygunRUMTimingMessage timing;

        public Builder(String name) {
            this.name = name;
        }

        public Builder timing(RaygunRUMTimingMessage timing) {
            this.timing = timing;
            return this;
        }

        public RaygunRUMData build() {
            return new RaygunRUMData(this);
        }
    }

    private RaygunRUMData(Builder builder) {
        name = builder.name;
        timing = builder.timing;
    }
}
