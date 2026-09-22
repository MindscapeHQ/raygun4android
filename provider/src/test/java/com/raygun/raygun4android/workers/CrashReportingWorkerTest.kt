package com.raygun.raygun4android.workers

import androidx.work.ListenableWorker.Result
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
            worker.processCrashReport(file, "api-key") { apiKey, message ->
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

        val result = worker.processCrashReport(file, "api-key") { _, _ -> 202 }

        assertEquals(Result.success(), result)
        assertFalse(file.exists())
    }

    @Test
    fun `permanent rejection removes payload`() {
        val file = rawReport("invalid payload")

        val result = worker.processCrashReport(file, "api-key") { _, _ -> 400 }

        assertEquals(Result.failure(), result)
        assertFalse(file.exists())
    }

    @Test
    fun `successful delivery reads and removes persistent cached payload`() {
        val file = persistentReport("recovered crash")
        var deliveredMessage: String? = null

        val result =
            worker.processCrashReport(file, "api-key") { _, message ->
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

        val result = worker.processCrashReport(file, "api-key") { _, _ -> 503 }

        assertEquals(Result.retry(), result)
        assertTrue(file.exists())
    }

    @Test
    fun `missing persistent payload fails without posting`() {
        val missingReport =
            File(CrashReportCache.persistentDirectory(application), "missing.raygun4")

        val result =
            worker.processCrashReport(missingReport, "api-key") { _, _ ->
                throw AssertionError("Missing payload must not be posted")
            }

        assertEquals(Result.failure(), result)
    }

    @Test
    fun `non-file persistent payload fails without posting`() {
        val directory = CrashReportCache.persistentDirectory(application).apply { mkdirs() }
        val invalidReport = File(directory, "invalid.raygun4").apply { mkdir() }

        val result =
            worker.processCrashReport(invalidReport, "api-key") { _, _ ->
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
                worker.processCrashReport(unreadableReport, "api-key") { _, _ ->
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
            worker.processCrashReport(file, "api-key") { _, message ->
                deliveredMessage = message
                202
            }

        assertEquals(Result.success(), result)
        assertEquals("legacy recovered crash", deliveredMessage)
        assertFalse(file.exists())
    }

    @Test
    fun `empty API key fails without posting and retains payload for later initialization`() {
        val file = rawReport("payload without API key")
        var postAttempted = false

        val result =
            worker.processCrashReport(file, "") { _, _ ->
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
            worker.processCrashReport(file, "api-key") { _, _ ->
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
}
