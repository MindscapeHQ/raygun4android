package com.raygun.raygun4android.messages.shared

import android.content.Context
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.network.RaygunNetworkUtils
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito

class RaygunUserInfoTest {
    private lateinit var mockRaygunClient: MockedStatic<RaygunClient>
    private lateinit var mockRaygunNetworkUtils: MockedStatic<RaygunNetworkUtils>

    @Before
    fun setUp() {
        mockRaygunClient = Mockito.mockStatic(RaygunClient::class.java)
        mockRaygunNetworkUtils = Mockito.mockStatic(RaygunNetworkUtils::class.java)

        mockRaygunClient
            .`when`<Context> { RaygunClient.getApplicationContext() }
            .thenReturn(Mockito.mock(android.content.Context::class.java))

        mockRaygunNetworkUtils
            .`when`<String> { RaygunNetworkUtils.getDeviceUuid(Mockito.any()) }
            .thenReturn("mock-uuid")
    }

    @After
    fun tearDown() {
        mockRaygunClient.close()
        mockRaygunNetworkUtils.close()
    }

    @Test
    fun `test create anonymous user`() {
        val user = RaygunUserInfo()

        assertTrue(user.isAnonymous)
        assertEquals("mock-uuid", user.identifier)
        assertNull(user.email)
        assertNull(user.fullName)
        assertNull(user.firstName)
    }

    @Test
    fun `test create user with identifier`() {
        val user = RaygunUserInfo("user123")

        assertFalse(user.isAnonymous)
        assertEquals("user123", user.identifier)
        assertNull(user.email)
        assertNull(user.fullName)
        assertNull(user.firstName)
    }

    @Test
    fun `test create user with full details`() {
        val user =
            RaygunUserInfo(
                identifier = "user123",
                firstName = "John",
                fullName = "John Doe",
                email = "john.doe@example.com",
            )

        assertFalse(user.isAnonymous)
        assertEquals("user123", user.identifier)
        assertEquals("John", user.firstName)
        assertEquals("John Doe", user.fullName)
        assertEquals("john.doe@example.com", user.email)
    }

    @Test
    fun `test set identifier for anonymous user`() {
        val user = RaygunUserInfo()
        user.setIdentifier("new-identifier")

        assertFalse(user.isAnonymous)
        assertEquals("new-identifier", user.identifier)
    }

    @Test
    fun `test set email for anonymous user is ignored`() {
        val user = RaygunUserInfo()
        user.email = "anonymous@example.com"

        assertNull(user.email)
    }

    @Test
    fun `test set email for non-anonymous user`() {
        val user = RaygunUserInfo("user123")
        user.email = "user@example.com"

        assertEquals("user@example.com", user.email)
    }

    @Test
    fun `test set fullName for anonymous user is ignored`() {
        val user = RaygunUserInfo()
        user.fullName = "Anonymous User"

        assertNull(user.fullName)
    }

    @Test
    fun `test set fullName for non-anonymous user`() {
        val user = RaygunUserInfo("user123")
        user.fullName = "John Doe"

        assertEquals("John Doe", user.fullName)
    }

    @Test
    fun `test set firstName for anonymous user is ignored`() {
        val user = RaygunUserInfo()
        user.firstName = "Anonymous"

        assertNull(user.firstName)
    }

    @Test
    fun `test set firstName for non-anonymous user`() {
        val user = RaygunUserInfo("user123")
        user.firstName = "John"

        assertEquals("John", user.firstName)
    }
}
