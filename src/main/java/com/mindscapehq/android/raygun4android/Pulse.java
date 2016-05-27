package main.java.com.mindscapehq.android.raygun4android;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class Pulse implements ActivityLifecycleCallbacks {
  private static Pulse _pulse;
  private static Activity _mainActivity;
  private static Activity _currentActivity;
  private static long _startTime;

  protected static void Attach(Activity mainActivity) {
    if (_pulse == null && mainActivity != null) {
      Application application = mainActivity.getApplication();
      if (application != null) {
        _mainActivity = mainActivity;
        _pulse = new Pulse();
        application.registerActivityLifecycleCallbacks(_pulse);

        RaygunClient.SendPulseEvent("session_start");
        _currentActivity = _mainActivity;
        _startTime = System.nanoTime();
      }
    }
  }

  protected static void Detach() {
    if(_pulse != null && _mainActivity != null && _mainActivity.getApplication() != null) {
      _mainActivity.getApplication().unregisterActivityLifecycleCallbacks(_pulse);
      _mainActivity = null;
      _currentActivity = null;
      _pulse = null;
    }
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {
    if(_currentActivity == null) {
      RaygunClient.SendPulseEvent("session_start");
    }

    if(activity != _currentActivity) {
      _currentActivity = activity;
      _startTime = System.nanoTime();
    }
    System.out.println("CREATED");
  }

  @Override
  public void onActivityStarted(Activity activity) {
    if(_currentActivity == null) {
      RaygunClient.SendPulseEvent("session_start");
    }

    if(activity != _currentActivity) {
      _currentActivity = activity;
      _startTime = System.nanoTime();
    }
    System.out.println("STARTED");
  }

  @Override
  public void onActivityResumed(Activity activity) {
    if(_currentActivity == null) {
      RaygunClient.SendPulseEvent("session_start");
    }

    String activityName = getActivityName(activity);
    double duration = 0;
    if(activity == _currentActivity) {
      long diff = System.nanoTime() - _startTime;
      duration = TimeUnit.NANOSECONDS.toMillis(diff);
    }
    _currentActivity = activity;

    RaygunClient.SendPulsePageTimingEvent(activityName, duration);
    System.out.println("RESUMED");
  }

  @Override
  public void onActivityPaused(Activity activity) {
    //System.out.println("PAUSED");
  }

  @Override
  public void onActivityStopped(Activity activity) {
    if(activity == _currentActivity) {
      _currentActivity = null;
      RaygunClient.SendPulseEvent("session_end");
    }
    System.out.println("STOPPED");
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    //System.out.println("SAVE");
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    //System.out.println("DESTROYED");
  }

  private static String getActivityName(Activity activity) {
    return activity.getClass().getSimpleName();
  }
}
