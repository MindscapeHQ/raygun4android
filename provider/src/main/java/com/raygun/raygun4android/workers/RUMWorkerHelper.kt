package com.raygun.raygun4android.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.raygun.raygun4android.logging.RaygunLogger.i

object RUMWorkerHelper {
    fun enqueueRUM(
        context: Context,
        message: String?,
        apiKey: String?,
    ) {
        val inputData =
            Data
                .Builder()
                .putString("msg", message)
                .putString("apikey", apiKey)
                .build()

        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val workRequest =
            OneTimeWorkRequest
                .Builder(RUMWorker::class.java)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        i("Work for RUMWorker has been put into the queue.")
    }
}
