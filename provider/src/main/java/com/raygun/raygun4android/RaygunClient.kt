@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.raygun.raygun4android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import com.raygun.raygun4android.CrashReporting.postCachedMessages
import com.raygun.raygun4android.RaygunSettings.rumEndpoint
import com.raygun.raygun4android.logging.RaygunLogger.d
import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.logging.RaygunLogger.w
import com.raygun.raygun4android.logging.TimberRaygunLoggerImplementation.init
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.shared.RaygunUserInfo
import com.raygun.raygun4android.messages.shared.RaygunUserInfo.Companion.create
import com.raygun.raygun4android.rum.RUM.Companion.instance
import com.raygun.raygun4android.utils.RaygunFileUtils.clearCachedReports
import java.util.UUID

/**
 * The official Raygun provider for Android. This is the main class that provides functionality for
 * automatically sending exceptions to the Raygun service.
 *
 * You should call init() on the static RaygunClient instance, passing in the application, instead
 * of instantiating this class.
 */
object RaygunClient {
    var apiKey: String? = null
        private set

    var version: String? = null
        private set

    var appContextIdentifier: String? = null
        private set

    var user: RaygunUserInfo? = null
        private set

    // During initialization, either application or applicationContext will be set.
    private var application: Application? = null

    private var applicationContext: Context? = null

    /**
     * Returns the status of Crash Reporting
     *
     * @return true or false, indicating if Crash Reporting is enabled or not.
     */
    var isCrashReportingEnabled: Boolean = false
        private set

    /**
     * Returns the status of RUM
     *
     * @return true or false, indicating if RUM is enabled or not.
     */
    var isRUMEnabled: Boolean = false
        private set

    /**
     * Initializes the Raygun client with an Android application context, your Raygun API key and
     * the version of your application.
     *
     * This method is intended to be used by 3rd-party libraries such as Raygun for React Native or
     * Raygun for Flutter etc.
     *
     * @param applicationContext The Android applicationContext
     * @param apiKey An API key that belongs to a Raygun application created in your dashboard
     * @param version The version of your application, format x.x.x.x, where x is a positive
     *   integer.
     */
    @JvmOverloads
    fun init(
        applicationContext: Context,
        apiKey: String? = null,
        version: String? = null,
    ) {
        RaygunClient.applicationContext = applicationContext
        sharedSetup(apiKey, version)
    }

    /**
     * Initializes the Raygun client with your Android application, your Raygun API key, and the
     * version of your application
     *
     * @param application The Android application
     * @param apiKey An API key that belongs to a Raygun application created in your dashboard
     * @param version The version of your application, format x.x.x.x, where x is a positive
     *   integer.
     */
    @JvmOverloads
    fun init(
        application: Application,
        apiKey: String? = null,
        version: String? = null,
    ) {
        RaygunClient.application = application
        sharedSetup(apiKey, version)
    }

    private fun sharedSetup(
        apiKey: String?,
        version: String?,
    ) {
        init()

        d("Configuring Raygun4Android (v" + RaygunSettings.RAYGUN_CLIENT_VERSION + ")")

        if (apiKey == null || apiKey.trim { it <= ' ' }.isEmpty()) {
            RaygunClient.apiKey = readApiKey(getApplicationContext())
        } else {
            RaygunClient.apiKey = apiKey
        }

        appContextIdentifier = UUID.randomUUID().toString()

        if (version == null || version.trim { it <= ' ' }.isEmpty()) {
            try {
                RaygunClient.version =
                    getApplicationContext()
                        .packageManager
                        .getPackageInfo(getApplicationContext().packageName, 0)
                        .versionName
            } catch (e: PackageManager.NameNotFoundException) {
                RaygunClient.version = "Not Provided"
                w("Couldn't read application version from calling package")
            }
        } else {
            RaygunClient.version = version
        }

        postCachedMessages()
    }

    /**
     * Sends an exception-type object to Raygun.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to
     *   Raygun.
     */
    fun send(throwable: Throwable) {
        CrashReporting.send(throwable, null, null)
    }

    /**
     * Sends an exception-type object to Raygun with a list of tags you specify.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to
     *   Raygun.
     * @param tags A list of data that will be attached to the Raygun message and visible on the
     *   error in the dashboard. This could be a build tag, lifecycle state, debug/production
     *   version etc.
     */
    fun send(
        throwable: Throwable,
        tags: Tags?,
    ) {
        CrashReporting.send(throwable, tags, null)
    }

    /**
     * Sends an exception-type object to Raygun with a list of tags you specify, and a set of custom
     * data.
     *
     * @param throwable The Throwable object that occurred in your application that will be sent to
     *   Raygun.
     * @param tags A list of data that will be attached to the Raygun message and visible on the
     *   error in the dashboard. This could be a build tag, lifecycle state, debug/production
     *   version etc.
     * @param customData A set of custom key-value pairs relating to your application and its
     *   current state. This is a bucket where you can attach any related data you want to see to
     *   the error.
     */
    fun send(
        throwable: Throwable,
        tags: Tags?,
        customData: CustomData?,
    ) {
        CrashReporting.send(throwable, tags, customData)
    }

