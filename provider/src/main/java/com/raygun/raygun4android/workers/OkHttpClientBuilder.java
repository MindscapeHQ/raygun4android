package com.raygun.raygun4android.workers;

import com.raygun.raygun4android.RaygunClient;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;

class OkHttpClientBuilder {
    private static final int NETWORK_TIMEOUT = 30;

    static OkHttpClient build() {
        SSLSocketFactory sslSocketFactory = RaygunClient.getSslSocketFactory();
        X509TrustManager x509TrustManager = RaygunClient.getX509TrustManager();

        OkHttpClient.Builder builder =
                new OkHttpClient.Builder()
                        .connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS);

        if (sslSocketFactory != null && x509TrustManager != null) {
            builder.sslSocketFactory(sslSocketFactory, x509TrustManager);
        }

        return builder.build();
    }
}
