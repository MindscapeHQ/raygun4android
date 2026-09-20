package com.raygun.raygun4android.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.JavaNetCookieJar
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.net.CookieManager
import java.net.HttpCookie
import java.net.URI

@RunWith(AndroidJUnit4::class)
class OkHttpDependencyAlignmentTest {
    @Test
    fun javaNetCookieJarCanLoadStoredCookie() {
        val cookieManager = CookieManager()
        cookieManager.cookieStore.add(URI(TEST_URL), HttpCookie(COOKIE_NAME, COOKIE_VALUE))

        val cookies = JavaNetCookieJar(cookieManager).loadForRequest(TEST_URL.toHttpUrl())

        assertEquals(COOKIE_VALUE, cookies.single { it.name == COOKIE_NAME }.value)
    }

    private companion object {
        const val TEST_URL = "https://example.com/"
        const val COOKIE_NAME = "session"
        const val COOKIE_VALUE = "persisted"
    }
}
