package com.example.DatingApp.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.DatingApp.OwnProfileActivity;
import com.example.DatingApp.ProfileActivity;
import com.example.DatingApp.R;
import com.example.DatingApp.Users.User;
import com.bumptech.glide.Glide;  // Подключи библиотеку Glide для работы с изображениями

import java.util.ArrayList;

public class OnlineRecyclerViewAdapter extends RecyclerView.Adapter<OnlineRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<User> mUsers;  // Список пользователей
    private Context mContext;

    public OnlineRecyclerViewAdapter(Context context, ArrayList<User> users) {
        mUsers = users;
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        User user = mUsers.get(position); // Используем список пользователей
        holder.userName.setText(user.getName());  // Имя пользователя

        holder.distance.setText(user.getDistance()); // Показать расстояние

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked on: " + user.getName());

                Intent intent;
                // Передаем данные о пользователе в активность
                if (position == 0) {
                    intent = new Intent(mContext, OwnProfileActivity.class);
                } else {
                    intent = new Intent(mContext, ProfileActivity.class);
                }
                intent.putExtra("USER_ID", user.getUid());  // Пример передачи данных
                intent.putExtra("USER_NAME", user.getName());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size(); // Количество пользователей
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView userName;
        TextView distance;
        ConstraintLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.newGuy); // Проверка правильности идентификатора
            userName = itemView.findViewById(R.id.txtUser); // Идентификатор для имени
            distance = itemView.findViewById(R.id.txtDistance); // Идентификатор для расстояния
            parentLayout = itemView.findViewById(R.id.parentLayout); // Родительский layout для кликабельности
        }
    }
}
