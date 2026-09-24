package com.raygun.raygun4android.workers

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
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
 *
 * Each report is stored with the API key and endpoint it was created for, so it is delivered there
 * even when a process that has not configured the client runs the work.
 */
internal object CrashReportCache {
    private const val DIRECTORY_NAME = "raygun-crash-reports"
    private const val TEMPORARY_SUFFIX = ".tmp"
    private const val API_KEY_FIELD = "apiKey"
    private const val ENDPOINT_FIELD = "endpoint"
    private const val MESSAGE_PAYLOAD_FIELD = "messagePayload"

    fun store(
        context: Context,
        message: String,
        apiKey: String? = null,
        endpoint: String = RaygunSettings.crashReportingEndpoint,
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
                val entry = serialize(CrashReportStoreEntry(apiKey, endpoint, message))
                output.write(entry.toByteArray(Charsets.UTF_8))
            }

            trim(context, RaygunSettings.maxReportsStoredOnDevice - 1)

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

        return cachedFile
    }

    fun files(context: Context): Array<File> {
        val persistentFiles =
            persistentDirectory(context).listFiles(RaygunFileFilter()) ?: emptyArray()
        val legacyCacheFiles = context.cacheDir.listFiles(RaygunFileFilter()) ?: emptyArray()
        return persistentFiles + legacyCacheFiles
    }

    fun readPersistent(file: File): CrashReportStoreEntry {
        val contents = file.readText(Charsets.UTF_8)
        return deserialize(contents) ?: CrashReportStoreEntry(null, null, contents)
    }

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
        val reports = files(context).sortedBy(File::lastModified)
        val excess = reports.size - maximum.coerceAtLeast(0)
        if (excess > 0) {
            w("Maximum stored reports reached. Removing $excess oldest report(s).")
            reports.take(excess).forEach(::remove)
        }
    }

    fun clear(context: Context) {
        files(context).forEach(::remove)
    }

    private fun serialize(entry: CrashReportStoreEntry): String =
        JsonObject()
            .apply {
                addProperty(API_KEY_FIELD, entry.apiKey)
                addProperty(ENDPOINT_FIELD, entry.endpoint)
                addProperty(MESSAGE_PAYLOAD_FIELD, entry.messagePayload)
            }.toString()

    /**
     * Reads a stored entry, or returns null for a report stored by an earlier SDK version, which
     * holds only the payload
     *
     * @param contents The contents of the stored report
     * @return CrashReportStoreEntry? - the entry, or null if the contents are not an entry
     */
    private fun deserialize(contents: String): CrashReportStoreEntry? {
        val json =
            try {
                JsonParser.parseString(contents)
            } catch (exception: JsonParseException) {
                return null
            }
        if (!json.isJsonObject) {
            return null
        }

        val entry = json.asJsonObject
        val messagePayload = entry.getString(MESSAGE_PAYLOAD_FIELD) ?: return null
        return CrashReportStoreEntry(
            entry.getString(API_KEY_FIELD),
            entry.getString(ENDPOINT_FIELD),
            messagePayload,
        )
    }

    private fun JsonObject.getString(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    internal fun persistentDirectory(context: Context): File = File(context.noBackupFilesDir, DIRECTORY_NAME)
}
