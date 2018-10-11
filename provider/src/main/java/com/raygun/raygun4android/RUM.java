package com.raygun.raygun4android;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

import com.raygun.raygun4android.network.RaygunNetworkLogger;

public class RUM implements ActivityLifecycleCallbacks {
  private static RUM rum;
  private static Activity mainActivity;
  private static Activity currentActivity;
  private static Activity loadingActivity;
  private static long startTime;

  protected static void attach(Activity mainActivity) {
    if (rum.rum == null && mainActivity != null) {
      Application application = mainActivity.getApplication();

      if (application != null) {
        rum.mainActivity = mainActivity;
        rum.currentActivity = mainActivity;
        rum.startTime = System.nanoTime();

        rum.rum = new RUM();
        application.registerActivityLifecycleCallbacks(rum.rum);

        RaygunClient.sendPulseEvent(RaygunSettings.RUM_EVENT_SESSION_START);
        RaygunNetworkLogger.init();
      }
    }
  }

  protected static void attach(Activity mainActivity, boolean networkLogging) {
    RaygunNetworkLogger.setEnabled(networkLogging);
    attach(mainActivity);
  }

  protected static void detach() {
    if (rum.rum != null && rum.mainActivity != null && rum.mainActivity.getApplication() != null) {
      rum.mainActivity.getApplication().unregisterActivityLifecycleCallbacks(rum.rum);
      rum.mainActivity = null;
      rum.currentActivity = null;
      rum.rum = null;
    }
  }

  protected static void sendRemainingActivity() {
    if (rum.rum != null) {
      if (rum.loadingActivity != null) {
        String activityName = getActivityName(rum.loadingActivity);

        long diff = System.nanoTime() - rum.startTime;
        long duration = TimeUnit.NANOSECONDS.toMillis(diff);
        RaygunClient.sendPulseTimingEvent(RaygunPulseEventType.ACTIVITY_LOADED, activityName, duration);
      }
      RaygunClient.sendPulseEvent(RaygunSettings.RUM_EVENT_SESSION_END);
    }
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {
    if (rum.currentActivity == null) {
      RaygunClient.sendPulseEvent(RaygunSettings.RUM_EVENT_SESSION_START);
    }

    if (activity != rum.currentActivity) {
      rum.currentActivity = activity;
      rum.loadingActivity = activity;
      rum.startTime = System.nanoTime();
    }
  }

  @Override
  public void onActivityStarted(Activity activity) {
    if (rum.currentActivity == null) {
      RaygunClient.sendPulseEvent(RaygunSettings.RUM_EVENT_SESSION_START);
    }

    if (activity != rum.currentActivity) {
      rum.currentActivity = activity;
      rum.loadingActivity = activity;
      rum.startTime = System.nanoTime();
    }
  }

  @Override
  public void onActivityResumed(Activity activity) {
    if (rum.currentActivity == null) {
      RaygunClient.sendPulseEvent(RaygunSettings.RUM_EVENT_SESSION_START);
    }

    String activityName = getActivityName(activity);
    long duration = 0;

    if (activity == rum.currentActivity) {
      long diff = System.nanoTime() - rum.startTime;
      duration = TimeUnit.NANOSECONDS.toMillis(diff);
    }

    rum.currentActivity = activity;
    rum.loadingActivity = null;

    RaygunClient.sendPulseTimingEvent(RaygunPulseEventType.ACTIVITY_LOADED, activityName, duration);
  }

  @Override
  public void onActivityPaused(Activity activity) {
  }

  @Override
  public void onActivityStopped(Activity activity) {
    if (activity == rum.currentActivity) {
      rum.currentActivity = null;
      rum.loadingActivity = null;
      RaygunClient.sendPulseEvent(RaygunSettings.RUM_EVENT_SESSION_END);
    }
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
  }

  private static String getActivityName(Activity activity) {
    return activity.getClass().getSimpleName();
  }
}
