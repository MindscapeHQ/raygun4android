package com.raygun.raygun4android.sample

import android.content.Context
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raygun.raygun4android.network.RaygunNetworkUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RaygunNetworkUtilsTest {
    @Test
    fun deviceUuidIsPersistedToDiskAndReusedFromFreshContext() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val preferences = context.getSharedPreferences("device_id.xml", Context.MODE_PRIVATE)
            assertTrue(preferences.edit().clear().commit())

            try {
                val firstUuid = RaygunNetworkUtils.getDeviceUuid(context)

                assertEquals(firstUuid, preferences.getString("device_id", null))
                assertTrue(uuidWasWrittenToDisk(context, firstUuid))

                val freshContext = context.createPackageContext(context.packageName, 0)
                assertEquals(firstUuid, RaygunNetworkUtils.getDeviceUuid(freshContext))
            } finally {
                assertTrue(preferences.edit().clear().commit())
            }
        }

    private fun uuidWasWrittenToDisk(
        context: Context,
        uuid: String,
    ): Boolean {
        // SharedPreferences appends ".xml" to the supplied preferences name.
        val preferencesFile =
            File(context.applicationInfo.dataDir, "shared_prefs/device_id.xml.xml")

        repeat(100) {
            val persisted =
                runCatching { preferencesFile.readText().contains(uuid) }.getOrDefault(false)
            if (persisted) {
                return true
            }
            SystemClock.sleep(10)
        }

        return false
    }
}
