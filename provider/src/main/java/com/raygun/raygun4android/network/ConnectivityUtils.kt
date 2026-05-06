package com.raygun.raygun4android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities

// Provides methods to check network connectivity and get network type.
// minSdk is 23 (Android M), so the legacy NetworkInfo-based API path
// has been removed.
object ConnectivityUtils {
    // Returns true when the device is connected to a network
    fun isNetworkAvailable(context: Context): Boolean {
        val networkCapabilities = networkCapabilities(context)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ?: false
    }

    // Returns the current network type (e.g., WiFi, Mobile, etc.)
    // otherwise return "Not connected" if not connected to a network
    fun networkConnectivityState(context: Context): String {
        if (!isNetworkAvailable(context)) {
            return "Not connected"
        }
        return readNetworkConnectivityState(context)
    }

    // Note: this returns null in unit tests
    private fun connectivityManager(context: Context): ConnectivityManager? =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager?

    private fun currentNetwork(context: Context): Network? = connectivityManager(context)?.activeNetwork

    private fun networkCapabilities(context: Context): NetworkCapabilities? {
        val connectivityManager = connectivityManager(context)
        val currentNetwork = currentNetwork(context)
        return connectivityManager?.getNetworkCapabilities(currentNetwork)
    }

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
    private fun readNetworkConnectivityState(context: Context): String {
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
}
