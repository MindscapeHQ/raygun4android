package com.raygun.raygun4android.network

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.UUID

internal interface UuidProvider {
    suspend fun getDeviceUuid(context: Context): String
}

object RaygunNetworkUtils {
    private const val PREFS_FILE = "device_id.xml"
    private const val PREFS_DEVICE_ID = "device_id"

    fun getStatusCode(urlConnection: URLConnection?): Int {
        var statusCode = 0
        if (urlConnection != null) {
            if ((urlConnection is HttpURLConnection)) {
                try {
                    statusCode = urlConnection.responseCode
                } catch (ignore: Exception) {
                }
            }
        }
        return statusCode
    }

    suspend fun getDeviceUuid(context: Context): String = uuidProvider.getDeviceUuid(context)

    @VisibleForTesting
    internal var uuidProvider: UuidProvider =
        object : UuidProvider {
            override suspend fun getDeviceUuid(context: Context): String {
                return withContext(Dispatchers.IO) {
                    synchronized(this) {
                        val prefs = context.getSharedPreferences(PREFS_FILE, 0)
                        var id = prefs.getString(PREFS_DEVICE_ID, null)
                        if (id != null) {
                            return@withContext UUID.fromString(id).toString()
                        } else {
                            @SuppressLint("HardwareIds")
                            val androidId =
                                Settings.Secure.getString(
                                    context.contentResolver,
                                    Settings.Secure.ANDROID_ID,
                                )

                            id =
                                if ("9774d56d682e549c" != androidId) {
                                    UUID
                                        .nameUUIDFromBytes(
                                            androidId.toByteArray(StandardCharsets.UTF_8),
                                        ).toString()
                                } else {
                                    UUID.randomUUID().toString()
                                }

                            prefs.edit().putString(PREFS_DEVICE_ID, id).apply()
                            return@withContext id
                        }
                    }
                }
            }
        }
}
