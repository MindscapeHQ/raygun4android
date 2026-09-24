package com.raygun.raygun4android.workers

/**
 * A stored crash report with the API key and endpoint it was created for.
 *
 * Reports stored by earlier SDK versions hold only the payload, so their API key and endpoint are
 * null.
 */
internal data class CrashReportStoreEntry(
    val apiKey: String?,
    val endpoint: String?,
    val messagePayload: String,
)
