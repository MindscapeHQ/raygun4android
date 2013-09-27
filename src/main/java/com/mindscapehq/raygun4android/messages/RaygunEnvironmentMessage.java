package main.java.com.mindscapehq.raygun4android.messages;

import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;
import java.util.Date;
import java.util.TimeZone;

public class RaygunEnvironmentMessage {

  private String cpu;
  private String architecture;
  private int processorCount;
  private String osVersion;
  private int windowBoundsWidth;
  private int windowBoundsHeight;
  private String currentOrientation;
  private String locale;
  private long totalPhysicalMemory;
  private long availablePhysicalMemory;
  private long totalVirtualMemory;
  private long availableVirtualMemory;
  private int diskSpaceFree;
  private double utcOffset;

  public RaygunEnvironmentMessage(Context context)
  {
    try {
      /*
      availablePhysicalMemory = sunMxBean.getFreePhysicalMemorySize();
      totalPhysicalMemory = sunMxBean.getTotalPhysicalMemorySize();
      totalVirtualMemory = sunMxBean.getTotalSwapSpaceSize();
      availableVirtualMemory = sunMxBean.getFreeSwapSpaceSize();*/

      architecture = Build.CPU_ABI;
      osVersion = Build.VERSION.RELEASE;

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
      windowBoundsWidth = d.getWidth();
      windowBoundsHeight = d.getHeight();

      TimeZone tz = TimeZone.getDefault();
      Date now = new Date();
      utcOffset = tz.getOffset(now.getTime());

      locale = context.getResources().getConfiguration().locale.toString();

    } catch (Exception e) {
    }

  }
}
