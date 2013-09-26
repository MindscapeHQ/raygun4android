package main.java.com.mindscapehq.raygun4android;

import android.content.Context;
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

        /*HttpURLConnection connection = (HttpURLConnection) new URL(RaygunSettings.getSettings().getApiEndpoint()).openConnection();

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
        return connection.getResponseCode();*/

        // ----

        /*HttpClient httpClient = new DefaultHttpClient();
        HttpPost post = new HttpPost(RaygunSettings.getSettings().getApiEndpoint());
        HttpResponse response;

        StringEntity se = new StringEntity(jsonPayload.toString());
        se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        post.setEntity(se);
        response = httpClient.execute(post);
        return response.getStatusLine().getStatusCode();*/
        //---
        /*URL url = new URL(RaygunSettings.getSettings().getApiEndpoint());
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, null, new SecureRandom());
        conn.setSSLSocketFactory(sc.getSocketFactory());
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        PrintWriter out = new PrintWriter(conn.getOutputStream());
        out.print(jsonPayload);
        out.close();

        String result = new String();
        InputStream is = conn.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          result += inputLine;
        }
        return 400;*/

        System.out.println(jsonPayload);

        DefaultHttpClient client = new RaygunHttpClient(_context);
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
      System.err.println("Raygun4Java: Couldn't post exception - "); // TODO change to use Log.e
      e.printStackTrace();
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
