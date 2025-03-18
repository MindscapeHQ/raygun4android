package com.raygun.raygun4android.workers;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.raygun.raygun4android.RaygunSettings;
import com.raygun.raygun4android.logging.RaygunLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CrashReportingWorkerHelper {

    private static final int MAX_DATA_SIZE = 10_000;

    public static void enqueueCrashReport(Context context, String message, String apiKey) {
        Data inputData;
        byte[] encoded = message.getBytes(StandardCharsets.UTF_8);
        int length = encoded.length;
        RaygunLogger.v("Message length: " + length);
        if (length > MAX_DATA_SIZE) {
            RaygunLogger.d("Message length (" + length + ") greater than " + MAX_DATA_SIZE + ", storing as file.");
            // Store the message in a file to circumvent the WorkManager's 10240 bytes limit
            String fileName = storeMessageInTempFile(context, encoded);
            RaygunLogger.i("Stored temp file: " + fileName);
            inputData =
                    new Data.Builder()
                            .putString("file", fileName)
                            .putString("apikey", apiKey)
                            .build();
        } else {
            inputData =
                    new Data.Builder()
                            .putByteArray("msg", encoded)
                            .putString("apikey", apiKey)
                            .build();
        }

        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(CrashReportingWorker.class)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        RaygunLogger.i("Work for CrashReportingWorker has been put into the queue.");
    }

    private static String storeMessageInTempFile(Context context, byte[] message) {
        @SuppressLint("SimpleDateFormat")
        String timestamp =
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()));
        String uuid = UUID.randomUUID().toString().replace("-", "");

        File file =
                new File(
                        context.getFilesDir(),
                        timestamp + "-" + uuid + "." + RaygunSettings.DEFAULT_FILE_EXTENSION);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(message);
            RaygunLogger.i("Crash report message has been written to file.");
        } catch (IOException e) {
            RaygunLogger.e("Failed to write crash report message to file: " + e.getMessage());
        }

        return file.getName();
    }
}
