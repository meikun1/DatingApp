package com.example.DatingApp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.DatingApp.Adapters.ChatThreadsRecyclerViewAdapter;
import com.example.DatingApp.Chat.Message;
import com.example.DatingApp.Chat.Room;
import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.MyUser;
import com.example.DatingApp.Users.User;
import com.google.firebase.Timestamp;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.DatingApp.Connection.HttpRequest.httpPostFCM;

public class ChatActivity extends AppCompatActivity {

    public static final String TAG = "ChatActivity";
    public static final String ROOMS_DB = "rooms";
    public static final String MESSAGES_DB = "messages";
    public static final String USERS_TOKENS_DB = "users_tokens";
    public static final String USER_SP = "user";
    public static final String USER_UID_SP = "user_uid";

    private Button sendBtn;
    private ProgressBar progressBar;
    private EditText msg;
    private User userQueried;
    private MyUser otherUser;
    private RecyclerView listOfMessages;
    private String room, uid;
    private String message;
    private ChatThreadsRecyclerViewAdapter adapter;

    private MyDBService myService;
    private ServiceConnection serviceConnection;
    private boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Инициализация UI
        msg = findViewById(R.id.inptMessage);
        listOfMessages = findViewById(R.id.list_of_messages);
        progressBar = findViewById(R.id.progressBarSendMsg);
        sendBtn = findViewById(R.id.sendBtn);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listOfMessages.setLayoutManager(linearLayoutManager);
        adapter = new ChatThreadsRecyclerViewAdapter(null);
        listOfMessages.setAdapter(adapter);

