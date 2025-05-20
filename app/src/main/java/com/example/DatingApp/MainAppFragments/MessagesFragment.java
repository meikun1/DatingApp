package com.example.DatingApp.MainAppFragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.DatingApp.Chat.Room;
import com.example.DatingApp.ChatActivity;
import com.example.DatingApp.R;
import com.example.DatingApp.Services.MyDBService;
import com.example.DatingApp.Users.MyUser;
import com.example.DatingApp.Users.User;

import java.util.ArrayList;
import java.util.List;

public class MessagesFragment extends Fragment {
	private static final String TAG = "MessagesFragment";

	private ProgressBar progressBar;
	private RecyclerView recyclerView;
	private TextView textViewNoChats;
	private MyDBService.MyLocalBinder binder;
	private MyDBService myService;

	private List<Room> roomsList = new ArrayList<>();
	private List<User> usersList = new ArrayList<>();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView: started.");
		View view = inflater.inflate(R.layout.fragment_messages, container, false);

		progressBar = view.findViewById(R.id.progress);
		recyclerView = view.findViewById(R.id.recyclerv_view);
		textViewNoChats = view.findViewById(R.id.noChatLbl);

		// Получаем сервис из аргументов фрагмента
		Bundle bundle = getArguments();
		if (bundle != null) {
			binder = (MyDBService.MyLocalBinder) bundle.getBinder("binder");
			if (binder != null) {
				myService = binder.getService();
				loadChatThreads();
			} else {
				Log.e(TAG, "Service binder is null");
				showNoChats();
			}
		} else {
			Log.e(TAG, "Bundle is null");
			showNoChats();
		}

		return view;
	}

	private void loadChatThreads() {
		progressBar.setVisibility(View.VISIBLE);

		if (myService == null) {
			Log.e(TAG, "MyService is null");
			progressBar.setVisibility(View.GONE);
			showNoChats();
			return;
		}

		// Асинхронно загружаем список комнат
		myService.getAllRoomsAsync(new MyDBService.OnRoomsLoadedListener() {
			@Override
			public void onRoomsLoaded(List<Room> rooms) {
				roomsList = rooms != null ? rooms : new ArrayList<>();
				if (roomsList.isEmpty()) {
					progressBar.setVisibility(View.GONE);
					showNoChats();
					return;
				}

				// Загружаем пользователей для комнат
				myService.getUsersFromRooms(roomsList, new MyDBService.NearbyUsersCallback() {
					@Override
					public void onNearbyUsersLoaded(List<User> users) {
						usersList = users != null ? users : new ArrayList<>();
						progressBar.setVisibility(View.GONE);
						if (usersList.isEmpty()) {
							showNoChats();
						} else {
							initRecyclerView();
						}
					}

					@Override
					public void onError(Exception e) {
						Log.e(TAG, "Failed to load users: " + e.getMessage());
						progressBar.setVisibility(View.GONE);
						showNoChats();
					}
				});
			}

			@Override
			public void onError(Exception e) {
				Log.e(TAG, "Failed to load rooms: " + e.getMessage());
				progressBar.setVisibility(View.GONE);
				showNoChats();
			}
		});
	}

	private void showNoChats() {
		recyclerView.setVisibility(View.GONE);
		textViewNoChats.setVisibility(View.VISIBLE);
	}

	private void initRecyclerView() {
		Log.d(TAG, "initRecyclerView: initializing RecyclerView.");
		RecyclerViewAdapter adapter = new RecyclerViewAdapter(roomsList, usersList);
		recyclerView.setAdapter(adapter);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		recyclerView.setVisibility(View.VISIBLE);
		textViewNoChats.setVisibility(View.GONE);
	}

	// Адаптер для RecyclerView
	public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

		private static final String TAG = "RecyclerViewAdapter";
		private List<Room> rooms;
		private List<User> users;

		public RecyclerViewAdapter(List<Room> rooms, List<User> users) {
			this.rooms = rooms != null ? rooms : new ArrayList<>();
			this.users = users != null ? users : new ArrayList<>();
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
			Log.d(TAG, "onBindViewHolder: binding view at position " + position);

			if (position >= users.size() || position >= rooms.size()) {
				Log.e(TAG, "Index out of bounds: position=" + position + ", users=" + users.size() + ", rooms=" + rooms.size());
				return;
			}

			User user = users.get(position);
			Room room = rooms.get(position);

			// Привязка данных к элементам списка
			holder.nameLbl.setText(user.getName() != null ? user.getName() : "Unknown");
			holder.image.setImageResource(R.drawable.ic_launcher_background); // Замените на реальное изображение

			if (room.getMessages() != null && !room.getMessages().isEmpty()) {
				holder.txtMessages.setText(room.getMessages().get(0).getContent());
			} else {
				holder.txtMessages.setText("");
			}

			// Обработка клика на элемент списка
			holder.parentLayout.setOnClickListener(view -> {
				Log.d(TAG, "onClick: clicked on: " + user.getName());
				Intent intent = new Intent(getContext(), ChatActivity.class);
				Bundle bundle = new Bundle();
				bundle.putSerializable("other_user", new MyUser(user));
				bundle.putString("room", room.getRoomUID());
				intent.putExtras(bundle);
				startActivity(intent);
			});
		}

		@Override
		public int getItemCount() {
			return Math.min(rooms.size(), users.size());
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			ImageView image;
			TextView nameLbl, txtMessages;
			ConstraintLayout parentLayout;

			public ViewHolder(View itemView) {
				super(itemView);
				image = itemView.findViewById(R.id.image);
				nameLbl = itemView.findViewById(R.id.image_name);
				parentLayout = itemView.findViewById(R.id.parent_layout);
				txtMessages = itemView.findViewById(R.id.txtMessage);
			}
		}
	}
}