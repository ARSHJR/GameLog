package com.example.gamelog;

import com.google.gson.annotations.SerializedName;

public class UserActivityItem {

    @SerializedName(value = "activity_id", alternate = {"id", "log_id"})
    private String activityId;

    @SerializedName(value = "action_type", alternate = {"action", "activity_type", "event_type"})
    private String actionType;

    @SerializedName(value = "entity_type", alternate = {"target_type"})
    private String entityType;

    @SerializedName(value = "entity_id", alternate = {"target_id"})
    private String entityId;

    @SerializedName(value = "duration_seconds", alternate = {"duration", "time_spent_seconds"})
    private Integer durationSeconds;

    @SerializedName(value = "occurred_at", alternate = {"created_at", "timestamp", "updated_at"})
    private String occurredAt;

    public String getActivityId() {
        return activityId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getOccurredAt() {
        return occurredAt;
    }
}
