package main.java.com.mindscapehq.android.raygun4android.network;

import main.java.com.mindscapehq.android.raygun4android.network.http.RaygunUrlStreamHandlerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class RaygunNetworkLogger
{
  class RequestInfo {
    public String protocol;
    public String url;
    public Long startTime;
  }

  private static final long CONNECTION_TIMEOUT = 60000L; // 1 min
  private static volatile HashMap<String, RequestInfo> connections = new HashMap();
  private static RaygunNetworkLogger networkLogger = null;

  private boolean networkMonitoringEnabled = true;
  private boolean networkMonitoringInitialized = false;

  public static synchronized RaygunNetworkLogger getInstance()
  {
    if (networkLogger == null) {
      networkLogger = new RaygunNetworkLogger();
    }
    return networkLogger;
  }

  public void initializeMonitoring()
  {
    if (networkMonitoringEnabled && !networkMonitoringInitialized)
    {
      try
      {
        RaygunUrlStreamHandlerFactory factory = new RaygunUrlStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(factory);
      }
      catch (final Error e)
      {
        networkMonitoringInitialized = false;
      }
      catch (SecurityException e)
      {
        networkMonitoringInitialized = false;
      }
      catch (Throwable e)
      {
        networkMonitoringInitialized = false;
      }
    }
  }

  public void setNetworkMonitoringEnabled(boolean _enabled) {
    this.networkMonitoringEnabled = _enabled;
  }

  public synchronized void startNetworkCall(String id, String url, long startTime, String protocol) {
    if (id != null)
    {
      id = sanitiseUrl(id);

      boolean blacklisted = false;// TODO
      if (!blacklisted)
      {
        RequestInfo request = new RequestInfo();

        request.protocol = protocol;
        request.url = url;
        request.startTime = Long.valueOf(startTime);

        connections.put(id, request);

        removeOldEntries();
      }
    }
  }

  public synchronized void endNetworkCall(String id, long stopTime, int statusCode) {
    if (id != null)
    {
      id = sanitiseUrl(id);
      if ((connections != null) && (connections.containsKey(id)))
      {
        RequestInfo request = connections.get(id);
        if (request != null)
        {
          connections.remove(id);

          sendNetworkTimingEvent(request.protocol, request.url, request.startTime.longValue(), stopTime, statusCode, null);
        }
      }
    }
  }

  public synchronized void cancelNetworkCall(String id, long stopTime, String exception) {
    if (id != null)
    {
      id = sanitiseUrl(id);
      if ((connections != null) && (connections.containsKey(id)))
      {
        RequestInfo request = connections.get(id);
        if (request != null)
        {
          connections.remove(id);

          sendNetworkTimingEvent(request.protocol, request.url, request.startTime.longValue(), stopTime, 0, exception);
        }
      }
    }
  }

  public synchronized void sendNetworkTimingEvent(String protocol, String url, long startTime, long endTime, int statusCode, String exception)
  {
    // Send as pulse event
  }

  public static final int getStatusCodeFromUrlConnection(URLConnection urlConnection)
  {
    int statusCode = 0;
    if (urlConnection != null)
    {
      if ((urlConnection instanceof HttpURLConnection)) {
        try
        {
          statusCode = ((HttpURLConnection)urlConnection).getResponseCode();
        }
        catch (Exception localException) {}
      }
      else if ((urlConnection instanceof HttpsURLConnection)) {
        try
        {
          statusCode = ((HttpsURLConnection)urlConnection).getResponseCode();
        }
        catch (Exception localException1) {}
      }
    }
    return statusCode;
  }

  private synchronized void removeOldEntries() {
    Iterator<Map.Entry<String, RequestInfo>> it = connections.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry<String, RequestInfo> pairs = (Map.Entry)it.next();

      long startTime = pairs.getValue().startTime.longValue();
      if (System.currentTimeMillis() - startTime > CONNECTION_TIMEOUT) {
        it.remove();
      }
    }
  }

  private String sanitiseUrl(String url) {
    if (url != null)
    {
      url = url.toLowerCase();
      url = url.replaceAll("https://", "");
      url = url.replaceAll("http://", "");
      url = url.replaceAll("www.", "");
    }
    return url;
  }
}