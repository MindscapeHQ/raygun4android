package com.raygun.raygun4android.network

import com.raygun.raygun4android.RaygunRUMEventType
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.network.http.RaygunUrlStreamHandlerFactory
import com.raygun.raygun4android.rum.RUM
import java.net.URL

object RaygunNetworkLogger {
    private const val CONNECTION_TIMEOUT = 60000L // 1 min
    private val connections = HashMap<String?, RaygunNetworkRequestInfo>()
    private var loggingEnabled = true
    private var loggingInitialized = false

    @JvmStatic
    fun init() {
        if (loggingEnabled && !loggingInitialized) {
            try {
                val factory = RaygunUrlStreamHandlerFactory()
                URL.setURLStreamHandlerFactory(factory)
                loggingInitialized = true
            } catch (e: SecurityException) {
                loggingInitialized = false
            }
        }
    }

    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        loggingEnabled = enabled
    }

    @Synchronized
    fun startNetworkCall(
        url: String?,
        startTime: Long,
    ) {
        if (!shouldIgnoreURL(url) && loggingEnabled) {
            removeOldEntries()
            val id = sanitiseURL(url)
            connections[id] = RaygunNetworkRequestInfo(url, startTime)
        }
    }

    @Synchronized
    fun endNetworkCall(
        url: String?,
        requestMethod: String,
        endTime: Long,
        statusCode: Int,
    ) {
        if (url != null) {
            val id = sanitiseURL(url)
            if ((connections.containsKey(id))) {
                val request = connections[id]
                if (request != null) {
                    sendNetworkTimingEvent(
                        request.url,
                        requestMethod,
                        request.startTime,
                        endTime,
                        statusCode,
                        null,
                    )
                    connections.remove(id)
                }
            }
        }
    }

    /**
     * When a network request is cancelled we stop tracking it and do not send the information
     * through. Future updates may include sending the cancelled request timing through with
     * information showing it was cancelled.
     *
     * @param url URL to cancel
     * @param requestMethod URL to cancel
     * @param endTime URL to cancel
     * @param exception URL to cancel
     */
    @Synchronized
    fun cancelNetworkCall(
        url: String?,
        requestMethod: String?,
        endTime: Long,
        exception: String?,
    ) {
        if (url != null) {
            val id = sanitiseURL(url)
            connections.remove(id)
        }
    }

    @Synchronized
    private fun sendNetworkTimingEvent(
        url: String?,
        requestMethod: String,
        startTime: Long,
        endTime: Long,
        statusCode: Int,
        exception: String?,
    ) {
        if (!shouldIgnoreURL(url) && loggingEnabled) {
            val sanitizedUrl = sanitiseURL(url)
            RUM
                .getInstance()
                .sendRUMTimingEvent(
                    RaygunRUMEventType.NETWORK_CALL,
                    "$requestMethod $sanitizedUrl",
                    endTime - startTime,
                )
        }
    }

    @Synchronized
    private fun removeOldEntries() {
        val it: MutableIterator<Map.Entry<String?, RaygunNetworkRequestInfo>> =
            connections.entries.iterator()
        while (it.hasNext()) {
            val pairs = it.next()
            val startTime = pairs.value.startTime
            if (System.currentTimeMillis() - startTime > CONNECTION_TIMEOUT) {
                it.remove()
            }
        }
    }

    private fun sanitiseURL(url: String?): String? {
        var sanitizedUrl = url
        if (sanitizedUrl != null) {
            val queryIndex = sanitizedUrl.indexOf("?")
            if (queryIndex > 0) {
                sanitizedUrl = sanitizedUrl.substring(0, queryIndex)
            }
        }
        return sanitizedUrl
    }

    private fun shouldIgnoreURL(url: String?): Boolean {
        if (url == null) {
            return true
        }
        for (ignoredUrl in RaygunSettings.getIgnoredURLs()) {
            if (url.contains(ignoredUrl) || ignoredUrl.contains(url)) {
                return true
            }
        }
        return false
    }
}
