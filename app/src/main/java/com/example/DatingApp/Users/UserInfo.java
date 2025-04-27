package com.example.DatingApp.Users;

public class UserInfo {
    private String userId;
    private String about;
    private String birthDate;
    private int height;
    private int weight;
    private String relationship;
    private String religion;
    private String orientation;
    private String role;
    private String ethnicity;
    private String reference;
    private String stds;

    // Пустой конструктор
    public UserInfo() {
        // Обязателен для Firestore
    }

    // Полный конструктор
    public UserInfo(String userId, String about, String birthDate, int height, int weight,
                    String relationship, String religion, String orientation, String role,
                    String ethnicity, String reference, String stds) {
        this.userId = userId;
        this.about = about;
        this.birthDate = birthDate;
        this.height = height;
        this.weight = weight;
        this.relationship = relationship;
        this.religion = religion;
        this.orientation = orientation;
        this.role = role;
        this.ethnicity = ethnicity;
        this.reference = reference;
        this.stds = stds;
    }

    // Геттеры
    public String getUserId() { return userId; }
    public String getAbout() { return about; }
    public String getBirthDate() { return birthDate; }
    public int getHeight() { return height; }
    public int getWeight() { return weight; }
    public String getRelationship() { return relationship; }
    public String getReligion() { return religion; }
    public String getOrientation() { return orientation; }
    public String getRole() { return role; }
    public String getEthnicity() { return ethnicity; }
    public String getReference() { return reference; }
    public String getStds() { return stds; }
}
