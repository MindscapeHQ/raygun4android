package com.raygun.raygun4android;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.raygun.raygun4android.logging.RaygunLogger;

public class RUMFragment extends FragmentManager.FragmentLifecycleCallbacks {
    private static RUMFragment rum;

    public static void attach(FragmentManager fragmentManager) {
        if (rum == null) {
            rum = new RUMFragment();
        }

        RaygunLogger.v("RUM - Attaching RUM Fragment");
        fragmentManager.registerFragmentLifecycleCallbacks(rum, true);
    }

    // TODO: Do we need this? Is there a reason on why a fragment manager may need to be detached?
    public static void detach(FragmentManager fragmentManager) {
        RaygunLogger.v("RUM - Detaching RUM Fragment");
        fragmentManager.unregisterFragmentLifecycleCallbacks(rum);
    }

    @Override
    public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
        RaygunLogger.v("RUM - Fragment created: " + f.getClass().getSimpleName());
        super.onFragmentCreated(fm, f, savedInstanceState);
    }

    @Override
    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
        RaygunLogger.v("RUM - Fragment started: " + f.getClass().getSimpleName());
        super.onFragmentStarted(fm, f);
    }

    @Override
    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        RaygunLogger.v("RUM - Fragment resumed: " + f.getClass().getSimpleName());
        super.onFragmentResumed(fm, f);
    }

    @Override
    public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
        RaygunLogger.v(" RUM - Fragment paused: " + f.getClass().getSimpleName());
        super.onFragmentPaused(fm, f);
    }

    @Override
    public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
        RaygunLogger.v("RUM - Fragment stopped: " + f.getClass().getSimpleName());
        super.onFragmentStopped(fm, f);
    }

    @Override
    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        RaygunLogger.v("RUM - Fragment destroyed: " + f.getClass().getSimpleName());
        super.onFragmentDestroyed(fm, f);
    }
}
