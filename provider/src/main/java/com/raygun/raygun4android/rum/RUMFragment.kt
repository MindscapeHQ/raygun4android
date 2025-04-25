package com.raygun.raygun4android.rum

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.raygun.raygun4android.RaygunRUMEventType
import com.raygun.raygun4android.logging.RaygunLogger.v
import java.util.concurrent.TimeUnit

/**
 * RUM for Fragments Sends the FRAGMENT_LOADED event when a Fragment is resumed. Also tracks the
 * time spent in the Fragment, based on the Fragment ID.
 */
class RUMFragment(
    private val rum: RUM,
) : FragmentManager.FragmentLifecycleCallbacks() {
    // Map of Fragment ID to start time in nanos
    private val fragmentStartTime: MutableMap<Int, Long> = HashMap()

    fun attach(fragmentManager: FragmentManager) {
        v("RUM - Attaching RUM Fragment")
        fragmentManager.registerFragmentLifecycleCallbacks(this, true)
    }

    fun detach(fragmentManager: FragmentManager) {
        v("RUM - Detaching RUM Fragment")
        fragmentManager.unregisterFragmentLifecycleCallbacks(this)
    }

    override fun onFragmentCreated(
        fm: FragmentManager,
        fragment: Fragment,
        savedInstanceState: Bundle?,
    ) {
        super.onFragmentCreated(fm, fragment, savedInstanceState)
        v(
            (
                "RUM - Fragment created: " +
                    getFragmentName(fragment) +
                    " id: " +
                    getUniqueId(fragment)
            ),
        )
        if (!fragmentStartTime.containsKey(getUniqueId(fragment))) {
            fragmentStartTime[getUniqueId(fragment)] = System.nanoTime()
        }
        rum.seen()
    }

    override fun onFragmentStarted(
        fm: FragmentManager,
        fragment: Fragment,
    ) {
        super.onFragmentStarted(fm, fragment)
        v(
            (
                "RUM - Fragment started: " +
                    getFragmentName(fragment) +
                    " id: " +
                    getUniqueId(fragment)
            ),
        )
        if (!fragmentStartTime.containsKey(getUniqueId(fragment))) {
            fragmentStartTime[getUniqueId(fragment)] = System.nanoTime()
        }
        rum.seen()
    }

    override fun onFragmentResumed(
        fm: FragmentManager,
        fragment: Fragment,
    ) {
        super.onFragmentResumed(fm, fragment)
        v(
            (
                "RUM - Fragment resumed: " +
                    getFragmentName(fragment) +
                    " id: " +
                    getUniqueId(fragment)
            ),
        )
        var duration: Long = 0
        if (!fragmentStartTime.containsKey(getUniqueId(fragment))) {
            fragmentStartTime[getUniqueId(fragment)] = System.nanoTime()
        } else {
            val startTime = fragmentStartTime[getUniqueId(fragment)]
            if (startTime != null) {
                val diff = System.nanoTime() - startTime
                duration = TimeUnit.NANOSECONDS.toMillis(diff)
            }
        }
        rum.sendRUMTimingEvent(
            RaygunRUMEventType.FRAGMENT_LOADED,
            getFragmentName(fragment),
            duration,
        )
        rum.seen()
    }

    override fun onFragmentPaused(
        fm: FragmentManager,
        fragment: Fragment,
    ) {
        super.onFragmentPaused(fm, fragment)
        v(("RUM - Fragment paused: " + getFragmentName(fragment) + " id: " + getUniqueId(fragment)))
        rum.seen()
    }

    override fun onFragmentStopped(
        fm: FragmentManager,
        fragment: Fragment,
    ) {
        super.onFragmentStopped(fm, fragment)
        v(
            (
                "RUM - Fragment stopped: " +
                    getFragmentName(fragment) +
                    " id: " +
                    getUniqueId(fragment)
            ),
        )
        // Remove the start time for the fragment
        fragmentStartTime.remove(getUniqueId(fragment))
        rum.seen()
    }

    override fun onFragmentDestroyed(
        fm: FragmentManager,
        fragment: Fragment,
    ) {
        super.onFragmentDestroyed(fm, fragment)
        v(
            (
                "RUM - Fragment destroyed: " +
                    getFragmentName(fragment) +
                    " id: " +
                    getUniqueId(fragment)
            ),
        )
        // Remove the start time for the fragment
        fragmentStartTime.remove(getUniqueId(fragment))
        rum.seen()
    }

    fun getFragmentName(fragment: Fragment): String {
        val activity = fragment.activity
        val simpleName = fragment.javaClass.simpleName
        return if (activity != null) {
            activity.javaClass.simpleName + " - " + simpleName
        } else {
            simpleName
        }
    }

    fun getUniqueId(fragment: Fragment): Int = fragment.hashCode()
}
