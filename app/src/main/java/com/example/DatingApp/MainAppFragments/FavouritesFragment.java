package com.example.DatingApp.MainAppFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.DatingApp.Adapters.OnlineRecyclerViewAdapterBig;
import com.example.DatingApp.R;
import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.User;

import java.util.List;

public class FavouritesFragment extends Fragment {

    private User user;
    private MyDBService myService;
    private RecyclerView recyclerView;
    private TextView txtView;
    private ProgressBar progressBar;
    private Boolean carryOn;
    private List<String> favs;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        carryOn = false;

        // Use a ViewModel for asynchronous work instead of AsyncTask
        FavouritesViewModel viewModel = new ViewModelProvider(this).get(FavouritesViewModel.class);

        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            this.user = user;
            carryOn = true;
            viewModel.loadUsersFromDB(user.getFavs());
        });

        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            if (users == null || users.isEmpty()) {
                txtView.setVisibility(View.VISIBLE);
            } else {
                initRecyclerView(users);
                recyclerView.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.GONE);
        });

        View view = inflater.inflate(R.layout.fragment_favourites, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        txtView = view.findViewById(R.id.noFavsLbl);
        progressBar = view.findViewById(R.id.progress_bar);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            MyDBService.MyLocalBinder binder = (MyDBService.MyLocalBinder) bundle.getBinder("binder");
            myService = binder.getService();
        }

        return view;
    }

    private void initRecyclerView(List<User> users) {
        OnlineRecyclerViewAdapterBig adapterOnline = new OnlineRecyclerViewAdapterBig(getContext(), users);
        recyclerView.setAdapter(adapterOnline);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
        recyclerView.setLayoutManager(gridLayoutManager);
    }
}
