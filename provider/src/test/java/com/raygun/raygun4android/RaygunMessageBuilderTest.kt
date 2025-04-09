package com.raygun.raygun4android

import android.content.Context
import com.raygun.raygun4android.messages.shared.RaygunUserInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class RaygunMessageBuilderTest {
    private lateinit var raygunMessageBuilder: RaygunMessageBuilder

    @Before
    fun setUp() {
        raygunMessageBuilder = RaygunMessageBuilder()
    }

    @Test
    fun `test build creates valid RaygunMessage`() {
        val message = raygunMessageBuilder.build()

        assertNotNull(message)
        assertNotNull(message.details)
        assertNotNull(message.occurredOn)
    }

    @Test
    fun `test setException sets exception details correctly`() {
        val exception = RuntimeException("Test exception")
        val message = raygunMessageBuilder.setExceptionDetails(exception).build()

        assertNotNull(message.details.error)
        assertEquals("RuntimeException: Test exception", message.details.error?.message)
        assertNotNull(message.details.error?.stackTrace)
    }

    @Test
    fun `test setTags adds tags correctly`() {
        val tags = listOf("critical", "ui", "crash")
        val message = raygunMessageBuilder.setTags(tags).build()

        assertEquals(tags, message.details.tags)
    }

    @Test
    fun `test setCustomData adds custom data correctly`() {
        val customData =
            mapOf("userId" to "12345", "screenName" to "LoginScreen", "apiVersion" to "v2")

        val message = raygunMessageBuilder.setCustomData(customData).build()

        assertEquals(customData, message.details.customData)
    }

    @Test
    fun `test setUser sets user information correctly`() {
        val mockUser = mock<RaygunUserInfo>()
        val message = raygunMessageBuilder.setUserInfo(mockUser).build()

        assertEquals(mockUser, message.details.userInfo)
    }

    @Test
    fun `test setAppContext adds application context correctly`() {
        val version = "1.2.3"

        val message = raygunMessageBuilder.setVersion(version).build()

        assertEquals(version, message.details.version)
    }

    @Test
    fun `test setEnvironmentDetails sets environment details correctly`() {
        runBlocking {
            val mockContext = Mockito.mock<Context>(Context::class.java)

            val message = raygunMessageBuilder.setEnvironmentDetails(mockContext).build()

            assertNotNull(message.details.environment)
        }
    }

    @Test
    fun `test chaining multiple methods works correctly`() {
        val exception = IllegalArgumentException("Invalid parameter")
        val tags = listOf("validation", "input")
        val customData = mapOf("field" to "email", "value" to "invalid")

        val message =
            raygunMessageBuilder
                .setExceptionDetails(exception)
                .setTags(tags)
                .setCustomData(customData)
                .build()

        assertEquals("IllegalArgumentException: Invalid parameter", message.details.error?.message)
        assertEquals(tags, message.details.tags)
        assertEquals(customData, message.details.customData)
    }
}
