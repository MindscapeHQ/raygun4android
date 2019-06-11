package com.raygun.raygun4android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.raygun.raygun4android.logging.RaygunLogger;
import com.raygun.raygun4android.messages.rum.RaygunRUMData;
import com.raygun.raygun4android.messages.rum.RaygunRUMDataMessage;
import com.raygun.raygun4android.messages.rum.RaygunRUMMessage;
import com.raygun.raygun4android.messages.rum.RaygunRUMTimingMessage;
import com.raygun.raygun4android.messages.shared.RaygunUserInfo;
import com.raygun.raygun4android.network.RaygunNetworkLogger;
import com.raygun.raygun4android.services.RUMPostService;

public class RUM implements ActivityLifecycleCallbacks {
    private static RUM rum;
    private static WeakReference<Activity> mainActivity;
    private static WeakReference<Activity> currentActivity;
    private static WeakReference<Activity> loadingActivity;
    private static long startTime;
    private static long lastSeenTime;
    private static String sessionId;
    private static RaygunUserInfo currentSessionUser;

    private static void attach(Activity mainActivity) {

        RaygunLogger.v("attached RUM");
        if (RUM.rum == null && mainActivity != null) {
            Application application = mainActivity.getApplication();

            if (application != null) {
                RUM.mainActivity = new WeakReference<>(mainActivity);
                RUM.currentActivity = new WeakReference<>(mainActivity);
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

    static void attach(Activity mainActivity, boolean networkLogging) {
        RaygunNetworkLogger.setEnabled(networkLogging);
        attach(mainActivity);
    }

    protected static void detach() {
        if (RUM.rum != null && RUM.mainActivity.get() != null && RUM.mainActivity.get().getApplication() != null) {
            RUM.mainActivity.get().getApplication().unregisterActivityLifecycleCallbacks(RUM.rum);
            RUM.mainActivity.clear();
            RUM.currentActivity.clear();
            RUM.rum = null;
        }
    }

    static void sendRemainingActivity() {
        if (RUM.rum != null) {
            if (RUM.loadingActivity.get() != null) {
                String activityName = getActivityName(RUM.loadingActivity.get());

                long diff = System.nanoTime() - RUM.startTime;
                long duration = TimeUnit.NANOSECONDS.toMillis(diff);
                sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration);
            }
            sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser);
        }

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    private static void enqueueWorkForRUMService(String apiKey, String jsonPayload) {
        Intent intent = new Intent(RaygunClient.getApplicationContext(), RUMPostService.class);
        intent.setAction("com.raygun.raygun4android.intent.action.LAUNCH_RUM_POST_SERVICE");
        intent.setPackage("com.raygun.raygun4android");
        intent.setComponent(new ComponentName(RaygunClient.getApplicationContext(), RUMPostService.class));

        intent.putExtra("msg", jsonPayload);
        intent.putExtra("apikey", apiKey);

        RUMPostService.enqueueWork(RaygunClient.getApplicationContext(), intent);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        RaygunLogger.v("RUM - onActivityCreated");
        if (RUM.currentActivity.get() == null) {
            if (doesNeedSessionRotation()) {
                rotateSession(currentSessionUser,currentSessionUser);
            }
        }

        if (activity != RUM.currentActivity.get()) {
            RUM.currentActivity = new WeakReference<>(activity);
            RUM.loadingActivity = new WeakReference<>(activity);
            RUM.startTime = System.nanoTime();
        }

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityStarted(Activity activity) {
        RaygunLogger.v("RUM - onActivityStarted");
        if (RUM.currentActivity.get() == null) {
            if (doesNeedSessionRotation()) {
                rotateSession(currentSessionUser,currentSessionUser);
            }
        }

        if (activity != RUM.currentActivity.get()) {
            RUM.currentActivity = new WeakReference<>(activity);
            RUM.loadingActivity = new WeakReference<>(activity);
            RUM.startTime = System.nanoTime();
        }

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        RaygunLogger.v("RUM - onActivityResumed");
        if (RUM.currentActivity.get() == null) {
            if (doesNeedSessionRotation()) {
                rotateSession(currentSessionUser,currentSessionUser);
            }
        }

        String activityName = getActivityName(activity);
        long duration = 0;

        if (activity == RUM.currentActivity.get()) {
            long diff = System.nanoTime() - RUM.startTime;
            duration = TimeUnit.NANOSECONDS.toMillis(diff);
        }

        RUM.currentActivity = new WeakReference<>(activity);
        RUM.loadingActivity.clear();

        sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration);

        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        RaygunLogger.v("RUM - onActivityPaused");
        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        RaygunLogger.v("RUM - onActivityStopped");
        if (activity == RUM.currentActivity.get()) {
            RUM.currentActivity.clear();
            RUM.loadingActivity.clear();
            RUM.lastSeenTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        RaygunLogger.v("RUM - onActivitySIS");
        RUM.lastSeenTime = System.currentTimeMillis();
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    private static String getActivityName(Activity activity) {
        return activity.getClass().getSimpleName();
    }

    private static void rotateSession(RaygunUserInfo currentSessionUser, RaygunUserInfo newSessionUser) {
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser);
        RUM.sessionId = UUID.randomUUID().toString();
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START, newSessionUser);
    }

    private static boolean doesNeedSessionRotation() {
        return RUM.lastSeenTime > 0 && System.currentTimeMillis() - RUM.lastSeenTime > RaygunSettings.RUM_SESSION_EXPIRY;
    }

    static void updateCurrentSessionUser(RaygunUserInfo userInfo) {

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

    /**
     * Sends a RUM event to Raygun. The message is sent on a background thread.
     *
     * @param eventName Tracks if this is a session start or session end event.
     */
    private static void sendRUMEvent(String eventName, RaygunUserInfo userInfo) {

        if (RaygunClient.isRUMEnabled()) {

            String timestamp;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime utcDateTime = LocalDateTime.now(ZoneId.of("UTC"));
                if (RaygunSettings.RUM_EVENT_SESSION_END.equals(eventName)) {
                    utcDateTime.plus(2, ChronoUnit.SECONDS);
                }
                timestamp = utcDateTime.toString();
            } else {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                Calendar c = Calendar.getInstance();
                if (RaygunSettings.RUM_EVENT_SESSION_END.equals(eventName)) {
                    c.add(Calendar.SECOND, 2);
                }
                timestamp = df.format(c.getTime());
            }

            RaygunUserInfo user = userInfo == null ? new RaygunUserInfo(null, null, null, null) : userInfo;

            RaygunRUMDataMessage dataMessage = new RaygunRUMDataMessage.Builder(eventName)
                .timestamp(timestamp)
                .sessionId(sessionId)
                .version(RaygunClient.getVersion())
                .os("Android")
                .osVersion(Build.VERSION.RELEASE)
                .platform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL))
                .user(user)
                .build();

            RaygunRUMMessage message = new RaygunRUMMessage();
            message.setEventData(new RaygunRUMDataMessage[]{dataMessage});

            enqueueWorkForRUMService(RaygunClient.getApiKey(), new Gson().toJson(message));
        } else {
            RaygunLogger.w("RUM is not enabled, please enable to use the sendRUMEvent() function");
        }
    }

