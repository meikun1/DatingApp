package com.example.DatingApp.Users;

import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable, Comparable<User> {
	public static final String TAG = "UserClass";
	public static final String NAME_DB = "name";
	public static final String TOKEN_DB = "token";
	public static final String LATLNG_DB = "latlng";
	public static final String MESSAGES_DB = "messages";
	public static final String IMG_URL_DB = "img_url";

	private String docID;
	private String userId; // Изменено с uid на userId, чтобы соответствовать Firestore
	private String name;
	private String token;
	private GeoPoint latlng;
	private List<String> messages;
	private String img_url;
	private String distance;
	private UserInfo info;
	private List<String> favs;
	// Поля из Firestore
	private String about;
	private String birthDate;
	private String ethnicity;
	private int height;
	private String orientation;
	private String preference; // Исправлено с "reference" на "preference"
	private String relationship;
	private String religion;
	private String role;
	private String stds;
	private int weight;

	public User() {
		// Инициализация по умолчанию
		this.messages = new ArrayList<>();
		this.favs = new ArrayList<>();
		this.latlng = new GeoPoint(0.0, 0.0); // Fallback координаты
	}

	public User(String userId, String name, String token, GeoPoint latlng, List<String> messages,
				String img_url, UserInfo info, List<String> favs, String about, String birthDate,
				String ethnicity, int height, String orientation, String preference, String relationship,
				String religion, String role, String stds, int weight) {
		this.userId = userId;
		this.name = name;
		this.token = token;
		this.latlng = latlng != null ? latlng : new GeoPoint(0.0, 0.0);
		this.messages = messages != null ? messages : new ArrayList<>();
		this.img_url = img_url;
		this.info = info;
		this.favs = favs != null ? favs : new ArrayList<>();
		this.about = about;
		this.birthDate = birthDate;
		this.ethnicity = ethnicity;
		this.height = height;
		this.orientation = orientation;
		this.preference = preference;
		this.relationship = relationship;
		this.religion = religion;
		this.role = role;
		this.stds = stds;
		this.weight = weight;
	}

	// Геттеры и сеттеры
	public String getDocID() {
		return docID;
	}

	public void setDocID(String docID) {
		this.docID = docID;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
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
		this.latlng = latlng != null ? latlng : new GeoPoint(0.0, 0.0);
	}

	public List<String> getMessages() {
		return messages;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages != null ? messages : new ArrayList<>();
	}

	public String getImg_url() {
		return img_url;
	}

	public void setImg_url(String img_url) {
		this.img_url = img_url;
	}

	public String getDistance() {
		return distance;
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

	public List<String> getFavs() {
		return favs;
	}

	public void setFavs(List<String> favs) {
		this.favs = favs != null ? favs : new ArrayList<>();
	}

	public String getAbout() {
		return about;
	}

	public void setAbout(String about) {
		this.about = about;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public String getEthnicity() {
		return ethnicity;
	}

	public void setEthnicity(String ethnicity) {
		this.ethnicity = ethnicity;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public String getPreference() {
		return preference;
	}

	public void setPreference(String preference) {
		this.preference = preference;
	}

	public String getRelationship() {
		return relationship;
	}

	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}

	public String getReligion() {
		return religion;
	}

	public void setReligion(String religion) {
		this.religion = religion;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getStds() {
		return stds;
	}

	public void setStds(String stds) {
		this.stds = stds;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public void addRoom(String id) {
		if (this.messages == null) {
			this.messages = new ArrayList<>();
		}
		this.messages.add(id);
	}

	@Override
	public int compareTo(User o) {
		if (getDistance() == null || o.getDistance() == null) {
			return 0;
		}
		return Integer.valueOf(getDistance()).compareTo(Integer.valueOf(o.getDistance()));
	}
}