package com.raygun.raygun4android.compatibility

import com.raygun.raygun4android.OkHttpClientBuilder
import com.raygun.raygun4android.RaygunClient
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import java.net.CookieManager
import java.net.InetAddress

fun isCrashReportingEnabled(): Boolean = RaygunClient.isCrashReportingEnabled

fun configureCustomHttpClient() {
    RaygunClient.setOkHttpClientBuilder(
        object : OkHttpClientBuilder {
            override fun build(): OkHttpClient =
                OkHttpClient
                    .Builder()
                    .dns { hostname -> InetAddress.getAllByName(hostname).toList() }
                    .cookieJar(JavaNetCookieJar(CookieManager()))
                    .build()
        },
    )
}
