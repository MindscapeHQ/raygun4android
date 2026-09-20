package com.raygun.raygun4android.workers

import android.content.Context
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.SerializedMessage
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.w
import com.raygun.raygun4android.utils.RaygunFileFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.UUID

internal object CrashReportCache {
    @Synchronized
    fun store(
        context: Context,
        message: String,
    ): Boolean {
        val cachedReports = context.cacheDir.listFiles(RaygunFileFilter()) ?: emptyArray()
        if (cachedReports.size >= RaygunSettings.maxReportsStoredOnDevice) {
            w("Maximum stored reports reached. Discarding message.")
            return false
        }

        val fileName = UUID.randomUUID().toString().replace("-", "")
        val temporaryFile = File(context.cacheDir, ".$fileName.tmp")
        val cachedFile =
            File(context.cacheDir, "$fileName.${RaygunSettings.DEFAULT_FILE_EXTENSION}")

        try {
            ObjectOutputStream(FileOutputStream(temporaryFile)).use { output ->
                output.writeObject(SerializedMessage(message))
            }

            if (!temporaryFile.renameTo(cachedFile)) {
                e("Error moving cached crash report into place")
                temporaryFile.delete()
                return false
            }
        } catch (exception: IOException) {
            e("Error writing message to filesystem: " + exception.message)
            temporaryFile.delete()
            return false
        }

        return true
    }
}
