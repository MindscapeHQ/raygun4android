package com.raygun.raygun4android.sample

import android.app.Service
import android.content.Intent
import android.os.Binder
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
    }

    override fun onBind(intent: Intent?): IBinder {
        Thread {
            Thread.sleep(CRASH_DELAY_MILLIS)
            throw IllegalStateException(CRASH_MESSAGE)
        }.start()
        return Binder()
    }

    companion object {
        const val CRASH_MESSAGE = "separate process crash"
        private const val CRASH_DELAY_MILLIS = 250L
    }
}
