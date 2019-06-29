package com.raygun.raygun4android.messages.crashreporting;

import java.util.Map;
import java.util.WeakHashMap;

public class RaygunBreadcrumbMessage {
    private String message;
    private String category;
    private int level;
    private String type = "Manual";
    private Map<String, Object> customData;
    private Long timestamp = System.currentTimeMillis();
    private String className;
    private String methodName;
    private Integer lineNumber;

    public static class Builder {
        private String message;
        private String category;
        private int level = RaygunBreadcrumbLevel.INFO.ordinal();
        private Map<String, Object> customData = new WeakHashMap<>();
        private String className;
        private String methodName;
        private Integer lineNumber;

        public Builder(String message) {
            this.message = message;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder level(RaygunBreadcrumbLevel level) {
            this.level = level.ordinal();
            return this;
        }

        public Builder customData(Map<String, Object> customData) {
            this.customData = customData;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder lineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public RaygunBreadcrumbMessage build() {
            return new RaygunBreadcrumbMessage(this);
        }
    }

    public String getMessage() {
        return message;
    }

    public String getCategory() {
        return category;
    }

    public RaygunBreadcrumbLevel getLevel() {
        return RaygunBreadcrumbLevel.values()[level];
    }

    public Map<String, Object> getCustomData() {
        return customData;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    private RaygunBreadcrumbMessage(Builder builder) {
        message = builder.message;
        category = builder.category;
        level = builder.level;
        customData = builder.customData;
        className = builder.className;
        methodName = builder.methodName;
        lineNumber = builder.lineNumber;
    }
}
