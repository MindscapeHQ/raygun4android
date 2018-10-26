package main.java.com.mindscapehq.android.raygun4android.utils;

import main.java.com.mindscapehq.android.raygun4android.RaygunSettings;

import java.io.File;
import java.io.FileFilter;

public class RaygunFileFilter implements FileFilter {
  @Override
  public boolean accept(File pathname) {

    String extension = "." + RaygunSettings.DEFAULT_FILE_EXTENSION;
    if (pathname.getName().toLowerCase().endsWith(extension)) {
      return true;
    }

    return false;
  }
}
