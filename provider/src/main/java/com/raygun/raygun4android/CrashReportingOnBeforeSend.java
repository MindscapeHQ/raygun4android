package com.raygun.raygun4android;

import com.raygun.raygun4android.messages.crashreporting.RaygunMessage;

interface CrashReportingOnBeforeSend {
    RaygunMessage onBeforeSend(RaygunMessage message);
}