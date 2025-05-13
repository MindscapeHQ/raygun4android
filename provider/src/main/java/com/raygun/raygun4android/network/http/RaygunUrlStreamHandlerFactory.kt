package com.raygun.raygun4android.network.http

import android.os.Build
import com.raygun.raygun4android.logging.RaygunLogger.e
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

class RaygunUrlStreamHandlerFactory : URLStreamHandlerFactory {
    private val handlers: MutableMap<String, URLStreamHandler> = HashMap()

    init {
        findHandler(RaygunHttpUrlStreamHandler.PROTOCOL)?.let {
            val raygunHttpHandler = RaygunHttpUrlStreamHandler(it)
            handlers[RaygunHttpUrlStreamHandler.PROTOCOL] = raygunHttpHandler
        }
        findHandler(RaygunHttpsUrlStreamHandler.PROTOCOL)?.let {
            val raygunHttpsHandler = RaygunHttpsUrlStreamHandler(it)
            handlers[RaygunHttpsUrlStreamHandler.PROTOCOL] = raygunHttpsHandler
        }
    }

    private fun findHandler(protocol: String): URLStreamHandler? {
        var streamHandler: URLStreamHandler? = null
        val packageList = System.getProperty("java.protocol.handler.pkgs")
        val contextClassLoader = Thread.currentThread().contextClassLoader

        if (packageList != null && contextClassLoader != null) {
            for (
            packageName in
            packageList.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            ) {
                val className = "$packageName.$protocol.Handler"
                try {
                    val c = contextClassLoader.loadClass(className)
                    streamHandler = c.getDeclaredConstructor().newInstance() as URLStreamHandler
                    return streamHandler
                } catch (ignore: IllegalAccessException) {
                } catch (
                    ignore: InstantiationException,
                ) {
                } catch (ignore: ClassNotFoundException) {
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 19) {
            if (protocol == "http") {
                streamHandler = createStreamHandler("com.android.okhttp.HttpHandler")
            } else if (protocol == "https") {
                streamHandler = createStreamHandler("com.android.okhttp.HttpsHandler")
            }
        } else {
            if (protocol == "http") {
                streamHandler = createStreamHandler("libcore.net.http.HttpHandler")
            } else if (protocol == "https") {
                streamHandler = createStreamHandler("libcore.net.http.HttpsHandler")
            }
        }

        return streamHandler
    }

    private fun createStreamHandler(className: String): URLStreamHandler? {
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance()
                as URLStreamHandler
        } catch (e: Exception) {
            e("Exception occurred in createStreamHandler: " + e.message)
        }
        return null
    }

    override fun createURLStreamHandler(protocol: String): URLStreamHandler? = handlers[protocol]
}
