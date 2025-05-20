package com.example.DatingApp.MainAppFragments;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.User;

import java.util.List;

public class FavouritesViewModel extends AndroidViewModel {

    private MyDBService myService;
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private MutableLiveData<List<User>> users = new MutableLiveData<>();

    public FavouritesViewModel(Application application) {
        super(application);
        // Initialize MyDBService here or inject via DI if needed
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<User>> getUsers() {
        return users;
    }

    public void loadUsersFromDB(List<String> favs) {
        if (myService == null) return;

        if (favs != null && !favs.isEmpty()) {
            myService.getUsersByFIREAsync(favs, new MyDBService.NearbyUsersCallback() {
                @Override
                public void onNearbyUsersLoaded(List<User> userList) {
                    users.postValue(userList);
                }

                @Override
                public void onError(Exception e) {
                    users.postValue(null);
                }
            });
        } else {
            users.setValue(null);
        }
    }


    public void setCurrentUser(User user) {
        currentUser.setValue(user);
    }

    // Set MyDBService through setter or dependency injection
    public void setMyService(MyDBService service) {
        this.myService = service;
    }
}
