package com.raygun.raygun4android;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class RaygunPostService extends JobIntentService {

    static final int RAYGUNPOSTSERVICE_JOB_ID = 4711;
    private Intent intent;

    static void enqueueWork(Context context, Intent intent) {
        RaygunLogger.i("Work for RaygunPostService has been put in the job queue");
        enqueueWork(context, RaygunPostService.class, RAYGUNPOSTSERVICE_JOB_ID, intent);
    }

    @Override
    public void onHandleWork(@NonNull Intent intent) {

        if (intent != null && intent.getExtras() != null) {
            this.intent = intent;

            final Bundle bundle = intent.getExtras();

            String message = bundle.getString("msg");
            String apiKey = bundle.getString("apikey");
            boolean isPulse = bundle.getBoolean("isPulse");
            boolean hasInternet = hasInternetConnection();

            if (isPulse && hasInternet) {
                RaygunClient.postPulseMessage(apiKey, message);
            } else if (!isPulse && hasInternet) {
                RaygunClient.post(apiKey, message);
            } else if (!isPulse && !hasInternet) {
                synchronized (this) {
                    int file = 0;
                    ArrayList<File> files = new ArrayList<File>(Arrays.asList(getCacheDir().listFiles()));
                    if (files != null) {
                        for (File f : files) {
                            String fileName = Integer.toString(file) + ".raygun";
                            if (RaygunClient.getExtension(f.getName()).equals("raygun") && !f.getName().equals(fileName)) {
                                break;
                            } else if (file < RaygunSettings.getMaxReportsStoredOnDevice()) {
                                file++;
                            } else {
                                files.get(0).delete();
                            }
                        }
                    }
                    File fn = new File(getCacheDir(), Integer.toString(file) + ".raygun");
                    try {
                        MessageApiKey messageApiKey = new MessageApiKey(apiKey, message);
                        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fn));
                        out.writeObject(messageApiKey);
                        out.close();
                    } catch (FileNotFoundException e) {
                        RaygunLogger.e("Error creating file when caching message to filesystem - " + e.getMessage());
                    } catch (IOException e) {
                        RaygunLogger.e("Error writing message to filesystem - " + e.getMessage());
                    }
                }
            }
        }
    }

    private boolean hasInternetConnection() {

        ConnectivityManager cm = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