        // Получение данных из Intent
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                otherUser = (MyUser) extras.getSerializable("other_user");
                room = extras.getString("room");
            }
        }

        // Получение UID текущего пользователя
        uid = getSharedPreferences(USER_SP, MODE_PRIVATE).getString(USER_UID_SP, null);
        if (uid == null) {
            Log.e(TAG, "User UID is null");
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Привязка к сервису
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MyDBService.MyLocalBinder binder = (MyDBService.MyLocalBinder) service;
                myService = binder.getService();
                isBound = true;
                // Загружаем данные чата
                loadChatInfo();
                // Загружаем информацию о пользователе
                if (otherUser != null) {
                    loadUserInfo(otherUser.getUid());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        Intent intent = new Intent(ChatActivity.this, MyDBService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void loadChatInfo() {
        if (myService == null || !isBound) {
            Log.e(TAG, "Service not bound");
            Toast.makeText(this, "Ошибка: сервис не доступен", Toast.LENGTH_SHORT).show();
            return;
        }

        if (room != null) {
            myService.getRoom(room, new MyDBService.RoomCallback() {
                @Override
                public void onRoomLoaded(Room roomObj) {
                    List<Message> messages = roomObj.getMessages();
                    setIsItMeByToFrom(messages);
                    runOnUiThread(() -> {
                        adapter.setMessages(messages);
                        adapter.notifyDataSetChanged();
                        listOfMessages.scrollToPosition(messages != null ? messages.size() - 1 : 0);
                        Log.d(TAG, "Room loaded with " + (messages != null ? messages.size() : 0) + " messages");
                    });
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading room", e);
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Ошибка загрузки чата", Toast.LENGTH_SHORT).show());
                }
            });
        } else if (otherUser != null) {
            // Проверяем, существует ли комната для этого пользователя
            myService.checkIfRoomExistsAsync(otherUser.getUid(), new MyDBService.OnRoomCheckListener() {
                @Override
                public void onRoomFound(String roomId) {
                    room = roomId;
                    loadChatInfo(); // Повторно загружаем информацию о комнате
                }

                @Override
                public void onRoomNotFound() {
                    Log.d(TAG, "No room exists for user " + otherUser.getUid());
                    // Комната будет создана при отправке первого сообщения
                }
            });
        } else {
            Log.e(TAG, "Other user is null");
            Toast.makeText(this, "Ошибка: пользователь не выбран", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserInfo(String userUid) {
        if (myService == null || !isBound || userUid == null) {
            Log.e(TAG, "Service not bound or user UID is null");
            return;
        }

        myService.getUserByUID(userUid, new MyDBService.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(User user) {
                userQueried = user;
                Log.d(TAG, "User loaded: " + user.getName());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading user info", e);
            }
        });
    }

    public void sendMsg(View view) {
        Log.d(TAG, "sendMsg: entered");

        message = msg.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Введите сообщение", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otherUser == null || otherUser.getToken() == null) {
            Log.e(TAG, "Other user or token is null");
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создание JSON для FCM
        try {
            JSONObject jsonObj = new JSONObject();
            JSONObject jsonData = new JSONObject();
            jsonData.put("title", "New message");
            jsonData.put("body", message);
            jsonObj.put("data", jsonData);
            jsonObj.put("to", otherUser.getToken());

            sendMessageAsync(jsonObj.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for FCM", e);
            Toast.makeText(this, "Ошибка отправки уведомления", Toast.LENGTH_SHORT).show();
        }

        msg.setText(""); // Очистка поля ввода
    }

    private void sendMessageAsync(String json) {
        progressBar.setVisibility(View.VISIBLE);
        sendBtn.setVisibility(View.GONE);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // Отправка FCM уведомления
            String url = "https://fcm.googleapis.com/fcm/send";
            try {
                httpPostFCM(url, json);
            } catch (IOException e) {
                Log.e(TAG, "Error sending FCM message", e);
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Ошибка отправки уведомления", Toast.LENGTH_SHORT).show());
            }

            // Создание нового сообщения
            Message messageObj = new Message(message, Timestamp.now(), Arrays.asList(otherUser.getUid(), uid));

            if (room != null) {
                // Добавляем сообщение в существующую комнату
                myService.addMessageToRoom(room, messageObj);
                myService.getRoom(room, new MyDBService.RoomCallback() {
                    @Override
                    public void onRoomLoaded(Room roomObj) {
                        List<Message> messages = roomObj.getMessages();
                        setIsItMeByToFrom(messages);
                        runOnUiThread(() -> {
                            adapter.setMessages(messages);
                            adapter.notifyDataSetChanged();
                            listOfMessages.scrollToPosition(messages != null ? messages.size() - 1 : 0);
                            progressBar.setVisibility(View.GONE);
                            sendBtn.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error loading room after adding message", e);
                        runOnUiThread(() -> {
                            Toast.makeText(ChatActivity.this, "Ошибка загрузки чата", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            sendBtn.setVisibility(View.VISIBLE);
                        });
                    }
                });
            } else if (otherUser != null && otherUser.getDocID() != null) {
                // Создаем новую комнату и добавляем сообщение
                myService.addNewRoomAndMessage(otherUser.getDocID(), otherUser.getUid(), messageObj, new MyDBService.OnRoomCheckListener() {
                    @Override
                    public void onRoomFound(String roomId) {
                        room = roomId;
                        myService.getRoom(roomId, new MyDBService.RoomCallback() {
                            @Override
                            public void onRoomLoaded(Room roomObj) {
                                List<Message> messages = roomObj.getMessages();
                                setIsItMeByToFrom(messages);
                                runOnUiThread(() -> {
                                    adapter.setMessages(messages);
                                    adapter.notifyDataSetChanged();
                                    listOfMessages.scrollToPosition(messages != null ? messages.size() - 1 : 0);
                                    progressBar.setVisibility(View.GONE);
                                    sendBtn.setVisibility(View.VISIBLE);
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error loading new room", e);
                                runOnUiThread(() -> {
                                    Toast.makeText(ChatActivity.this, "Ошибка создания чата", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                    sendBtn.setVisibility(View.VISIBLE);
                                });
                            }
                        });
                    }

                    @Override
                    public void onRoomNotFound() {
                        Log.e(TAG, "Failed to create new room");
                        runOnUiThread(() -> {
                            Toast.makeText(ChatActivity.this, "Ошибка создания чата", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            sendBtn.setVisibility(View.VISIBLE);
                        });
                    }
                });
            } else {
                Log.e(TAG, "Other user or docID is null");
                runOnUiThread(() -> {
                    Toast.makeText(ChatActivity.this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    sendBtn.setVisibility(View.VISIBLE);
                });
            }
        });
        executor.shutdown();
    }

    private void setIsItMeByToFrom(List<Message> messages) {
        if (messages == null) return;
        for (Message msg : messages) {
            if (msg.getTofrom() != null && !msg.getTofrom().isEmpty()) {
                msg.setIsItMe(msg.getTofrom().get(0).equals(uid));
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}