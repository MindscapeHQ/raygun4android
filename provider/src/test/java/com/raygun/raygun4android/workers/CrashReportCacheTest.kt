package com.raygun.raygun4android.workers

import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.SerializedMessage
import com.raygun.raygun4android.utils.RaygunFileFilter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream

@RunWith(RobolectricTestRunner::class)
class CrashReportCacheTest {
    private val application = RuntimeEnvironment.getApplication()
    private val originalMaximum = RaygunSettings.maxReportsStoredOnDevice

    @Before
    fun setUp() {
        cachedReports().forEach { it.delete() }
    }

    @After
    fun tearDown() {
        RaygunSettings.maxReportsStoredOnDevice = originalMaximum
        cachedReports().forEach { it.delete() }
    }

    @Test
    fun `store writes a crash report that can be recovered after process restart`() {
        assertTrue(CrashReportCache.store(application, "first payload"))

        val files = cachedReports()
        assertEquals(1, files.size)
        val message =
            ObjectInputStream(FileInputStream(files.single())).use { input ->
                input.readObject() as SerializedMessage
            }
        assertEquals("first payload", message.message)
    }

    @Test
    fun `store respects maximum cached report count`() {
        RaygunSettings.maxReportsStoredOnDevice = 1

        assertTrue(CrashReportCache.store(application, "first payload"))
        assertFalse(CrashReportCache.store(application, "second payload"))
        assertEquals(1, cachedReports().size)
    }

    @Test
    fun `cached work request retains durable file until worker runs`() {
        assertTrue(CrashReportCache.store(application, "cached payload"))
        val cachedFile = cachedReports().single()

        val request =
            CrashReportingWorkerHelper.cachedCrashReportWorkRequest(
                cachedFile,
                "test-api-key",
            )

        assertEquals(
            cachedFile.name,
            request.workSpec.input.getString(CrashReportingWorkerHelper.CACHED_FILE_INPUT),
        )
        assertEquals(
            "test-api-key",
            request.workSpec.input.getString(CrashReportingWorkerHelper.API_KEY_INPUT),
        )
        assertEquals(
            null,
            request.workSpec.input.getString(CrashReportingWorkerHelper.TEMP_FILE_INPUT),
        )
        assertTrue(cachedFile.exists())
    }

    @Test
    fun `cached work name is stable per file and distinct between reports`() {
        val first = File(application.cacheDir, "first.raygun4")
        val second = File(application.cacheDir, "second.raygun4")

        assertEquals(
            CrashReportingWorkerHelper.cachedWorkName(first),
            CrashReportingWorkerHelper.cachedWorkName(first),
        )
        assertTrue(
            CrashReportingWorkerHelper.cachedWorkName(first) !=
                CrashReportingWorkerHelper.cachedWorkName(second),
        )
    }

    private fun cachedReports() = application.cacheDir.listFiles(RaygunFileFilter()) ?: emptyArray()
}
