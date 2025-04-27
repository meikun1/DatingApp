package com.example.DatingApp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
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


import com.example.DatingApp.Services.RoomCallback;

import com.example.DatingApp.Chat.Room;
import com.example.DatingApp.Adapters.ChatThreadsRecyclerViewAdapter;
import com.example.DatingApp.Chat.Message;
import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.MyUser;
import com.example.DatingApp.Users.User;
import com.google.firebase.Timestamp;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
    private String room, uid, newChatUserUID;
    private String message;

    private MyDBService myService;
    private ServiceConnection serviceConnection;
    private Boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind service to load chat data
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MyDBService.MyLocalBinder binder = (MyDBService.MyLocalBinder) service;
                myService = binder.getService();
                isBound = true;
                // Now that the service is bound, we can load chat data.
                loadChatInfo();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        Intent intent = new Intent(ChatActivity.this, MyDBService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                otherUser = (MyUser) extras.getSerializable("other_user");
                room = extras.getString("room");
            }
        }
        setContentView(R.layout.activity_chat);

        uid = getSharedPreferences(USER_SP, MODE_PRIVATE).getString(USER_UID_SP, null);
        msg = findViewById(R.id.inptMessage);
        listOfMessages = findViewById(R.id.list_of_messages);
        progressBar = findViewById(R.id.progressBarSendMsg);
        sendBtn = findViewById(R.id.sendBtn);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        listOfMessages.setLayoutManager(linearLayoutManager);

        // Load user info if available
        if (otherUser != null) {
            loadUserInfo(otherUser.getUid());
        }
    }


    private void loadChatInfo() {
        if (myService != null && room != null) {
            // Передаем идентификатор комнаты и коллбек
            myService.getRoom(room, new RoomCallback() {
                @Override
                public void onRoomLoaded(Room room) {
                    // Обрабатываем загруженную комнату
                    List<Message> msgs = room.getMessages();  // Получаем сообщения
                    setIsItMeByToFrom(msgs);  // Устанавливаем, кто из сообщений отправил
                    ChatThreadsRecyclerViewAdapter adapter = new ChatThreadsRecyclerViewAdapter(msgs);
                    listOfMessages.setAdapter(adapter);  // Устанавливаем адаптер для RecyclerView
                }

                @Override
                public void onError(Exception e) {
                    // Обрабатываем ошибку
                    Log.e(TAG, "Ошибка при загрузке комнаты", e);
                }
            });
        }
    }





    private void loadUserInfo(String uid) {
        if (myService != null) {
            myService.getUserByUID(uid, new MyDBService.OnUserLoadedListener() {
                @Override
                public void onUserLoaded(User user) {
                    userQueried = user;
                    // Update UI or perform further actions
                    Log.d(TAG, "User loaded: " + user.getName());
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading user info", e);
                }
            });
        }
    }

    public void sendMsg(View view) throws JSONException {
        Log.d(TAG, "sendMsg: entered");

        message = msg.getText().toString().trim();

        if (message.isEmpty()) {
            return;
        }

        String targetedToken = otherUser.getToken();
        String targetedName = "New message";
        JSONObject jsonObj = new JSONObject();
        JSONObject jsonData = new JSONObject();
        jsonData.put("title", targetedName);
        jsonData.put("body", message);
        jsonObj.put("data", jsonData);
        jsonObj.put("to", targetedToken);

        new SendMessageTask().execute(jsonObj.toString());

        msg.setText(""); // Clear the input field
    }

    private class SendMessageTask extends AsyncTask<String, Void, List<Message>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            sendBtn.setVisibility(View.GONE);
        }

        @Override
        protected List<Message> doInBackground(String... strings) {
            String url = "https://fcm.googleapis.com/fcm/send";
            try {
                httpPostFCM(url, strings[0]);
            } catch (IOException e) {
                Log.d(TAG, "Error sending message", e);
            }

            // Добавляем сообщение в комнату
            Message message1 = new Message(message, Timestamp.now(), Arrays.asList(otherUser.getUid(), uid));
            String roomID = null;

            // Проверяем, если комната существует
            if (room != null && otherUser.getToken() != null && otherUser.getUid() != null) {
                myService.addMessageToRoom(room, message1);
                roomID = room;
            } else if (otherUser != null) {
                roomID = myService.addNewRoomAndMessage(otherUser.getDocID(), otherUser.getUid(), message1);
            }

            // Теперь вызываем getRoom с коллбеком
            final List<Message>[] msgs = new List[1];  // Используем массив для хранения результатов

            myService.getRoom(roomID, new RoomCallback() {
                @Override
                public void onRoomLoaded(Room room) {
                    msgs[0] = room.getMessages();  // Заполняем сообщения
                    setIsItMeByToFrom(msgs[0]);  // Обрабатываем сообщения
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Ошибка при загрузке комнаты", e);
                }
            });

            // Пауза для того, чтобы дождаться асинхронной операции (хотя это не самый хороший способ)
            while (msgs[0] == null) {
                try {
                    Thread.sleep(100);  // Даем время асинхронной операции
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return msgs[0];  // Возвращаем сообщения
        }

        @Override
        protected void onPostExecute(List<Message> msgs) {
            super.onPostExecute(msgs);
            sendBtn.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            ChatThreadsRecyclerViewAdapter adapter = new ChatThreadsRecyclerViewAdapter(msgs);
            adapter.notifyDataSetChanged();
            listOfMessages.setAdapter(adapter);
        }
    }

    private void setIsItMeByToFrom(List<Message> msgs) {
        for (Message msg : msgs) {
            if (msg.getTofrom().get(0).equals(uid)) {
                msg.setIsItMe(false);
            } else {
                msg.setIsItMe(true);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
        }
    }
}
