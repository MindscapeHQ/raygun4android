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
    private const val MAX_GROUPING_KEY_CODE_POINTS = 100
    private const val MAX_VERSION_BYTES = 256
    private const val MAX_CLIENT_NAME_BYTES = 256
    private const val MAX_CLIENT_VERSION_BYTES = 256
    private const val MAX_CLIENT_URL_BYTES = 1024
    private const val MAX_INNER_ERROR_DEPTH = 64
    private const val BREADCRUMB_MARKER_CATEGORY = "Raygun4Android"
    private const val BREADCRUMB_MARKER_TYPE = "Manual"
    private const val BREADCRUMB_INFO_LEVEL = 1
    internal const val CUSTOM_DATA_MARKER_KEY = "raygun.payloadReduction"
    private const val TAGS_TRUNCATED_PREFIX = "TagsTruncated"
    private const val TRUNCATED_TEXT = "…"
    private val removedFramesMessage = Regex("^(\\d+) frames removed from middle of stack trace$")

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

        trimStackTraces(report, MAX_STACK_FRAMES, KEEP_TOP_FRAMES, KEEP_BOTTOM_FRAMES)
        serializedWithinLimit(report, maxBytes)?.let {
            return it
        }

        truncateBreadcrumbStrings(report)
        serializedWithinLimit(report, maxBytes)?.let {
            return it
        }

        trimOldestBreadcrumbs(report, maxBytes)?.let {
            return it
        }

        removeLargestCustomDataEntries(report, maxBytes)?.let {
            return it
        }

        truncateStackFrameStrings(report)
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

        replaceTagsWithRemovalMarker(report)
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
        val sourceDetails = report.obj("details") ?: return null
        val sourceError = sourceDetails.obj("error") ?: return null

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
        sourceDetails.string("groupingKey")?.let { groupingKey ->
            details.addProperty(
                "groupingKey",
                truncateCodePoints(groupingKey, MAX_GROUPING_KEY_CODE_POINTS),
            )
        }
        sourceDetails.string("version")?.let { version ->
            details.addProperty("version", truncateUtf8(version, MAX_VERSION_BYTES))
        }
        minimalClient(sourceDetails.obj("client"))?.let { client ->
            details.add("client", client)
        }
        minimalCustomDataMarker(sourceDetails.get("userCustomData"))?.let { customData ->
            details.add("userCustomData", customData)
        }
        minimalBreadcrumbMarker(sourceDetails.get("breadcrumbs"))?.let { breadcrumbs ->
            details.add("breadcrumbs", breadcrumbs)
        }
        removedValueCount(sourceDetails.get("tags"))
            .takeIf { it > 0 }
            ?.let { removedTags ->
                details.add(
                    "tags",
                    JsonArray().apply { add(tagsRemovalMarker(removedTags)) },
                )
            }

        return JsonObject()
            .apply {
                addProperty("occurredOn", occurredOn)
                add("details", details)
            }.toString()
    }

    private fun minimalClient(sourceClient: JsonObject?): JsonObject? {
        sourceClient ?: return null
        val client = JsonObject()
        sourceClient.string("name")?.let { name ->
            client.addProperty("name", truncateUtf8(name, MAX_CLIENT_NAME_BYTES))
        }
        sourceClient.string("version")?.let { version ->
            client.addProperty("version", truncateUtf8(version, MAX_CLIENT_VERSION_BYTES))
        }
        sourceClient.string("clientUrl")?.let { clientUrl ->
            client.addProperty("clientUrl", truncateUtf8(clientUrl, MAX_CLIENT_URL_BYTES))
        }
        return client.takeIf { it.size() > 0 }
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
        sourceFrame?.string("raw")?.let { raw ->
            frame.addProperty("raw", truncateUtf8(raw, MAX_FRAME_STRING_BYTES))
        }
        val originalFrameCount = stackFrameCount(sourceError)
        return JsonArray().apply {
            add(frame)
            if (originalFrameCount > 1) {
                add(removedFramesMarker(originalFrameCount - 1))
            }
        }
    }

    private fun stackFrameCount(sourceError: JsonObject): Long {
        var error: JsonObject? = sourceError
        var depth = 0
        var count = 0L
        while (error != null && depth < MAX_INNER_ERROR_DEPTH) {
            error.array("stackTrace")?.forEach { frame -> count += representedFrameCount(frame) }
            error = error.obj("innerError")
            depth++
        }
        return count
    }

    private fun minimalCustomDataMarker(sourceCustomData: JsonElement?): JsonObject? {
        sourceCustomData ?: return null
        val marker =
            if (sourceCustomData.isJsonObject) {
                val entriesRemoved = sourceCustomData.asJsonObject.size()
                if (entriesRemoved == 0) {
                    return null
                }
                customDataRemovalMarker(entriesRemoved = entriesRemoved)
            } else {
                customDataRemovalMarker(valueRemoved = true)
            }
        return JsonObject().apply { add(CUSTOM_DATA_MARKER_KEY, marker) }
    }

    private fun minimalBreadcrumbMarker(sourceBreadcrumbs: JsonElement?): JsonArray? {
        val breadcrumbs =
            sourceBreadcrumbs?.takeIf(JsonElement::isJsonArray)?.asJsonArray?.toList()
                ?: return null
        if (breadcrumbs.isEmpty()) {
            return null
        }
        return breadcrumbsWithRemovalMarker(breadcrumbs, 0)
    }

    private fun removedValueCount(element: JsonElement?): Int =
        when {
            element == null || element.isJsonNull -> 0
            element.isJsonArray -> element.asJsonArray.size()
            else -> 1
        }

    private fun tagsRemovalMarker(removedCount: Int): String = "$TAGS_TRUNCATED_PREFIX-$removedCount-tags-removed"

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

    private fun replaceTagsWithRemovalMarker(report: JsonObject) {
        val details = report.obj("details") ?: return
        val originalTags = details.get("tags") ?: return
        val removedCount = removedValueCount(originalTags)
        val replacement = JsonArray().apply { add(tagsRemovalMarker(removedCount)) }
        if (
            removedCount > 0 &&
            replacement.toString().byteSize() < originalTags.toString().byteSize()
        ) {
            details.add("tags", replacement)
        }
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

            var removed = 0L
            for (index in keepTop until frames.size() - keepBottom) {
                removed += representedFrameCount(frames[index])
            }
            val trimmed = JsonArray()
            repeat(keepTop) { index -> trimmed.add(frames[index]) }
            trimmed.add(removedFramesMarker(removed))
            repeat(keepBottom) { offset ->
                trimmed.add(frames[frames.size() - keepBottom + offset])
            }
            error.add("stackTrace", trimmed)
        }
    }

    private fun representedFrameCount(frame: JsonElement): Long {
        if (!frame.isJsonObject) {
            return 1
        }

        val marker = frame.asJsonObject
        if (marker.int("lineNumber") != 0) {
            return 1
        }

        val message = marker.string("raw") ?: return 1
        val represented =
            removedFramesMessage
                .matchEntire(message)
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
        return represented?.takeIf { it in 1..Int.MAX_VALUE.toLong() } ?: 1
    }

    private fun removedFramesMarker(removed: Long): JsonObject =
        JsonObject().apply {
            addProperty("lineNumber", 0)
            addProperty("raw", "$removed frames removed from middle of stack trace")
        }

    private fun removeLargestCustomDataEntries(
        report: JsonObject,
        maxBytes: Int,
    ): String? {
        val details = report.obj("details") ?: return null
        val customDataElement = details.get("userCustomData") ?: return null
        if (!customDataElement.isJsonObject) {
            details.add(
                "userCustomData",
                JsonObject().apply {
                    add(CUSTOM_DATA_MARKER_KEY, customDataRemovalMarker(valueRemoved = true))
                },
            )
            return serializedWithinLimit(report, maxBytes)
        }

        val customData = customDataElement.asJsonObject
        if (customData.size() == 0) {
            return null
        }
        val markerKey = availableCustomDataMarkerKey(customData)
        val omittedValue =
            com.google.gson.JsonPrimitive(
                "Value omitted because the crash report payload was too large",
            )
        val omittedValueBytes = omittedValue.toString().byteSize()
        var replacementBytes = report.toString().byteSize()
        var valuesRemoved = 0
        val valuesByDescendingSize =
            customData
                .entrySet()
                .map { (key, value) -> key to value.toString().byteSize() }
                .sortedByDescending { (_, size) -> size }
        for ((key, size) in valuesByDescendingSize) {
            if (size <= omittedValueBytes) {
                continue
            }
            customData.add(key, omittedValue)
            replacementBytes -= size - omittedValueBytes
            valuesRemoved++
            val marker = customDataRemovalMarker(valuesRemoved = valuesRemoved)
            if (replacementBytes + customDataEntrySize(markerKey, marker) + 1 <= maxBytes) {
                customData.add(markerKey, marker)
                serializedWithinLimit(report, maxBytes)?.let {
                    return it
                }
                customData.remove(markerKey)
            }
        }
        val originalBytes = report.toString().byteSize()
        val originalEntryCount = customData.size()
        var removedEntryBytes = 0
        var removedEntries = 0
        val keysByDescendingSize =
            customData
                .entrySet()
                .map { (key, value) -> Triple(key, value, customDataEntrySize(key, value)) }
                .sortedByDescending { (_, _, size) -> size }
        for ((key, _, entrySize) in keysByDescendingSize) {
            customData.remove(key)
            removedEntryBytes += entrySize
            removedEntries++
            val marker =
                customDataRemovalMarker(
                    entriesRemoved = removedEntries,
                    valuesRemoved = valuesRemoved,
                )
            val removedCommaBytes = removedEntries.coerceAtMost(originalEntryCount - 1)
            val markerEntryBytes = customDataEntrySize(markerKey, marker)
            val markerCommaBytes = if (customData.size() > 0) 1 else 0
            val estimatedBytes =
                originalBytes - removedEntryBytes - removedCommaBytes +
                    markerEntryBytes +
                    markerCommaBytes
            if (estimatedBytes <= maxBytes) {
                customData.add(markerKey, marker)
                serializedWithinLimit(report, maxBytes)?.let {
                    return it
                }
                customData.remove(markerKey)
            }
        }
        customData.add(
            markerKey,
            customDataRemovalMarker(entriesRemoved = removedEntries, valuesRemoved = valuesRemoved),
        )
        return null
    }

    private fun availableCustomDataMarkerKey(customData: JsonObject): String {
        if (!customData.has(CUSTOM_DATA_MARKER_KEY)) {
            return CUSTOM_DATA_MARKER_KEY
        }

        var suffix = 1
        while (customData.has("$CUSTOM_DATA_MARKER_KEY.$suffix")) {
            suffix++
        }
        return "$CUSTOM_DATA_MARKER_KEY.$suffix"
    }

    private fun customDataRemovalMarker(
        entriesRemoved: Int? = null,
        valueRemoved: Boolean = false,
        valuesRemoved: Int = 0,
    ): JsonObject =
        JsonObject().apply {
            entriesRemoved?.let { addProperty("entriesRemoved", it) }
            if (valuesRemoved > 0) {
                addProperty("valuesRemoved", valuesRemoved)
            }
            if (valueRemoved) {
                addProperty("valueRemoved", true)
            }
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
        if (breadcrumbs.isEmpty()) {
            return null
        }
        var minimum = 0
        var maximum = breadcrumbs.size - 1
        var bestPayload: String? = null
        var bestCount = -1
        while (minimum <= maximum) {
            val count = minimum + (maximum - minimum) / 2
            details.add("breadcrumbs", breadcrumbsWithRemovalMarker(breadcrumbs, count))
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
            details.add("breadcrumbs", breadcrumbsWithRemovalMarker(breadcrumbs, bestCount))
            return bestPayload
        }

        details.add("breadcrumbs", breadcrumbsWithRemovalMarker(breadcrumbs, 0))
        return null
    }

    private fun breadcrumbsWithRemovalMarker(
        breadcrumbs: List<JsonElement>,
        retainedCount: Int,
    ): JsonArray {
        val removedCount = breadcrumbs.size - retainedCount
        return JsonArray().apply {
            add(breadcrumbRemovalMarker(removedCount, breadcrumbs[removedCount - 1]))
            breadcrumbs.takeLast(retainedCount).forEach(::add)
        }
    }

    private fun breadcrumbRemovalMarker(
        removedCount: Int,
        lastRemovedBreadcrumb: JsonElement,
    ): JsonObject =
        JsonObject().apply {
            addProperty(
                "message",
                "$removedCount older breadcrumbs removed during payload reduction",
            )
            addProperty("category", BREADCRUMB_MARKER_CATEGORY)
            addProperty("level", BREADCRUMB_INFO_LEVEL)
            addProperty("type", BREADCRUMB_MARKER_TYPE)
            val timestamp =
                lastRemovedBreadcrumb
                    .takeIf(JsonElement::isJsonObject)
                    ?.asJsonObject
                    ?.get("timestamp")
                    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
            if (timestamp != null) {
                add("timestamp", timestamp.deepCopy())
            } else {
                addProperty("timestamp", 0)
            }
        }

    private fun truncateErrorStrings(report: JsonObject) {
        forEachError(report) { error ->
            error.truncateString("message", MAX_ERROR_MESSAGE_BYTES)
            error.truncateString("className", MAX_ERROR_CLASS_BYTES)
        }
    }

    private fun truncateStackFrameStrings(report: JsonObject) {
        forEachError(report) { error ->
            error.array("stackTrace")?.forEach { frame ->
                if (frame.isJsonObject) {
                    frame.asJsonObject.truncateString("className", MAX_FRAME_STRING_BYTES)
                    frame.asJsonObject.truncateString("fileName", MAX_FRAME_STRING_BYTES)
                    frame.asJsonObject.truncateString("methodName", MAX_FRAME_STRING_BYTES)
                    frame.asJsonObject.truncateString("raw", MAX_FRAME_STRING_BYTES)
                }
            }
        }
    }

    private fun truncateCodePoints(
        value: String,
        maxCodePoints: Int,
    ): String {
        val codePointCount = value.codePointCount(0, value.length)
        if (codePointCount <= maxCodePoints) {
            return value
        }

        val contentCodePoints = (maxCodePoints - 1).coerceAtLeast(0)
        val endIndex = value.offsetByCodePoints(0, contentCodePoints)
        return value.substring(0, endIndex) + TRUNCATED_TEXT
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

    private fun String.byteSize(): Int = toByteArray(Charsets.UTF_8).size
}
