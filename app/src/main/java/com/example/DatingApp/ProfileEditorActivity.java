package com.example.DatingApp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Button;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.DatingApp.Users.UserInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileEditorActivity extends AppCompatActivity {

    private String userId;
    private EditText txtAbout;
    private NumberPicker pkrDay, pkrMonth, pkrYear, pkrHeight, pkrWeight;
    private Spinner spnrEthnicity, spnrReligion, spnrRelationship, spnrOrientation, spnrReference, spnrSTDs, spnrRole;
    private ArrayAdapter<CharSequence> arrayAdapter;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_editor);

        // Получение UID авторизованного пользователя
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Log.e("ProfileEditor", "FirebaseAuth.getCurrentUser() вернул null!");
            userId = null;
        }

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Инициализация UI
        initializeUI();

        FloatingActionButton btnChat = findViewById(R.id.btnChat);

        btnChat.setOnClickListener(view -> {
            // Проверка и обновление данных перед переходом в MainAppActivity
            editProfileAndNavigateToMainApp();
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Настройка NumberPickers и Spinners
        setupNumberPickers();
        setupSpinners();
    }

    private void initializeUI() {
        txtAbout = findViewById(R.id.about);
        pkrDay = findViewById(R.id.day);
        pkrMonth = findViewById(R.id.month);
        pkrYear = findViewById(R.id.year);
        pkrHeight = findViewById(R.id.height);
        pkrWeight = findViewById(R.id.weight);
        spnrEthnicity = findViewById(R.id.ethnicity);
        spnrOrientation = findViewById(R.id.orientation);
        spnrRelationship = findViewById(R.id.relationship);
        spnrReligion = findViewById(R.id.religion);
        spnrReference = findViewById(R.id.reference);
        spnrSTDs = findViewById(R.id.stdss);
        spnrRole = findViewById(R.id.role);
        progressBar = findViewById(R.id.progressBarInfo);
    }

    private void setupNumberPickers() {
        pkrDay.setMinValue(1);
        pkrDay.setMaxValue(31);
        pkrMonth.setMinValue(1);
        pkrMonth.setMaxValue(12);
        pkrYear.setMinValue(1930);
        pkrYear.setMaxValue(2001);
        pkrYear.setValue(1995);
        pkrHeight.setMinValue(100);
        pkrHeight.setMaxValue(230);
        pkrWeight.setMinValue(40);
        pkrWeight.setMaxValue(200);
    }

    private void setupSpinners() {
        arrayAdapter = ArrayAdapter.createFromResource(this, R.array.ethnicity, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnrEthnicity.setAdapter(arrayAdapter);

        setupSpinner(spnrReference, R.array.reference);
        setupSpinner(spnrReligion, R.array.religion);
        setupSpinner(spnrRelationship, R.array.relationship);
        setupSpinner(spnrOrientation, R.array.orientation);
        setupSpinner(spnrSTDs, R.array.stds);
        setupSpinner(spnrRole, R.array.role);
    }

    private void setupSpinner(Spinner spinner, int arrayResourceId) {
        arrayAdapter = ArrayAdapter.createFromResource(this, arrayResourceId, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
    }

    public void editProfileAndNavigateToMainApp() {
        if (userId == null || userId.isEmpty()) {
            Log.e("ProfileEditor", "User ID is null or empty. Cannot update profile.");
            return;
        }

        try {
            String about = txtAbout.getText().toString();
            String birthDate = pkrDay.getValue() + "/" + pkrMonth.getValue() + "/" + pkrYear.getValue();
            int height = pkrHeight.getValue();
            int weight = pkrWeight.getValue();
            String ethnicity = String.valueOf(spnrEthnicity.getSelectedItemPosition());
            String relationship = String.valueOf(spnrRelationship.getSelectedItemPosition());
            String religion = String.valueOf(spnrReligion.getSelectedItemPosition());
            String reference = String.valueOf(spnrReference.getSelectedItemPosition());
            String orientation = String.valueOf(spnrOrientation.getSelectedItemPosition());
            String stdS = String.valueOf(spnrSTDs.getSelectedItemPosition());
            String role = String.valueOf(spnrRole.getSelectedItemPosition());

            UserInfo userInfo = new UserInfo(
                    userId,
                    about,
                    birthDate,
                    height,
                    weight,
                    relationship,
                    religion,
                    orientation,
                    role,
                    ethnicity,
                    reference,
                    stdS);

            updateProfileAndNavigate(userInfo);
        } catch (Exception e) {
            Log.e("ProfileEditor", "Error in editProfileAndNavigateToMainApp", e);
        }
    }

 private void updateProfileAndNavigate(UserInfo userInfo) {
     executor.execute(() -> {
         if (userInfo != null) {
             try {
                 // Сохраняем данные в Firestore
                 DocumentReference userRef = db.collection("users").document(userInfo.getUserId());

                 userRef.set(userInfo)
                     .addOnSuccessListener(aVoid -> {
                         // После успешного обновления профиля, переходим в MainAppActivity
                         runOnUiThread(() -> {
                             Intent intent = new Intent(ProfileEditorActivity.this, MainAppActivity.class);
                             startActivity(intent);
                             finish(); // Закрываем текущую активность
                         });
                     })
                     .addOnFailureListener(e -> {
                         Log.e("ProfileEditor", "Error updating profile in Firestore", e);
                     });
             } catch (Exception e) {
                 Log.e("ProfileEditor", "Error updating profile", e);
             }
         }
     });
  }

  @Override
  protected void onStop() {
      super.onStop();
  }

  @Override
  protected void onDestroy() {
      super.onDestroy();
  }

    public void editProfile(View view) {
    }
}
