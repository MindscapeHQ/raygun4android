package com.raygun.raygun4android.workers

import androidx.work.ListenableWorker.Result
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.SerializedMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

@RunWith(RobolectricTestRunner::class)
class CrashReportingWorkerTest {
    private val application = RuntimeEnvironment.getApplication()
    private lateinit var worker: CrashReportingWorker

    @Before
    fun setUp() {
        CrashReportCache.clear(application)
        worker = CrashReportingWorker(application, mock())
    }

    @Test
    fun `transient delivery failure retains payload for WorkManager retry`() {
        val file = rawReport("payload to retry")

        val result =
            worker.processCrashReport(file, "api-key") { _, apiKey, message ->
                assertEquals("api-key", apiKey)
                assertEquals("payload to retry", message)
                -1
            }

        assertEquals(Result.retry(), result)
        assertTrue(file.exists())
    }

    @Test
    fun `successful delivery removes raw payload`() {
        val file = rawReport("delivered payload")

        val result = worker.processCrashReport(file, "api-key") { _, _, _ -> 202 }

        assertEquals(Result.success(), result)
        assertFalse(file.exists())
    }

    @Test
    fun `permanent rejection removes payload`() {
        val file = rawReport("invalid payload")

        val result = worker.processCrashReport(file, "api-key") { _, _, _ -> 400 }

        assertEquals(Result.failure(), result)
        assertFalse(file.exists())
    }

    @Test
    fun `successful delivery reads and removes persistent cached payload`() {
        val file = persistentReport("recovered crash")
        var deliveredMessage: String? = null

        val result =
            worker.processCrashReport(file, "api-key") { _, _, message ->
                deliveredMessage = message
                202
            }

        assertEquals(Result.success(), result)
        assertEquals("recovered crash", deliveredMessage)
        assertFalse(file.exists())
    }

    @Test
    fun `transient failure retains persistent cached payload`() {
        val file = persistentReport("recovered crash to retry")

        val result = worker.processCrashReport(file, "api-key") { _, _, _ -> 503 }

        assertEquals(Result.retry(), result)
        assertTrue(file.exists())
    }

