package com.raygun.raygun4android.utils;

import com.raygun.raygun4android.RaygunSettings;

import java.io.File;
import java.io.FileFilter;

public class RaygunFileFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {

        String extension = "." + RaygunSettings.DEFAULT_FILE_EXTENSION;
        return pathname.getName().toLowerCase().endsWith(extension);

    }
}
