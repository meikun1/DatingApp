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

        // Проверка наличия данных в сообщении
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleNow();
        }

        // Если сообщение содержит уведомление
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Отправка уведомления в случае получения данных
        sendNotification(remoteMessage);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendTokenToDB(token);
    }

    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    // Планируем задачу для фона
    private void scheduleJob() {
        Data inputData = new Data.Builder()
                .putString("message", "Task to be processed")
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);
    }

    // Отправка токена на сервер Firebase
    private void sendTokenToDB(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences(USER_SP, MODE_PRIVATE);
        String userID = sharedPreferences.getString(USER_UID_SP, null);
        String firestoreId = sharedPreferences.getString(FIRESTORE_UID_SP, null);

        // Если ID пользователя нет, выходим
        if (userID == null || firestoreId == null) {
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put(TOKEN_DB, token);
        user.put(UID_DB, userID);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(USERS_TOKENS_DB).document(firestoreId)
                .set(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving token", e));
    }

    // Создание и отображение уведомления
    private void sendNotification(RemoteMessage message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle(getString(R.string.fcm_message))
                        .setContentText(message.getData().toString())
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Создание канала уведомлений для Android Oreo и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
