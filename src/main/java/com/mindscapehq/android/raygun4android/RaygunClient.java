package main.java.com.mindscapehq.android.raygun4android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.Gson;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.lang.Thread.UncaughtExceptionHandler;
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
  private static String _version;

  /**
   * Initializes the Raygun client. This expects that you have placed the API key in your
   * AndroidManifest.xml, in a <meta-data /> element.
   * @param context The context of the calling Android activity.
   */
  public static void Init(Context context) {
    String apiKey = readApiKey(context);
    Init(context, apiKey);
  }

  /**
   * Initializes the Raygun client with the version of your application.
   * This expects that you have placed the API key in your AndroidManifest.xml, in a <meta-data /> element.
   * @param version The version of your application which will be attached to any exception messages sent.
   * @param context The context of the calling Android activity.
   */
  public static void Init(String version, Context context) {
    String apiKey = readApiKey(context);
    Init(context, apiKey, version);
  }

  /**
   * Initializes the Raygun client with your Android application's context and your
   * Raygun API key.
   * @param context The Android context of your activity
   * @param apiKey An API key that belongs to a Raygun application created in your dashboard
   */
  public static void Init(Context context, String apiKey)
  {
    _apiKey = apiKey;
    _context = context;
  }

  /**
   * Initializes the Raygun client with your Android application's context, your
   * Raygun API key, and the version of your application
   * @param context The Android context of your activity
   * @param apiKey An API key that belongs to a Raygun application created in your dashboard
   * @param version The current version identifier of your Android application. This will be attached to the Raygun message.
   */
  public static void Init(Context context, String apiKey, String version)
  {
    Init(context, apiKey);
    _version = version;
  }

  /**
   * Sends an exception-type object to Raygun.
   * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
   * @return An HTTP code representing the response from the Raygun API.
   *         200 if successful, 400 if bad message generated, 403 if incorrect API key.
   */
  public static int Send(Throwable throwable)
  {
    return Post(BuildMessage(throwable));
  }

  /**
   * Sends an exception-type object to Raygun with a list of tags you specify.
   * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
   * @param tags A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
   *             This could be a build tag, lifecycle state, debug/production version etc.
   * @return An HTTP code representing the response from the Raygun API.
   * 200 if successful, 400 if bad message generated, 403 if incorrect API key.
   */
  public static int Send(Throwable throwable, AbstractList tags)
  {
    RaygunMessage msg = BuildMessage(throwable);
    msg.getDetails().setTags(tags);
    return Post(msg);
  }

  /**
   * Sends an exception-type object to Raygun with a list of tags you specify, and a set of
   * custom data.
   * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
   * @param tags A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
   *             This could be a build tag, lifecycle state, debug/production version etc.
   * @param userCustomData A set of custom key-value pairs relating to your application and its current state. This is a bucket
   *                       where you can attach any related data you want to see to the error.
   * @return An HTTP code representing the response from the Raygun API.
   * 200 if successful, 400 if bad message generated, 403 if incorrect API key.
   */
  public static int Send(Throwable throwable, AbstractList tags, Map userCustomData)
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
      RaygunMessage msg =  RaygunMessageBuilder.New()
                              .SetEnvironmentDetails(_context)
                              .SetMachineName(Build.MODEL)
                              .SetExceptionDetails(throwable)
                              .SetClientDetails()
                              .Build();
      if (_version != null)
      {
        msg.getDetails().setVersion(_version);
      }
      return msg;
    }
    catch (Exception e)
    {
      Log.e("Raygun4Android", "Failed to build RaygunMessage - " + e);
    }
    return null;
  }

  /**
   * Raw post method that delivers a pre-built RaygunMessage to the Raygun API. You do not need to call this method
   * directly unless you want to manually build your own message - for most purposes you should call Send().
   * @param raygunMessage The RaygunMessage to deliver over HTTPS.
   * @return
   */
  public static int Post(RaygunMessage raygunMessage)
  {
    try
    {
      if (validateApiKey())
      {
        String jsonPayload = new Gson().toJson(raygunMessage);

        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(RaygunSettings.getSettings().getApiEndpoint());
        post.addHeader("X-ApiKey", _apiKey);
        post.addHeader("Content-Type", "application/json");

        StringEntity se = new StringEntity(jsonPayload.toString());
        post.setEntity(se);
        HttpResponse response = client.execute(post);
        int result = response.getStatusLine().getStatusCode();
        Log.d("Raygun4Android", "Exception message HTTP POST result: " + result);
        return result;
      }
    }
    catch (Exception e)
    {
      Log.e("Raygun4Android", "Couldn't post exception - ");
      e.printStackTrace();
    }
    return -1;
  }

  private static String readApiKey(Context context)
  {
    try
    {
      ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
      Bundle bundle = ai.metaData;
      String apiKey = bundle.getString("com.mindscapehq.android.raygun4android.apikey");
      return apiKey;
    }
    catch (PackageManager.NameNotFoundException e)
    {
      Log.e("Raygun4Android", "Couldn't read API key from your AndroidManifest.xml <meta-data /> element; cannot send: " + e.getMessage());
    }
    return null;
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
