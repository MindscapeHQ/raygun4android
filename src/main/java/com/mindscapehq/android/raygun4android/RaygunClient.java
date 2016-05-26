package main.java.com.mindscapehq.android.raygun4android;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.google.gson.Gson;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunMessage;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunPulseDataMessage;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunPulseMessage;
import main.java.com.mindscapehq.android.raygun4android.messages.RaygunUserInfo;

/**
 * User: Mindscape
 * The official Raygun provider for Android. This is the main class that provides functionality
 * for automatically sending exceptions to the Raygun service.
 *
 * You should call Init() on the static RaygunClient instance, passing in the current Context,
 * instead of instantiating this class.
 */
public class RaygunClient
{
  private static String _apiKey;
  private static Context _context;
  private static String _version;
  private static Intent _service;
  private static String _appContextIdentifier;
  private static String _user;
  private static RaygunUserInfo _userInfo;
  private static RaygunUncaughtExceptionHandler _handler;
  private static RaygunOnBeforeSend _onBeforeSend;

  private static List _tags;
  private static Map _userCustomData;

  /**
   * Initializes the Raygun client. This expects that you have placed the API key in your
   * AndroidManifest.xml, in a <meta-data /> element.
   * @param context The context of the calling Android activity.
   */
  public static void Init(Context context) {
    String apiKey = readApiKey(context);
    Init(context, apiKey);
    _appContextIdentifier = UUID.randomUUID().toString();
  }

  /**
   * Initializes the Raygun client with the version of your application.
   * This expects that you have placed the API key in your AndroidManifest.xml, in a <meta-data /> element.
   * @param version The version of your application, format x.x.x.x, where x is a positive integer.
   * @param context The context of the calling Android activity.
   */
  public static void Init(String version, Context context) {
    String apiKey = readApiKey(context);
    Init(context, apiKey, version);
  }

  /**
   * Initializes the Raygun client with your Android application's context and your
   * Raygun API key. The version transmitted will be the value of the versionName attribute in your manifest element.
   * @param context The Android context of your activity
   * @param apiKey An API key that belongs to a Raygun application created in your dashboard
   */
  public static void Init(Context context, String apiKey)
  {
    _apiKey = apiKey;
    _context = context;

    String version = null;
    try
    {
      version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException e)
    {
      Log.i("Raygun4Android", "Couldn't read version from calling package");
    }
    if (version != null)
    {
      _version = version;
    }
    else
    {
      _version = "Not provided";
    }
  }

  /**
   * Initializes the Raygun client with your Android application's context, your
   * Raygun API key, and the version of your application
   * @param context The Android context of your activity
   * @param apiKey An API key that belongs to a Raygun application created in your dashboard
   * @param version The version of your application, format x.x.x.x, where x is a positive integer.
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
      _handler = new RaygunUncaughtExceptionHandler(oldHandler);

      Thread.setDefaultUncaughtExceptionHandler(_handler);
    }
  }

  /**
   * Attaches a pre-built Raygun exception handler to the thread's DefaultUncaughtExceptionHandler.
   * This automatically sends any exceptions that reaches it to the Raygun API.
   * @param tags A list of tags that relate to the calling application's currently build or state.
   *             These will be appended to all exception messages sent to Raygun.
   * @deprecated Call AttachExceptionHandler(), then SetTags(List) instead
   */
  @Deprecated
  public static void AttachExceptionHandler(List tags) {
    UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    if (!(oldHandler instanceof RaygunUncaughtExceptionHandler))
    {
      _handler = new RaygunUncaughtExceptionHandler(oldHandler, tags);

      Thread.setDefaultUncaughtExceptionHandler(_handler);
    }
  }

  /**
   * Attaches the Raygun Pulse feature which will automatically report session and view events.
   */
  public static void AttachPulse(Activity activity) {
    Pulse.Attach(activity);
  }

  /**
   * Attaches a pre-built Raygun exception handler to the thread's DefaultUncaughtExceptionHandler.
   * This automatically sends any exceptions that reaches it to the Raygun API.
   * @param tags A list of tags that relate to the calling application's currently build or state.
   *             These will be appended to all exception messages sent to Raygun.
   * @param userCustomData A set of key-value pairs that will be attached to each exception message
   *                       sent to Raygun. This can contain any extra data relating to the calling
   *                       application's state you would like to see.
   * @deprecated Call AttachExceptionHandler(), then SetUserCustomData(Map) instead
   */
  @Deprecated
  public static void AttachExceptionHandler(List tags, Map userCustomData) {
    UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
    if (!(oldHandler instanceof RaygunUncaughtExceptionHandler))
    {
      _handler = new RaygunUncaughtExceptionHandler(oldHandler, tags, userCustomData);

      Thread.setDefaultUncaughtExceptionHandler(_handler);
    }
  }

