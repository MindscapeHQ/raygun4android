package com.raygun.raygun4android.sample

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raygun.raygun4android.network.RaygunNetworkUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RaygunNetworkUtilsTest {
    @Test
    fun deviceUuidIsPersistedAndReused() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val preferences = context.getSharedPreferences("device_id.xml", Context.MODE_PRIVATE)
            assertTrue(preferences.edit().clear().commit())

            try {
                val firstUuid = RaygunNetworkUtils.getDeviceUuid(context)

                assertEquals(firstUuid, preferences.getString("device_id", null))
                assertEquals(firstUuid, RaygunNetworkUtils.getDeviceUuid(context))
            } finally {
                assertTrue(preferences.edit().clear().commit())
            }
        }
}
