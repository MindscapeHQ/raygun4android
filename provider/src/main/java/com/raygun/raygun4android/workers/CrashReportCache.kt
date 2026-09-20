package com.raygun.raygun4android.workers

import android.content.Context
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.w
import com.raygun.raygun4android.utils.RaygunFileFilter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID

internal object CrashReportCache {
    private const val DIRECTORY_NAME = "raygun-crash-reports"
    private const val TEMPORARY_SUFFIX = ".tmp"
    private const val PROCESSED_SUFFIX = ".processed"
    private const val LOCK_FILE_NAME = ".lock"

    @Synchronized
    fun store(
        context: Context,
        message: String,
    ): File? {
        if (Thread.currentThread().isInterrupted) {
            return null
        }

        return withCacheLock(context, null) { directory ->
            val cachedReports = filesLocked(context, directory)
            if (cachedReports.size >= RaygunSettings.maxReportsStoredOnDevice) {
                w("Maximum stored reports reached. Discarding message.")
                return null
            }

            val fileName = UUID.randomUUID().toString().replace("-", "")
            val temporaryFile = File(directory, ".$fileName$TEMPORARY_SUFFIX")
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

            cachedFile
        }
    }

    @Synchronized
    fun files(context: Context): Array<File> = withCacheLock(context, emptyArray()) { directory -> filesLocked(context, directory) }

    private fun filesLocked(
        context: Context,
        directory: File,
    ): Array<File> {
        cleanUpIncompleteFiles(directory)
        cleanUpProcessedFiles(context.cacheDir)
        cleanUpProcessedFiles(context.filesDir)
        val persistentFiles = directory.listFiles(RaygunFileFilter()) ?: emptyArray()
        val legacyCacheFiles = context.cacheDir.listFiles(RaygunFileFilter()) ?: emptyArray()
        return persistentFiles + legacyCacheFiles
    }

    fun readPersistent(file: File): String = file.readText(Charsets.UTF_8)

    @Synchronized
    fun markProcessed(
        context: Context,
        file: File,
    ): Boolean =
        withCacheLock(context, false) {
            if (!file.exists()) {
                true
            } else {
                val processedFile = File(file.parentFile, ".${file.name}$PROCESSED_SUFFIX")
                if (!file.renameTo(processedFile)) {
                    if (file.delete()) {
                        w("Couldn't rename processed crash report; deleted source (${file.name})")
                        true
                    } else {
                        e("Failed to mark cached crash report as processed: ${file.name}")
                        false
                    }
                } else {
                    if (!processedFile.delete()) {
                        w("Couldn't delete processed crash report (${processedFile.name})")
                    }
                    true
                }
            }
        }

    @Synchronized
    fun clear(context: Context) {
        withCacheLock(context, Unit) { directory ->
            filesLocked(context, directory).forEach(::delete)
            directory.listFiles { file -> isIncomplete(file) }?.forEach(::delete)
        }
    }

    internal fun persistentDirectory(context: Context): File = File(context.noBackupFilesDir, DIRECTORY_NAME)

    private fun cleanUpIncompleteFiles(directory: File) {
        directory
            .listFiles { file ->
                file.name.endsWith(PROCESSED_SUFFIX) || file.name.endsWith(TEMPORARY_SUFFIX)
            }?.forEach(::delete)
    }

    private fun cleanUpProcessedFiles(directory: File) {
        directory
            .listFiles { file ->
                file.name.startsWith(".") &&
                    file.name.endsWith(".${RaygunSettings.DEFAULT_FILE_EXTENSION}$PROCESSED_SUFFIX")
            }?.forEach(::delete)
    }

    private fun isIncomplete(file: File): Boolean = file.name.endsWith(TEMPORARY_SUFFIX) || file.name.endsWith(PROCESSED_SUFFIX)

    private inline fun <T> withCacheLock(
        context: Context,
        fallback: T,
        action: (File) -> T,
    ): T {
        val directory = persistentDirectory(context)
        if (!directory.exists() && !directory.mkdirs()) {
            e("Error creating crash report storage directory")
            return fallback
        }

        return try {
            RandomAccessFile(File(directory, LOCK_FILE_NAME), "rw").channel.use { channel ->
                channel.lock().use { action(directory) }
            }
        } catch (exception: IOException) {
            e("Error locking crash report storage: " + exception.message)
            fallback
        }
    }

    private fun delete(file: File) {
        if (!file.delete()) {
            w("Couldn't delete cached report (${file.name})")
        }
    }
}
