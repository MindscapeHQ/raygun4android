package com.raygun.raygun4android.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.raygun.raygun4android.RaygunSettings;
import com.raygun.raygun4android.logging.RaygunLogger;
import com.raygun.raygun4android.network.RaygunNetworkUtils;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RUMWorker extends Worker {

    private static final int NETWORK_TIMEOUT = 30;

    public RUMWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve data from WorkManager
        String message = getInputData().getString("msg");
        String apiKey = getInputData().getString("apikey");

        RaygunLogger.v(message);

        // Moved the check for internet connection as close as possible to the calls because the
        // condition can change quite rapidly
        if (message != null && apiKey != null) {
            if (RaygunNetworkUtils.hasInternetConnection(getApplicationContext())) {
                int responseCode = postRUM(apiKey, message);
                RaygunLogger.responseCode(responseCode);
            }
            return Result.success();
        }
        RaygunLogger.e("No message or API key was provided.");
        return Result.failure();
    }

    /**
     * Raw post method that delivers a pre-built RUM payload to the Raygun API.
     *
     * @param apiKey The API key of the app to deliver to
     * @param jsonPayload The JSON representation of a ??? to be delivered over HTTPS.
     * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message
     *     (invalid properties)
     */
    private static int postRUM(String apiKey, String jsonPayload) {
        try {
            if (RaygunWorkerHelper.validateApiKey(apiKey)) {
                String endpoint = RaygunSettings.getRUMEndpoint();
                MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

                OkHttpClient client = RaygunSettings.getHttpClient();

                RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, jsonPayload);

                Request request =
                        new Request.Builder()
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
                    RaygunLogger.e("OkHttp POST to Raygun RUM backend failed: " + ioe.getMessage());
                    ioe.printStackTrace();
                } finally {
                    if (response != null) {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            RaygunLogger.e("Can't post to RUM. Exception - " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
}
