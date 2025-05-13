@file:Suppress("DEPRECATION")

package com.raygun.raygun4android.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import androidx.annotation.RequiresApi

object ConnectivityUtils {
    fun isNetworkAvailable(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isNetworkAvailable23(context)
        } else {
            isNetworkAvailableLegacy(context)
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
}