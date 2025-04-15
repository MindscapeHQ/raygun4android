package com.raygun.raygun4android.network.http

import com.raygun.raygun4android.logging.RaygunLogger.e
import com.raygun.raygun4android.utils.RaygunReflectionUtils
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

internal class RaygunHttpUrlStreamHandler(private val originalHandler: URLStreamHandler) :
    URLStreamHandler() {
    @Throws(IOException::class)
    override fun openConnection(url: URL): URLConnection {
        try {
            val method =
                RaygunReflectionUtils.findMethod(
                    originalHandler.javaClass,
                    "openConnection",
                    arrayOf<Class<*>>(URL::class.java)
                )
            method.isAccessible = true

            val urlConnection = method.invoke(originalHandler, url) as URLConnection
                ?: throw IOException("Failed to create connection")

            return RaygunHttpUrlConnection(urlConnection)
        } catch (e: NoSuchMethodException) {
            e("Exception occurred in openConnection: " + e.message)
        } catch (e: IllegalAccessException) {
            e("Exception occurred in openConnection: " + e.message)
        } catch (e: InvocationTargetException) {
            e("Exception occurred in openConnection: " + e.message)
        }

        throw IOException()
    }

    @Throws(IOException::class)
    override fun openConnection(url: URL, proxy: Proxy): URLConnection {
        try {
            val method =
                RaygunReflectionUtils.findMethod(
                    originalHandler.javaClass,
                    "openConnection",
                    arrayOf(URL::class.java, Proxy::class.java)
                )
            method.isAccessible = true

            val urlConnection =
                method.invoke(originalHandler, url, proxy) as URLConnection
                    ?: throw IOException("Failed to create connection")

            return RaygunHttpUrlConnection(urlConnection)
        } catch (e: NoSuchMethodException) {
            e("Exception occurred in openConnection: " + e.message)
        } catch (e: IllegalAccessException) {
            e("Exception occurred in openConnection: " + e.message)
        } catch (e: InvocationTargetException) {
            e("Exception occurred in openConnection: " + e.message)
        }

        throw IOException()
    }

    public override fun getDefaultPort(): Int {
        return PORT
    }

    companion object {
        private const val PORT = 80
        const val PROTOCOL = "http"
    }
}
