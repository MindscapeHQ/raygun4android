package com.raygun.raygun4android.workers

import android.content.ContextWrapper
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.RaygunSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CrashReportCacheTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

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
    fun `failed replacement write preserves the existing report`() {
        RaygunSettings.maxReportsStoredOnDevice = 1
        val noBackupDirectory = temporaryFolder.newFolder("no-backup")
        val unusableCacheDirectory = temporaryFolder.newFile("not-a-directory")
        val context =
            object : ContextWrapper(application) {
                override fun getNoBackupFilesDir(): File = noBackupDirectory

                override fun getCacheDir(): File = unusableCacheDirectory
            }
        val existingReport =
            File(CrashReportCache.persistentDirectory(context), "existing.raygun4").apply {
                parentFile?.mkdirs()
                writeText("existing payload")
            }

        assertNull(CrashReportCache.store(context, "replacement payload"))

        assertTrue(existingReport.exists())
        assertEquals("existing payload", existingReport.readText())
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
    fun `decreasing the maximum keeps the newest reports`() {
        RaygunClient.init(application, null, "1.0.0")
        val oldest =
            CrashReportCache.store(application, "oldest")!!.apply { setLastModified(1_000L) }
        val newest =
            CrashReportCache.store(application, "newest")!!.apply { setLastModified(2_000L) }

        RaygunClient.setMaxReportsStoredOnDevice(1)

        assertEquals(1, RaygunSettings.maxReportsStoredOnDevice)
        assertFalse(oldest.exists())
        assertTrue(newest.exists())
    }

    @Test
    fun `maximum outside 1 to 64 is ignored`() {
        RaygunSettings.maxReportsStoredOnDevice = 8

        RaygunSettings.maxReportsStoredOnDevice = 0
        RaygunSettings.maxReportsStoredOnDevice = -1
        RaygunSettings.maxReportsStoredOnDevice =
            RaygunSettings.DEFAULT_MAX_REPORTS_STORED_ON_DEVICE + 1

        assertEquals(8, RaygunSettings.maxReportsStoredOnDevice)
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
