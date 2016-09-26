package main.java.com.mindscapehq.android.raygun4android.network;

//import main.java.com.mindscapehq.android.raygun4android.RaygunClient;
import main.java.com.mindscapehq.android.raygun4android.RaygunSettings;
import main.java.com.mindscapehq.android.raygun4android.network.http.RaygunUrlStreamHandlerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RaygunNetworkLogger {

  class RequestInfo {
    public String url;
    public Long startTime;
  }

  private static final long CONNECTION_TIMEOUT = 60000L; // 1 min
  private static volatile HashMap<String, RequestInfo> connections = new HashMap<String, RequestInfo>();
  private static RaygunNetworkLogger logger = null;

  private boolean loggingEnabled = true;
  private boolean loggingInitialized = false;

  public static synchronized RaygunNetworkLogger getInstance() {
    if (logger == null) {
      logger = new RaygunNetworkLogger();
    }
    return logger;
  }

  public void init() {
    if (loggingEnabled && !loggingInitialized) {
      try {
        RaygunUrlStreamHandlerFactory factory = new RaygunUrlStreamHandlerFactory();
        URL.setURLStreamHandlerFactory(factory);
        loggingInitialized = true;
      }
      catch (SecurityException e) {
        loggingInitialized = false;
      }
    }
  }

  public void setEnabled(boolean enabled) {
    loggingEnabled = enabled;
  }

  public synchronized void startNetworkCall(String url, long startTime) {
    if (!ignoreUrl(url)) {
        RequestInfo request = new RequestInfo();

        request.url = url;
        request.startTime = startTime;

        String id = sanitiseUrl(url);
        connections.put(id, request);

        removeOldEntries();
    }
  }

  public synchronized void endNetworkCall(String url, long endTime, int statusCode) {
    if (url != null) {

      String id = sanitiseUrl(url);
      if ((connections.containsKey(id))) {

        RequestInfo request = connections.get(id);
        if (request != null) {
          connections.remove(url);
          sendNetworkTimingEvent(request.url, request.startTime, endTime, statusCode, null);
        }
      }
    }
  }

  public synchronized void cancelNetworkCall(String url, long endTime, String exception) {
    if (url != null) {
      String id = sanitiseUrl(url);
      if ((connections != null) && (connections.containsKey(id))) {
        RequestInfo request = connections.get(id);
        if (request != null) {
          connections.remove(id);
          sendNetworkTimingEvent(request.url, request.startTime, endTime, 0, exception);
        }
      }
    }
  }

  public synchronized void sendNetworkTimingEvent(String url, long startTime, long endTime, int statusCode, String exception) {
    if (!ignoreUrl(url)) {
      //RaygunClient.SendPulseTimingEvent(RaygunPulseEventType.NetworkCall, url, endTime - startTime);
    }
  }

  private synchronized void removeOldEntries() {
    Iterator<Map.Entry<String, RequestInfo>> it = connections.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, RequestInfo> pairs = it.next();

      long startTime = pairs.getValue().startTime;
      if (System.currentTimeMillis() - startTime > CONNECTION_TIMEOUT) {
        it.remove();
      }
    }
  }

  private String sanitiseUrl(String url) {
    if (url != null) {
      url = url.toLowerCase();
      url = url.replaceAll("https://", "");
      url = url.replaceAll("http://", "");
      url = url.replaceAll("www.", "");
    }
    return url;
  }

  private boolean ignoreUrl(String url) {
    if (url == null) {
      return true;
    }

    for (String ignoredUrl : RaygunSettings.getSettings().getIgnoredUrls()) {
      if (url.contains(ignoredUrl) || ignoredUrl.contains(url)) {
        return true;
      }
    }

    return false;
  }
}