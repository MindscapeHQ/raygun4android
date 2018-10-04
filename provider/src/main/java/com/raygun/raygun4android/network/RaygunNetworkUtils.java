package com.raygun.raygun4android.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.HttpURLConnection;
import java.net.URLConnection;

public class RaygunNetworkUtils {

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

}