package com.raygun.raygun4android.workers

import androidx.work.ListenableWorker.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class RaygunWorkerHelperTest {
    @Test
    fun toWorkerResult_returns_success_for_2xx() {
        assertEquals(Result.success(), RaygunWorkerHelper.toWorkerResult(200))
        assertEquals(Result.success(), RaygunWorkerHelper.toWorkerResult(202))
        assertEquals(Result.success(), RaygunWorkerHelper.toWorkerResult(299))
    }

    @Test
    fun toWorkerResult_returns_failure_for_400() {
        assertEquals(Result.failure(), RaygunWorkerHelper.toWorkerResult(400))
    }

    @Test
    fun toWorkerResult_returns_failure_for_403() {
        assertEquals(Result.failure(), RaygunWorkerHelper.toWorkerResult(403))
    }

    @Test
    fun toWorkerResult_returns_failure_for_413() {
        assertEquals(Result.failure(), RaygunWorkerHelper.toWorkerResult(413))
    }

    @Test
    fun toWorkerResult_returns_retry_for_429() {
        assertEquals(Result.retry(), RaygunWorkerHelper.toWorkerResult(429))
    }

    @Test
    fun toWorkerResult_returns_retry_for_5xx() {
        assertEquals(Result.retry(), RaygunWorkerHelper.toWorkerResult(500))
        assertEquals(Result.retry(), RaygunWorkerHelper.toWorkerResult(502))
        assertEquals(Result.retry(), RaygunWorkerHelper.toWorkerResult(503))
    }

    @Test
    fun toWorkerResult_returns_retry_for_negative_one() {
        assertEquals(Result.retry(), RaygunWorkerHelper.toWorkerResult(-1))
    }

    @Test
    fun toWorkerResult_returns_retry_for_unknown_codes() {
        assertEquals(Result.retry(), RaygunWorkerHelper.toWorkerResult(0))
        assertEquals(Result.retry(), RaygunWorkerHelper.toWorkerResult(418))
    }
}
