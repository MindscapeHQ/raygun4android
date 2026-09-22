package com.raygun.raygun4android.utils

import android.content.Context
import com.raygun.raygun4android.workers.CrashReportCache
import kotlin.math.max

object RaygunFileUtils {
    fun getExtension(filename: String?): String? {
        if (filename == null) {
            return null
        }
        val separator =
            max(filename.lastIndexOf('/').toDouble(), filename.lastIndexOf('\\').toDouble()).toInt()
        val dotPos = filename.lastIndexOf(".")
        val index = if (separator > dotPos) -1 else dotPos
        return if (index == -1) {
            ""
        } else {
            filename.substring(index + 1)
        }
    }

    @JvmStatic
    fun clearCachedReports(context: Context) {
        CrashReportCache.clear(context)
    }
}
