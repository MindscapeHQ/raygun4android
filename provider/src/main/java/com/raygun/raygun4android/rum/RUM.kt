package com.raygun.raygun4android.rum

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import com.google.gson.Gson
import com.raygun.raygun4android.RaygunClient
import com.raygun.raygun4android.RaygunRUMEventType
import com.raygun.raygun4android.RaygunSettings
import com.raygun.raygun4android.logging.RaygunLogger.v
import com.raygun.raygun4android.logging.RaygunLogger.w
import com.raygun.raygun4android.messages.rum.RaygunRUMData
import com.raygun.raygun4android.messages.rum.RaygunRUMDataMessage
import com.raygun.raygun4android.messages.rum.RaygunRUMMessage
import com.raygun.raygun4android.messages.rum.RaygunRUMTimingMessage
import com.raygun.raygun4android.messages.shared.RaygunUserInfo
import com.raygun.raygun4android.messages.shared.RaygunUserInfo.Companion.anonymousSync
import com.raygun.raygun4android.network.RaygunNetworkLogger.init
import com.raygun.raygun4android.network.RaygunNetworkLogger.setEnabled
import com.raygun.raygun4android.workers.RUMWorkerHelper
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

class RUM private constructor() {
    private var lastSeenTime: Long = 0
    private var sessionId: String? = null
    private var currentSessionUser: RaygunUserInfo? = null
    private val rumActivity =
        RUMActivity(this, RUMFragment(this))

    /**
     * Attaches the RUM instance to the main activity and starts tracking RUM events.
     *
     * @param mainActivity The main activity of the application.
     * @param networkLogging Whether to log network requests.
     */
    fun attach(mainActivity: Activity, networkLogging: Boolean) {
        v("attached RUM")
        setEnabled(networkLogging)
        if (!rumActivity.isAttached) {
            rumActivity.attach(mainActivity)
            maybeRotateSession()
            init()
        }
        seen()
    }

