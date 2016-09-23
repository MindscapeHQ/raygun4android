package main.java.com.mindscapehq.android.raygun4android.network.http;

import main.java.com.mindscapehq.android.raygun4android.RaygunLogger;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import android.os.Build;

public class RaygunUrlStreamHandlerFactory implements URLStreamHandlerFactory
{
  private Map<String, URLStreamHandler> handlers;

  public RaygunUrlStreamHandlerFactory()
  {
    createStreamHandlers();
  }

  public void createStreamHandlers()
  {
    this.handlers = new HashMap();

    URLStreamHandler httpHandler = findHandler("http");
    if (httpHandler != null)
    {
      RaygunHttpUrlStreamHandler raygunHttpHandler = new RaygunHttpUrlStreamHandler(httpHandler);
      this.handlers.put(raygunHttpHandler.getProtocol(), raygunHttpHandler);
    }

    URLStreamHandler httpsHandler = findHandler("https");
    if (httpsHandler != null)
    {
      RaygunHttpsUrlStreamHandler raygunHttpsHandler = new RaygunHttpsUrlStreamHandler(httpsHandler);
      this.handlers.put(raygunHttpsHandler.getProtocol(), raygunHttpsHandler);
    }
  }

  private URLStreamHandler findHandler(String protocol)
  {
    URLStreamHandler streamHandler = null;

    String packageList = System.getProperty("java.protocol.handler.pkgs");
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

    if (packageList != null && contextClassLoader != null)
    {
      for (String packageName : packageList.split("\\|"))
      {
        String className = packageName + "." + protocol + ".Handler";
        try
        {
          Class<?> c = contextClassLoader.loadClass(className);
          streamHandler = (URLStreamHandler) c.newInstance();

          if (streamHandler != null)
          {
            return streamHandler;
          }
        }
        catch (IllegalAccessException ignored) {
        }
        catch (InstantiationException ignored) {
        }
        catch (ClassNotFoundException ignored) {
        }
        catch (Throwable e) {
        }
      }
    }

    if (Build.VERSION.SDK_INT >= 19)
    {
      if (protocol.equals("http"))
      {
        streamHandler = createStreamHandler("com.android.okhttp.HttpHandler");
      }
      else if (protocol.equals("https"))
      {
        streamHandler = createStreamHandler("com.android.okhttp.HttpsHandler");
      }
    }
    else
    {
      if (protocol.equals("http"))
      {
        streamHandler = createStreamHandler("libcore.net.http.HttpHandler");
      }
      else if (protocol.equals("https"))
      {
        streamHandler = createStreamHandler("libcore.net.http.HttpsHandler");
      }
    }

    return streamHandler;
  }

  private URLStreamHandler createStreamHandler(String className)
  {
    try
    {
      return (URLStreamHandler) Class.forName(className).newInstance();
    }
    catch (Exception e)
    {
      throw new AssertionError(e);
    }
  }

  public URLStreamHandler createURLStreamHandler(String protocol)
  {
    return this.handlers.get(protocol);
  }
}