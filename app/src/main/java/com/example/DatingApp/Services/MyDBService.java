package com.example.DatingApp.Services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import androidx.annotation.NonNull;
import android.util.Log;
import android.content.Context;


import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

import com.example.DatingApp.Chat.Message;
import com.example.DatingApp.Chat.Room;
import com.example.DatingApp.Users.User;
import com.example.DatingApp.Users.UserInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class MyDBService extends Service {

    private static final String PREFS_USER = "prefs_user";

    private Context context;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static final String USERS_TOKENS_DB = "users_tokens";
    public static final String USER_SP = "user";
    public static final String FIRESTORE_UID_SP = "firestore_uid_db";
    public static final String ROOMS_DB = "rooms";
    public static final String MESSAGES_DB = "messages";
    public static final String LATLNG_DB = "latlng";

    public static final String KILOMETERS = "K";
    public static final String NAUTICAL_MILES = "N";
    public static final String MILES = "M";
    public static final String UID_DB = "uid";
    public static final int NEW_DEVICE = 1;
    public static final int NORMAL_STATE = 10;
    public static final int NEW_USER = 2;
    public static final String TAG = "GeoQuery";
    public static final String INFO_DB = "info";
    private static final String ACTION_UPDATE_USER_LOCATION = "com.example.myapplication.action.UPDATE_USER_LOCATION";
    public static final String FAVS_DB = "favs";
    private SharedPreferences sp;
    private int returnVal, index;
    private User currentUser, otherUser;
    private FirebaseUser currentFirebaseUser;
    private Message message;
    private List<Message> messages;
    private Room room;
    private List<Room> rooms;
    private String uid, firestore, roomUID, otherFirestore;
    private List<User> nearbyUsers;
    private Boolean shouldContinue, isComplete;
    private Boolean gotCurrentUser = false, gotNearbyUsers = false, gotAllRooms = false, gotRoom = false;
    private int userState;
    private IBinder myBinder = new MyLocalBinder();
    private Object tsk;
    private List<User> favUsersList;

    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<Message> messages);
        void onError(Exception e);
    }

    public MyDBService() {
        // ОБЯЗАТЕЛЬНО пустой конструктор
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this; // Теперь контекст — это сам сервис
        db = FirebaseFirestore.getInstance(); // И сразу инициализация Firestore
    }


    public void getUserByUID(String uid, final MyDBService.OnUserLoadedListener listener) {
        if (uid == null || uid.isEmpty()) {
            listener.onError(new IllegalArgumentException("UID is null or empty"));
            return;
        }

        // Запрос на получение документа
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            listener.onUserLoaded(user);
                        } else {
                            listener.onError(new Exception("Failed to parse user from document"));
                        }
                    } else {
                        listener.onError(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError(e);
                });
    }

    public void addMessageToRoom(String roomId, Message message) {
        // Получаем ссылку на коллекцию сообщений в комнате
        CollectionReference messagesRef = db.collection(ROOMS_DB).document(roomId).collection(MESSAGES_DB);

        // Добавляем сообщение в Firestore
        messagesRef.add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message added to room: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding message to room", e);
                });
    }
    public String addNewRoomAndMessage(String docID, String userUID, Message message) {
        // Создаём новую комнату
        DocumentReference roomRef = db.collection(ROOMS_DB).document(docID);

        // Создаем коллекцию сообщений внутри комнаты
        CollectionReference messagesRef = roomRef.collection(MESSAGES_DB);

        // Добавляем сообщение в коллекцию
        messagesRef.add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message added to new room: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding message to new room", e);
                });

        return docID; // Возвращаем ID комнаты
    }

    // Прочие методы для работы с пользователями, чатами и т.д.


    private static int distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            if (unit == KILOMETERS) {
                dist = dist * 1.609344;
            } else if (unit == NAUTICAL_MILES) {
                dist = dist * 0.8684;
            } else if (unit == MILES) {
                //return distance
            }

            return (int) dist;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sp = getSharedPreferences(USER_SP, MODE_PRIVATE);
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        getFirestoreDocumentID();

        return super.onStartCommand(intent, flags, startId);
    }
    // Пример метода для получения пользователей для всех комнат
    public void getUsersFromRooms(List<Room> rooms, final OnUsersLoadedListener listener) {
        // Проверка на null, чтобы избежать ошибок
        if (rooms == null || rooms.isEmpty()) {
            listener.onError(new Exception("No rooms found"));
            return;
        }

        // Создаем список пользователей
        List<User> users = new ArrayList<>();

        // Пример асинхронной загрузки пользователей (здесь может быть запрос к базе данных)
        // Имитация загрузки данных (замените этот код на ваш способ получения пользователей)
        for (Room room : rooms) {
            // Предполагаем, что у каждой комнаты есть информация о пользователе
            User user = new User();  // Замените на реальную логику для получения пользователей
            user.setName("User for room " + room.getRoomUID());  // Например, генерируем имя пользователя
            users.add(user);
        }

        // После завершения загрузки данных, вызываем listener для передачи данных
        listener.onUsersLoaded(users);  // Вызываем метод, передавая список пользователей
    }

    private void getFirestoreDocumentID() {
        SharedPreferences sp = context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE);
        String firestoreId = sp.getString(FIRESTORE_UID_SP, null);

        if (firestoreId == null) {
            if (currentFirebaseUser != null) {
                FirebaseFirestore.getInstance()
                        .collection(USERS_TOKENS_DB)
                        .whereEqualTo(UID_DB, currentFirebaseUser.getUid())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                                    firestore = task.getResult().getDocuments().get(0).getId();
                                    getCurrentUser(true);
                                }
                            }
                        });
                userState = NEW_DEVICE;
            } else {
                // Обработай ситуацию, когда currentFirebaseUser == null
                Log.e("MyDBService", "Firebase user is null.");
            }
        } else {
            firestore = firestoreId;
            getCurrentUser(true);
        }
    }


    public List<Room> getAllRooms() {
        gotAllRooms = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                getAllRoomsAndMessages(getCurrentUser());
            }
        }).start();
        while (!gotAllRooms) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return rooms;
    }

    public void getRoom(String roomUID, RoomCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("rooms")
                .document(roomUID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Room room = task.getResult().toObject(Room.class);
                        callback.onRoomLoaded(room);  // Вызываем коллбек с загруженной комнатой
                    } else {
                        callback.onError(task.getException());  // Вызываем коллбек с ошибкой
                    }
                });
    }


    // Интерфейс для обработки результата

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    public String getFirestore() {
        return firestore;
    }

    public void updateLocationFieldInUsersTokens(final String field, final Object value) {
        getFirestoreDocumentID();


        FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .whereEqualTo(UID_DB, uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                            firestore = task.getResult().getDocuments().get(0).getId();
                            sp.edit().putString(FIRESTORE_UID_SP, firestore).commit();
                            FirebaseFirestore.getInstance()
                                    .collection(USERS_TOKENS_DB)
                                    .document(firestore)
                                    .update(field, value);
                        }
                    }
                });
    }


    public void updateInfoFieldInUsersTokens(UserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null) {
            Log.e("MyDBService", "userInfo или userId равен null");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .document(userInfo.getUserId()) // Используем UID пользователя
                .update(INFO_DB, userInfo)
                .addOnSuccessListener(aVoid -> Log.d("MyDBService", "User info updated"))
                .addOnFailureListener(e -> Log.e("MyDBService", "Failed to update user info", e));
    }


    public void updateFieldInUsersTokens(final Map<String, Object> map) {
        getFirestoreDocumentID();

        uid = (String) map.get(UID_DB);
        FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .whereEqualTo(UID_DB, uid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                            firestore = task.getResult().getDocuments().get(0).getId();
                            sp.edit().putString(FIRESTORE_UID_SP, firestore).commit();
                            FirebaseFirestore.getInstance()
                                    .collection(USERS_TOKENS_DB)
                                    .document(firestore)
                                    .update(map);
                        } else {
                            FirebaseFirestore.getInstance()
                                    .collection(USERS_TOKENS_DB)
                                    .add(map)
                                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            firestore = task.getResult().getId();
                                            sp.edit().putString(FIRESTORE_UID_SP, firestore).commit();
                                        }
                                    });
                        }
                    }
                });
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public List<User> getNearbyUsers() {
        return nearbyUsers;
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(Exception e);
    }

    public interface OnUsersLoadedListener {
        void onUsersLoaded(List<User> users);
        void onError(Exception e);
    }

    public List<Room> getAllRoomsAndMessages() {
        List<Room> rooms = new ArrayList<>();

        try {
            // Здесь мы используем текущие методы вашего сервиса для получения данных о комнатах
            rooms = getAllRooms();  // Предположим, что этот метод существует и возвращает список комнат

            if (rooms != null) {
                // Перебираем комнаты и загружаем сообщения
                for (Room room : rooms) {
                    if (room != null && room.getMessages() == null) {
                        room.setMessages(new ArrayList<>());  // Создаем пустой список сообщений, если их нет
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading rooms and messages", e);
        }

        return rooms;
    }



    // Интерфейс для обработки результата





    public List<User> refreshNearbyUsers() {
        //TODO
        shouldContinue = false;
        getNearbyUsers(51, true);
        while (!shouldContinue) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return nearbyUsers;
    }

    public Room getChatList(String roomID) {
        Room room = null;
        if (rooms.contains(roomID)) {
            room = rooms.get(rooms.indexOf(roomID));
        }
        return room;
    }

    public void addUser(User user) {
        currentUser = user;
        FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .add(user);

    }

    public String checkIfRoomExists(String otherUserUID) {
        roomUID = null;
        FirebaseFirestore.getInstance()
                .collection(ROOMS_DB)
                .whereArrayContains("tofrom", otherUserUID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        roomUID = task.getResult().getDocuments().get(0).getId();
                    }
                });
        while (roomUID == null) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return roomUID;
    }

    public void addMessagetoARoom(String roomUID, Message... messages) {
        room = null;
        if (rooms != null) {
            for (Room r : rooms) {
                if (r.getRoomUID().equals(roomUID)) {
                    room = r;
                }
            }
        }

        for (Message msg : messages) {
            room.addMessage(msg);
        }
        List<Message> roomMsgs = room.getMessages();
        FirebaseFirestore.getInstance()
                .collection(ROOMS_DB)
                .document(roomUID)
                .update(MESSAGES_DB, roomMsgs);
    }
    public void addFavourites(User favUser){
        List<String> favs = currentUser.getFavs();
        if (favs == null){
            favs = new ArrayList<>();
        }
        favs.add(favUser.getDocID());
        FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .document(firestore)
                .update(FAVS_DB, favs);
    }
    
    public List<User> getUsersByFIRE(List<String> firestoreIDs){
        getUsers(firestoreIDs);
        while (!isComplete){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return favUsersList;
    }
    
    private void getUsers(final List<String> firestoreIDs){
        isComplete = false;
        favUsersList = new ArrayList<>();
        for (int i = 0; i < firestoreIDs.size() ; i++){
            index = i;
            FirebaseFirestore.getInstance()
                    .collection(USERS_TOKENS_DB)
                    .document(firestoreIDs.get(i))
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    favUsersList.add(task.getResult().toObject(User.class));
                    if (index == firestoreIDs.size()-1){
                        isComplete = true;
                    }
                }
            });
        }
    }

    private void getNearbyUsers(int limit, final Boolean refresh) {
        Task<QuerySnapshot> task = FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .limit(limit)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                User usr;
                nearbyUsers = new ArrayList<>();
                for (DocumentSnapshot o : task.getResult().getDocuments()) {
                    usr = o.toObject(User.class);
                    usr.setDocID(o.getId());
                    nearbyUsers.add(usr);
                }
                if (refresh) {
                    shouldContinue = true;
                }
                for (User user : nearbyUsers) {
                    user.setDistance(
                            String.valueOf(distance(currentUser.getLatlng().getLatitude(),
                                    currentUser.getLatlng().getLongitude(),
                                    user.getLatlng().getLatitude(),
                                    user.getLatlng().getLongitude(),
                                    KILOMETERS
                            ))
                    );
                }
                Collections.sort(nearbyUsers);
                nearbyUsers.remove(currentUser);
                gotNearbyUsers = true;
            }
        });
        while (!task.isComplete()){
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void getCurrentUser(Boolean isCurrentUser) {

        Task<DocumentSnapshot> firebaseTask = FirebaseFirestore.getInstance()
                .collection(USERS_TOKENS_DB)
                .document(firestore)
                .get();

        if (isCurrentUser) {
            firebaseTask.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        currentUser = task.getResult().toObject(User.class);
                        currentUser.setDocID(task.getResult().getId());
                        gotCurrentUser = true;
                        getNearbyUsers(50, false);
                        getAllRoomsAndMessages(currentUser);

                    } else {
//TODO do something when not successful
                    }
                }
            });
        } else {
            firebaseTask.addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        otherUser = task.getResult().toObject(User.class);
                        getAllRoomsAndMessages(currentUser);
                        getNearbyUsers(50, false);
                    } else {
//TODO do something when not successful
                    }
                }
            });
        }

    }

    private void updateFieldInRooms(String field, Object value, String collection, String roomUID) {
        FirebaseFirestore.getInstance()
                .collection(collection)
                .document(roomUID)
                .update(field, value);
    }

    private Task<QuerySnapshot> getAllRoomsAndMessages(User user) {
        gotAllRooms = false;
        gotRoom = false;
        rooms = new ArrayList<>();
        final List<String> messagesUIDs = user.getMessages();
        if (messagesUIDs.size() > 0) {
            for (int i = 0; i < messagesUIDs.size(); i++) {
                index = i;
                if (messagesUIDs.get(i) != null) {
                    tsk = FirebaseFirestore.getInstance()
                            .collection(ROOMS_DB)
                            .document(messagesUIDs.get(i))
                            .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                room = task.getResult().toObject(Room.class);
                                String roomID = task.getResult().getId();
                                room.setRoomUID(roomID);
                                rooms.add(room);
                                if (index == messagesUIDs.size()-1){
                                    gotAllRooms = true;
                                }
                            } else {
                                //TODO
                            }
                        }
                    });
                }
            }
        }
        return (Task<QuerySnapshot>)tsk;

    }

    private SharedPreferences getPreferences() {
        return context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE);
    }
    public class MyLocalBinder extends Binder {
        public MyDBService getService() {
            return MyDBService.this;
        }
    }
}
