package com.raygun.raygun4android.rum

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.raygun.raygun4android.RaygunRUMEventType
import com.raygun.raygun4android.logging.RaygunLogger.v
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class RUMActivity(
    // Reference to the RUM instance
    private val rum: RUM,
    // Each time a new Activity is created, attach to the FragmentManager
    // When the Activity is destroyed, detach from the FragmentManager
    private val rumFragment: RUMFragment
) : ActivityLifecycleCallbacks {
    private var mainActivity: WeakReference<Activity?>? = null
    private var currentActivity: WeakReference<Activity?>? = null
    private var loadingActivity: WeakReference<Activity?>? = null

    // Tracks time spent in the current activity
    private var startTime: Long = 0

    val isAttached: Boolean
        get() = mainActivity != null && mainActivity!!.get() != null

    fun attach(mainActivity: Activity) {
        this.mainActivity = WeakReference(mainActivity)
        this.currentActivity = WeakReference(mainActivity)

        // Register the ActivityLifecycleCallbacks of the Application
        val application = mainActivity.application
        application.registerActivityLifecycleCallbacks(this)

        // Attach to the FragmentManager from Main Activity
        if (mainActivity is FragmentActivity) {
            rumFragment.attach(mainActivity.supportFragmentManager)
        }

        startTime = System.nanoTime()
    }

    fun detach() {
        if (mainActivity != null && mainActivity!!.get() != null) {
            mainActivity!!.get()!!.application.unregisterActivityLifecycleCallbacks(this)
            if (mainActivity!!.get() is FragmentActivity) {
                rumFragment.detach(
                    (mainActivity!!.get() as FragmentActivity).supportFragmentManager
                )
            }
        }
        mainActivity = null
        currentActivity = null
        loadingActivity = null
    }

    fun sendRemaining() {
        if (loadingActivity != null && loadingActivity!!.get() != null) {
            val activityName = getActivityName(loadingActivity!!.get()!!)

            val diff = System.nanoTime() - startTime
            val duration = TimeUnit.NANOSECONDS.toMillis(diff)
            rum.sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration)
        }
    }

    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        v("RUM - Activity created: " + getActivityName(activity))
        if (currentActivity == null || currentActivity!!.get() == null) {
            rum.maybeRotateSession()
        }
        if (currentActivity == null || currentActivity!!.get() !== activity) {
            currentActivity = WeakReference(activity)
            loadingActivity = WeakReference(activity)
            startTime = System.nanoTime()

            // Attach to the FragmentManager from Current Activity
            if (activity is FragmentActivity) {
                rumFragment.attach(activity.supportFragmentManager)
            }
        }
        rum.seen()
    }

    override fun onActivityStarted(activity: Activity) {
        v("RUM - Activity started: " + getActivityName(activity))
        if (currentActivity == null || currentActivity!!.get() == null) {
            rum.maybeRotateSession()
        }
        if (currentActivity == null || currentActivity!!.get() !== activity) {
            currentActivity = WeakReference(activity)
            loadingActivity = WeakReference(activity)
            startTime = System.nanoTime()
        }
        rum.seen()
    }

    override fun onActivityResumed(activity: Activity) {
        v("RUM - Activity resumed: " + getActivityName(activity))
        if (currentActivity == null || currentActivity!!.get() == null) {
            rum.maybeRotateSession()
        }
        val activityName = getActivityName(activity)
        var duration: Long = 0
        if (currentActivity != null && activity === currentActivity!!.get()) {
            val diff = System.nanoTime() - startTime
            duration = TimeUnit.NANOSECONDS.toMillis(diff)
        }
        currentActivity = WeakReference(activity)
        loadingActivity = null
        rum.sendRUMTimingEvent(RaygunRUMEventType.ACTIVITY_LOADED, activityName, duration)
        rum.seen()
    }

    override fun onActivityPaused(activity: Activity) {
        v("RUM - Activity paused: " + getActivityName(activity))
        rum.seen()
    }

    override fun onActivityStopped(activity: Activity) {
        v("RUM - Activity stopped: " + getActivityName(activity))
        if (currentActivity != null && currentActivity!!.get() === activity) {
            currentActivity = null
            loadingActivity = null
        }
        rum.seen()
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
        v("RUM - onActivitySaveInstanceState: " + getActivityName(activity))
        rum.seen()
    }

    override fun onActivityDestroyed(activity: Activity) {
        v("RUM - Activity destroyed: " + getActivityName(activity))
        // Detach the FragmentManager from destroyed Activity
        if (activity is FragmentActivity) {
            rumFragment.detach(activity.supportFragmentManager)
        }
        rum.seen()
    }

    private fun getActivityName(activity: Activity): String {
        return activity.javaClass.simpleName
    }
}
