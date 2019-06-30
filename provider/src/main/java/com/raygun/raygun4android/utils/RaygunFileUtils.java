package com.raygun.raygun4android.utils;

import android.content.Context;

import com.raygun.raygun4android.RaygunSettings;
import com.raygun.raygun4android.logging.RaygunLogger;

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
            if (fileList != null) {
                for (File f : fileList) {
                    if (RaygunFileUtils.getExtension(f.getName()).equalsIgnoreCase(RaygunSettings.DEFAULT_FILE_EXTENSION)) {
                        if (!f.delete()) {
                            RaygunLogger.w("Couldn't delete cached report (" + f.getName() + ")");
                        }
                    }
                }
            } else {
                RaygunLogger.e("Error in handling cached message from filesystem - could not get a list of files from cache dir");
            }

        }
    }
}
