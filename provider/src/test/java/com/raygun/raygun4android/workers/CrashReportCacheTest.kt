package com.raygun.raygun4android.workers

import com.raygun.raygun4android.RaygunSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

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
        val storedFile = CrashReportCache.store(application, "first payload")

        assertNotNull(storedFile)
        val files = cachedReports()
        assertEquals(1, files.size)
        assertEquals(CrashReportCache.persistentDirectory(application), files.single().parentFile)
        assertEquals("first payload", CrashReportCache.readPersistent(files.single()))
    }

    @Test
    fun `store evicts the oldest report when the maximum is reached`() {
        RaygunSettings.maxReportsStoredOnDevice = 1

        assertNotNull(CrashReportCache.store(application, "first payload"))
        val newest = CrashReportCache.store(application, "second payload")

        assertNotNull(newest)
        assertEquals("second payload", CrashReportCache.readPersistent(cachedReports().single()))
    }

    @Test
    fun `store counts legacy reports toward maximum`() {
        RaygunSettings.maxReportsStoredOnDevice = 1
        val legacyFile = File(application.cacheDir, "legacy.raygun4").apply { writeText("legacy") }

        assertNotNull(CrashReportCache.store(application, "new payload"))
        assertFalse(legacyFile.exists())
        assertEquals("new payload", CrashReportCache.readPersistent(cachedReports().single()))
    }

    @Test
    fun `trim removes the oldest reports first`() {
        val oldest =
            CrashReportCache.store(application, "oldest")!!.apply { setLastModified(1_000L) }
        val middle =
            CrashReportCache.store(application, "middle")!!.apply { setLastModified(2_000L) }
        val newest =
            CrashReportCache.store(application, "newest")!!.apply { setLastModified(3_000L) }

        CrashReportCache.trim(application, 2)

        assertFalse(oldest.exists())
        assertTrue(middle.exists())
        assertTrue(newest.exists())
    }

    @Test
    fun `store leaves no temporary file behind`() {
        assertNotNull(CrashReportCache.store(application, "payload"))

        assertTrue(
            application.cacheDir.listFiles { file -> file.name.endsWith(".tmp") }!!.isEmpty(),
        )
    }

    @Test
    fun `cached work request retains durable file until worker runs`() {
        val cachedFile = CrashReportCache.store(application, "cached payload")!!

        val request =
            CrashReportingWorkerHelper.cachedCrashReportWorkRequest(
                cachedFile,
                "test-api-key",
            )

        assertEquals(
            cachedFile.absolutePath,
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

    @Test
    fun `cached report is retained when API key is unavailable`() {
        val cachedFile = CrashReportCache.store(application, "cached payload")!!

        assertFalse(
            CrashReportingWorkerHelper.enqueueCachedCrashReport(application, cachedFile, null),
        )
        assertTrue(cachedFile.exists())
    }

    private fun cachedReports() = CrashReportCache.files(application)
}
