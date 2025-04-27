package com.example.DatingApp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.LinearLayout;
import com.google.firebase.firestore.GeoPoint;
import java.util.ArrayList;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.DatingApp.Services.MyDBService;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.material.snackbar.Snackbar;

import android.app.NotificationChannel;
import android.app.NotificationManager;

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
    private MyDBService myService;
    private boolean isBound = false;
    private Map<String, Object> user;

    private ServiceConnection serviceConnection = new ServiceConnection() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new AppExceptionHandler(this));
        setContentView(R.layout.activity_main);

        checkDeviceVersion();
        checkIfHasExtrasFromNotification();
        checkConnectionToInternet(findViewById(R.id.linearLayout));

        // Привязываем сервис
        Intent intent = new Intent(MainActivity.this, MyDBService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            // Пользователь не авторизован — запускаем аутентификацию
            startSignInFlow();
        } else {
            // Пользователь авторизован — проверим наличие в базе
            reloadAndProceedUser();
        }

    }

    private void reloadAndProceedUser() {
        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser reloadedUser = firebaseAuth.getCurrentUser();
            if (reloadedUser != null) {
                proceedWithUser(reloadedUser);
            } else {
                startSignInFlow();
            }
        });
    }

    private void startSignInFlow() {
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.PhoneBuilder().build()
        );

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setLogo(R.drawable.logo)
                        .setTosAndPrivacyPolicyUrls(
                                "https://example.com/terms.html",
                                "https://example.com/privacy.html")
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN
        );
    }

    private void proceedWithUser(FirebaseUser user) {
        String userId = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User exists in database");
                        goToMainApp();
                    } else {
                        Log.d(TAG, "User does not exist in database");
                        goToProfileEditor();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user in database: " + e.getMessage());
                });
    }

    private void goToMainApp() {
        Intent intent = new Intent(MainActivity.this, MainAppActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToProfileEditor() {
        Intent intent = new Intent(MainActivity.this, ProfileEditorActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    proceedWithUser(user);
                }
            } else {
                if (response == null) {
                    Log.d(TAG, "Sign-in cancelled");
                    finish();
                } else {
                    Log.e(TAG, "Sign-in error: " + response.getError().getMessage());
                    finish();
                }
            }
        }
    }

    private void checkDeviceVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT));
            }
        }
    }

    private void checkIfHasExtrasFromNotification() {
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
    }

    private void checkConnectionToInternet(LinearLayout layout) {
        if (!isNetworkAvailable()) {
            Snackbar.make(layout, "No Internet connection found", Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }
}
