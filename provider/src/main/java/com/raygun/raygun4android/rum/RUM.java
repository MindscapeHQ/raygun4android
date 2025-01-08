package com.raygun.raygun4android.rum;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import com.google.gson.Gson;
import com.raygun.raygun4android.RaygunClient;
import com.raygun.raygun4android.RaygunRUMEventType;
import com.raygun.raygun4android.RaygunSettings;
import com.raygun.raygun4android.logging.RaygunLogger;
import com.raygun.raygun4android.messages.rum.RaygunRUMData;
import com.raygun.raygun4android.messages.rum.RaygunRUMDataMessage;
import com.raygun.raygun4android.messages.rum.RaygunRUMMessage;
import com.raygun.raygun4android.messages.rum.RaygunRUMTimingMessage;
import com.raygun.raygun4android.messages.shared.RaygunUserInfo;
import com.raygun.raygun4android.network.RaygunNetworkLogger;
import com.raygun.raygun4android.services.RUMPostService;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

public class RUM {
    // Singleton instance
    private static RUM instance;

    private long lastSeenTime;
    private String sessionId;
    private RaygunUserInfo currentSessionUser;
    private final RUMActivity rumActivity;

    private RUM() {
        this.rumActivity = new RUMActivity(this, new RUMFragment(this));
    }

    /**
     * Gets the singleton instance of the RUM class.
     */
    public static RUM getInstance() {
        if (instance == null) {
            instance = new RUM();
        }
        return instance;
    }

    /**
     * Attaches the RUM instance to the main activity and starts tracking RUM events.
     *
     * @param mainActivity   The main activity of the application.
     * @param networkLogging Whether to log network requests.
     */
    public void attach(Activity mainActivity, boolean networkLogging) {
        RaygunLogger.v("attached RUM");
        RaygunNetworkLogger.setEnabled(networkLogging);
        if (!rumActivity.isAttached()) {
            rumActivity.attach(mainActivity);
            maybeRotateSession();
            RaygunNetworkLogger.init();
        }
        seen();
    }

    /**
     * Send all remaining RUM events before the app is closed.
     */
    public void sendRemaining() {
        rumActivity.sendRemaining();
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser);
        seen();
    }

    /**
     * Call this method everytime there is a lifecycle event in activities or fragments.
     */
    protected void seen() {
        lastSeenTime = System.currentTimeMillis();
    }

    /**
     * Rotates the User session if expired
     */
    protected void maybeRotateSession() {
        if (doesNeedSessionRotation()) {
            rotateSession(currentSessionUser, currentSessionUser);
        }
    }

    private boolean doesNeedSessionRotation() {
        return lastSeenTime > 0
            && System.currentTimeMillis() - lastSeenTime
            > RaygunSettings.RUM_SESSION_EXPIRY;
    }

    public void updateCurrentSessionUser(RaygunUserInfo userInfo) {
        if (currentSessionUser != null) {
            boolean currentSessionUserIsAnon = currentSessionUser.getIsAnonymous();
            boolean usersAreTheSame =
                currentSessionUser.getIdentifier().equals(userInfo.getIdentifier());
            boolean changedUser = !usersAreTheSame && !currentSessionUserIsAnon;

            if (changedUser) {
                rotateSession(currentSessionUser, userInfo);
            }
        }
        currentSessionUser = userInfo;
    }

    /**
     * Detaches the RUM instance from the main activity and stops tracking RUM events.
     * Also detaches from the FragmentManager in the main activity.
     * And clears the singleton instance.
     */
    public void detach() {
        rumActivity.detach();
        instance = null;
    }

    private void rotateSession(
        RaygunUserInfo currentSessionUser, RaygunUserInfo newSessionUser) {
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser);
        sessionId = UUID.randomUUID().toString();
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START, newSessionUser);
    }

    private void enqueueWorkForRUMService(String apiKey, String jsonPayload) {
        Intent intent = new Intent(RaygunClient.getApplicationContext(), RUMPostService.class);
        intent.setAction("com.raygun.raygun4android.intent.action.LAUNCH_RUM_POST_SERVICE");
        intent.setPackage("com.raygun.raygun4android");
        intent.setComponent(
            new ComponentName(RaygunClient.getApplicationContext(), RUMPostService.class));

        intent.putExtra("msg", jsonPayload);
        intent.putExtra("apikey", apiKey);

        RUMPostService.enqueueWork(RaygunClient.getApplicationContext(), intent);
    }

    /**
     * Sends a RUM event to Raygun. The message is sent on a background thread.
     *
     * @param eventName Tracks if this is a session start or session end event.
     */
    private void sendRUMEvent(String eventName, RaygunUserInfo userInfo) {
        if (RaygunClient.isRUMEnabled()) {
            String timestamp;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime utcDateTime = LocalDateTime.now(ZoneId.of("UTC"));
                if (RaygunSettings.RUM_EVENT_SESSION_END.equals(eventName)) {
                    utcDateTime.plusSeconds(2);
                }
                timestamp = utcDateTime.toString();
            } else {
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                Calendar c = Calendar.getInstance();
                if (RaygunSettings.RUM_EVENT_SESSION_END.equals(eventName)) {
                    c.add(Calendar.SECOND, 2);
                }
                timestamp = df.format(c.getTime());
            }

            RaygunUserInfo user =
                userInfo == null ? new RaygunUserInfo(null, null, null, null) : userInfo;

            RaygunRUMDataMessage dataMessage =
                new RaygunRUMDataMessage.Builder(eventName)
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

    private void sendRUMEvent(String eventName) {
        RaygunUserInfo user =
            RaygunClient.getUser() == null
                ? new RaygunUserInfo(null, null, null, null)
                : RaygunClient.getUser();
        sendRUMEvent(eventName, user);
    }

    /**
     * Sends a RUM timing event to Raygun. The message is sent on a background thread.
     *
     * @param eventType    The type of event that occurred.
     * @param name         The name of the event resource such as the activity name or URL of a network
     *                     call.
     * @param milliseconds The duration of the event in milliseconds.
     */
    public void sendRUMTimingEvent(
        RaygunRUMEventType eventType, String name, long milliseconds) {

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
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MILLISECOND, -(int) milliseconds);
                timestamp = df.format(c.getTime());
            }

            RaygunUserInfo user =
                RaygunClient.getUser() == null
                    ? new RaygunUserInfo(null, null, null, null)
                    : RaygunClient.getUser();

            RaygunRUMTimingMessage timingMessage =
                new RaygunRUMTimingMessage.Builder(
                    eventType == RaygunRUMEventType.ACTIVITY_LOADED || eventType == RaygunRUMEventType.FRAGMENT_LOADED ? "p" : "n")
                    .duration(milliseconds)
                    .build();

            RaygunRUMData data = new RaygunRUMData.Builder(name).timing(timingMessage).build();

            RaygunRUMData[] dataArray = new RaygunRUMData[]{data};
            String dataStr = new Gson().toJson(dataArray);

            RaygunRUMDataMessage dataMessage =
                new RaygunRUMDataMessage.Builder(RaygunSettings.RUM_EVENT_TIMING)
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
            RaygunLogger.w(
                "RUM is not enabled, please enable to use the sendRUMTimingEvent() function");
        }
    }

    private boolean shouldIgnoreView(String viewName) {
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
