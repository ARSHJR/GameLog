package com.example.gamelog;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class FavouriteToggleResponse {

    @SerializedName("is_favourite")
    @Nullable
    private Boolean isFavourite;

    @Nullable
    public Boolean getIsFavourite() {
        return isFavourite;
    }
}