  /**
   * Sends an exception-type object to Raygun.
   * @param throwable The Throwable object that occurred in your application that will be sent to Raygun.
   */
  public static void Send(Throwable throwable)
  {
    RaygunMessage msg = BuildMessage(throwable);
    postCachedMessages();

    if (_tags != null) {
      msg.getDetails().setTags(_tags);
    }

    if (_userCustomData != null) {
      msg.getDetails().setUserCustomData(_userCustomData);
    }

    if (_onBeforeSend != null) {
      msg = _onBeforeSend.OnBeforeSend(msg);

      if (msg == null) {
        return;
      }
    }

    spinUpService(_apiKey, new Gson().toJson(msg), false);
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

    msg.getDetails().setTags(mergeTags(tags));

    if (_userCustomData != null) {
      msg.getDetails().setUserCustomData(_userCustomData);
    }

    if (_onBeforeSend != null) {
      msg = _onBeforeSend.OnBeforeSend(msg);

      if (msg == null) {
        return;
      }
    }

    postCachedMessages();
    spinUpService(_apiKey, new Gson().toJson(msg), false);
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

    msg.getDetails().setTags(mergeTags(tags));
    msg.getDetails().setUserCustomData(mergeUserCustomData(userCustomData));

    if (_onBeforeSend != null) {
      msg = _onBeforeSend.OnBeforeSend(msg);

      if (msg == null) {
        return;
      }
    }

    postCachedMessages();
    spinUpService(_apiKey, new Gson().toJson(msg), false);
  }

  /**
   * Raw post method that delivers a pre-built RaygunMessage to the Raygun API. You do not need to call this method
   * directly unless you want to manually build your own message - for most purposes you should call Send().
   * @param apiKey The API key of the app to deliver to
   * @param jsonPayload The JSON representation of a RaygunMessage to be delivered over HTTPS.
   * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message (invalid properties)
   */
  public static int Post(String apiKey, String jsonPayload)
  {
    try
    {
      if (validateApiKey(apiKey))
      {
        URL endpoint = new URL(RaygunSettings.getSettings().getApiEndpoint());
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-ApiKey", apiKey);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(jsonPayload.toString().getBytes("UTF-8"));
        outputStream.close();

        int responseCode = connection.getResponseCode();
        Log.d("Raygun4Android", "Exception message HTTP POST result: " + responseCode);

        return responseCode;
      }
    }
    catch (Exception e)
    {
      Log.e("Raygun4Android", "Couldn't post exception - " + e.getMessage());
      e.printStackTrace();
    }
    return -1;
  }

  private static String _sessionId;

  protected static void SendPulseEvent(String name) {
    if("session_start".equals(name)) {
      _sessionId = UUID.randomUUID().toString();
    }

    RaygunPulseMessage message = new RaygunPulseMessage();
    RaygunPulseDataMessage pulseData = new RaygunPulseDataMessage();

    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    String timestamp = df.format(Calendar.getInstance().getTime());
    pulseData.setTimestamp(timestamp);
    pulseData.setVersion(_version);

    pulseData.setSessionId(_sessionId);
    pulseData.setType(name);

    message.setEventData(new RaygunPulseDataMessage[]{pulseData});

    spinUpService(_apiKey, new Gson().toJson(message), true);
  }

  protected static int PostPulseMessage(String apiKey, String jsonPayload)
  {
    try
    {
      if (validateApiKey(apiKey))
      {
        URL endpoint = new URL(RaygunSettings.getSettings().getPulseEndpoint());
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("X-ApiKey", apiKey);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(jsonPayload.toString().getBytes("UTF-8"));
        outputStream.close();

        int responseCode = connection.getResponseCode();
        Log.d("Raygun4Android", "Pulse HTTP POST result: " + responseCode);

        return responseCode;
      }
    }
    catch (Exception e)
    {
      Log.e("Raygun4Android", "Couldn't post exception - " + e.getMessage());
      e.printStackTrace();
    }
    return -1;
  }

