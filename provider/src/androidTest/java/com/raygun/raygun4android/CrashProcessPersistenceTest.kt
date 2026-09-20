package com.raygun.raygun4android

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raygun.raygun4android.workers.CrashReportCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CrashProcessPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun setUp() {
        CrashReportCache.clear(context)
    }

    @After
    fun tearDown() {
        CrashReportCache.clear(context)
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
            reports = CrashReportCache.files(context)
            if (reports.isEmpty()) {
                Thread.sleep(POLL_INTERVAL_MILLIS)
            }
        } while (reports.isEmpty() && System.currentTimeMillis() < deadline)

        assertEquals(1, reports.size)
        val payload = CrashReportCache.readPersistent(reports.single())
        assertTrue(payload?.contains(CrashProcessActivity.CRASH_MESSAGE) == true)
    }

    companion object {
        private const val REPORT_TIMEOUT_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 50L
    }
}
