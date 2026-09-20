package com.raygun.raygun4android

import com.google.gson.JsonParser
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage
import com.raygun.raygun4android.workers.CrashReportCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream

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

        var cachedMessageAtTermination: SerializedMessage? = null
        val defaultHandler =
            Thread.UncaughtExceptionHandler { _, _ ->
                val files = cachedReports()
                assertEquals(1, files.size)
                cachedMessageAtTermination = readCachedMessage(files.single())
            }
        val handler = CrashReporting.RaygunUncaughtExceptionHandler(defaultHandler)

        handler.uncaughtException(Thread.currentThread(), IllegalStateException("test crash"))

        val payload = JsonParser.parseString(cachedMessageAtTermination?.message).asJsonObject
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

    private fun cachedReports(): Array<File> = CrashReportCache.files(application)

    private fun readCachedMessage(file: File): SerializedMessage =
        ObjectInputStream(FileInputStream(file)).use { input ->
            val message = input.readObject() as SerializedMessage
            assertNotNull(message.message)
            message
        }
}
