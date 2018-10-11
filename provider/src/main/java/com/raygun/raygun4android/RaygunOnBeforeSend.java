package com.raygun.raygun4android;

import com.raygun.raygun4android.messages.crashreporting.RaygunMessage;

public interface RaygunOnBeforeSend {
    RaygunMessage onBeforeSend(RaygunMessage message);
}