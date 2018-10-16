package com.raygun.raygun4android.utils;

import android.content.Context;

import com.raygun.raygun4android.RaygunSettings;
import java.io.File;

public class RaygunFileUtils {

    public static String getExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int separator = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        int dotPos = filename.lastIndexOf(".");
        int index = separator > dotPos ? -1 : dotPos;
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    public static void clearCachedReports(Context context) {
        synchronized(RaygunFileUtils.class) {
            final File[] fileList = context.getCacheDir().listFiles(new RaygunFileFilter());

            for (File f : fileList) {
                if (RaygunFileUtils.getExtension(f.getName()).equalsIgnoreCase(RaygunSettings.DEFAULT_FILE_EXTENSION)) {
                    f.delete();
                }
            }
        }
    }
}
