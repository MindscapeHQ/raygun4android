package com.raygun.raygun4android.messages.crashreporting

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import com.raygun.raygun4android.logging.RaygunLogger
import com.raygun.raygun4android.network.ConnectivityUtils
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
            val networkConnectivityState = ConnectivityUtils.networkConnectivityState(context)
            return NetworkInfo(addresses, networkConnectivityState)
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
