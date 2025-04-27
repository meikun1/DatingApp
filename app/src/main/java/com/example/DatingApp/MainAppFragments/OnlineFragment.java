package com.example.DatingApp.MainAppFragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Adapters.OnlineRecyclerViewAdapterBig;
import com.example.DatingApp.R;
import com.example.DatingApp.Users.User;

public class OnlineFragment extends Fragment {
    private static final String TAG = "OnlineActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBarOnline;
    private RecyclerView recyclerView;
    private View view;
    private List<User> nearbyUsers = new ArrayList<>();

    public OnlineFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_online, container, false);

        recyclerView = view.findViewById(R.id.recyclerv_view);
        progressBarOnline = view.findViewById(R.id.progressBarOnline);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);

        swipeRefreshLayout.setOnRefreshListener(this::refreshNearbyUsers);

        loadUsersFromFirestore(); // Загружаем пользователей при открытии фрагмента

        return view;
    }

    private void loadUsersFromFirestore() {
        progressBarOnline.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    nearbyUsers.clear();
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    String currentUserId = (currentUser != null) ? currentUser.getUid() : "";

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            String userId = user.getUid();
                            if (userId != null && !userId.equals(currentUserId)) {
                                nearbyUsers.add(user);
                            }
                        }
                    }
                    initRecyclerView(nearbyUsers);
                    progressBarOnline.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load users", e);
                    Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
                    progressBarOnline.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void refreshNearbyUsers() {
        loadUsersFromFirestore();
    }

    private void initRecyclerView(List<User> users) {
        OnlineRecyclerViewAdapterBig adapterOnline = new OnlineRecyclerViewAdapterBig(getContext(), users);
        recyclerView.setAdapter(adapterOnline);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FirebaseAuth.getInstance().signOut();
        Log.d(TAG, "Signed out current user");
    }
}
