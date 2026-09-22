package com.raygun.raygun4android

import com.google.gson.JsonParser
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class CrashReportingTest {
    private val application = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        cachedReports().forEach(File::delete)
        CrashReporting.setOnBeforeSend(null)
        RaygunClient.init(application, "test-api-key", "1.0.0")
        RaygunClient.setUser("test-user")
        RaygunClient.enableCrashReporting(attachDefaultHandler = false)
    }

    @After
    fun tearDown() {
        CrashReporting.setOnBeforeSend(null)
        cachedReports().forEach(File::delete)
    }

    @Test
    fun `uncaught exception is cached before default handler runs`() {
        CrashReporting.setOnBeforeSend(
            object : CrashReportingOnBeforeSend {
                override fun onBeforeSend(message: RaygunMessage): RaygunMessage {
                    Thread.sleep(100)
                    return message
                }
            },
        )

        var cachedMessageAtTermination: String? = null
        val defaultHandler =
            Thread.UncaughtExceptionHandler { _, _ ->
                val files = cachedReports()
                assertEquals(1, files.size)
                cachedMessageAtTermination = CrashReportCache.readPersistent(files.single())
            }
        val handler = CrashReporting.RaygunUncaughtExceptionHandler(defaultHandler)

        handler.uncaughtException(Thread.currentThread(), IllegalStateException("test crash"))

        val payload = JsonParser.parseString(cachedMessageAtTermination).asJsonObject
        val details = payload.getAsJsonObject("details")
        assertTrue(
            details
                .getAsJsonObject("error")
                .get("message")
                .asString
                .contains("test crash"),
        )
        assertTrue(
            details
                .getAsJsonArray("tags")
                .map { it.asString }
                .contains(RaygunSettings.CRASH_REPORTING_UNHANDLED_EXCEPTION_TAG),
        )
    }

    @Test
    fun `caching an uncaught exception returns at its deadline and a late write is kept`() {
        val releaseCallback = CountDownLatch(1)
        CrashReporting.setOnBeforeSend(
            object : CrashReportingOnBeforeSend {
                override fun onBeforeSend(message: RaygunMessage): RaygunMessage {
                    releaseCallback.await()
                    return message
                }
            },
        )

        val startedAt = System.nanoTime()
        val stored =
            CrashReporting.cacheUnhandledException(
                IllegalStateException("test crash"),
                listOf(RaygunSettings.CRASH_REPORTING_UNHANDLED_EXCEPTION_TAG),
                timeoutMillis = 25,
            )
        val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

        assertFalse(stored)
        assertTrue("Crash handler blocked for $elapsedMillis ms", elapsedMillis < 1000)

        releaseCallback.countDown()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (cachedReports().isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }
        assertEquals(1, cachedReports().size)
    }

    @Test
    fun `failing onBeforeSend in a handled send does not reach the uncaught exception handler`() {
        val uncaughtExceptions = CopyOnWriteArrayList<Throwable>()
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            uncaughtExceptions.add(exception)
        }

        try {
            val callbackFailed = CountDownLatch(1)
            CrashReporting.setOnBeforeSend(
                object : CrashReportingOnBeforeSend {
                    override fun onBeforeSend(message: RaygunMessage): RaygunMessage {
                        callbackFailed.countDown()
                        throw IllegalStateException("callback failed")
                    }
                },
            )
            CrashReporting.send(IllegalStateException("first"), null)
            assertTrue(callbackFailed.await(2, TimeUnit.SECONDS))
            CrashReporting.setOnBeforeSend(null)
            CrashReporting.send(IllegalStateException("second"), null)

            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            while (cachedReports().isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }

            assertEquals(1, cachedReports().size)
            assertTrue(uncaughtExceptions.isEmpty())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        }
    }

    private fun cachedReports(): Array<File> = CrashReportCache.files(application)
}
