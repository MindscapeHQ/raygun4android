package com.raygun.raygun4android.messages.crashreporting

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import com.raygun.raygun4android.logging.RaygunLogger
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

data class NetworkInfo(
    var iPAddress: List<String> = ArrayList(),
    var networkConnectivityState: String,
) {
    companion object {
        operator fun invoke(context: Context): NetworkInfo {
            val addresses = readIPAddress()
            val networkConnectivityState = readNetworkConnectivityState(context)
            return NetworkInfo(addresses, networkConnectivityState)
        }

        private fun readNetworkConnectivityState(context: Context): String {
            var result = "Not connected"

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val info = cm.activeNetworkInfo

            if (info != null) {
                if (info.isConnected) {
                    result = "Connected - "

                    val type = info.type

                    when (type) {
                        ConnectivityManager.TYPE_WIFI -> result += "WiFi"
                        ConnectivityManager.TYPE_WIMAX -> result += "WiMax"
                        ConnectivityManager.TYPE_MOBILE,
                        ConnectivityManager.TYPE_MOBILE_DUN,
                        ConnectivityManager.TYPE_MOBILE_HIPRI,
                        ConnectivityManager.TYPE_MOBILE_MMS,
                        ConnectivityManager.TYPE_MOBILE_SUPL,
                        -> {
                            result += "Mobile - "

                            val subtype = info.subtype
                            result +=
                                when (subtype) {
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
            }

            return result
        }

        private fun readIPAddress(): List<String> {
            val iPAddress: MutableList<String> = ArrayList()
            try {
                val interfaces: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())

                for (intf in interfaces) {
                    val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)

                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val isIPv4 = addr is Inet4Address
                            addr.hostAddress?.uppercase()?.let {
                                if (isIPv4) {
                                    if (!iPAddress.contains(it)) {
                                        iPAddress.add(it)
                                    }
                                } else {
                                    val delim = it.indexOf('%') // drop ip6 port suffix
                                    val delimited = if (delim < 0) it else it.substring(0, delim)
                                    if (!iPAddress.contains(delimited)) {
                                        iPAddress.add(delimited)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                RaygunLogger.w("Couldn't get IPs: $ex")
            }
            return iPAddress
        }
    }
}
