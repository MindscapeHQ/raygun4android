package com.raygun.raygun4android.network.http

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

/**
 * Unit tests for [RaygunHttpUrlStreamHandler].
 *
 * Drives the protected `openConnection` overrides through the public [URL.openConnection] /
 * [URL.openConnection] (Proxy) entry points by constructing a [URL] with the handler attached.
 *
 * The `*ReturnsNull` tests are regression guards for the Kotlin warning "Elvis operator (?:) always
 * returns the left operand of non-nullable type 'URLConnection'". Before that fix, an underlying
 * handler that returns `null` would surface as a [NullPointerException] from the unchecked cast;
 * after the fix it must surface as the documented [IOException].
 */
class RaygunHttpUrlStreamHandlerTest {
    @Test
    fun openConnectionWrapsUnderlyingConnection() {
        val handler = RaygunHttpUrlStreamHandler(WrappingFakeHandler())
        val url = URL("http", "example.com", -1, "/", handler)

        val connection = url.openConnection()

        assertNotNull(connection)
        assertTrue(
            "Expected RaygunHttpUrlConnection wrapper but got ${connection::class.java.name}",
            connection is RaygunHttpUrlConnection,
        )
    }

    @Test(expected = IOException::class)
    fun openConnectionThrowsIoExceptionWhenUnderlyingReturnsNull() {
        val handler = RaygunHttpUrlStreamHandler(NullReturningFakeHandler())
        val url = URL("http", "example.com", -1, "/", handler)

        url.openConnection()
    }

    @Test
    fun openConnectionWithProxyWrapsUnderlyingConnection() {
        val handler = RaygunHttpUrlStreamHandler(WrappingFakeHandler())
        val url = URL("http", "example.com", -1, "/", handler)

        val connection = url.openConnection(Proxy.NO_PROXY)

        assertNotNull(connection)
        assertTrue(connection is RaygunHttpUrlConnection)
    }

    @Test(expected = IOException::class)
    fun openConnectionWithProxyThrowsIoExceptionWhenUnderlyingReturnsNull() {
        val handler = RaygunHttpUrlStreamHandler(NullReturningFakeHandler())
        val url = URL("http", "example.com", -1, "/", handler)

        url.openConnection(Proxy.NO_PROXY)
    }

    private class WrappingFakeHandler : URLStreamHandler() {
        override fun openConnection(u: URL): URLConnection = FakeHttpUrlConnection(u)

        override fun openConnection(
            u: URL,
            p: Proxy,
        ): URLConnection = FakeHttpUrlConnection(u)
    }

    private class NullReturningFakeHandler : URLStreamHandler() {
        // Override with nullable return so the fake legitimately returns null
        // (parent's URLConnection return type is a platform type and can be
        // narrowed to URLConnection? in Kotlin overrides).
        override fun openConnection(u: URL): URLConnection? = null

        override fun openConnection(
            u: URL,
            p: Proxy,
        ): URLConnection? = null
    }

    private class FakeHttpUrlConnection(
        url: URL,
    ) : HttpURLConnection(url) {
        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getInputStream(): InputStream = InputStream.nullInputStream()

        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
    }
}
