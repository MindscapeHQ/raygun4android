package com.raygun.raygun4android

import android.app.Activity
import android.os.Bundle
import android.os.Process

class CrashProcessActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            Process.killProcess(Process.myPid())
        }
        RaygunClient.init(this, "test-api-key", "1.0.0")
        RaygunClient.enableCrashReporting()

        Thread {
            throw IllegalStateException(CRASH_MESSAGE)
        }.start()
    }

    companion object {
        const val CRASH_MESSAGE = "separate process crash"
    }
}
