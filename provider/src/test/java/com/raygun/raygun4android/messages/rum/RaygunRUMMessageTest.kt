package com.raygun.raygun4android.messages.rum

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.RaygunRUMEventType
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.messages.shared.RaygunUserInfo
import com.raygun.raygun4android.network.RaygunNetworkUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito

class RaygunRUMMessageTest {
    private lateinit var message: RaygunRUMMessage
    private lateinit var data: RaygunRUMData
    private lateinit var mockRaygunClient: MockedStatic<RaygunClient>
    private lateinit var mockRaygunNetworkUtils: MockedStatic<RaygunNetworkUtils>

    @Before
    fun setup() {
        mockRaygunClient = Mockito.mockStatic(RaygunClient::class.java)
        mockRaygunNetworkUtils = Mockito.mockStatic(RaygunNetworkUtils::class.java)

        mockRaygunClient
            .`when`<Context> { RaygunClient.getApplicationContext() }
            .thenReturn(Mockito.mock(android.content.Context::class.java))

        mockRaygunNetworkUtils
            .`when`<String> { RaygunNetworkUtils.getDeviceUuid(Mockito.any()) }
            .thenReturn("mock-uuid")
        val userInfo = RaygunUserInfo(identifier = "123")
        val dataMessage: RaygunRUMDataMessage =
            RaygunRUMDataMessage.Builder(RaygunSettings.RUM_EVENT_TIMING)
                .timestamp("2023-10-01T12:00:00").sessionId("123")
                .version(RaygunClient.getVersion()).os("Android").osVersion(Build.VERSION.RELEASE)
                .platform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL)).user(userInfo)
                .data("DATA").build()
        message = RaygunRUMMessage()
        message.eventData = arrayOf(
            dataMessage
        )

        val timingMessage =
            RaygunRUMTimingMessage.Builder(
                "p"
            )
                .duration(1234)
                .build()

        data = RaygunRUMData.Builder("name").timing(timingMessage).build()
    }

    @After
    fun tearDown() {
        mockRaygunClient.close()
        mockRaygunNetworkUtils.close()
    }

    @Test
    fun `test serialization and deserialization of data`() {
        val serialized = Gson().toJson(data)!!
        val deserialized = Gson().fromJson(serialized, RaygunRUMData::class.java)

        // Deserialized data should be equal to the original data
        assertEquals(data, deserialized)
    }

    @Test
    fun `test serialization and deserialization of message`() {
        val serialized = Gson().toJson(message)!!
        val deserialized = Gson().fromJson(serialized, RaygunRUMMessage::class.java)

        // Deserialized message should be equal to the original message
        assertEquals(message, deserialized)
    }
}
