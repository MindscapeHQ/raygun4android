package com.raygun.raygun4android.workers

import android.content.Context
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.w
import com.raygun.raygun4android.utils.RaygunFileFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Durable spool of crash reports awaiting delivery.
 *
 * Reports are written to a temporary file in the cache directory and renamed into the spool, so a
 * report is either absent or complete and no directory-wide lock is needed. The spool holds at most
 * [RaygunSettings.maxReportsStoredOnDevice] reports, evicting the oldest first. The limit is soft:
 * concurrent writers, in one process or several, can each exceed it by one until the next write
 * trims the spool.
 */
internal object CrashReportCache {
    private const val DIRECTORY_NAME = "raygun-crash-reports"
    private const val TEMPORARY_SUFFIX = ".tmp"

    fun store(
        context: Context,
        message: String,
    ): File? {
        val directory = persistentDirectory(context)
        if (!directory.mkdirs() && !directory.isDirectory) {
            e("Error creating crash report storage directory")
            return null
        }

        val fileName = UUID.randomUUID().toString().replace("-", "")
        val temporaryFile = File(context.cacheDir, ".$fileName$TEMPORARY_SUFFIX")
        val cachedFile = File(directory, "$fileName.${RaygunSettings.DEFAULT_FILE_EXTENSION}")

        try {
            FileOutputStream(temporaryFile).use { output ->
                output.write(message.toByteArray(Charsets.UTF_8))
            }

            if (!temporaryFile.renameTo(cachedFile)) {
                e("Error moving cached crash report into place")
                temporaryFile.delete()
                return null
            }
        } catch (exception: IOException) {
            e("Error writing message to filesystem: " + exception.message)
            temporaryFile.delete()
            return null
        }

        trim(context, RaygunSettings.maxReportsStoredOnDevice)

        return cachedFile
    }

    fun files(context: Context): Array<File> {
        val persistentFiles =
            persistentDirectory(context).listFiles(RaygunFileFilter()) ?: emptyArray()
        val legacyCacheFiles = context.cacheDir.listFiles(RaygunFileFilter()) ?: emptyArray()
        return persistentFiles + legacyCacheFiles
    }

    fun readPersistent(file: File): String = file.readText(Charsets.UTF_8)

    fun remove(file: File) {
        if (!file.delete() && file.exists()) {
            e("Failed to remove cached crash report: ${file.name}")
        }
    }

    /**
     * Deletes the oldest cached reports until at most [maximum] remain
     *
     * @param context The Android context
     * @param maximum The number of reports to keep
     */
    fun trim(
        context: Context,
        maximum: Int,
    ) {
        val reports =
            files(context)
                .sortedWith(compareBy<File>(File::lastModified).thenBy(File::getAbsolutePath))
        val excess = reports.size - maximum.coerceAtLeast(0)
        if (excess > 0) {
            w("Maximum stored reports reached. Removing $excess oldest report(s).")
            reports.take(excess).forEach(::remove)
        }
    }

    fun clear(context: Context) {
        files(context).forEach(::remove)
    }

    internal fun persistentDirectory(context: Context): File = File(context.noBackupFilesDir, DIRECTORY_NAME)
}
