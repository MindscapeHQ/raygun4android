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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CrashReportingWorkerHelper {
    public static void enqueueCrashReport(Context context, String message, String apiKey) {

        Data inputData;

        if (message.length() > 10000) {
            // Store the message in a file to circumvent the WorkManager's 10240 bytes limit
            String fileName = storeMessageInTempFile(context, message);
            RaygunLogger.i("Stored temp file: " + fileName);
            inputData =
                    new Data.Builder()
                            .putString("file", fileName)
                            .putString("apikey", apiKey)
                            .build();
        } else {
            inputData =
                    new Data.Builder()
                            .putString("msg", message)
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

    private static String storeMessageInTempFile(Context context, String message) {
        @SuppressLint("SimpleDateFormat")
        String timestamp =
                new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis()));
        String uuid = UUID.randomUUID().toString().replace("-", "");

        File file =
                new File(
                        context.getFilesDir(),
                        timestamp + "-" + uuid + "." + RaygunSettings.DEFAULT_FILE_EXTENSION);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(message.getBytes());
            RaygunLogger.i("Crash report message has been written to file.");
        } catch (IOException e) {
            RaygunLogger.e("Failed to write crash report message to file: " + e.getMessage());
        }

        return file.getName();
    }
}
