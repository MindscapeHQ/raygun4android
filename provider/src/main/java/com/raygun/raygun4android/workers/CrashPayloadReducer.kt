package com.raygun.raygun4android.workers

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser

/** Reduces Crash Reporting payloads without changing the cached original. */
internal object CrashPayloadReducer {
    // The API documents a 128KB maximum. This leaves a margin whether KB is enforced as 1,000 or
    // 1,024 bytes.
    internal const val MAX_PAYLOAD_BYTES = 120 * 1024
    internal const val TRUNCATION_TAG = "raygun:payload-truncated"

    private const val MAX_STACK_FRAMES = 100
    private const val KEEP_TOP_FRAMES = 20
    private const val KEEP_BOTTOM_FRAMES = 20
    private const val AGGRESSIVE_KEEP_TOP_FRAMES = 5
    private const val AGGRESSIVE_KEEP_BOTTOM_FRAMES = 5
    private const val MAX_ERROR_MESSAGE_BYTES = 2 * 1024
    private const val MAX_ERROR_CLASS_BYTES = 512
    private const val MAX_FRAME_STRING_BYTES = 512
    private const val MAX_BREADCRUMB_STRING_BYTES = 1024
    private const val MAX_OCCURRED_ON_BYTES = 128
    private const val MAX_INNER_ERROR_DEPTH = 64
    private const val TRUNCATED_TEXT = "…"

    /**
     * Returns [payload] unchanged when it is already within [maxBytes]. Otherwise, optional data is
     * reduced in usefulness order and a bounded minimal report is used as a last resort.
     */
    fun reduceToLimit(
        payload: String,
        maxBytes: Int = MAX_PAYLOAD_BYTES,
    ): String {
        if (payload.byteSize() <= maxBytes) {
            return payload
        }

        val report = parseObject(payload) ?: return payload
        val minimalSource = report.deepCopy()
        addTruncationMarker(report)

        trimStackTraces(report, MAX_STACK_FRAMES, KEEP_TOP_FRAMES, KEEP_BOTTOM_FRAMES)
        serializedWithinLimit(report, maxBytes)?.let {
            return it
        }

        removeLargestCustomDataEntries(report, maxBytes)?.let {
            return it
        }
        trimOldestBreadcrumbs(report, maxBytes)?.let {
            return it
        }

        replaceTagsWithTruncationMarker(report)
        serializedWithinLimit(report, maxBytes)?.let {
            return it
        }

        trimStackTraces(
            report,
            AGGRESSIVE_KEEP_TOP_FRAMES + AGGRESSIVE_KEEP_BOTTOM_FRAMES,
            AGGRESSIVE_KEEP_TOP_FRAMES,
            AGGRESSIVE_KEEP_BOTTOM_FRAMES,
        )
        serializedWithinLimit(report, maxBytes)?.let {
            return it
        }

        truncateErrorStrings(report)
        serializedWithinLimit(report, maxBytes)?.let {
            return it
        }

        truncateBreadcrumbStrings(report)
        serializedWithinLimit(report, maxBytes)?.let {
            return it
        }

        val minimal = minimalPayload(minimalSource) ?: return payload
        return minimal.takeIf { it.byteSize() <= maxBytes } ?: payload
    }

    /** Creates a small valid report for one final delivery attempt after an HTTP 413 response. */
    fun minimalPayload(payload: String): String? = parseObject(payload)?.let(::minimalPayload)

    private fun minimalPayload(report: JsonObject): String? {
        val occurredOn =
            report.string("occurredOn")?.let { truncateUtf8(it, MAX_OCCURRED_ON_BYTES) }
                ?: return null
        val sourceError = report.obj("details")?.obj("error") ?: return null

        val error = JsonObject()
        error.addProperty(
            "message",
            truncateUtf8(
                sourceError.string("message") ?: "Crash report payload reduced for delivery",
                MAX_ERROR_MESSAGE_BYTES,
            ),
        )
        error.addProperty(
            "className",
            truncateUtf8(sourceError.string("className") ?: "UnknownError", MAX_ERROR_CLASS_BYTES),
        )
        error.add("stackTrace", minimalStackTrace(sourceError))

        val details = JsonObject()
        details.add("error", error)
        details.add(
            "tags",
            JsonArray().apply {
                add(TRUNCATION_TAG)
            },
        )

        return JsonObject()
            .apply {
                addProperty("occurredOn", occurredOn)
                add("details", details)
            }.toString()
    }

