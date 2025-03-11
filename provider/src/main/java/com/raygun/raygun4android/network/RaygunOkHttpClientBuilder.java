package com.raygun.raygun4android.network;

import com.raygun.raygun4android.OkHttpClientBuilder;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class RaygunOkHttpClientBuilder implements OkHttpClientBuilder {
    public static final int NETWORK_TIMEOUT = 30;

    public static RaygunOkHttpClientBuilder instance = new RaygunOkHttpClientBuilder();

    @Override
    public OkHttpClient build() {
        return new OkHttpClient.Builder()
                .connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }
}
