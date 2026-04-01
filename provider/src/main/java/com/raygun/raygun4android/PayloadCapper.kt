package com.raygun.raygun4android

import com.raygun.raygun4android.logging.RaygunLogger
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbLevel
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunErrorMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunErrorStackTraceLineMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage

internal object PayloadCapper {
    private const val MAX_PAYLOAD_BYTES = 120 * 1024
    private const val MAX_BREADCRUMBS = 30
    private const val MAX_STACK_FRAMES = 100
    private const val KEEP_TOP_FRAMES = 20
    private const val KEEP_BOTTOM_FRAMES = 20

    fun capIfNeeded(
        message: RaygunMessage,
        serialize: (RaygunMessage) -> String,
    ): String {
        var json = serialize(message)
        if (json.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            return json
        }

        RaygunLogger.w(
            "Crash report payload exceeds ${MAX_PAYLOAD_BYTES / 1024}KB, capping breadcrumbs",
        )
        capBreadcrumbs(message)
        json = serialize(message)
        if (json.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) {
            return json
        }

        RaygunLogger.w(
            "Payload still exceeds ${MAX_PAYLOAD_BYTES / 1024}KB after breadcrumb capping, capping stack frames",
        )
        capStackFrames(message.details.error)
        json = serialize(message)

        if (json.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES) {
            RaygunLogger.e(
                "Payload still exceeds ${MAX_PAYLOAD_BYTES / 1024}KB after all capping (${json.toByteArray(
                    Charsets.UTF_8,
                ).size} bytes). Sending anyway, but the API may reject it.",
            )
        }

        return json
    }

    private fun capBreadcrumbs(message: RaygunMessage) {
        val breadcrumbs = message.details.breadcrumbs ?: return
        if (breadcrumbs.size <= MAX_BREADCRUMBS) return

        val removed = breadcrumbs.size - MAX_BREADCRUMBS
        val kept = breadcrumbs.takeLast(MAX_BREADCRUMBS)

        val note =
            RaygunBreadcrumbMessage
                .Builder(
                    "$removed earlier breadcrumbs removed to reduce payload size",
                ).category("Raygun4Android")
                .level(RaygunBreadcrumbLevel.WARNING)
                .build()

        message.details.breadcrumbs = listOf(note) + kept
    }

    private fun capStackFrames(error: RaygunErrorMessage?) {
        if (error == null) return

        val frames = error.stackTrace
        if (frames.size > MAX_STACK_FRAMES) {
            val removed = frames.size - (KEEP_TOP_FRAMES + KEEP_BOTTOM_FRAMES)
            val top = frames.take(KEEP_TOP_FRAMES)
            val bottom = frames.takeLast(KEEP_BOTTOM_FRAMES)

            val note =
                RaygunErrorStackTraceLineMessage(
                    lineNumber = 0,
                    className = "--- Raygun4Android ---",
                    fileName = "",
                    methodName = "$removed frames removed from middle of stack trace",
                )

            error.stackTrace = top + listOf(note) + bottom
        }

        capStackFrames(error.innerError)
    }
}
