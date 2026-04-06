package com.example.gamelog;

import android.net.Uri;

public class GameItem {
    private String title;
    private String notes;
    private String status;
    private String imageUri; // Store Uri as String
    private String location;

    public GameItem(String title, String notes, String status) {
        this.title = title;
        this.notes = notes;
        this.status = status;
    }

    public GameItem(String title, String notes, String status, String imageUri, String location) {
        this.title = title;
        this.notes = notes;
        this.status = status;
        this.imageUri = imageUri;
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getLocation() {
        return location;
    }
}
