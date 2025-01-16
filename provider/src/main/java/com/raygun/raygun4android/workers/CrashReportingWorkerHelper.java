package com.raygun.raygun4android.workers;

import android.content.Context;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.raygun.raygun4android.logging.RaygunLogger;

public class CrashReportingWorkerHelper {
    public static void enqueueCrashReport(Context context, String message, String apiKey) {
        Data inputData =
                new Data.Builder().putString("msg", message).putString("apikey", apiKey).build();

        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(CrashReportingWorker.class)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        RaygunLogger.i("Work for CrashReportingWorker has been put into the queue.");
    }
}
