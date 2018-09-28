package com.raygun.raygun4android;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class RaygunPostService extends IntentService {

    private Intent intent;

    public RaygunPostService() {
        super("RaygunPostService");
    }

    @Override
    public void onHandleIntent(Intent intent) {

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
                            } else if (file < 64) {
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
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
