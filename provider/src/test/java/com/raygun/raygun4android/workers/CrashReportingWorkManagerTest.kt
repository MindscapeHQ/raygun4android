package com.raygun.raygun4android.workers

import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.raygun.raygun4android.CrashReporting
import com.raygun.raygun4android.OkHttpClientBuilder
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.RaygunSettings
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CrashReportingWorkManagerTest {
    private val application = RuntimeEnvironment.getApplication()
    private val originalMaximum = RaygunSettings.maxReportsStoredOnDevice

    @Before
    fun setUp() {
        CrashReportCache.clear(application)
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(application, configuration)
        RaygunSettings.okHttpClientBuilder =
            object : OkHttpClientBuilder {
                override fun build(): OkHttpClient =
                    OkHttpClient
                        .Builder()
                        .addInterceptor { chain ->
                            Response
                                .Builder()
                                .request(chain.request())
                                .protocol(Protocol.HTTP_1_1)
                                .code(202)
                                .message("Accepted")
                                .body("".toResponseBody())
                                .build()
                        }.build()
            }
    }

    @After
    fun tearDown() {
        RaygunSettings.okHttpClientBuilder = null
        RaygunSettings.maxReportsStoredOnDevice = originalMaximum
        CrashReportCache.clear(application)
    }

    @Test
    fun `full spool keeps newest report and WorkManager delivers it once`() {
        RaygunSettings.maxReportsStoredOnDevice = 1
        CrashReportingWorkerHelper.enqueueCrashReport(application, "{\"first\":true}", null)
        CrashReportingWorkerHelper.enqueueCrashReport(application, "{\"second\":true}", "api-key")
        val file = CrashReportCache.files(application).single()
        assertEquals("{\"second\":true}", CrashReportCache.readPersistent(file))

        assertTrue(
            CrashReportingWorkerHelper.enqueueCachedCrashReport(
                application,
                file,
                "api-key",
            ),
        )
        assertTrue(
            CrashReportingWorkerHelper.enqueueCachedCrashReport(
                application,
                file,
                "api-key",
            ),
        )

        val workManager = WorkManager.getInstance(application)
        val workName = CrashReportingWorkerHelper.cachedWorkName(file)
        val queuedWork = workManager.getWorkInfosForUniqueWork(workName).get()
        assertEquals(1, queuedWork.size)
        assertEquals(WorkInfo.State.ENQUEUED, queuedWork.single().state)
        assertTrue(file.exists())

        val testDriver = requireNotNull(WorkManagerTestInitHelper.getTestDriver(application))
        testDriver.setAllConstraintsMet(queuedWork.single().id)

        val completedWork = workManager.getWorkInfosForUniqueWork(workName).get().single()
        assertEquals(WorkInfo.State.SUCCEEDED, completedWork.state)
        assertFalse(file.exists())
    }

    @Test
    fun `next send rescans retained reports`() {
        RaygunClient.init(application, "api-key", "1.0.0")
        RaygunClient.enableCrashReporting(attachDefaultHandler = false)
        val retainedFile =
            requireNotNull(CrashReportCache.store(application, "{\"retained\":true}"))
        assertFalse(
            CrashReportingWorkerHelper.enqueueCachedCrashReport(
                application,
                retainedFile,
                null,
            ),
        )

        CrashReporting.send(IllegalStateException("trigger cached report rescan"), null)

        val workManager = WorkManager.getInstance(application)
        val workName = CrashReportingWorkerHelper.cachedWorkName(retainedFile)
        val deadline = System.currentTimeMillis() + RESCAN_TIMEOUT_MILLIS
        var queuedWork = workManager.getWorkInfosForUniqueWork(workName).get()
        while (queuedWork.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
            queuedWork = workManager.getWorkInfosForUniqueWork(workName).get()
        }

        assertEquals(1, queuedWork.size)
        assertEquals(WorkInfo.State.ENQUEUED, queuedWork.single().state)
    }

    companion object {
        private const val RESCAN_TIMEOUT_MILLIS = 2_000L
    }
}
