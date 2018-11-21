package com.raygun.raygun4android;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.gson.Gson;
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage;
import com.raygun.raygun4android.messages.rum.RaygunRUMData;
import com.raygun.raygun4android.messages.rum.RaygunRUMDataMessage;
import com.raygun.raygun4android.messages.rum.RaygunRUMMessage;
import com.raygun.raygun4android.messages.rum.RaygunRUMTimingMessage;
import com.raygun.raygun4android.messages.shared.RaygunUserInfo;
import com.raygun.raygun4android.network.RaygunNetworkUtils;
import com.raygun.raygun4android.services.CrashReportingPostService;
import com.raygun.raygun4android.services.RUMPostService;
import com.raygun.raygun4android.utils.RaygunFileFilter;
import com.raygun.raygun4android.utils.RaygunFileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * The official Raygun provider for Android. This is the main class that provides functionality for
 * automatically sending exceptions to the Raygun service.
 *
 * You should call init() on the static RaygunClient instance, passing in the application, instead
 * of instantiating this class.
 */
public class RaygunClient {
    private static String apiKey;
    private static Application application;
    private static String version;
    private static String appContextIdentifier;
    private static RaygunUserInfo userInfo;
    private static RaygunUncaughtExceptionHandler handler;
    private static RaygunOnBeforeSend onBeforeSend;
    private static List tags;
    private static Map userCustomData;
    private static boolean crashReportingEnabled = false;
    private static boolean RUMEnabled = false;

    /**
     * Initializes the Raygun client. This expects that you have placed the API key in your
     * AndroidManifest.xml, in a meta-data element.
     *
     * @param application The Android application
     */
    public static void init(Application application) {
        RaygunClient.application = application;
        String apiKey = readApiKey(getApplicationContext());
        init(application, apiKey);
    }

    /**
     * Initializes the Raygun client with the version of your application. This expects that you have
     * placed the API key in your AndroidManifest.xml, in a meta-data element.
     *
     * @param version The version of your application, format x.x.x.x, where x is a positive integer.
     * @param application The Android application
     */
    public static void init(String version, Application application) {
        RaygunClient.application = application;
        String apiKey = readApiKey(getApplicationContext());
        init(application, apiKey, version);
    }

    /**
     * Initializes the Raygun client with your Android application and your Raygun API key. The version
     * transmitted will be the value of the versionName attribute in your manifest element.
     *
     * @param application The Android application
     * @param apiKey An API key that belongs to a Raygun application created in your dashboard
     */
    public static void init(Application application, String apiKey) {
        if (RaygunClient.application == null) {
            RaygunClient.application = application;
        }

        RaygunClient.apiKey = apiKey;
        RaygunClient.appContextIdentifier = UUID.randomUUID().toString();

        RaygunLogger.d("Configuring Raygun4Android (v" + RaygunSettings.RAYGUN_CLIENT_VERSION + ")");

        if (RaygunClient.version == null || RaygunClient.version.trim().isEmpty()) {
            try {
                RaygunClient.version = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                RaygunClient.version = "not provided";
                RaygunLogger.w("Couldn't read application version from calling package");
            }
        }

        postCachedMessages();
    }

    /**
     * Initializes the Raygun client with your Android application, your Raygun API key, and the
     * version of your application
     *
     * @param application The Android application
     * @param apiKey  An API key that belongs to a Raygun application created in your dashboard
     * @param version The version of your application, format x.x.x.x, where x is a positive integer.
     */
    public static void init(Application application, String apiKey, String version) {
        RaygunClient.version = version;
        init(application, apiKey);
    }

