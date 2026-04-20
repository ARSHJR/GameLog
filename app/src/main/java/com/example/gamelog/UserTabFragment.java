package com.example.gamelog;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserTabFragment extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_profile, container, false);

        applyHeaderInsets(root.findViewById(R.id.profile_header_container));

        ImageButton backButton = root.findViewById(R.id.profile_back_button);
        backButton.setVisibility(View.GONE);
        ImageButton settingsButton = root.findViewById(R.id.profile_settings_button);
        settingsButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));

        loadingProgress = root.findViewById(R.id.profile_loading_progress);
        loadingText = root.findViewById(R.id.profile_loading_text);
        errorContainer = root.findViewById(R.id.profile_error_container);
        errorText = root.findViewById(R.id.profile_error_text);
        retryButton = root.findViewById(R.id.profile_retry_button);

        contentContainer = root.findViewById(R.id.profile_content_container);
        displayNameText = root.findViewById(R.id.profile_display_name);
        emailText = root.findViewById(R.id.profile_email);
        avatarFallbackText = root.findViewById(R.id.profile_avatar_fallback);
        avatarHintText = root.findViewById(R.id.profile_avatar_hint);

        totalGamesText = root.findViewById(R.id.profile_total_games);
        totalFavouritesText = root.findViewById(R.id.profile_total_favourites);
        totalNotesText = root.findViewById(R.id.profile_total_notes);
        totalRemindersText = root.findViewById(R.id.profile_total_reminders);
        completedTasksText = root.findViewById(R.id.profile_completed_tasks);

        totalTimeText = root.findViewById(R.id.profile_total_time);
        lastActivityText = root.findViewById(R.id.profile_last_activity);

        activityRecyclerView = root.findViewById(R.id.profile_activity_recycler_view);
        activityEmptyContainer = root.findViewById(R.id.profile_activity_empty_container);
        activitySectionErrorText = root.findViewById(R.id.profile_activity_error_text);
        activityRetryButton = root.findViewById(R.id.profile_activity_retry_button);
        Button signOutButton = root.findViewById(R.id.profile_sign_out_button);

        activityRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        activityAdapter = new UserActivityAdapter(new ArrayList<>());
        activityRecyclerView.setAdapter(activityAdapter);

        retryButton.setOnClickListener(v -> fetchProfileAndActivity());
        activityRetryButton.setOnClickListener(v -> fetchActivityOnly());
        signOutButton.setOnClickListener(v -> performSignOut());

        backendUserId = BackendUserHelper.getBackendUserId(requireContext());
        if (TextUtils.isEmpty(backendUserId)) {
            showErrorState("Missing user context.");
            return root;
        }

        fetchProfileAndActivity();
        return root;
    }

    private void fetchProfileAndActivity() {
        showLoadingState();
        ApiService apiService = RetrofitClient.getApiService();

        apiService.getUserProfile(backendUserId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState("Could not load profile.");
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                    return;
                }

                latestProfile = response.body();
                bindProfile(response.body());
                fetchActivityOnly();
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                showErrorState("Unable to connect to server.");
                Toast.makeText(requireContext(), "Failed to load profile: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                if (!isAdded()) {
                    return;
                }
                if (!response.isSuccessful() || response.body() == null) {
                    activityAdapter.updateItems(new ArrayList<>());
                    activityEmptyContainer.setVisibility(View.VISIBLE);
                    activitySectionErrorText.setVisibility(View.VISIBLE);
                    activitySectionErrorText.setText("Could not load recent activity.");
                    activityRetryButton.setVisibility(View.VISIBLE);
                    showContentState();
                    Toast.makeText(requireContext(), "Failed to load activity", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<UserActivityItem> activities = response.body();
                activityAdapter.updateItems(activities);
                activityEmptyContainer.setVisibility(activities.isEmpty() ? View.VISIBLE : View.GONE);
                showContentState();
            }

            @Override
            public void onFailure(Call<List<UserActivityItem>> call, Throwable t) {
                if (!isAdded()) {
                    return;
                }
                activityAdapter.updateItems(new ArrayList<>());
                activityEmptyContainer.setVisibility(View.VISIBLE);
                activitySectionErrorText.setVisibility(View.VISIBLE);
                activitySectionErrorText.setText("Unable to load activity right now.");
                activityRetryButton.setVisibility(View.VISIBLE);
                showContentState();
                Toast.makeText(requireContext(), "Failed to load activity: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void performSignOut() {
        if (!isAdded()) {
            return;
        }

        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("GameLogPrefs", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        BackendUserHelper.clearBackendUserId(requireContext());

        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void applyHeaderInsets(View headerContainer) {
        if (headerContainer == null) {
            return;
        }

        final int baseTopPadding = headerContainer.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(headerContainer, (view, insets) -> {
            int statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPadding(
                    view.getPaddingLeft(),
                    baseTopPadding + statusBarTop,
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(headerContainer);
    }
}
