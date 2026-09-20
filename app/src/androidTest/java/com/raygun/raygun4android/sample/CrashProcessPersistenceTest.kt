package com.raygun.raygun4android.sample

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raygun.raygun4android.RaygunSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CrashProcessPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val reportDirectory = File(context.noBackupFilesDir, "raygun-crash-reports")

    @Before
    fun setUp() {
        clearReports()
    }

    @After
    fun tearDown() {
        clearReports()
    }

    @Test
    fun unhandledCrashSurvivesSeparateProcessTermination() {
        context.startActivity(
            Intent(context, CrashProcessActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )

        val deadline = System.currentTimeMillis() + REPORT_TIMEOUT_MILLIS
        var reports: Array<File>
        do {
            reports =
                reportDirectory.listFiles { file ->
                    file.extension == RaygunSettings.DEFAULT_FILE_EXTENSION
                } ?: emptyArray()
            if (reports.isEmpty()) {
                Thread.sleep(POLL_INTERVAL_MILLIS)
            }
        } while (reports.isEmpty() && System.currentTimeMillis() < deadline)

        assertEquals(1, reports.size)
        assertTrue(reports.single().readText().contains(CrashProcessActivity.CRASH_MESSAGE))
    }

    private fun clearReports() {
        reportDirectory.listFiles()?.forEach(File::delete)
    }

    companion object {
        private const val REPORT_TIMEOUT_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 50L
    }
}
