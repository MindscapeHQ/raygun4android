package com.raygun.raygun4android.logging

import android.util.Log
import timber.log.Timber
import kotlin.math.min

internal class TimberRaygunReleaseTree : Timber.Tree() {
    override fun log(
        priority: Int, tag: String?, message: String, t: Throwable?
    ) {
        if (priority == Log.ERROR || priority == Log.WARN) {
            if (message.length < MAX_LOG_LENGTH) {
                Log.println(priority, tag, message)
                return
            }

            // Split by line, then ensure each line can fit into Log's maximum length.
            var i = 0
            val length = message.length
            while (i < length) {
                var newline = message.indexOf('\n', i)
                newline = if (newline != -1) newline else length
                do {
                    val end = min(newline.toDouble(), (i + MAX_LOG_LENGTH).toDouble()).toInt()
                    val part = message.substring(i, end)
                    Log.println(priority, tag, part)
                    i = end
                } while (i < newline)
                i++
            }
        }
    }

    companion object {
        private const val MAX_LOG_LENGTH = 4000
    }
}
