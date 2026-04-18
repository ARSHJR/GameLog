package com.example.gamelog;

import com.google.gson.annotations.SerializedName;

public class AddToCollectionRequest {

    @SerializedName("game_id")
    private final String gameId;

    @SerializedName("status")
    private final String status;

    public AddToCollectionRequest(String gameId, String status) {
        this.gameId = gameId;
        this.status = status;
    }

    public String getGameId() {
        return gameId;
    }

    public String getStatus() {
        return status;
    }
}
