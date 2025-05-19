package com.example.anew;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private String imageUrl;
    private GeoPoint location;
    private String locationName;
    private Timestamp timestamp;
    private List<String> likes;
    private List<Comment> comments;
    private boolean isLiked;

    public Post() {
        // Required empty constructor for Firestore
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.timestamp = Timestamp.now();
    }

    public Post(String userId, String userName, String userAvatar, String content, String imageUrl,
                GeoPoint location, String locationName) {
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.content = content;
        this.imageUrl = imageUrl;
        this.location = location;
        this.locationName = locationName;
        this.likes = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.timestamp = Timestamp.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getLikes() {
        return likes;
    }

    public void setLikes(List<String> likes) {
        this.likes = likes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public static class Comment {
        private String userId;
        private String userName;
        private String userAvatar;
        private String content;
        private Timestamp timestamp;

        public Comment() {
            // Required empty constructor for Firestore
        }

        public Comment(String userId, String userName, String userAvatar, String content) {
            this.userId = userId;
            this.userName = userName;
            this.userAvatar = userAvatar;
            this.content = content;
            this.timestamp = Timestamp.now();
        }

        // Getters and Setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUserAvatar() {
            return userAvatar;
        }

        public void setUserAvatar(String userAvatar) {
            this.userAvatar = userAvatar;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Timestamp timestamp) {
            this.timestamp = timestamp;
        }
    }
} 