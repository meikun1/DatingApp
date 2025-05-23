package com.example.DatingApp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.DatingApp.MainAppFragments.FavouritesFragment;
import com.example.DatingApp.MainAppFragments.MessagesFragment;
import com.example.DatingApp.MainAppFragments.OnlineFragment;
import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;
import java.util.Random;


public class MainAppActivity extends AppCompatActivity {

	public static final String TAG = "MainApp";
	public static final int REQUEST_CODE_LAST_LOCATION = 123;
	private static final String[] TEST_EMAILS = {
			"test1@mail.com", "test2@mail.com", "test3@mail.com", "test4@mail.com", "test5@mail.com",
			"test6@mail.com", "test7@mail.com", "test8@mail.com", "test9@mail.com", "test10@mail.com"
	};
	private static final String TEST_PASSWORD = "qwertyuiop";
	private static final long AUTH_RETRY_DELAY_MS = 3000; // Задержка 3 секунды

	private Fragment selectedFragment;
	private final Activity mainActivity = this;
	private Location thisLocation;
	private LocationRequest locationRequest;
	private LocationCallback locationCallback;
	private boolean shouldStartLocationUpdates = false;
	private FusedLocationProviderClient fusedLocationProviderClient;
	private MyDBService myService;
	private MyDBService.MyLocalBinder binder;
	private Boolean isBound;
	private ServiceConnection serviceConnection;
	private ProgressBar progressBar;
	private TextView progressLbl;
	private FirebaseAuth mAuth;
	private final Handler authHandler = new Handler();

