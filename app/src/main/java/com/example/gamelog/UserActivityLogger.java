package com.example.gamelog;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class UserActivityLogger {

    private UserActivityLogger() {
    }

    public static void logDuration(Context context,
                                   String actionName,
                                   String entityType,
                                   @Nullable String entityId,
                                   @Nullable String actionDetails,
                                   long durationMillis) {
        if (context == null || TextUtils.isEmpty(actionName) || TextUtils.isEmpty(entityType)) {
            return;
        }

        String backendUserId = BackendUserHelper.getBackendUserId(context);
        if (TextUtils.isEmpty(backendUserId)) {
            return;
        }

        int durationSeconds = (int) Math.max(1L, durationMillis / 1000L);
        CreateUserActivityRequest request = new CreateUserActivityRequest(
                actionName,
                entityType,
                entityId,
                actionDetails,
                durationSeconds
        );

        ApiService apiService = RetrofitClient.getApiService();
        apiService.createUserActivity(backendUserId, request).enqueue(new Callback<UserActivityItem>() {
            @Override
            public void onResponse(Call<UserActivityItem> call, Response<UserActivityItem> response) {
                // best effort logging; no-op
            }

            @Override
            public void onFailure(Call<UserActivityItem> call, Throwable t) {
                // best effort logging; no-op
            }
        });
    }
}
