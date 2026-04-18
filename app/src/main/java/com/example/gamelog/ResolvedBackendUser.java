package com.example.gamelog;

import com.google.gson.annotations.SerializedName;

public class ResolvedBackendUser {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("auth_user_id")
    private String authUserId;

    @SerializedName("email")
    private String email;

    @SerializedName("display_name")
    private String displayName;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public String getUserId() {
        return userId;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
