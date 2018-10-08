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
import com.raygun.raygun4android.messages.RaygunMessage;
import com.raygun.raygun4android.messages.RaygunPulseData;
import com.raygun.raygun4android.messages.RaygunPulseDataMessage;
import com.raygun.raygun4android.messages.RaygunPulseMessage;
import com.raygun.raygun4android.messages.RaygunPulseTimingMessage;
import com.raygun.raygun4android.messages.RaygunUserContext;
import com.raygun.raygun4android.messages.RaygunUserInfo;
import com.raygun.raygun4android.network.RaygunNetworkUtils;
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
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
    private static String sessionId;

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

        try {
            RaygunClient.version = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            RaygunClient.version = "not provided";
            RaygunLogger.i("Couldn't read application version from calling package");
        }

        RaygunClient.appContextIdentifier = UUID.randomUUID().toString();
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
        init(application, apiKey);
        RaygunClient.version = version;
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
     * Attaches the Raygun Pulse feature which will automatically report session and view events.
     *
     * @param activity The main/entry activity of the Android app.
     */
    public static void attachPulse(Activity activity) {
        Pulse.attach(activity);
    }

    /**
     * Attaches the Raygun Pulse feature which will automatically report session and view events.
     *
     * @param activity       The main/entry activity of the Android app.
     * @param networkLogging Automatically report the performance of network requests.
     */
    public static void attachPulse(Activity activity, boolean networkLogging) {
        Pulse.attach(activity, networkLogging);
    }

    /**
     * Sends an exception-type object to Raygun.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
     */
    public static void send(Throwable throwable) {
        RaygunMessage msg = buildMessage(throwable);
        postCachedMessages();

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

        enqueueWorkForService(RaygunClient.apiKey, new Gson().toJson(msg), false);
    }

    /**
     * Sends an exception-type object to Raygun with a list of tags you specify.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
     * @param tags      A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
     *                  This could be a build tag, lifecycle state, debug/production version etc.
     */
    public static void send(Throwable throwable, List tags) {
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

        postCachedMessages();
        enqueueWorkForService(RaygunClient.apiKey, new Gson().toJson(msg), false);
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
        RaygunMessage msg = buildMessage(throwable);

        msg.getDetails().setTags(mergeTags(tags));
        msg.getDetails().setUserCustomData(mergeUserCustomData(userCustomData));

        if (RaygunClient.onBeforeSend != null) {
            msg = RaygunClient.onBeforeSend.onBeforeSend(msg);
            if (msg == null) {
                return;
            }
        }

        postCachedMessages();
        enqueueWorkForService(RaygunClient.apiKey, new Gson().toJson(msg), false);
    }

    protected static void sendPulseEvent(String name) {
        if ("session_start".equals(name)) {
            RaygunClient.sessionId = UUID.randomUUID().toString();
        }

        RaygunPulseMessage message = new RaygunPulseMessage();
        RaygunPulseDataMessage pulseData = new RaygunPulseDataMessage();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar c = Calendar.getInstance();

        if ("session_end".equals(name)) {
            c.add(Calendar.SECOND, 2);
        }

        String timestamp = df.format(c.getTime());
        pulseData.setTimestamp(timestamp);
        pulseData.setVersion(RaygunClient.version);
        pulseData.setOS("Android");
        pulseData.setOSVersion(Build.VERSION.RELEASE);
        pulseData.setPlatform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL));

        RaygunUserContext userContext = RaygunClient.userInfo == null ? new RaygunUserContext(new RaygunUserInfo(null, null, null, null, null, true), getApplicationContext()) : new RaygunUserContext(RaygunClient.userInfo, getApplicationContext());
        pulseData.setUser(userContext);

        pulseData.setSessionId(RaygunClient.sessionId);
        pulseData.setType(name);

        message.setEventData(new RaygunPulseDataMessage[]{pulseData});

        enqueueWorkForService(RaygunClient.apiKey, new Gson().toJson(message), true);
    }

    /**
     * Sends a pulse timing event to Raygun. The message is sent on a background thread.
     *
     * @param eventType    The type of event that occurred.
     * @param name         The name of the event resource such as the activity name or URL of a network call.
     * @param milliseconds The duration of the event in milliseconds.
     */
    public static void sendPulseTimingEvent(RaygunPulseEventType eventType, String name, long milliseconds) {
        if (RaygunClient.sessionId == null) {
            sendPulseEvent("session_start");
        }

        if (eventType == RaygunPulseEventType.ACTIVITY_LOADED) {
            if (RaygunClient.shouldIgnoreView(name)) {
                return;
            }
        }

        RaygunPulseMessage message = new RaygunPulseMessage();
        RaygunPulseDataMessage dataMessage = new RaygunPulseDataMessage();

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MILLISECOND, -(int) milliseconds);
        String timestamp = df.format(c.getTime());

        dataMessage.setTimestamp(timestamp);
        dataMessage.setSessionId(RaygunClient.sessionId);
        dataMessage.setVersion(RaygunClient.version);
        dataMessage.setOS("Android");
        dataMessage.setOSVersion(Build.VERSION.RELEASE);
        dataMessage.setPlatform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL));
        dataMessage.setType("mobile_event_timing");

        RaygunUserContext userContext = RaygunClient.userInfo == null ? new RaygunUserContext(new RaygunUserInfo(null, null, null, null, null, true), getApplicationContext()) : new RaygunUserContext(RaygunClient.userInfo, getApplicationContext());
        dataMessage.setUser(userContext);

        RaygunPulseData data = new RaygunPulseData();
        RaygunPulseTimingMessage timingMessage = new RaygunPulseTimingMessage();
        timingMessage.setType(eventType == RaygunPulseEventType.ACTIVITY_LOADED ? "p" : "n");
        timingMessage.setDuration(milliseconds);
        data.setName(name);
        data.setTiming(timingMessage);

        RaygunPulseData[] dataArray = new RaygunPulseData[]{data};
        String dataStr = new Gson().toJson(dataArray);
        dataMessage.setData(dataStr);

        message.setEventData(new RaygunPulseDataMessage[]{dataMessage});

        enqueueWorkForService(RaygunClient.apiKey, new Gson().toJson(message), true);
    }

    protected static int postPulseMessage(String apiKey, String jsonPayload) {
        try {
            if (validateApiKey(apiKey)) {
                String endpoint = RaygunSettings.getPulseEndpoint();
                MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, jsonPayload);

                Request request = new Request.Builder()
                        .url(endpoint)
                        .header("X-ApiKey", apiKey)
                        .post(body)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    RaygunLogger.d("Pulse HTTP POST result: " + response.code());
                    return response.code();
                } catch (IOException ioe) {
                    RaygunLogger.e("OkHttp POST to Raygun Pulse backend failed - " + ioe.getMessage());
                    ioe.printStackTrace();
                }
            }
        } catch (Exception e) {
            RaygunLogger.e("Couldn't post exception - " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
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
                msg.getDetails().setUserContext(RaygunClient.userInfo, getApplicationContext());
            } else {
                msg.getDetails().setUserContext(getApplicationContext());
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
            return bundle.getString("com.raygun.raygun4android.apikey");
        } catch (PackageManager.NameNotFoundException e) {
            RaygunLogger.e("Couldn't read API key from your AndroidManifest.xml <meta-data /> element; cannot send. Detailed error: " + e.getMessage());
        }
        return null;
    }

    protected static Boolean validateApiKey(String apiKey) {
        if (apiKey.length() == 0) {
            RaygunLogger.e("API key has not been provided, exception will not be logged");
            return false;
        } else {
            return true;
        }
    }

    private static void postCachedMessages() {
        if (RaygunNetworkUtils.hasInternetConnection(getApplicationContext())) {
            File[] fileList = getApplicationContext().getCacheDir().listFiles();
            for (File f : fileList) {
                try {
                    if (RaygunFileUtils.getExtension(f.getName()).equalsIgnoreCase(RaygunSettings.DEFAULT_FILE_EXTENSION)) {
                        ObjectInputStream ois = null;
                        try {
                            ois = new ObjectInputStream(new FileInputStream(f));
                            SerializedMessage serializedMessage = (SerializedMessage) ois.readObject();
                            enqueueWorkForService(RaygunClient.apiKey, serializedMessage.message, false);
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

    private static void enqueueWorkForService(String apiKey, String jsonPayload, boolean isPulse) {
        Intent intent = new Intent(getApplicationContext(), RaygunPostService.class);
        intent.setAction("com.raygun.raygun4android.intent.action.LAUNCH_POST_SERVICE");
        intent.setPackage("com.raygun.raygun4android");
        intent.setComponent(new ComponentName(getApplicationContext(), RaygunPostService.class));

        intent.putExtra("msg", jsonPayload);
        intent.putExtra("apikey", apiKey);
        intent.putExtra("isPulse", isPulse);

        RaygunPostService.enqueueWork(getApplicationContext(), intent);
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

    /**
     * Sets the current user of your application. If user is an email address which is associated with a Gravatar,
     * their picture will be displayed in the error view. If setUser is not called a random ID will be assigned.
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
        if (user != null && user.length() > 0) {
            RaygunClient.userInfo = new RaygunUserInfo(user);
        }
    }

    /**
     * Sets the current user of your application. If user is an email address which is associated with a Gravatar,
     * their picture will be displayed in the error view. If setUser is not called a random ID will be assigned.
     * If the user context changes in your application (i.e log in/out), be sure to call this again with the
     * updated user name/email address.
     *
     * @param userInfo A RaygunUserInfo object containing the user data you want to send in its fields.
     */
    public static void setUser(RaygunUserInfo userInfo) {
        RaygunClient.userInfo = userInfo;
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
            RaygunSettings.setApiEndpoint(url);
        } else {
            RaygunLogger.w("A custom crash reporting endpoint can't be null or empty. Custom endpoint has NOT been applied");
        }
    }

    /**
     * Allows the user to set a custom endpoint for Pulse
     *
     * @param url String with the URL to be used
     */
    public static void setCustomPulseEndpoint(String url) {
        if (url != null && !url.isEmpty()) {
            RaygunSettings.setPulseEndpoint(url);
        } else {
            RaygunLogger.w("A custom Pulse endpoint can't be null or empty. Custom endpoint has NOT been applied");
        }
    }

    /**
     * Allows the user to set the maximum number of crash reports stored on the device.
     *
     * The default and maximum value for this is 64. We do not recommend to change this setting
     * unless you have a very good reason and use case.
     *
     * @param maxReportsStoredOnDevice An int with the new maximum number of crash reports
     */
    public static void setMaxReportsStoredOnDevice(int maxReportsStoredOnDevice) {
        RaygunSettings.setMaxReportsStoredOnDevice(maxReportsStoredOnDevice);
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
                Pulse.sendRemainingActivity();
            }
            defaultHandler.uncaughtException(thread, throwable);
        }
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
}