  private static boolean hasInternetConnection()
  {
    if (_context != null) {
      ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
      return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    return false;
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
          .SetAppContext(_appContextIdentifier)
          .SetVersion(_version)
          .SetNetworkInfo(_context)
          .Build();

      if (_version != null)
      {
        msg.getDetails().setVersion(_version);
      }

      if (_userInfo != null)
      {
        msg.getDetails().setUserContext(_userInfo, _context);
      }
      else if (_user != null)
      {
        msg.getDetails().setUserContext(_user);
      }
      else
      {
        msg.getDetails().setUserContext(_context);
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

  private static Boolean validateApiKey(String apiKey) throws Exception
  {
    if (apiKey.length() == 0)
    {
      Log.e("Raygun4Android", "API key has not been provided, exception will not be logged");
      return false;
    }
    else
    {
      return true;
    }
  }

  private static void postCachedMessages()
  {
    if (hasInternetConnection())
    {
      File[] fileList = _context.getCacheDir().listFiles();
      for (File f : fileList)
      {
        try
        {
          String ext = getExtension(f.getName());
          if (ext.equalsIgnoreCase("raygun"))
          {
            ObjectInputStream ois = null;
            try
            {
              ois = new ObjectInputStream(new FileInputStream(f));
              MessageApiKey messageApiKey = (MessageApiKey) ois.readObject();
              spinUpService(messageApiKey.apiKey, messageApiKey.message, false);
              f.delete();
            }
            finally
            {
              ois.close();
            }
          }
        }
        catch (FileNotFoundException e)
        {
          Log.e("Raygun4Android", "Error loading cached message from filesystem - " + e.getMessage());
        } catch (IOException e)
        {
          Log.e("Raygun4Android", "Error reading cached message from filesystem - " + e.getMessage());
        } catch (ClassNotFoundException e)
        {
          Log.e("Raygun4Android", "Error in cached message from filesystem - " + e.getMessage());
        }
      }
    }
  }

  private static void spinUpService(String apiKey, String jsonPayload, boolean isPulse)
  {
    System.out.println(jsonPayload);
      Intent intent;
      if (_service == null)
      {
          intent = new Intent(_context, RaygunPostService.class);
          intent.setAction("main.java.com.mindscapehq.android.raygun4android.RaygunClient.RaygunPostService");
          intent.setPackage("main.java.com.mindscapehq.android.raygun4android.RaygunClient");
          intent.setComponent(new ComponentName(_context, RaygunPostService.class));
      }
      else
      {
          intent = _service;
      }
      intent.putExtra("msg", jsonPayload);
      intent.putExtra("apikey", apiKey);
      intent.putExtra("isPulse", isPulse ? "True" : "False");
      _service = intent;
      _context.startService(_service);
  }

  public static void closePostService()
  {
    if (_service != null)
    {
      _context.stopService(_service);
      _service = null;
    }
  }

  private static List mergeTags(List paramTags) {
    if (_tags != null) {
      List merged = new ArrayList(_tags);
      merged.addAll(paramTags);

      return merged;
    } else {
      return paramTags;
    }
  }

  private static Map mergeUserCustomData(Map paramUserCustomData) {
    if (_userCustomData != null) {
      Map merged = new HashMap(_userCustomData);
      merged.putAll(paramUserCustomData);

      return merged;
    } else {
      return paramUserCustomData;
    }
  }

  protected static String getExtension(String filename) {
    if (filename == null)
    {
      return null;
    }
    int separator = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
    int dotPos = filename.lastIndexOf(".");
    int index =  separator > dotPos ? -1 : dotPos;
    if (index == -1)
    {
      return "";
    }
    else
    {
      return filename.substring(index + 1);
    }
  }

  /**
   * Sets the current user of your application. If user is an email address which is associated with a Gravatar,
   * their picture will be displayed in the error view. If this is not called a random ID will be assigned.
   * If the user context changes in your application (i.e log in/out), be sure to call this again with the
   * updated user name/email address.
   * @deprecated Call SetUser(RaygunUserInfo) instead
   * @param user A user name or email address representing the current user
   */
  @Deprecated
  public static void SetUser(String user)
  {
    if (user != null && user.length() > 0)
    {
      _user = user;
    }
  }

  public static void SetUser(RaygunUserInfo userInfo)
  {
    _userInfo = userInfo;
  }

  /**
   * Manually stores the version of your application to be transmitted with each message, for version
   * filtering. This is normally read from your AndroidManifest.xml (the versionName attribute on <manifest>)
   * or passed in on Init(); this is only provided as a convenience.
   * @param version The version of your application, format x.x.x.x, where x is a positive integer.
   */
  public static void SetVersion(String version)
  {
    if (version != null)
    {
      _version = version;
    }
  }

  public static RaygunUncaughtExceptionHandler GetExceptionHandler() {
    return _handler;
  }

  public static String getApiKey()
  {
    return _apiKey;
  }

  public static List GetTags() {
    return _tags;
  }

  public static void SetTags(List tags) {
    _tags = tags;
  }

  public static Map GetUserCustomData() {
    return _userCustomData;
  }

  public static void SetUserCustomData(Map userCustomData) {
    _userCustomData = userCustomData;
  }

  public static void SetOnBeforeSend(RaygunOnBeforeSend onBeforeSend) { _onBeforeSend = onBeforeSend; }

  public static class RaygunUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
  {
    private UncaughtExceptionHandler _defaultHandler;
    private List _tags;
    private Map _userCustomData;

    public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler)
    {
      _defaultHandler = defaultHandler;
    }

    @Deprecated
    public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler, List tags)
    {
      _defaultHandler = defaultHandler;
      _tags = tags;
    }

    @Deprecated
    public RaygunUncaughtExceptionHandler(UncaughtExceptionHandler defaultHandler, List tags, Map userCustomData)
    {
      _defaultHandler = defaultHandler;
      _tags = tags;
      _userCustomData = userCustomData;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
      if (_userCustomData != null)
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
