package com.raygun.raygun4androidsample.sample

import android.util.Log
import com.raygun.raygun4android.CrashReportingOnBeforeSend
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage

internal class SampleOnBeforeSend : CrashReportingOnBeforeSend {
    override fun onBeforeSend(message: RaygunMessage): RaygunMessage {
        Log.i("Raygun4Android-Sample", "In SampleOnBeforeSend - About to post to Raygun, returning the payload as is...")
        return message
    }
}
