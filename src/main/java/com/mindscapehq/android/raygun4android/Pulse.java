package main.java.com.mindscapehq.android.raygun4android;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

public class Pulse implements ActivityLifecycleCallbacks {
    private static Pulse pulse;

    protected static void Attach(Activity mainActivity) {
        if(mainActivity != null) {
            Application application = mainActivity.getApplication();
            if(application != null) {
                pulse = new Pulse();
                application.registerActivityLifecycleCallbacks(pulse);

                RaygunClient.SendPulseEvent("session_start");
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        System.out.println("CREATED");
    }

    @Override
    public void onActivityStarted(Activity activity) {
        System.out.println("STARTED");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        System.out.println("RESUMED");
    }

    @Override
    public void onActivityPaused(Activity activity) {
        System.out.println("PAUSED");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        System.out.println("STOPPED");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        System.out.println("SAVE");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        System.out.println("DESTROYED");
    }
}
