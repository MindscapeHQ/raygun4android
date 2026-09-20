package com.raygun.raygun4android

import com.google.gson.JsonParser
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage
import com.raygun.raygun4android.workers.CrashReportCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

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
    fun `caching an uncaught exception returns after its deadline when callback blocks`() {
        val releaseCallback = CountDownLatch(1)
        val callbackStopped = CountDownLatch(1)
        CrashReporting.setOnBeforeSend(
            object : CrashReportingOnBeforeSend {
                override fun onBeforeSend(message: RaygunMessage): RaygunMessage? =
                    try {
                        try {
                            releaseCallback.await()
                            null
                        } catch (_: InterruptedException) {
                            message
                        }
                    } finally {
                        callbackStopped.countDown()
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

        assertTrue(!stored)
        assertTrue("Crash handler blocked for $elapsedMillis ms", elapsedMillis < 1000)
        assertTrue(callbackStopped.await(1, TimeUnit.SECONDS))
        assertTrue(cachedReports().isEmpty())

        releaseCallback.countDown()
    }

    @Test
    fun `timed out uncaught exception is not cached after cache monitor becomes available`() {
        val monitorHeld = CountDownLatch(1)
        val releaseMonitor = CountDownLatch(1)
        val cacheAttemptCompleted = CountDownLatch(1)
        val stored = AtomicBoolean(true)
        val blocker =
            thread {
                synchronized(CrashReportCache) {
                    monitorHeld.countDown()
                    releaseMonitor.await()
                }
            }

        try {
            assertTrue(monitorHeld.await(1, TimeUnit.SECONDS))
            thread {
                stored.set(
                    CrashReporting.cacheUnhandledException(
                        IllegalStateException("timed out while waiting for cache"),
                        listOf(RaygunSettings.CRASH_REPORTING_UNHANDLED_EXCEPTION_TAG),
                        timeoutMillis = 500,
                    ),
                )
                cacheAttemptCompleted.countDown()
            }

            assertTrue(waitForCacheStoreToBlock())
            assertTrue(cacheAttemptCompleted.await(1, TimeUnit.SECONDS))
            assertTrue(!stored.get())
        } finally {
            releaseMonitor.countDown()
            blocker.join(1_000)
        }

        Thread.sleep(250)
        assertTrue(cachedReports().isEmpty())
    }

    private fun waitForCacheStoreToBlock(): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (System.nanoTime() < deadline) {
            val blocked =
                Thread.getAllStackTraces().any { (thread, stackTrace) ->
                    thread.state == Thread.State.BLOCKED &&
                        stackTrace.any { frame ->
                            frame.className == CrashReportCache::class.java.name &&
                                frame.methodName == "store"
                        }
                }
            if (blocked) {
                return true
            }
            Thread.sleep(5)
        }
        return false
    }

    private fun cachedReports(): Array<File> = CrashReportCache.files(application)
}
