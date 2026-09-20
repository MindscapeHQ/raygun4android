package com.raygun.raygun4android.workers

import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.SerializedMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream

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
    fun `store respects maximum cached report count`() {
        RaygunSettings.maxReportsStoredOnDevice = 1

        assertNotNull(CrashReportCache.store(application, "first payload"))
        assertNull(CrashReportCache.store(application, "second payload"))
        assertEquals(1, cachedReports().size)
    }

    @Test
    fun `store counts legacy reports toward maximum`() {
        RaygunSettings.maxReportsStoredOnDevice = 1
        File(application.cacheDir, "legacy.raygun4").writeText("legacy")

        assertNull(CrashReportCache.store(application, "new payload"))
        assertEquals(1, cachedReports().size)
    }

    @Test
    fun `discovery removes interrupted temporary and processed files`() {
        val directory = CrashReportCache.persistentDirectory(application).apply { mkdirs() }
        val temporaryFile =
            File(directory, ".interrupted.tmp").apply {
                writeText("partial")
                setLastModified(0L)
            }
        val processedFile =
            File(directory, ".delivered.raygun4.processed").apply { writeText("sent") }

        assertTrue(CrashReportCache.files(application).isEmpty())
        assertFalse(temporaryFile.exists())
        assertFalse(processedFile.exists())
    }

    @Test
    fun `cached work request retains durable file until worker runs`() {
        val cachedFile = CrashReportCache.store(application, "cached payload")!!

        val request =
            CrashReportingWorkerHelper.cachedCrashReportWorkRequest(
                application,
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
        assertFalse(
            request.workSpec.input.getBoolean(
                CrashReportingWorkerHelper.LEGACY_SERIALIZED_INPUT,
                true,
            ),
        )
        assertTrue(cachedFile.exists())
    }

    @Test
    fun `legacy cached request records serialized format`() {
        val legacyFile = File(application.cacheDir, "legacy.raygun4")
        ObjectOutputStream(FileOutputStream(legacyFile)).use { output ->
            output.writeObject(SerializedMessage("legacy payload"))
        }

        val request =
            CrashReportingWorkerHelper.cachedCrashReportWorkRequest(
                application,
                legacyFile,
                "test-api-key",
            )

        assertTrue(
            request.workSpec.input.getBoolean(
                CrashReportingWorkerHelper.LEGACY_SERIALIZED_INPUT,
                false,
            ),
        )
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
