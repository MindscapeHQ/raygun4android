package com.raygun.raygun4android

import android.os.Build
import com.google.gson.Gson
import com.raygun.raygun4android.logging.RaygunLogger
import com.raygun.raygun4android.messages.crashreporting.RaygunBreadcrumbMessage
import com.raygun.raygun4android.messages.crashreporting.RaygunMessage
import com.raygun.raygun4android.network.ConnectivityUtils
import com.raygun.raygun4android.rum.RUM
import com.raygun.raygun4android.utils.RaygunFileFilter
import com.raygun.raygun4android.utils.RaygunFileUtils
import com.raygun.raygun4android.utils.RaygunUtils
import com.raygun.raygun4android.workers.CrashReportingWorkerHelper
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream

typealias Tags = List<String>

typealias CustomData = Map<String, Any?>

object CrashReporting {
    private var exceptionHandler: RaygunUncaughtExceptionHandler? = null
    private var onBeforeSend: CrashReportingOnBeforeSend? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + CoroutineName("CrashReporting"))

    @JvmField var tags: Tags? = null

    @JvmField var customData: CustomData? = null

    private val breadcrumbs: MutableList<RaygunBreadcrumbMessage> = ArrayList()
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

    private val breadcrumbLock = Any()

    @JvmStatic
    fun recordBreadcrumb(breadcrumb: RaygunBreadcrumbMessage) {
        synchronized(breadcrumbLock) {
            breadcrumbs.add(
                processBreadcrumbLocation(breadcrumb, shouldProcessBreadcrumbLocation()),
            )
        }
    }

    @JvmStatic
    fun clearBreadcrumbs() {
        synchronized(breadcrumbLock) { breadcrumbs.clear() }
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
                var msg = buildMessage(throwable)

                if (msg == null) {
                    RaygunLogger.e(
                        "Failed to send RaygunMessage - due to invalid message being built",
                    )
                    return@launch
                }

                msg.details.tags = RaygunUtils.mergeLists(CrashReporting.tags, tags).toList()
                msg.details.customData =
                    RaygunUtils.mergeMaps(CrashReporting.customData, customData).toMap()

                if (onBeforeSend != null) {
                    msg = onBeforeSend!!.onBeforeSend(msg)
                    if (msg == null) {
                        return@launch
                    }
                }

                enqueueWorkForCrashReporting(RaygunClient.apiKey, Gson().toJson(msg))
                postCachedMessages()
            }
        } else {
            RaygunLogger.w(
                "Crash Reporting is not enabled, please enable to use the send() function",
            )
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
                    .setBreadcrumbs(breadcrumbs.toList())
                    .build()

            if (RaygunClient.version != null) {
                msg.details.version = RaygunClient.version
            }

            if (RaygunClient.user != null) {
                msg.details.userInfo = RaygunClient.user
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
        if (ConnectivityUtils.isNetworkAvailable(RaygunClient.getApplicationContext())) {
            coroutineScope.launch {
                val fileList =
                    withContext(Dispatchers.IO) {
                        RaygunClient.getApplicationContext().cacheDir.listFiles(RaygunFileFilter())
                    }
                if (fileList != null) {
                    for (f in fileList) {
                        try {
                            if (
                                RaygunFileUtils
                                    .getExtension(f.name)
                                    .equals(
                                        RaygunSettings.DEFAULT_FILE_EXTENSION,
                                        ignoreCase = true,
                                    )
                            ) {
                                var ois: ObjectInputStream? = null
                                try {
                                    ois = ObjectInputStream(FileInputStream(f))
                                    val serializedMessage = ois.readObject() as SerializedMessage
                                    enqueueWorkForCrashReporting(
                                        RaygunClient.apiKey,
                                        serializedMessage.message,
                                    )
                                    if (!f.delete()) {
                                        RaygunLogger.w(
                                            "Couldn't delete cached report (" + f.name + ")",
                                        )
                                    }
                                } finally {
                                    ois?.close()
                                }
                            }
                        } catch (e: FileNotFoundException) {
                            RaygunLogger.e(
                                "Error loading cached message from filesystem - " + e.message,
                            )
                        } catch (e: IOException) {
                            RaygunLogger.e(
                                "Error reading cached message from filesystem - " + e.message,
                            )
                        } catch (e: ClassNotFoundException) {
                            RaygunLogger.e(
                                "Error in handling cached message from filesystem - " + e.message,
                            )
                        }
                    }
                } else {
                    RaygunLogger.e(
                        "Error in handling cached message from filesystem - could not get a list of" +
                            " files from cache dir",
                    )
                }
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
            send(throwable, tags)
            RUM.instance.sendRemaining()
            defaultHandler.uncaughtException(thread, throwable)
        }
    }
}
