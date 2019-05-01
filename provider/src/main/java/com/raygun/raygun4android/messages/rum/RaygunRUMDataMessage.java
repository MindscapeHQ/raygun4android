package com.raygun.raygun4android.messages.rum;

import com.raygun.raygun4android.messages.shared.RaygunUserInfo;

@SuppressWarnings("FieldCanBeLocal")
public class RaygunRUMDataMessage {
    private String sessionId;
    private String timestamp;
    private String type;
    private RaygunUserInfo user;
    private String version;
    private String os;
    private String osVersion;
    private String platform;
    private String data;

    public static class Builder {

        private String sessionId;
        private String timestamp;
        private String type;
        private RaygunUserInfo user;
        private String version;
        private String os;
        private String osVersion;
        private String platform;
        private String data;

        public Builder(String type) {
            this.type = type;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder user(RaygunUserInfo user) {
            this.user = user;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder os(String os) {
            this.os = os;
            return this;
        }

        public Builder osVersion(String osVersion) {
            this.osVersion = osVersion;
            return this;
        }

        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder data(String data) {
            this.data = data;
            return this;
        }

        public RaygunRUMDataMessage build() {
            return new RaygunRUMDataMessage(this);
        }
    }

    private RaygunRUMDataMessage(Builder builder) {
        type = builder.type;
        sessionId = builder.sessionId;
        timestamp = builder.timestamp;
        user = builder.user;
        version = builder.version;
        os = builder.os;
        osVersion = builder.osVersion;
        platform = builder.platform;
        data = builder.data;
    }
}

