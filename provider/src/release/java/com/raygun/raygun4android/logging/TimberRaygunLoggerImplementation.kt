package com.raygun.raygun4android.logging

import timber.log.Timber

object TimberRaygunLoggerImplementation : TimberRaygunLogger {
    override fun init() {
        if (Timber.treeCount == 0) {
            Timber.plant(TimberRaygunReleaseTree())
        }
    }
}
