package com.raygun.raygun4android

import android.os.Build
import com.google.gson.Gson
import com.raygun.raygun4android.logging.RaygunLogger
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage
import com.raygun.raygun4android.rum.RUM
import com.raygun.raygun4android.utils.RaygunUtils
import com.raygun.raygun4android.workers.CrashReportCache
import com.raygun.raygun4android.workers.CrashReportingWorkerHelper
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList

typealias Tags = List<String>

typealias CustomData = Map<String, Any?>

object CrashReporting {
    private var exceptionHandler: RaygunUncaughtExceptionHandler? = null
    private var onBeforeSend: CrashReportingOnBeforeSend? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("CrashReporting"))

    @JvmField var tags: Tags? = null

    @JvmField var customData: CustomData? = null

    private val breadcrumbs: CopyOnWriteArrayList<RaygunBreadcrumbMessage> = CopyOnWriteArrayList()
    private var shouldProcessBreadcrumbLocation = false

    @JvmStatic
    fun setOnBeforeSend(onBeforeSend: CrashReportingOnBeforeSend?) {
        CrashReporting.onBeforeSend = onBeforeSend
    }

    @JvmStatic
    fun recordBreadcrumb(message: String?) {
        val breadcrumb = RaygunBreadcrumbMessage.Builder(message).build()
        recordBreadcrumb(breadcrumb)
    }

    @JvmStatic
    fun recordBreadcrumb(breadcrumb: RaygunBreadcrumbMessage) {
        breadcrumbs.add(processBreadcrumbLocation(breadcrumb, shouldProcessBreadcrumbLocation()))
    }

    @JvmStatic
    fun clearBreadcrumbs() {
        breadcrumbs.clear()
    }

    private fun processBreadcrumbLocation(
        breadcrumb: RaygunBreadcrumbMessage,
        shouldProcessBreadcrumbLocation: Boolean,
    ): RaygunBreadcrumbMessage {
        if (shouldProcessBreadcrumbLocation && breadcrumb.className == null) {
            val trace = Thread.currentThread().stackTrace
            var frame: StackTraceElement? = null

            if (trace.isNotEmpty()) {
                for (i in 0..<trace.size - 1) {
                    val thisFrame = trace[i]
                    val nextFrame = trace[i + 1]

                    if (
                        thisFrame.className.contains("com.raygun.raygun4android.") &&
                        !nextFrame.className.contains("com.raygun.raygun4android.")
                    ) {
                        frame = nextFrame
                        break
                    }
                }
            }

            if (frame != null) {
                return RaygunBreadcrumbMessage
                    .Builder(breadcrumb.message)
                    .category(breadcrumb.category)
                    .customData(breadcrumb.customData)
                    .level(breadcrumb.level)
                    .className(frame.className)
                    .methodName(frame.methodName)
                    .lineNumber(frame.lineNumber)
                    .build()
            }
        }

        return breadcrumb
    }

    private fun shouldProcessBreadcrumbLocation(): Boolean = shouldProcessBreadcrumbLocation

    @JvmStatic
    fun shouldProcessBreadcrumbLocation(shouldProcessBreadcrumbLocation: Boolean) {
        CrashReporting.shouldProcessBreadcrumbLocation = shouldProcessBreadcrumbLocation
    }

    @JvmStatic
    @JvmOverloads
    fun send(
        throwable: Throwable,
        tags: Tags?,
        customData: CustomData? = null,
    ) {
        if (RaygunClient.isCrashReportingEnabled) {
            coroutineScope.launch {
                val jsonPayload = buildJsonPayload(throwable, tags, customData) ?: return@launch
                enqueueWorkForCrashReporting(RaygunClient.apiKey, jsonPayload)
                postCachedMessages()
            }
        } else {
            RaygunLogger.w(
                "Crash Reporting is not enabled, please enable to use the send() function",
            )
        }
    }

    private suspend fun buildJsonPayload(
        throwable: Throwable,
        tags: Tags?,
        customData: CustomData?,
    ): String? {
        var msg = buildMessage(throwable)

        if (msg == null) {
            RaygunLogger.e("Failed to send RaygunMessage - due to invalid message being built")
            return null
        }

        msg.details.tags = RaygunUtils.mergeLists(CrashReporting.tags, tags)
        msg.details.customData = RaygunUtils.mergeMaps(CrashReporting.customData, customData)

        if (onBeforeSend != null) {
            msg = onBeforeSend!!.onBeforeSend(msg) ?: return null
        }

        return Gson().toJson(msg)
    }

    private fun cacheUnhandledException(
        throwable: Throwable,
        tags: Tags,
    ) {
        runBlocking(Dispatchers.IO) {
            val jsonPayload = buildJsonPayload(throwable, tags, null) ?: return@runBlocking
            CrashReportCache.store(RaygunClient.getApplicationContext(), jsonPayload)
        }
    }

    private suspend fun buildMessage(throwable: Throwable): RaygunMessage? {
        try {
            val msg =
                RaygunMessageBuilder()
                    .setEnvironmentDetails(RaygunClient.getApplicationContext())
                    .setMachineName(Build.MODEL)
                    .setExceptionDetails(throwable)
                    .setClientDetails()
                    .setAppContext(RaygunClient.appContextIdentifier)
                    .setVersion(RaygunClient.version)
                    .setNetworkInfo(RaygunClient.getApplicationContext())
                    .setBreadcrumbs(breadcrumbs)
                    .build()

            if (RaygunClient.version != null) {
                msg.details.version = RaygunClient.version
            }

            if (RaygunClient.user != null) {
                msg.details.user = RaygunClient.user
            } else {
                msg.details.setUserInfo()
            }

            return msg
        } catch (e: Exception) {
            RaygunLogger.e("Failed to build RaygunMessage - $e")
        }
        return null
    }

    @JvmStatic
    fun attachExceptionHandler() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (oldHandler !is RaygunUncaughtExceptionHandler && oldHandler != null) {
            exceptionHandler = RaygunUncaughtExceptionHandler(oldHandler)
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
        }
    }

    @JvmStatic
    fun postCachedMessages() {
        coroutineScope.launch {
            for (file in CrashReportCache.files(RaygunClient.getApplicationContext())) {
                CrashReportingWorkerHelper.enqueueCachedCrashReport(
                    RaygunClient.getApplicationContext(),
                    file,
                    RaygunClient.apiKey,
                )
            }
        }
    }

    private fun enqueueWorkForCrashReporting(
        apiKey: String?,
        jsonPayload: String,
    ) {
        CrashReportingWorkerHelper.enqueueCrashReport(
            RaygunClient.getApplicationContext(),
            jsonPayload,
            apiKey,
        )
    }

    class RaygunUncaughtExceptionHandler(
        private val defaultHandler: Thread.UncaughtExceptionHandler,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(
            thread: Thread,
            throwable: Throwable,
        ) {
            val tags = listOf(RaygunSettings.CRASH_REPORTING_UNHANDLED_EXCEPTION_TAG)
            try {
                cacheUnhandledException(throwable, tags)
                RUM.instance.sendRemaining()
            } catch (exception: Exception) {
                RaygunLogger.e("Failed to cache unhandled exception: $exception")
            } finally {
                defaultHandler.uncaughtException(thread, throwable)
            }
        }
    }
}
