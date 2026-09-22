package com.raygun.raygun4android.sample

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raygun.raygun4android.OkHttpClientBuilder
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.RaygunSettings
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class CrashProcessPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val reportDirectory = File(context.noBackupFilesDir, "raygun-crash-reports")

    @Before
    fun setUp() {
        clearReports()
        RaygunClient.init(context, null, "1.0.0")
    }

    @After
    fun tearDown() {
        RaygunClient.setOkHttpClientBuilder(null)
        clearReports()
    }

    @Test
    fun unhandledCrashSurvivesSeparateProcessTermination() {
        val crashProcess = CrashProcessBinding(Intent(context, CrashProcessService::class.java))
        try {
            crashProcess.bind()
            crashProcess.awaitTermination()
        } finally {
            crashProcess.close()
        }

        val reports = reports()
        assertEquals(1, reports.size)
        assertTrue(reports.single().readText().contains(CrashProcessService.CRASH_MESSAGE))

        val deliveredPayload = AtomicReference<String>()
        val reportDelivered = CountDownLatch(1)
        RaygunClient.setOkHttpClientBuilder(
            object : OkHttpClientBuilder {
                override fun build(): OkHttpClient =
                    OkHttpClient
                        .Builder()
                        .addInterceptor { chain ->
                            val buffer = Buffer()
                            chain.request().body?.writeTo(buffer)
                            deliveredPayload.set(buffer.readUtf8())
                            reportDelivered.countDown()
                            Response
                                .Builder()
                                .request(chain.request())
                                .protocol(Protocol.HTTP_1_1)
                                .code(202)
                                .message("Accepted")
                                .body("".toResponseBody())
                                .build()
                        }.build()
            },
        )

        RaygunClient.init(context, "test-api-key", "1.0.0")

        assertTrue(reportDelivered.await(REPORT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        assertTrue(deliveredPayload.get().contains(CrashProcessService.CRASH_MESSAGE))
        waitForReportsToBeRemoved()
    }

    private fun reports(): Array<File> =
        reportDirectory.listFiles { file ->
            file.extension == RaygunSettings.DEFAULT_FILE_EXTENSION
        } ?: emptyArray()

    private fun waitForReportsToBeRemoved() {
        val deadline = System.currentTimeMillis() + REPORT_TIMEOUT_MILLIS
        while (reports().isNotEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        assertTrue(reports().isEmpty())
    }

    private fun clearReports() {
        reportDirectory.listFiles()?.forEach(File::delete)
    }

    private inner class CrashProcessBinding(
        private val intent: Intent,
    ) {
        private val connected = CountDownLatch(1)
        private val disconnected = CountDownLatch(1)
        private var bound = false
        private val connection =
            object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName?,
                    service: IBinder?,
                ) {
                    connected.countDown()
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    disconnected.countDown()
                }

                override fun onBindingDied(name: ComponentName?) {
                    disconnected.countDown()
                }
            }

        fun bind() {
            bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            assertTrue(bound)
            assertTrue(connected.await(PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        }

        fun awaitTermination() {
            assertTrue(disconnected.await(PROCESS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
        }

        fun close() {
            if (bound) {
                context.unbindService(connection)
                bound = false
            }
        }
    }

    companion object {
        private const val REPORT_TIMEOUT_MILLIS = 10_000L
        private const val PROCESS_TIMEOUT_MILLIS = 10_000L
        private const val POLL_INTERVAL_MILLIS = 50L
    }
}
