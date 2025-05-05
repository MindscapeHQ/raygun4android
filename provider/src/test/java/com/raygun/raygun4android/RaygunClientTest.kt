package com.raygun.raygun4android

import android.app.Application
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import timber.log.Timber

@RunWith(MockitoJUnitRunner::class)
class RaygunClientTest {
    @Before
    fun setup() {
        Timber.plant(TestTree())
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    private lateinit var mockApplication: Application

    @Test
    fun initializesWithApplicationAndApiKeyAndVersion() {
        val mockApplication = mock<Application>(Application::class.java)
        whenever(mockApplication.applicationContext).thenReturn(mockApplication)

        val apiKey = "testApiKey"

        RaygunClient.init(mockApplication, apiKey, "1.0.0")

        assertEquals(apiKey, RaygunClient.apiKey)
        assertEquals("1.0.0", RaygunClient.version)
        assertNotNull(RaygunClient.getApplicationContext())
    }
}