    @Test
    fun `missing persistent payload fails without posting`() {
        val missingReport =
            File(CrashReportCache.persistentDirectory(application), "missing.raygun4")

        val result =
            worker.processCrashReport(missingReport, "api-key") { _, _, _ ->
                throw AssertionError("Missing payload must not be posted")
            }

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `non-file persistent payload fails without posting`() {
        val directory = CrashReportCache.persistentDirectory(application).apply { mkdirs() }
        val invalidReport = File(directory, "invalid.raygun4").apply { mkdir() }

        val result =
            worker.processCrashReport(invalidReport, "api-key") { _, _, _ ->
                throw AssertionError("Non-file payload must not be posted")
            }

        assertEquals(Result.failure(), result)
        assertTrue(invalidReport.exists())
    }

    @Test
    fun `read failure for existing persistent payload retains it for retry`() {
        val unreadableReport = persistentReport("temporarily unreadable")
        assertTrue(unreadableReport.setReadable(false, false))

        try {
            val result =
                worker.processCrashReport(unreadableReport, "api-key") { _, _, _ ->
                    throw AssertionError("Unreadable payload must not be posted")
                }

            assertEquals(Result.retry(), result)
            assertTrue(unreadableReport.exists())
        } finally {
            unreadableReport.setReadable(true, false)
        }
    }

    @Test
    fun `successful delivery remains compatible with legacy serialized payload`() {
        val file = serializedReport("legacy recovered crash")
        var deliveredMessage: String? = null

        val result =
            worker.processCrashReport(file, "api-key") { _, _, message ->
                deliveredMessage = message
                202
            }

        assertEquals(Result.success(), result)
        assertEquals("legacy recovered crash", deliveredMessage)
        assertFalse(file.exists())
    }

    @Test
    fun `report is posted with the API key and endpoint it was stored with`() {
        val file =
            requireNotNull(
                CrashReportCache.store(
                    application,
                    "stored crash",
                    "stored-api-key",
                    CUSTOM_ENDPOINT,
                ),
            )
        var postedEndpoint: String? = null
        var postedApiKey: String? = null

        val result =
            worker.processCrashReport(file, "current-api-key") { endpoint, apiKey, _ ->
                postedEndpoint = endpoint
                postedApiKey = apiKey
                202
            }

        assertEquals(Result.success(), result)
        assertEquals(CUSTOM_ENDPOINT, postedEndpoint)
        assertEquals("stored-api-key", postedApiKey)
    }

    @Test
    fun `report stored by an earlier SDK version uses the current API key and endpoint`() {
        val file = rawReport("earlier crash")
        var postedEndpoint: String? = null
        var postedApiKey: String? = null
        RaygunSettings.crashReportingEndpoint = CUSTOM_ENDPOINT

        try {
            val result =
                worker.processCrashReport(file, "api-key") { endpoint, apiKey, _ ->
                    postedEndpoint = endpoint
                    postedApiKey = apiKey
                    202
                }

            assertEquals(Result.success(), result)
            assertEquals(CUSTOM_ENDPOINT, postedEndpoint)
            assertEquals("api-key", postedApiKey)
        } finally {
            RaygunSettings.crashReportingEndpoint = RaygunSettings.DEFAULT_CRASHREPORTING_ENDPOINT
        }
    }

    @Test
    fun `oversized payload for stored Raygun endpoint is reduced before first post`() {
        val originalPayload = validPayload(130 * 1024)
        val file =
            requireNotNull(
                CrashReportCache.store(
                    application,
                    originalPayload,
                    "stored-api-key",
                    RaygunSettings.DEFAULT_CRASHREPORTING_ENDPOINT,
                ),
            )
        RaygunSettings.crashReportingEndpoint = CUSTOM_ENDPOINT
        var postCount = 0

        try {
            val result =
                worker.processCrashReport(file, "current-api-key") { endpoint, apiKey, payload ->
                    postCount++
                    assertEquals(RaygunSettings.DEFAULT_CRASHREPORTING_ENDPOINT, endpoint)
                    assertEquals("stored-api-key", apiKey)
                    assertTrue(
                        payload.toByteArray(Charsets.UTF_8).size <=
                            CrashPayloadReducer.MAX_PAYLOAD_BYTES,
                    )
                    assertTrue(hasTruncationMarker(payload))
                    202
                }

            assertEquals(Result.success(), result)
            assertEquals(1, postCount)
            assertFalse(file.exists())
        } finally {
            RaygunSettings.crashReportingEndpoint = RaygunSettings.DEFAULT_CRASHREPORTING_ENDPOINT
        }
    }

    @Test
    fun `oversized payload for custom endpoint is posted unchanged when accepted`() {
        val originalPayload = validPayload(130 * 1024)
        val file =
            requireNotNull(
                CrashReportCache.store(
                    application,
                    originalPayload,
                    "api-key",
                    CUSTOM_ENDPOINT,
                ),
            )

        val result =
            worker.processCrashReport(file, "current-api-key") { endpoint, _, payload ->
                assertEquals(CUSTOM_ENDPOINT, endpoint)
                assertEquals(originalPayload, payload)
                202
            }

        assertEquals(Result.success(), result)
        assertFalse(file.exists())
    }

    @Test
    fun `413 retries once with minimal payload and success removes cached report`() {
        val originalPayload = validPayload(1_024)
        val file =
            requireNotNull(
                CrashReportCache.store(application, originalPayload, "api-key", CUSTOM_ENDPOINT),
            )
        val postedPayloads = mutableListOf<String>()

        val result =
            worker.processCrashReport(file, "current-api-key") { _, _, payload ->
                postedPayloads += payload
                if (postedPayloads.size == 1) 413 else 202
            }

        assertEquals(Result.success(), result)
        assertEquals(2, postedPayloads.size)
        assertEquals(originalPayload, postedPayloads[0])
        assertTrue(hasCustomDataReductionMarker(postedPayloads[1]))
        assertTrue(postedPayloads[1].length < originalPayload.length)
        assertFalse(file.exists())
    }

    @Test
    fun `transient failure after 413 retry retains original cached report`() {
        val originalPayload = validPayload(1_024)
        val file =
            requireNotNull(
                CrashReportCache.store(application, originalPayload, "api-key", CUSTOM_ENDPOINT),
            )
        var postCount = 0

        val result =
            worker.processCrashReport(file, "current-api-key") { _, _, _ ->
                postCount++
                if (postCount == 1) 413 else 503
            }

        assertEquals(Result.retry(), result)
        assertEquals(2, postCount)
        assertTrue(file.exists())
        assertEquals(originalPayload, CrashReportCache.readPersistent(file).messagePayload)
    }

    @Test
    fun `second 413 stops retrying and removes permanently rejected report`() {
        val file =
            requireNotNull(
                CrashReportCache.store(
                    application,
                    validPayload(1_024),
                    "api-key",
                    CUSTOM_ENDPOINT,
                ),
            )
        var postCount = 0

        val result =
            worker.processCrashReport(file, "current-api-key") { _, _, _ ->
                postCount++
                413
            }

        assertEquals(Result.failure(), result)
        assertEquals(2, postCount)
        assertFalse(file.exists())
    }

    @Test
    fun `oversized legacy serialized payload is reduced before delivery`() {
        val file = serializedReport(validPayload(130 * 1024))
        var deliveredPayload: String? = null

        val result =
            worker.processCrashReport(file, "api-key") { endpoint, _, payload ->
                assertEquals(RaygunSettings.DEFAULT_CRASHREPORTING_ENDPOINT, endpoint)
                deliveredPayload = payload
                202
            }

        assertEquals(Result.success(), result)
        assertTrue(
            requireNotNull(deliveredPayload).toByteArray(Charsets.UTF_8).size <=
                CrashPayloadReducer.MAX_PAYLOAD_BYTES,
        )
        assertTrue(hasTruncationMarker(requireNotNull(deliveredPayload)))
        assertFalse(file.exists())
    }

    @Test
    fun `empty API key fails without posting and retains payload for later initialization`() {
        val file = rawReport("payload without API key")
        var postAttempted = false

        val result =
            worker.processCrashReport(file, "") { _, _, _ ->
                postAttempted = true
                202
            }

        assertEquals(Result.failure(), result)
        assertFalse(postAttempted)
        assertTrue(file.exists())
    }

    @Test
    fun `corrupt cached payload fails without posting and is removed`() {
        val file = legacyCacheReport("not a serialized message")
        var postAttempted = false

        val result =
            worker.processCrashReport(file, "api-key") { _, _, _ ->
                postAttempted = true
                202
            }

        assertEquals(Result.failure(), result)
        assertFalse(postAttempted)
        assertFalse(file.exists())
    }

    private fun rawReport(message: String): File =
        File.createTempFile("raygun-", ".raygun4", application.filesDir).apply {
            writeText(message)
            deleteOnExit()
        }

    private fun persistentReport(message: String): File = requireNotNull(CrashReportCache.store(application, message))

    private fun legacyCacheReport(message: String): File =
        File.createTempFile("raygun-", ".raygun4", application.cacheDir).apply {
            writeText(message)
            deleteOnExit()
        }

    private fun serializedReport(message: String): File =
        File.createTempFile("raygun-", ".raygun4", application.cacheDir).apply {
            ObjectOutputStream(FileOutputStream(this)).use { output ->
                output.writeObject(SerializedMessage(message))
            }
            deleteOnExit()
        }

    private fun validPayload(customDataBytes: Int = 0): String =
        JsonObject()
            .apply {
                addProperty("occurredOn", "2026-09-29T10:15:30Z")
                add(
                    "details",
                    JsonObject().apply {
                        add(
                            "error",
                            JsonObject().apply {
                                addProperty("message", "test message with enough detail")
                                addProperty("className", "TestException")
                                add(
                                    "stackTrace",
                                    JsonArray().apply {
                                        add(
                                            JsonObject().apply {
                                                addProperty("lineNumber", 42)
                                                addProperty("className", "Example")
                                                addProperty("fileName", "Example.kt")
                                                addProperty("methodName", "crash")
                                            },
                                        )
                                    },
                                )
                            },
                        )
                        if (customDataBytes > 0) {
                            add(
                                "userCustomData",
                                JsonObject().apply {
                                    addProperty("large-value", "x".repeat(customDataBytes))
                                },
                            )
                        }
                    },
                )
            }.toString()

    private fun hasTruncationMarker(payload: String): Boolean =
        JsonParser
            .parseString(payload)
            .asJsonObject["details"]
            .asJsonObject["tags"]
            .asJsonArray
            .any { it.asString == CrashPayloadReducer.TRUNCATION_TAG }

    private fun hasCustomDataReductionMarker(payload: String): Boolean =
        JsonParser
            .parseString(payload)
            .asJsonObject["details"]
            .asJsonObject["userCustomData"]
            .asJsonObject
            .has(CrashPayloadReducer.CUSTOM_DATA_MARKER_KEY)

    companion object {
        private const val CUSTOM_ENDPOINT = "https://crash.example.com/entries"
    }
}