    /**
     * Sends an exception name and reason to Raygun by constructing a Throwable object from it. Adds
     * a list of tags you specify, and a set of custom data.
     *
     * @param exceptionName The name or description of the exception that occurred in your
     *   application that will be sent to Raygun.
     * @param reason The reason for the exception that occurred in your application that will be
     *   sent to Raygun.
     * @param tags A list of data that will be attached to the Raygun message and visible on the
     *   error in the dashboard. This could be a build tag, lifecycle state, debug/production
     *   version etc.
     * @param customData A set of custom key-value pairs relating to your application and its
     *   current state. This is a bucket where you can attach any related data you want to see to
     *   the error.
     */
    fun send(
        exceptionName: String?,
        reason: String?,
        tags: Tags?,
        customData: CustomData?,
    ) {
        CrashReporting.send(Throwable(exceptionName, Throwable(reason)), tags, customData)
    }

    /**
     * Sets the current user of your application. If user is an email address which is associated
     * with a Gravatar, their picture will be displayed in the error view. If setUser is not called,
     * a random ID will be assigned. If the user context changes in your application (i.e log
     * in/out), be sure to call this again with the updated user name/email address.
     *
     * If you use an email address to identify the user, please consider using
     * setUser(RaygunUserInfo userInfo) instead of this method as it would allow you to set the
     * email address into both the identifier and email fields of the crash data to be sent.
     *
     * @param user A user name or email address representing the current user.
     */
    fun setUser(user: String) {
        val newUser = create(user)
        setUser(newUser)
    }

    /**
     * Sets the current user of your application. If user is an email address which is associated
     * with a Gravatar, their picture will be displayed in the error view. If setUser is not called,
     * a random ID will be assigned. If the user context changes in your application (i.e log
     * in/out), be sure to call this again with the updated user name/email address.
     *
     * @param userInfo A RaygunUserInfo object containing the user data you want to send in its
     *   fields.
     */
    fun setUser(userInfo: RaygunUserInfo) {
        if (isRUMEnabled) {
            instance.updateCurrentSessionUser(userInfo)
        }
        user = userInfo
    }

    /**
     * Manually stores the version of your application to be transmitted with each message, for
     * version filtering. This is normally read from your AndroidManifest.xml (the versionName
     * attribute on manifest element) or passed in on init(); this is only provided as a
     * convenience.
     *
     * @param version The version of your application, format x.x.x.x, where x is a positive
     *   integer.
     */
    fun setVersion(version: String?) {
        if (version != null) {
            RaygunClient.version = version
        }
    }

    /**
     * Sets a List of tags which will be sent along with every exception. This will be merged with
     * any other tags passed as the second param of send().
     *
     * @param tags List object containing tags to be sent to Raygun
     */
    fun setTags(tags: Tags?) {
        CrashReporting.tags = tags
    }

    /**
     * Sets a key-value Map which, like the tags, will be sent along with every exception. This will
     * be merged with any other tags passed as the third param of send().
     *
     * @param customData Map with custom user data to be sent to Raygun
     */
    fun setCustomData(customData: CustomData?) {
        CrashReporting.customData = customData
    }

    /**
     * Records a breadcrumb via a string message
     *
     * @param message Message for the breadcrumb
     */
    fun recordBreadcrumb(message: String?) {
        CrashReporting.recordBreadcrumb(message)
    }

    /**
     * Records a breadcrumb as a RaygunBreadcrumbMessage
     *
     * @param breadcrumb RaygunBreadcrumbMessage object containing the breadcrumb
     */
    fun recordBreadcrumb(breadcrumb: RaygunBreadcrumbMessage) {
        CrashReporting.recordBreadcrumb(breadcrumb)
    }

    /** Clears breadcrumbs */
    fun clearBreadcrumbs() {
        CrashReporting.clearBreadcrumbs()
    }

    /**
     * Enables the processing of the full location of breadcrumb messages. This defaults to false
     * and please be aware that setting this to true could seriously degrade the performance of your
     * application.
     *
     * @param shouldProcessBreadcrumbLocation enable or disable the full location processing of
     *   breadcrumb messages
     */
    fun shouldProcessBreadcrumbLocation(shouldProcessBreadcrumbLocation: Boolean) {
        CrashReporting.shouldProcessBreadcrumbLocation(shouldProcessBreadcrumbLocation)
    }

    /**
     * Sets an instance of a class which has an onBeforeSend method that can be used to inspect,
     * mutate or cancel the send to the Raygun API immediately before it happens. Can be used to
     * filter arbitrary data.
     *
     * @param onBeforeSend Instance of type CrashReportingOnBeforeSend
     */
    fun setOnBeforeSend(onBeforeSend: CrashReportingOnBeforeSend?) {
        CrashReporting.setOnBeforeSend(onBeforeSend)
    }

