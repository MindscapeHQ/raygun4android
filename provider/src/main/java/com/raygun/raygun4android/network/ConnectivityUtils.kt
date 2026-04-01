package com.raygun.raygun4android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
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

    // Note: this returns null in unit tests
    private fun connectivityManager(context: Context): ConnectivityManager? =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager?

    @Suppress("DEPRECATION")
    private fun currentNetworkInfoLegacy(context: Context): NetworkInfo? = connectivityManager(context)?.activeNetworkInfo

    @Suppress("DEPRECATION")
    private fun isNetworkAvailableLegacy(context: Context): Boolean = currentNetworkInfoLegacy(context)?.isConnected ?: false

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun currentNetwork(context: Context): Network? = connectivityManager(context)?.activeNetwork

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
        return connectivityManager?.getNetworkCapabilities(currentNetwork)
    }

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun linkProperties(context: Context): LinkProperties? {
        val connectivityManager = connectivityManager(context)
        val currentNetwork = currentNetwork(context)
        return connectivityManager?.getLinkProperties(currentNetwork)
    }

    // Obtain a string representing the connectivity state.
    // e.g. "Connected - VPN - WiFi - tun0" or "Connected - WiFi - wlan0"
    // From the Android documentation:
    // On Android, a network can have multiple transports at the same time.
    // An example of this is a VPN operating over both Wi-Fi and mobile networks.
    // See
    // https://developer.android.com/develop/connectivity/network-ops/reading-network-state#introducing-net-capabilities
    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun readNetworkConnectivityState23(context: Context): String {
        var result = "Connected"
        networkCapabilities(context)?.let { capabilities ->
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                result += " - VPN"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                result += " - WiFi"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) {
                result += " - WiFi Aware"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)) {
                result += " - Low pan"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                result += " - Ethernet"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                result += " - Bluetooth"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB)) {
                result += " - USB"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_SATELLITE)) {
                result += " - Satellite"
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                result += " - Mobile"
                // Note: getNetworkType requires READ_PHONE_STATE permission
                // without it the result is always "unknown"
                result += " - " + TelephonyUtils.getNetworkType(context)
            }

            // Attaches LinkProperties.interfaceName to the result if available
            // e.g. wlan0
            linkProperties(context)?.interfaceName?.let {
                if (it.isNotEmpty()) {
                    result += " - $it"
                }
            }
        }
        return result
    }

    // Obtain a string representing the connectivity state.
    // e.g. "Connected - WiFi" or "Connected - Mobile - LTE"
    // Uses the old API (pre-23) to get the network type.
    // Only one network type is returned, and does not support reporting on VPN
    // or other transport types.
    @Suppress("DEPRECATION")
    private fun readNetworkConnectivityStateLegacy(context: Context): String {
        var result = "Connected - "
        currentNetworkInfoLegacy(context)?.let { info ->
            when (info.type) {
                ConnectivityManager.TYPE_WIFI -> {
                    result += "WiFi"
                }

                ConnectivityManager.TYPE_WIMAX -> {
                    result += "WiMax"
                }

                ConnectivityManager.TYPE_MOBILE,
                ConnectivityManager.TYPE_MOBILE_DUN,
                ConnectivityManager.TYPE_MOBILE_HIPRI,
                ConnectivityManager.TYPE_MOBILE_MMS,
                ConnectivityManager.TYPE_MOBILE_SUPL,
                -> {
                    result += "Mobile - "
                    result += TelephonyUtils.networkTypeToString(info.subtype)
                }

                else -> {
                    result += "unknown type"
                }
            }
        }
        return result
    }
}
