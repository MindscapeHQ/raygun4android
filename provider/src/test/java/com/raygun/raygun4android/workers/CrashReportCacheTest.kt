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

    private fun cachedReports() = application.cacheDir.listFiles(RaygunFileFilter()) ?: emptyArray()
}
