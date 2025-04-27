package com.example.DatingApp.Adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.DatingApp.OwnProfileActivity;
import com.example.DatingApp.ProfileActivity;
import com.example.DatingApp.R;
import com.example.DatingApp.Users.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class OnlineRecyclerViewAdapterBig extends RecyclerView.Adapter<OnlineRecyclerViewAdapterBig.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    private List<User> users;
    private Context mContext;

    public OnlineRecyclerViewAdapterBig(Context context, List<User> users) {
        this.mContext = context;
        this.users = users;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_big, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            if (position == 0) {
                holder.cardView.setRadius(17);
                holder.cardView.setBackground(null);
                holder.userName.setText(currentUser.getDisplayName());
                holder.distance.setText(users.get(position).getDistance());
            } else {
                holder.cardView.setBackgroundResource(R.drawable.ic_launcher_background);
                holder.userName.setText(users.get(position).getName());
                holder.distance.setText(users.get(position).getDistance());
            }

            holder.parentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: clicked on: ");
                    Intent intent;
                    if (position == 0) {
                        intent = new Intent(mContext, OwnProfileActivity.class);
                    } else {
                        intent = new Intent(mContext, ProfileActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("uid", users.get(position).getUid());
                        intent.putExtras(bundle);
                    }
                    mContext.startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView userName;
        TextView distance;
        ConstraintLayout parentLayout;
        CardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.txtUser);
            distance = itemView.findViewById(R.id.txtDistance);
            parentLayout = itemView.findViewById(R.id.parentLayout);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
