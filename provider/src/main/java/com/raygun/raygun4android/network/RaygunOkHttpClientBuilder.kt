package com.raygun.raygun4android.network

import com.raygun.raygun4android.OkHttpClientBuilder
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class RaygunOkHttpClientBuilder : OkHttpClientBuilder {
    override fun build(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(NETWORK_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .writeTimeout(NETWORK_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT.toLong(), TimeUnit.SECONDS)
            .build()

    companion object {
        const val NETWORK_TIMEOUT: Int = 30

        @JvmField var instance: RaygunOkHttpClientBuilder = RaygunOkHttpClientBuilder()
    }
}
