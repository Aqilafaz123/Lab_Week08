package com.example.lab_week_08.worker

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class ThirdWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val id = inputData.getString(INPUT_DATA_ID)
        Thread.sleep(3000L) // simulasi proses 3 detik

        val output = Data.Builder()
            .putString(OUTPUT_DATA_ID, id)
            .build()

        return Result.success(output)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}
