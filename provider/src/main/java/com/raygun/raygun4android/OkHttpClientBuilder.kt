package com.raygun.raygun4android

import okhttp3.OkHttpClient

/**
 * Implement this class and call `RaygunClient.setOkHttpClientBuilder()` to provide a custom
 * OkHttpClient, e.g. to set custom SSLContext.
 */
interface OkHttpClientBuilder {
    /**
     * @return new instance of OkHttpClient
     */
    fun build(): OkHttpClient
}
