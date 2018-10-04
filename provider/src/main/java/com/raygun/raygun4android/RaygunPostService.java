package com.raygun.raygun4android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.raygun.raygun4android.network.RaygunNetworkUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RaygunPostService extends JobIntentService {

    static final int RAYGUNPOSTSERVICE_JOB_ID = 4711;
    static final int NETWORK_TIMEOUT = 30;

    static void enqueueWork(Context context, Intent intent) {
        RaygunLogger.i("Work for RaygunPostService has been put in the job queue");
        enqueueWork(context, RaygunPostService.class, RAYGUNPOSTSERVICE_JOB_ID, intent);
    }

    @Override
    public void onHandleWork(@NonNull Intent intent) {

        if (intent != null && intent.getExtras() != null) {

            final Bundle bundle = intent.getExtras();

            String message = bundle.getString("msg");
            String apiKey = bundle.getString("apikey");
            boolean isPulse = bundle.getBoolean("isPulse");

            // Moved the check for internet connection as close as possible to the calls because this can change quite rapidly
            if (isPulse && RaygunNetworkUtils.hasInternetConnection(this.getApplicationContext())) {
                RaygunClient.postPulseMessage(apiKey, message);
            } else if (!isPulse && RaygunNetworkUtils.hasInternetConnection(this.getApplicationContext())) {
                post(apiKey, message);
            } else if (!isPulse && !RaygunNetworkUtils.hasInternetConnection(this.getApplicationContext())) {
                synchronized (this) {
                    int file = 0;
                    ArrayList<File> files = new ArrayList<>(Arrays.asList(getCacheDir().listFiles()));
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

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Raw post method that delivers a pre-built RaygunMessage to the Raygun API.
     *
     * @param apiKey      The API key of the app to deliver to
     * @param jsonPayload The JSON representation of a RaygunMessage to be delivered over HTTPS.
     * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message (invalid properties)
     */
    private static int post(String apiKey, String jsonPayload) {
        try {
            if (RaygunClient.validateApiKey(apiKey)) {
                String endpoint = RaygunSettings.getApiEndpoint();
                MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                        .build();

                RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, jsonPayload);

                Request request = new Request.Builder()
                        .url(endpoint)
                        .header("X-ApiKey", apiKey)
                        .post(body)
                        .build();
                try {
                    Response response = client.newCall(request).execute();
                    RaygunLogger.d("Exception message HTTP POST result: " + response.code());
                    return response.code();
                } catch (IOException ioe) {
                    RaygunLogger.e("OkHttp POST to Raygun Crash Reporting backend failed - " + ioe.getMessage());
                    ioe.printStackTrace();
                }
            }
        } catch (Exception e) {
            RaygunLogger.e("Couldn't post exception - " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
}
