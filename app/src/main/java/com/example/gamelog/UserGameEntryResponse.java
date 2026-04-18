package com.example.gamelog;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class UserGameEntryResponse {

    @SerializedName("user_game_id")
    @Nullable
    private String userGameId;

    @SerializedName("user_id")
    @Nullable
    private String userId;

    @SerializedName("game_id")
    @Nullable
    private String gameId;

    @SerializedName("status")
    @Nullable
    private String status;

    @SerializedName("is_favourite")
    @Nullable
    private Boolean isFavourite;

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
}
