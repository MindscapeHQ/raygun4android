package com.raygun.raygun4android.utils

import com.raygun.raygun4android.RaygunSettings
import java.io.File
import java.io.FileFilter

class RaygunFileFilter : FileFilter {
    override fun accept(pathname: File): Boolean {
        val extension = "." + RaygunSettings.DEFAULT_FILE_EXTENSION
        return pathname.name.lowercase().endsWith(extension)
    }
}
