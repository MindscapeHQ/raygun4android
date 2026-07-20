package com.raygun.raygun4android.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

// Utils class to obtain the mobile network type using the TelephonyManager API.
// Requires READ_PHONE_STATE permission in app, otherwise it will return "Unknown".
object TelephonyUtils {
    private const val UNKNOWN = "Unknown"

    // Obtains the mobile network type as a string.
    // Requires permission READ_PHONE_STATE.
    // Returns "Unknown" if permission is not granted or if the network type cannot be determined.
    @SuppressLint("MissingPermission")
    fun getNetworkType(context: Context): String {
        val permissionCheck = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            return UNKNOWN
        }
        val type =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                networkType24(context)
            } else {
                networkTypeLegacy(context)
            }
        return networkTypeToString(type)
    }

    private fun telephonyManager(context: Context): TelephonyManager =
        context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @Suppress("DEPRECATION")
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun networkTypeLegacy(context: Context): Int = telephonyManager(context).networkType

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    @RequiresApi(android.os.Build.VERSION_CODES.N)
    private fun networkType24(context: Context): Int = telephonyManager(context).dataNetworkType

    // The CDMA-era NETWORK_TYPE_* constants below (1xRTT, CDMA, EHRPD,
    // EVDO_0/A/B) were deprecated in API 30 but are intentionally kept in
    // the mapping so that crash report payloads from older devices that
    // still report these values render correctly.
    @Suppress("DEPRECATION")
    fun networkTypeToString(networkType: Int): String =
        when (networkType) {
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO rev. 0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO rev. A"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO rev. B"
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            else -> UNKNOWN
        }
}
