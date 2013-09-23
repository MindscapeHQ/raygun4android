package main.java.com.mindscapehq.raygun4android;

import com.google.gson.Gson;
import main.java.com.mindscapehq.raygun4android.messages.RaygunMessage;

import java.io.OutputStreamWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractList;
import java.util.Map;

/**
 * User: Mindscape
 * The official Raygun provider for Android. This is the main class that provides functionality
 * for automatically sending exceptions to the Raygun service.
 */
public class RaygunClient
{
  private static final String _endpoint = "https://api.raygun.io/entries";

  private static String _apiKey;

  public static void Init(String apiKey)
  {
    _apiKey = apiKey;
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
          .SetDeviceName("TODO DEVICE NAME")
          .SetExceptionDetails(throwable)
          .SetClientDetails()
          .SetVersion()
          .Build();
    }
    catch (Exception e)
    {
      System.err.println("Raygun4Java: Failed to build RaygunMessage - " + e);
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

        HttpURLConnection connection = (HttpURLConnection) new URL(RaygunSettings.getSettings().getApiEndpoint()).openConnection();

        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("charset", "utf-8");
        connection.setRequestProperty("X-ApiKey", _apiKey);

        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(jsonPayload);
        writer.flush();
        writer.close();
        connection.disconnect();
        return connection.getResponseCode();
      }
    }
    catch (Exception e)
    {
      System.err.println("Raygun4Java: Couldn't post exception - " + e.getMessage());
    }
    return -1;
  }

  private static Boolean validateApiKey() throws Exception
  {
    if (_apiKey.length() == 0)
    {
      throw new Exception("API key has not been provided, exception will not be logged");
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
