package main.java.com.mindscapehq.raygun4android;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import main.java.com.mindscapehq.raygun4android.messages.RaygunMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import javax.crypto.Cipher;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.AbstractList;
import java.util.Map;

/**
 * User: Mindscape
 * The official Raygun provider for Android. This is the main class that provides functionality
 * for automatically sending exceptions to the Raygun service.
 */
public class RaygunClient
{
  private static String _apiKey;
  private static Context _context;

  public static void Init(Context context, String apiKey)
  {
    _apiKey = apiKey;
    _context = context;
  }

  public static int Send(Throwable throwable)
  {
    return Post(BuildMessage(throwable));
  }

  public static int Send(Throwable throwable, AbstractList<Object> tags)
  {
    RaygunMessage msg = BuildMessage(throwable);
    msg.getDetails().setTags(tags);
    return Post(msg);
  }

  public static int Send(Throwable throwable, AbstractList<Object> tags, Map<Object, Object> userCustomData)
  {
    RaygunMessage msg = BuildMessage(throwable);
    msg.getDetails().setTags(tags);
    msg.getDetails().setUserCustomData(userCustomData);
    return Post(msg);
  }

  private static RaygunMessage BuildMessage(Throwable throwable)
  {
    try
    {
      return RaygunMessageBuilder.New()
          .SetEnvironmentDetails()
          .SetMachineName("TODO DEVICE NAME")
          .SetExceptionDetails(throwable)
          .SetClientDetails()
          .SetVersion()
          .Build();
    }
    catch (Exception e)
    {
      Log.e("Raygun4Java", "Failed to build RaygunMessage - " + e);
    }
    return null;
  }

  public static int Post(RaygunMessage raygunMessage)
  {
    try
    {
      if (validateApiKey())
      {
        String jsonPayload = new Gson().toJson(raygunMessage);

        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(RaygunSettings.getSettings().getApiEndpoint());
        HttpResponse response;

        StringEntity se = new StringEntity(jsonPayload.toString());
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        response = client.execute(post);
        return response.getStatusLine().getStatusCode();
      }
    }
    catch (Exception e)
    {
      Log.e("Raygun4Android", "Couldn't post exception - ");
      e.printStackTrace();
    }
    return -1;
  }

  private static Boolean validateApiKey() throws Exception
  {
    if (_apiKey.length() == 0)
    {
      Log.e("Raygun4Android", "API key has not been provided, exception will not be logged");
      return false;
    }
    else
    {
      return true;
    }
  }

  private static class RaygunUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
  {
    private UncaughtExceptionHandler defaultHandler;

    public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler)
    {
      this.defaultHandler = defaultHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
      RaygunClient.Send(throwable);
      this.defaultHandler.uncaughtException(thread, throwable);
    }
  }
}
