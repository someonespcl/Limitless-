package com.limitless.models;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DisplayUsers {

    private String userId;
    private String userName;
    private String userEmail;
    private String imageUrl;
    
    public DisplayUsers() {}

    public DisplayUsers(String userId, String userName, String userEmail, String imageUrl) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return this.userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
