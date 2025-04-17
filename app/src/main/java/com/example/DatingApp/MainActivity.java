package com.example.DatingApp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.DatingApp.Services.MyDBService;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String FIRESTORE_UID_SP = "firestore_uid_db";
    public static final String USER_SP = "user";
    public static final String USER_UID_SP = "user_uid";
    public static final String USERS_TOKENS_DB = "users_tokens";
    public static final String TOKEN_DB = "token";
    public static final String UID_DB = "uid";
    public static final String NAME_DB = "name";
    public static final int RC_SIGN_IN = 1;
    private static final int RC_SIGN_UP_PHONE_NUM = 2;
    public static final String IMG_URL_DB = "img_url";
    public static final String MESSAGES_DB = "messages";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private Map<String, Object> user;
    private MyDBService myService;
    private ServiceConnection serviceConnection;
    private Boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().setAutoInitEnabled(true);

        checkDeviceVersion();
        checkIfHasExtrasFromNotification();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MyDBService.MyLocalBinder binder = (MyDBService.MyLocalBinder) service;
                myService = binder.getService();
                isBound = true;
                Log.d(TAG, "Service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
                Log.d(TAG, "Service disconnected");
            }
        };

        Intent intent = new Intent(MainActivity.this, MyDBService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        Log.d(TAG, "onCreate: SharedPrefs " + getSharedPreferences(USER_SP, MODE_PRIVATE).getAll().toString());

        LinearLayout linearLayout = findViewById(R.id.linearLayout);

        checkConnectionToIntenet(linearLayout);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build()
        );

        startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.drawable.logo)
                        .setTosAndPrivacyPolicyUrls(
                                "https://example.com/terms.html",
                                "https://example.com/privacy.html")
                        .build(),
                RC_SIGN_IN);

        try {
            AndroidFirebaseSDK();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkConnectionToIntenet(LinearLayout linearLayout) {
        // Check if connection is available
        if (!isNetworkAvailable()) {
            Snackbar.make(linearLayout, "No connection has been found", Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkDeviceVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    private void checkIfHasExtrasFromNotification() {
        // Handle possible data accompanying notification message.
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
    }

    private void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = getString(R.string.msg_subscribed);
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "Subscription failed");
                        }
                        Log.d(TAG, msg);
                    }
                });
    }

    private void saveTokenToDB() {
        // Get the token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        // Get the new instance ID token
                        String token = task.getResult();

                        // Save the token to Firebase database
                        user = new HashMap<>();
                        user.put(TOKEN_DB, token);
                        user.put(UID_DB, currentUser.getUid());
                        user.put(NAME_DB, currentUser.getDisplayName());

                        // Add a null check for photo URL
                        String photoUrl = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";
                        user.put(IMG_URL_DB, photoUrl);

                        SharedPreferences sharedPreferences = getSharedPreferences(USER_SP, MODE_PRIVATE);

                        if (sharedPreferences.getString(FIRESTORE_UID_SP, null) == null ||
                                sharedPreferences.getString(USER_UID_SP, null) == null) {
                            myService.updateFieldInUsersTokens(user);
                            addToSharedPrefs(myService.getFirestore());

                            // Save UID in SharedPreferences
                            sharedPreferences.edit().putString(USER_UID_SP, currentUser.getUid()).apply();

                            // Subscribe to topic
                            subscribeToTopic("general");

                            // Go to the next screen
                            Intent intent = new Intent(MainActivity.this, MainAppActivity.class);
                            startActivity(intent);
                        } else {
                            // If UID exists, update the data in the database
                            updateUserToDB(null);
                        }

                        // Log and show toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserToDB(String fileId) {
        if (fileId == null) {
            fileId = getSharedPreferences(USER_SP, MODE_PRIVATE).getString(FIRESTORE_UID_SP, null);
            Log.d(TAG, "FileID: " + fileId);
        } else {
            addToSharedPrefs(fileId);
        }

        myService.updateFieldInUsersTokens(user);
        subscribeToTopic("general");
        Intent intent = new Intent(MainActivity.this, MainAppActivity.class);
        startActivity(intent);
    }

    private void AndroidFirebaseSDK() throws IOException {
        FileInputStream serviceAccount =
                new FileInputStream("/serviceAccountKey.json");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey("AAAA-Fp1ji4:APA91bHLh1u989ujPYCWnSdQpgcwJnDlSHXu-kKYQ4x9pDhNFL0YcdZwzkxcKA_TLv5AtpCUw9DHwDOI26xV297KwqbCviTAgzjNQnaBEZy5j67Ewrrvpvzni_2gkC7eiPd4F6sk8jfV")
                .setDatabaseUrl("https://mychatapplicationpp.firebaseio.com")
                .build();

        FirebaseApp.initializeApp(this, options);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            // Google Sign In was successful, authenticate with Firebase
            currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "onActivityResult: " + currentUser.getDisplayName());
                saveTokenToDB();
            } else {
                Log.e(TAG, "User not authenticated");
            }
        }
    }

    private void addToSharedPrefs(String documentId) {
        SharedPreferences sharedPreferences = getSharedPreferences(USER_SP, MODE_PRIVATE);
        sharedPreferences.edit().putString(FIRESTORE_UID_SP, documentId).apply();
        sharedPreferences.edit().putString(USER_UID_SP, currentUser.getUid()).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }
}
