package com.raygun.raygun4android.logging;

import timber.log.Timber;

public class TimberRaygunLoggerImplementation implements TimberRaygunLogger {

    public static void init() {
        if (Timber.treeCount() == 0) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
