package main.java.com.mindscapehq.android.raygun4android;

import android.app.IntentService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.google.gson.Gson;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
   * Attaches a pre-built Raygun exception handler to the thread's DefaultUncaughtExceptionHandler.
   * This automatically sends any exceptions that reaches it to the Raygun API.
   */
  public static void AttachExceptionHandler() {
    UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    if (!(oldHandler instanceof RaygunUncaughtExceptionHandler))
    {
      Thread.setDefaultUncaughtExceptionHandler(new RaygunUncaughtExceptionHandler(oldHandler));
    }
  }

  /**
   * Attaches a pre-built Raygun exception handler to the thread's DefaultUncaughtExceptionHandler.
   * This automatically sends any exceptions that reaches it to the Raygun API.
   * @param tags A list of tags that relate to the calling application's currently build or state.
   *             These will be appended to all exception messages sent to Raygun.
   */
  public static void AttachExceptionHandler(List tags) {
    UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    if (!(oldHandler instanceof RaygunUncaughtExceptionHandler))
    {
      Thread.setDefaultUncaughtExceptionHandler(new RaygunUncaughtExceptionHandler(oldHandler, tags));
    }
  }

  /**
   * Attaches a pre-built Raygun exception handler to the thread's DefaultUncaughtExceptionHandler.
   * This automatically sends any exceptions that reaches it to the Raygun API.
   * @param tags A list of tags that relate to the calling application's currently build or state.
   *             These will be appended to all exception messages sent to Raygun.
   * @param userCustomData A set of key-value pairs that will be attached to each exception message
   *                       sent to Raygun. This can contain any extra data relating to the calling
   *                       application's state you would like to see.
   */
  public static void AttachExceptionHandler(List tags, Map userCustomData) {
    UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    if (!(oldHandler instanceof RaygunUncaughtExceptionHandler))
    {
      Thread.setDefaultUncaughtExceptionHandler(new RaygunUncaughtExceptionHandler(oldHandler, tags, userCustomData));
    }
  }

  /**
   * Sends an exception-type object to Raygun.
   * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
   */
  public static void Send(Throwable throwable)
  {
    RaygunMessage msg = BuildMessage(throwable);
    SpinUpService(new Gson().toJson(msg));
  }

  /**
   * Sends an exception-type object to Raygun with a list of tags you specify.
   * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
   * @param tags A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
   *             This could be a build tag, lifecycle state, debug/production version etc.
   */
  public static void Send(Throwable throwable, List tags)
  {
    RaygunMessage msg = BuildMessage(throwable);
    msg.getDetails().setTags(tags);
    SpinUpService(new Gson().toJson(msg));
  }

  /**
   * Sends an exception-type object to Raygun with a list of tags you specify, and a set of
   * custom data.
   * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
   * @param tags A list of data that will be attached to the Raygun message and visible on the error in the dashboard.
   *             This could be a build tag, lifecycle state, debug/production version etc.
   * @param userCustomData A set of custom key-value pairs relating to your application and its current state. This is a bucket
   *                       where you can attach any related data you want to see to the error.
   */
  public static void Send(Throwable throwable, List tags, Map userCustomData)
  {
    RaygunMessage msg = BuildMessage(throwable);
    msg.getDetails().setTags(tags);
    msg.getDetails().setUserCustomData(userCustomData);
    SpinUpService(new Gson().toJson(msg));
  }

  /**
   * Raw post method that delivers a pre-built RaygunMessage to the Raygun API. You do not need to call this method
   * directly unless you want to manually build your own message - for most purposes you should call Send().
   * @param jsonPayload The JSON representation of a RaygunMessage to be delivered over HTTPS.
   * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message (invalid properties)
   */
  public static int Post(String jsonPayload)
  {
    try
    {
      if (validateApiKey())
      {
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

  private static void SpinUpService(String jsonPayload)
  {
    Intent intent = new Intent(_context, RaygunPostService.class);
    intent.setAction("main.java.com.mindscapehq.android.raygun4android.RaygunClient.RaygunPostService");
    intent.setPackage("main.java.com.mindscapehq.android.raygun4android.RaygunClient");
    intent.setComponent(new ComponentName(_context, RaygunPostService.class));
    intent.putExtra("msg", jsonPayload);
    intent.putExtra("apikey", _apiKey);
    Log.i("Raygun4Android", "About to start service");
    _context.startService(intent);
  }

  private static class RaygunUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
  {
    private UncaughtExceptionHandler _defaultHandler;
    private List _tags;
    private Map _userCustomData;

    public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler)
    {
      _defaultHandler = defaultHandler;
    }

    public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler, List tags)
    {
      _defaultHandler = defaultHandler;
      _tags = tags;
    }

    public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler, List tags, Map userCustomData)
    {
      _defaultHandler = defaultHandler;
      _tags = tags;
      _userCustomData = userCustomData;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
      if (_userCustomData != null && _tags != null)
      {
        RaygunClient.Send(throwable, _tags, _userCustomData);
      }
      else if (_tags != null)
      {
        RaygunClient.Send(throwable, _tags);
      }
      else
      {
        RaygunClient.Send(throwable);
      }
      _defaultHandler.uncaughtException(thread, throwable);
    }
  }
}