    /** Send all remaining RUM events before the app is closed.  */
    fun sendRemaining() {
        rumActivity.sendRemaining()
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser)
        seen()
    }

    /** Call this method everytime there is a lifecycle event in activities or fragments.  */
    fun seen() {
        lastSeenTime = System.currentTimeMillis()
    }

    /** Rotates the User session if expired  */
    fun maybeRotateSession() {
        if (doesNeedSessionRotation()) {
            rotateSession(currentSessionUser, currentSessionUser)
        }
    }

    private fun doesNeedSessionRotation(): Boolean {
        return lastSeenTime > 0
                && System.currentTimeMillis() - lastSeenTime > RaygunSettings.RUM_SESSION_EXPIRY
    }

    fun updateCurrentSessionUser(userInfo: RaygunUserInfo) {
        if (currentSessionUser != null) {
            val currentSessionUserIsAnon = currentSessionUser!!.isAnonymous
            val usersAreTheSame =
                currentSessionUser!!.identifier == userInfo.identifier
            val changedUser = !usersAreTheSame && !currentSessionUserIsAnon

            if (changedUser) {
                rotateSession(currentSessionUser, userInfo)
            }
        }
        currentSessionUser = userInfo
    }

    /**
     * Detaches the RUM instance from the main activity and stops tracking RUM events. Also detaches
     * from the FragmentManager in the main activity. And clears the singleton instance.
     */
    fun detach() {
        rumActivity.detach()
        _instance = null
    }

    private fun rotateSession(
        currentSessionUser: RaygunUserInfo?,
        newSessionUser: RaygunUserInfo?
    ) {
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_END, currentSessionUser)
        sessionId = UUID.randomUUID().toString()
        sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START, newSessionUser)
    }

    /**
     * Sends a RUM event to Raygun. The message is sent on a background thread.
     *
     * @param eventName Tracks if this is a session start or session end event.
     */
    private fun sendRUMEvent(eventName: String, userInfo: RaygunUserInfo?) {
        if (RaygunClient.isRUMEnabled()) {
            val timestamp: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val utcDateTime = LocalDateTime.now(ZoneId.of("UTC"))
                if (RaygunSettings.RUM_EVENT_SESSION_END == eventName) {
                    utcDateTime.plusSeconds(2)
                }
                timestamp = utcDateTime.toString()
            } else {
                @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                df.timeZone = TimeZone.getTimeZone("UTC")
                val c = Calendar.getInstance()
                if (RaygunSettings.RUM_EVENT_SESSION_END == eventName) {
                    c.add(Calendar.SECOND, 2)
                }
                timestamp = df.format(c.time)
            }

            val user = userInfo ?: anonymousSync()

            val dataMessage =
                RaygunRUMDataMessage.Builder(eventName)
                    .timestamp(timestamp)
                    .sessionId(sessionId)
                    .version(RaygunClient.getVersion())
                    .os("Android")
                    .osVersion(Build.VERSION.RELEASE)
                    .platform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL))
                    .user(user)
                    .build()

            val message = RaygunRUMMessage()
            message.eventData = arrayOf(dataMessage)

            enqueueWorkForRUMService(RaygunClient.getApiKey(), Gson().toJson(message))
        } else {
            w("RUM is not enabled, please enable to use the sendRUMEvent() function")
        }
    }

    private fun sendRUMEvent(eventName: String) {
        val user =
            if (RaygunClient.getUser() == null)
                anonymousSync()
            else
                RaygunClient.getUser()
        sendRUMEvent(eventName, user)
    }

    /**
     * Sends a RUM timing event to Raygun. The message is sent on a background thread.
     *
     * @param eventType The type of event that occurred.
     * @param name The name of the event resource such as the activity name or URL of a network
     * call.
     * @param milliseconds The duration of the event in milliseconds.
     */
    fun sendRUMTimingEvent(eventType: RaygunRUMEventType, name: String, milliseconds: Long) {
        if (RaygunClient.isRUMEnabled()) {
            if (sessionId == null) {
                sessionId = UUID.randomUUID().toString()
                sendRUMEvent(RaygunSettings.RUM_EVENT_SESSION_START)
            }

            if (eventType == RaygunRUMEventType.ACTIVITY_LOADED) {
                if (shouldIgnoreView(name)) {
                    return
                }
            }

            val timestamp: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val utcDateTime = LocalDateTime.now(ZoneId.of("UTC"))
                utcDateTime.minus(milliseconds, ChronoUnit.MILLIS)
                timestamp = utcDateTime.toString()
            } else {
                @SuppressLint("SimpleDateFormat") val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                df.timeZone = TimeZone.getTimeZone("UTC")
                val c = Calendar.getInstance()
                c.add(Calendar.MILLISECOND, -milliseconds.toInt())
                timestamp = df.format(c.time)
            }

            val user =
                if (RaygunClient.getUser() == null)
                    anonymousSync()
                else
                    RaygunClient.getUser()

            val timingMessage =
                RaygunRUMTimingMessage.Builder(
                    if (eventType == RaygunRUMEventType.ACTIVITY_LOADED
                        || (eventType
                                == RaygunRUMEventType.FRAGMENT_LOADED)
                    )
                        "p"
                    else
                        "n"
                )
                    .duration(milliseconds)
                    .build()

            val data = RaygunRUMData.Builder(name).timing(timingMessage).build()

            val dataArray = arrayOf(data)
            val dataStr = Gson().toJson(dataArray)

            val dataMessage =
                RaygunRUMDataMessage.Builder(RaygunSettings.RUM_EVENT_TIMING)
                    .timestamp(timestamp)
                    .sessionId(sessionId)
                    .version(RaygunClient.getVersion())
                    .os("Android")
                    .osVersion(Build.VERSION.RELEASE)
                    .platform(String.format("%s %s", Build.MANUFACTURER, Build.MODEL))
                    .user(user)
                    .data(dataStr)
                    .build()

            val message = RaygunRUMMessage()
            message.eventData = arrayOf(dataMessage)

            enqueueWorkForRUMService(RaygunClient.getApiKey(), Gson().toJson(message))
        } else {
            w(
                "RUM is not enabled, please enable to use the sendRUMTimingEvent() function"
            )
        }
    }

    private fun shouldIgnoreView(viewName: String?): Boolean {
        if (viewName == null) {
            return true
        }
        for (ignoredView in RaygunSettings.getIgnoredViews()) {
            if (viewName.contains(ignoredView) || ignoredView.contains(viewName)) {
                return true
            }
        }
        return false
    }

    companion object {
        // Singleton instance
        private var _instance: RUM? = null

        @JvmStatic
        val instance: RUM
            get() {
                if (_instance == null) {
                    _instance = RUM()
                }
                return _instance!!
            }

        private fun enqueueWorkForRUMService(apiKey: String, jsonPayload: String) {
            RUMWorkerHelper.enqueueRUM(RaygunClient.getApplicationContext(), jsonPayload, apiKey)
        }
    }
}
