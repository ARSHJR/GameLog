package com.example.gamelog;

import androidx.annotation.Nullable;

public class AddToCollectionResponse {

    @Nullable
    private Boolean created;

    @Nullable
    private UserGameEntryResponse entry;

    @Nullable
    public Boolean getCreated() {
        return created;
    }

    @Nullable
    public UserGameEntryResponse getEntry() {
        return entry;
    }
}
