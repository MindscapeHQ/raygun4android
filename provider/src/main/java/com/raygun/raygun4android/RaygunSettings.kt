@file:Suppress("MemberVisibilityCanBePrivate")

package com.raygun.raygun4android

import com.raygun.raygun4android.logging.RaygunLogger.w
import com.raygun.raygun4android.network.RaygunOkHttpClientBuilder
import okhttp3.OkHttpClient

object RaygunSettings {
    // General
    const val APIKEY_MANIFEST_FIELD: String = "com.raygun.raygun4android.apikey"
    const val RAYGUN_CLIENT_VERSION: String = BuildConfig.VERSION_NAME
    const val LOGGING_TAG: String = "Raygun4Android"

    // HTTP error response codes
    const val RESPONSE_CODE_ACCEPTED: Int = 202
    const val RESPONSE_CODE_BAD_MESSAGE: Int = 400
    const val RESPONSE_CODE_INVALID_API_KEY: Int = 403
    const val RESPONSE_CODE_LARGE_PAYLOAD: Int = 413
    const val RESPONSE_CODE_RATE_LIMITED: Int = 429

    // Crash Reporting
    const val DEFAULT_CRASHREPORTING_ENDPOINT: String = "https://api.raygun.io/entries"
    const val DEFAULT_FILE_EXTENSION: String = "raygun4"
    const val DEFAULT_MAX_REPORTS_STORED_ON_DEVICE: Int = 64
    const val CRASH_REPORTING_UNHANDLED_EXCEPTION_TAG: String = "UnhandledException"

    // RUM
    const val RUM_EVENT_SESSION_START: String = "session_start"
    const val RUM_EVENT_SESSION_END: String = "session_end"
    const val RUM_EVENT_TIMING: String = "mobile_event_timing"
    const val DEFAULT_RUM_ENDPOINT: String = "https://api.raygun.io/events"

    // 30 minutes in milliseconds
    const val RUM_SESSION_EXPIRY: Int = 30 * 60 * 1000

    private val ignoredURLs = IgnoredURLs("api.raygun.io")

    val ignoredViews: HashSet<String> = HashSet()

    @JvmStatic
    var maxReportsStoredOnDevice: Int = DEFAULT_MAX_REPORTS_STORED_ON_DEVICE
        set(maxReportsStoredOnDevice) {
            if (maxReportsStoredOnDevice <= DEFAULT_MAX_REPORTS_STORED_ON_DEVICE) {
                field = maxReportsStoredOnDevice
            } else {
                w(
                    (
                        "It's not possible to exceed the value " +
                            DEFAULT_MAX_REPORTS_STORED_ON_DEVICE +
                            " for the number of reports stored on the device. The setting has not" +
                            " been applied."
                    ),
                )
            }
        }

    @JvmField var crashReportingEndpoint: String = DEFAULT_CRASHREPORTING_ENDPOINT

    var rumEndpoint: String = DEFAULT_RUM_ENDPOINT

    @JvmField var okHttpClientBuilder: OkHttpClientBuilder? = null

    fun getIgnoredURLs(): HashSet<String> = ignoredURLs

    fun ignoreURLs(urls: Array<String?>?) {
        if (urls != null) {
            for (url in urls) {
                if (url != null) {
                    ignoredURLs.add(url)
                }
            }
        }
    }

    fun ignoreViews(views: Array<String?>?) {
        if (views != null) {
            for (view in views) {
                if (view != null) {
                    ignoredViews.add(view)
                }
            }
        }
    }

    val httpClient: OkHttpClient
        get() {
            return if (okHttpClientBuilder != null) {
                okHttpClientBuilder!!.build()
            } else {
                RaygunOkHttpClientBuilder.instance.build()
            }
        }

    class IgnoredURLs internal constructor(
        vararg defaultIgnoredUrls: String,
    ) : HashSet<String>() {
        init {
            this.addAll(defaultIgnoredUrls.toList())
        }
    }
}
