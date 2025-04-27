package com.example.DatingApp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.viewpager.widget.ViewPager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.DatingApp.Adapters.ViewPagerAdapter;
import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.MyUser;
import com.example.DatingApp.Users.User;
import com.example.DatingApp.Users.UserInfo;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrListener;

import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {

    public static final String TAG = "ProfileManager";
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private DrawerLayout drawerLayout;
    private LinearLayout linearLayout, infoMenu;
    private FloatingActionButton openChatBtn;
    private TextView txtNameAge,
            txtAbout,
            txtRole,
            txtHeight,
            txtWeight,
            txtEthnicity,
            txtRelationship,
            txtReference,
            txtOrientation,
            txtReligion;

    private MyDBService myService;
    private ServiceConnection serviceConnection;
    private Boolean isBound;
    private String uid;
    private User currentUser;
    private MyUser myCurrentUser;

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            //unbindService(serviceConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            //unbindService(serviceConnection);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, MyDBService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MyDBService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MyDBService.MyLocalBinder binder = (MyDBService.MyLocalBinder) service;
                myService = binder.getService();
                isBound = true;
                loadUserData(); // Загрузка данных после подключения
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        Bundle bundle = getIntent().getExtras();
        uid = bundle.getString("uid");

        // Инициализация других компонентов
        SlidrConfig config = new SlidrConfig.Builder()
                .sensitivity(999999999).velocityThreshold(0)
                .listener(new SlidrListener() {
                    @Override
                    public void onSlideStateChanged(int state) {
                        Log.d(TAG, "onSlideStateChanged: started.");
                    }

                    @Override
                    public void onSlideChange(float percent) {
                        Log.d(TAG, "onSlideChange: started.");
                    }

                    @Override
                    public void onSlideOpened() {
                        Log.d(TAG, "onSlideOpened: started.");
                    }

                    @Override
                    public void onSlideClosed() {
                        Log.d(TAG, "onSlideClosed: started.");
                    }
                })
                .build();
        Slidr.attach(this, config);

        // Инициализация UI элементов
        drawerLayout = findViewById(R.id.drawerLayout);
        linearLayout = findViewById(R.id.drawer);
        txtAbout = findViewById(R.id.aboutP);
        txtHeight = findViewById(R.id.txtHeight);
        txtRole = findViewById(R.id.txtRole);
        txtWeight = findViewById(R.id.txtWeight);
        txtEthnicity = findViewById(R.id.txtEthnicity);
        txtReference = findViewById(R.id.txtReference);
        txtRelationship = findViewById(R.id.txtRelationship);
        txtOrientation = findViewById(R.id.txtOrientation);
        txtReligion = findViewById(R.id.txtReligion);
        txtNameAge = findViewById(R.id.nameAge);
        openChatBtn = findViewById(R.id.btnChat);
        infoMenu = findViewById(R.id.infoMenu);

        // Начальная загрузка данных
        loadUserData();
    }

    private void loadUserData() {
        myService.getUserByUID(uid, new MyDBService.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                currentUser = user;
                myCurrentUser = new MyUser(user);
                openChatBtn.setEnabled(true);
                UserInfo userInfo = user.getInfo();
                if (userInfo != null) {
                    infoMenu.setVisibility(View.VISIBLE);
                    int userAge = getAgeFromYear(userInfo.getBirthDate());
                    txtNameAge.setText(user.getName() + ", " + userAge);
                    txtAbout.setText(String.valueOf(userInfo.getAbout()));
                    txtHeight.setText(String.valueOf(userInfo.getHeight()));
                    txtWeight.setText(String.valueOf(userInfo.getWeight()));
                    txtEthnicity.setText(getResources().getStringArray(R.array.ethnicity)[Integer.valueOf(userInfo.getEthnicity())]);
                    txtReference.setText(getResources().getStringArray(R.array.reference)[Integer.valueOf(userInfo.getReference())]);
                    txtRelationship.setText(getResources().getStringArray(R.array.relationship)[Integer.valueOf(userInfo.getRelationship())]);
                    txtReligion.setText(getResources().getStringArray(R.array.religion)[Integer.valueOf(userInfo.getReligion())]);
                    txtOrientation.setText(getResources().getStringArray(R.array.orientation)[Integer.valueOf(userInfo.getOrientation())]);
                    txtRole.setText(getResources().getStringArray(R.array.role)[Integer.valueOf(userInfo.getRole())]);
                } else {
                    txtNameAge.setText(user.getName());
                    infoMenu.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading user", e);
            }
        });
    }

    private int getAgeFromYear(String birthDate) {
        return Calendar.getInstance().get(Calendar.YEAR) - Integer.valueOf(birthDate.split("/")[2]);
    }
}
