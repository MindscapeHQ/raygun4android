package com.raygun.raygun4android.rum;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.raygun.raygun4android.RaygunRUMEventType;
import com.raygun.raygun4android.logging.RaygunLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RUM for Fragments Sends the FRAGMENT_LOADED event when a Fragment is resumed. Also tracks the
 * time spent in the Fragment, based on the Fragment ID.
 */
public class RUMFragment extends FragmentManager.FragmentLifecycleCallbacks {
    private final RUM rum;

    // Map of Fragment ID to start time in nanos
    private final Map<Integer, Long> fragmentStartTime = new HashMap<>();

    public RUMFragment(RUM rum) {
        this.rum = rum;
    }

    public void attach(FragmentManager fragmentManager) {
        RaygunLogger.v("RUM - Attaching RUM Fragment");
        fragmentManager.registerFragmentLifecycleCallbacks(this, true);
    }

    public void detach(FragmentManager fragmentManager) {
        RaygunLogger.v("RUM - Detaching RUM Fragment");
        fragmentManager.unregisterFragmentLifecycleCallbacks(this);
    }

    @Override
    public void onFragmentCreated(
            @NonNull FragmentManager fm,
            @NonNull Fragment fragment,
            @Nullable Bundle savedInstanceState) {
        super.onFragmentCreated(fm, fragment, savedInstanceState);
        RaygunLogger.v(
                "RUM - Fragment created: "
                        + getFragmentName(fragment)
                        + " id: "
                        + getUniqueId(fragment));
        if (!fragmentStartTime.containsKey(getUniqueId(fragment))) {
            fragmentStartTime.put(getUniqueId(fragment), System.nanoTime());
        }
        rum.seen();
    }

    @Override
    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentStarted(fm, fragment);
        RaygunLogger.v(
                "RUM - Fragment started: "
                        + getFragmentName(fragment)
                        + " id: "
                        + getUniqueId(fragment));
        if (!fragmentStartTime.containsKey(getUniqueId(fragment))) {
            fragmentStartTime.put(getUniqueId(fragment), System.nanoTime());
        }
        rum.seen();
    }

    @Override
    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentResumed(fm, fragment);
        RaygunLogger.v(
                "RUM - Fragment resumed: "
                        + getFragmentName(fragment)
                        + " id: "
                        + getUniqueId(fragment));
        long duration = 0;
        if (!fragmentStartTime.containsKey(getUniqueId(fragment))) {
            fragmentStartTime.put(getUniqueId(fragment), System.nanoTime());
        } else {
            Long startTime = fragmentStartTime.get(getUniqueId(fragment));
            if (startTime != null) {
                long diff = System.nanoTime() - startTime;
                duration = TimeUnit.NANOSECONDS.toMillis(diff);
            }
        }
        rum.sendRUMTimingEvent(
                RaygunRUMEventType.FRAGMENT_LOADED, getFragmentName(fragment), duration);
        rum.seen();
    }

    @Override
    public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentPaused(fm, fragment);
        RaygunLogger.v(
                "RUM - Fragment paused: "
                        + getFragmentName(fragment)
                        + " id: "
                        + getUniqueId(fragment));
        rum.seen();
    }

    @Override
    public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentStopped(fm, fragment);
        RaygunLogger.v(
                "RUM - Fragment stopped: "
                        + getFragmentName(fragment)
                        + " id: "
                        + getUniqueId(fragment));
        // Remove the start time for the fragment
        fragmentStartTime.remove(getUniqueId(fragment));
        rum.seen();
    }

    @Override
    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentDestroyed(fm, fragment);
        RaygunLogger.v(
                "RUM - Fragment destroyed: "
                        + getFragmentName(fragment)
                        + " id: "
                        + getUniqueId(fragment));
        // Remove the start time for the fragment
        fragmentStartTime.remove(getUniqueId(fragment));
        rum.seen();
    }

    public String getFragmentName(Fragment fragment) {
        FragmentActivity activity = fragment.getActivity();
        String simpleName = fragment.getClass().getSimpleName();
        if (activity != null) {
            return activity.getClass().getSimpleName() + " - " + simpleName;
        } else {
            return simpleName;
        }
    }

    public int getUniqueId(Fragment fragment) {
        return fragment.hashCode();
    }
}
