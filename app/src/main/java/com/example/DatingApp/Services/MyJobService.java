package com.example.DatingApp.Services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

@SuppressLint("WorkerHasAPublicModifier")
class MyWorker extends Worker {

    private static final String TAG = "MyWorker";

    public MyWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        // Здесь вы можете реализовать свою логику, например обработку данных
        Log.d(TAG, "Worker is executing the task.");

        // Если задача завершена успешно
        return Result.success();

        // Если произошла ошибка

    }
}
