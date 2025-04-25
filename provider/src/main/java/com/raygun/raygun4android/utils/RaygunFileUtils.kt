package com.raygun.raygun4android.utils

import android.content.Context
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.w
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
        synchronized(this) {
            val fileList = context.cacheDir.listFiles(RaygunFileFilter())
            if (fileList != null) {
                for (f in fileList) {
                    if (
                        getExtension(f.name)
                            .equals(RaygunSettings.DEFAULT_FILE_EXTENSION, ignoreCase = true)
                    ) {
                        if (!f.delete()) {
                            w("Couldn't delete cached report (" + f.name + ")")
                        }
                    }
                }
            } else {
                e(
                    "Error in handling cached message from filesystem - could not get a list of" +
                        " files from cache dir",
                )
            }
        }
    }
}
