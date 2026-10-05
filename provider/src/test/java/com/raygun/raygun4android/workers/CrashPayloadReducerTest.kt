package com.raygun.raygun4android.workers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashPayloadReducerTest {
    @Test
    fun `payload within byte limit is returned unchanged`() {
        val payload = report().toString()

        assertEquals(payload, CrashPayloadReducer.reduceToLimit(payload))
    }

    @Test
    fun `payload at exact byte limit is unchanged but one byte over is reduced`() {
        val report = report()
        report
            .details()
            .add(
                "userCustomData",
                JsonObject().apply { addProperty("removable", "x".repeat(1_000)) },
            )
        val payload = report.toString()
        val payloadBytes = payload.toByteArray(Charsets.UTF_8).size

        assertEquals(payload, CrashPayloadReducer.reduceToLimit(payload, payloadBytes))

        val reduced = CrashPayloadReducer.reduceToLimit(payload, payloadBytes - 1)

        assertNotEquals(payload, reduced)
        assertMarked(parse(reduced))
        assertTrue(reduced.toByteArray(Charsets.UTF_8).size <= payloadBytes - 1)
    }

    @Test
    fun `long stack traces are trimmed before custom data`() {
        val report = report()
        report
            .details()
            .add(
                "userCustomData",
                JsonObject().apply { addProperty("keep-me", "custom-value") },
            )
        report.details().addProperty("unknownField", "preserved")
        val stack = report.error().getAsJsonArray("stackTrace")
        repeat(140) { index -> stack.add(frame(index, "m$index-${"x".repeat(1_000)}")) }

        val reduced = parse(CrashPayloadReducer.reduceToLimit(report.toString()))
        val reducedStack = reduced.error().getAsJsonArray("stackTrace")

        assertEquals(41, reducedStack.size())
        assertEquals("m0-${"x".repeat(1_000)}", reducedStack[0].asJsonObject["methodName"].asString)
        assertEquals(
            "100 frames removed from middle of stack trace",
            reducedStack[20].asJsonObject["raw"].asString,
        )
        assertEquals(setOf("lineNumber", "raw"), reducedStack[20].asJsonObject.keySet())
        assertEquals(
            "m139-${"x".repeat(1_000)}",
            reducedStack[40].asJsonObject["methodName"].asString,
        )
        assertEquals(
            "custom-value",
            reduced.details()["userCustomData"].asJsonObject["keep-me"].asString,
        )
        assertEquals("preserved", reduced.details()["unknownField"].asString)
        assertMarked(reduced)
        assertWithinLimit(reduced.toString())
    }

    @Test
    fun `stack traces in nested errors are also trimmed`() {
        val outerStack = report().error().getAsJsonArray("stackTrace")
        repeat(110) { index -> outerStack.add(frame(index, "outer-${"x".repeat(700)}")) }
        val inner = error("InnerException")
        repeat(110) { index ->
            inner.getAsJsonArray("stackTrace").add(frame(index, "inner-${"x".repeat(700)}"))
        }
        val report = report()
        report.error().add("stackTrace", outerStack)
        report.error().add("innerError", inner)

        val reduced = parse(CrashPayloadReducer.reduceToLimit(report.toString()))

        assertEquals(41, reduced.error().getAsJsonArray("stackTrace").size())
        assertEquals(
            41,
            reduced
                .error()["innerError"]
                .asJsonObject
                .getAsJsonArray("stackTrace")
                .size(),
        )
        assertWithinLimit(reduced.toString())
    }

    @Test
    fun `large stack frame strings are reduced before custom data`() {
        val report = report()
        report
            .details()
            .add(
                "userCustomData",
                JsonObject().apply { addProperty("important", "keep me") },
            )
        val stack = report.error().getAsJsonArray("stackTrace")
        repeat(50) { index -> stack.add(frame(index, "method-${"x".repeat(3_000)}")) }

        val reduced = parse(CrashPayloadReducer.reduceToLimit(report.toString()))
        val reducedStack = reduced.error().getAsJsonArray("stackTrace")

        assertEquals(50, reducedStack.size())
        assertEquals(
            "keep me",
            reduced.details()["userCustomData"].asJsonObject["important"].asString,
        )
        assertTrue(
            reducedStack[0]
                .asJsonObject["methodName"]
                .asString
                .toByteArray(Charsets.UTF_8)
                .size <=
                512,
        )
        assertWithinLimit(reduced.toString())
    }

    @Test
    fun `repeated stack reductions report the cumulative number of removed frames`() {
        val inner = errorWithFrames("InnerException", 200, 700)
        val middle =
            errorWithFrames("MiddleException", 200, 700).apply {
                add("innerError", inner)
            }
        val report = report()
        report
            .details()
            .add(
                "error",
                errorWithFrames("OuterException", 200, 700).apply {
                    add("innerError", middle)
                },
            )

        val reduced = parse(CrashPayloadReducer.reduceToLimit(report.toString()))
        var error: JsonObject? = reduced.error()

        repeat(3) {
            val stack = requireNotNull(error).getAsJsonArray("stackTrace")
            assertEquals(11, stack.size())
            assertEquals(
                listOf(0, 1, 2, 3, 4),
                stack.take(5).map { frame -> frame.asJsonObject["lineNumber"].asInt },
            )
            assertEquals(
                "190 frames removed from middle of stack trace",
                stack[5].asJsonObject["raw"].asString,
            )
            assertEquals(setOf("lineNumber", "raw"), stack[5].asJsonObject.keySet())
            assertEquals(
                listOf(195, 196, 197, 198, 199),
                stack.drop(6).map { frame -> frame.asJsonObject["lineNumber"].asInt },
            )
            error = error?.getAsJsonObject("innerError")
        }
        assertWithinLimit(reduced.toString())
    }

    @Test
    fun `largest custom data entries are removed first`() {
        val report = report()
        report
            .details()
            .add(
                "userCustomData",
                JsonObject().apply {
                    addProperty("smaller", "s".repeat(40 * 1024))
                    addProperty("largest", "l".repeat(90 * 1024))
                },
            )

        val reduced = parse(CrashPayloadReducer.reduceToLimit(report.toString()))
        val customData = reduced.details()["userCustomData"].asJsonObject

        assertFalse(customData.has("largest"))
        assertTrue(customData.has("smaller"))
        assertMarked(reduced)
        assertWithinLimit(reduced.toString())
    }

    @Test
    fun `oldest breadcrumbs are removed and newest are retained`() {
        val report = report()
        report
            .details()
            .add(
                "breadcrumbs",
                JsonArray().apply {
                    repeat(80) { index ->
                        add(
                            JsonObject().apply {
                                addProperty("message", "breadcrumb-$index-${"b".repeat(2_000)}")
                            },
                        )
                    }
                },
            )

        val reduced = parse(CrashPayloadReducer.reduceToLimit(report.toString()))
        val breadcrumbs = reduced.details().getAsJsonArray("breadcrumbs")
        val firstRetained = breadcrumbs[0].asJsonObject["message"].asString

        assertTrue(breadcrumbs.size() in 1..79)
        assertNotEquals("breadcrumb-0-", firstRetained.take("breadcrumb-0-".length))
        assertTrue(
            breadcrumbs
                .last()
                .asJsonObject["message"]
                .asString
                .startsWith("breadcrumb-79-"),
        )
        assertWithinLimit(reduced.toString())
    }

    @Test
    fun `oversized tags are replaced with one truncation marker`() {
        val report = report()
        report
            .details()
            .add(
                "tags",
                JsonArray().apply {
                    add(CrashPayloadReducer.TRUNCATION_TAG)
                    repeat(80) { index -> add("tag-$index-${"t".repeat(2_000)}") }
                    add(CrashPayloadReducer.TRUNCATION_TAG)
                },
            )

        val reduced = parse(CrashPayloadReducer.reduceToLimit(report.toString()))
        val tags = reduced.details().getAsJsonArray("tags")

        assertEquals(1, tags.size())
        assertEquals(CrashPayloadReducer.TRUNCATION_TAG, tags[0].asString)
        assertWithinLimit(reduced.toString())
    }

    @Test
    fun `large unicode strings are truncated at code point boundaries`() {
        val report = report()
        report.error().addProperty("message", "😀".repeat(40_000) + "\u0001")

        val reducedJson = CrashPayloadReducer.reduceToLimit(report.toString())
        val reduced = parse(reducedJson)
        val message = reduced.error()["message"].asString

        assertTrue(message.endsWith("…"))
        assertFalse(message.contains('\uFFFD'))
        assertMarked(reduced)
        assertWithinLimit(reducedJson)
    }

    @Test
    fun `minimal fallback preserves crash identity and is valid and bounded`() {
        val report = report("OriginalException")
        report.addProperty("occurredOn", "2026-09-29T10:15:30Z")
        report.error().addProperty("message", "original message")
        report.error().getAsJsonArray("stackTrace").add(frame(42, "originalMethod"))
        report.details().addProperty("groupingKey", "stable-group")
        report.details().addProperty("version", "1.2.3")
        report
            .details()
            .add(
                "client",
                JsonObject().apply {
                    addProperty("name", "Raygun4Android")
                    addProperty("version", "4.0.0")
                    addProperty("clientUrl", "https://github.com/MindscapeHQ/raygun4android")
                },
            )
        report.details().addProperty("environment", "x".repeat(300 * 1024))

        val minimalJson = CrashPayloadReducer.reduceToLimit(report.toString())
        val minimal = parse(minimalJson)
        val minimalFrame = minimal.error().getAsJsonArray("stackTrace")[0].asJsonObject

        assertEquals("2026-09-29T10:15:30Z", minimal["occurredOn"].asString)
        assertEquals("OriginalException", minimal.error()["className"].asString)
        assertEquals("original message", minimal.error()["message"].asString)
        assertEquals(42, minimalFrame["lineNumber"].asInt)
        assertEquals("originalMethod", minimalFrame["methodName"].asString)
        assertEquals("stable-group", minimal.details()["groupingKey"].asString)
        assertEquals("1.2.3", minimal.details()["version"].asString)
        assertEquals(
            "Raygun4Android",
            minimal.details()["client"].asJsonObject["name"].asString,
        )
        assertEquals(
            setOf("error", "groupingKey", "version", "client", "tags"),
            minimal.details().keySet(),
        )
        assertMarked(minimal)
        assertWithinLimit(minimalJson)
    }

    @Test
    fun `minimal fallback supplies a line numbered frame when original stack is empty`() {
        val minimal = parse(requireNotNull(CrashPayloadReducer.minimalPayload(report().toString())))
        val frame = minimal.error().getAsJsonArray("stackTrace")[0].asJsonObject

        assertEquals(0, frame["lineNumber"].asInt)
        assertEquals("payloadTruncated", frame["methodName"].asString)
    }

    @Test
    fun `minimal fallback uses first nested frame while preserving outer error identity`() {
        val report = report("OuterException")
        report.error().addProperty("message", "outer message")
        report
            .error()
            .add(
                "innerError",
                error("InnerException").apply {
                    getAsJsonArray("stackTrace").add(frame(73, "innerMethod"))
                },
            )

        val minimal = parse(requireNotNull(CrashPayloadReducer.minimalPayload(report.toString())))
        val minimalFrame = minimal.error().getAsJsonArray("stackTrace")[0].asJsonObject

        assertEquals("OuterException", minimal.error()["className"].asString)
        assertEquals("outer message", minimal.error()["message"].asString)
        assertEquals(73, minimalFrame["lineNumber"].asInt)
        assertEquals("innerMethod", minimalFrame["methodName"].asString)
    }

    @Test
    fun `minimal fallback bounds retained grouping and client metadata`() {
        val report = report()
        report.details().addProperty("groupingKey", "😀".repeat(101))
        report.details().addProperty("version", "v".repeat(1_000))
        report
            .details()
            .add(
                "client",
                JsonObject().apply {
                    addProperty("name", "n".repeat(1_000))
                    addProperty("version", "v".repeat(1_000))
                    addProperty("clientUrl", "u".repeat(5_000))
                },
            )

        val minimalJson = requireNotNull(CrashPayloadReducer.minimalPayload(report.toString()))
        val minimalDetails = parse(minimalJson).details()
        val groupingKey = minimalDetails["groupingKey"].asString
        val client = minimalDetails["client"].asJsonObject

        assertEquals(100, groupingKey.codePointCount(0, groupingKey.length))
        assertTrue(groupingKey.endsWith("…"))
        assertTrue(minimalDetails["version"].asString.endsWith("…"))
        assertTrue(client["name"].asString.endsWith("…"))
        assertTrue(client["version"].asString.endsWith("…"))
        assertTrue(client["clientUrl"].asString.endsWith("…"))
        assertWithinLimit(minimalJson)
    }

    @Test
    fun `malformed payload cannot be reduced or converted to a fallback`() {
        val malformed = "not-json-${"x".repeat(CrashPayloadReducer.MAX_PAYLOAD_BYTES)}"

        assertEquals(malformed, CrashPayloadReducer.reduceToLimit(malformed))
        assertNull(CrashPayloadReducer.minimalPayload(malformed))
    }

    @Test
    fun `minimal fallback rejects json without required report structure`() {
        val incompleteReports =
            listOf(
                "{}",
                "{\"occurredOn\":\"2026-09-29T10:15:30Z\"}",
                "{\"occurredOn\":\"2026-09-29T10:15:30Z\",\"details\":{}}",
                "{\"details\":{\"error\":{}}}",
            )

        incompleteReports.forEach { payload ->
            assertNull(payload, CrashPayloadReducer.minimalPayload(payload))
        }
    }

    private fun report(className: String = "TestException"): JsonObject =
        JsonObject().apply {
            addProperty("occurredOn", "2026-09-29T10:15:30Z")
            add(
                "details",
                JsonObject().apply {
                    add("error", error(className))
                    add("tags", JsonArray().apply { add("existing-tag") })
                },
            )
        }

    private fun error(className: String): JsonObject =
        JsonObject().apply {
            addProperty("message", "test message")
            addProperty("className", className)
            add("stackTrace", JsonArray())
        }

    private fun errorWithFrames(
        className: String,
        frameCount: Int,
        methodNameBytes: Int,
    ): JsonObject =
        error(className).apply {
            val stack = getAsJsonArray("stackTrace")
            repeat(frameCount) { index ->
                stack.add(
                    frame(index, "method-${"x".repeat(methodNameBytes)}").apply {
                        addProperty("className", "c".repeat(methodNameBytes))
                        addProperty("fileName", "f".repeat(methodNameBytes))
                    },
                )
            }
        }

    private fun frame(
        lineNumber: Int,
        methodName: String,
    ): JsonObject =
        JsonObject().apply {
            addProperty("lineNumber", lineNumber)
            addProperty("className", "Example")
            addProperty("fileName", "Example.kt")
            addProperty("methodName", methodName)
        }

    private fun parse(payload: String): JsonObject = JsonParser.parseString(payload).asJsonObject

    private fun JsonObject.details(): JsonObject = getAsJsonObject("details")

    private fun JsonObject.error(): JsonObject = details().getAsJsonObject("error")

    private fun assertMarked(report: JsonObject) {
        assertTrue(
            report.details().getAsJsonArray("tags").any {
                it.asString == CrashPayloadReducer.TRUNCATION_TAG
            },
        )
    }

    private fun assertWithinLimit(payload: String) {
        assertTrue(
            "${payload.toByteArray(Charsets.UTF_8).size} bytes exceeded limit",
            payload.toByteArray(Charsets.UTF_8).size <= CrashPayloadReducer.MAX_PAYLOAD_BYTES,
        )
    }
}
