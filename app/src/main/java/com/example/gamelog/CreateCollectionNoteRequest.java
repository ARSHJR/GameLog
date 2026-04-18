package com.example.gamelog;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class CreateCollectionNoteRequest {

    @SerializedName("note_type")
    private final String noteType;

    @SerializedName("title")
    @Nullable
    private final String title;

    @SerializedName("note_text")
    @Nullable
    private final String noteText;

    @SerializedName("media_uri")
    @Nullable
    private final String mediaUri;

    @SerializedName("latitude")
    @Nullable
    private final Double latitude;

    @SerializedName("longitude")
    @Nullable
    private final Double longitude;

    @SerializedName("task_status")
    @Nullable
    private final String taskStatus;

    @SerializedName("frequency")
    @Nullable
    private final String frequency;

    public CreateCollectionNoteRequest(String noteType, @Nullable String title, @Nullable String noteText,
                                       @Nullable String mediaUri, @Nullable Double latitude,
                                       @Nullable Double longitude, @Nullable String taskStatus) {
        this(noteType, title, noteText, mediaUri, latitude, longitude, taskStatus, null);
    }

    public CreateCollectionNoteRequest(String noteType, @Nullable String title, @Nullable String noteText,
                                       @Nullable String mediaUri, @Nullable Double latitude,
                                       @Nullable Double longitude, @Nullable String taskStatus,
                                       @Nullable String frequency) {
        this.noteType = noteType;
        this.title = title;
        this.noteText = noteText;
        this.mediaUri = mediaUri;
        this.latitude = latitude;
        this.longitude = longitude;
        this.taskStatus = taskStatus;
        this.frequency = frequency;
    }

    public String getNoteType() {
        return noteType;
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
    public String getTaskStatus() {
        return taskStatus;
    }

    @Nullable
    public String getFrequency() {
        return frequency;
    }
}
