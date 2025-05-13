package com.raygun.raygun4android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

// Provides methods to check network connectivity and get network type.
// Handles both pre-23 and post-23 APIs. The SDK minimum API is 21.
// Once we increase the minimum API to 23, we can remove the legacy methods.
object ConnectivityUtils {
    // Returns true when the device is connected to a network
    fun isNetworkAvailable(context: Context): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isNetworkAvailable23(context)
        } else {
            isNetworkAvailableLegacy(context)
        }

    // Returns the current network type (e.g., WiFi, Mobile, etc.)
    // otherwise return "Not connected" if not connected to a network
    fun networkConnectivityState(context: Context): String {
        if (!isNetworkAvailable(context)) {
            return "Not connected"
        }
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            readNetworkConnectivityState23(context)
        } else {
            readNetworkConnectivityStateLegacy(context)
        }
    }

    private fun connectivityManager(context: Context): ConnectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    @Suppress("DEPRECATION")
    private fun currentNetworkInfoLegacy(context: Context): NetworkInfo? = connectivityManager(context).activeNetworkInfo

    @Suppress("DEPRECATION")
    private fun isNetworkAvailableLegacy(context: Context): Boolean = currentNetworkInfoLegacy(context)?.isConnected ?: false

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun currentNetwork(context: Context): Network? = connectivityManager(context).activeNetwork

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun isNetworkAvailable23(context: Context): Boolean {
        val networkCapabilities = networkCapabilities(context)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false
    }

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun networkCapabilities(context: Context): NetworkCapabilities? {
        val connectivityManager = connectivityManager(context)
        val currentNetwork = currentNetwork(context)
        return connectivityManager.getNetworkCapabilities(currentNetwork)
    }

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun linkProperties(context: Context): LinkProperties? {
        val connectivityManager = connectivityManager(context)
        val currentNetwork = currentNetwork(context)
        return connectivityManager.getLinkProperties(currentNetwork)
    }

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun readNetworkConnectivityState23(context: Context): String {
        var result = "Connected - "
        networkCapabilities(context)?.let { capabilities ->
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> result += "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    result += "Mobile - "
                    // Note: getNetworkType requires READ_PHONE_STATE permission
                    // without it the result is always "unknown"
                    result += TelephonyUtils.getNetworkType(context)
                    val linkProperties = linkProperties(context)
                    if (linkProperties != null) {
                        result += " - " + linkProperties.interfaceName
                    }
                }
                else -> result += "unknown type"
            }
        }
        return result
    }

    @Suppress("DEPRECATION")
    private fun readNetworkConnectivityStateLegacy(context: Context): String {
        var result = "Connected - "
        currentNetworkInfoLegacy(context)?.let { info ->
            when (info.type) {
                ConnectivityManager.TYPE_WIFI -> result += "WiFi"
                ConnectivityManager.TYPE_WIMAX -> result += "WiMax"
                ConnectivityManager.TYPE_MOBILE,
                ConnectivityManager.TYPE_MOBILE_DUN,
                ConnectivityManager.TYPE_MOBILE_HIPRI,
                ConnectivityManager.TYPE_MOBILE_MMS,
                ConnectivityManager.TYPE_MOBILE_SUPL,
                -> {
                    result += "Mobile - "
                    // Note: subtype seems to always return TelephonyManager.NETWORK_TYPE_UNKNOWN
                    // probably because this API is deprecated
                    result +=
                        when (info.subtype) {
                            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                            TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
                            TelephonyManager.NETWORK_TYPE_UNKNOWN ->
                                "subtype unknown/EVDO_B/EHRPD/LTE/HSPAP or similar"

                            else -> "subtype unknown/EVDO_B/EHRPD/LTE/HSPAP or similar"
                        }
                }

                else -> result += "unknown type"
            }
        }
        return result
    }
}
