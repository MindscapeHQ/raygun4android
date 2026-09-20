package com.raygun.raygun4android.sample

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import com.raygun.raygun4android.RaygunClient

class CrashProcessService : Service() {
    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, _ ->
            Process.killProcess(Process.myPid())
        }
        RaygunClient.init(this, "test-api-key", "1.0.0")
        RaygunClient.enableCrashReporting()

        Thread {
            throw IllegalStateException(CRASH_MESSAGE)
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CRASH_MESSAGE = "separate process crash"
    }
}
