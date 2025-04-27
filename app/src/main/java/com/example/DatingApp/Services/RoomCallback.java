package com.example.DatingApp.Services;

import com.example.DatingApp.Chat.Room;

public interface RoomCallback {
    void onRoomLoaded(Room room);
    void onError(Exception e);
}
