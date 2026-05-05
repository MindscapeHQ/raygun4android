package com.raygun.raygun4android.network.http

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
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
 * Mirror of [RaygunHttpUrlStreamHandlerTest] for the HTTPS variant.
 *
 * The `*ReturnsNull` tests assert on the IOException message ("Failed to create connection") so
 * they pin to the safe-cast path and don't accidentally pass against the catch-chain fallthrough
 * that throws an empty-message [IOException].
 */
class RaygunHttpsUrlStreamHandlerTest {
    @Test
    fun openConnectionWrapsUnderlyingConnection() {
        val handler = RaygunHttpsUrlStreamHandler(WrappingFakeHandler())
        val url = URL("https", "example.com", -1, "/", handler)

        val connection = url.openConnection()

        assertNotNull(connection)
        assertTrue(
            "Expected RaygunHttpsUrlConnection wrapper but got ${connection::class.java.name}",
            connection is RaygunHttpsUrlConnection,
        )
    }

    @Test
    fun openConnectionThrowsIoExceptionWhenUnderlyingReturnsNull() {
        val handler = RaygunHttpsUrlStreamHandler(NullReturningFakeHandler())
        val url = URL("https", "example.com", -1, "/", handler)

        val thrown = assertThrows(IOException::class.java) { url.openConnection() }
        assertEquals("Failed to create connection", thrown.message)
    }

    @Test
    fun openConnectionWithProxyWrapsUnderlyingConnection() {
        val handler = RaygunHttpsUrlStreamHandler(WrappingFakeHandler())
        val url = URL("https", "example.com", -1, "/", handler)

        val connection = url.openConnection(Proxy.NO_PROXY)

        assertNotNull(connection)
        assertTrue(connection is RaygunHttpsUrlConnection)
    }

    @Test
    fun openConnectionWithProxyThrowsIoExceptionWhenUnderlyingReturnsNull() {
        val handler = RaygunHttpsUrlStreamHandler(NullReturningFakeHandler())
        val url = URL("https", "example.com", -1, "/", handler)

        val thrown = assertThrows(IOException::class.java) { url.openConnection(Proxy.NO_PROXY) }
        assertEquals("Failed to create connection", thrown.message)
    }

    private class WrappingFakeHandler : URLStreamHandler() {
        override fun openConnection(u: URL): URLConnection = FakeHttpsUrlConnection(u)

        override fun openConnection(
            u: URL,
            p: Proxy,
        ): URLConnection = FakeHttpsUrlConnection(u)
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

    private class FakeHttpsUrlConnection(
        url: URL,
    ) : HttpURLConnection(url) {
        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun getInputStream(): InputStream = InputStream.nullInputStream()

        override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
    }
}
