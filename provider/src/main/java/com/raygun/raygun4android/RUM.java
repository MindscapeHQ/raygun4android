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
        if (RUM.rum == null && mainActivity != null) {
            Application application = mainActivity.getApplication();

            if (application != null) {
                RUM.mainActivity = mainActivity;
                RUM.currentActivity = mainActivity;
                RUM.startTime = System.nanoTime();

                RUM.rum = new RUM();
                application.registerActivityLifecycleCallbacks(RUM.rum);

                RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START);
                RaygunNetworkLogger.init();
            }
        }
    }

    protected static void attach(Activity mainActivity, boolean networkLogging) {
        RaygunNetworkLogger.setEnabled(networkLogging);
        attach(mainActivity);
    }

    protected static void detach() {
        if (RUM.rum != null && RUM.mainActivity != null && RUM.mainActivity.getApplication() != null) {
            RUM.mainActivity.getApplication().unregisterActivityLifecycleCallbacks(RUM.rum);
            RUM.mainActivity = null;
            RUM.currentActivity = null;
            RUM.rum = null;
        }
    }

    protected static void sendRemainingActivity() {
        if (RUM.rum != null) {
            if (RUM.loadingActivity != null) {
                String activityName = getActivityName(RUM.loadingActivity);

                long diff = System.nanoTime() - RUM.startTime;
                long duration = TimeUnit.NANOSECONDS.toMillis(diff);
                RaygunClient.sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration);
            }
            RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (RUM.currentActivity == null) {
            RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START);
        }

        if (activity != RUM.currentActivity) {
            RUM.currentActivity = activity;
            RUM.loadingActivity = activity;
            RUM.startTime = System.nanoTime();
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (RUM.currentActivity == null) {
            RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START);
        }

        if (activity != RUM.currentActivity) {
            RUM.currentActivity = activity;
            RUM.loadingActivity = activity;
            RUM.startTime = System.nanoTime();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (RUM.currentActivity == null) {
            RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START);
        }

        String activityName = getActivityName(activity);
        long duration = 0;

        if (activity == RUM.currentActivity) {
            long diff = System.nanoTime() - RUM.startTime;
            duration = TimeUnit.NANOSECONDS.toMillis(diff);
        }

        RUM.currentActivity = activity;
        RUM.loadingActivity = null;

        RaygunClient.sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration);
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity == RUM.currentActivity) {
            RUM.currentActivity = null;
            RUM.loadingActivity = null;
            RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END);
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
