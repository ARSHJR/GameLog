package com.example.gamelog;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class CollectionEntryItem {

    @SerializedName("user_game_id")
    @Nullable
    private String userGameId;

    @SerializedName("user_id")
    @Nullable
    private String userId;

    @SerializedName("game_id")
    @Nullable
    private String gameId;

    @Nullable
    private String status;

    @SerializedName("is_favourite")
    @Nullable
    private Boolean isFavourite;

    @SerializedName("added_at")
    @Nullable
    private String addedAt;

    @SerializedName("updated_at")
    @Nullable
    private String updatedAt;

    @Nullable
    private String title;

    @Nullable
    private String description;

    @SerializedName("cover_image_url")
    @Nullable
    private String coverImageUrl;

    @SerializedName("release_date")
    @Nullable
    private String releaseDate;

    @Nullable
    private String developer;

    @Nullable
    private String publisher;

    @Nullable
    private String platform;

    @Nullable
    public String getUserGameId() {
        return userGameId;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    @Nullable
    public String getGameId() {
        return gameId;
    }

    @Nullable
    public String getStatus() {
        return status;
    }

    @Nullable
    public Boolean getIsFavourite() {
        return isFavourite;
    }

    @Nullable
    public String getAddedAt() {
        return addedAt;
    }

    @Nullable
    public String getUpdatedAt() {
        return updatedAt;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    @Nullable
    public String getReleaseDate() {
        return releaseDate;
    }

    @Nullable
    public String getDeveloper() {
        return developer;
    }

    @Nullable
    public String getPublisher() {
        return publisher;
    }

    @Nullable
    public String getPlatform() {
        return platform;
    }
}
