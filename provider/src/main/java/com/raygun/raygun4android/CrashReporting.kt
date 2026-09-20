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
import java.util.concurrent.TimeUnit

typealias Tags = List<String>

typealias CustomData = Map<String, Any?>

object CrashReporting {
    private const val UNHANDLED_EXCEPTION_TIMEOUT_MILLIS = 2000L
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
                if (CrashReportingWorkerHelper.takeCachedReportRescanRequired()) {
                    postCachedMessages()
                }
                val jsonPayload = buildJsonPayload(throwable, tags, customData) ?: return@launch
                enqueueWorkForCrashReporting(RaygunClient.apiKey, jsonPayload)
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
        val message = buildMessage(throwable)

        if (message == null) {
            RaygunLogger.e("Failed to send RaygunMessage - due to invalid message being built")
            return null
        }

        message.details.tags = RaygunUtils.mergeLists(CrashReporting.tags, tags)
        message.details.customData = RaygunUtils.mergeMaps(CrashReporting.customData, customData)

        val beforeSend = onBeforeSend
        val filteredMessage =
            if (beforeSend != null) {
                beforeSend.onBeforeSend(message) ?: return null
            } else {
                message
            }

        return Gson().toJson(filteredMessage)
    }

    internal fun cacheUnhandledException(
        throwable: Throwable,
        tags: Tags,
        timeoutMillis: Long = UNHANDLED_EXCEPTION_TIMEOUT_MILLIS,
    ): Boolean {
        var stored = false
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        val persistenceThread =
            Thread(
                {
                    try {
                        val jsonPayload =
                            runBlocking { buildJsonPayload(throwable, tags, null) } ?: return@Thread
                        if (Thread.currentThread().isInterrupted || System.nanoTime() >= deadline) {
                            return@Thread
                        }
                        stored =
                            CrashReportCache.store(
                                RaygunClient.getApplicationContext(),
                                jsonPayload,
                            ) != null
                    } catch (throwable: Throwable) {
                        RaygunLogger.e("Failed to cache unhandled exception: $throwable")
                    }
                },
                "RaygunCrashPersistence",
            )
        persistenceThread.start()

        return try {
            persistenceThread.join(timeoutMillis)
            if (persistenceThread.isAlive) {
                persistenceThread.interrupt()
                RaygunLogger.w("Timed out while caching unhandled exception")
                false
            } else {
                stored
            }
        } catch (exception: InterruptedException) {
            persistenceThread.interrupt()
            Thread.currentThread().interrupt()
            RaygunLogger.w("Interrupted while caching unhandled exception")
            false
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
        CrashReportingWorkerHelper.takeCachedReportRescanRequired()
        coroutineScope.launch {
            try {
                for (file in CrashReportCache.files(RaygunClient.getApplicationContext())) {
                    CrashReportingWorkerHelper.enqueueCachedCrashReport(
                        RaygunClient.getApplicationContext(),
                        file,
                        RaygunClient.apiKey,
                    )
                }
            } catch (exception: Exception) {
                RaygunLogger.e("Failed to schedule cached crash reports: $exception")
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
