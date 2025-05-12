package com.raygun.raygun4android.sample

import android.app.Application
import android.os.Build
import android.os.StrictMode

class RaygunApp : Application() {
    override fun onCreate() {
        // Enable strict mode for debugging on API 35
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 35) {
            enableStrictMode()
        }
        super.onCreate()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyFlashScreen()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy
                .Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build(),
        )
    }
}
