package com.raygun.raygun4android.utils

import android.content.Context
import android.content.res.Configuration

data class Resolution(
    val width: Int,
    val height: Int,
)

object DisplayUtils {
    // Returns the screen resolution of the device display metrics
    fun getResolution(applicationContext: Context): Resolution {
        val displayMetrics = applicationContext.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        return Resolution(width, height)
    }

    // Returns the screen orientation as a String
    fun getOrientation(applicationContext: Context): String {
        val orientation = applicationContext.resources.configuration.orientation
        return when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> "Portrait"
            Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
            else -> "Undefined"
        }
    }
}
