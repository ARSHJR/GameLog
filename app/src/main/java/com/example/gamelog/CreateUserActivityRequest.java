package com.example.gamelog;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class CreateUserActivityRequest {

    @SerializedName("action_name")
    private final String actionName;

    @SerializedName("entity_type")
    private final String entityType;

    @SerializedName("entity_id")
    @Nullable
    private final String entityId;

    @SerializedName("action_details")
    @Nullable
    private final String actionDetails;

    @SerializedName("duration_seconds")
    private final int durationSeconds;

    public CreateUserActivityRequest(String actionName,
                                     String entityType,
                                     @Nullable String entityId,
                                     @Nullable String actionDetails,
                                     int durationSeconds) {
        this.actionName = actionName;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actionDetails = actionDetails;
        this.durationSeconds = durationSeconds;
    }

    public String getActionName() {
        return actionName;
    }

    public String getEntityType() {
        return entityType;
    }

    @Nullable
    public String getEntityId() {
        return entityId;
    }

    @Nullable
    public String getActionDetails() {
        return actionDetails;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }
}
