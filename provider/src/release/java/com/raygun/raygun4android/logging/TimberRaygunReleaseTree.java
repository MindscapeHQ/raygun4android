package com.raygun.raygun4android.logging;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import timber.log.Timber;

class TimberRaygunReleaseTree extends Timber.Tree {

    static private final int MAX_LOG_LENGTH = 4000;

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        if (priority == Log.ERROR || priority == Log.WARN) {

            if (message.length() <  MAX_LOG_LENGTH) {
                Log.println(priority, tag, message);
                return;
            }

            // Split by line, then ensure each line can fit into Log's maximum length.
            for (int i = 0, length = message.length(); i < length; i++) {
                int newline = message.indexOf('\n', i);
                newline = newline != -1 ? newline : length;
                do {
                    int end = Math.min(newline, i + MAX_LOG_LENGTH);
                    String part = message.substring(i, end);
                    Log.println(priority, tag, part);
                    i = end;
                } while (i < newline);
            }
        }
    }
}
