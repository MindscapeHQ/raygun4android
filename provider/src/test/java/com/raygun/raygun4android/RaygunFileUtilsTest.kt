package com.raygun.raygun4android

import com.raygun.raygun4android.utils.RaygunFileUtils
import com.raygun.raygun4android.workers.CrashReportCache
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

@RunWith(RobolectricTestRunner::class)
class RaygunFileUtilsTest {
    private val application = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        CrashReportCache.files(application).forEach(File::delete)
    }

    @After
    fun tearDown() {
        CrashReportCache.files(application).forEach(File::delete)
    }

    @Test
    fun getExtensionReturnsCorrectExtension() {
        val filename = "testfile.txt"
        val expectedExtension = "txt"
        val resultExtension = RaygunFileUtils.getExtension(filename)

        assertEquals(expectedExtension, resultExtension)
    }

    @Test
    fun clearCachedReportsLeavesUnrelatedFiles() {
        val persistentTextFile =
            File(CrashReportCache.persistentDirectory(application), "keep.txt").apply {
                parentFile?.mkdirs()
                writeText("keep")
            }
        val cacheTextFile =
            File(application.cacheDir, "keep.txt").apply {
                writeText("keep")
            }

        RaygunFileUtils.clearCachedReports(application)

        assertTrue(persistentTextFile.exists())
        assertTrue(cacheTextFile.exists())
        persistentTextFile.delete()
        cacheTextFile.delete()
    }

    @Test
    fun clearCachedReportsDeletesPersistentAndLegacyReports() {
        val persistentReport = CrashReportCache.store(application, "persistent report")!!
        val legacyReport =
            File(application.cacheDir, "legacy.raygun4").apply {
                writeText("legacy report")
            }

        RaygunFileUtils.clearCachedReports(application)

        assertFalse(persistentReport.exists())
        assertFalse(legacyReport.exists())
    }
}
