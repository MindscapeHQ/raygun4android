@file:Suppress("DEPRECATION")

package com.raygun.raygun4android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import timber.log.Timber

object ConnectivityUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isNetworkAvailable23(context)
        } else {
            isNetworkAvailableLegacy(context)
        }
    }

    fun networkConnectivityState(context: Context): String {
        if (!isNetworkAvailable(context)) {
            return "Not connected"
        }
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            readNetworkConnectivityState(context)
        } else {
            readNetworkConnectivityStateLegacy(context)
        }
    }

    private fun connectivityManager(context: Context): ConnectivityManager {
        return context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private fun currentNetworkInfoLegacy(context: Context): NetworkInfo? {
        return connectivityManager(context).activeNetworkInfo
    }

    private fun isNetworkAvailableLegacy(context: Context): Boolean {
        return currentNetworkInfoLegacy(context)?.isConnected ?: false
    }

    @RequiresApi(android.os.Build.VERSION_CODES.M)
    private fun currentNetwork(context: Context): Network? {
        return connectivityManager(context).activeNetwork
    }

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
    private fun readNetworkConnectivityState(context: Context): String {
        var result = "Connected - "
        networkCapabilities(context)?.let { capabilities ->
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> result += "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    result += "Mobile - "
                    val linkProperties = linkProperties(context)
                    if (linkProperties != null) {
                        result += linkProperties.interfaceName
                    }
                }
                else -> result += "unknown type"
            }
        }
        Timber.d("Raygun4", "Network capabilities: $result")
        return result
    }

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