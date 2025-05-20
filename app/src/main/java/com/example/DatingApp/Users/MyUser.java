package com.example.DatingApp.Users;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.List;

public class MyUser implements Serializable {

	private User user; // Оригинальный объект User
	private GeoPoint geoPoint; // Для хранения GeoPoint напрямую
	private String uid; // Переименовано с userId для соответствия логике
	private String name;
	private String token;
	private GeoPoint latlng; // Используем GeoPoint вместо MyGeoPoint
	private List<String> messages;
	private String img_url;
	private String distance;
	private UserInfo info;
	private String docID;
	private List<String> favs;

	public MyUser(User user) {
		if (user != null) {
			this.uid = user.getUserId(); // Заменено getUid() на getUserId()
			this.name = user.getName();
			this.token = user.getToken();
			this.latlng = user.getLatlng(); // Прямое использование GeoPoint
			this.messages = user.getMessages() != null ? user.getMessages() : null;
			this.img_url = user.getImg_url();
			this.info = user.getInfo();
			this.docID = user.getDocID();
			this.favs = user.getFavs() != null ? user.getFavs() : null;
			this.geoPoint = user.getLatlng(); // Копия GeoPoint
		}
	}

	public List<String> getFavs() {
		return favs;
	}

	public void setFavs(List<String> favs) {
		this.favs = favs;
	}

	public String getDocID() {
		return docID;
	}

	public void setDocID(String docID) {
		this.docID = docID;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getName() {
		return name != null ? name : "Anonymous";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public GeoPoint getLatlng() {
		return latlng;
	}

	public void setLatlng(GeoPoint latlng) {
		this.latlng = latlng;
	}

	public List<String> getMessages() {
		return messages;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

	public String getImg_url() {
		return img_url;
	}

	public void setImg_url(String img_url) {
		this.img_url = img_url;
	}

	public String getDistance() {
		return distance != null ? distance : "Unknown";
	}

	public void setDistance(String distance) {
		this.distance = distance;
	}

	public UserInfo getInfo() {
		return info;
	}

	public void setInfo(UserInfo info) {
		this.info = info;
	}

	public GeoPoint getGeoPoint() {
		return geoPoint;
	}

	public void setGeoPoint(GeoPoint geoPoint) {
		this.geoPoint = geoPoint;
	}

	// Метод для получения оригинального объекта User (если нужен)
	public User getUser() {
		return user;
	}
}