package com.raygun.raygun4android;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.raygun.raygun4android.messages.shared.RaygunUserInfo;
import com.raygun.raygun4android.network.RaygunNetworkLogger;

public class RUM implements ActivityLifecycleCallbacks {
    private static RUM rum;
    private static Activity mainActivity;
    private static Activity currentActivity;
    private static Activity loadingActivity;
    private static long startTime;
    private static long lastSeenTime;
    protected static String sessionId;
    private static RaygunUserInfo currentSessionUser;

    protected static void attach(Activity mainActivity) {
        RaygunLogger.v("attach");
        if (RUM.rum == null && mainActivity != null) {
            Application application = mainActivity.getApplication();

            if (application != null) {
                RUM.mainActivity = mainActivity;
                RUM.currentActivity = mainActivity;
                RUM.startTime = System.nanoTime();

                RUM.rum = new RUM();
                application.registerActivityLifecycleCallbacks(RUM.rum);

                if (doesNeedSessionRotation()) {
                    rotateSession(currentSessionUser, currentSessionUser);
                }

                RaygunNetworkLogger.init();
            }
        }

        RUM.lastSeenTime = System.currentTimeMillis();
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
            RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser);
        }

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        RaygunLogger.v("onActivityCreated");
        if (RUM.currentActivity == null) {
            if (doesNeedSessionRotation()) {
                rotateSession(currentSessionUser,currentSessionUser);
            }
        }

        if (activity != RUM.currentActivity) {
            RUM.currentActivity = activity;
            RUM.loadingActivity = activity;
            RUM.startTime = System.nanoTime();
        }

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityStarted(Activity activity) {
        RaygunLogger.v("onActivityStarted");
        if (RUM.currentActivity == null) {
            if (doesNeedSessionRotation()) {
                rotateSession(currentSessionUser,currentSessionUser);
            }
        }

        if (activity != RUM.currentActivity) {
            RUM.currentActivity = activity;
            RUM.loadingActivity = activity;
            RUM.startTime = System.nanoTime();
        }

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        RaygunLogger.v("onActivityResumed");
        if (RUM.currentActivity == null) {
            if (doesNeedSessionRotation()) {
                rotateSession(currentSessionUser,currentSessionUser);
            }
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

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        RaygunLogger.v("onActivityPaused");
        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        RaygunLogger.v("onActivityStopped");
        if (activity == RUM.currentActivity) {
            RUM.currentActivity = null;
            RUM.loadingActivity = null;
            RUM.lastSeenTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        RaygunLogger.v("onActivitySIS");
        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    private static String getActivityName(Activity activity) {
        return activity.getClass().getSimpleName();
    }

    private static void rotateSession(RaygunUserInfo currentSessionUser, RaygunUserInfo newSessionUser) {
        RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser);
        RUM.sessionId = UUID.randomUUID().toString();
        RaygunClient.sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START, newSessionUser);
    }

    private static boolean doesNeedSessionRotation() {
        if (RUM.lastSeenTime > 0 &&
            System.currentTimeMillis()-RUM.lastSeenTime > RaygunSettings.RUM_SESSION_EXPIRY) {
            return true;
        }
        return false;
    }

    public static void updateCurrentSessionUser(RaygunUserInfo userInfo) {

        if (RUM.currentSessionUser != null) {
            boolean currentSessionUserIsAnon = RUM.currentSessionUser.getIsAnonymous();
            boolean usersAreTheSame = RUM.currentSessionUser.getIdentifier().equals(userInfo.getIdentifier());
            boolean changedUser = !usersAreTheSame && !currentSessionUserIsAnon;

            if (changedUser) {
                rotateSession(RUM.currentSessionUser,userInfo);
            }
        }
        RUM.currentSessionUser = userInfo;
    }
}
