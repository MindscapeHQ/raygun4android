package com.raygun.raygun4android.utils;

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
}
