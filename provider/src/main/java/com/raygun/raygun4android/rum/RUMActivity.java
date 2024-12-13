package com.raygun.raygun4android.rum;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.raygun.raygun4android.RaygunRUMEventType;
import com.raygun.raygun4android.logging.RaygunLogger;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class RUMActivity implements Application.ActivityLifecycleCallbacks {
    // Reference to the RUM instance
    private final RUM rum;

    // Each time a new Activity is created, attach to the FragmentManager
    // When the Activity is destroyed, detach from the FragmentManager
    private final RUMFragment rumFragment;

    private WeakReference<Activity> mainActivity;
    private WeakReference<Activity> currentActivity;
    private WeakReference<Activity> loadingActivity;

    // Tracks time spent in the current activity
    private long startTime;

    public RUMActivity(RUM rum, RUMFragment rumFragment) {
        this.rum = rum;
        this.rumFragment = rumFragment;
    }

    public boolean isAttached() {
        return mainActivity != null && mainActivity.get() != null;
    }

    public void attach(Activity mainActivity) {
        this.mainActivity = new WeakReference<>(mainActivity);
        this.currentActivity = new WeakReference<>(mainActivity);

        // Register the ActivityLifecycleCallbacks of the Application
        Application application = mainActivity.getApplication();
        application.registerActivityLifecycleCallbacks(this);

        // Attach to the FragmentManager from Main Activity
        if (mainActivity instanceof FragmentActivity) {
            rumFragment.attach(((FragmentActivity) mainActivity).getSupportFragmentManager());
        }

        startTime = System.nanoTime();
    }

    public void detach() {
        if (mainActivity != null && mainActivity.get() != null) {
            mainActivity.get().getApplication().unregisterActivityLifecycleCallbacks(this);
        }
        mainActivity = null;
        currentActivity = null;
        loadingActivity = null;
    }

    void sendRemaining() {
        if (loadingActivity != null && loadingActivity.get() != null) {
            String activityName = getActivityName(loadingActivity.get());

            long diff = System.nanoTime() - startTime;
            long duration = TimeUnit.NANOSECONDS.toMillis(diff);
            rum.sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        RaygunLogger.v("RUM - Activity created: " + getActivityName(activity));
        if (currentActivity == null || currentActivity.get() == null) {
            rum.maybeRotateSession();
        }
        if (currentActivity == null || currentActivity.get() != activity) {
            currentActivity = new WeakReference<>(activity);
            loadingActivity = new WeakReference<>(activity);
            startTime = System.nanoTime();

            // Attach to the FragmentManager from Current Activity
            if (activity instanceof FragmentActivity) {
                rumFragment.attach(((FragmentActivity) activity).getSupportFragmentManager());
            }
        }
        rum.seen();
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        RaygunLogger.v("RUM - Activity started: " + getActivityName(activity));
        if (currentActivity == null || currentActivity.get() == null) {
            rum.maybeRotateSession();
        }
        if (currentActivity == null || currentActivity.get() != activity) {
            currentActivity = new WeakReference<>(activity);
            loadingActivity = new WeakReference<>(activity);
            startTime = System.nanoTime();
        }
        rum.seen();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        RaygunLogger.v("RUM - Activity resumed: " + getActivityName(activity));
        if (currentActivity == null || currentActivity.get() == null) {
            rum.maybeRotateSession();
        }
        String activityName = getActivityName(activity);
        long duration = 0;
        if (currentActivity != null && activity == currentActivity.get()) {
            long diff = System.nanoTime() - startTime;
            duration = TimeUnit.NANOSECONDS.toMillis(diff);
        }
        currentActivity = new WeakReference<>(activity);
        loadingActivity = null;
        rum.sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration);
        rum.seen();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        RaygunLogger.v("RUM - Activity paused: " + getActivityName(activity));
        rum.seen();
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        RaygunLogger.v("RUM - Activity stopped: " + getActivityName(activity));
        if (currentActivity != null && currentActivity.get() == activity) {
            currentActivity = null;
            loadingActivity = null;
        }
        rum.seen();
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {
        RaygunLogger.v("RUM - onActivitySaveInstanceState: " + getActivityName(activity));
        rum.seen();
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        RaygunLogger.v("RUM - Activity destroyed: " + getActivityName(activity));
        // Detach the FragmentManager from destroyed Activity
        if (activity instanceof FragmentActivity) {
            rumFragment.detach(((FragmentActivity) activity).getSupportFragmentManager());
        }
        rum.seen();
    }

    private String getActivityName(Activity activity) {
        return activity.getClass().getSimpleName();
    }
}
