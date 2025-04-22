package com.raygun.raygun4android.network.http

import com.raygun.raygun4android.network.RaygunNetworkLogger
import com.raygun.raygun4android.network.RaygunNetworkUtils
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.net.URLConnection
import java.security.Permission
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException

internal class RaygunHttpsUrlConnection(
    private val connectionInstance: URLConnection,
) : HttpsURLConnection(connectionInstance.url) {
    init {
        RaygunNetworkLogger.startNetworkCall(
            connectionInstance.url.toExternalForm(),
            System.currentTimeMillis(),
        )
    }

    @Throws(IOException::class)
    override fun connect() {
        try {
            connectionInstance.connect()
        } catch (e: IOException) {
            RaygunNetworkLogger.cancelNetworkCall(
                url.toExternalForm(),
                requestMethod,
                System.currentTimeMillis(),
                e.message,
            )
            throw e
        }
    }

    override fun disconnect() {
        val statusCode = RaygunNetworkUtils.getStatusCode(connectionInstance)
        RaygunNetworkLogger.endNetworkCall(
            url.toExternalForm(),
            requestMethod,
            System.currentTimeMillis(),
            statusCode,
        )

        if ((connectionInstance is HttpURLConnection)) {
            connectionInstance.disconnect()
        }
    }

    @Throws(IOException::class)
    override fun getInputStream(): InputStream {
        try {
            return connectionInstance.getInputStream()
        } catch (e: IOException) {
            RaygunNetworkLogger.cancelNetworkCall(
                url.toExternalForm(),
                requestMethod,
                System.currentTimeMillis(),
                e.message,
            )
            throw e
        }
    }

    @Throws(IOException::class)
    override fun getOutputStream(): OutputStream {
        try {
            return connectionInstance.getOutputStream()
        } catch (e: IOException) {
            RaygunNetworkLogger.cancelNetworkCall(
                url.toExternalForm(),
                requestMethod,
                System.currentTimeMillis(),
                e.message,
            )
            throw e
        }
    }

    override fun getAllowUserInteraction(): Boolean = connectionInstance.allowUserInteraction

    override fun addRequestProperty(
        field: String,
        newValue: String,
    ) {
        connectionInstance.addRequestProperty(field, newValue)
    }

    override fun getConnectTimeout(): Int = connectionInstance.connectTimeout

    @Throws(IOException::class)
    override fun getContent(): Any {
        try {
            return connectionInstance.content
        } catch (e: IOException) {
            RaygunNetworkLogger.cancelNetworkCall(
                url.toExternalForm(),
                requestMethod,
                System.currentTimeMillis(),
                e.message,
            )
            throw e
        }
    }

    @Throws(IOException::class)
    override fun getContent(types: Array<Class<*>?>?): Any {
        try {
            return connectionInstance.getContent(types)
        } catch (e: IOException) {
            RaygunNetworkLogger.cancelNetworkCall(
                url.toExternalForm(),
                requestMethod,
                System.currentTimeMillis(),
                e.message,
            )
            throw e
        }
    }

    override fun getContentEncoding(): String = connectionInstance.contentEncoding

    override fun getContentLength(): Int = connectionInstance.contentLength

    override fun getContentType(): String = connectionInstance.contentType

    override fun getDate(): Long = connectionInstance.date

    override fun getDefaultUseCaches(): Boolean = connectionInstance.defaultUseCaches

    override fun getDoInput(): Boolean = connectionInstance.doInput

    override fun getDoOutput(): Boolean = connectionInstance.doOutput

    override fun getExpiration(): Long = connectionInstance.expiration

    override fun getHeaderField(pos: Int): String = connectionInstance.getHeaderField(pos)

    override fun getHeaderField(key: String): String = connectionInstance.getHeaderField(key)

    override fun getHeaderFieldDate(
        field: String,
        defaultValue: Long,
    ): Long = connectionInstance.getHeaderFieldDate(field, defaultValue)

    override fun getHeaderFieldInt(
        field: String,
        defaultValue: Int,
    ): Int = connectionInstance.getHeaderFieldInt(field, defaultValue)

    override fun getHeaderFieldKey(posn: Int): String = connectionInstance.getHeaderFieldKey(posn)

    override fun getHeaderFields(): Map<String, List<String>> = connectionInstance.headerFields

    override fun getIfModifiedSince(): Long = connectionInstance.getIfModifiedSince()

    override fun getLastModified(): Long = connectionInstance.lastModified

    @Throws(IOException::class)
    override fun getPermission(): Permission {
        try {
            return connectionInstance.permission
        } catch (e: IOException) {
            RaygunNetworkLogger.cancelNetworkCall(
                url.toExternalForm(),
                requestMethod,
                System.currentTimeMillis(),
                e.message,
            )
            throw e
        }
    }

    override fun getReadTimeout(): Int = connectionInstance.readTimeout

    override fun getRequestProperties(): Map<String, List<String>> = connectionInstance.requestProperties

    override fun getRequestProperty(field: String): String = connectionInstance.getRequestProperty(field)

    override fun getURL(): URL = connectionInstance.url

    override fun getUseCaches(): Boolean = connectionInstance.useCaches

    override fun setAllowUserInteraction(newValue: Boolean) {
        connectionInstance.allowUserInteraction = newValue
    }

    override fun setConnectTimeout(timeoutMillis: Int) {
        connectionInstance.connectTimeout = timeoutMillis
    }

    override fun setDefaultUseCaches(newValue: Boolean) {
        connectionInstance.defaultUseCaches = newValue
    }

    override fun setDoInput(newValue: Boolean) {
        connectionInstance.doInput = newValue
    }

    override fun setDoOutput(newValue: Boolean) {
        connectionInstance.doOutput = newValue
    }

    override fun setIfModifiedSince(newValue: Long) {
        connectionInstance.ifModifiedSince = newValue
    }

    override fun setReadTimeout(timeoutMillis: Int) {
        connectionInstance.readTimeout = timeoutMillis
    }

    override fun setRequestProperty(
        field: String,
        newValue: String,
    ) {
        connectionInstance.setRequestProperty(field, newValue)
    }

    override fun setUseCaches(newValue: Boolean) {
        connectionInstance.useCaches = newValue
    }

    override fun usingProxy(): Boolean {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.usingProxy()
        }
        return false
    }

    override fun getErrorStream(): InputStream? {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.errorStream
        }
        return null
    }

    override fun getInstanceFollowRedirects(): Boolean {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.instanceFollowRedirects
        }
        return true
    }

    override fun getRequestMethod(): String {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.requestMethod
        }
        return "GET"
    }

    @Throws(IOException::class)
    override fun getResponseCode(): Int {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.responseCode
        }
        return -1
    }

    @Throws(IOException::class)
    override fun getResponseMessage(): String {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.responseMessage
        }
        return ""
    }

    override fun setChunkedStreamingMode(chunkLength: Int) {
        if ((connectionInstance is HttpsURLConnection)) {
            connectionInstance.setChunkedStreamingMode(chunkLength)
        }
    }

    override fun setFixedLengthStreamingMode(contentLength: Int) {
        if ((connectionInstance is HttpsURLConnection)) {
            connectionInstance.setFixedLengthStreamingMode(contentLength)
        }
    }

    override fun setInstanceFollowRedirects(followRedirects: Boolean) {
        if ((connectionInstance is HttpsURLConnection)) {
            connectionInstance.instanceFollowRedirects = followRedirects
        }
    }

    @Throws(ProtocolException::class)
    override fun setRequestMethod(method: String) {
        if ((connectionInstance is HttpsURLConnection)) {
            connectionInstance.requestMethod = method
        }
    }

    override fun getCipherSuite(): String {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.cipherSuite
        }
        return ""
    }

    override fun getLocalCertificates(): Array<Certificate>? {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.localCertificates
        }
        return null
    }

    @Throws(SSLPeerUnverifiedException::class)
    override fun getServerCertificates(): Array<Certificate>? {
        if ((connectionInstance is HttpsURLConnection)) {
            return connectionInstance.serverCertificates
        }
        return null
    }
}
