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
import java.util.concurrent.TimeUnit

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
        assertEquals(
            "first payload",
            CrashReportCache.readPersistent(files.single()).messagePayload,
        )
    }

    @Test
    fun `store records the API key and endpoint the report was created for`() {
        val storedFile =
            CrashReportCache.store(application, "payload", "api-key", CUSTOM_ENDPOINT)!!

        assertEquals(
            CrashReportStoreEntry("api-key", CUSTOM_ENDPOINT, "payload"),
            CrashReportCache.readPersistent(storedFile),
        )
    }

    @Test
    fun `report stored by an earlier SDK version is read as its payload alone`() {
        val payload = "{\"occurredOn\":\"2026-01-01T00:00:00Z\",\"details\":{}}"
        val earlierReport =
            File(CrashReportCache.persistentDirectory(application), "earlier.raygun4").apply {
                parentFile?.mkdirs()
                writeText(payload)
            }

        assertEquals(
            CrashReportStoreEntry(null, null, payload),
            CrashReportCache.readPersistent(earlierReport),
        )
    }

    @Test
    fun `store evicts the oldest report when the maximum is reached`() {
        RaygunSettings.maxReportsStoredOnDevice = 1

        assertNotNull(CrashReportCache.store(application, "first payload"))
        val newest = CrashReportCache.store(application, "second payload")

        assertNotNull(newest)
        assertEquals(
            "second payload",
            CrashReportCache.readPersistent(cachedReports().single()).messagePayload,
        )
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
        assertEquals(
            "new payload",
            CrashReportCache.readPersistent(cachedReports().single()).messagePayload,
        )
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
    fun `discovery removes stale temporary report and preserves possible active report`() {
        val staleTemporaryFile =
            temporaryReport('0', System.currentTimeMillis() - EIGHT_DAYS_MILLIS)
        val freshTemporaryFile = temporaryReport('1', System.currentTimeMillis())

        try {
            CrashReportCache.files(application)

            assertFalse(staleTemporaryFile.exists())
            assertTrue(freshTemporaryFile.exists())
        } finally {
            staleTemporaryFile.delete()
            freshTemporaryFile.delete()
        }
    }

    @Test
    fun `temporary report cleanup leaves files it does not own`() {
        val unrelatedTemporaryFile =
            File(application.cacheDir, "unrelated.tmp").apply {
                writeText("keep")
                setLastModified(1L)
            }
        val malformedTemporaryFile =
            File(application.cacheDir, ".not-a-raygun-report.tmp").apply {
                writeText("keep")
                setLastModified(1L)
            }
        val matchingDirectory =
            File(application.cacheDir, ".${"2".repeat(32)}.tmp").apply { mkdir() }

        try {
            CrashReportCache.files(application)

            assertTrue(unrelatedTemporaryFile.exists())
            assertTrue(malformedTemporaryFile.exists())
            assertTrue(matchingDirectory.exists())
        } finally {
            unrelatedTemporaryFile.delete()
            malformedTemporaryFile.delete()
            matchingDirectory.delete()
        }
    }

    @Test
    fun `temporary report cleanup preserves unknown and future timestamps`() {
        val unknownTimestamp = temporaryReport('3', 0L)
        val futureTimestamp = temporaryReport('4', System.currentTimeMillis() + EIGHT_DAYS_MILLIS)

        try {
            CrashReportCache.files(application)

            assertTrue(unknownTimestamp.exists())
            assertTrue(futureTimestamp.exists())
        } finally {
            unknownTimestamp.delete()
            futureTimestamp.delete()
        }
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

    private fun temporaryReport(
        identifier: Char,
        lastModified: Long,
    ): File =
        File(application.cacheDir, ".${identifier.toString().repeat(32)}.tmp").apply {
            writeText("temporary report")
            setLastModified(lastModified)
        }

    private fun cachedReports() = CrashReportCache.files(application)

    companion object {
        private const val CUSTOM_ENDPOINT = "https://crash.example.com/entries"
        private val EIGHT_DAYS_MILLIS = TimeUnit.DAYS.toMillis(8)
    }
}
