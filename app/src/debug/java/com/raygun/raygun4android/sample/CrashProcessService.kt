package com.raygun.raygun4android.sample

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.workers.CrashReportingWorkerHelper

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

open class CapacityProcessService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        val maximumReports = intent?.getIntExtra(EXTRA_MAXIMUM_REPORTS, -1) ?: -1
        if (maximumReports >= 0) {
            RaygunClient.init(this, null, "1.0.0")
            RaygunClient.setMaxReportsStoredOnDevice(maximumReports)
        }
        val writeAt = intent?.getLongExtra(EXTRA_WRITE_AT_ELAPSED_REALTIME, -1L) ?: -1L
        Thread {
            SystemClock.sleep((writeAt - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            CrashReportingWorkerHelper.enqueueCrashReport(this, "{}", null)
            Process.killProcess(Process.myPid())
        }.start()
        return Binder()
    }

    companion object {
        const val EXTRA_MAXIMUM_REPORTS = "maximumReports"
        const val EXTRA_WRITE_AT_ELAPSED_REALTIME = "writeAtElapsedRealtime"
    }
}

class SecondCapacityProcessService : CapacityProcessService()
