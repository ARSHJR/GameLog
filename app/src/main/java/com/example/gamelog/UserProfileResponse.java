package com.example.gamelog;

import com.google.gson.annotations.SerializedName;

public class UserProfileResponse {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("email")
    private String email;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("total_games")
    private Integer totalGames;

    @SerializedName("total_favourites")
    private Integer totalFavourites;

    @SerializedName("total_notes")
    private Integer totalNotes;

    @SerializedName("total_reminders")
    private Integer totalReminders;

    @SerializedName("completed_tasks")
    private Integer completedTasks;

    @SerializedName("total_time_spent")
    private Integer totalTimeSpent;

    @SerializedName("last_activity_at")
    private String lastActivityAt;

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Integer getTotalGames() {
        return totalGames;
    }

    public Integer getTotalFavourites() {
        return totalFavourites;
    }

    public Integer getTotalNotes() {
        return totalNotes;
    }

    public Integer getTotalReminders() {
        return totalReminders;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public Integer getTotalTimeSpent() {
        return totalTimeSpent;
    }

    public String getLastActivityAt() {
        return lastActivityAt;
    }
}
