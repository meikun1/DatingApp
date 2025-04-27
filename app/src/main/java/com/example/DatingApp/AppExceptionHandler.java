package com.example.DatingApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

public class AppExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultUEH;

    public AppExceptionHandler(Context context) {
        this.context = context;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e("AppExceptionHandler", "Uncaught exception", e);

        // Пытаемся показать диалог перед падением
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> showCrashDialog());
        } else {
            restartApp();
        }
    }

    private void showCrashDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Упс! Что-то пошло не так.")
                .setMessage("Хотите перезапустить приложение?")
                .setCancelable(false)
                .setPositiveButton("Перезапустить", (dialog, which) -> restartApp())
                .setNegativeButton("Выход", (dialog, which) -> {
                    Process.killProcess(Process.myPid());
                    System.exit(1);
                })
                .show();
    }

    private void restartApp() {
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        }
        Process.killProcess(Process.myPid());
        System.exit(1);
    }
}
