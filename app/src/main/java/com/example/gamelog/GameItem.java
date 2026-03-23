package com.example.gamelog;

public class GameItem {
    private String title;
    private String notes;
    private String status;

    public GameItem(String title, String notes) {
        this.title = title;
        this.notes = notes;
        this.status = "In progress";
    }

    public GameItem(String title, String notes, String status) {
        this.title = title;
        this.notes = notes;
        this.status = status;
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
}
