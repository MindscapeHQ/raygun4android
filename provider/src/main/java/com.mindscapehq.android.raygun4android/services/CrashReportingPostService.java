package main.java.com.mindscapehq.android.raygun4android.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import main.java.com.mindscapehq.android.raygun4android.RaygunLogger;
import main.java.com.mindscapehq.android.raygun4android.RaygunSettings;
import main.java.com.mindscapehq.android.raygun4android.SerializedMessage;
import main.java.com.mindscapehq.android.raygun4android.network.RaygunNetworkUtils;
import main.java.com.mindscapehq.android.raygun4android.utils.RaygunFileFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CrashReportingPostService extends RaygunPostService {

  static final int CRASHREPORTING_POSTSERVICE_JOB_ID = 4711;
  static final int NETWORK_TIMEOUT = 30;

  public static void enqueueWork(Context context, Intent intent) {
    RaygunLogger.i("Work for CrashReportingPostService has been put in the job queue");
    enqueueWork(context, CrashReportingPostService.class, CRASHREPORTING_POSTSERVICE_JOB_ID, intent);
  }

  @Override
  public void onHandleWork(@NonNull Intent intent) {

    if (intent.getExtras() != null) {

      final Bundle bundle = intent.getExtras();

      String message = bundle.getString("msg");
      String apiKey = bundle.getString("apikey");

      RaygunLogger.v(message);

      // Moved the check for internet connection as close as possible to the calls because the condition can change quite rapidly
      // TODO Check for the 429 coming back from post CR
      if (RaygunNetworkUtils.hasInternetConnection(this.getApplicationContext())) {
        postCrashReporting(apiKey, message);
      } else if (!RaygunNetworkUtils.hasInternetConnection(this.getApplicationContext())) {
        synchronized (this) {
          ArrayList<File> cachedFiles = new ArrayList<>(Arrays.asList(getCacheDir().listFiles(new RaygunFileFilter())));

          if (cachedFiles.size() < RaygunSettings.getMaxReportsStoredOnDevice()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String timestamp = dateFormat.format(new Date(System.currentTimeMillis()));
            File fn = new File(getCacheDir(), timestamp + "-" + uuid + "." + RaygunSettings.DEFAULT_FILE_EXTENSION);

            try {
              SerializedMessage serializedMessage = new SerializedMessage(message);
              ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fn));
              out.writeObject(serializedMessage);
              out.close();
            } catch (FileNotFoundException e) {
              RaygunLogger.e("Error creating file when caching message to filesystem - " + e.getMessage());
            } catch (IOException e) {
              RaygunLogger.e("Error writing message to filesystem - " + e.getMessage());
            }
          } else {
            RaygunLogger.w("Can't write crash report to local disk, maximum number of stored reports reached.");
          }
        }
      }
    }
  }

  /**
   * Raw post method that delivers a pre-built Crash Reporting payload to the Raygun API.
   *
   * @param apiKey      The API key of the app to deliver to
   * @param jsonPayload The JSON representation of a RaygunMessage to be delivered over HTTPS.
   * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message (invalid properties), 429 if rate limited
   */
  private static int postCrashReporting(String apiKey, String jsonPayload) {
    try {
      if (validateApiKey(apiKey)) {
        String endpoint = RaygunSettings.getCrashReportingEndpoint();
        MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT, TimeUnit.SECONDS)
            .build();

        RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, jsonPayload);

        Request request = new Request.Builder()
            .url(endpoint)
            .header("X-ApiKey", apiKey)
            .post(body)
            .build();

        Response response = null;

        try {
          response = client.newCall(request).execute();
          RaygunLogger.d("Exception message HTTP POST result: " + response.code());
          return response.code();
        } catch (IOException ioe) {
          RaygunLogger.e("OkHttp POST to Raygun Crash Reporting backend failed - " + ioe.getMessage());
          ioe.printStackTrace();
        } finally {
          if (response != null) response.body().close();
        }
      }
    } catch (Exception e) {
      RaygunLogger.e("Can't postCrashReporting exception - " + e.getMessage());
      e.printStackTrace();
    }
    return -1;
  }

}
