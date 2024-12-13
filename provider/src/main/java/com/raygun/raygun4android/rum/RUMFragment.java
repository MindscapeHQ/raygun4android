package com.raygun.raygun4android.rum;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.raygun.raygun4android.RaygunRUMEventType;
import com.raygun.raygun4android.logging.RaygunLogger;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RUM for Fragments
 * Sends the FRAGMENT_LOADED event when a Fragment is resumed.
 * Also tracks the time spent in the Fragment, based on the Fragment ID.
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
    public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment fragment, @Nullable Bundle savedInstanceState) {
        super.onFragmentCreated(fm, fragment, savedInstanceState);
        RaygunLogger.v("RUM - Fragment created: " + fragment.getClass().getSimpleName());
        if (!fragmentStartTime.containsKey(fragment.getId())) {
            fragmentStartTime.put(fragment.getId(), System.nanoTime());
        }
        rum.seen();
    }

    @Override
    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentStarted(fm, fragment);
        RaygunLogger.v("RUM - Fragment started: " + fragment.getClass().getSimpleName());
        if (!fragmentStartTime.containsKey(fragment.getId())) {
            fragmentStartTime.put(fragment.getId(), System.nanoTime());
        }
        rum.seen();
    }

    @Override
    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentResumed(fm, fragment);
        RaygunLogger.v("RUM - Fragment resumed: " + fragment.getClass().getSimpleName());
        long duration = 0;
        if (!fragmentStartTime.containsKey(fragment.getId())) {
            fragmentStartTime.put(fragment.getId(), System.nanoTime());
        } else {
            Long startTime = fragmentStartTime.get(fragment.getId());
            if (startTime != null) {
                long diff = System.nanoTime() - startTime;
                duration = TimeUnit.NANOSECONDS.toMillis(diff);
            }
        }
        String fragmentName = fragment.getClass().getSimpleName();
        // TODO: Maybe provide Parent Activity + Fragment Name
//        fragment.getActivity().getClass().getSimpleName();
//        rum.sendRUMTimingEvent(RaygunRUMEventType.FRAGMENT_LOADED, fragmentName, duration);
        rum.seen();
    }

    @Override
    public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentPaused(fm, f);
        RaygunLogger.v(" RUM - Fragment paused: " + f.getClass().getSimpleName());
        rum.seen();
    }

    @Override
    public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentStopped(fm, fragment);
        RaygunLogger.v("RUM - Fragment stopped: " + fragment.getClass().getSimpleName());
        // Remove the start time for the fragment
        fragmentStartTime.remove(fragment.getId());
        rum.seen();
    }

    @Override
    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
        super.onFragmentDestroyed(fm, fragment);
        RaygunLogger.v("RUM - Fragment destroyed: " + fragment.getClass().getSimpleName());
        // Remove the start time for the fragment
        fragmentStartTime.remove(fragment.getId());
        rum.seen();
    }
}