	private void switchFragment(Fragment fragment) {
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.mainFrame, fragment)
				.addToBackStack(null)
				.commit();
	}

	private final BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
		@SuppressLint("NonConstantResourceId")
		@Override
		public boolean onNavigationItemSelected(@NonNull MenuItem item) {
			switch (item.getItemId()) {
				case R.id.nav_home:
					setTitle(R.string.online);
					selectedFragment = new OnlineFragment();
					if (!shouldStartLocationUpdates) {
						startLocationUpdates();
						shouldStartLocationUpdates = true;
					}
					break;

				case R.id.nav_chat:
					setTitle(R.string.messages);
					selectedFragment = new MessagesFragment();
					if (shouldStartLocationUpdates) {
						stopLocationUpdates();
						shouldStartLocationUpdates = false;
					}
					break;

				case R.id.nav_favs:
					setTitle(R.string.favourites);
					selectedFragment = new FavouritesFragment();
					if (shouldStartLocationUpdates) {
						stopLocationUpdates();
						shouldStartLocationUpdates = false;
					}
					break;
			}

			if (selectedFragment != null) {
				Bundle bundle = new Bundle();
				bundle.putBinder("binder", binder);
				selectedFragment.setArguments(bundle);
				switchFragment(selectedFragment);
			}

			return true;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_app);

		// Инициализация Firebase Auth
		mAuth = FirebaseAuth.getInstance();

		// Привязка к сервису
		serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				binder = (MyDBService.MyLocalBinder) service;
				myService = binder.getService();
				isBound = true;
				// Авторизуем тестового пользователя
				authenticateTestUser(0);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				isBound = false;
			}
		};

		Intent intent = new Intent(MainAppActivity.this, MyDBService.class);
		bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

		// Настройка LocationCallback для обновлений местоположения
		locationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(@NonNull LocationResult locationResult) {
				List<Location> locationList = locationResult.getLocations();
				if (!locationList.isEmpty()) {
					Location location = locationList.get(locationList.size() - 1);
					Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
					thisLocation = location;
					SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
					String id = sharedPreferences.getString("firestore_uid_db", null);
					sharedPreferences.edit().putString("location", thisLocation.getLatitude() + "," + thisLocation.getLongitude()).apply();
					// Обновляем местоположение в базе данных
					if (isBound && myService != null && mAuth.getCurrentUser() != null) {
						myService.updateLocationFieldInUsersTokens("latlng", new GeoPoint(location.getLatitude(), location.getLongitude()));
						// Запускаем UpdateDBField только после авторизации и получения местоположения
						new UpdateDBField().execute(thisLocation);
					}
				}
				if (selectedFragment == null) {
					selectedFragment = new OnlineFragment();
					Bundle bundle = new Bundle();
					bundle.putBinder("binder", binder);
					selectedFragment.setArguments(bundle);
					switchFragment(selectedFragment);
				}
			}
		};

		BottomNavigationView navigation = findViewById(R.id.navigation);
		progressBar = findViewById(R.id.progressBarFragments);
		progressLbl = findViewById(R.id.progressLabel);
		navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
		getLocation();
	}

	private void authenticateTestUser(int attempt) {
		if (attempt >= TEST_EMAILS.length) {
			Log.e(TAG, "All authentication attempts failed. Please check Firebase configuration, user credentials, and reCAPTCHA settings.");
			return;
		}

		Random random = new Random();
		String email = TEST_EMAILS[random.nextInt(TEST_EMAILS.length)];
		Log.d(TAG, "Checking existence of: " + email);

		// Проверяем существование пользователя
		mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(fetchTask -> {
			if (fetchTask.isSuccessful()) {
				List<String> signInMethods = fetchTask.getResult().getSignInMethods();
				if (signInMethods != null && !signInMethods.isEmpty()) {
					// Пользователь существует, пытаемся войти
					Log.d(TAG, "User exists, attempting to sign in: " + email);
					mAuth.signInWithEmailAndPassword(email, TEST_PASSWORD)
							.addOnCompleteListener(this, signInTask -> {
								if (signInTask.isSuccessful()) {
									FirebaseUser user = mAuth.getCurrentUser();
									if (user != null) {
										SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
										sharedPreferences.edit().putString("firestore_uid_db", user.getUid()).apply();
										Log.d(TAG, "Test user authenticated: " + email + ", UID: " + user.getUid());
									} else {
										Log.e(TAG, "FirebaseUser is null after successful sign-in for " + email);
										if (!isFinishing()) {
											authHandler.postDelayed(() -> authenticateTestUser(attempt + 1), AUTH_RETRY_DELAY_MS);
										}
									}
								} else {
									Log.e(TAG, "Authentication failed for " + email + ": " + signInTask.getException().getMessage());
									if (!isFinishing()) {
										authHandler.postDelayed(() -> authenticateTestUser(attempt + 1), AUTH_RETRY_DELAY_MS);
									}
								}
							});
				} else {
					// Пользователь не существует, создаем нового
					Log.d(TAG, "User does not exist, creating: " + email);
					if (!isFinishing()) {
						mAuth.createUserWithEmailAndPassword(email, TEST_PASSWORD)
								.addOnCompleteListener(createTask -> {
									if (createTask.isSuccessful()) {
										FirebaseUser user = mAuth.getCurrentUser();
										if (user != null) {
											SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
											sharedPreferences.edit().putString("firestore_uid_db", user.getUid()).apply();
											Log.d(TAG, "Test user created and authenticated: " + email + ", UID: " + user.getUid());
										} else {
											Log.e(TAG, "FirebaseUser is null after successful user creation for " + email);
											authHandler.postDelayed(() -> authenticateTestUser(attempt + 1), AUTH_RETRY_DELAY_MS);
										}
									} else {
										Log.e(TAG, "Failed to create user " + email + ": " + createTask.getException().getMessage());
										authHandler.postDelayed(() -> authenticateTestUser(attempt + 1), AUTH_RETRY_DELAY_MS);
									}
								});
					}
				}
			} else {
				Log.e(TAG, "Failed to fetch sign-in methods for " + email + ": " + fetchTask.getException().getMessage());
				if (!isFinishing()) {
					authHandler.postDelayed(() -> authenticateTestUser(attempt + 1), AUTH_RETRY_DELAY_MS);
				}
			}
		});
	}

	// Метод для получения местоположения
	private void getLocation() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
				ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{
							Manifest.permission.ACCESS_COARSE_LOCATION,
							Manifest.permission.ACCESS_FINE_LOCATION
					},
					REQUEST_CODE_LAST_LOCATION);
		} else {
			createLocationRequest();
			startLocationUpdates();
		}
	}

	private void createLocationRequest() {
		if (locationRequest == null)
			locationRequest = LocationRequest.create();
		locationRequest.setInterval(5000);
		locationRequest.setFastestInterval(5000);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	@SuppressLint("MissingPermission")
	private void startLocationUpdates() {
		shouldStartLocationUpdates = true;
		fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
	}

	private void stopLocationUpdates() {
		shouldStartLocationUpdates = false;
		fusedLocationProviderClient.removeLocationUpdates(locationCallback);
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdates();
	}

	@Override
	public void onStop() {
		super.onStop();
		stopLocationUpdates();
		if (isBound) {
			unbindService(serviceConnection);
			isBound = false;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopLocationUpdates();
		// Разлогинивание пользователя
		mAuth.signOut();
		// Очистка SharedPreferences
		SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
		sharedPreferences.edit().remove("firestore_uid_db").apply();
		// Очистка Handler
		authHandler.removeCallbacksAndMessages(null);
		Log.d(TAG, "User signed out");
	}

	@Override
	public void onResume() {
		super.onResume();
		if (shouldStartLocationUpdates)
			startLocationUpdates();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == REQUEST_CODE_LAST_LOCATION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				getLocation();
			}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	public Activity getMainActivity() {
		return mainActivity;
	}

	@SuppressLint("StaticFieldLeak")
	public class UpdateDBField extends AsyncTask<Location, Integer, Void> {

		@Override
		protected void onPreExecute() {
			progressBar.setVisibility(View.VISIBLE);
			progressLbl.setVisibility(View.VISIBLE);
		}

		@SuppressLint("SetTextI18n")
		@Override
		protected void onProgressUpdate(Integer... ints) {
			switch (ints[0]) {
				case 1:
					progressLbl.setText("Getting location updates...");
					break;
				case 2:
					progressLbl.setText("Preparing users' info...");
					break;
				case 3:
					progressLbl.setText("Preparing nearby users...");
					break;
			}
		}

		@Override
		protected Void doInBackground(Location... locations) {
			publishProgress(1);
			Log.d(TAG, "doInBackground: published 1");
			updateLocationInDB(locations[0]);

			publishProgress(2);
			Log.d(TAG, "doInBackground: published 2");
			if (!defineCurrentUser()) {
				Log.e(TAG, "Aborting: Current user not defined");
				return null;
			}

			publishProgress(3);
			Log.d(TAG, "doInBackground: published 3");
			if (locations[0] != null) {
				defineNearbyUsers();
			} else {
				Log.e(TAG, "Aborting: Location not initialized");
			}
			return null;
		}

		private void updateLocationInDB(Location location) {
			if (location == null || myService == null || !isBound) {
				Log.e(TAG, "Cannot update location: location or service is null");
				return;
			}
			myService.updateLocationFieldInUsersTokens("latlng", new GeoPoint(location.getLatitude(), location.getLongitude()));
		}

		private boolean defineCurrentUser() {
			if (myService == null || !isBound) {
				Log.e(TAG, "Service not bound or null");
				return false;
			}
			User currentUser = myService.getCurrentUser();
			if (currentUser == null) {
				Log.e(TAG, "Current user is null");
				return false;
			}
			// Получаем email из FirebaseAuth
			FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
			String email = firebaseUser != null ? firebaseUser.getEmail() : "unknown";
			Log.d(TAG, "Current user defined: " + email);
			return true;
		}

		private void defineNearbyUsers() {
			if (myService == null || !isBound) {
				Log.e(TAG, "Service not bound or null");
				return;
			}
			myService.getNearbyUsersAsync(51, true, new MyDBService.NearbyUsersCallback() {
				@Override
				public void onNearbyUsersLoaded(List<User> users) {
					Log.d(TAG, "Nearby users loaded: " + users.size());
					// Передаем пользователей в OnlineFragment, если он активен
					if (selectedFragment instanceof OnlineFragment) {
						((OnlineFragment) selectedFragment).updateUsers(users);
					}
				}

				@Override
				public void onError(Exception e) {
					Log.e(TAG, "Failed to retrieve nearby users", e);
				}
			});
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			progressBar.setVisibility(View.GONE);
			progressLbl.setVisibility(View.GONE);
		}
	}
}