    private fun minimalStackTrace(sourceError: JsonObject): JsonArray {
        val sourceFrame = firstFrame(sourceError)
        val frame = JsonObject()
        frame.addProperty("lineNumber", sourceFrame?.int("lineNumber") ?: 0)
        frame.addProperty(
            "className",
            truncateUtf8(
                sourceFrame?.string("className")
                    ?: sourceError.string("className")
                    ?: "UnknownError",
                MAX_FRAME_STRING_BYTES,
            ),
        )
        frame.addProperty(
            "fileName",
            truncateUtf8(sourceFrame?.string("fileName") ?: "", MAX_FRAME_STRING_BYTES),
        )
        frame.addProperty(
            "methodName",
            truncateUtf8(
                sourceFrame?.string("methodName") ?: "payloadTruncated",
                MAX_FRAME_STRING_BYTES,
            ),
        )
        return JsonArray().apply { add(frame) }
    }

    private fun firstFrame(sourceError: JsonObject): JsonObject? {
        var error: JsonObject? = sourceError
        var depth = 0
        while (error != null && depth < MAX_INNER_ERROR_DEPTH) {
            val frame = error.array("stackTrace")?.firstOrNull { it.isJsonObject }?.asJsonObject
            if (frame != null) {
                return frame
            }
            error = error.obj("innerError")
            depth++
        }
        return null
    }

    private fun addTruncationMarker(report: JsonObject) {
        val details = report.obj("details") ?: return
        val tags = details.array("tags") ?: JsonArray().also { details.add("tags", it) }
        if (tags.none { it.isJsonPrimitive && it.asString == TRUNCATION_TAG }) {
            tags.add(TRUNCATION_TAG)
        }
    }

    private fun replaceTagsWithTruncationMarker(report: JsonObject) {
        report
            .obj("details")
            ?.add(
                "tags",
                JsonArray().apply {
                    add(TRUNCATION_TAG)
                },
            )
    }

    private fun trimStackTraces(
        report: JsonObject,
        maximumFrames: Int,
        keepTop: Int,
        keepBottom: Int,
    ) {
        forEachError(report) { error ->
            val frames = error.array("stackTrace") ?: return@forEachError
            if (frames.size() <= maximumFrames) {
                return@forEachError
            }

            val removed = frames.size() - keepTop - keepBottom
            val trimmed = JsonArray()
            repeat(keepTop) { index -> trimmed.add(frames[index]) }
            trimmed.add(removedFramesMarker(removed))
            repeat(keepBottom) { offset ->
                trimmed.add(frames[frames.size() - keepBottom + offset])
            }
            error.add("stackTrace", trimmed)
        }
    }

    private fun removedFramesMarker(removed: Int): JsonObject =
        JsonObject().apply {
            addProperty("lineNumber", 0)
            addProperty("className", "Raygun4Android")
            addProperty("fileName", "")
            addProperty("methodName", "$removed frames removed from middle of stack trace")
        }

    private fun removeLargestCustomDataEntries(
        report: JsonObject,
        maxBytes: Int,
    ): String? {
        val details = report.obj("details") ?: return null
        val customDataElement = details.get("userCustomData") ?: return null
        if (!customDataElement.isJsonObject) {
            details.remove("userCustomData")
            return serializedWithinLimit(report, maxBytes)
        }

        val customData = customDataElement.asJsonObject
        var currentBytes = report.toString().byteSize()
        var remainingEntries = customData.size()
        val keysByDescendingSize =
            customData.entrySet().sortedByDescending { (key, value) ->
                customDataEntrySize(key, value)
            }
        for ((key, value) in keysByDescendingSize) {
            currentBytes -= customDataEntrySize(key, value)
            if (remainingEntries > 1) {
                currentBytes--
            }
            remainingEntries--
            customData.remove(key)
            if (currentBytes <= maxBytes) {
                serializedWithinLimit(report, maxBytes)?.let {
                    return it
                }
            }
        }
        return null
    }

    private fun customDataEntrySize(
        key: String,
        value: JsonElement,
    ): Int = JsonObject().apply { add(key, value) }.toString().byteSize() - 2

