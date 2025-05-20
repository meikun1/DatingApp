package com.example.DatingApp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.DatingApp.Adapters.ViewPagerAdapter;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrListener;

public class OwnProfileActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private Intent intent;

    public static final String TAG = "OwnProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_own_profile);
        initViewPagerAdapter();

        SlidrConfig config = new SlidrConfig.Builder()
                .sensitivity(999999999)
                .velocityThreshold(0)
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
    }

    private void initViewPagerAdapter() {
        Log.d(TAG, "initViewPagerAdapter: started.");

        viewPager = findViewById(R.id.viewPager);
        viewPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);
    }

    public void settings(View view) {
        intent = new Intent(this, SettingsActivity.class);
        this.startActivity(intent);
    }

    public void editProfile(View view) {
        intent = new Intent(this, ProfileEditorActivity.class);
        this.startActivity(intent);
    }
}
