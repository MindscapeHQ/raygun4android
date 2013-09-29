package main.java.com.mindscapehq.raygun4android.messages;

import android.app.ActivityManager;
import android.content.Context;
import android.os.*;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaygunEnvironmentMessage {

  private String cpu;
  private String architecture;
  private int processorCount;
  private String oSVersion;
  private String osSDKVersion;
  private int windowsBoundWidth;
  private int windowsBoundHeight;
  private String currentOrientation;
  private String locale;
  private long totalPhysicalMemory;
  private long availablePhysicalMemory;
  private long totalVirtualMemory;
  private long availableVirtualMemory;
  private int diskSpaceFree;
  private double utcOffset;
  private String deviceName;
  private String brand;
  private String board;
  private String deviceCode;

  public RaygunEnvironmentMessage(Context context)
  {
    try {
      architecture = Build.CPU_ABI;
      oSVersion = Build.VERSION.RELEASE;
      osSDKVersion = Build.VERSION.SDK;
      deviceName = Build.MODEL;
      deviceCode = Build.DEVICE;
      brand = Build.BRAND;
      board = Build.BOARD;

      processorCount = Runtime.getRuntime().availableProcessors();

      int orientation = context.getResources().getConfiguration().orientation;
      if (orientation == 1)
      {
        currentOrientation = "Portrait";
      }
      else if (orientation == 2)
      {
        currentOrientation = "Landscape";
      }
      else if (orientation == 3)
      {
        currentOrientation = "Square";
      }
      else
      {
        currentOrientation = "Undefined";
      }

      WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      Display d = wm.getDefaultDisplay();
      windowsBoundWidth = d.getWidth();
      windowsBoundHeight = d.getHeight();

      TimeZone tz = TimeZone.getDefault();
      Date now = new Date();
      utcOffset = TimeUnit.HOURS.convert(tz.getOffset(now.getTime()), TimeUnit.MILLISECONDS);
      locale = context.getResources().getConfiguration().locale.toString();

      ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
      ActivityManager am = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
      am.getMemoryInfo(mi);
      availablePhysicalMemory = mi.availMem / 0x100000;

      Pattern p = Pattern.compile("^\\D*(\\d*).*$");
      Matcher m = p.matcher(getTotalRam());
      m.find();
      String match = m.group(1);
      totalPhysicalMemory = Long.parseLong(match) / 0x400;

      StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
      diskSpaceFree = (stat.getAvailableBlocks() * stat.getBlockSize()) / 0x100000;
    } catch (Exception e) {
      Log.w("Raygun4Android", "Couldn't get all env data: " + e);
    }
  }

  private String getTotalRam() throws IOException {
    RandomAccessFile reader = null;
    String load = null;
    try {
      reader = new RandomAccessFile("/proc/meminfo", "r");
      load = reader.readLine();
    } finally {
      reader.close();
    }
    return load;
  }
}
