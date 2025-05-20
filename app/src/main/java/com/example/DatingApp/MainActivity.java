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
import java.util.ArrayList;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.FirebaseTooManyRequestsException;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

	public static final String TAG = "MainApp";
	public static final int REQUEST_CODE_LAST_LOCATION = 123;
	private static final int MAX_AUTH_ATTEMPTS = 3;

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
	private int authAttempts = 0;
	private boolean isUserAuthenticated = false;

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
				Log.d(TAG, "Service connected");
				// Авторизация начинается после подключения сервиса
				authenticateTestUser();
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				isBound = false;
			}
		};

		Intent intent = new Intent(MainActivity.this, MyDBService.class);
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
					// Обновляем местоположение в базе данных только если пользователь авторизован
					if (isBound && myService != null && isUserAuthenticated) {
						myService.updateLocationFieldInUsersTokens("latlng", new GeoPoint(location.getLatitude(), location.getLongitude()));
						new UpdateDBField().execute(thisLocation);
					}
				}
				if (selectedFragment == null && isUserAuthenticated) {
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

	private void authenticateTestUser() {
		if (authAttempts >= MAX_AUTH_ATTEMPTS) {
			Log.e(TAG, "Maximum authentication attempts reached");
			Toast.makeText(this, "Слишком много попыток входа. Попробуйте позже.", Toast.LENGTH_LONG).show();
			return;
		}

		// Список тестовых пользователей
		String[] testEmails = {
				"test1@mail.com", "test2@mail.com", "test3@mail.com", "test4@mail.com", "test5@mail.com",
				"test6@mail.com", "test7@mail.com", "test8@mail.com", "test9@mail.com", "test10@mail.com"
		};
		String testPassword = "qwertyuiop";

		// Выбор случайного пользователя
		Random random = new Random();
		String email = testEmails[random.nextInt(testEmails.length)];
		authAttempts++;

		Log.d(TAG, "Attempting to authenticate user: " + email + " (Attempt " + authAttempts + ")");

		// Авторизация через Firebase
		mAuth.signInWithEmailAndPassword(email, testPassword)
				.addOnCompleteListener(this, task -> {
					if (task.isSuccessful()) {
						FirebaseUser user = mAuth.getCurrentUser();
						if (user != null) {
							// Сохраняем UID в SharedPreferences
							SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
							sharedPreferences.edit().putString("firestore_uid_db", user.getUid()).apply();
							Log.d(TAG, "Test user authenticated: " + email + ", UID: " + user.getUid());
							isUserAuthenticated = true;
							// Загружаем текущего пользователя в MyDBService
							if (isBound && myService != null) {
								myService.getUserByUID(user.getUid(), new MyDBService.OnUserLoadedListener() {
									@Override
									public void onUserLoaded(User user) {
										Log.d(TAG, "Current user loaded in service: " + user.getUserId());
										// Инициируем обновление данных после авторизации
										if (selectedFragment instanceof OnlineFragment) {
											((OnlineFragment) selectedFragment).updateUsers(new ArrayList<>());
										}
									}

									@Override
									public void onError(Exception e) {
										Log.e(TAG, "Failed to load current user in service", e);
									}
								});
							}
							// Запускаем обновления местоположения после авторизации
							if (!shouldStartLocationUpdates) {
								startLocationUpdates();
								shouldStartLocationUpdates = true;
							}
						}
					} else {
						Log.e(TAG, "Authentication failed for " + email, task.getException());
						if (task.getException() instanceof FirebaseTooManyRequestsException) {
							Toast.makeText(this, "Слишком много запросов. Попробуйте позже.", Toast.LENGTH_LONG).show();
							Log.e(TAG, "Firebase blocked requests due to too many attempts");
						} else if (!isFinishing() && authAttempts < MAX_AUTH_ATTEMPTS) {
							new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
									this::authenticateTestUser, 1000);
						} else {
							Toast.makeText(this, "Не удалось войти. Попробуйте позже.", Toast.LENGTH_LONG).show();
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
			if (isUserAuthenticated) {
				startLocationUpdates();
			}
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
		if (mAuth.getCurrentUser() != null) {
			mAuth.signOut();
			Log.d(TAG, "User signed out");
		}
		// Очистка SharedPreferences
		SharedPreferences sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
		sharedPreferences.edit().remove("firestore_uid_db").apply();
		Log.d(TAG, "SharedPreferences cleared");
	}

	@Override
	public void onResume() {
		super.onResume();
		if (shouldStartLocationUpdates && isUserAuthenticated) {
			startLocationUpdates();
		}
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
			FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
			String email = firebaseUser != null ? firebaseUser.getEmail() : "unknown";
			Log.d(TAG, "Current user defined: " + email + ", UserId: " + currentUser.getUserId());
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