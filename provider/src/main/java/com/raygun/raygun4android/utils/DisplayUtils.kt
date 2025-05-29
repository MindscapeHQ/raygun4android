package com.raygun.raygun4android.utils

import android.content.Context
import android.content.res.Configuration

object DisplayUtils {
    data class Resolution(
        val width: Int,
        val height: Int,
    )

    /**
     * Get screen resolution of the device display metrics
     *
     * @return Resolution object containing width and height in pixels
     */
    fun getResolution(applicationContext: Context): Resolution {
        val displayMetrics = applicationContext.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        return Resolution(width, height)
    }

    /**
     * Get screen orientation as a String
     *
     * @return "Portrait", "Landscape", or "Undefined"
     */
    fun getOrientation(applicationContext: Context): String {
        val orientation = applicationContext.resources.configuration.orientation
        return when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> "Portrait"
            Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
            else -> "Undefined"
        }
    }
}