    /**
     * Attaches a pre-built Raygun exception handler to the thread's DefaultUncaughtExceptionHandler.
     * This automatically sends any exceptions that reaches it to the Raygun API.
     */
    public static void attachExceptionHandler() {
        UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(oldHandler instanceof RaygunUncaughtExceptionHandler)) {
            RaygunClient.handler = new RaygunUncaughtExceptionHandler(oldHandler);
            Thread.setDefaultUncaughtExceptionHandler(RaygunClient.handler);
        }
    }

    /**
     * Sends an exception-type object to Raygun.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
     */
    public static void send(Throwable throwable) {
        if (RaygunClient.isCrashReportingEnabled()) {

            RaygunMessage msg = buildMessage(throwable);

            if (RaygunClient.tags != null) {
                msg.getDetails().setTags(RaygunClient.tags);
            }

            if (RaygunClient.userCustomData != null) {
                msg.getDetails().setUserCustomData(RaygunClient.userCustomData);
            }

            if (RaygunClient.onBeforeSend != null) {
                msg = RaygunClient.onBeforeSend.onBeforeSend(msg);
                if (msg == null) {
                    return;
                }
            }

            enqueueWorkForCrashReportingService(RaygunClient.apiKey, new Gson().toJson(msg));
            postCachedMessages();
        } else {
            RaygunLogger.w("Crash Reporting is not enabled, please enable to use the send() function");
        }
    }

    /**
     * Sends an exception-type object to Raygun with a list of tags you specify.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
     * @param tags      A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
     *                  This could be a build tag, lifecycle state, debug/production version etc.
     */
    public static void send(Throwable throwable, List tags) {
        if (RaygunClient.isCrashReportingEnabled()) {

            RaygunMessage msg = buildMessage(throwable);
            msg.getDetails().setTags(mergeTags(tags));

            if (RaygunClient.userCustomData != null) {
                msg.getDetails().setUserCustomData(RaygunClient.userCustomData);
            }

            if (RaygunClient.onBeforeSend != null) {
                msg = RaygunClient.onBeforeSend.onBeforeSend(msg);
                if (msg == null) {
                    return;
                }
            }

            enqueueWorkForCrashReportingService(RaygunClient.apiKey, new Gson().toJson(msg));
            postCachedMessages();
        } else {
            RaygunLogger.w("Crash Reporting is not enabled, please enable to use the send() function");
        }
    }

    /**
     * Sends an exception-type object to Raygun with a list of tags you specify, and a set of custom data.
     *
     * @param throwable      The Throwable object that occurred in your application that will be sent to Raygun.
     * @param tags           A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
     *                       This could be a build tag, lifecycle state, debug/production version etc.
     * @param userCustomData A set of custom key-value pairs relating to your application and its current state. This is a bucket
     *                       where you can attach any related data you want to see to the error.
     */
    public static void send(Throwable throwable, List tags, Map userCustomData) {
        if (RaygunClient.isCrashReportingEnabled()) {

            RaygunMessage msg = buildMessage(throwable);

            msg.getDetails().setTags(mergeTags(tags));
            msg.getDetails().setUserCustomData(mergeUserCustomData(userCustomData));

            if (RaygunClient.onBeforeSend != null) {
                msg = RaygunClient.onBeforeSend.onBeforeSend(msg);
                if (msg == null) {
                    return;
                }
            }

            enqueueWorkForCrashReportingService(RaygunClient.apiKey, new Gson().toJson(msg));
            postCachedMessages();
        } else {
            RaygunLogger.w("Crash Reporting is not enabled, please enable to use the send() function");
        }
    }

    /**
     * Sets the current user of your application. If user is an email address which is associated with a Gravatar,
     * their picture will be displayed in the error view. If setUser is not called, a random ID will be assigned.
     * If the user context changes in your application (i.e log in/out), be sure to call this again with the
     * updated user name/email address.
     *
     * If you use an email address to identify the user, please consider using setUser(RaygunUserInfo userInfo)
     * instead of this method as it would allow you to set the email address into both the identifier and email fields
     * of the crash data to be sent.
     *
     * @param user A user name or email address representing the current user.
     */
    public static void setUser(String user) {
        RaygunUserInfo newUser = new RaygunUserInfo(user);
        setUser(newUser);
    }

    /**
     * Sets the current user of your application. If user is an email address which is associated with a Gravatar,
     * their picture will be displayed in the error view. If setUser is not called, a random ID will be assigned.
     * If the user context changes in your application (i.e log in/out), be sure to call this again with the
     * updated user name/email address.
     *
     * @param userInfo A RaygunUserInfo object containing the user data you want to send in its fields.
     */
    public static void setUser(RaygunUserInfo userInfo) {
        if (isRUMEnabled()) {
            RUM.updateCurrentSessionUser(userInfo);
        }
        RaygunClient.userInfo = userInfo;
    }

    public static RaygunUserInfo getUser() {
        return RaygunClient.userInfo;
    }

    /**
     * Manually stores the version of your application to be transmitted with each message, for version
     * filtering. This is normally read from your AndroidManifest.xml (the versionName attribute on manifest element)
     * or passed in on init(); this is only provided as a convenience.
     *
     * @param version The version of your application, format x.x.x.x, where x is a positive integer.
     */
    public static void setVersion(String version) {
        if (version != null) {
            RaygunClient.version = version;
        }
    }

    public static RaygunUncaughtExceptionHandler getExceptionHandler() {
        return RaygunClient.handler;
    }

    public static String getApiKey() {
        return RaygunClient.apiKey;
    }

    public static List getTags() {
        return RaygunClient.tags;
    }

    public static void setTags(List tags) {
        RaygunClient.tags = tags;
    }

    public static Map getUserCustomData() {
        return RaygunClient.userCustomData;
    }

    public static void setUserCustomData(Map userCustomData) {
        RaygunClient.userCustomData = userCustomData;
    }

    public static void setOnBeforeSend(RaygunOnBeforeSend onBeforeSend) {
        RaygunClient.onBeforeSend = onBeforeSend;
    }

    /**
     * Allows the user to add more URLs to filter out, so network timing events are not sent for them.
     *
     * @param urls An array of urls to filter out by.
     */
    public static void ignoreURLs(String[] urls) {
        RaygunSettings.ignoreURLs(urls);
    }

    /**
     * Allows the user to add more views to filter out, so load timing events are not sent for them.
     *
     * @param views An array of activity names to filter out by.
     */
    public static void ignoreViews(String[] views) {
        RaygunSettings.ignoreViews(views);
    }

    /**
     * Allows the user to set a custom endpoint for Crash Reporting
     *
     * @param url String with the URL to be used
     */
    public static void setCustomCrashReportingEndpoint(String url) {
        if (url != null && !url.isEmpty()) {
            RaygunSettings.setCrashReportingEndpoint(url);
        } else {
            RaygunLogger.w("A custom crash reporting endpoint can't be null or empty. Custom endpoint has NOT been applied");
        }
    }

    /**
     * Allows the user to set a custom endpoint for RUM
     *
     * @param url String with the URL to be used
     */
    public static void setCustomRUMEndpoint(String url) {
        if (url != null && !url.isEmpty()) {
            RaygunSettings.setRUMEndpoint(url);
        } else {
            RaygunLogger.w("A custom RUM endpoint can't be null or empty. Custom endpoint has NOT been applied");
        }
    }

    /**
     * Allows the user to set the maximum number of crash reports stored on the device.
     *
     * The default and maximum value for this is 64. We do not recommend to change this setting
     * unless you have a very good reason and use case.
     *
     * If you decrease the value of maxReportsStoredOnDevice, all currently cached reports will be deleted.
     *
     * @param maxReportsStoredOnDevice An int with the new maximum number of crash reports
     */
    public static void setMaxReportsStoredOnDevice(int maxReportsStoredOnDevice) {
        int currentMaxReportsStoredOnDevice = RaygunSettings.getMaxReportsStoredOnDevice();

        if (maxReportsStoredOnDevice < currentMaxReportsStoredOnDevice) {
            RaygunFileUtils.clearCachedReports(getApplicationContext());
        }

        RaygunSettings.setMaxReportsStoredOnDevice(maxReportsStoredOnDevice);
    }

    private static boolean isCrashReportingEnabled() {
        return crashReportingEnabled;
    }

    public static void enableCrashReporting() {
        RaygunClient.crashReportingEnabled = true;
        attachExceptionHandler();
    }

    private static boolean isRUMEnabled() {
        return RUMEnabled;
    }

    /**
     * Enables the Raygun RUM feature which will automatically report session and view events.
     *
     * @param activity The main/entry activity of the Android app.
     */
    public static void enableRUM(Activity activity) {
        RaygunClient.RUMEnabled = true;
        RUM.attach(activity);
        if (RaygunClient.userInfo != null) {
            RUM.updateCurrentSessionUser(RaygunClient.userInfo);
        }
    }

    /**
     * Enables the Raygun RUM feature which will automatically report session and view events AND network performance.
     *
     * @param activity       The main/entry activity of the Android app.
     * @param networkLogging Automatically report the performance of network requests.
     */
    public static void enableRUM(Activity activity, boolean networkLogging) {
        RaygunClient.RUMEnabled = true;
        RUM.attach(activity, networkLogging);
        if (RaygunClient.userInfo != null) {
            RUM.updateCurrentSessionUser(RaygunClient.userInfo);
        }
    }

    /**
     * Sends a RUM event to Raygun. The message is sent on a background thread.
     *
     * @param eventName Tracks if this is a session start or session end event.
     */
    protected static void sendRUMEvent(String eventName, RaygunUserInfo userInfo) {

        if (RaygunClient.isRUMEnabled()) {

            RaygunRUMMessage message = new RaygunRUMMessage();
            RaygunRUMDataMessage dataMessage = new RaygunRUMDataMessage();

            dataMessage.setType(eventName);

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            Calendar c = Calendar.getInstance();
            if (RaygunSettings.RUM_EVENT_SESSION_END.equals(eventName)) {
                c.add(Calendar.SECOND, 2);
            }
            String timestamp = df.format(c.getTime());
            dataMessage.setTimestamp(timestamp);

            dataMessage.setSessionId(RUM.sessionId);
            dataMessage.setVersion(RaygunClient.version);
            dataMessage.setOS("Android");
            dataMessage.setOSVersion(Build.VERSION.RELEASE);
            dataMessage.setPlatform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL));

            RaygunUserInfo user = userInfo == null ? new RaygunUserInfo(null, null, null, null) : userInfo;
            dataMessage.setUser(user);

            message.setEventData(new RaygunRUMDataMessage[]{dataMessage});

            enqueueWorkForRUMService(RaygunClient.apiKey, new Gson().toJson(message));

            RaygunLogger.v(new Gson().toJson(message));
        } else {
            RaygunLogger.w("RUM is not enabled, please enable to use the sendRUMEvent() function");
        }
    }

    private static void sendRUMEvent(String eventName) {
        RaygunUserInfo user = RaygunClient.userInfo == null ? new RaygunUserInfo(null, null, null, null) : RaygunClient.userInfo;
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
            if (RUM.sessionId == null) {
                RUM.sessionId = UUID.randomUUID().toString();
                sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START);
            }

            if (eventType == RaygunRUMEventType.ACTIVITY_LOADED) {
                if (RaygunClient.shouldIgnoreView(name)) {
                    return;
                }
            }

            RaygunRUMMessage message = new RaygunRUMMessage();
            RaygunRUMDataMessage dataMessage = new RaygunRUMDataMessage();

            dataMessage.setType(RaygunSettings.RUM_EVENT_TIMING);

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MILLISECOND, -(int) milliseconds);
            String timestamp = df.format(c.getTime());
            dataMessage.setTimestamp(timestamp);

            dataMessage.setSessionId(RUM.sessionId);
            dataMessage.setVersion(RaygunClient.version);
            dataMessage.setOS("Android");
            dataMessage.setOSVersion(Build.VERSION.RELEASE);
            dataMessage.setPlatform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL));

            RaygunUserInfo user = RaygunClient.userInfo == null ? new RaygunUserInfo(null, null, null, null) : RaygunClient.userInfo;
            dataMessage.setUser(user);

            RaygunRUMData data = new RaygunRUMData();
            RaygunRUMTimingMessage timingMessage = new RaygunRUMTimingMessage();
            timingMessage.setType(eventType == RaygunRUMEventType.ACTIVITY_LOADED ? "p" : "n");
            timingMessage.setDuration(milliseconds);
            data.setName(name);
            data.setTiming(timingMessage);

            RaygunRUMData[] dataArray = new RaygunRUMData[]{data};
            String dataStr = new Gson().toJson(dataArray);
            dataMessage.setData(dataStr);

            message.setEventData(new RaygunRUMDataMessage[]{dataMessage});

            enqueueWorkForRUMService(RaygunClient.apiKey, new Gson().toJson(message));

            RaygunLogger.v(new Gson().toJson(message));
        } else {
            RaygunLogger.w("RUM is not enabled, please enable to use the sendRUMTimingEvent() function");
        }
    }

    private static RaygunMessage buildMessage(Throwable throwable) {
        try {
            RaygunMessage msg = RaygunMessageBuilder.instance()
                .setEnvironmentDetails(getApplicationContext())
                .setMachineName(Build.MODEL)
                .setExceptionDetails(throwable)
                .setClientDetails()
                .setAppContext(RaygunClient.appContextIdentifier)
                .setVersion(RaygunClient.version)
                .setNetworkInfo(getApplicationContext())
                .build();

            if (RaygunClient.version != null) {
                msg.getDetails().setVersion(RaygunClient.version);
            }

            if (RaygunClient.userInfo != null) {
                msg.getDetails().setUserInfo(RaygunClient.userInfo);
            } else {
                msg.getDetails().setUserInfo();
            }
            return msg;
        } catch (Exception e) {
            RaygunLogger.e("Failed to build RaygunMessage - " + e);
        }
        return null;
    }

    private static String readApiKey(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.getString(RaygunSettings.APIKEY_MANIFEST_FIELD);
        } catch (PackageManager.NameNotFoundException e) {
            RaygunLogger.e("Couldn't read API key from your AndroidManifest.xml <meta-data /> element; cannot send. Detailed error: " + e.getMessage());
        }
        return null;
    }

    private static void postCachedMessages() {
        if (RaygunNetworkUtils.hasInternetConnection(getApplicationContext())) {
            File[] fileList = getApplicationContext().getCacheDir().listFiles(new RaygunFileFilter());
            for (File f : fileList) {
                try {
                    if (RaygunFileUtils.getExtension(f.getName()).equalsIgnoreCase(RaygunSettings.DEFAULT_FILE_EXTENSION)) {
                        ObjectInputStream ois = null;
                        try {
                            ois = new ObjectInputStream(new FileInputStream(f));
                            SerializedMessage serializedMessage = (SerializedMessage) ois.readObject();
                            enqueueWorkForCrashReportingService(RaygunClient.apiKey, serializedMessage.message);
                            f.delete();
                        } finally {
                            if (ois != null) {
                                ois.close();
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    RaygunLogger.e("Error loading cached message from filesystem - " + e.getMessage());
                } catch (IOException e) {
                    RaygunLogger.e("Error reading cached message from filesystem - " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    RaygunLogger.e("Error in cached message from filesystem - " + e.getMessage());
                }
            }
        }
    }

    private static void enqueueWorkForRUMService(String apiKey, String jsonPayload) {
        Intent intent = new Intent(getApplicationContext(), RUMPostService.class);
        intent.setAction("com.raygun.raygun4android.intent.action.LAUNCH_RUM_POST_SERVICE");
        intent.setPackage("com.raygun.raygun4android");
        intent.setComponent(new ComponentName(getApplicationContext(), RUMPostService.class));

        intent.putExtra("msg", jsonPayload);
        intent.putExtra("apikey", apiKey);

        RUMPostService.enqueueWork(getApplicationContext(), intent);
    }

    private static void enqueueWorkForCrashReportingService(String apiKey, String jsonPayload) {
        Intent intent = new Intent(getApplicationContext(), CrashReportingPostService.class);
        intent.setAction("com.raygun.raygun4android.intent.action.LAUNCH_CRASHREPORTING_POST_SERVICE");
        intent.setPackage("com.raygun.raygun4android");
        intent.setComponent(new ComponentName(getApplicationContext(), CrashReportingPostService.class));

        intent.putExtra("msg", jsonPayload);
        intent.putExtra("apikey", apiKey);

        CrashReportingPostService.enqueueWork(getApplicationContext(), intent);
    }

    private static List mergeTags(List paramTags) {
        if (RaygunClient.tags != null) {
            List merged = new ArrayList(RaygunClient.tags);
            merged.addAll(paramTags);
            return merged;
        } else {
            return paramTags;
        }
    }

    private static Map mergeUserCustomData(Map paramUserCustomData) {
        if (RaygunClient.userCustomData != null) {
            Map merged = new HashMap(RaygunClient.userCustomData);
            merged.putAll(paramUserCustomData);
            return merged;
        } else {
            return paramUserCustomData;
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

    /**
     * Returns the current Application's context.
     *
     * @return The current application Context.
     * @throws java.lang.IllegalStateException if init() has not been called.
     */
    public static Context getApplicationContext() {
        if (RaygunClient.application == null) {
            throw new IllegalStateException("init() must be called first.");
        }

        return RaygunClient.application.getApplicationContext();
    }

    public static class RaygunUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private UncaughtExceptionHandler defaultHandler;
        private List tags;
        private Map userCustomData;

        public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler) {
            this.defaultHandler = defaultHandler;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            if (userCustomData != null) {
                RaygunClient.send(throwable, tags, userCustomData);
            } else if (tags != null) {
                RaygunClient.send(throwable, tags);
            } else {
                List tags = new ArrayList();
                tags.add("UnhandledException");
                RaygunClient.send(throwable, tags);
                RUM.sendRemainingActivity();
            }
            defaultHandler.uncaughtException(thread, throwable);
        }
    }


}
