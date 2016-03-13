package main.java.com.mindscapehq.android.raygun4android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnPackageReplaced extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        RaygunClient.closePostService();
    }
}
