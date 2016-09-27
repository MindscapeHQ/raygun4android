package main.java.com.mindscapehq.android.raygun4android.network;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import main.java.com.mindscapehq.android.raygun4android.RaygunClient;
import main.java.com.mindscapehq.android.raygun4android.RaygunLogger;
import main.java.com.mindscapehq.android.raygun4android.RaygunPulseEventType;
import main.java.com.mindscapehq.android.raygun4android.RaygunSettings;
import main.java.com.mindscapehq.android.raygun4android.network.http.RaygunUrlStreamHandlerFactory;

public class RaygunNetworkLogger {

  private static final long CONNECTION_TIMEOUT = 60000L; // 1 min
  private static volatile HashMap<String, RaygunNetworkRequestInfo> connections = new HashMap<String, RaygunNetworkRequestInfo>();
  private static boolean loggingEnabled = true;
  private static boolean loggingInitialized = false;

  public static void init() {
    if (loggingEnabled && !loggingInitialized) {
      try {
        RaygunUrlStreamHandlerFactory factory = new RaygunUrlStreamHandlerFactory();
        RaygunLogger.d("MD | Setting URL Stream Handler Factory!");
        URL.setURLStreamHandlerFactory(factory);
        loggingInitialized = true;
      }
      catch (SecurityException e) {
        loggingInitialized = false;
      }
    }
  }

  public static void setEnabled(boolean enabled) {
    loggingEnabled = enabled;
  }

  public static synchronized void startNetworkCall(String url, long startTime) {
    RaygunLogger.d("MD | startNetworkCall: "+url);
    if (!shouldIgnoreUrl(url)) {

        String id = sanitiseUrl(url);
        connections.put(id, new RaygunNetworkRequestInfo(url, startTime));

        removeOldEntries();
    }
  }

  public static synchronized void endNetworkCall(String url, long endTime, int statusCode) {
    if (url != null) {

      String id = sanitiseUrl(url);
      if ((connections.containsKey(id))) {

        RaygunNetworkRequestInfo request = connections.get(id);
        if (request != null) {
          connections.remove(url);
          sendNetworkTimingEvent(request.url, request.startTime, endTime, statusCode, null);
        }
      }
    }
  }

  public static synchronized void cancelNetworkCall(String url, long endTime, String exception) {
    if (url != null) {
      String id = sanitiseUrl(url);
      if ((connections != null) && (connections.containsKey(id))) {
        RaygunNetworkRequestInfo request = connections.get(id);
        if (request != null) {
          connections.remove(id);
          sendNetworkTimingEvent(request.url, request.startTime, endTime, 0, exception);
        }
      }
    }
  }

  public static synchronized void sendNetworkTimingEvent(String url, long startTime, long endTime, int statusCode, String exception) {
    if (!shouldIgnoreUrl(url)) {
      RaygunClient.SendPulseTimingEvent(RaygunPulseEventType.NetworkCall, url, endTime - startTime);
    }
  }

  private static synchronized void removeOldEntries() {
    Iterator<Map.Entry<String, RaygunNetworkRequestInfo>> it = connections.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, RaygunNetworkRequestInfo> pairs = it.next();
      long startTime = pairs.getValue().startTime;
      if (System.currentTimeMillis() - startTime > CONNECTION_TIMEOUT) {
        it.remove();
      }
    }
  }

  private static String sanitiseUrl(String url) {
    if (url != null) {
      url = url.toLowerCase();
      url = url.replaceAll("https://", "");
      url = url.replaceAll("http://", "");
      url = url.replaceAll("www.", "");
    }
    return url;
  }

  private static boolean shouldIgnoreUrl(String url) {
    if (url == null) {
      return true;
    }
    for (String ignoredUrl : RaygunSettings.getSettings().getIgnoredUrls()) {
      if (url.contains(ignoredUrl) || ignoredUrl.contains(url)) {
        RaygunLogger.d("MD | RaygunNetworkLogger - Ignoring: "+url);
        return true;
      }
    }
    return false;
  }
}