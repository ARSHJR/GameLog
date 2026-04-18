package com.example.gamelog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.android.material.button.MaterialButtonToggleGroup;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectionGameDetailActivity extends AppCompatActivity {

    public static final String EXTRA_USER_GAME_ID = "extra_user_game_id";
    public static final String EXTRA_GAME_ID = "extra_game_id";
    public static final String EXTRA_GAME_TITLE = "extra_game_title";

    private static final String TYPE_NOTE = "note";
    private static final String TYPE_REMINDER = "reminder";

    private String userGameId;
    private String gameTitle;
    private String selectedType = TYPE_NOTE;

    private RecyclerView recyclerView;
    private CollectionNotesAdapter adapter;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View errorContainer;
    private TextView errorText;
    private Button retryButton;
    private View emptyContainer;
    private TextView emptyTitle;
    private TextView emptySubtitle;
    private boolean isCreateRequestInFlight;
    private final Set<String> mutationInFlightNoteIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_game_detail);

        userGameId = getIntent().getStringExtra(EXTRA_USER_GAME_ID);
        gameTitle = getIntent().getStringExtra(EXTRA_GAME_TITLE);

        if (TextUtils.isEmpty(userGameId)) {
            Toast.makeText(this, "Missing collection entry id.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupHeader();
        setupTabs();
        findViewById(R.id.collection_detail_add_fab).setOnClickListener(v -> showCreateDialog());

        retryButton.setOnClickListener(v -> fetchCurrentTabData());
        fetchCurrentTabData();
    }

    private void bindViews() {
        recyclerView = findViewById(R.id.collection_detail_recycler_view);
        loadingProgress = findViewById(R.id.collection_detail_loading_progress);
        loadingText = findViewById(R.id.collection_detail_loading_text);
        errorContainer = findViewById(R.id.collection_detail_error_container);
        errorText = findViewById(R.id.collection_detail_error_text);
        retryButton = findViewById(R.id.collection_detail_retry_button);
        emptyContainer = findViewById(R.id.collection_detail_empty_container);
        emptyTitle = findViewById(R.id.collection_detail_empty_title);
        emptySubtitle = findViewById(R.id.collection_detail_empty_subtitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CollectionNotesAdapter(new ArrayList<>());
        adapter.setOnNoteMutationListener(new CollectionNotesAdapter.OnNoteMutationListener() {
            @Override
            public void onTogglePin(CollectionNoteItem item) {
                togglePin(item);
            }

            @Override
            public void onToggleTaskStatus(CollectionNoteItem item, String nextStatus) {
                updateTaskStatus(item, nextStatus);
            }
        });
        recyclerView.setAdapter(adapter);

        findViewById(R.id.collection_detail_back_button).setOnClickListener(v -> finish());
    }

    private void setupHeader() {
        TextView titleView = findViewById(R.id.collection_detail_title);
        TextView subtitleView = findViewById(R.id.collection_detail_subtitle);

        titleView.setText(TextUtils.isEmpty(gameTitle) ? "Collection Entry" : gameTitle);
        subtitleView.setText("Notes and reminders for this game");
    }

    private void setupTabs() {
        MaterialButtonToggleGroup tabGroup = findViewById(R.id.collection_detail_tab_group);
        tabGroup.check(R.id.collection_tab_notes);
        tabGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            if (checkedId == R.id.collection_tab_notes) {
                selectedType = TYPE_NOTE;
            } else if (checkedId == R.id.collection_tab_reminders) {
                selectedType = TYPE_REMINDER;
            }

            fetchCurrentTabData();
        });
    }

    private void fetchCurrentTabData() {
        showLoadingState();

        ApiService apiService = RetrofitClient.getApiService();
        Call<List<CollectionNoteItem>> call = apiService.getCollectionNotes(userGameId, selectedType);
        call.enqueue(new Callback<List<CollectionNoteItem>>() {
            @Override
            public void onResponse(Call<List<CollectionNoteItem>> call, Response<List<CollectionNoteItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showErrorState("Could not load " + selectedType + " entries.");
                    Toast.makeText(CollectionGameDetailActivity.this, "Failed to load " + selectedType + " entries", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<CollectionNoteItem> sortedItems = new ArrayList<>(response.body());
                sortPinnedFirst(sortedItems);
                adapter.updateItems(sortedItems);
                adapter.setMutationInFlightNoteIds(mutationInFlightNoteIds);

                if (sortedItems.isEmpty()) {
                    showEmptyState();
                } else {
                    showContentState();
                }
            }

            @Override
            public void onFailure(Call<List<CollectionNoteItem>> call, Throwable t) {
                showErrorState("Unable to connect to server.");
                Toast.makeText(CollectionGameDetailActivity.this, "Failed to load " + selectedType + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void togglePin(CollectionNoteItem item) {
        String noteId = item.getNoteId();
        if (TextUtils.isEmpty(noteId) || mutationInFlightNoteIds.contains(noteId)) {
            return;
        }

        mutationInFlightNoteIds.add(noteId);
        adapter.setMutationInFlightNoteIds(mutationInFlightNoteIds);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.toggleNotePin(noteId).enqueue(new Callback<CollectionNoteItem>() {
            @Override
            public void onResponse(Call<CollectionNoteItem> call, Response<CollectionNoteItem> response) {
                mutationInFlightNoteIds.remove(noteId);
                adapter.setMutationInFlightNoteIds(mutationInFlightNoteIds);

                if (!response.isSuccessful()) {
                    Toast.makeText(CollectionGameDetailActivity.this, "Failed to update pin state", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(CollectionGameDetailActivity.this, "Pin state updated", Toast.LENGTH_SHORT).show();
                fetchCurrentTabData();
            }

            @Override
            public void onFailure(Call<CollectionNoteItem> call, Throwable t) {
                mutationInFlightNoteIds.remove(noteId);
                adapter.setMutationInFlightNoteIds(mutationInFlightNoteIds);
                Toast.makeText(CollectionGameDetailActivity.this, "Failed to update pin: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTaskStatus(CollectionNoteItem item, String nextStatus) {
        String noteId = item.getNoteId();
        if (TextUtils.isEmpty(noteId) || mutationInFlightNoteIds.contains(noteId)) {
            return;
        }

        mutationInFlightNoteIds.add(noteId);
        adapter.setMutationInFlightNoteIds(mutationInFlightNoteIds);

        ApiService apiService = RetrofitClient.getApiService();
        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(nextStatus);
        apiService.updateReminderTaskStatus(noteId, request).enqueue(new Callback<CollectionNoteItem>() {
            @Override
            public void onResponse(Call<CollectionNoteItem> call, Response<CollectionNoteItem> response) {
                mutationInFlightNoteIds.remove(noteId);
                adapter.setMutationInFlightNoteIds(mutationInFlightNoteIds);

                if (!response.isSuccessful()) {
                    Toast.makeText(CollectionGameDetailActivity.this, "Failed to update reminder status", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(CollectionGameDetailActivity.this,
                        "completed".equals(nextStatus) ? "Marked completed" : "Marked pending",
                        Toast.LENGTH_SHORT).show();
                fetchCurrentTabData();
            }

            @Override
            public void onFailure(Call<CollectionNoteItem> call, Throwable t) {
                mutationInFlightNoteIds.remove(noteId);
                adapter.setMutationInFlightNoteIds(mutationInFlightNoteIds);
                Toast.makeText(CollectionGameDetailActivity.this, "Failed to update reminder: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_collection_note, null);

        MaterialButtonToggleGroup typeGroup = dialogView.findViewById(R.id.create_note_type_group);
        EditText titleInput = dialogView.findViewById(R.id.create_note_title);
        EditText noteTextInput = dialogView.findViewById(R.id.create_note_text);
        EditText mediaUriInput = dialogView.findViewById(R.id.create_note_media_uri);
        EditText latitudeInput = dialogView.findViewById(R.id.create_note_latitude);
        EditText longitudeInput = dialogView.findViewById(R.id.create_note_longitude);
        TextView feedbackText = dialogView.findViewById(R.id.create_note_feedback);
        Button cancelButton = dialogView.findViewById(R.id.create_note_cancel);
        Button saveButton = dialogView.findViewById(R.id.create_note_save);

        if (TYPE_REMINDER.equals(selectedType)) {
            typeGroup.check(R.id.create_type_reminder);
        } else {
            typeGroup.check(R.id.create_type_note);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        cancelButton.setOnClickListener(v -> {
            if (!isCreateRequestInFlight) {
                dialog.dismiss();
            }
        });

        saveButton.setOnClickListener(v -> {
            if (isCreateRequestInFlight) {
                return;
            }

            String createType = resolveCreateType(typeGroup.getCheckedButtonId());
            if (TextUtils.isEmpty(createType)) {
                showDialogError(feedbackText, "Please choose Note or Reminder.");
                return;
            }

            String title = toNullableText(titleInput.getText() != null ? titleInput.getText().toString() : null);
            String noteText = toNullableText(noteTextInput.getText() != null ? noteTextInput.getText().toString() : null);
            String mediaUri = toNullableText(mediaUriInput.getText() != null ? mediaUriInput.getText().toString() : null);

            if (title == null && noteText == null) {
                showDialogError(feedbackText, "Add at least a title or note text.");
                return;
            }

            Double latitude = parseNullableDouble(latitudeInput.getText() != null ? latitudeInput.getText().toString() : null);
            Double longitude = parseNullableDouble(longitudeInput.getText() != null ? longitudeInput.getText().toString() : null);

            String rawLat = toNullableText(latitudeInput.getText() != null ? latitudeInput.getText().toString() : null);
            String rawLng = toNullableText(longitudeInput.getText() != null ? longitudeInput.getText().toString() : null);
            if ((rawLat != null && latitude == null) || (rawLng != null && longitude == null)) {
                showDialogError(feedbackText, "Latitude/Longitude must be valid numbers.");
                return;
            }

            hideDialogError(feedbackText);
            createNoteOrReminder(dialog, saveButton, cancelButton, createType, title, noteText, mediaUri, latitude, longitude);
        });

        dialog.show();
    }

    private void createNoteOrReminder(AlertDialog dialog, Button saveButton, Button cancelButton,
                                      String createType, String title, String noteText,
                                      String mediaUri, Double latitude, Double longitude) {
        isCreateRequestInFlight = true;
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        saveButton.setText("Creating...");

        String taskStatus = TYPE_REMINDER.equals(createType) ? "pending" : null;
        CreateCollectionNoteRequest request = new CreateCollectionNoteRequest(
                createType,
                title,
                noteText,
                mediaUri,
                latitude,
                longitude,
                taskStatus
        );

        ApiService apiService = RetrofitClient.getApiService();
        apiService.createCollectionNote(userGameId, request).enqueue(new Callback<CollectionNoteItem>() {
            @Override
            public void onResponse(Call<CollectionNoteItem> call, Response<CollectionNoteItem> response) {
                isCreateRequestInFlight = false;
                saveButton.setEnabled(true);
                cancelButton.setEnabled(true);
                saveButton.setText("Create");

                if (!response.isSuccessful()) {
                    Toast.makeText(CollectionGameDetailActivity.this, "Failed to create entry", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                Toast.makeText(CollectionGameDetailActivity.this,
                        TYPE_REMINDER.equals(createType) ? "Reminder created" : "Note created",
                        Toast.LENGTH_SHORT).show();

                if (!TextUtils.equals(selectedType, createType)) {
                    selectedType = createType;
                    MaterialButtonToggleGroup tabGroup = findViewById(R.id.collection_detail_tab_group);
                    if (TYPE_REMINDER.equals(createType)) {
                        tabGroup.check(R.id.collection_tab_reminders);
                    } else {
                        tabGroup.check(R.id.collection_tab_notes);
                    }
                } else {
                    fetchCurrentTabData();
                }
            }

            @Override
            public void onFailure(Call<CollectionNoteItem> call, Throwable t) {
                isCreateRequestInFlight = false;
                saveButton.setEnabled(true);
                cancelButton.setEnabled(true);
                saveButton.setText("Create");
                Toast.makeText(CollectionGameDetailActivity.this, "Failed to create entry: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String resolveCreateType(int checkedId) {
        if (checkedId == R.id.create_type_note) {
            return TYPE_NOTE;
        }
        if (checkedId == R.id.create_type_reminder) {
            return TYPE_REMINDER;
        }
        return null;
    }

    private String toNullableText(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Double parseNullableDouble(String raw) {
        String normalized = toNullableText(raw);
        if (normalized == null) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void showDialogError(TextView feedbackText, String message) {
        feedbackText.setVisibility(View.VISIBLE);
        feedbackText.setText(message);
    }

    private void hideDialogError(TextView feedbackText) {
        feedbackText.setVisibility(View.GONE);
        feedbackText.setText("");
    }

    private void sortPinnedFirst(List<CollectionNoteItem> items) {
        Collections.sort(items, Comparator.comparing(
                item -> !Boolean.TRUE.equals(item.getIsPinned())
        ));
    }

    private void showLoadingState() {
        loadingProgress.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("Loading " + selectedType + " entries...");

        errorContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        emptyContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        errorContainer.setVisibility(View.VISIBLE);
        errorText.setText(message);
    }

    private void showEmptyState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        errorContainer.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        emptyContainer.setVisibility(View.VISIBLE);
        if (TYPE_NOTE.equals(selectedType)) {
            emptyTitle.setText("No notes yet");
            emptySubtitle.setText("This collection entry does not have saved notes yet.");
        } else {
            emptyTitle.setText("No reminders yet");
            emptySubtitle.setText("This collection entry does not have reminder tasks yet.");
        }
    }

    private void showContentState() {
        loadingProgress.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);

        errorContainer.setVisibility(View.GONE);
        emptyContainer.setVisibility(View.GONE);

        recyclerView.setVisibility(View.VISIBLE);
    }
}
