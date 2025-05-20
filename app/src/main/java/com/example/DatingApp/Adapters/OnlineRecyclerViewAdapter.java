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
import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class OnlineRecyclerViewAdapter extends RecyclerView.Adapter<OnlineRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<User> mUsers;
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

        User user = mUsers.get(position);
        holder.userName.setText(user.getName());

        holder.distance.setText(user.getDistance());

        // Загрузка изображения с помощью Glide, если img_url доступен
        if (user.getImg_url() != null && !user.getImg_url().isEmpty()) {
            Glide.with(mContext)
                    .load(user.getImg_url())
                    .placeholder(R.drawable.placeholder_image) // Укажите запасное изображение
                    .error(R.drawable.error_image) // Укажите изображение для ошибки
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.placeholder_image); // Укажите запасное изображение
        }

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked on: " + user.getName());

                Intent intent;
                if (position == 0) {
                    intent = new Intent(mContext, OwnProfileActivity.class);
                } else {
                    intent = new Intent(mContext, ProfileActivity.class);
                }
                intent.putExtra("USER_ID", user.getUserId()); // Заменено getUid() на getUserId()
                intent.putExtra("USER_NAME", user.getName());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView userName;
        TextView distance;
        ConstraintLayout parentLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.newGuy);
            userName = itemView.findViewById(R.id.txtUser);
            distance = itemView.findViewById(R.id.txtDistance);
            parentLayout = itemView.findViewById(R.id.parentLayout);
        }
    }
}