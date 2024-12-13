package com.raygun.raygun4android.rum;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.raygun.raygun4android.logging.RaygunLogger;

public class RUMFragment extends FragmentManager.FragmentLifecycleCallbacks {
    private final RUM rum;

    public RUMFragment(RUM rum) {
        this.rum = rum;
    }

    public void attach(FragmentManager fragmentManager) {
        RaygunLogger.v("RUM - Attaching RUM Fragment");
        fragmentManager.registerFragmentLifecycleCallbacks(this, true);
    }

    // TODO: Do we need this? Is there a reason on why a fragment manager may need to be detached?
    public void detach(FragmentManager fragmentManager) {
        RaygunLogger.v("RUM - Detaching RUM Fragment");
        fragmentManager.unregisterFragmentLifecycleCallbacks(this);
    }

    public void sendRemaining() {

    }

    @Override
    public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
        super.onFragmentCreated(fm, f, savedInstanceState);
        RaygunLogger.v("RUM - Fragment created: " + f.getClass().getSimpleName());
        rum.seen();
    }

    @Override
    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentStarted(fm, f);
        RaygunLogger.v("RUM - Fragment started: " + f.getClass().getSimpleName());
        rum.seen();
    }

    @Override
    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentResumed(fm, f);
        RaygunLogger.v("RUM - Fragment resumed: " + f.getClass().getSimpleName());
        rum.seen();
    }

    @Override
    public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentPaused(fm, f);
        RaygunLogger.v(" RUM - Fragment paused: " + f.getClass().getSimpleName());
        rum.seen();
    }

    @Override
    public void onFragmentStopped(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentStopped(fm, f);
        RaygunLogger.v("RUM - Fragment stopped: " + f.getClass().getSimpleName());
        rum.seen();
    }

    @Override
    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
        super.onFragmentDestroyed(fm, f);
        RaygunLogger.v("RUM - Fragment destroyed: " + f.getClass().getSimpleName());
        rum.seen();
    }
}
