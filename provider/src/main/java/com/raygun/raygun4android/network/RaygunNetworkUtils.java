package com.raygun.raygun4android.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import com.raygun.raygun4android.messages.crashreporting.RaygunAppContext;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.UUID;

public class RaygunNetworkUtils {

    private static final String PREFS_FILE = "device_id.xml";
    private static final String PREFS_DEVICE_ID = "device_id";

    public static int getStatusCode(URLConnection urlConnection) {
        int statusCode = 0;
        if (urlConnection != null) {
            if ((urlConnection instanceof HttpURLConnection)) {
                try {
                    statusCode = ((HttpURLConnection) urlConnection).getResponseCode();
                } catch (Exception ignore) {
                }
            }
        }
        return statusCode;
    }

    public static boolean hasInternetConnection(Context appContext) {
        ConnectivityManager cm = (ConnectivityManager) appContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }

        return false;
    }

    public static String getDeviceUuid(Context context) {
        synchronized (RaygunNetworkUtils.class) {
            final SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
            String id = prefs.getString(PREFS_DEVICE_ID, null);

            if (id != null) {
                return UUID.fromString(id).toString();
            } else {
                final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

                try {
                    if (!"9774d56d682e549c".equals(androidId)) {
                        id = UUID.nameUUIDFromBytes(androidId.getBytes("utf8")).toString();
                    } else {
                        id = UUID.randomUUID().toString();
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

                prefs.edit().putString(PREFS_DEVICE_ID, id).apply();
                return id;
            }
        }
    }

}