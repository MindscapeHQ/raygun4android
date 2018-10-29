package main.java.com.mindscapehq.android.raygun4android.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import main.java.com.mindscapehq.android.raygun4android.RaygunLogger;
import main.java.com.mindscapehq.android.raygun4android.RaygunSettings;
import main.java.com.mindscapehq.android.raygun4android.network.RaygunNetworkUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RUMPostService extends RaygunPostService {

  static final int RUM_POSTSERVICE_JOB_ID = 815;
  static final int NETWORK_TIMEOUT = 30;

  public static void enqueueWork(Context context, Intent intent) {
    RaygunLogger.i("Work for RUMPostService has been put in the job queue");
    enqueueWork(context, RUMPostService.class, RUM_POSTSERVICE_JOB_ID, intent);
  }

  @Override
  public void onHandleWork(@NonNull Intent intent) {

    if (intent.getExtras() != null) {
      final Bundle bundle = intent.getExtras();

      String message = bundle.getString("msg");
      String apiKey = bundle.getString("apikey");

      RaygunLogger.v(message);

      // Moved the check for internet connection as close as possible to the calls because the condition can change quite rapidly
      if (RaygunNetworkUtils.hasInternetConnection(this.getApplicationContext())) {
        postRUM(apiKey, message);
      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  /**
   * Raw post method that delivers a pre-built RUM payload to the Raygun API.
   *
   * @param apiKey      The API key of the app to deliver to
   * @param jsonPayload The JSON representation of a ??? to be delivered over HTTPS.
   * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message (invalid properties)
   */
  private static int postRUM(String apiKey, String jsonPayload) {
    try {
      if (validateApiKey(apiKey)) {
        String endpoint = RaygunSettings.getRUMEndpoint();
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
          RaygunLogger.d("RUM HTTP POST result: " + response.code());
          return response.code();
        } catch (IOException ioe) {
          RaygunLogger.e("OkHttp POST to Raygun RUM backend failed - " + ioe.getMessage());
          ioe.printStackTrace();
        } finally {
          if (response != null) response.body().close();
        }
      }
    } catch (Exception e) {
      RaygunLogger.e("Can't post to RUM. Exception - " + e.getMessage());
      e.printStackTrace();
    }
    return -1;
  }
}