    private static void sendRUMEvent(String eventName) {
        RaygunUserInfo user = RaygunClient.getUser() == null ? new RaygunUserInfo(null, null, null, null) : RaygunClient.getUser();
        sendRUMEvent(eventName, user);
    }

    /**
     * Sends a RUM timing event to Raygun. The message is sent on a background thread.
     *
     * @param eventType    The type of event that occurred.
     * @param name         The name of the event resource such as the activity name or URL of a network call.
     * @param milliseconds The duration of the event in milliseconds.
     */
    public static void sendRUMTimingEvent(RaygunRUMEventType eventType, String name, long milliseconds) {

        if (RaygunClient.isRUMEnabled()) {
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString();
                sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START);
            }

            if (eventType == RaygunRUMEventType.ACTIVITY_LOADED) {
                if (shouldIgnoreView(name)) {
                    return;
                }
            }

            String timestamp;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime utcDateTime = LocalDateTime.now(ZoneId.of("UTC"));
                utcDateTime.minus(milliseconds, ChronoUnit.MILLIS);
                timestamp = utcDateTime.toString();
            } else {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MILLISECOND, -(int) milliseconds);
                timestamp = df.format(c.getTime());
            }

            RaygunUserInfo user = RaygunClient.getUser() == null ? new RaygunUserInfo(null, null, null, null) : RaygunClient.getUser();

            RaygunRUMTimingMessage timingMessage = new RaygunRUMTimingMessage.Builder(eventType == RaygunRUMEventType.ACTIVITY_LOADED ? "p" : "n")
                .duration(milliseconds)
                .build();

            RaygunRUMData data = new RaygunRUMData.Builder(name)
                .timing(timingMessage)
                .build();

            RaygunRUMData[] dataArray = new RaygunRUMData[]{data};
            String dataStr = new Gson().toJson(dataArray);

            RaygunRUMDataMessage dataMessage = new RaygunRUMDataMessage.Builder(RaygunSettings.RUM_EVENT_TIMING)
                .timestamp(timestamp)
                .sessionId(sessionId)
                .version(RaygunClient.getVersion())
                .os("Android")
                .osVersion(Build.VERSION.RELEASE)
                .platform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL))
                .user(user)
                .data(dataStr)
                .build();

            RaygunRUMMessage message = new RaygunRUMMessage();
            message.setEventData(new RaygunRUMDataMessage[]{dataMessage});

            enqueueWorkForRUMService(RaygunClient.getApiKey(), new Gson().toJson(message));
        } else {
            RaygunLogger.w("RUM is not enabled, please enable to use the sendRUMTimingEvent() function");
        }
    }

    private static boolean shouldIgnoreView(String viewName) {
        if (viewName == null) {
            return true;
        }
        for (String ignoredView : RaygunSettings.getIgnoredViews()) {
            if (viewName.contains(ignoredView) || ignoredView.contains(viewName)) {
                return true;
            }
        }
        return false;
    }
}
