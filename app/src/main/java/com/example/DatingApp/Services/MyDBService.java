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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.GeoPoint;

import com.example.DatingApp.Chat.Message;
import com.example.DatingApp.Chat.Room;
import com.example.DatingApp.Users.User;
import com.example.DatingApp.Users.UserInfo;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class MyDBService extends Service {

    private static final String PREFS_USER = "prefs_user";
    private static final String TAG = "GeoQuery";
    private static final String USER_SP = "user";
    public static final String USERS_TOKENS_DB = "users"; // Исправлено на правильную коллекцию
    public static final String FIRESTORE_UID_SP = "firestore_uid_db";
    public static final String ROOMS_DB = "rooms";
    public static final String MESSAGES_DB = "messages";
    public static final String LATLNG_DB = "latlng";
    public static final String KILOMETERS = "K";
    public static final String NAUTICAL_MILES = "N";
    public static final String MILES = "M";
    public static final String UID_DB = "userId"; // Исправлено на userId
    public static final String INFO_DB = "info";
    public static final String FAVS_DB = "favs";
    public static final int NEW_DEVICE = 1;
    public static final int NORMAL_STATE = 10;
    public static final int NEW_USER = 2;
    private static final String ACTION_UPDATE_USER_LOCATION = "com.example.myapplication.action.UPDATE_USER_LOCATION";

    private Context context;
    private FirebaseFirestore db;
    private SharedPreferences sp;
    private User currentUser;
    private FirebaseUser currentFirebaseUser;
    private String uid, firestore;
    private List<User> nearbyUsers;
    private List<Room> rooms;
    private final IBinder myBinder = new MyLocalBinder();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
        void onError(Exception e);
    }

    public interface NearbyUsersCallback {
        void onNearbyUsersLoaded(List<User> users);
        void onError(Exception e);
    }

    public interface OnRoomsLoadedListener {
        void onRoomsLoaded(List<Room> rooms);
        void onError(Exception e);
    }

    public interface OnRoomCheckListener {
        void onRoomFound(String roomId);
        void onRoomNotFound();
    }

    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<Message> messages);
        void onError(Exception e);
    }

    public interface RoomCallback {
        void onRoomLoaded(Room room);
        void onError(Exception e);
    }

    public MyDBService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        db = FirebaseFirestore.getInstance();
        sp = getSharedPreferences(USER_SP, MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentFirebaseUser != null) {
            createUserDocumentIfNotExists(currentFirebaseUser);
        } else {
            Log.e(TAG, "No authenticated user");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private void createUserDocumentIfNotExists(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            Log.e(TAG, "FirebaseUser is null");
            return;
        }

        String userId = firebaseUser.getUid();
        DocumentReference userDocRef = db.collection(USERS_TOKENS_DB).document(userId);

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    // Создаём новый документ с полями, соответствующими Firestore
                    User newUser = new User();
                    newUser.setUserId(userId);
                    newUser.setName(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Anonymous");
                    newUser.setLatlng(new GeoPoint(37.4219983, -122.084));
                    newUser.setMessages(new ArrayList<>());
                    newUser.setFavs(new ArrayList<>());
                    newUser.setAbout("");
                    newUser.setBirthDate("1/1/1990");
                    newUser.setEthnicity("0");
                    newUser.setHeight(170);
                    newUser.setOrientation("0");
                    newUser.setPreference("0");
                    newUser.setRelationship("0");
                    newUser.setReligion("0");
                    newUser.setRole("0");
                    newUser.setStds("0");
                    newUser.setWeight(70);

                    userDocRef.set(newUser)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User document created for userId: " + userId);
                                sp.edit().putString(FIRESTORE_UID_SP, userId).apply();
                                firestore = userId;
                                currentUser = newUser;
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to create user document", e));
                } else {
                    User user = task.getResult().toObject(User.class);
                    if (user != null) {
                        user.setDocID(userId);
                        currentUser = user;
                        firestore = userId;
                        sp.edit().putString(FIRESTORE_UID_SP, userId).apply();
                        Log.d(TAG, "User document loaded for userId: " + userId);
                    } else {
                        Log.e(TAG, "Failed to parse user document");
                    }
                }
            } else {
                Log.e(TAG, "Error checking user document", task.getException());
            }
        });
    }

    public void getUserByUID(String uid, final OnUserLoadedListener listener) {
        if (uid == null || uid.isEmpty()) {
            listener.onError(new IllegalArgumentException("UID is null or empty"));
            return;
        }

        Log.d(TAG, "Fetching user with userId: " + uid);
        db.collection(USERS_TOKENS_DB)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.setDocID(documentSnapshot.getId());
                            listener.onUserLoaded(user);
                        } else {
                            listener.onError(new Exception("Failed to parse user"));
                        }
                    } else {
                        listener.onError(new Exception("User document does not exist"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user with userId: " + uid, e);
                    listener.onError(e);
                });
    }

    public void addMessageToRoom(String roomId, Message message) {
        if (roomId == null || message == null) {
            Log.e(TAG, "RoomId or message is null");
            return;
        }

        CollectionReference messagesRef = db.collection(ROOMS_DB).document(roomId).collection(MESSAGES_DB);
        messagesRef.add(message)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Message added: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding message", e));
    }

    public void addNewRoomAndMessage(String docID, String userId, Message message, final OnRoomCheckListener listener) {
        if (docID == null || userId == null || message == null) {
            Log.e(TAG, "Invalid parameters for new room");
            listener.onRoomNotFound();
            return;
        }

        DocumentReference roomRef = db.collection(ROOMS_DB).document(docID);
        CollectionReference messagesRef = roomRef.collection(MESSAGES_DB);

        messagesRef.add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message added to new room: " + documentReference.getId());
                    listener.onRoomFound(docID);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding message to new room", e);
                    listener.onRoomNotFound();
                });
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void getNearbyUsersAsync(int limit, boolean refresh, final NearbyUsersCallback listener) {
        Log.d(TAG, "Checking currentUser: " + (currentUser != null ? currentUser.getUserId() : "null"));
        if (currentUser == null || currentUser.getLatlng() == null) {
            Log.d(TAG, "Current user or location not initialized, attempting to load");
            if (currentFirebaseUser != null) {
                getUserByUID(currentFirebaseUser.getUid(), new OnUserLoadedListener() {
                    @Override
                    public void onUserLoaded(User user) {
                        currentUser = user;
                        if (currentUser.getLatlng() == null) {
                            currentUser.setLatlng(new GeoPoint(0.0, 0.0));
                        }
                        proceedWithNearbyUsers(limit, refresh, listener);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to load current user", e);
                        listener.onError(new IllegalStateException("Current user or location not initialized"));
                    }
                });
            } else {
                Log.e(TAG, "No authenticated user");
                listener.onError(new IllegalStateException("No authenticated user"));
                return;
            }
        } else {
            proceedWithNearbyUsers(limit, refresh, listener);
        }
    }

    private void proceedWithNearbyUsers(int limit, boolean refresh, NearbyUsersCallback listener) {
        Log.d(TAG, "Starting getNearbyUsersAsync with limit: " + limit);
        db.collection(USERS_TOKENS_DB)
                .limit(limit)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<User> nearbyUsers = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            User user = document.toObject(User.class);
                            if (user != null) {
                                user.setDocID(document.getId());
                                nearbyUsers.add(user);
                            }
                        }

                        executor.execute(() -> {
                            for (User user : nearbyUsers) {
                                if (user.getLatlng() != null) {
                                    user.setDistance(String.valueOf(distance(
                                            currentUser.getLatlng().getLatitude(),
                                            currentUser.getLatlng().getLongitude(),
                                            user.getLatlng().getLatitude(),
                                            user.getLatlng().getLongitude(),
                                            KILOMETERS
                                    )));
                                }
                            }
                            Collections.sort(nearbyUsers);
                            nearbyUsers.remove(currentUser);

                            new Handler(Looper.getMainLooper()).post(() -> {
                                this.nearbyUsers = nearbyUsers;
                                listener.onNearbyUsersLoaded(nearbyUsers);
                            });
                        });
                    } else {
                        Log.e(TAG, "Error fetching nearby users", task.getException());
                        listener.onError(task.getException());
                    }
                });
    }

    public void refreshNearbyUsersAsync(final NearbyUsersCallback listener) {
        getNearbyUsersAsync(51, true, listener);
    }

    public void getAllRoomsAsync(final OnRoomsLoadedListener listener) {
        if (currentUser == null) {
            Log.e(TAG, "Current user not initialized");
            listener.onError(new IllegalStateException("Current user not initialized"));
            return;
        }

        getAllRoomsAndMessages(currentUser).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                listener.onRoomsLoaded(rooms);
            } else {
                Log.e(TAG, "Error fetching rooms", task.getException());
                listener.onError(task.getException());
            }
        });
    }

    public void getRoom(String roomUID, RoomCallback callback) {
        if (roomUID == null || roomUID.isEmpty()) {
            Log.e(TAG, "Room UID is null or empty");
            callback.onError(new IllegalArgumentException("Room UID is null or empty"));
            return;
        }

        db.collection(ROOMS_DB)
                .document(roomUID)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Room room = task.getResult().toObject(Room.class);
                        if (room != null) {
                            room.setRoomUID(task.getResult().getId());
                            callback.onRoomLoaded(room);
                        } else {
                            callback.onError(new Exception("Failed to parse room"));
                        }
                    } else {
                        Log.e(TAG, "Error fetching room: " + roomUID, task.getException());
                        callback.onError(task.getException());
                    }
                });
    }

    public void checkIfRoomExistsAsync(String otherUserId, final OnRoomCheckListener listener) {
        if (otherUserId == null || otherUserId.isEmpty()) {
            Log.e(TAG, "Other user ID is null or empty");
            listener.onRoomNotFound();
            return;
        }

        db.collection(ROOMS_DB)
                .whereArrayContains("tofrom", otherUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        String roomId = task.getResult().getDocuments().get(0).getId();
                        listener.onRoomFound(roomId);
                    } else {
                        listener.onRoomNotFound();
                    }
                });
    }

    public void addFavourites(User favUser) {
        if (favUser == null || favUser.getDocID() == null) {
            Log.e(TAG, "Favourite user or docID is null");
            return;
        }

        List<String> favs = currentUser.getFavs();
        if (favs == null) {
            favs = new ArrayList<>();
        }
        if (!favs.contains(favUser.getDocID())) {
            favs.add(favUser.getDocID());
            db.collection(USERS_TOKENS_DB)
                    .document(firestore)
                    .update(FAVS_DB, favs)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User added to favourites"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to add user to favourites", e));
        }
    }

    public void getUsersByFIREAsync(List<String> firestoreIDs, final NearbyUsersCallback listener) {
        if (firestoreIDs == null || firestoreIDs.isEmpty()) {
            Log.e(TAG, "Firestore IDs list is null or empty");
            listener.onError(new IllegalArgumentException("Firestore IDs list is null or empty"));
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String id : firestoreIDs) {
            tasks.add(db.collection(USERS_TOKENS_DB).document(id).get());
        }

        Tasks.whenAllSuccess(tasks).addOnCompleteListener(task -> {
            List<User> users = new ArrayList<>();
            for (Object result : task.getResult()) {
                DocumentSnapshot document = (DocumentSnapshot) result;
                User user = document.toObject(User.class);
                if (user != null) {
                    user.setDocID(document.getId());
                    users.add(user);
                }
            }
            listener.onNearbyUsersLoaded(users);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching users by Firestore IDs", e);
            listener.onError(e);
        });
    }

    public void addUser(User user) {
        if (user == null) {
            Log.e(TAG, "User is null");
            return;
        }

        currentUser = user;
        db.collection(USERS_TOKENS_DB)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    user.setDocID(documentReference.getId());
                    Log.d(TAG, "User added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error adding user", e));
    }

    public void updateFieldInUsersTokens(final Map<String, Object> map) {
        if (map == null || !map.containsKey(UID_DB)) {
            Log.e(TAG, "Map or userId is null");
            return;
        }

        uid = (String) map.get(UID_DB);
        db.collection(USERS_TOKENS_DB)
                .whereEqualTo(UID_DB, uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        firestore = task.getResult().getDocuments().get(0).getId();
                        sp.edit().putString(FIRESTORE_UID_SP, firestore).apply();
                        db.collection(USERS_TOKENS_DB)
                                .document(firestore)
                                .update(map)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Field updated"))
                                .addOnFailureListener(e -> Log.e(TAG, "Error updating field", e));
                    } else {
                        db.collection(USERS_TOKENS_DB)
                                .add(map)
                                .addOnSuccessListener(documentReference -> {
                                    firestore = documentReference.getId();
                                    sp.edit().putString(FIRESTORE_UID_SP, firestore).apply();
                                    Log.d(TAG, "New document added with ID: " + firestore);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error adding document", e));
                    }
                });
    }

    public void updateInfoFieldInUsersTokens(UserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null) {
            Log.e(TAG, "UserInfo or userId is null");
            return;
        }

        db.collection(USERS_TOKENS_DB)
                .document(userInfo.getUserId())
                .update(INFO_DB, userInfo)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User info updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user info", e));
    }

    public void updateLocationFieldInUsersTokens(final String field, final Object value) {
        if (firestore == null) {
            Log.e(TAG, "Firestore ID not initialized");
            return;
        }

        db.collection(USERS_TOKENS_DB)
                .document(firestore)
                .update(field, value)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Location updated");
                    getCurrentUserAsync(true, new OnUserLoadedListener() {
                        @Override
                        public void onUserLoaded(User user) {
                            currentUser = user;
                            Log.d(TAG, "Current user updated with new location");
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Failed to update current user after location update", e);
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating location", e));
    }

    public void getUsersFromRooms(List<Room> rooms, final NearbyUsersCallback listener) {
        if (rooms == null || rooms.isEmpty()) {
            Log.e(TAG, "No rooms found");
            listener.onError(new Exception("No rooms found"));
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (Room room : rooms) {
            if (room.getTofrom() != null) {
                for (String userId : room.getTofrom()) {
                    if (!userId.equals(currentUser.getUserId())) {
                        tasks.add(db.collection(USERS_TOKENS_DB).document(userId).get());
                    }
                }
            }
        }

        Tasks.whenAllSuccess(tasks).addOnCompleteListener(task -> {
            List<User> users = new ArrayList<>();
            for (Object result : task.getResult()) {
                DocumentSnapshot document = (DocumentSnapshot) result;
                User user = document.toObject(User.class);
                if (user != null) {
                    user.setDocID(document.getId());
                    users.add(user);
                }
            }
            listener.onNearbyUsersLoaded(users);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching users from rooms", e);
            listener.onError(e);
        });
    }

    private static int distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        dist = dist * 60 * 1.1515;
        if (unit.equals(KILOMETERS)) {
            dist = dist * 1.609344;
        } else if (unit.equals(NAUTICAL_MILES)) {
            dist = dist * 0.8684;
        }
        return (int) dist;
    }

    private void getFirestoreDocumentID() {
        if (currentFirebaseUser == null) {
            Log.e(TAG, "Current Firebase user is null");
            return;
        }

        String userId = currentFirebaseUser.getUid();
        firestore = sp.getString(FIRESTORE_UID_SP, null);

        if (firestore == null) {
            db.collection(USERS_TOKENS_DB)
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            firestore = userId;
                            sp.edit().putString(FIRESTORE_UID_SP, firestore).apply();
                            currentUser = task.getResult().toObject(User.class);
                            if (currentUser != null) {
                                currentUser.setDocID(firestore);
                                Log.d(TAG, "Current user initialized: " + currentUser.getUserId());
                            } else {
                                Log.e(TAG, "Failed to parse user document");
                            }
                        } else {
                            Log.d(TAG, "No Firestore document found, creating new one");
                            createUserDocumentIfNotExists(currentFirebaseUser);
                        }
                    });
        } else {
            getCurrentUserAsync(true, new OnUserLoadedListener() {
                @Override
                public void onUserLoaded(User user) {
                    currentUser = user;
                    Log.d(TAG, "Current user initialized: " + user.getUserId());
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading current user", e);
                }
            });
        }
    }

    private void getCurrentUserAsync(boolean isCurrentUser, final OnUserLoadedListener listener) {
        if (firestore == null) {
            Log.e(TAG, "Firestore ID not initialized");
            listener.onError(new IllegalStateException("Firestore ID not initialized"));
            return;
        }

        db.collection(USERS_TOKENS_DB)
                .document(firestore)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null) {
                            user.setDocID(task.getResult().getId());
                            if (isCurrentUser) {
                                currentUser = user;
                            }
                            listener.onUserLoaded(user);
                        } else {
                            listener.onError(new Exception("Failed to parse user"));
                        }
                    } else {
                        Log.e(TAG, "Error fetching user document", task.getException());
                        listener.onError(task.getException());
                    }
                });
    }

    private Task<Void> getAllRoomsAndMessages(User user) {
        if (user == null) {
            Log.e(TAG, "User is null");
            return Tasks.forException(new NullPointerException("User is null"));
        }

        rooms = new ArrayList<>();
        List<String> messagesUIDs = user.getMessages();

        if (messagesUIDs == null || messagesUIDs.isEmpty()) {
            return Tasks.forResult(null);
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (String roomId : messagesUIDs) {
            if (roomId != null) {
                tasks.add(db.collection(ROOMS_DB).document(roomId).get());
            }
        }

        return Tasks.whenAll(tasks)
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        for (Task<DocumentSnapshot> docTask : tasks) {
                            if (docTask.isSuccessful() && docTask.getResult().exists()) {
                                Room room = docTask.getResult().toObject(Room.class);
                                if (room != null) {
                                    room.setRoomUID(docTask.getResult().getId());
                                    rooms.add(room);
                                }
                            }
                        }
                    }
                    return Tasks.forResult(null);
                });
    }

    public void addMessagetoARoom(String roomUID, Message... messages) {
        if (roomUID == null || messages == null) {
            Log.e(TAG, "Room UID or messages are null");
            return;
        }

        getRoom(roomUID, new RoomCallback() {
            @Override
            public void onRoomLoaded(Room room) {
                List<Message> roomMessages = room.getMessages();
                if (roomMessages == null) {
                    roomMessages = new ArrayList<>();
                }
                Collections.addAll(roomMessages, messages);

                db.collection(ROOMS_DB)
                        .document(roomUID)
                        .update(MESSAGES_DB, roomMessages)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Messages updated in room"))
                        .addOnFailureListener(e -> Log.e(TAG, "Error updating messages", e));
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading room for messages", e);
            }
        });
    }

    public void getAllRoomsAndMessagesAsync(final OnRoomsLoadedListener listener) {
        getAllRoomsAsync(listener);
    }

    public void getChatList(String roomID, RoomCallback callback) {
        getRoom(roomID, callback);
    }

    public String getFirestore() {
        return firestore;
    }

    public class MyLocalBinder extends Binder {
        public MyDBService getService() {
            return MyDBService.this;
        }
    }
}