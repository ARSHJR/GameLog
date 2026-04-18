package com.example.gamelog;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;

public final class BackendUserHelper {

    private static final String PREFS_NAME = "GameLogPrefs";
    private static final String KEY_BACKEND_USER_ID = "backend_user_id";
    private static final String TEMP_BACKEND_USER_ID = "1";

    private BackendUserHelper() {
        // Utility class.
    }

    public static String getBackendUserId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedBackendUserId = normalizeBackendUserId(preferences.getString(KEY_BACKEND_USER_ID, null));
        if (!TextUtils.isEmpty(storedBackendUserId)) {
            return storedBackendUserId;
        }

        // Debug-only fallback for local/dev environments while backend identity mapping is being completed.
        return isDebugBuild(context) ? TEMP_BACKEND_USER_ID : null;
    }

    public static void persistBackendUserId(Context context, String backendUserId) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String normalized = normalizeBackendUserId(backendUserId);
        if (TextUtils.isEmpty(normalized)) {
            preferences.edit().remove(KEY_BACKEND_USER_ID).apply();
            return;
        }
        preferences.edit().putString(KEY_BACKEND_USER_ID, normalized).apply();
    }

    public static void clearBackendUserId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove(KEY_BACKEND_USER_ID).apply();
    }

    public static boolean hasStoredBackendUserId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return !TextUtils.isEmpty(normalizeBackendUserId(preferences.getString(KEY_BACKEND_USER_ID, null)));
    }

    private static String normalizeBackendUserId(String backendUserId) {
        if (backendUserId == null) {
            return null;
        }
        String trimmed = backendUserId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isDebugBuild(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
