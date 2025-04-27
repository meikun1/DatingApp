package com.example.DatingApp.Services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.DatingApp.MainActivity;
import com.example.DatingApp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

import static com.example.DatingApp.MainActivity.FIRESTORE_UID_SP;
import static com.example.DatingApp.MainActivity.TOKEN_DB;
import static com.example.DatingApp.MainActivity.UID_DB;
import static com.example.DatingApp.MainActivity.USERS_TOKENS_DB;
import static com.example.DatingApp.MainActivity.USER_SP;
import static com.example.DatingApp.MainActivity.USER_UID_SP;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            sendNotification(remoteMessage);
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendTokenToDB(token);
    }

    private void sendTokenToDB(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(USER_SP, MODE_PRIVATE);
        String userID = sharedPreferences.getString(USER_UID_SP, null);
        String firestoreId = sharedPreferences.getString(FIRESTORE_UID_SP, null);

        if (userID == null || firestoreId == null) {
            Log.w(TAG, "User ID or Firestore ID is null, token not saved.");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put(TOKEN_DB, token);
        userData.put(UID_DB, userID);

        FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .document(firestoreId)
                .set(userData)
                .addOnSuccessListener(unused -> Log.d(TAG, "Token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving token", e));
    }

    private void sendNotification(RemoteMessage remoteMessage) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        String channelId = getString(R.string.default_notification_channel_id);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : getString(R.string.fcm_message);
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : remoteMessage.getData().toString();

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // Check if the notification channel already exists, create it if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
                if (channel == null) {
                    channel = new NotificationChannel(
                            channelId,
                            "Dating App Notifications",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
                    notificationManager.createNotificationChannel(channel);
                }
            }

            notificationManager.notify(0, notificationBuilder.build());
        } else {
            Log.e(TAG, "NotificationManager is null");
        }
    }

    // Дополнительно: метод для запуска фоновой задачи (если понадобится)
    private void scheduleJob(String message) {
        Data inputData = new Data.Builder()
                .putString("message", message)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);
    }
}
