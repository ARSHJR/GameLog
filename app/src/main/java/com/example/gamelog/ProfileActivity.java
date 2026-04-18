package com.example.gamelog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final int RECENT_ACTIVITY_LIMIT = 8;

    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View errorContainer;
    private TextView errorText;
    private Button retryButton;

    private View contentContainer;
    private TextView displayNameText;
    private TextView emailText;
    private TextView avatarFallbackText;
    private TextView avatarHintText;

    private TextView totalGamesText;
    private TextView totalFavouritesText;
    private TextView totalNotesText;
    private TextView totalRemindersText;
    private TextView completedTasksText;

    private TextView totalTimeText;
    private TextView lastActivityText;

    private RecyclerView activityRecyclerView;
    private UserActivityAdapter activityAdapter;
    private View activityEmptyContainer;
    private TextView activitySectionErrorText;
    private Button activityRetryButton;

    private String backendUserId;
    private UserProfileResponse latestProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ImageButton backButton = findViewById(R.id.profile_back_button);
        backButton.setOnClickListener(v -> finish());

        loadingProgress = findViewById(R.id.profile_loading_progress);
        loadingText = findViewById(R.id.profile_loading_text);
        errorContainer = findViewById(R.id.profile_error_container);
        errorText = findViewById(R.id.profile_error_text);
        retryButton = findViewById(R.id.profile_retry_button);

        contentContainer = findViewById(R.id.profile_content_container);
        displayNameText = findViewById(R.id.profile_display_name);
        emailText = findViewById(R.id.profile_email);
        avatarFallbackText = findViewById(R.id.profile_avatar_fallback);
        avatarHintText = findViewById(R.id.profile_avatar_hint);

        totalGamesText = findViewById(R.id.profile_total_games);
        totalFavouritesText = findViewById(R.id.profile_total_favourites);
        totalNotesText = findViewById(R.id.profile_total_notes);
        totalRemindersText = findViewById(R.id.profile_total_reminders);
        completedTasksText = findViewById(R.id.profile_completed_tasks);

        totalTimeText = findViewById(R.id.profile_total_time);
        lastActivityText = findViewById(R.id.profile_last_activity);

        activityRecyclerView = findViewById(R.id.profile_activity_recycler_view);
        activityEmptyContainer = findViewById(R.id.profile_activity_empty_container);
        activitySectionErrorText = findViewById(R.id.profile_activity_error_text);
        activityRetryButton = findViewById(R.id.profile_activity_retry_button);

        activityRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new UserActivityAdapter(new ArrayList<>());
        activityRecyclerView.setAdapter(activityAdapter);

        retryButton.setOnClickListener(v -> fetchProfileAndActivity());
        activityRetryButton.setOnClickListener(v -> fetchActivityOnly());

        backendUserId = BackendUserHelper.getBackendUserId(this);
        if (TextUtils.isEmpty(backendUserId)) {
            showErrorState("Missing user context.");
            return;
        }

        fetchProfileAndActivity();
    }

    private void fetchProfileAndActivity() {
        showLoadingState();
        ApiService apiService = RetrofitClient.getApiService();

        apiService.getUserProfile(backendUserId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState("Could not load profile.");
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    return;
                }

                latestProfile = response.body();
                bindProfile(response.body());
                fetchActivityOnly();
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                showErrorState("Unable to connect to server.");
                Toast.makeText(ProfileActivity.this, "Failed to load profile: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchActivityOnly() {
        ApiService apiService = RetrofitClient.getApiService();
        activitySectionErrorText.setVisibility(View.GONE);
        activityRetryButton.setVisibility(View.GONE);

        apiService.getUserActivity(backendUserId, RECENT_ACTIVITY_LIMIT).enqueue(new Callback<List<UserActivityItem>>() {
            @Override
            public void onResponse(Call<List<UserActivityItem>> call, Response<List<UserActivityItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    activityAdapter.updateItems(new ArrayList<>());
                    activityEmptyContainer.setVisibility(View.VISIBLE);
                    activitySectionErrorText.setVisibility(View.VISIBLE);
                    activitySectionErrorText.setText("Could not load recent activity.");
                    activityRetryButton.setVisibility(View.VISIBLE);
                    showContentState();
                    Toast.makeText(ProfileActivity.this, "Failed to load activity", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<UserActivityItem> activities = response.body();
                activityAdapter.updateItems(activities);
                activityEmptyContainer.setVisibility(activities.isEmpty() ? View.VISIBLE : View.GONE);
                showContentState();
            }

            @Override
            public void onFailure(Call<List<UserActivityItem>> call, Throwable t) {
                activityAdapter.updateItems(new ArrayList<>());
                activityEmptyContainer.setVisibility(View.VISIBLE);
                activitySectionErrorText.setVisibility(View.VISIBLE);
                activitySectionErrorText.setText("Unable to load activity right now.");
                activityRetryButton.setVisibility(View.VISIBLE);
                showContentState();
                Toast.makeText(ProfileActivity.this, "Failed to load activity: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindProfile(UserProfileResponse profile) {
        String displayName = fallback(profile.getDisplayName(), "Player");
        String email = fallback(profile.getEmail(), "Email not available");

        displayNameText.setText(displayName);
        emailText.setText(email);

        avatarFallbackText.setText(extractInitial(displayName));
        if (TextUtils.isEmpty(profile.getAvatarUrl())) {
            avatarHintText.setText("No avatar linked");
        } else {
            avatarHintText.setText("Avatar linked");
        }

        totalGamesText.setText(formatNumber(profile.getTotalGames()));
        totalFavouritesText.setText(formatNumber(profile.getTotalFavourites()));
        totalNotesText.setText(formatNumber(profile.getTotalNotes()));
        totalRemindersText.setText(formatNumber(profile.getTotalReminders()));
        completedTasksText.setText(formatNumber(profile.getCompletedTasks()));

        totalTimeText.setText(formatDuration(profile.getTotalTimeSpent()));
        lastActivityText.setText(fallback(profile.getLastActivityAt(), "No activity yet"));
    }

    private void showLoadingState() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        errorContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);

        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void showContentState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        contentContainer.setVisibility(View.VISIBLE);

        if (latestProfile == null) {
            showErrorState("Could not load profile.");
        }
    }

    private String formatNumber(Integer value) {
        return String.valueOf(value != null ? value : 0);
    }

    private String formatDuration(Integer totalSeconds) {
        int seconds = totalSeconds != null ? Math.max(totalSeconds, 0) : 0;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;

        if (hours > 0) {
            return String.format(Locale.US, "%dh %dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format(Locale.US, "%dm", minutes);
        }
        return seconds + "s";
    }

    private String fallback(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String extractInitial(String name) {
        String safeName = fallback(name, "Player").trim();
        return safeName.isEmpty() ? "P" : safeName.substring(0, 1).toUpperCase(Locale.US);
    }
}
