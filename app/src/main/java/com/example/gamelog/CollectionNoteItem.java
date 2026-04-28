package com.example.gamelog;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class CollectionNoteItem {

    @SerializedName("note_id")
    @Nullable
    private String noteId;

    @SerializedName("user_id")
    @Nullable
    private String userId;

    @SerializedName("user_game_id")
    @Nullable
    private String userGameId;

    @SerializedName("game_id")
    @Nullable
    private String gameId;

    @SerializedName("game_title")
    @Nullable
    private String gameTitle;

    @Nullable
    private String title;

    @SerializedName("note_text")
    @Nullable
    private String noteText;

    @SerializedName("media_uri")
    @Nullable
    private String mediaUri;

    @Nullable
    private Double latitude;

    @Nullable
    private Double longitude;

    @SerializedName("note_type")
    @Nullable
    private String noteType;

    @SerializedName("is_pinned")
    @Nullable
    private Boolean isPinned;

    @SerializedName("task_status")
    @Nullable
    private String taskStatus;

    @SerializedName("completed_at")
    @Nullable
    private String completedAt;

    @SerializedName("created_at")
    @Nullable
    private String createdAt;

    @SerializedName("reminder_id")
    @Nullable
    private String reminderId;

    @Nullable
    private String frequency;

    @SerializedName("is_active")
    @Nullable
    private Boolean isActive;

    @SerializedName("next_trigger_at")
    @Nullable
    private String nextTriggerAt;

    @SerializedName("last_triggered_at")
    @Nullable
    private String lastTriggeredAt;

    @SerializedName("snoozed_until")
    @Nullable
    private String snoozedUntil;

    @SerializedName("timezone_id")
    @Nullable
    private String timezoneId;

    @Nullable
    public String getNoteId() {
        return noteId;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getUserGameId() {
        return userGameId;
    }

    @Nullable
    public String getGameId() {
        return gameId;
    }

    @Nullable
    public String getGameTitle() {
        return gameTitle;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getNoteText() {
        return noteText;
    }

    @Nullable
    public String getMediaUri() {
        return mediaUri;
    }

    @Nullable
    public Double getLatitude() {
        return latitude;
    }

    @Nullable
    public Double getLongitude() {
        return longitude;
    }

    @Nullable
    public String getNoteType() {
        return noteType;
    }

    @Nullable
    public Boolean getIsPinned() {
        return isPinned;
    }

    @Nullable
    public String getTaskStatus() {
        return taskStatus;
    }

    @Nullable
    public String getCompletedAt() {
        return completedAt;
    }

    @Nullable
    public String getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public String getReminderId() {
        return reminderId;
    }

    @Nullable
    public String getFrequency() {
        return frequency;
    }

    @Nullable
    public Boolean getIsActive() {
        return isActive;
    }

    @Nullable
    public String getNextTriggerAt() {
        return nextTriggerAt;
    }

    @Nullable
    public String getLastTriggeredAt() {
        return lastTriggeredAt;
    }

    @Nullable
    public String getSnoozedUntil() {
        return snoozedUntil;
    }

    @Nullable
    public String getTimezoneId() {
        return timezoneId;
    }
}
