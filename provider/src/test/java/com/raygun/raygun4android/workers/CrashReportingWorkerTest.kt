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
            worker.processCrashReport(file, false, "api-key") { apiKey, message ->
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

        val result = worker.processCrashReport(file, false, "api-key") { _, _ -> 202 }

        assertEquals(Result.success(), result)
        assertFalse(file.exists())
    }

    @Test
    fun `permanent rejection removes payload`() {
        val file = rawReport("invalid payload")

        val result = worker.processCrashReport(file, false, "api-key") { _, _ -> 400 }

        assertEquals(Result.failure(), result)
        assertFalse(file.exists())
    }

    @Test
    fun `successful delivery reads and removes persistent cached payload`() {
        val file = persistentReport("recovered crash")
        var deliveredMessage: String? = null

        val result =
            worker.processCrashReport(file, false, "api-key") { _, message ->
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

        val result = worker.processCrashReport(file, false, "api-key") { _, _ -> 503 }

        assertEquals(Result.retry(), result)
        assertTrue(file.exists())
    }

    @Test
    fun `filesystem read failure retains persistent payload for retry`() {
        val directory = CrashReportCache.persistentDirectory(application).apply { mkdirs() }
        val unreadableReport = File(directory, "unreadable.raygun4").apply { mkdir() }

        val result =
            worker.processCrashReport(unreadableReport, false, "api-key") { _, _ ->
                throw AssertionError("Unreadable payload must not be posted")
            }

        assertEquals(Result.retry(), result)
        assertTrue(unreadableReport.exists())
    }

    @Test
    fun `successful delivery remains compatible with legacy serialized payload`() {
        val file = serializedReport("legacy recovered crash")
        var deliveredMessage: String? = null

        val result =
            worker.processCrashReport(file, true, "api-key") { _, message ->
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
            worker.processCrashReport(file, false, "") { _, _ ->
                postAttempted = true
                202
            }

        assertEquals(Result.failure(), result)
        assertFalse(postAttempted)
        assertTrue(file.exists())
    }

    @Test
    fun `corrupt cached payload fails without posting and is removed`() {
        val file = rawReport("not a serialized message")
        var postAttempted = false

        val result =
            worker.processCrashReport(file, true, "api-key") { _, _ ->
                postAttempted = true
                202
            }

        assertEquals(Result.failure(), result)
        assertFalse(postAttempted)
        assertFalse(file.exists())
    }

    @Test
    fun `successful delivery does not leave a discoverable source when marking fails`() {
        val file = persistentReport("delivered before cleanup failure")
        val processedTarget = File(file.parentFile, ".${file.name}.processed")

        try {
            val result =
                worker.processCrashReport(file, false, "api-key") { _, _ ->
                    assertTrue(file.delete())
                    assertTrue(file.mkdir())
                    assertTrue(processedTarget.mkdir())
                    File(processedTarget, "prevent-rename").writeText("occupied")
                    202
                }

            assertEquals(Result.success(), result)
            assertFalse(file.exists())
            assertTrue(
                CrashReportCache.files(application).none { it.absolutePath == file.absolutePath },
            )
        } finally {
            file.deleteRecursively()
            processedTarget.deleteRecursively()
        }
    }

    private fun rawReport(message: String): File =
        File.createTempFile("raygun-", ".raygun4", application.filesDir).apply {
            writeText(message)
            deleteOnExit()
        }

    private fun persistentReport(message: String): File = requireNotNull(CrashReportCache.store(application, message))

    private fun serializedReport(message: String): File =
        File.createTempFile("raygun-", ".raygun4", application.cacheDir).apply {
            ObjectOutputStream(FileOutputStream(this)).use { output ->
                output.writeObject(SerializedMessage(message))
            }
            deleteOnExit()
        }
}
