package com.raygun.raygun4android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.raygun.raygun4android.logging.RaygunLogger;
import com.raygun.raygun4android.logging.TimberRaygunLoggerImplementation;
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage;
import com.raygun.raygun4android.messages.shared.RaygunUserInfo;
import com.raygun.raygun4android.utils.RaygunFileUtils;

import java.util.List;
import java.util.Map;
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
    private static Context applicationContext;

    private static boolean crashReportingEnabled = false;
    private static boolean RUMEnabled = false;

    /**
     * Initializes the Raygun client. This expects that you have placed the API key in your
     * AndroidManifest.xml, in a meta-data element.
     *
     * @param application The Android application
     */
    public static void init(Application application) {
        init(application, null, null);
    }

    /**
     * Initializes the Raygun client with your Android application and your Raygun API key. The version
     * transmitted will be the value of the versionName attribute in your manifest element.
     *
     * @param application The Android application
     * @param apiKey An API key that belongs to a Raygun application created in your dashboard
     */
    public static void init(Application application, String apiKey) {
        init(application, apiKey, null);
    }

    /**
     * Initializes the Raygun client with applicationContext and your Raygun API key. The version
     * transmitted will be the value of the versionName attribute in your manifest element. This
     * function should be used by 3rd party library.
     *
     * @param Context The Android applicationContext
     * @param apiKey An API key that belongs to a Raygun application created in your dashboard
     */
    public static void init(Context applicationContext, String apiKey) {
        TimberRaygunLoggerImplementation.init();

        RaygunLogger.d("Configuring Raygun4Android (v" + RaygunSettings.RAYGUN_CLIENT_VERSION + ")");

        RaygunClient.applicationContext = applicationContext;

        if (apiKey == null || apiKey.trim().isEmpty()) {
            RaygunClient.apiKey = readApiKey(getApplicationContext());
        } else {
            RaygunClient.apiKey = apiKey;
        }

        RaygunClient.appContextIdentifier = UUID.randomUUID().toString();

        if (version == null || version.trim().isEmpty()) {
            try {
                RaygunClient.version = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                RaygunClient.version = "Not Provided";
                RaygunLogger.w("Couldn't read application version from calling package");
            }
        } else {
            RaygunClient.version = version;
        }

        CrashReporting.postCachedMessages();
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

        TimberRaygunLoggerImplementation.init();

        RaygunLogger.d("Configuring Raygun4Android (v" + RaygunSettings.RAYGUN_CLIENT_VERSION + ")");

        RaygunClient.application = application;

        if (apiKey == null || apiKey.trim().isEmpty()) {
           RaygunClient.apiKey = readApiKey(getApplicationContext());
        } else {
            RaygunClient.apiKey = apiKey;
        }

        RaygunClient.appContextIdentifier = UUID.randomUUID().toString();

        if (version == null || version.trim().isEmpty()) {
            try {
                RaygunClient.version = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                RaygunClient.version = "Not Provided";
                RaygunLogger.w("Couldn't read application version from calling package");
            }
        } else {
            RaygunClient.version = version;
        }

        CrashReporting.postCachedMessages();
    }

    /**
     * Sends an exception-type object to Raygun.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
     */
    public static void send(Throwable throwable) {
        CrashReporting.send(throwable, null, null);
    }

    /**
     * Sends an exception-type object to Raygun with a list of tags you specify.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
     * @param tags      A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
     *                  This could be a build tag, lifecycle state, debug/production version etc.
     */
    public static void send(Throwable throwable, List tags) {
        CrashReporting.send(throwable, tags, null);
    }

    /**
     * Sends an exception-type object to Raygun with a list of tags you specify, and a set of custom data.
     *
     * @param throwable     The Throwable object that occurred in your application that will be sent to Raygun.
     * @param tags          A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
     *                       This could be a build tag, lifecycle state, debug/production version etc.
     * @param customData    A set of custom key-value pairs relating to your application and its current state. This is a bucket
     *                       where you can attach any related data you want to see to the error.
     */
    public static void send(Throwable throwable, List tags, Map customData) {
        CrashReporting.send(throwable, tags, customData);
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

    static RaygunUserInfo getUser() {
        return RaygunClient.userInfo;
    }

    /**
     * Manually stores the version of your application to be transmitted with each message, for version
     * filtering. This is normally read from your AndroidManifest.xml (the versionName attribute
     * on manifest element) or passed in on init(); this is only provided as a convenience.
     *
     * @param version The version of your application, format x.x.x.x, where x is a positive integer.
     */
    public static void setVersion(String version) {
        if (version != null) {
            RaygunClient.version = version;
        }
    }

    static String getVersion() {
        return RaygunClient.version;
    }

    static String getApiKey() {
        return RaygunClient.apiKey;
    }

    static String getAppContextIdentifier() {
        return appContextIdentifier;
    }

    /**
     * Sets a List of tags which will be sent along with every exception. This will be merged
     * with any other tags passed as the second param of send().
     *
     * @param tags List object containing tags to be sent to Raygun
     */
    public static void setTags(List tags) {
        CrashReporting.setTags(tags);
    }

    /**
     * Sets a key-value Map which, like the tags, will be sent along with every exception.
     * This will be merged with any other tags passed as the third param of send().
     *
     * @param customData Map with custom user data to be sent to Raygun
     */
    public static void setCustomData(Map customData) {
        CrashReporting.setCustomData(customData);
    }

    /**
     * Records a breadcrumb via a string message
     *
     * @param message Message for the breadcrumb
     */
    public static void recordBreadcrumb(String message) {
        CrashReporting.recordBreadcrumb(message);
    }

    /**
     * Records a breadcrumb as a RaygunBreadcrumbMessage
     *
     * @param breadcrumb RaygunBreadcrumbMessage object containing the breadcrumb
     */
    public static void recordBreadcrumb(RaygunBreadcrumbMessage breadcrumb) {
        CrashReporting.recordBreadcrumb(breadcrumb);
    }

    /**
     * Clears breadcrumbs
     *
     */
    public static void clearBreadcrumbs() {
        CrashReporting.clearBreadcrumbs();
    }

    /**
     * Enables the processing of the full location of breadcrumb messages. This defaults to false and please
     * be aware that setting this to true could seriously degrade the performance of your application.
     *
     * @param shouldProcessBreadcrumbLocation enable or disable the full location processing of breadcrumb messages
     */
    public static void shouldProcessBreadcrumbLocation(boolean shouldProcessBreadcrumbLocation) {
        CrashReporting.shouldProcessBreadcrumbLocation(shouldProcessBreadcrumbLocation);
    }

    /**
     * Sets an instance of a class which has an onBeforeSend method that can be used to inspect,
     * mutate or cancel the send to the Raygun API immediately before it happens. Can be used to
     * filter arbitrary data.
     *
     * @param onBeforeSend Instance of type CrashReportingOnBeforeSend
     */
    public static void setOnBeforeSend(CrashReportingOnBeforeSend onBeforeSend) {
        CrashReporting.setOnBeforeSend(onBeforeSend);
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
            RaygunLogger.w("A custom crash reporting endpoint can't be null or empty. Custom endpoint has NOT been applied and default will be used.");
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
            RaygunLogger.w("A custom RUM endpoint can't be null or empty. Custom endpoint has NOT been applied and default will be used.");
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

    /**
     * Returns the status of Crash Reporting
     *
     * @return true or false, indicating if Crash Reporting is enabled or not.
     */
    public static boolean isCrashReportingEnabled() {
        return crashReportingEnabled;
    }

    /**
     * Enables the Raygun Crash Reporting feature with the default exception handler enabled.
     */
    public static void enableCrashReporting() {
        enableCrashReporting(true);
    }

    /**
     * Enables the Raygun Crash Reporting feature. The default exception handler can be toggled with an additional parameter.
     *
     * @param attachDefaultHandler Automatically report all unhandled exceptions.
     */
    public static void enableCrashReporting(boolean attachDefaultHandler) {
        RaygunClient.crashReportingEnabled = true;

        if (attachDefaultHandler) {
            attachExceptionHandler();
        }
    }

    /**
     * Returns the status of RUM
     *
     * @return true or false, indicating if RUM is enabled or not.
     */
    public static boolean isRUMEnabled() {
        return RUMEnabled;
    }

    /**
     * Enables the Raygun RUM feature which will automatically report session and view events. Network logging will be enabled for RUM by default.
     *
     * @param activity The main/entry activity of the Android app.
     */
    public static void enableRUM(Activity activity) {
        enableRUM(activity, true);
    }

    /**
     * Enables the Raygun RUM feature which will automatically report session and view events. Network logging can be toggled with an additional parameter.
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

    private static String readApiKey(Context context) {
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.getString(RaygunSettings.APIKEY_MANIFEST_FIELD);
        } catch (PackageManager.NameNotFoundException e) {
            RaygunLogger.e("Couldn't read API key from your AndroidManifest.xml <meta-data /> element; cannot send. Detailed error: " + e.getMessage());
        } catch (NullPointerException e) {
            RaygunLogger.e("Couldn't find <meta-data /> element for your API key in the AndroidManifest.xml element; cannot send. Detailed error: " + e.getMessage());
        }

        return null;
    }

    private static void attachExceptionHandler() {
        CrashReporting.attachExceptionHandler();
    }

    /**
     * Returns the current Application's context.
     *
     * @return The current application Context.
     * @throws java.lang.IllegalStateException if init() has not been called.
     */
    public static Context getApplicationContext() {
        if (RaygunClient.application != null) {
            return RaygunClient.application.getApplicationContext();
        }
        if (RaygunClient.applicationContext != null) {
            return RaygunClient.applicationContext;
        }

        throw new IllegalStateException("init() must be called first.");
    }
}