    /**
     * Allows the user to add more URLs to filter out, so network timing events are not sent for
     * them.
     *
     * @param urls An array of urls to filter out by.
     */
    fun ignoreURLs(urls: Array<String?>?) {
        RaygunSettings.ignoreURLs(urls)
    }

    /**
     * Allows the user to add more views to filter out, so load timing events are not sent for them.
     *
     * @param views An array of activity names to filter out by.
     */
    fun ignoreViews(views: Array<String?>?) {
        RaygunSettings.ignoreViews(views)
    }

    /**
     * Allows the user to set a custom endpoint for Crash Reporting
     *
     * @param url String with the URL to be used
     */
    fun setCustomCrashReportingEndpoint(url: String?) {
        if (!url.isNullOrEmpty()) {
            RaygunSettings.crashReportingEndpoint = url
        } else {
            w(
                "A custom crash reporting endpoint can't be null or empty. Custom endpoint has" +
                    " NOT been applied and default will be used.",
            )
        }
    }

    /**
     * Allows the user to set a custom endpoint for RUM
     *
     * @param url String with the URL to be used
     */
    fun setCustomRUMEndpoint(url: String?) {
        if (!url.isNullOrEmpty()) {
            rumEndpoint = url
        } else {
            w(
                "A custom RUM endpoint can't be null or empty. Custom endpoint has NOT been" +
                    " applied and default will be used.",
            )
        }
    }

    /**
     * Allows the user to set the maximum number of crash reports stored on the device.
     *
     * The default and maximum value for this is 64. We do not recommend to change this setting
     * unless you have a very good reason and use case.
     *
     * If you decrease the value of maxReportsStoredOnDevice, all currently cached reports will be
     * deleted.
     *
     * @param maxReportsStoredOnDevice An int with the new maximum number of crash reports
     */
    fun setMaxReportsStoredOnDevice(maxReportsStoredOnDevice: Int) {
        val currentMaxReportsStoredOnDevice = RaygunSettings.maxReportsStoredOnDevice

        if (maxReportsStoredOnDevice < currentMaxReportsStoredOnDevice) {
            clearCachedReports(getApplicationContext())
        }

        RaygunSettings.maxReportsStoredOnDevice = maxReportsStoredOnDevice
    }

    /**
     * Enables the Raygun Crash Reporting feature. The default exception handler can be toggled with
     * an additional parameter.
     *
     * @param attachDefaultHandler Automatically report all unhandled exceptions.
     */
    @JvmOverloads
    fun enableCrashReporting(attachDefaultHandler: Boolean = true) {
        isCrashReportingEnabled = true

        if (attachDefaultHandler) {
            CrashReporting.attachExceptionHandler()
        }
    }

    /**
     * Enables the Raygun RUM feature which will automatically report session and view events.
     * Network logging can be toggled with an additional parameter.
     *
     * @param activity The main/entry activity of the Android app.
     * @param networkLogging Automatically report the performance of network requests.
     */
    @JvmOverloads
    fun enableRUM(
        activity: Activity,
        networkLogging: Boolean = true,
    ) {
        isRUMEnabled = true
        instance.attach(activity, networkLogging)
        if (user != null) {
            instance.updateCurrentSessionUser(user!!)
        }
    }

    private fun readApiKey(context: Context): String? {
        try {
            val ai =
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA,
                )
            val bundle = ai.metaData
            return bundle.getString(RaygunSettings.APIKEY_MANIFEST_FIELD)
        } catch (e: PackageManager.NameNotFoundException) {
            e(
                (
                    "Couldn't read API key from your AndroidManifest.xml <meta-data /> element;" +
                        " cannot send. Detailed error: " +
                        e.message
                ),
            )
        } catch (e: NullPointerException) {
            e(
                (
                    "Couldn't find <meta-data /> element for your API key in the" +
                        " AndroidManifest.xml element; cannot send. Detailed error: " +
                        e.message
                ),
            )
        }

        return null
    }

    /**
     * Returns the current Application's context.
     *
     * @return The current application context.
     * @throws IllegalStateException if init() has not been called.
     */
    fun getApplicationContext(): Context {
        if (application != null) {
            return application!!.applicationContext
        }
        if (applicationContext != null) {
            return applicationContext!!
        }

        throw IllegalStateException("init() must be called first.")
    }

    /**
     * Customizable OkHttpClient builder e.g. provide custom SSL Context. Setting to null uses
     * default internal builder.
     */
    fun setOkHttpClientBuilder(okHttpClientBuilder: OkHttpClientBuilder?) {
        RaygunSettings.okHttpClientBuilder = okHttpClientBuilder
    }
}
