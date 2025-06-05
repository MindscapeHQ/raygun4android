package com.raygun.raygun4android.workers;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.raygun.raygun4android.RaygunSettings;
import com.raygun.raygun4android.SerializedMessage;
import com.raygun.raygun4android.logging.RaygunLogger;
import com.raygun.raygun4android.network.RaygunNetworkUtils;
import com.raygun.raygun4android.utils.RaygunFileFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CrashReportingWorker extends Worker {

    private static final int NETWORK_TIMEOUT = 30;

    public CrashReportingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrieve data from WorkManager
        String file = getInputData().getString("file");
        String apiKey = getInputData().getString("apikey");

        String message = readMessageFromTempFileAndDelete(file);

        if (apiKey != null) {
            if (RaygunNetworkUtils.hasInternetConnection(getApplicationContext())) {
                int responseCode = postCrashReporting(apiKey, message);
                RaygunLogger.responseCode(responseCode);

                if (responseCode == RaygunSettings.RESPONSE_CODE_RATE_LIMITED) {
                    saveMessage(message);
                }
            } else {
                saveMessage(message);
            }
            return Result.success();
        }

        RaygunLogger.e("No message or API key was provided.");
        return Result.failure();
    }

    private void saveMessage(String message) {
        synchronized (this) {
            ArrayList<File> cachedFiles =
                    new ArrayList<>(
                            Arrays.asList(
                                    getApplicationContext()
                                            .getCacheDir()
                                            .listFiles(new RaygunFileFilter())));

            if (cachedFiles.size() < RaygunSettings.getMaxReportsStoredOnDevice()) {
                @SuppressLint("SimpleDateFormat")
                String timestamp =
                        new SimpleDateFormat("yyyyMMddHHmmss")
                                .format(new Date(System.currentTimeMillis()));
                String uuid = UUID.randomUUID().toString().replace("-", "");
                File file =
                        new File(
                                getApplicationContext().getCacheDir(),
                                timestamp
                                        + "-"
                                        + uuid
                                        + "."
                                        + RaygunSettings.DEFAULT_FILE_EXTENSION);

                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                    SerializedMessage serializedMessage = new SerializedMessage(message);
                    out.writeObject(serializedMessage);
                    out.close();
                } catch (FileNotFoundException e) {
                    RaygunLogger.e(
                            "Error creating file when caching message to filesystem: "
                                    + e.getMessage());
                } catch (IOException e) {
                    RaygunLogger.e("Error writing message to filesystem: " + e.getMessage());
                }
            } else {
                RaygunLogger.w("Maximum stored reports reached. Discarding message.");
            }
        }
    }

    /**
     * Raw post method that delivers a pre-built Crash Reporting payload to the Raygun API.
     *
     * @param apiKey The API key of the app to deliver to
     * @param jsonPayload The JSON representation of a RaygunMessage to be delivered over HTTPS.
     * @return HTTP result code - 202 if successful, 403 if API key invalid, 400 if bad message
     *     (invalid properties), 429 if rate limited
     */
    private int postCrashReporting(String apiKey, String jsonPayload) {
        try {
            if (RaygunWorkerHelper.validateApiKey(apiKey)) {
                String endpoint = RaygunSettings.getCrashReportingEndpoint();
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
                    RaygunLogger.d("Crash Reporting HTTP POST result: " + response.code());
                    return response.code();
                } catch (IOException ioe) {
                    RaygunLogger.e(
                            "OkHttp POST to Raygun Crash Reporting backend failed: "
                                    + ioe.getMessage());
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
            RaygunLogger.e("Error posting to Crash Reporting: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private String readMessageFromTempFileAndDelete(String fileName) {
        File file = new File(getApplicationContext().getFilesDir(), fileName);
        StringBuilder message = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file)) {
            int ch;
            while ((ch = fis.read()) != -1) {
                message.append((char) ch);
            }

            if (!file.delete()) {
                RaygunLogger.e("Failed to delete the file: " + fileName);
            }
        } catch (IOException e) {
            RaygunLogger.e("Failed to read message from file: " + e.getMessage());
        }

        return message.toString();
    }
}
