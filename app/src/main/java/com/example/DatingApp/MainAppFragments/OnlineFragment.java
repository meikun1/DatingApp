package com.example.DatingApp.MainAppFragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.DatingApp.Adapters.OnlineRecyclerViewAdapterBig;
import com.example.DatingApp.R;
import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class OnlineFragment extends Fragment {
    private static final String TAG = "OnlineFragment";

    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBarOnline;
    private RecyclerView recyclerView;
    private TextView noUsersTextView;
    private List<User> nearbyUsers = new ArrayList<>();
    private MyDBService myService;
    private boolean isServiceBound = false;
    private FirebaseAuth mAuth;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyDBService.MyLocalBinder binder = (MyDBService.MyLocalBinder) service;
            myService = binder.getService();
            isServiceBound = true;
            Log.d(TAG, "Service connected");
            // Проверяем авторизацию перед загрузкой пользователей
            checkAuthAndLoadUsers();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            myService = null;
            Log.d(TAG, "Service disconnected");
        }
    };

    public OnlineFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_online, container, false);

        recyclerView = view.findViewById(R.id.recyclerv_view);
        progressBarOnline = view.findViewById(R.id.progressBarOnline);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        noUsersTextView = view.findViewById(R.id.noUsersLbl);

        // Инициализация FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Привязка сервиса
        Intent intent = new Intent(requireContext(), MyDBService.class);
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        swipeRefreshLayout.setOnRefreshListener(this::refreshNearbyUsers);

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            requireContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    public void updateUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            requireActivity().runOnUiThread(() -> updateUI(true));
            return;
        }

        nearbyUsers.clear();
        nearbyUsers.addAll(users);
        requireActivity().runOnUiThread(() -> updateUI(false));
    }

    private void checkAuthAndLoadUsers() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "No authenticated user, delaying loadUsers");
            // Пробуем снова через 1 секунду, если пользователь ещё не авторизован
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::checkAuthAndLoadUsers, 1000);
        } else {
            Log.d(TAG, "User authenticated: " + currentUser.getUid());
            loadUsers();
        }
    }

    private void loadUsers() {
        if (!isServiceBound || myService == null) {
            Log.e(TAG, "MyService is not bound");
            Toast.makeText(requireContext(), "Ошибка: сервис недоступен", Toast.LENGTH_SHORT).show();
            progressBarOnline.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            showNoUsers();
            return;
        }

        progressBarOnline.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noUsersTextView.setVisibility(View.GONE);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null");
            Toast.makeText(requireContext(), "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show();
            progressBarOnline.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            showNoUsers();
            return;
        }

        myService.getNearbyUsersAsync(51, true, new MyDBService.NearbyUsersCallback() {
            @Override
            public void onNearbyUsersLoaded(List<User> users) {
                nearbyUsers.clear();
                for (User user : users) {
                    if (user != null && user.getUserId() != null && !user.getUserId().equals(currentUserId)) {
                        nearbyUsers.add(user);
                    }
                }
                requireActivity().runOnUiThread(() -> updateUI(nearbyUsers.isEmpty()));
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to load nearby users", e);
                requireActivity().runOnUiThread(() -> updateUI(true));
            }
        });
    }

    private void refreshNearbyUsers() {
        checkAuthAndLoadUsers();
    }

    private void updateUI(boolean isErrorOrEmpty) {
        progressBarOnline.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        if (isErrorOrEmpty) {
            showNoUsers();
            Toast.makeText(requireContext(), nearbyUsers.isEmpty() ? "Нет пользователей онлайн" : "Не удалось загрузить пользователей", Toast.LENGTH_SHORT).show();
        } else {
            initRecyclerView(nearbyUsers);
        }
    }

    private void initRecyclerView(List<User> users) {
        OnlineRecyclerViewAdapterBig adapterOnline = new OnlineRecyclerViewAdapterBig(requireContext(), users);
        recyclerView.setAdapter(adapterOnline);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.setVisibility(View.VISIBLE);
        noUsersTextView.setVisibility(View.GONE);
    }

    private void showNoUsers() {
        recyclerView.setVisibility(View.GONE);
        noUsersTextView.setVisibility(View.VISIBLE);
    }
}