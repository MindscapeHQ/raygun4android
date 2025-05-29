package com.raygun.raygun4android.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

object LocaleUtils {
    /**
     * Gets the current locale of the device.
     *
     * @param context Application or Activity context
     * @return The current locale as a string.
     */
    fun getLocale(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getLocale24(context)
        } else {
            getLocaleLegacy(context)
        }

    @Suppress("DEPRECATION")
    private fun getLocaleLegacy(context: Context): String =
        context.resources.configuration.locale
            .toString()

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getLocale24(context: Context): String =
        context.resources.configuration.locales[0]
            .toString()
}
