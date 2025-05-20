package com.example.DatingApp.Adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.DatingApp.Chat.Message;
import com.example.DatingApp.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatThreadsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private List<Message> messagesList;

	private static final int VIEW_TYPE_SENT = 1;
	private static final int VIEW_TYPE_RECEIVED = 2;

	public ChatThreadsRecyclerViewAdapter(List<Message> messagesList) {
		this.messagesList = messagesList != null ? new ArrayList<>(messagesList) : new ArrayList<>();
		sortMessages();
	}

	// Метод для обновления списка сообщений
	public void setMessages(List<Message> messages) {
		this.messagesList = messages != null ? new ArrayList<>(messages) : new ArrayList<>();
		sortMessages();
		notifyDataSetChanged();
	}

	// Сортировка сообщений по времени
	private void sortMessages() {
		if (!messagesList.isEmpty()) {
			try {
				Collections.sort(messagesList);
			} catch (Exception e) {
				// Логируем ошибку, если Message не реализует Comparable или есть проблемы с данными
				android.util.Log.e("ChatAdapter", "Error sorting messages", e);
			}
		}
	}

	@Override
	public int getItemViewType(int position) {
		if (position < 0 || position >= messagesList.size()) {
			return VIEW_TYPE_RECEIVED; // Запасной вариант
		}
		Boolean isItMe = messagesList.get(position).getIsItMe();
		return (isItMe != null && isItMe) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		if (viewType == VIEW_TYPE_SENT) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_chat, parent, false);
			return new MyViewHolder(view);
		} else {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat, parent, false);
			return new ViewHolder(view);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		if (position < 0 || position >= messagesList.size()) {
			return; // Защита от некорректных индексов
		}

		Message message = messagesList.get(position);
		if (message == null) {
			return;
		}

		if (holder instanceof MyViewHolder) {
			MyViewHolder myViewHolder = (MyViewHolder) holder;
			myViewHolder.content.setText(message.getContent() != null ? message.getContent() : "");
			myViewHolder.time.setText(message.getTimestamp() != null ?
					message.getTimestamp().toDate().toString() : "");
			myViewHolder.itemView.setOnLongClickListener(v -> {
				Toast.makeText(v.getContext(), "Long pressed on " + message.getContent(), Toast.LENGTH_LONG).show();
				return true;
			});
		} else if (holder instanceof ViewHolder) {
			ViewHolder viewHolder = (ViewHolder) holder;
			viewHolder.content.setText(message.getContent() != null ? message.getContent() : "");
			viewHolder.time.setText(message.getTimestamp() != null ?
					message.getTimestamp().toDate().toString() : "");
			viewHolder.itemView.setOnLongClickListener(v -> {
				Toast.makeText(v.getContext(), "Long pressed on " + message.getContent(), Toast.LENGTH_LONG).show();
				return true;
			});
		}
	}

	@Override
	public int getItemCount() {
		return messagesList.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		TextView content, time;

		public ViewHolder(View itemView) {
			super(itemView);
			content = itemView.findViewById(R.id.txtChat);
			time = itemView.findViewById(R.id.txtTime);
		}
	}

	public static class MyViewHolder extends RecyclerView.ViewHolder {
		TextView content, time;

		public MyViewHolder(View itemView) {
			super(itemView);
			content = itemView.findViewById(R.id.txtChat);
			time = itemView.findViewById(R.id.txtTime);
		}
	}
}