    private fun trimOldestBreadcrumbs(
        report: JsonObject,
        maxBytes: Int,
    ): String? {
        val details = report.obj("details") ?: return null
        val breadcrumbElement = details.get("breadcrumbs") ?: return null
        if (!breadcrumbElement.isJsonArray) {
            details.remove("breadcrumbs")
            return serializedWithinLimit(report, maxBytes)
        }

        val breadcrumbs = breadcrumbElement.asJsonArray.toList()
        var minimum = 0
        var maximum = breadcrumbs.size
        var bestPayload: String? = null
        var bestCount = -1
        while (minimum <= maximum) {
            val count = minimum + (maximum - minimum) / 2
            details.add("breadcrumbs", breadcrumbs.takeLast(count).toJsonArray())
            val candidate = report.toString()
            if (candidate.byteSize() <= maxBytes) {
                bestPayload = candidate
                bestCount = count
                minimum = count + 1
            } else {
                maximum = count - 1
            }
        }

        if (bestCount >= 0) {
            details.add("breadcrumbs", breadcrumbs.takeLast(bestCount).toJsonArray())
            return bestPayload
        }

        details.add("breadcrumbs", JsonArray())
        return null
    }

    private fun truncateErrorStrings(report: JsonObject) {
        forEachError(report) { error ->
            error.truncateString("message", MAX_ERROR_MESSAGE_BYTES)
            error.truncateString("className", MAX_ERROR_CLASS_BYTES)
            error.array("stackTrace")?.forEach { frame ->
                if (frame.isJsonObject) {
                    frame.asJsonObject.truncateString("className", MAX_FRAME_STRING_BYTES)
                    frame.asJsonObject.truncateString("fileName", MAX_FRAME_STRING_BYTES)
                    frame.asJsonObject.truncateString("methodName", MAX_FRAME_STRING_BYTES)
                }
            }
        }
    }

    private fun truncateBreadcrumbStrings(report: JsonObject) {
        report.obj("details")?.array("breadcrumbs")?.forEach { breadcrumb ->
            truncateStrings(breadcrumb, MAX_BREADCRUMB_STRING_BYTES)
        }
    }

    private fun truncateStrings(
        element: JsonElement,
        maxBytes: Int,
    ) {
        when {
            element.isJsonObject -> {
                val jsonObject = element.asJsonObject
                jsonObject.entrySet().toList().forEach { (key, value) ->
                    if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        jsonObject.addProperty(key, truncateUtf8(value.asString, maxBytes))
                    } else {
                        truncateStrings(value, maxBytes)
                    }
                }
            }

            element.isJsonArray -> {
                element.asJsonArray.forEach { truncateStrings(it, maxBytes) }
            }
        }
    }

    private fun JsonObject.truncateString(
        name: String,
        maxBytes: Int,
    ) {
        string(name)?.let { addProperty(name, truncateUtf8(it, maxBytes)) }
    }

    private fun forEachError(
        report: JsonObject,
        block: (JsonObject) -> Unit,
    ) {
        var error = report.obj("details")?.obj("error")
        var depth = 0
        while (error != null && depth < MAX_INNER_ERROR_DEPTH) {
            block(error)
            error = error.obj("innerError")
            depth++
        }
    }

    private fun truncateUtf8(
        value: String,
        maxBytes: Int,
    ): String {
        if (value.byteSize() <= maxBytes) {
            return value
        }

        val markerBytes = TRUNCATED_TEXT.byteSize()
        val contentBudget = (maxBytes - markerBytes).coerceAtLeast(0)
        val result = StringBuilder()
        var usedBytes = 0
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            val codePointText = String(Character.toChars(codePoint))
            val codePointBytes = codePointText.byteSize()
            if (usedBytes + codePointBytes > contentBudget) {
                break
            }
            result.append(codePointText)
            usedBytes += codePointBytes
            index += Character.charCount(codePoint)
        }
        if (markerBytes <= maxBytes) {
            result.append(TRUNCATED_TEXT)
        }
        return result.toString()
    }

    private fun parseObject(payload: String): JsonObject? =
        try {
            JsonParser.parseString(payload).takeIf { it.isJsonObject }?.asJsonObject
        } catch (exception: JsonParseException) {
            null
        } catch (exception: IllegalStateException) {
            null
        }

    private fun serializedWithinLimit(
        report: JsonObject,
        maxBytes: Int,
    ): String? = report.toString().takeIf { it.byteSize() <= maxBytes }

    private fun JsonObject.obj(name: String): JsonObject? = get(name)?.takeIf(JsonElement::isJsonObject)?.asJsonObject

    private fun JsonObject.array(name: String): JsonArray? = get(name)?.takeIf(JsonElement::isJsonArray)?.asJsonArray

    private fun JsonObject.string(name: String): String? = get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.int(name: String): Int? =
        try {
            get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
        } catch (exception: NumberFormatException) {
            null
        }

    private fun List<JsonElement>.toJsonArray(): JsonArray = JsonArray().also { array -> forEach(array::add) }

    private fun String.byteSize(): Int = toByteArray(Charsets.UTF_8).size
}
